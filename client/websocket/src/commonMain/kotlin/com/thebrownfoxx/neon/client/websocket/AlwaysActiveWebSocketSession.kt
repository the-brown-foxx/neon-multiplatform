package com.thebrownfoxx.neon.client.websocket

import com.thebrownfoxx.neon.client.websocket.WebSocketRequester.RequestTimeout
import com.thebrownfoxx.neon.common.data.websocket.WebSocketSession
import com.thebrownfoxx.neon.common.data.websocket.WebSocketSession.SendError
import com.thebrownfoxx.neon.common.data.websocket.awaitClose
import com.thebrownfoxx.neon.common.data.websocket.model.SerializedWebSocketMessage
import com.thebrownfoxx.neon.common.data.websocket.model.WebSocketMessage
import com.thebrownfoxx.neon.common.extension.ExponentialBackoff
import com.thebrownfoxx.neon.common.extension.ExponentialBackoffValues
import com.thebrownfoxx.neon.common.extension.coroutineScope
import com.thebrownfoxx.neon.common.extension.flow.channelFlow
import com.thebrownfoxx.neon.common.extension.flow.mirror
import com.thebrownfoxx.neon.common.extension.loop
import com.thebrownfoxx.neon.common.extension.supervisorScope
import com.thebrownfoxx.neon.common.extension.withTimeout
import com.thebrownfoxx.neon.common.logError
import com.thebrownfoxx.neon.common.logInfo
import com.thebrownfoxx.neon.common.type.Type
import com.thebrownfoxx.outcome.Failure
import com.thebrownfoxx.outcome.Outcome
import com.thebrownfoxx.outcome.Success
import com.thebrownfoxx.outcome.UnitOutcome
import com.thebrownfoxx.outcome.UnitSuccess
import com.thebrownfoxx.outcome.map.getOrNull
import com.thebrownfoxx.outcome.map.onFailure
import com.thebrownfoxx.outcome.map.onSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

@Deprecated("Use a WebSocketSessionProvider instead")
class AlwaysActiveWebSocketSession : WebSocketSession, WebSocketSubscriber, WebSocketRequester {
    private val requestTimeout = 5.seconds

    private val connectionExponentialBackoffValues = ExponentialBackoffValues(
        initialDelay = 1.seconds,
        maxDelay = 32.seconds,
        factor = 2.0,
    )

    private val responseExponentialBackoffValues = ExponentialBackoffValues(
        initialDelay = 5.seconds,
        maxDelay = 32.seconds,
        factor = 2.0,
    )

    private val session = MutableStateFlow<WebSocketSession?>(null)
    private var collectionJob: Job? = null

    private val sendChannel = Channel<Message>(Channel.BUFFERED)

    /**
     * AlwaysActiveWebSocketSession will never close so this Flow will never emit anything.
     */
    override val closed = MutableStateFlow(false).asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SerializedWebSocketMessage>()
    override val incomingMessages = _incomingMessages.asSharedFlow()

    override suspend fun send(message: Any?, type: Type): UnitOutcome<SendError> {
        sendChannel.send(Message(message, type))
        return UnitSuccess
    }

    /**
     * This will not actually close AlwaysActiveWebSocketSession but will close the current
     * underlying WebSocketSession.
     */
    override suspend fun close() {
        session.value?.close()
    }

    suspend fun connect(
        initializeSession: suspend () -> Outcome<WebSocketSession, WebSocketConnectionError>,
    ) {
        val exponentialBackoff = ExponentialBackoff(connectionExponentialBackoffValues)
        loop {
            initializeSession()
                .onFailure { onConnectionFailure(it, log, onUnauthorized = { breakLoop() }) }
                .onSuccess { onConnectionSuccess(it, exponentialBackoff) }
            exponentialBackoff.delay()
        }
    }

    override fun <R> subscribeAsFlow(
        request: WebSocketMessage?,
        requestType: Type,
        handleResponse: SubscriptionHandler<R>.() -> Unit,
    ): Flow<R> {
        return channelFlow {
            session.collectLatest { session ->
                if (session == null) return@collectLatest
                val responseExponentialBackoff =
                    ExponentialBackoff(responseExponentialBackoffValues)
                while (true) {
                    val subscriptionHandler = SubscriptionHandler.create(
                        requestId = request?.requestId,
                        session = session,
                        externalScope = this,
                        handleResponse = handleResponse,
                    )
                    val mirrorJob = launch { mirror(subscriptionHandler.response) }
                    if (request != null) session.send(request, requestType)
                    responseExponentialBackoff.withTimeout {
                        subscriptionHandler.awaitFirst()
                        runAfterTimeout {
                            responseExponentialBackoff.reset()
                            session.awaitClose()
                        }
                    }
                    mirrorJob.cancel()
                }
            }
        }
    }

    override suspend fun <R> request(
        request: WebSocketMessage?,
        requestType: Type,
        handleResponse: RequestHandler<R>.() -> Unit,
    ): Outcome<R, RequestTimeout> {
        val session = session.filterNotNull().first()
        var response: R? = null
        supervisorScope {
            val requestHandler = RequestHandler.create(
                webSocketSession = session,
                externalScope = this,
                handleResponse = handleResponse,
            )
            session.send(request, requestType)
            withTimeout(requestTimeout) {
                response = requestHandler.await()
            }
            cancel()
        }
        return when (val finalResponse = response) {
            null -> Failure(RequestTimeout)
            else -> Success(finalResponse)
        }
    }

    private fun onConnectionFailure(
        error: WebSocketConnectionError,
        log: String,
        onUnauthorized: () -> Unit,
    ) {
        logError("WebSocket connection failed. $log")
        when (error) {
            WebSocketConnectionError.Unauthorized -> {
                logError("WebSocket reconnection canceled")
                onUnauthorized()
            }

            WebSocketConnectionError.ConnectionError ->
                logError("Reconnecting WebSocket")
        }
    }

    private suspend fun onConnectionSuccess(
        session: WebSocketSession,
        exponentialBackoff: ExponentialBackoff,
    ) {
        logInfo("WebSocket connected")
        collectionJob?.cancel()
        collectionJob?.join()
        this.session.value = session
        collectionJob = coroutineScope {
            launch {
                launch { session.mirrorIncomingMessages() }
                launch { session.sendQueuedMessages() }
            }
        }.getOrNull()
        exponentialBackoff.reset()
        session.awaitClose()
        logInfo("WebSocket finished. Reconnecting...")
    }

    private suspend fun WebSocketSession.mirrorIncomingMessages() {
        _incomingMessages.mirror(incomingMessages) {
            val label = it.serializedValue.getOrNull() ?: "<unknown message>"
            logInfo("WS RECEIVED: $label")
            it
        }
    }

    private suspend fun WebSocketSession.sendQueuedMessages() {
        while (true) {
            yield()
            val message = sendChannel.receive()
            val exponentialBackoff = ExponentialBackoff(
                initialDelay = 1.seconds,
                maxDelay = 32.seconds,
                factor = 2.0,
            )
            loop {
                send(message.value, message.type).onSuccess {
                    logInfo("WS SENT: ${message.value}")
                    breakLoop()
                }
                exponentialBackoff.delay()
            }
        }
    }
}

private data class Message(
    val value: Any?,
    val type: Type,
)
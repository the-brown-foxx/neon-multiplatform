package com.thebrownfoxx.neon.common.websocket

import com.thebrownfoxx.neon.common.Logger
import com.thebrownfoxx.neon.common.extension.withTimeout
import com.thebrownfoxx.neon.common.websocket.model.SerializedWebSocketMessage
import com.thebrownfoxx.neon.common.websocket.model.Type
import com.thebrownfoxx.neon.common.websocket.model.WebSocketMessage
import com.thebrownfoxx.neon.common.websocket.model.WebSocketMessageLabel
import com.thebrownfoxx.neon.common.websocket.model.deserialize
import com.thebrownfoxx.neon.common.websocket.model.typeOf
import com.thebrownfoxx.outcome.Failure
import com.thebrownfoxx.outcome.Outcome
import com.thebrownfoxx.outcome.UnitOutcome
import com.thebrownfoxx.outcome.getOrElse
import com.thebrownfoxx.outcome.getOrNull
import com.thebrownfoxx.outcome.mapError
import com.thebrownfoxx.outcome.onFailure
import com.thebrownfoxx.outcome.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

abstract class WebSocketSession(private val logger: Logger) {
    @PublishedApi
    internal val waitTimeout = 10.seconds

    protected abstract val sessionScope: CoroutineScope
    abstract val close: Flow<Unit> // TODO: Maybe remove this? Just use incomingMessages' completion
    abstract val incomingMessages: Flow<SerializedWebSocketMessage>

    abstract suspend fun send(message: Any?, type: Type): UnitOutcome<SendError>
    abstract suspend fun close()

    suspend inline fun <reified T : WebSocketMessage> send(
        message: T,
    ): UnitOutcome<SendError> = send(message, typeOf<T>())

    suspend inline fun <reified T : WebSocketMessage> sendAndWait(
        message: T,
    ): Outcome<SerializedWebSocketMessage, SendAndWaitError> {
        if (message.requestId != null) return Failure(SendAndWaitError.NoRequestIdError)
        send(message).onFailure { return mapError(SendAndWaitError.SendError) }
        return withTimeout(waitTimeout) {
            incomingMessages.first { it.getRequestId().getOrNull() == message.requestId }
        }.mapError { SendAndWaitError.WaitTimeout }
    }

    inline fun <reified T : WebSocketMessage> subscribe(
        crossinline action: (T) -> Unit,
    ): Job {
        return internalSessionScope.launch {
            incomingMessages
                .filter { it.getLabel().getOrNull() == WebSocketMessageLabel(T::class) }
                .collect { serializedMessage ->
                    serializedMessage.deserialize<T>()
                        .onSuccess { action(it) }
                        .onFailure { logSerializationFailure(serializedMessage) }
                }
        }
    }

    @PublishedApi
    internal fun logSerializationFailure(
        serializedMessage: SerializedWebSocketMessage,
    ) {
        val string = serializedMessage.serializedValue.getOrElse { serializedMessage.toString() }
        internalLogger.logError(message = "Failed to deserialize $string")
    }

    @PublishedApi
    internal val internalLogger: Logger
        get() = logger

    @PublishedApi
    internal val internalSessionScope: CoroutineScope
        get() = sessionScope

    data object SendError

    enum class SendAndWaitError {
        NoRequestIdError,
        SendError,
        WaitTimeout,
    }
}
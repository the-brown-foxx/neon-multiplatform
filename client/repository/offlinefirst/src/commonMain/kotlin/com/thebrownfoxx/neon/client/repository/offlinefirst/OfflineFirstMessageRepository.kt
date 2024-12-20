package com.thebrownfoxx.neon.client.repository.offlinefirst

import com.thebrownfoxx.neon.client.converter.toLocalMessage
import com.thebrownfoxx.neon.client.model.LocalConversationPreviews
import com.thebrownfoxx.neon.client.model.LocalMessage
import com.thebrownfoxx.neon.client.repository.MessageRepository
import com.thebrownfoxx.neon.client.repository.local.LocalMessageDataSource
import com.thebrownfoxx.neon.client.repository.remote.RemoteMessageDataSource
import com.thebrownfoxx.neon.common.data.DataOperationError
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.type.id.MessageId
import com.thebrownfoxx.outcome.Outcome
import com.thebrownfoxx.outcome.Success
import com.thebrownfoxx.outcome.getOrElse
import com.thebrownfoxx.outcome.mapError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class OfflineFirstMessageRepository(
    private val localDataSource: LocalMessageDataSource,
    private val remoteDataSource: RemoteMessageDataSource,
) : MessageRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

    override val conversationPreviews = run {
        val sharedFlow =
            MutableSharedFlow<Outcome<LocalConversationPreviews, DataOperationError>>(replay = 1)

        coroutineScope.launch {
            localDataSource.conversationPreviews.collect { localConversationPreviewsOutcome ->
                val localConversationPreviews =
                    localConversationPreviewsOutcome.getOrElse {
                        sharedFlow.emit(mapError(error))
                        return@collect
                    }
                sharedFlow.emit(Success(localConversationPreviews))
            }
        }

        coroutineScope.launch {
            remoteDataSource.conversationPreviews.collect { remoteConversationPreviewsOutcome ->
                val remoteConversationPreviews =
                    remoteConversationPreviewsOutcome.getOrElse {
                        sharedFlow.emit(mapError(error))
                        return@collect
                    }
                localDataSource.batchUpsert(remoteConversationPreviews.map { it.toLocalMessage() })
            }
        }

        sharedFlow.asSharedFlow()
    }

    override fun get(id: MessageId): Flow<Outcome<LocalMessage, GetError>> {
        val sharedFlow = MutableSharedFlow<Outcome<LocalMessage, GetError>>(replay = 1)

        coroutineScope.launch {
            localDataSource.getMessageAsFlow(id).collect { localMessageOutcome ->
                val localMessage = localMessageOutcome.getOrElse {
                    when (error) {
                        GetError.NotFound -> {}
                        GetError.ConnectionError, GetError.UnexpectedError ->
                            sharedFlow.emit(mapError(error))
                    }
                    return@collect
                }
                sharedFlow.emit(Success(localMessage))
            }
        }

        coroutineScope.launch {
            remoteDataSource.getMessageAsFlow(id).collect { remoteMessageOutcome ->
                val remoteMessage = remoteMessageOutcome.getOrElse {
                    sharedFlow.emit(mapError(error))
                    return@collect
                }
                localDataSource.upsert(remoteMessage.toLocalMessage())
            }
        }

        return sharedFlow.asSharedFlow()
    }
}
package com.thebrownfoxx.neon.client.repository

import com.thebrownfoxx.neon.client.model.LocalChatPreviews
import com.thebrownfoxx.neon.client.model.LocalMessage
import com.thebrownfoxx.neon.client.model.LocalTimestampedMessageId
import com.thebrownfoxx.neon.common.data.DataOperationError
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.type.id.GroupId
import com.thebrownfoxx.neon.common.type.id.MessageId
import com.thebrownfoxx.outcome.Outcome
import com.thebrownfoxx.outcome.UnitOutcome
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    val chatPreviews: Flow<Outcome<LocalChatPreviews, DataOperationError>>

    val outgoingQueue: ReceiveChannel<LocalMessage>

    fun getMessagesAsFlow(
        id: GroupId,
    ): Flow<Outcome<List<LocalTimestampedMessageId>, DataOperationError>>

    fun getMessageAsFlow(id: MessageId): Flow<Outcome<LocalMessage, GetError>>

    suspend fun upsert(message: LocalMessage): UnitOutcome<DataOperationError>

    suspend fun batchUpsert(messages: List<LocalMessage>): UnitOutcome<DataOperationError>

    suspend fun batchUpsertTimestampedIds(
        messageIds: List<LocalTimestampedMessageId>,
    ): UnitOutcome<DataOperationError>
}
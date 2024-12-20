package com.thebrownfoxx.neon.client.repository.remote

import com.thebrownfoxx.neon.common.data.DataOperationError
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.type.id.MessageId
import com.thebrownfoxx.neon.server.model.Message
import com.thebrownfoxx.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface RemoteMessageDataSource {
    val conversationPreviews: Flow<Outcome<List<Message>, DataOperationError>>
    fun getMessageAsFlow(id: MessageId): Flow<Outcome<Message, GetError>>
}
package com.thebrownfoxx.neon.server.application.websocket.message

import com.thebrownfoxx.neon.common.type.id.GroupId
import com.thebrownfoxx.neon.common.websocket.WebSocketSession
import com.thebrownfoxx.neon.server.model.ChatGroup
import com.thebrownfoxx.neon.server.model.Community
import com.thebrownfoxx.neon.server.route.websocket.group.GetGroupNotFound
import com.thebrownfoxx.neon.server.route.websocket.group.GetGroupRequest
import com.thebrownfoxx.neon.server.route.websocket.group.GetGroupSuccessfulChatGroup
import com.thebrownfoxx.neon.server.route.websocket.group.GetGroupSuccessfulCommunity
import com.thebrownfoxx.neon.server.route.websocket.group.GetGroupUnexpectedError
import com.thebrownfoxx.neon.server.service.GroupManager
import com.thebrownfoxx.neon.server.service.GroupManager.GetGroupError
import com.thebrownfoxx.outcome.onFailure
import com.thebrownfoxx.outcome.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

class GroupWebSocketMessageManager(
    private val session: WebSocketSession,
    private val groupManager: GroupManager,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO) + SupervisorJob()

    private val getGroupJobManager = JobManager<GroupId>(coroutineScope, session.close)

    init {
        session.subscribe<GetGroupRequest> { request ->
            getGroup(request.id)
        }
    }

    private fun getGroup(id: GroupId) {
        getGroupJobManager[id] = {
            groupManager.getGroup(id).collect { groupOutcome ->
                groupOutcome.onSuccess { group ->
                    when (group) {
                        is ChatGroup -> session.send(GetGroupSuccessfulChatGroup(group))
                        is Community -> session.send(GetGroupSuccessfulCommunity(group))
                    }
                }.onFailure {
                    when (error) {
                        GetGroupError.NotFound -> session.send(GetGroupNotFound(id))
                        GetGroupError.UnexpectedError ->
                            session.send(GetGroupUnexpectedError(id))
                    }
                }
            }
        }
    }
}
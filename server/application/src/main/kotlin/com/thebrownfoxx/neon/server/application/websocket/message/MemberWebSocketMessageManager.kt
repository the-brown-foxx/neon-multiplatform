package com.thebrownfoxx.neon.server.application.websocket.message

import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.neon.common.websocket.WebSocketSession
import com.thebrownfoxx.neon.common.websocket.listen
import com.thebrownfoxx.neon.common.websocket.send
import com.thebrownfoxx.neon.server.route.websocket.member.GetMemberNotFound
import com.thebrownfoxx.neon.server.route.websocket.member.GetMemberRequest
import com.thebrownfoxx.neon.server.route.websocket.member.GetMemberSuccessful
import com.thebrownfoxx.neon.server.route.websocket.member.GetMemberUnexpectedError
import com.thebrownfoxx.neon.server.service.MemberManager
import com.thebrownfoxx.neon.server.service.MemberManager.GetMemberError
import com.thebrownfoxx.outcome.map.onFailure
import com.thebrownfoxx.outcome.map.onSuccess
import kotlinx.coroutines.CoroutineScope

class MemberWebSocketMessageManager(
    private val session: WebSocketSession,
    private val memberManager: MemberManager,
    externalScope: CoroutineScope,
) {
    private val getMemberJobManager = JobManager<MemberId>(externalScope)

    init {
        session.listen<GetMemberRequest>(externalScope) { request ->
            getMember(request.id)
        }
    }

    private fun getMember(id: MemberId) {
        getMemberJobManager[id] = {
            memberManager.getMember(id).collect { memberOutcome ->
                memberOutcome.onSuccess { member ->
                    session.send(GetMemberSuccessful(member))
                }.onFailure { error ->
                    when (error) {
                        GetMemberError.NotFound -> session.send(GetMemberNotFound(id))
                        GetMemberError.UnexpectedError -> session.send(GetMemberUnexpectedError(id))
                    }
                }
            }
        }
    }
}
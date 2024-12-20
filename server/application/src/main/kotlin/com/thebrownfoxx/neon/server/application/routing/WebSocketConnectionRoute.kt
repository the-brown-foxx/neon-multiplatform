package com.thebrownfoxx.neon.server.application.routing

import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.neon.common.type.id.Uuid
import com.thebrownfoxx.neon.server.application.dependency.DependencyProvider
import com.thebrownfoxx.neon.server.application.plugin.AuthenticationType
import com.thebrownfoxx.neon.server.application.plugin.MemberIdClaim
import com.thebrownfoxx.neon.server.application.plugin.authenticate
import com.thebrownfoxx.neon.server.application.websocket.MutableKtorServerWebSocketSession
import com.thebrownfoxx.neon.server.application.websocket.message.WebSocketMessageManagers
import com.thebrownfoxx.neon.server.route.websocket.WebSocketConnectionResponse
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach

fun Route.webSocketConnectionRoute() {
    with(DependencyProvider.dependencies) {
        authenticate(AuthenticationType.Jwt) {
            webSocket("/connect") {
                val jwt = call.principal<JWTPrincipal>()!!.payload
                val memberIdValue = jwtProcessor.getClaim(jwt, MemberIdClaim)!!.value
                val memberId = MemberId(Uuid(memberIdValue))
                val session = MutableKtorServerWebSocketSession(
                    session = this,
                    memberId = memberId,
                    logger = logger,
                )
                webSocketManager.addSession(session)
                session.send(WebSocketConnectionResponse.ConnectionSuccessful())
                WebSocketMessageManagers(
                    session = session,
                    groupManager = groupManager,
                    memberManager = memberManager,
                    messenger = messenger,
                )
                incoming.consumeEach { session.emitFrame(it) }
                webSocketManager.removeSession(session.id)
            }
        }
    }
}
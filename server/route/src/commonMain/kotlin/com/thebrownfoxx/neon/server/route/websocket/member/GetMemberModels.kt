package com.thebrownfoxx.neon.server.route.websocket.member

import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.neon.common.websocket.model.WebSocketMessage
import com.thebrownfoxx.neon.server.model.Member
import kotlinx.serialization.Serializable

@Serializable
data class GetMemberRequest(
    val id: MemberId,
) : WebSocketMessage(kClass = this::class) {
    override val requestId = null
}

@Serializable
data class GetMemberNotFound(
    val id: MemberId,
) : WebSocketMessage(
    kClass = GetMemberNotFound::class,
    description = "The member with the given id was not found",
) {
    override val requestId = null
}

@Serializable
data class GetMemberConnectionError(
    val id: MemberId,
) : WebSocketMessage(
    kClass = GetMemberConnectionError::class,
    description = "There was an error connecting to one of the components of the server",
) {
    override val requestId = null
}

@Serializable
data class GetMemberSuccessful(
    val member: Member,
) : WebSocketMessage(
    kClass = GetMemberSuccessful::class,
    description = "Successfully retrieved the member",
) {
    override val requestId = null
}
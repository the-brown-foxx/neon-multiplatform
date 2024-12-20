package com.thebrownfoxx.neon.client.model

import com.thebrownfoxx.neon.common.type.Url
import com.thebrownfoxx.neon.common.type.id.MemberId
import kotlinx.serialization.Serializable

@Serializable
data class LocalMember(
    val id: MemberId = MemberId(),
    val username: String,
    val avatarUrl: Url?,
)
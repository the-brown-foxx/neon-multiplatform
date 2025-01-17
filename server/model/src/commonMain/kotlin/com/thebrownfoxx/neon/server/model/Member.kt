package com.thebrownfoxx.neon.server.model

import com.thebrownfoxx.neon.common.type.Url
import com.thebrownfoxx.neon.common.type.id.MemberId
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: MemberId = MemberId(),
    val username: String,
    val avatarUrl: Url?,
)
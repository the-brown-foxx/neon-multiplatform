package com.thebrownfoxx.neon.server.repository.data.model

import com.thebrownfoxx.neon.server.model.Member

data class MemberRecord(
    val member: Member,
    val inviteCode: String,
    val password: String,
)
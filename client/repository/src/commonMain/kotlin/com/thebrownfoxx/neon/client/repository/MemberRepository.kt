package com.thebrownfoxx.neon.client.repository

import com.thebrownfoxx.neon.client.model.LocalMember
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun get(id: MemberId): Flow<Outcome<LocalMember, GetError>>
}
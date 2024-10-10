package com.thebrownfoxx.neon.client.repository.memory

import com.thebrownfoxx.neon.client.repository.member.MemberRepository
import com.thebrownfoxx.neon.client.repository.member.model.AddMemberError
import com.thebrownfoxx.neon.client.repository.member.model.GetMemberError
import com.thebrownfoxx.neon.common.model.Failure
import com.thebrownfoxx.neon.common.model.Member
import com.thebrownfoxx.neon.common.model.MemberId
import com.thebrownfoxx.neon.common.model.Result
import com.thebrownfoxx.neon.common.model.Success
import com.thebrownfoxx.neon.common.model.UnitResult
import com.thebrownfoxx.neon.common.model.unitSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryMemberRepository : MemberRepository {
    private val members = MutableStateFlow<Map<MemberId, Member>>(emptyMap())

    override fun get(id: MemberId): Flow<Result<Member, GetMemberError>> {
        return members.mapLatest { members ->
            when (val member = members[id]) {
                null -> Failure(GetMemberError.NotFound)
                else -> Success(member)
            }
        }
    }

    override suspend fun add(member: Member): UnitResult<AddMemberError> {
        return when {
            members.value.containsKey(member.id) -> Failure(AddMemberError.DuplicateId)
            else -> {
                members.update { it + (member.id to member) }
                unitSuccess()
            }
        }
    }
}
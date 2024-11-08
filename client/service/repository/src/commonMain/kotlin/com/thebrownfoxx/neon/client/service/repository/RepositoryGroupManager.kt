package com.thebrownfoxx.neon.client.service.repository

import com.thebrownfoxx.neon.client.repository.group.GroupRepository
import com.thebrownfoxx.neon.client.repository.group.model.AddGroupEntityError
import com.thebrownfoxx.neon.client.repository.group.model.AddGroupMemberEntityError
import com.thebrownfoxx.neon.client.repository.group.model.GetGroupEntityError
import com.thebrownfoxx.neon.client.repository.group.model.InGodCommunityError
import com.thebrownfoxx.neon.client.repository.group.model.IsGroupAdminError
import com.thebrownfoxx.neon.client.service.authenticator.Authenticator
import com.thebrownfoxx.neon.client.service.group.GroupManager
import com.thebrownfoxx.neon.client.service.group.model.AddGroupMemberError
import com.thebrownfoxx.neon.client.service.group.model.CreateCommunityError
import com.thebrownfoxx.neon.client.service.group.model.GetGroupError
import com.thebrownfoxx.neon.common.model.Community
import com.thebrownfoxx.neon.common.model.Failure
import com.thebrownfoxx.neon.common.model.Group
import com.thebrownfoxx.neon.common.model.GroupId
import com.thebrownfoxx.neon.common.model.MemberId
import com.thebrownfoxx.neon.common.model.Result
import com.thebrownfoxx.neon.common.model.UnitResult
import com.thebrownfoxx.neon.common.model.getOrElse
import com.thebrownfoxx.neon.common.model.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RepositoryGroupManager(
    private val authenticator: Authenticator,
    private val groupRepository: GroupRepository,
) : GroupManager {
    override fun getGroup(id: GroupId): Flow<Result<Group, GetGroupError>> {
        return groupRepository.get(id).map {
            it.map(
                onSuccess = { it },
                onFailure = { it.toGetGroupError() },
            )
        }
    }

    private fun GetGroupEntityError.toGetGroupError() = when (this) {
        GetGroupEntityError.NotFound -> GetGroupError.NotFound
        GetGroupEntityError.ConnectionError -> GetGroupError.ConnectionError
    }

    override suspend fun createCommunity(
        name: String,
        inviteCode: String,
    ): Result<GroupId, CreateCommunityError> {
        val authorizationError = authorizeCreateCommunity()
        if (authorizationError != null) return Failure(authorizationError)

        val community = Community(
            name = name,
            avatarUrl = null,
            inviteCode = inviteCode,
            god = false,
        )

        return groupRepository.add(community).map(
            onSuccess = { community.id },
            onFailure = { it.toCreateCommunityError() },
        )
    }

    private suspend fun authorizeCreateCommunity(): CreateCommunityError? {
        val loggedInMember = authenticator.loggedInMemberId.value

        if (loggedInMember == null) return CreateCommunityError.ConnectionError

        val inGodGroup = groupRepository.inGodCommunity(loggedInMember).first()
            .getOrElse { return it.toCreateCommunityError(loggedInMember) }

        if (!inGodGroup) return CreateCommunityError.Unauthorized(loggedInMember)

        return null
    }

    private fun InGodCommunityError.toCreateCommunityError(
        loggedInMember: MemberId,
    ): CreateCommunityError {
        return when (this) {
            InGodCommunityError.ConnectionError -> CreateCommunityError.Unauthorized(loggedInMember)
        }
    }

    private fun AddGroupEntityError.toCreateCommunityError() = when (this) {
        AddGroupEntityError.DuplicateId -> error("Cannot add community with duplicate id")
        AddGroupEntityError.ConnectionError -> CreateCommunityError.ConnectionError
    }

    override suspend fun addMember(
        groupId: GroupId,
        memberId: MemberId,
        isAdmin: Boolean,
    ): UnitResult<AddGroupMemberError> {
        val authorizationError = authorizeAddMember(groupId)

        if (authorizationError != null) return Failure(authorizationError)

        return groupRepository.addMember(groupId, memberId, isAdmin).map(
            onSuccess = { it },
            onFailure = { it.toAddGroupMemberError() },
        )
    }

    private suspend fun authorizeAddMember(groupId: GroupId): AddGroupMemberError? {
        val loggedInMember = authenticator.loggedInMemberId.value

        if (loggedInMember == null) return AddGroupMemberError.Unauthorized(null)

        val inGodGroup = groupRepository.inGodCommunity(loggedInMember).first()
            .getOrElse { return it.toAddGroupMemberError() }

        if (!inGodGroup) return AddGroupMemberError.Unauthorized(loggedInMember)

        val isGroupAdmin = groupRepository.isGroupAdmin(groupId, loggedInMember).first()
            .getOrElse { return it.toAddGroupMemberError() }

        if (!isGroupAdmin) return AddGroupMemberError.Unauthorized(loggedInMember)

        return null
    }

    private fun InGodCommunityError.toAddGroupMemberError() = when (this) {
        InGodCommunityError.ConnectionError -> AddGroupMemberError.ConnectionError
    }

    private fun IsGroupAdminError.toAddGroupMemberError() = when (this) {
        IsGroupAdminError.NotFound -> AddGroupMemberError.GroupNotFound
        IsGroupAdminError.ConnectionError -> AddGroupMemberError.ConnectionError
    }

    private fun AddGroupMemberEntityError.toAddGroupMemberError() = when (this) {
        AddGroupMemberEntityError.GroupNotFound -> AddGroupMemberError.GroupNotFound
        AddGroupMemberEntityError.ConnectionError -> AddGroupMemberError.ConnectionError
    }
}
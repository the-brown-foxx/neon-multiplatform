package com.thebrownfoxx.neon.server.repository.test

import com.thebrownfoxx.neon.common.model.GroupId
import com.thebrownfoxx.neon.common.model.MemberId
import com.thebrownfoxx.neon.common.model.Success
import com.thebrownfoxx.neon.common.model.unitSuccess
import com.thebrownfoxx.neon.must.mustBe
import com.thebrownfoxx.neon.server.repository.groupmember.GroupMemberRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

abstract class GroupMemberRepositoryTest {
    private val groupXId = GroupId()
    private val memberYId = MemberId()

    private val initialGroupMembers = listOf(
        GroupMember(groupXId, MemberId(), admin = true),
        GroupMember(groupXId, memberYId),
        GroupMember(GroupId(), memberYId, admin = true),
    )

    private lateinit var groupMemberRepository: GroupMemberRepository

    abstract fun createRepository(): GroupMemberRepository

    @BeforeTest
    fun setup() = runTest {
        groupMemberRepository = createRepository()

        for (groupMember in initialGroupMembers) {
            groupMemberRepository.addMember(groupMember.groupId, groupMember.memberId)
        }
    }

    @Test
    fun getMembersShouldReturnMembers() = runTest {
        val actualMembers = groupMemberRepository.getMembers(groupXId).first()

        val expectedMembers = initialGroupMembers
            .filter { it.groupId == groupXId }
            .map { it.memberId }
        actualMembers mustBe Success(expectedMembers)
    }

    @Test
    fun getGroupsShouldReturnGroups() = runTest {
        val actualGroups = groupMemberRepository.getGroups(memberYId).first()

        val expectedGroups = initialGroupMembers
            .filter { it.memberId == memberYId }
            .map { it.groupId }
        actualGroups mustBe Success(expectedGroups)
    }

    @Test
    fun getAdminsShouldReturnAdmins() = runTest {
        val actualAdmins = groupMemberRepository.getAdmins(groupXId).first()

        val expectedAdmins = initialGroupMembers
            .filter { it.groupId == groupXId && it.admin }
            .map { it.memberId }

        actualAdmins mustBe Success(expectedAdmins)
    }

    @Test
    fun addMemberShouldAddMember() = runTest {
        val memberId = MemberId()

        val addResult = groupMemberRepository.addMember(groupXId, memberId)
        addResult mustBe unitSuccess()

        val actualMembers = groupMemberRepository.getMembers(groupXId).first()

        val expectedMembers = initialGroupMembers
            .filter { it.groupId == groupXId }
            .map { it.memberId } + memberId

        actualMembers mustBe Success(expectedMembers)
    }
}

private data class GroupMember(
    val groupId: GroupId,
    val memberId: MemberId,
    val admin: Boolean = false,
)
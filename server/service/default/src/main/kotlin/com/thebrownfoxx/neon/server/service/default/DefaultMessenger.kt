package com.thebrownfoxx.neon.server.service.default

import com.thebrownfoxx.neon.common.data.AddError
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.data.UpdateError
import com.thebrownfoxx.neon.common.data.transaction.transaction
import com.thebrownfoxx.neon.common.extension.flow
import com.thebrownfoxx.neon.common.outcome.Failure
import com.thebrownfoxx.neon.common.outcome.Outcome
import com.thebrownfoxx.neon.common.outcome.Success
import com.thebrownfoxx.neon.common.outcome.UnitOutcome
import com.thebrownfoxx.neon.common.outcome.asFailure
import com.thebrownfoxx.neon.common.outcome.getOrElse
import com.thebrownfoxx.neon.common.outcome.mapError
import com.thebrownfoxx.neon.common.outcome.onFailure
import com.thebrownfoxx.neon.common.outcome.unitSuccess
import com.thebrownfoxx.neon.common.type.id.GroupId
import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.neon.common.type.id.MessageId
import com.thebrownfoxx.neon.server.model.ChatGroup
import com.thebrownfoxx.neon.server.model.Delivery
import com.thebrownfoxx.neon.server.model.Message
import com.thebrownfoxx.neon.server.repository.GroupMemberRepository
import com.thebrownfoxx.neon.server.repository.GroupRepository
import com.thebrownfoxx.neon.server.repository.MemberRepository
import com.thebrownfoxx.neon.server.repository.MessageRepository
import com.thebrownfoxx.neon.server.service.messenger.Messenger
import com.thebrownfoxx.neon.server.service.messenger.model.GetConversationPreviewError
import com.thebrownfoxx.neon.server.service.messenger.model.GetConversationPreviewsError
import com.thebrownfoxx.neon.server.service.messenger.model.GetConversationsError
import com.thebrownfoxx.neon.server.service.messenger.model.GetMessageError
import com.thebrownfoxx.neon.server.service.messenger.model.GetMessagesError
import com.thebrownfoxx.neon.server.service.messenger.model.MarkConversationAsReadError
import com.thebrownfoxx.neon.server.service.messenger.model.NewConversationError
import com.thebrownfoxx.neon.server.service.messenger.model.SendMessageError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessenger(
    private val messageRepository: MessageRepository,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) : Messenger {
    @Suppress("DEPRECATION")
    @Deprecated("Use getConversationPreviews instead")
    override fun getConversations(
        actorId: MemberId,
    ): Flow<Outcome<Set<GroupId>, GetConversationsError>> {
        return combine(
            memberRepository.getAsFlow(actorId),
            messageRepository.getConversationsAsFlow(actorId),
        ) { memberOutcome, conversationsOutcome ->
            memberOutcome.onFailure { error ->
                return@combine when (error) {
                    GetError.NotFound -> GetConversationsError.MemberNotFound
                    GetError.ConnectionError -> GetConversationsError.InternalError
                }.asFailure()
            }
            conversationsOutcome.mapError { GetConversationsError.InternalError }
        }
    }

    @Deprecated("Use getConversationPreviews instead")
    override fun getConversationPreview(
        actorId: MemberId,
        groupId: GroupId,
    ): Flow<Outcome<MessageId?, GetConversationPreviewError>> {
        return groupRepository.getAsFlow(groupId).flatMapLatest { group ->
            group.onFailure { error ->
                return@flatMapLatest when (error) {
                    GetError.NotFound -> GetConversationPreviewError.GroupNotFound
                    GetError.ConnectionError -> GetConversationPreviewError.InternalError
                }.asFailure().flow()
            }
            getConversationPreviewFromRepository(groupId, actorId)
        }
    }

    override fun getConversationPreviews(
        actorId: MemberId,
    ): Flow<Outcome<List<Message>, GetConversationPreviewsError>> {
        return combine(
            memberRepository.getAsFlow(actorId),
            messageRepository.getConversationPreviewsAsFlow(actorId),
        ) { memberOutcome, conversationsOutcome ->
            memberOutcome.onFailure { error ->
                return@combine when (error) {
                    GetError.NotFound -> GetConversationPreviewsError.MemberNotFound
                    GetError.ConnectionError -> GetConversationPreviewsError.InternalError
                }.asFailure()
            }
            conversationsOutcome.mapError { GetConversationPreviewsError.InternalError }
        }
    }

    override fun getMessage(
        actorId: MemberId,
        id: MessageId,
    ): Flow<Outcome<Message, GetMessageError>> {
        return messageRepository.getAsFlow(id).flatMapLatest { messageOutcome ->
            val message = messageOutcome.getOrElse { error ->
                return@flatMapLatest when (error) {
                    GetError.NotFound -> GetMessageError.NotFound
                    GetError.ConnectionError -> GetMessageError.InternalError
                }.asFailure().flow()
            }

            groupMemberRepository.getMembersAsFlow(message.groupId)
                .mapLatest { groupMemberIdsOutcome ->
                    val groupMemberId = groupMemberIdsOutcome.getOrElse {
                        return@mapLatest Failure(GetMessageError.InternalError)
                    }

                    if (actorId !in groupMemberId)
                        return@mapLatest Failure(GetMessageError.Unauthorized)

                    Success(message)
                }
        }
    }

    private fun getConversationPreviewFromRepository(
        groupId: GroupId,
        actorId: MemberId,
    ): Flow<Outcome<MessageId?, GetConversationPreviewError>> {
        return messageRepository.getConversationPreviewAsFlow(groupId).flatMapLatest { messageOutcome ->
            val previewId = messageOutcome.getOrElse {
                return@flatMapLatest Failure(GetConversationPreviewError.InternalError).flow()
            }

            groupMemberRepository.getMembersAsFlow(groupId).mapLatest { groupMemberIdsOutcome ->
                val groupMemberId = groupMemberIdsOutcome.getOrElse {
                    return@mapLatest Failure(GetConversationPreviewError.InternalError)
                }

                if (actorId !in groupMemberId)
                    return@mapLatest Failure(GetConversationPreviewError.Unauthorized)

                Success(previewId)
            }
        }
    }

    override suspend fun getMessages(
        actorId: MemberId,
        groupId: GroupId,
    ): Outcome<Set<MessageId>, GetMessagesError> {
        TODO("Not yet implemented")
    }

    override suspend fun newConversation(
        memberIds: Set<MemberId>,
    ): UnitOutcome<NewConversationError> {
        for (memberId in memberIds) {
            memberRepository.get(memberId).onFailure { error ->
                return when (error) {
                    GetError.NotFound -> NewConversationError.MemberNotFound(memberId)
                    GetError.ConnectionError -> NewConversationError.InternalError
                }.asFailure()
            }
        }

        val chatGroup = ChatGroup()

        return transaction {
            groupRepository.add(chatGroup).register().onFailure { error ->
                return@transaction when (error) {
                    AddError.Duplicate -> error("What are the chances?")
                    AddError.ConnectionError -> NewConversationError.InternalError
                }.asFailure()
            }

            for (memberId in memberIds) {
                groupMemberRepository.addMember(
                    groupId = chatGroup.id,
                    memberId = memberId,
                    isAdmin = false,
                ).register().onFailure { error ->
                    return@transaction when (error) {
                        AddError.ConnectionError -> NewConversationError.InternalError
                        AddError.Duplicate -> error("Can't be?")
                    }.asFailure()
                }
            }

            unitSuccess()
        }
    }

    override suspend fun sendMessage(
        actorId: MemberId,
        groupId: GroupId,
        content: String,
    ): UnitOutcome<SendMessageError> {
        groupRepository.get(groupId).onFailure { error ->
            return when (error) {
                GetError.NotFound -> SendMessageError.GroupNotFound(groupId)
                GetError.ConnectionError -> SendMessageError.InternalError
            }.asFailure()
        }

        val groupMemberIds = groupMemberRepository.getMembers(groupId)
            .getOrElse { return Failure(SendMessageError.InternalError) }

        if (actorId !in groupMemberIds) return Failure(SendMessageError.Unauthorized(actorId))

        val message = Message(
            groupId = groupId,
            senderId = actorId,
            content = content,
            timestamp = Clock.System.now(),
            delivery = Delivery.Sent,
        )

        messageRepository.add(message).result.onFailure { error ->
            return when (error) {
                AddError.Duplicate -> error("What are the chances?")
                AddError.ConnectionError -> SendMessageError.InternalError
            }.asFailure()
        }

        return unitSuccess()
    }

    override suspend fun markConversationAsRead(
        actorId: MemberId,
        groupId: GroupId,
    ): UnitOutcome<MarkConversationAsReadError> {
        groupRepository.get(groupId).onFailure { error ->
            return when (error) {
                GetError.NotFound -> MarkConversationAsReadError.GroupNotFound(groupId)
                GetError.ConnectionError -> MarkConversationAsReadError.InternalError
            }.asFailure()
        }

        val groupMemberIds = groupMemberRepository.getMembers(groupId)
            .getOrElse { return Failure(MarkConversationAsReadError.InternalError) }

        if (actorId !in groupMemberIds)
            return Failure(MarkConversationAsReadError.Unauthorized(actorId))

        val unreadMessageIds =
            messageRepository.getUnreadMessages(groupId)
                .getOrElse { return Failure(MarkConversationAsReadError.InternalError) }

        val unreadMessages = unreadMessageIds.map {
            messageRepository.get(it).getOrElse { error ->
                when (error) {
                    GetError.NotFound -> null
                    GetError.ConnectionError ->
                        return Failure(MarkConversationAsReadError.InternalError)
                }
            }
        }.filterNotNull()

        if (unreadMessages.isEmpty()) return Failure(MarkConversationAsReadError.AlreadyRead)

        return transaction {
            for (message in unreadMessages) {
                messageRepository.update(message.copy(delivery = Delivery.Read)).register()
                    .onFailure { error ->
                        when (error) {
                            UpdateError.NotFound -> {}
                            UpdateError.ConnectionError ->
                                return@transaction Failure(MarkConversationAsReadError.InternalError)
                        }
                    }
            }

            unitSuccess()
        }
    }
}
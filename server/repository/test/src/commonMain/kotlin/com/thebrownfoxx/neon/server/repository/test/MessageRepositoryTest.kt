//package com.thebrownfoxx.neon.server.repository.test
//
//import com.thebrownfoxx.neon.common.type.Failure
//import com.thebrownfoxx.neon.common.type.id.GroupId
//import com.thebrownfoxx.neon.common.type.id.MemberId
//import com.thebrownfoxx.neon.common.type.id.MessageId
//import com.thebrownfoxx.neon.common.type.Success
//import com.thebrownfoxx.neon.common.type.UnitSuccess
//import com.thebrownfoxx.neon.must.mustBe
//import com.thebrownfoxx.neon.must.mustBeA
//import com.thebrownfoxx.neon.server.model.ChatGroup
//import com.thebrownfoxx.neon.server.model.Delivery
//import com.thebrownfoxx.neon.server.model.Message
//import com.thebrownfoxx.neon.server.repository.GroupMemberRepository
//import com.thebrownfoxx.neon.server.repository.MessageRepository
//import com.thebrownfoxx.neon.server.repository.message.model.RepositoryAddMessageError
//import com.thebrownfoxx.neon.server.repository.message.model.RepositoryGetMessageError
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.test.runTest
//import kotlinx.datetime.Instant
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//
//abstract class MessageRepositoryTest {
//    // TODO: Test the flows for changes
//
//    private val memberXId = MemberId()
//
//    private val groupX = ChatGroup()
//
//    private val initialMessages = listOf(
//        Message(
//            id = MessageId(),
//            groupId = groupX.id,
//            senderId = memberXId,
//            content = "Hello, world!",
//            timestamp = Instant.fromEpochSeconds(1),
//            delivery = Delivery.Delivered,
//        ),
//        Message(
//            id = MessageId(),
//            groupId = groupX.id,
//            senderId = MemberId(),
//            content = "How are you?",
//            timestamp = Instant.fromEpochSeconds(2),
//            delivery = Delivery.Delivered,
//        ),
//        Message(
//            id = MessageId(),
//            groupId = GroupId(),
//            senderId = MemberId(),
//            content = "./.",
//            timestamp = Instant.fromEpochSeconds(2),
//            delivery = Delivery.Read,
//        ),
//    )
//
//    private lateinit var groupMemberRepository: GroupMemberRepository
//    private lateinit var messageRepository: MessageRepository
//
//    abstract fun createRepositories(): Pair<GroupMemberRepository, MessageRepository>
//
//    @BeforeTest
//    fun setup() {
//        runTest {
//            val (groupMemberRepository, messageRepository) = createRepositories()
//
//            this@MessageRepositoryTest.groupMemberRepository = groupMemberRepository
//            this@MessageRepositoryTest.messageRepository = messageRepository
//
//            groupMemberRepository.addMember(groupX.id, memberXId)
//
//            for (initialMessage in initialMessages) {
//                messageRepository.add(initialMessage)
//            }
//        }
//    }
//
//    @Test
//    fun getShouldReturnMessage() {
//        runTest {
//            for (expectedMessage in initialMessages) {
//                val actualMessageOutcome = messageRepository.get(expectedMessage.id).first()
//                actualMessageOutcome mustBe Success(expectedMessage)
//            }
//        }
//    }
//
//    @Test
//    fun getShouldReturnNotFoundIfMessageDoesNotExist() {
//        runTest {
//            val actualMessageOutcome = messageRepository.get(MessageId()).first()
//            actualMessageOutcome mustBe Failure(RepositoryGetMessageError.NotFound)
//        }
//    }
//
//    @Test
//    fun addShouldAddMessage() {
//        runTest {
//            val expectedMessage = Message(
//                id = MessageId(),
//                groupId = GroupId(),
//                senderId = MemberId(),
//                content = "New message",
//                timestamp = Instant.fromEpochSeconds(3),
//                delivery = Delivery.Read,
//            )
//
//            val addOutcome = messageRepository.add(expectedMessage)
//            addOutcome.mustBeA<UnitSuccess>()
//
//            val actualMessageOutcome = messageRepository.get(expectedMessage.id).first()
//            actualMessageOutcome mustBe Success(expectedMessage)
//        }
//    }
//
//    @Test
//    fun addShouldReturnDuplicateIdIfMessageIdAlreadyExists() {
//        runTest {
//            val duplicateMessage = initialMessages[0].copy()
//
//            val actualAddOutcome = messageRepository.add(duplicateMessage)
//            actualAddOutcome mustBe Failure(RepositoryAddMessageError.DuplicateId)
//        }
//    }
//
//    @Test
//    fun getConversationsShouldReturnConversations() {
//        runTest {
//            val expectedConversations = initialMessages
//                .map { it.groupId }
//                .filter { it == groupX.id }
//                .toSet()
//            val actualConversationsOutcome = messageRepository.getConversations(
//                memberId = memberXId,
//                count = 10,
//                offset = 0,
//                read = false,
//            ).first()
//            actualConversationsOutcome mustBe Success(expectedConversations)
//        }
//    }
//
//    @Test
//    fun getConversationsShouldReturnEmptySetIfNoConversations() {
//        runTest {
//            val expectedConversations = emptySet<GroupId>()
//            val actualConversationsOutcome = messageRepository.getConversations(
//                memberId = MemberId(),
//                count = 10,
//                offset = 0,
//                read = false,
//            ).first()
//            actualConversationsOutcome mustBe Success(expectedConversations)
//        }
//    }
//
//    @Test
//    fun getChatPreviewshouldReturnPreview() {
//        runTest {
//            val expectedPreview = initialMessages
//                .sortedByDescending { it.timestamp }
//                .first { it.groupId == groupX.id }
//                .id
//            val actualPreviewOutcome = messageRepository.getConversationPreview(groupX.id).first()
//            actualPreviewOutcome mustBe Success(expectedPreview)
//        }
//    }
//
//    @Test
//    fun getChatPreviewshouldReturnNotFoundIfNoPreview() {
//        runTest {
//            val actualPreviewOutcome = messageRepository.getConversationPreview(GroupId()).first()
////            actualPreviewOutcome mustBe Failure(RepositoryGetConversationPreviewError.NotFound)
//        }
//    }
//
//    @Test
//    fun getMessagesShouldReturnMessages() {
//        runTest {
//            val expectedMessages = initialMessages
//                .filter { it.groupId == groupX.id }
//                .map { it.id }
//                .toSet()
//            val actualMessagesOutcome = messageRepository.getMessages(
//                groupId = groupX.id,
//                count = 10,
//                offset = 0,
//            ).first()
//            actualMessagesOutcome mustBe Success(expectedMessages)
//        }
//    }
//
//    @Test
//    fun getMessagesShouldReturnEmptySetIfNoMessages() {
//        runTest {
//            val expectedMessages = emptySet<MessageId>()
//            val actualMessagesOutcome = messageRepository.getMessages(
//                groupId = GroupId(),
//                count = 10,
//                offset = 0,
//            ).first()
//            actualMessagesOutcome mustBe Success(expectedMessages)
//        }
//    }
//}
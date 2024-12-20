package com.thebrownfoxx.neon.server.repository.exposed

import com.thebrownfoxx.neon.common.data.AddError
import com.thebrownfoxx.neon.common.data.DataOperationError
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.data.UpdateError
import com.thebrownfoxx.neon.common.data.exposed.ExposedDataSource
import com.thebrownfoxx.neon.common.data.exposed.dataTransaction
import com.thebrownfoxx.neon.common.data.exposed.firstOrNotFound
import com.thebrownfoxx.neon.common.data.exposed.mapAddTransaction
import com.thebrownfoxx.neon.common.data.exposed.mapGetTransaction
import com.thebrownfoxx.neon.common.data.exposed.mapOperationTransaction
import com.thebrownfoxx.neon.common.data.exposed.mapUpdateTransaction
import com.thebrownfoxx.neon.common.data.exposed.toCommonUuid
import com.thebrownfoxx.neon.common.data.exposed.toJavaUuid
import com.thebrownfoxx.neon.common.data.exposed.tryAdd
import com.thebrownfoxx.neon.common.data.exposed.tryUpdate
import com.thebrownfoxx.neon.common.data.transaction.ReversibleUnitOutcome
import com.thebrownfoxx.neon.common.data.transaction.asReversible
import com.thebrownfoxx.neon.common.type.id.GroupId
import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.neon.common.type.id.MessageId
import com.thebrownfoxx.neon.server.model.Delivery
import com.thebrownfoxx.neon.server.model.Message
import com.thebrownfoxx.neon.server.repository.MessageRepository
import com.thebrownfoxx.outcome.Outcome
import com.thebrownfoxx.outcome.getOrElse
import com.thebrownfoxx.outcome.map
import com.thebrownfoxx.outcome.mapError
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll

class ExposedMessageRepository(
    database: Database,
) : MessageRepository, ExposedDataSource(database, MessageTable) {
    private val reactiveConversationPreviewsCache = ReactiveCache(::getConversationPreviews)
    private val reactiveMessageCache = ReactiveCache(::get)

    override fun getConversationPreviewsAsFlow(memberId: MemberId): Flow<Outcome<List<Message>, DataOperationError>> =
        reactiveConversationPreviewsCache.getAsFlow(memberId)

    override fun getAsFlow(id: MessageId) = reactiveMessageCache.getAsFlow(id)

    override suspend fun get(id: MessageId): Outcome<Message, GetError> {
        return dataTransaction {
            MessageTable
                .selectAll()
                .where(MessageTable.id eq id.toJavaUuid())
                .firstOrNotFound()
                .map { it.toMessage() }
        }.mapGetTransaction()
    }

    override suspend fun add(message: Message): ReversibleUnitOutcome<AddError> {
        return dataTransaction {
            MessageTable.tryAdd() {
                it[id] = message.id.toJavaUuid()
                it[groupId] = message.groupId.toJavaUuid()
                it[senderId] = message.senderId.toJavaUuid()
                it[content] = message.content
                it[timestamp] = message.timestamp
                it[delivery] = message.delivery.name
            }
        }
            .mapAddTransaction()
            .asReversible {
                dataTransaction { MessageTable.deleteWhere { id eq message.id.toJavaUuid() } }
            }
    }

    override suspend fun update(message: Message): ReversibleUnitOutcome<UpdateError> {
        val oldMessage = get(message.id)
            .getOrElse { return mapError(error.toUpdateError()).asReversible() }

        return dataTransaction { updateMessage(message) }
            .mapUpdateTransaction()
            .asReversible { dataTransaction { updateMessage(oldMessage) } }
    }

    private fun updateMessage(message: Message) = MessageTable.tryUpdate {
        it[id] = message.id.toJavaUuid()
        it[groupId] = message.groupId.toJavaUuid()
        it[senderId] = message.senderId.toJavaUuid()
        it[content] = message.content
        it[timestamp] = message.timestamp
        it[delivery] = message.delivery.name
    }

    override suspend fun getMessages(
        groupId: GroupId,
        count: Int,
        offset: Int,
    ): Outcome<Set<MessageId>, DataOperationError> {
        TODO("Not yet implemented")
    }

    override suspend fun getUnreadMessages(groupId: GroupId): Outcome<Set<MessageId>, DataOperationError> {
        TODO("Not yet implemented")
    }

    private suspend fun getConversationPreviews(
        memberId: MemberId,
    ): Outcome<List<Message>, DataOperationError> {
        return dataTransaction {
            val groupId = MessageTable.groupId.alias("group_id")
            val maxTimestamp = MessageTable.timestamp.max().alias("max_timestamp")

            val conversations = MessageTable
                .join(
                    GroupMemberTable,
                    JoinType.INNER,
                    onColumn = MessageTable.groupId,
                    otherColumn = GroupMemberTable.groupId,
                )
                .select(groupId, maxTimestamp)
                .groupBy(MessageTable.groupId)
                .where(GroupMemberTable.memberId eq memberId.toJavaUuid())
                .alias("conversations")

            MessageTable
                .join(
                    conversations,
                    JoinType.INNER,
                ) {
                    (MessageTable.groupId eq conversations[groupId]) and
                            (MessageTable.timestamp eq conversations[maxTimestamp])
                }.selectAll()
                .map { it.toMessage() }
        }.mapOperationTransaction()
    }

    private fun ResultRow.toMessage() = Message(
        id = MessageId(this[MessageTable.id].toCommonUuid()),
        groupId = GroupId(this[MessageTable.groupId].toCommonUuid()),
        senderId = MemberId(this[MessageTable.senderId].toCommonUuid()),
        content = this[MessageTable.content],
        timestamp = this[MessageTable.timestamp],
        delivery = Delivery.valueOf(this[MessageTable.delivery])
    )
}

private object MessageTable : Table("message") {
    val id = uuid("id")
    val groupId = uuid("group_id")
    val senderId = uuid("sender_id")
    val content = text("content")
    val timestamp = timestamp("timestamp")

    // We could make like a Postgres mapping but it's too complicated and bound to Postgres
    // (at least with Exposed's current implementation)
    // So I'm just gonna save it as a string and parse it later
    val delivery = varchar("delivery", 32)
}
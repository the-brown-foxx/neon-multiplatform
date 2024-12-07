package com.thebrownfoxx.neon.server.repository.exposed

import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.data.exposed.ExposedDataSource
import com.thebrownfoxx.neon.common.data.exposed.dataTransaction
import com.thebrownfoxx.neon.common.data.exposed.firstOrNotFound
import com.thebrownfoxx.neon.common.data.exposed.mapGetTransaction
import com.thebrownfoxx.neon.common.data.exposed.toCommonUuid
import com.thebrownfoxx.neon.common.data.exposed.toJavaUuid
import com.thebrownfoxx.neon.common.data.transaction.ReversibleUnitOutcome
import com.thebrownfoxx.neon.common.data.transaction.asReversible
import com.thebrownfoxx.neon.common.outcome.Failure
import com.thebrownfoxx.neon.common.outcome.Outcome
import com.thebrownfoxx.neon.common.outcome.fold
import com.thebrownfoxx.neon.common.outcome.map
import com.thebrownfoxx.neon.common.outcome.unitSuccess
import com.thebrownfoxx.neon.common.type.id.GroupId
import com.thebrownfoxx.neon.server.repository.InviteCode
import com.thebrownfoxx.neon.server.repository.InviteCodeRepository
import com.thebrownfoxx.neon.server.repository.RepositorySetInviteCodeError
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import java.util.UUID

class ExposedInviteCodeRepository(
    database: Database,
) : InviteCodeRepository, ExposedDataSource(database, InviteCodeTable) {
    private val reactiveCache = ReactiveCache(::get)

    override fun getAsFlow(groupId: GroupId) = reactiveCache.getAsFlow(groupId)

    override suspend fun getGroup(inviteCode: String): Outcome<GroupId, GetError> {
        return dataTransaction {
            InviteCodeTable
                .selectAll()
                .where(InviteCodeTable.inviteCode eq inviteCode)
                .firstOrNotFound()
                .map { GroupId(it[InviteCodeTable.groupId].toCommonUuid()) }
        }.mapGetTransaction()
    }

    override suspend fun set(
        groupId: GroupId,
        inviteCode: String,
    ): ReversibleUnitOutcome<RepositorySetInviteCodeError> {
        val exists = getGroup(inviteCode).fold(
            onSuccess = { true },
            onFailure = { error ->
                when (error) {
                    GetError.NotFound -> false
                    GetError.ConnectionError ->
                        return Failure(RepositorySetInviteCodeError.ConnectionError).asReversible()
                }
            }
        )
        if (exists) return Failure(RepositorySetInviteCodeError.DuplicateInviteCode).asReversible()

        val id = UUID.randomUUID()
        dataTransaction {
            InviteCodeTable.upsert {
                it[this.id] = id
                it[this.groupId] = groupId.toJavaUuid()
                it[this.inviteCode] = inviteCode
            }
        }
        return unitSuccess().asReversible {
            dataTransaction { InviteCodeTable.deleteWhere { InviteCodeTable.id eq id } }
        }
    }

    private suspend fun get(groupId: GroupId): Outcome<InviteCode, GetError> {
        return dataTransaction {
            InviteCodeTable
                .selectAll()
                .where(InviteCodeTable.groupId eq groupId.toJavaUuid())
                .firstOrNotFound()
                .map { it[InviteCodeTable.inviteCode] }
        }.mapGetTransaction()
    }
}

private object InviteCodeTable : Table("invite_code") {
    val id = uuid("id")
    val groupId = uuid("group_id").uniqueIndex()
    val inviteCode = varchar("invite_code", length = 15).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}
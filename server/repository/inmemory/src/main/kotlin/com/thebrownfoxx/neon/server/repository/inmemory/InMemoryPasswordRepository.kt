package com.thebrownfoxx.neon.server.repository.inmemory

import com.thebrownfoxx.neon.common.data.ConnectionError
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.data.transaction.ReversibleUnitOutcome
import com.thebrownfoxx.neon.common.data.transaction.asReversible
import com.thebrownfoxx.neon.common.hash.Hash
import com.thebrownfoxx.neon.common.outcome.Failure
import com.thebrownfoxx.neon.common.outcome.Outcome
import com.thebrownfoxx.neon.common.outcome.Success
import com.thebrownfoxx.neon.common.outcome.unitSuccess
import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.neon.server.repository.PasswordRepository

class InMemoryPasswordRepository : PasswordRepository {
    private val passwordHashes = mutableMapOf<MemberId, Hash>()

    override suspend fun getHash(memberId: MemberId): Outcome<Hash, GetError> {
        return when (val passwordHash = passwordHashes[memberId]) {
            null -> Failure(GetError.NotFound)
            else -> Success(passwordHash)
        }
    }

    override suspend fun setHash(memberId: MemberId, hash: Hash): ReversibleUnitOutcome<ConnectionError> {
        passwordHashes[memberId] = hash
        return unitSuccess().asReversible { passwordHashes.remove(memberId) }
    }
}
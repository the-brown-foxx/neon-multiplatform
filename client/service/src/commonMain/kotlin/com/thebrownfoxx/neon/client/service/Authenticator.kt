package com.thebrownfoxx.neon.client.service

import com.thebrownfoxx.neon.common.type.id.MemberId
import com.thebrownfoxx.outcome.UnitOutcome
import kotlinx.coroutines.flow.StateFlow

interface Authenticator {
    val loggedIn: StateFlow<Boolean>
    val loggedInMember: StateFlow<MemberId?>
    suspend fun login(username: String, password: String): UnitOutcome<LoginError>
    suspend fun logout(): UnitOutcome<LogoutError>

    enum class LoginError {
        InvalidCredentials,
        ConnectionError,
        UnknownError,
    }

    enum class LogoutError {
        ConnectionError,
        UnknownError,
    }
}
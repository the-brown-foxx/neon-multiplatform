package com.thebrownfoxx.neon.common.extension

import com.thebrownfoxx.outcome.Outcome
import com.thebrownfoxx.outcome.mapError
import com.thebrownfoxx.outcome.runFailing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.contracts.ExperimentalContracts
import kotlin.time.Duration

@OptIn(ExperimentalContracts::class)
suspend fun <T> withTimeout(
    timeout: Duration,
    block: suspend CoroutineScope.() -> T,
): Outcome<T, TimeoutError> {
    return runFailing { withTimeout(timeout.inWholeMilliseconds, block) }
        .mapError { TimeoutError }
}

data object TimeoutError
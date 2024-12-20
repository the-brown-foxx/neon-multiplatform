package com.thebrownfoxx.neon.common.data.exposed

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Deprecated(
    "Use dataTransaction instead",
    ReplaceWith("dataTransaction(block)"),
)
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
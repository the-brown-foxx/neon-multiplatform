package com.thebrownfoxx.neon.client.repository.offlinefirst

import com.thebrownfoxx.neon.client.converter.toLocalGroup
import com.thebrownfoxx.neon.client.model.LocalGroup
import com.thebrownfoxx.neon.client.repository.GroupRepository
import com.thebrownfoxx.neon.client.repository.local.LocalGroupDataSource
import com.thebrownfoxx.neon.client.repository.remote.GetGroupError
import com.thebrownfoxx.neon.client.repository.remote.RemoteGroupDataSource
import com.thebrownfoxx.neon.common.data.GetError
import com.thebrownfoxx.neon.common.outcome.Failure
import com.thebrownfoxx.neon.common.outcome.Outcome
import com.thebrownfoxx.neon.common.outcome.Success
import com.thebrownfoxx.neon.common.outcome.getOrElse
import com.thebrownfoxx.neon.common.type.id.GroupId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class OfflineFirstGroupRepository(
    private val localDataSource: LocalGroupDataSource,
    private val remoteDataSource: RemoteGroupDataSource,
) : GroupRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

    override fun get(id: GroupId): Flow<Outcome<LocalGroup, GetError>> {
        val sharedFlow = MutableSharedFlow<Outcome<LocalGroup, GetError>>(replay = 1)

        coroutineScope.launch {
            localDataSource.getAsFlow(id).collect { localGroupOutcome ->
                val localGroup = localGroupOutcome.getOrElse { error ->
                    when (error) {
                        GetError.NotFound -> {}
                        GetError.ConnectionError ->
                            sharedFlow.emit(Failure(GetError.ConnectionError))
                    }
                    return@collect
                }
                sharedFlow.emit(Success(localGroup))
            }
        }

        coroutineScope.launch {
            remoteDataSource.getAsFlow(id).collect { remoteGroupOutcome ->
                val remoteGroup = remoteGroupOutcome.getOrElse { error ->
                    val mappedError = when (error) {
                        GetGroupError.NotFound -> GetError.NotFound
                        GetGroupError.ServerError -> GetError.ConnectionError // TODO: Fix errors T-T
                    }
                    sharedFlow.emit(Failure(mappedError))
                    return@collect
                }
                localDataSource.upsert(remoteGroup.toLocalGroup())
            }
        }

        return sharedFlow.asSharedFlow()
    }
}
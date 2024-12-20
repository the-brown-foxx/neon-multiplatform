package com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.state

import com.thebrownfoxx.neon.client.application.ui.component.avatar.state.AvatarState
import com.thebrownfoxx.neon.common.type.id.GroupId

data class ChatPreviewState(
    val groupId: GroupId = GroupId(),
    val avatar: AvatarState?,
    val name: String?,
    val content: ChatPreviewContentState?,
    val emphasized: Boolean = false,
)
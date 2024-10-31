package com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.thebrownfoxx.neon.client.application.ui.component.avatar.state.GroupAvatarState
import com.thebrownfoxx.neon.client.application.ui.component.avatar.state.SingleAvatarState
import com.thebrownfoxx.neon.client.application.ui.component.delivery.state.DeliveryState
import com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.state.ChatPreviewState
import com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.state.ChatPreviewContentState
import com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.state.ReceivedCommunityState
import com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.state.ReceivedDirectState
import com.thebrownfoxx.neon.client.application.ui.screen.chat.previews.state.SentState
import com.thebrownfoxx.neon.client.application.ui.theme.NeonTheme
import com.thebrownfoxx.neon.common.extension.toLocalDateTime
import kotlinx.datetime.Clock

@Preview
@Composable
private fun SentDirectPreview() {
    NeonTheme {
        ChatPreview(
            chatPreview = ChatPreviewState(
                avatar = SingleAvatarState(url = null, placeholder = "carlito"),
                name = "carlito",
                content = ChatPreviewContentState(
                    message = "i'm ready 😉",
                    timestamp = Clock.System.now().toLocalDateTime(),
                    delivery = DeliveryState.Delivered,
                    senderState = SentState,
                ),
            ),
            read = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun ReceivedDirectPreview() {
    NeonTheme {
        ChatPreview(
            chatPreview = ChatPreviewState(
                avatar = SingleAvatarState(url = null, placeholder = "carlito"),
                name = "carlito",
                content = ChatPreviewContentState(
                    message = "i'm ready 😉",
                    timestamp = Clock.System.now().toLocalDateTime(),
                    delivery = null,
                    senderState = ReceivedDirectState,
                ),
            ),
            read = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SentCommunityPreview() {
    NeonTheme {
        ChatPreview(
            chatPreview = ChatPreviewState(
                avatar = GroupAvatarState(
                    front = SingleAvatarState(url = null, placeholder = "SharlLeclaire"),
                    back = SingleAvatarState(url = null, placeholder = "little_lando"),
                ),
                name = "carlito",
                content = ChatPreviewContentState(
                    message = "yall ready bois?",
                    timestamp = Clock.System.now().toLocalDateTime(),
                    delivery = DeliveryState.Sent,
                    senderState = SentState,
                ),
            ),
            read = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun ReceivedCommunityPreview() {
    NeonTheme {
        ChatPreview(
            chatPreview = ChatPreviewState(
                avatar = GroupAvatarState(
                    front = SingleAvatarState(url = null, placeholder = "SharlLeclaire"),
                    back = SingleAvatarState(url = null, placeholder = "carlito"),
                ),
                name = "carlito",
                content = ChatPreviewContentState(
                    message = "yall ready?",
                    timestamp = Clock.System.now().toLocalDateTime(),
                    delivery = null,
                    senderState = ReceivedCommunityState(
                        senderAvatar = SingleAvatarState(url = null, placeholder = "carlito"),
                    ),
                ),
            ),
            read = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun UnreadPreview() {
    NeonTheme {
        ChatPreview(
            chatPreview = ChatPreviewState(
                avatar = SingleAvatarState(url = null, placeholder = "SharlLeclerc"),
                name = "SharlLeclerc",
                content = ChatPreviewContentState(
                    message = "is that 🕳️ ready?",
                    timestamp = Clock.System.now().toLocalDateTime(),
                    delivery = null,
                    senderState = ReceivedDirectState,
                ),
            ),
            read = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
    }
}
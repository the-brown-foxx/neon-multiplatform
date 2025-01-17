package com.thebrownfoxx.neon.client.application.ui.screen.login.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginScreenEventHandler
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginScreenState
import com.thebrownfoxx.neon.client.application.ui.theme.NeonTheme

@Preview
@Composable
private fun Preview() {
    NeonTheme {
        LoginScreenContent(
            state = LoginScreenState(),
            eventHandler = LoginScreenEventHandler.Blank,
            modifier = Modifier.padding(16.dp),
        )
    }
}
package com.thebrownfoxx.neon.client.application.ui.screen.login.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CloudOff
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thebrownfoxx.neon.client.application.ui.component.InformationCard
import com.thebrownfoxx.neon.client.application.ui.component.InformationCardIconText
import com.thebrownfoxx.neon.client.application.ui.component.InformationCardProgressIndicator
import com.thebrownfoxx.neon.client.application.ui.component.common.AnimatedVisibility
import com.thebrownfoxx.neon.client.application.ui.component.common.ExpandAxis
import com.thebrownfoxx.neon.client.application.ui.extension.padding
import com.thebrownfoxx.neon.client.application.ui.extension.rememberCache
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginState
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginState.ConnectionError
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginState.CredentialsIncorrect
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginState.CredentialsMissing
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginState.Idle
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.LoginState.LoggingIn
import com.thebrownfoxx.neon.client.application.ui.screen.login.state.MissingCredential
import neon.client.application.generated.resources.Res
import neon.client.application.generated.resources.connection_error
import neon.client.application.generated.resources.connection_error_message
import neon.client.application.generated.resources.credentials_incorrect_message
import neon.client.application.generated.resources.credentials_missing_message
import neon.client.application.generated.resources.error
import neon.client.application.generated.resources.logging_in_message
import neon.client.application.generated.resources.password_missing_message
import neon.client.application.generated.resources.unknown_error
import neon.client.application.generated.resources.unknown_error_message
import neon.client.application.generated.resources.username_missing_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginStateIndicator(
    state: LoginState,
    modifier: Modifier = Modifier,
    padding: PaddingValues = 0.dp.padding,
) {
    val cachedStatus = rememberCache(target = state) { state != Idle }

    AnimatedVisibility(
        visible = state != Idle,
        modifier = modifier,
        expandAxis = ExpandAxis.Vertical,
    ) {
        if (cachedStatus != Idle && cachedStatus != null) {
            val containerColor by animateColorAsState(
                targetValue = when (state) {
                    LoggingIn, is CredentialsMissing ->
                        MaterialTheme.colorScheme.surfaceContainer

                    else -> MaterialTheme.colorScheme.errorContainer
                },
                label = "containerColor",
            )

            val contentColor by animateColorAsState(
                targetValue = when (state) {
                    LoggingIn -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                },
                label = "contentColor",
            )

            InformationCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                progressIndicator = {
                    InformationCardProgressIndicator(visible = state == LoggingIn) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                containerColor = containerColor,
                contentColor = contentColor,
            ) {
                AnimatedContent(
                    targetState = cachedStatus,
                    label = "statusContent",
                ) { targetStatus ->
                    Content(targetStatus)
                }
            }
        }
    }
}

@Composable
private fun Content(targetStatus: LoginState) {
    Box(modifier = Modifier.fillMaxWidth()) {
        when (targetStatus) {
            Idle -> error("This should never happen")
            is CredentialsMissing -> CredentialsMissingContent(targetStatus.missingCredential)
            LoggingIn -> LoggingInContent()
            CredentialsIncorrect -> CredentialsIncorrectContent()
            ConnectionError -> ConnectionErrorContent()
            LoginState.UnknownError -> UnknownErrorContent()
        }
    }
}

@Composable
private fun CredentialsMissingContent(missingCredential: MissingCredential) {
    val text = when (missingCredential) {
        MissingCredential.Both ->
            stringResource(Res.string.credentials_missing_message)

        MissingCredential.Username ->
            stringResource(Res.string.username_missing_message)

        MissingCredential.Password ->
            stringResource(Res.string.password_missing_message)
    }
    Text(text = text)
}

@Composable
private fun LoggingInContent() {
    Text(text = stringResource(Res.string.logging_in_message))
}

@Composable
private fun CredentialsIncorrectContent() {
    InformationCardIconText(
        icon = Icons.TwoTone.Warning,
        iconContentDescription = stringResource(Res.string.error),
        text = stringResource(Res.string.credentials_incorrect_message),
    )
}

@Composable
private fun ConnectionErrorContent() {
    InformationCardIconText(
        icon = Icons.TwoTone.CloudOff,
        iconContentDescription = stringResource(Res.string.connection_error),
        text = stringResource(Res.string.connection_error_message),
    )
}

@Composable
private fun UnknownErrorContent() {
    InformationCardIconText(
        icon = Icons.TwoTone.Error,
        iconContentDescription = stringResource(Res.string.unknown_error),
        text = stringResource(Res.string.unknown_error_message),
    )
}

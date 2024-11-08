package com.thebrownfoxx.neon.client.application.ui.component.loader

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thebrownfoxx.neon.client.application.ui.extension.loaderContentTransition
import com.thebrownfoxx.neon.common.type.Loadable
import com.thebrownfoxx.neon.common.type.Loaded
import com.thebrownfoxx.neon.common.type.Loading

@Composable
fun <T> AnimatedLoadableContent(
    targetState: Loadable<T>,
    loader: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        label = "loaderContent",
        contentKey = { it::class },
        transitionSpec = { loaderContentTransition() },
        modifier = modifier,
    ) {
        when (it) {
            is Loading -> loader()
            is Loaded -> content(it.value)
        }
    }
}
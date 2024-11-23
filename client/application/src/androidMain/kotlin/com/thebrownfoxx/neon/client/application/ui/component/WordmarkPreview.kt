package com.thebrownfoxx.neon.client.application.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thebrownfoxx.neon.client.application.ui.theme.NeonTheme

@Preview
@Composable
private fun LightPreview() {
    NeonTheme {
        Surface {
            Wordmark(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun DarkPreview() {
    NeonTheme(darkTheme = true) {
        Surface {
            Wordmark(modifier = Modifier.padding(16.dp))
        }
    }
}
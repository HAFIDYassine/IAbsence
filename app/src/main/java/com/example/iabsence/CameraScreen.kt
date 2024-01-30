package com.example.iabsence

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun UseCamera() {
    val context = LocalContext.current
    val intent = remember { Intent(MediaStore.ACTION_IMAGE_CAPTURE) }

    LaunchedEffect(Unit) {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}


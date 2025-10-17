package com.example.paradigmaapp.android.utils

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

private const val ARTWORK_LOG_TAG = "ArtworkLoader"

@Composable
fun rememberCoilImageRequest(
	primaryData: Any?,
	fallbackData: Any? = null,
	debugLabel: String? = null
): ImageRequest {
	val context = LocalContext.current
	return remember(context, primaryData, fallbackData, debugLabel) {
		ImageRequest.Builder(context)
			.data(primaryData ?: fallbackData)
			.crossfade(true)
			.allowHardware(false)
			.bitmapConfig(Bitmap.Config.ARGB_8888)
			.listener(
				onError = { _, result ->
					if (fallbackData != null && primaryData != fallbackData) {
						Log.w(ARTWORK_LOG_TAG, "Primary image failed for $debugLabel, falling back", result.throwable)
					} else if (debugLabel != null) {
						Log.w(ARTWORK_LOG_TAG, "Image load failed for $debugLabel", result.throwable)
					}
				}
			)
			.build()
	}
}

package snd.komelia.ui.settings.imagereader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import snd.komelia.ui.LocalPlatform
import snd.komelia.ui.common.components.AppSliderDefaults
import snd.komelia.ui.common.components.SwitchWithLabel
import snd.komelia.ui.platform.PlatformType
import snd.komelia.ui.settings.imagereader.onnxruntime.OnnxRuntimeSettingsContent
import snd.komelia.ui.settings.imagereader.onnxruntime.OnnxRuntimeSettingsState
import snd.komelia.ui.settings.imagereader.onnxruntime.isOnnxRuntimeSupported
import kotlin.math.roundToInt

@Composable
fun ImageReaderSettingsContent(
    loadThumbnailPreviews: Boolean,
    onLoadThumbnailPreviewsChange: (Boolean) -> Unit,

    volumeKeysNavigation: Boolean,
    onVolumeKeysNavigationChange: (Boolean) -> Unit,

    prefetchSpreadCount: Int,
    onPrefetchSpreadCountChange: (Int) -> Unit,

    imageCacheSize: Int,
    onImageCacheSizeChange: (Int) -> Unit,

    onCacheClear: () -> Unit,
    onnxRuntimeSettingsState: OnnxRuntimeSettingsState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val platform = LocalPlatform.current
        SwitchWithLabel(
            checked = loadThumbnailPreviews,
            onCheckedChange = onLoadThumbnailPreviewsChange,
            label = { Text("Load small previews when dragging navigation slider") },
            supportingText = { Text("can be slow for high resolution images") },
        )

        if (platform == PlatformType.MOBILE) {
            SwitchWithLabel(
                checked = volumeKeysNavigation,
                onCheckedChange = onVolumeKeysNavigationChange,
                label = { Text("Volume keys navigation") },
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 5.dp))
        Text("Page Prefetch & Cache", style = MaterialTheme.typography.titleSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(120.dp)) {
                Text("Prefetch Spreads", style = MaterialTheme.typography.labelLarge)
                Text("$prefetchSpreadCount", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = prefetchSpreadCount.toFloat(),
                onValueChange = { onPrefetchSpreadCountChange(it.roundToInt()) },
                steps = 8,
                valueRange = 1f..10f,
                colors = AppSliderDefaults.colors()
            )
        }
        Text(
            "Number of spreads loaded ahead/behind current page. Higher = faster page turns but more memory.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(120.dp)) {
                Text("Image Cache Size", style = MaterialTheme.typography.labelLarge)
                Text("$imageCacheSize", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = imageCacheSize.toFloat(),
                onValueChange = { onImageCacheSizeChange(it.roundToInt()) },
                steps = 8,
                valueRange = 10f..100f,
                colors = AppSliderDefaults.colors()
            )
        }
        Text(
            "Maximum decoded page images kept in memory. Should be at least (Prefetch x 2 + 1).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(Modifier.padding(vertical = 5.dp))

        FilledTonalButton(
            onClick = onCacheClear,
        ) { Text("Clear image cache") }

        if (isOnnxRuntimeSupported()) {
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            OnnxRuntimeSettingsContent(
                executionProvider = onnxRuntimeSettingsState.currentExecutionProvider,
                availableDevices = onnxRuntimeSettingsState.availableDevices,
                deviceId = onnxRuntimeSettingsState.deviceId.collectAsState().value,
                onDeviceIdChange = onnxRuntimeSettingsState::onDeviceIdChange,
                upscaleMode = onnxRuntimeSettingsState.upscaleMode.collectAsState().value,
                onUpscaleModeChange = onnxRuntimeSettingsState::onUpscaleModeChange,
                upscalerTileSize = onnxRuntimeSettingsState.upscalerTileSize.collectAsState().value,
                onUpscalerTileSizeChange = onnxRuntimeSettingsState::onTileSizeChange,
                upscaleModelPath = onnxRuntimeSettingsState.upscaleModelPath.collectAsState().value,
                onUpscaleModelPathChange = onnxRuntimeSettingsState::onUpscaleModelPathChange,
                onOrtInstall = onnxRuntimeSettingsState::onInstallRequest,
                mangaJaNaiIsInstalled = onnxRuntimeSettingsState.mangaJaNaiIsInstalled.collectAsState().value,
                onMangaJaNaiDownload = onnxRuntimeSettingsState::onMangaJaNaiDownloadRequest,
                panelModelIsDownloaded = onnxRuntimeSettingsState.panelModelIsDownloaded.collectAsState().value,
                onPanelDetectionModelDownloadRequest = onnxRuntimeSettingsState::onPanelDetectionModelDownloadRequest
            )
        }
    }
}

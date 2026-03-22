package snd.komelia.ui.reader.image.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import snd.komelia.ui.common.components.AnimatedDropdownMenu
import snd.komelia.settings.model.ReaderTapNavigationMode
import snd.komelia.settings.model.ReaderType.CONTINUOUS
import snd.komelia.settings.model.ReaderType.PAGED
import snd.komelia.settings.model.ReaderType.PANELS
import snd.komelia.ui.LocalPlatform
import snd.komelia.ui.LocalWindowState
import snd.komelia.ui.common.components.LoadingMaxSizeIndicator
import snd.komelia.ui.platform.PlatformType.MOBILE
import snd.komelia.ui.reader.image.ReaderState
import snd.komelia.ui.reader.image.ScreenScaleState
import snd.komelia.ui.reader.image.continuous.ContinuousReaderContent
import snd.komelia.ui.reader.image.continuous.ContinuousReaderState
import snd.komelia.ui.reader.image.paged.PagedReaderContent
import snd.komelia.ui.reader.image.paged.PagedReaderState
import snd.komelia.ui.reader.image.panels.PanelsReaderContent
import snd.komelia.ui.reader.image.panels.PanelsReaderState
import snd.komelia.ui.reader.image.settings.SettingsOverlay
import snd.komelia.ui.settings.imagereader.ncnn.NcnnSettingsState
import snd.komelia.ui.settings.imagereader.onnxruntime.OnnxRuntimeSettingsState

@Composable
fun ReaderContent(
    commonReaderState: ReaderState,
    pagedReaderState: PagedReaderState,
    continuousReaderState: ContinuousReaderState,
    panelsReaderState: PanelsReaderState?,
    onnxRuntimeSettingsState: OnnxRuntimeSettingsState?,
    ncnnSettingsState: NcnnSettingsState,
    screenScaleState: ScreenScaleState,
    readingOffline: StateFlow<Boolean>? = null,

    isColorCorrectionActive: Boolean,
    onColorCorrectionClick: () -> Unit,
    onExit: () -> Unit,
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showImageContextMenu by remember { mutableStateOf(false) }
    var contextMenuAnchorOffset by remember { mutableStateOf(Offset.Zero) }
    val onLongPress: (Offset) -> Unit = { offset ->
        contextMenuAnchorOffset = offset
        showImageContextMenu = true
    }
    if (LocalPlatform.current == MOBILE) {
        val windowState = LocalWindowState.current
        DisposableEffect(showSettingsMenu) {
            if (showSettingsMenu) {
                windowState.setFullscreen(false)
            } else {
                windowState.setFullscreen(true)
            }
            onDispose {
                windowState.setFullscreen(false)
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        screenScaleState.composeScope = coroutineScope
    }
    val density = LocalDensity.current
    LaunchedEffect(density) {
        commonReaderState.pixelDensity.value = density
    }

    val topLevelFocus = remember { FocusRequester() }
    val volumeKeysNavigation = commonReaderState.volumeKeysNavigation.collectAsState().value
    val tapNavigationMode = commonReaderState.tapNavigationMode.collectAsState().value
    var hasFocus by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                screenScaleState.setAreaSize(it)
            }
            .focusable()
            .focusRequester(topLevelFocus)
            .onFocusChanged { hasFocus = it.hasFocus }
            .onKeyEvent { event ->
                if (event.type != KeyUp) return@onKeyEvent false

                var consumed = true
                when (event.key) {
                    Key.M -> showSettingsMenu = !showSettingsMenu
                    Key.Escape -> showSettingsMenu = false
                    Key.H -> showHelpDialog = true
                    Key.DirectionLeft -> if (event.isAltPressed) onExit() else consumed = false
                    Key.Back -> if (showSettingsMenu) showSettingsMenu = false else onExit()
                    Key.U -> commonReaderState.onStretchToFitCycle()
                    Key.C -> if (event.isAltPressed) commonReaderState.onColorCorrectionDisable() else consumed = false
                    else -> consumed = false
                }
                consumed
            }
    ) {
        val areaSize = screenScaleState.areaSize.collectAsState()
        if (areaSize.value == IntSize.Zero) {
            LoadingMaxSizeIndicator()
            return
        }

        when (commonReaderState.readerType.collectAsState().value) {
            PAGED -> {
                PagedReaderContent(
                    showHelpDialog = showHelpDialog,
                    onShowHelpDialogChange = { showHelpDialog = it },
                    showSettingsMenu = showSettingsMenu,
                    onShowSettingsMenuChange = { showSettingsMenu = it },
                    screenScaleState = screenScaleState,
                    pagedReaderState = pagedReaderState,
                    volumeKeysNavigation = volumeKeysNavigation,
                    tapNavigationMode = tapNavigationMode,
                    onLongPress = onLongPress
                )
            }

            CONTINUOUS -> {
                ContinuousReaderContent(
                    showHelpDialog = showHelpDialog,
                    onShowHelpDialogChange = { showHelpDialog = it },
                    showSettingsMenu = showSettingsMenu,
                    onShowSettingsMenuChange = { showSettingsMenu = it },
                    screenScaleState = screenScaleState,
                    continuousReaderState = continuousReaderState,
                    volumeKeysNavigation = volumeKeysNavigation,
                    tapNavigationMode = tapNavigationMode,
                    onLongPress = onLongPress
                )
            }

            PANELS -> {
                check(panelsReaderState != null)
                PanelsReaderContent(
                    showHelpDialog = showHelpDialog,
                    onShowHelpDialogChange = { showHelpDialog = it },
                    showSettingsMenu = showSettingsMenu,
                    onShowSettingsMenuChange = { showSettingsMenu = it },
                    screenScaleState = screenScaleState,
                    panelsReaderState = panelsReaderState,
                    volumeKeysNavigation = volumeKeysNavigation,
                    tapNavigationMode = tapNavigationMode,
                    onLongPress = onLongPress
                )
            }

        }

        Box(
            Modifier.offset {
                IntOffset(contextMenuAnchorOffset.x.toInt(), contextMenuAnchorOffset.y.toInt())
            }
        ) {
            AnimatedDropdownMenu(
                expanded = showImageContextMenu,
                onDismissRequest = { showImageContextMenu = false },
                transformOrigin = TransformOrigin(0f, 0f)
            ) {
                DropdownMenuItem(
                    text = { Text("Save image") },
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    onClick = {
                        showImageContextMenu = false
                        commonReaderState.saveCurrentPageToDownloads()
                    }
                )
            }
        }

        SettingsOverlay(
            show = showSettingsMenu,
            commonReaderState = commonReaderState,
            pagedReaderState = pagedReaderState,
            continuousReaderState = continuousReaderState,
            panelsReaderState = panelsReaderState,
            onnxRuntimeSettingsState = onnxRuntimeSettingsState,
            ncnnSettingsState = ncnnSettingsState,
            screenScaleState = screenScaleState,
            isColorCorrectionsActive = isColorCorrectionActive,
            onColorCorrectionClick = onColorCorrectionClick,
            onBackPress = onExit,
            ohShowHelpDialogChange = { showHelpDialog = it },
        )

        EInkFlashOverlay(
            enabled = commonReaderState.flashOnPageChange.collectAsState().value,
            pageChangeFlow = commonReaderState.pageChangeFlow,
            flashEveryNPages = commonReaderState.flashEveryNPages.collectAsState().value,
            flashWith = commonReaderState.flashWith.collectAsState().value,
            flashDuration = commonReaderState.flashDuration.collectAsState().value
        )

        // Offline reading badge — auto-hides after 3 seconds
        if (readingOffline != null) {
            val isOffline = readingOffline.collectAsState().value
            var showBadge by remember { mutableStateOf(isOffline) }
            LaunchedEffect(isOffline) {
                if (isOffline) {
                    showBadge = true
                    delay(3000)
                    showBadge = false
                }
            }
            AnimatedVisibility(
                visible = showBadge,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 8.dp)
            ) {
                Text(
                    text = "\uD83D\uDCF1 Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
    LaunchedEffect(hasFocus) {
        if (!hasFocus) topLevelFocus.requestFocus()
    }
}

@Composable
fun ReaderControlsOverlay(
    readingDirection: LayoutDirection,
    onNexPageClick: suspend () -> Unit,
    onPrevPageClick: suspend () -> Unit,
    isSettingsMenuOpen: Boolean,
    onSettingsMenuToggle: () -> Unit,
    tapNavigationMode: ReaderTapNavigationMode,
    contentAreaSize: IntSize,
    scaleState: ScreenScaleState,
    tapToZoom: Boolean,
    modifier: Modifier,
    onLongPress: ((Offset) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val nextAction = { coroutineScope.launch { onNexPageClick() } }
    val prevAction = { coroutineScope.launch { onPrevPageClick() } }

    val areaCenter = remember(contentAreaSize) { Offset(contentAreaSize.width / 2f, contentAreaSize.height / 2f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .pointerInput(
                contentAreaSize,
                readingDirection,
                onSettingsMenuToggle,
                isSettingsMenuOpen,
                tapNavigationMode,
                tapToZoom,
                onLongPress
            ) {
                val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis

                fun handleNavTap(offset: Offset) {
                    val width = contentAreaSize.width.toFloat()
                    val height = contentAreaSize.height.toFloat()
                    val isLeft = offset.x < width / 3
                    when (tapNavigationMode) {
                        ReaderTapNavigationMode.LEFT_RIGHT -> {
                            if (readingDirection == LayoutDirection.Ltr) {
                                if (isLeft) prevAction() else nextAction()
                            } else {
                                if (isLeft) nextAction() else prevAction()
                            }
                        }

                        ReaderTapNavigationMode.RIGHT_LEFT -> {
                            if (readingDirection == LayoutDirection.Ltr) {
                                if (isLeft) nextAction() else prevAction()
                            } else {
                                if (isLeft) prevAction() else nextAction()
                            }
                        }

                        ReaderTapNavigationMode.HORIZONTAL_SPLIT -> {
                            if (offset.y < height / 2) prevAction() else nextAction()
                        }

                        ReaderTapNavigationMode.REVERSED_HORIZONTAL_SPLIT -> {
                            if (offset.y < height / 2) nextAction() else prevAction()
                        }
                    }
                }

                fun isEdgeZone(offset: Offset): Boolean {
                    val actionWidth = contentAreaSize.width.toFloat() / 3
                    return offset.x < actionWidth || offset.x > actionWidth * 2
                }

                fun isCenterZone(offset: Offset): Boolean {
                    val actionWidth = contentAreaSize.width.toFloat() / 3
                    return offset.x in actionWidth..actionWidth * 2
                }

                if (tapToZoom) {
                    // Custom gesture: instant taps on edges, double-tap timeout only in center
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val downOffset = down.position
                        down.consume()
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        up.consume()
                        val tapOffset = up.position

                        if (isSettingsMenuOpen) {
                            onSettingsMenuToggle()
                            return@awaitEachGesture
                        }

                        if (isEdgeZone(tapOffset)) {
                            // Edge tap: fire immediately, no double-tap wait
                            handleNavTap(tapOffset)
                        } else if (isCenterZone(tapOffset)) {
                            // Center zone: wait for possible double-tap
                            val secondDown = withTimeoutOrNull(doubleTapTimeoutMs) {
                                awaitFirstDown()
                            }
                            if (secondDown != null) {
                                secondDown.consume()
                                val secondUp = waitForUpOrCancellation()
                                secondUp?.consume()
                                if (secondUp != null) {
                                    scaleState.toggleZoom(secondUp.position - areaCenter)
                                }
                            } else {
                                onSettingsMenuToggle()
                            }
                        }
                    }
                } else {
                    // No double-tap: all taps fire instantly
                    detectTapGestures(
                        onLongPress = onLongPress,
                        onTap = { offset ->
                            if (isSettingsMenuOpen) {
                                onSettingsMenuToggle()
                                return@detectTapGestures
                            }
                            if (isCenterZone(offset)) {
                                onSettingsMenuToggle()
                                return@detectTapGestures
                            }
                            handleNavTap(offset)
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

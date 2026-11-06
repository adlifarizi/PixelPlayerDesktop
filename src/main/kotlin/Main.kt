import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import org.jetbrains.compose.resources.painterResource as composePainterResource

import com.kayoi.pixelplayerdesktop.di.appModules
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_tray
import com.kayoi.pixelplayerdesktop.presentation.components.InputFocusTracker
import com.kayoi.pixelplayerdesktop.presentation.components.LocalInputFocusTracker
import com.kayoi.pixelplayerdesktop.presentation.components.LocalWindowMinimized
import com.kayoi.pixelplayerdesktop.presentation.components.MiniPlayerWindowView
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopBackDispatcher
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavigator
import com.kayoi.pixelplayerdesktop.presentation.navigation.LocalBackDispatcher
import com.kayoi.pixelplayerdesktop.presentation.navigation.toDefaultTarget
import com.kayoi.pixelplayerdesktop.presentation.screens.MainAppScaffold
import com.kayoi.pixelplayerdesktop.presentation.screens.setup.SetupScreen
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.SystemAccentColorProvider
import com.kayoi.pixelplayerdesktop.utils.applyNativeWindowAppearance
import io.github.vinceglb.filekit.FileKit
import java.awt.AWTEvent
import java.awt.Dimension
import java.awt.Frame
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.event.WindowStateListener
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.lifecycle.AppShutdownCoordinator

// 600 x 400 keeps the window inside a half-screen snap zone on a 1280-dp-wide display.
private const val MIN_CONTENT_WIDTH_DP = 600
private const val MIN_CONTENT_HEIGHT_DP = 400

/**
 * Parses an ICO resource into a list of [BufferedImage] at every embedded resolution.
 */
private fun loadMultiResolutionIcoImages(resourcePath: String): List<BufferedImage> {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        ?: return emptyList()
    return try {
        val bytes = stream.use { it.readBytes() }
        if (bytes.size < 6) return emptyList()
        val count = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
        val images = mutableListOf<BufferedImage>()
        for (i in 0 until count) {
            val dirOffset = 6 + i * 16
            if (dirOffset + 16 > bytes.size) break
            val dataSize = ((bytes[dirOffset + 8].toInt() and 0xFF)) or
                    ((bytes[dirOffset + 9].toInt() and 0xFF) shl 8) or
                    ((bytes[dirOffset + 10].toInt() and 0xFF) shl 16) or
                    ((bytes[dirOffset + 11].toInt() and 0xFF) shl 24)
            val dataOffset = ((bytes[dirOffset + 12].toInt() and 0xFF)) or
                    ((bytes[dirOffset + 13].toInt() and 0xFF) shl 8) or
                    ((bytes[dirOffset + 14].toInt() and 0xFF) shl 16) or
                    ((bytes[dirOffset + 15].toInt() and 0xFF) shl 24)
            if (dataOffset + dataSize > bytes.size) continue
            val imageBytes = bytes.copyOfRange(dataOffset, dataOffset + dataSize)
            val isPng = imageBytes.size >= 8 &&
                    imageBytes[0] == 0x89.toByte() && imageBytes[1] == 0x50.toByte() &&
                    imageBytes[2] == 0x4E.toByte() && imageBytes[3] == 0x47.toByte()
            if (isPng) {
                val img = ImageIO.read(java.io.ByteArrayInputStream(imageBytes))
                if (img != null) images.add(img)
            } else {
                if (imageBytes.size < 40) continue
                val bmpWidth = ((imageBytes[4].toInt() and 0xFF)) or
                        ((imageBytes[5].toInt() and 0xFF) shl 8) or
                        ((imageBytes[6].toInt() and 0xFF) shl 16) or
                        ((imageBytes[7].toInt() and 0xFF) shl 24)
                val bmpHeightRaw = ((imageBytes[8].toInt() and 0xFF)) or
                        ((imageBytes[9].toInt() and 0xFF) shl 8) or
                        ((imageBytes[10].toInt() and 0xFF) shl 16) or
                        ((imageBytes[11].toInt() and 0xFF) shl 24)
                val bmpHeight = bmpHeightRaw / 2
                val bpp = ((imageBytes[14].toInt() and 0xFF)) or
                        ((imageBytes[15].toInt() and 0xFF) shl 8)
                if (bpp == 32 && bmpWidth > 0 && bmpHeight > 0) {
                    val img = BufferedImage(bmpWidth, bmpHeight, BufferedImage.TYPE_INT_ARGB)
                    val pixelDataOffset = 40
                    for (y in 0 until bmpHeight) {
                        for (x in 0 until bmpWidth) {
                            val pos = pixelDataOffset + ((bmpHeight - 1 - y) * bmpWidth + x) * 4
                            if (pos + 3 >= imageBytes.size) continue
                            val b = imageBytes[pos].toInt() and 0xFF
                            val g = imageBytes[pos + 1].toInt() and 0xFF
                            val r = imageBytes[pos + 2].toInt() and 0xFF
                            val a = imageBytes[pos + 3].toInt() and 0xFF
                            img.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                        }
                    }
                    images.add(img)
                }
            }
        }
        images
    } catch (e: Exception) {
        emptyList()
    }
}

private fun isIgnorableSkikoDisposalException(throwable: Throwable?): Boolean {
    var current = throwable
    while (current != null) {
        if (current is IllegalArgumentException && current.message?.contains("RootNodeOwner is already disposed") == true) {
            return true
        }
        for (suppressed in current.suppressed) {
            if (isIgnorableSkikoDisposalException(suppressed)) {
                return true
            }
        }
        current = current.cause
    }
    return false
}

fun main(args: Array<String> = emptyArray()) {
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        if (isIgnorableSkikoDisposalException(throwable)) {
            return@setDefaultUncaughtExceptionHandler
        }
        previousHandler?.uncaughtException(thread, throwable)
    }
    val osName = System.getProperty("os.name", "").lowercase()
    if (osName.contains("mac") || osName.contains("darwin")) {
        System.setProperty("apple.awt.application.appearance", "system")
    }
    val appStartTime = System.currentTimeMillis()
    val isBenchmark = args.contains("--benchmark-first-frame")
    AppShutdownCoordinator.registerShutdownHook()
    FileKit.init("com.kayoi.pixelplayerdesktop")
    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Maximized,
            width = 1100.dp,
            height = 800.dp
        )

    val koin = remember {
        val existing = GlobalContext.getOrNull()
        if (existing == null) {
            startKoin {
                modules(appModules)
            }.koin
        } else {
            existing.get()
        }
    }

    val playerViewModel = remember { koin.get<PlayerViewModel>() }
    val audioEngine = remember { koin.get<AudioEngine>() }
    val initialSetupDone by playerViewModel.initialSetupDone.collectAsState()
    val isDarkTheme by playerViewModel.isDarkMode.collectAsState()
    val uiScale by playerViewModel.uiScale.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val playerInfo by playerViewModel.playerInfo.collectAsState()
    val closeToTray by playerViewModel.closeToTray.collectAsState()
    val isMiniPlayerMode by playerViewModel.isMiniPlayerMode.collectAsState()
    val isMiniPlayerAlwaysOnTop by playerViewModel.isMiniPlayerAlwaysOnTop.collectAsState()
    var isWindowVisible by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }

    val exitApp: () -> Unit = remember(audioEngine) {
        {
            if (!isExiting) {
                isExiting = true
                isWindowVisible = false
                AppShutdownCoordinator.shutdownAndExit(audioEngine = audioEngine) {
                    SwingUtilities.invokeLater {
                        exitApplication()
                    }
                }
            }
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    var isInputActive by remember { mutableStateOf(false) }
    val inputFocusTracker = remember { InputFocusTracker { isInputActive = it } }
    var isQueueSidePanelOpen by remember { mutableStateOf(true) }

    val backDispatcher = remember { DesktopBackDispatcher() }
    val defaultTabKey by playerViewModel.defaultTab.collectAsState()
    val initialNavTarget = remember(defaultTabKey) {
        DesktopNavDestination.entries.firstOrNull { it.name == defaultTabKey }?.toDefaultTarget() ?: DesktopNavDestination.HOME.toDefaultTarget()
    }
    val navigator = remember { DesktopNavigator(initialNavTarget) }

    var lastNavTimestamp by remember { mutableStateOf(0L) }
    val triggerBack: () -> Unit = remember(backDispatcher) {
        {
            val now = System.currentTimeMillis()
            if (now - lastNavTimestamp > 350L) {
                lastNavTimestamp = now
                backDispatcher.dispatchBack()
            }
        }
    }
    val triggerForward: () -> Unit = remember(backDispatcher) {
        {
            val now = System.currentTimeMillis()
            if (now - lastNavTimestamp > 350L) {
                lastNavTimestamp = now
                backDispatcher.dispatchForward()
            }
        }
    }

    val isWindows = remember { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }

    val icoIconImages = remember {
        if (isWindows) loadMultiResolutionIcoImages("icons/icon.ico") else emptyList()
    }
    val isLinux = remember { System.getProperty("os.name").contains("linux", ignoreCase = true) }

    DisposableEffect(triggerBack, triggerForward, isLinux) {
        val mouseListener = AWTEventListener { event ->
            if (event is MouseEvent && event.id == MouseEvent.MOUSE_RELEASED) {
                // OpenJDK on X11 maps side buttons 8/9 to AWT 6/7; buttons 4/5 are horizontal scroll.
                val isBack = if (isLinux) event.button == 6 else event.button == 4
                val isForward = if (isLinux) event.button == 7 else event.button == 5
                if (isBack) {
                    triggerBack()
                } else if (isForward) {
                    triggerForward()
                }
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(
            mouseListener,
            AWTEvent.MOUSE_EVENT_MASK
        )
        onDispose {
            Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener)
        }
    }

    // System-Based Dynamic Theme (Windows / Linux Accent Color)
    val persistedAccent = playerViewModel.themeStateHolder.lastKnownAccentColor.value
    val initialAccent = remember {
        persistedAccent?.let { Color(it) } ?: SystemAccentColorProvider.defaultAccentColor
    }
    val systemColorSchemePair by produceState(
        initialValue = remember(initialAccent) { DynamicColorProcessor.generateColorSchemePair(initialAccent) }
    ) {
        val systemSeed = withContext(Dispatchers.IO) {
            SystemAccentColorProvider.getSystemAccentColor()
        }
        if (persistedAccent == null || Color(persistedAccent) != systemSeed) {
            playerViewModel.themeStateHolder.updateLastKnownAccentColor(systemSeed.toArgb())
        }
        if (systemSeed != initialAccent) {
            value = DynamicColorProcessor.generateColorSchemePair(systemSeed)
        }
    }
    val systemColorScheme = if (isDarkTheme) systemColorSchemePair.dark else systemColorSchemePair.light

    // Scan user's library on launch if setup already completed
    LaunchedEffect(initialSetupDone) {
        if (initialSetupDone) {
            playerViewModel.scanLibrary()
        }
    }

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null && queue.isNotEmpty()) {
            if (windowState.size.width >= 800.dp) {
                isQueueSidePanelOpen = true
            }
        }
    }

    if (isBenchmark) {
        LaunchedEffect(Unit) {
            val ttf = System.currentTimeMillis() - appStartTime
            println("TIME_TO_FIRST_FRAME: ${ttf}ms")
            exitApp()
        }
    }

    if (closeToTray) {
        Tray(
            icon = composePainterResource(Res.drawable.ic_tray),
            tooltip = "PixelPlayer",
            onAction = {
                isWindowVisible = true
                windowState.isMinimized = false
            },
            menu = {
                Item(
                    text = "Open PixelPlayer",
                    onClick = {
                        isWindowVisible = true
                        windowState.isMinimized = false
                    }
                )
                Item(
                    text = if (isMiniPlayerMode) "Switch to Full Player" else "Switch to Mini Player",
                    onClick = {
                        isWindowVisible = true
                        playerViewModel.toggleMiniPlayerMode()
                    }
                )
                Item(
                    text = if (playerInfo.isPlaying) "Pause" else "Play",
                    onClick = { playerViewModel.togglePlayPause() }
                )
                Item(
                    text = "Next",
                    onClick = { playerViewModel.playNext() }
                )
                Item(
                    text = "Previous",
                    onClick = { playerViewModel.playPrevious() }
                )
                Separator()
                Item(
                    text = "Quit",
                    onClick = exitApp
                )
            }
        )
    }

    Window(
        visible = isWindowVisible && !isMiniPlayerMode,
        onCloseRequest = {
            if (closeToTray) {
                isWindowVisible = false
            } else {
                exitApp()
            }
        },
        title = "PixelPlayer",
        icon = painterResource("icons/icon.png"),
        state = windowState,
        onPreviewKeyEvent = { keyEvent ->
            val isModifier = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    isModifier && keyEvent.key == Key.K -> {
                        searchFocusRequester.requestFocus()
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionRight -> {
                        if (!isInputActive) {
                            playerViewModel.playNext()
                            true
                        } else {
                            false
                        }
                    }
                    isModifier && keyEvent.key == Key.DirectionLeft -> {
                        if (!isInputActive) {
                            playerViewModel.playPrevious()
                            true
                        } else {
                            false
                        }
                    }
                    !isModifier && !keyEvent.isAltPressed && !isInputActive && keyEvent.key == Key.DirectionRight -> {
                        playerViewModel.seekForward(5000L)
                        true
                    }
                    !isModifier && !keyEvent.isAltPressed && !isInputActive && keyEvent.key == Key.DirectionLeft -> {
                        playerViewModel.seekBackward(5000L)
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionUp -> {
                        val currentVol = playerViewModel.playerInfo.value.volume
                        playerViewModel.setVolume((currentVol + 0.05f).coerceIn(0f, 1f))
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionDown -> {
                        val currentVol = playerViewModel.playerInfo.value.volume
                        playerViewModel.setVolume((currentVol - 0.05f).coerceIn(0f, 1f))
                        true
                    }
                    keyEvent.key == Key.M && !isInputActive && !isModifier -> {
                        playerViewModel.toggleMute()
                        true
                    }
                    isModifier && keyEvent.key == Key.S -> {
                        playerViewModel.toggleShuffle()
                        true
                    }
                    isModifier && keyEvent.key == Key.R -> {
                        playerViewModel.cycleRepeatMode()
                        true
                    }
                    keyEvent.key == Key.L && !isInputActive && !isModifier -> {
                        playerViewModel.toggleFavoriteCurrentSong()
                        true
                    }
                    (isModifier || keyEvent.isAltPressed) && keyEvent.key == Key.Q -> {
                        isQueueSidePanelOpen = !isQueueSidePanelOpen
                        true
                    }
                    isModifier && keyEvent.isShiftPressed && keyEvent.key == Key.M -> {
                        playerViewModel.toggleMiniPlayerMode()
                        true
                    }
                    keyEvent.key == Key.F11 -> {
                        windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                            WindowPlacement.Maximized
                        } else {
                            WindowPlacement.Fullscreen
                        }
                        true
                    }
                    keyEvent.key == Key.Spacebar && !isInputActive -> {
                        playerViewModel.togglePlayPause()
                        true
                    }
                    keyEvent.isAltPressed && keyEvent.key == Key.DirectionLeft -> {
                        triggerBack()
                        true
                    }
                    keyEvent.isAltPressed && keyEvent.key == Key.DirectionRight -> {
                        triggerForward()
                        true
                    }
                    (isModifier && keyEvent.key == Key.Slash) || keyEvent.key == Key.F1 -> {
                        playerViewModel.toggleShortcutsDialog()
                        true
                    }
                    isModifier && (keyEvent.key == Key.Equals || keyEvent.key == Key.Plus || keyEvent.key == Key.NumPadAdd) -> {
                        playerViewModel.adjustUiScale(0.10f)
                        true
                    }
                    isModifier && (keyEvent.key == Key.Minus || keyEvent.key == Key.NumPadSubtract) -> {
                        playerViewModel.adjustUiScale(-0.10f)
                        true
                    }
                    isModifier && (keyEvent.key == Key.Zero || keyEvent.key == Key.NumPad0) -> {
                        playerViewModel.resetUiScale()
                        true
                    }
                    else -> false
                }
            } else if (keyEvent.type == KeyEventType.KeyUp) {
                when {
                    (isModifier && keyEvent.key == Key.Slash) || keyEvent.key == Key.F1 -> true
                    isModifier && (keyEvent.key == Key.Equals || keyEvent.key == Key.Plus || keyEvent.key == Key.NumPadAdd ||
                        keyEvent.key == Key.Minus || keyEvent.key == Key.NumPadSubtract ||
                        keyEvent.key == Key.Zero || keyEvent.key == Key.NumPad0) -> true
                    keyEvent.isAltPressed && (keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.DirectionRight) -> true
                    isModifier && (keyEvent.key == Key.K || keyEvent.key == Key.S || keyEvent.key == Key.R || keyEvent.key == Key.Q) -> true
                    isModifier && !isInputActive && (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown) -> true
                    !isModifier && !keyEvent.isAltPressed && !isInputActive && (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.DirectionLeft) -> true
                    keyEvent.isAltPressed && keyEvent.key == Key.Q -> true
                    keyEvent.key == Key.F11 -> true
                    (keyEvent.key == Key.M || keyEvent.key == Key.L || keyEvent.key == Key.Spacebar) && !isInputActive && !isModifier -> true
                    else -> false
                }
            } else {
                false
            }
         }
    ) {
        DisposableEffect(window) {
            if (icoIconImages.isNotEmpty()) {
                window.iconImages = icoIconImages
            }

            // minimumSize is in physical pixels on Windows; insets use logical units.
            fun applyMinimumContentSize() {
                val insets = window.insets ?: Insets(0, 0, 0, 0)
                val dpiScale = if (isWindows) {
                    window.graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
                } else {
                    1.0
                }
                val target = Dimension(
                    (MIN_CONTENT_WIDTH_DP * dpiScale).roundToInt() +
                        (insets.left * dpiScale).roundToInt() +
                        (insets.right * dpiScale).roundToInt(),
                    (MIN_CONTENT_HEIGHT_DP * dpiScale).roundToInt() +
                        (insets.top * dpiScale).roundToInt() +
                        (insets.bottom * dpiScale).roundToInt()
                )
                if (window.minimumSize != target) {
                    window.minimumSize = target
                }
            }
            applyMinimumContentSize()
            // Insets can be zero before the window is displayable.
            SwingUtilities.invokeLater(::applyMinimumContentSize)

            fun availableScreenBounds(): Rectangle? {
                val config = window.graphicsConfiguration ?: return null
                val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
                val bounds = config.bounds
                return Rectangle(
                    bounds.x + insets.left,
                    bounds.y + insets.top,
                    bounds.width - insets.left - insets.right,
                    bounds.height - insets.top - insets.bottom
                )
            }

            fun updateMaximizedBounds() {
                if (!isWindows) return
                window.maximizedBounds = availableScreenBounds() ?: return
            }

            updateMaximizedBounds()

            // Restoring from maximized should not shrink back to the previous floating
            // height on Windows; stretch the window to the full available height of its monitor.
            val windowStateListener = WindowStateListener { event ->
                if (!isWindows) return@WindowStateListener
                val wasMaximized = (event.oldState and Frame.MAXIMIZED_BOTH) != 0
                val isMaximized = (event.newState and Frame.MAXIMIZED_BOTH) != 0
                if (wasMaximized && !isMaximized) {
                    SwingUtilities.invokeLater {
                        if ((window.extendedState and Frame.MAXIMIZED_BOTH) != 0 || !window.isShowing) {
                            return@invokeLater
                        }
                        val available = availableScreenBounds() ?: return@invokeLater
                        val current = window.bounds
                        val width = current.width.coerceAtMost(available.width)
                        val minVisibleMargin = 120
                        val minX = available.x - width + minVisibleMargin
                        val maxX = available.x + available.width - minVisibleMargin
                        val x = current.x.coerceIn(minX, maxX.coerceAtLeast(minX))
                        val target = Rectangle(x, available.y, width, available.height)
                        if (window.bounds != target) {
                            window.bounds = target
                        }
                    }
                }
            }
            window.addWindowStateListener(windowStateListener)

            val componentListener = object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent?) {
                    updateMaximizedBounds()
                    // Keep the minimum size in sync with the monitor's DPI.
                    applyMinimumContentSize()
                }
                override fun componentResized(e: ComponentEvent?) {
                    updateMaximizedBounds()
                }
            }
            window.addComponentListener(componentListener)

            onDispose {
                window.removeComponentListener(componentListener)
                window.removeWindowStateListener(windowStateListener)
            }
        }

        DisposableEffect(window, isDarkTheme) {
            applyNativeWindowAppearance(window, isDarkTheme)
            onDispose {}
        }

        val navDispatcherOwner = LocalNavigationEventDispatcherOwner.current
        DisposableEffect(navDispatcherOwner, triggerBack) {
            val dispatcher = navDispatcherOwner?.navigationEventDispatcher ?: return@DisposableEffect onDispose {}
            val handler = object : NavigationEventHandler<NavigationEventInfo>(
                object : NavigationEventInfo() {},
                true
            ) {
                override fun onBackCompleted() {
                    triggerBack()
                }
            }
            dispatcher.addHandler(handler)
            onDispose {
                handler.remove()
            }
        }

        val baseDensity = LocalDensity.current
        val scaledDensity = remember(baseDensity, uiScale) {
            Density(
                density = baseDensity.density * uiScale,
                fontScale = baseDensity.fontScale
            )
        }

        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalWindowMinimized provides windowState.isMinimized,
            LocalInputFocusTracker provides inputFocusTracker,
            LocalBackDispatcher provides backDispatcher
        ) {
            PixelPlayerTheme(
                darkTheme = isDarkTheme,
                colorSchemeOverride = systemColorScheme
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!initialSetupDone) {
                        SetupScreen(
                            viewModel = playerViewModel,
                            onSetupComplete = {
                                playerViewModel.setInitialSetupDone(true)
                                playerViewModel.scanLibrary(forceRescan = true)
                            }
                        )
                    } else {
                        MainAppScaffold(
                            viewModel = playerViewModel,
                            searchFocusRequester = searchFocusRequester,
                            onSearchFocusChange = { inputFocusTracker.onFocusChanged(it) },
                            isQueueSidePanelOpen = isQueueSidePanelOpen,
                            onToggleQueueSidePanel = { isQueueSidePanelOpen = !isQueueSidePanelOpen },
                            onCloseQueueSidePanel = { isQueueSidePanelOpen = false },
                            navigator = navigator,
                            backDispatcher = backDispatcher
                        )
                    }
                }
            }
        }
    }

    if (isMiniPlayerMode && !isExiting) {
        val miniWindowState = rememberWindowState(
            width = 360.dp,
            height = 184.dp
        )
        Window(
            visible = isWindowVisible,
            onCloseRequest = {
                playerViewModel.setMiniPlayerMode(false)
            },
            title = currentSong?.let { "${it.title} • ${it.artist}" } ?: "PixelPlayer Mini",
            icon = painterResource("icons/icon.png"),
            state = miniWindowState,
            alwaysOnTop = isMiniPlayerAlwaysOnTop,
            resizable = false,
            onPreviewKeyEvent = { keyEvent ->
                val isModifier = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        isModifier && keyEvent.isShiftPressed && keyEvent.key == Key.M -> {
                            playerViewModel.setMiniPlayerMode(false)
                            true
                        }
                        keyEvent.key == Key.Spacebar -> {
                            playerViewModel.togglePlayPause()
                            true
                        }
                        isModifier && keyEvent.key == Key.DirectionRight -> {
                            playerViewModel.playNext()
                            true
                        }
                        isModifier && keyEvent.key == Key.DirectionLeft -> {
                            playerViewModel.playPrevious()
                            true
                        }
                        !isModifier && !keyEvent.isAltPressed && keyEvent.key == Key.DirectionRight -> {
                            playerViewModel.seekForward(5000L)
                            true
                        }
                        !isModifier && !keyEvent.isAltPressed && keyEvent.key == Key.DirectionLeft -> {
                            playerViewModel.seekBackward(5000L)
                            true
                        }
                        isModifier && keyEvent.key == Key.DirectionUp -> {
                            val currentVol = playerViewModel.playerInfo.value.volume
                            playerViewModel.setVolume((currentVol + 0.05f).coerceIn(0f, 1f))
                            true
                        }
                        isModifier && keyEvent.key == Key.DirectionDown -> {
                            val currentVol = playerViewModel.playerInfo.value.volume
                            playerViewModel.setVolume((currentVol - 0.05f).coerceIn(0f, 1f))
                            true
                        }
                        keyEvent.key == Key.M && !isModifier -> {
                            playerViewModel.toggleMute()
                            true
                        }
                        keyEvent.key == Key.L -> {
                            playerViewModel.toggleFavoriteCurrentSong()
                            true
                        }
                        else -> false
                    }
                } else false
            }
        ) {
            DisposableEffect(window, isDarkTheme) {
                applyNativeWindowAppearance(window, isDarkTheme)
                onDispose {}
            }

            DisposableEffect(window) {
                if (icoIconImages.isNotEmpty()) {
                    window.iconImages = icoIconImages
                }
                onDispose {}
            }

            PixelPlayerTheme(
                darkTheme = isDarkTheme,
                colorSchemeOverride = systemColorScheme
            ) {
                MiniPlayerWindowView(
                    viewModel = playerViewModel,
                    isAlwaysOnTop = isMiniPlayerAlwaysOnTop,
                    onToggleAlwaysOnTop = { playerViewModel.toggleMiniPlayerAlwaysOnTop() },
                    onRestoreMainWindow = { playerViewModel.setMiniPlayerMode(false) }
                )
            }
        }
    }
}
    AppShutdownCoordinator.performTeardown(timeoutMs = 1000L)
    exitProcess(0)
}

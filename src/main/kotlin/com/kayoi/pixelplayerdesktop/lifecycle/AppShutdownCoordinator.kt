package com.kayoi.pixelplayerdesktop.lifecycle

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

import okhttp3.OkHttpClient
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.watcher.LibraryWatcherService
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionBridge
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.DesktopListeningStatsTracker

object AppShutdownCoordinator {
    private val isTeardownExecuted = AtomicBoolean(false)
    private var shutdownHookThread: Thread? = null

    fun registerShutdownHook(timeoutMs: Long = 2500L) {
        if (shutdownHookThread != null) return
        val hook = Thread({
            performTeardown(timeoutMs = timeoutMs)
        }, "PixelPlayer-ShutdownHook")
        shutdownHookThread = hook
        runCatching {
            Runtime.getRuntime().addShutdownHook(hook)
        }
    }

    fun performTeardown(
        audioEngine: AudioEngine? = null,
        timeoutMs: Long = 2500L
    ) {
        if (!isTeardownExecuted.compareAndSet(false, true)) {
            return
        }

        val targetAudioEngine = audioEngine ?: runCatching {
            GlobalContext.getOrNull()?.getOrNull<AudioEngine>()
        }.getOrNull()

        val teardownFuture = CompletableFuture.runAsync {
            runCatching {
                val mediaSessionBridge = GlobalContext.getOrNull()?.getOrNull<MediaSessionBridge>()
                mediaSessionBridge?.release()
            }

            runCatching {
                targetAudioEngine?.release()
            }

            runCatching {
                val statsTracker = GlobalContext.getOrNull()?.getOrNull<DesktopListeningStatsTracker>()
                statsTracker?.finalizeCurrentSession(forceSynchronous = true)
            }

            runCatching {
                val watcherService = GlobalContext.getOrNull()?.getOrNull<LibraryWatcherService>()
                watcherService?.close()
            }

            runCatching {
                val okHttpClient = GlobalContext.getOrNull()?.getOrNull<OkHttpClient>()
                okHttpClient?.dispatcher?.executorService?.shutdown()
                okHttpClient?.connectionPool?.evictAll()
            }

            runCatching {
                val database = GlobalContext.getOrNull()?.getOrNull<PixelPlayerDatabase>()
                database?.close()
            }

            runCatching {
                stopKoin()
            }
        }

        try {
            teardownFuture.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            teardownFuture.cancel(true)
        } catch (_: Throwable) {
        }
    }

    fun shutdownAndExit(
        audioEngine: AudioEngine? = null,
        timeoutMs: Long = 2500L,
        onExit: () -> Unit
    ) {
        thread(name = "PixelPlayer-TeardownCoordinator", isDaemon = true) {
            performTeardown(audioEngine = audioEngine, timeoutMs = timeoutMs)
            onExit()
        }
    }

    internal fun resetForTesting() {
        isTeardownExecuted.set(false)
        shutdownHookThread = null
    }
}

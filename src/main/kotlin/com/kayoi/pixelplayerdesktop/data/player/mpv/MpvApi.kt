package com.kayoi.pixelplayerdesktop.data.player.mpv

import java.io.File

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure

interface MpvApi : Library {

    fun mpv_client_api_version(): Long
    fun mpv_error_string(error: Int): String
    fun mpv_create(): Pointer?
    fun mpv_initialize(handle: Pointer): Int
    fun mpv_destroy(handle: Pointer)
    fun mpv_terminate_destroy(handle: Pointer)
    fun mpv_command(handle: Pointer, args: Array<String?>): Int
    fun mpv_command_string(handle: Pointer, args: String): Int
    fun mpv_set_property(handle: Pointer, name: String, format: Int, data: Pointer): Int
    fun mpv_set_property_string(handle: Pointer, name: String, data: String): Int
    fun mpv_get_property_string(handle: Pointer, name: String): Pointer?
    fun mpv_observe_property(handle: Pointer, replyUserdata: Long, name: String, format: Int): Int
    fun mpv_unobserve_property(handle: Pointer, registeredReplyUserdata: Long): Int
    fun mpv_wait_event(handle: Pointer, timeout: Double): MpvEvent.ByReference?
    fun mpv_wakeup(handle: Pointer)
    fun mpv_free(data: Pointer)

    companion object {
        const val MPV_FORMAT_NONE = 0
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_OSD_STRING = 2
        const val MPV_FORMAT_FLAG = 3
        const val MPV_FORMAT_INT64 = 4
        const val MPV_FORMAT_DOUBLE = 5
        const val MPV_FORMAT_NODE = 6

        const val MPV_EVENT_NONE = 0
        const val MPV_EVENT_SHUTDOWN = 1
        const val MPV_EVENT_START_FILE = 6
        const val MPV_EVENT_END_FILE = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_IDLE = 11
        const val MPV_EVENT_SEEK = 20
        const val MPV_EVENT_PLAYBACK_RESTART = 21
        const val MPV_EVENT_PROPERTY_CHANGE = 22

        const val MPV_END_FILE_REASON_EOF = 0
        const val MPV_END_FILE_REASON_STOP = 2
        const val MPV_END_FILE_REASON_QUIT = 3
        const val MPV_END_FILE_REASON_ERROR = 4

        private interface LibC : Library {
            fun setlocale(category: Int, locale: String): String?
        }

        fun setupNativeDiscovery() {
            runCatching {
                val appResourceDir = System.getProperty("compose.application.resources.dir")?.let { File(it) }
                val userDir = File(System.getProperty("user.dir", "."))
                val codeSourceLocation = runCatching {
                    MpvApi::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
                }.getOrNull()

                val candidates = listOfNotNull(
                    appResourceDir?.resolve("mpv"),
                    appResourceDir?.resolve("windows/mpv"),
                    appResourceDir?.resolve("linux/mpv"),
                    appResourceDir?.resolve("macos/mpv"),
                    File(userDir, "mpv"),
                    File(userDir, "app/mpv"),
                    File(userDir, "resources/mpv"),
                    File(userDir, "app/resources/mpv"),
                    codeSourceLocation?.parentFile?.resolve("mpv"),
                    codeSourceLocation?.parentFile?.parentFile?.resolve("mpv"),
                    codeSourceLocation?.parentFile?.parentFile?.resolve("resources/mpv"),
                    codeSourceLocation?.parentFile?.parentFile?.resolve("app/resources/mpv"),
                    File("src/main/resources-packaged/windows/mpv"),
                    File("src/main/resources-packaged/linux/mpv"),
                    File("src/main/resources-packaged/macos/mpv"),
                    File("/opt/homebrew/lib"),
                    File("/usr/local/lib")
                )

                val mpvDir = candidates.firstOrNull { dir ->
                    File(dir, "mpv-2.dll").exists() ||
                    File(dir, "libmpv-2.dll").exists() ||
                    File(dir, "libmpv.so.2").exists() ||
                    File(dir, "libmpv.so").exists() ||
                    File(dir, "libmpv.2.dylib").exists() ||
                    File(dir, "libmpv.dylib").exists()
                }

                if (mpvDir != null) {
                    val path = mpvDir.absolutePath
                    NativeLibrary.addSearchPath("mpv", path)
                    NativeLibrary.addSearchPath("mpv-2", path)
                    NativeLibrary.addSearchPath("libmpv-2", path)
                    NativeLibrary.addSearchPath("libmpv", path)
                    NativeLibrary.addSearchPath("libmpv.2", path)
                    NativeLibrary.addSearchPath("libmpv.2.dylib", path)
                    NativeLibrary.addSearchPath("libmpv.dylib", path)
                }
            }
        }

        fun ensureLocaleNumeric() {
            runCatching {
                val isWin = Platform.isWindows()
                val libName = if (isWin) "msvcrt" else "c"
                val libc = Native.load(libName, LibC::class.java)
                val lcNumeric = if (isWin || Platform.isMac()) 4 else 1
                libc.setlocale(lcNumeric, "C")
            }
        }

        val INSTANCE: MpvApi by lazy {
            System.setProperty("jna.encoding", "UTF-8")
            setupNativeDiscovery()
            ensureLocaleNumeric()
            val loadOptions = mapOf<String, Any>(
                Library.OPTION_STRING_ENCODING to "UTF-8"
            )
            val candidates = listOf("mpv", "mpv-2", "libmpv-2", "libmpv.so.2", "libmpv.so", "libmpv.2.dylib", "libmpv.dylib")
            var lastError: Throwable? = null
            for (candidate in candidates) {
                try {
                    return@lazy Native.load(candidate, MpvApi::class.java, loadOptions)
                } catch (t: Throwable) {
                    lastError = t
                }
            }
            throw IllegalStateException("Failed to load libmpv native library from candidates: $candidates", lastError)
        }
    }
}

open class MpvEvent : Structure() {
    @JvmField var eventId: Int = 0
    @JvmField var error: Int = 0
    @JvmField var replyUserdata: Long = 0
    @JvmField var data: Pointer? = null

    class ByReference : MpvEvent(), Structure.ByReference

    override fun getFieldOrder(): List<String> = listOf("eventId", "error", "replyUserdata", "data")
}

open class MpvEventProperty : Structure() {
    @JvmField var name: String? = null
    @JvmField var format: Int = 0
    @JvmField var data: Pointer? = null

    class ByReference : MpvEventProperty(), Structure.ByReference

    override fun getFieldOrder(): List<String> = listOf("name", "format", "data")
}

open class MpvEventEndFile : Structure() {
    @JvmField var reason: Int = 0
    @JvmField var error: Int = 0

    class ByReference : MpvEventEndFile(), Structure.ByReference

    override fun getFieldOrder(): List<String> = listOf("reason", "error")
}

fun MpvApi.getPropertyString(handle: Pointer, name: String): String? {
    val ptr = mpv_get_property_string(handle, name) ?: return null
    return try {
        ptr.getString(0, "UTF-8")
    } finally {
        mpv_free(ptr)
    }
}

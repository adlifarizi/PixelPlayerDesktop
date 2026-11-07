package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import com.kayoi.pixelplayerdesktop.generated.resources.ic_lyrics
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_folder
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_person
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_sync
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_account_circle
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_auto_awesome
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_brush
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_dark_mode
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete_forever
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_fullscreen
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_graphic_eq
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_info
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_layers
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_palette
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_settings
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_settings_backup_restore
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speed
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_tab
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_tune
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_view_week
import org.jetbrains.compose.resources.DrawableResource
import com.kayoi.pixelplayerdesktop.presentation.screens.LibrarySubDetail

data class SettingSearchItem(
    val title: String,
    val description: String,
    val section: SettingsSection,
    val subDetail: LibrarySubDetail = LibrarySubDetail.NONE,
    val keywords: List<String> = emptyList(),
    val icon: DrawableResource? = null
)

object SettingsSearchCatalog {
    val allSettings: List<SettingSearchItem> = listOf(
        // --- LIBRARY: Structure & Details ---
        SettingSearchItem(
            title = "Music Folders",
            description = "Configure scanned audio directories and excluded paths",
            section = SettingsSection.LIBRARY,
            subDetail = LibrarySubDetail.FOLDERS,
            keywords = listOf("directory", "folder", "scan", "paths", "exclude", "storage"),
            icon = Res.drawable.ic_outlined_folder
        ),
        SettingSearchItem(
            title = "Artist Delimiters & Formatting",
            description = "Multi-artist splitting delimiters and featuring phrase cleanup",
            section = SettingsSection.LIBRARY,
            subDetail = LibrarySubDetail.ARTISTS,
            keywords = listOf("artist", "delimiters", "feat", "featuring", "split", "separator"),
            icon = Res.drawable.ic_outlined_person
        ),
        SettingSearchItem(
            title = "Duplicate Tracks",
            description = "Scan and resolve duplicate song files across folders",
            section = SettingsSection.LIBRARY,
            subDetail = LibrarySubDetail.DUPLICATES,
            keywords = listOf("duplicate", "clean", "clones", "same songs"),
            icon = Res.drawable.ic_rounded_delete_forever
        ),
        SettingSearchItem(
            title = "Minimum Song Duration",
            description = "Filter out audio files shorter than specified length (e.g. ringtones)",
            section = SettingsSection.LIBRARY,
            keywords = listOf("duration", "short", "filter", "seconds", "ringtone"),
            icon = Res.drawable.ic_rounded_tune
        ),
        SettingSearchItem(
            title = "Minimum Tracks per Album",
            description = "Hide incomplete albums or singles with fewer tracks than threshold",
            section = SettingsSection.LIBRARY,
            keywords = listOf("album", "tracks", "filter", "threshold", "singles"),
            icon = Res.drawable.ic_rounded_tune
        ),
        SettingSearchItem(
            title = "Rescan Music Library",
            description = "Scan folders to detect newly added, moved, or deleted audio files",
            section = SettingsSection.LIBRARY,
            keywords = listOf("rescan", "refresh", "scan", "sync", "reload", "library"),
            icon = Res.drawable.ic_outlined_sync
        ),
        SettingSearchItem(
            title = "Clear Album Art Cache",
            description = "Delete cached image thumbnails from disk to free up storage",
            section = SettingsSection.LIBRARY,
            keywords = listOf("cache", "clear", "artwork", "thumbnails", "storage", "disk"),
            icon = Res.drawable.ic_lyrics
        ),
        SettingSearchItem(
            title = "Deezer Artist Images",
            description = "Fetch high-resolution online artist profile pictures from Deezer",
            section = SettingsSection.LIBRARY,
            keywords = listOf("deezer", "artist", "pictures", "photos", "avatar", "online"),
            icon = Res.drawable.ic_outlined_person
        ),
        SettingSearchItem(
            title = "Online Lyrics Fetching",
            description = "Download synchronized and plain lyrics automatically from internet",
            section = SettingsSection.LIBRARY,
            keywords = listOf("lyrics", "online", "download", "lrc", "synced", "internet"),
            icon = Res.drawable.ic_lyrics
        ),
        SettingSearchItem(
            title = "Lyrics Source Preference",
            description = "Choose priority between embedded audio tags or online lyrics providers",
            section = SettingsSection.LIBRARY,
            keywords = listOf("lyrics", "priority", "embedded", "source", "provider"),
            icon = Res.drawable.ic_lyrics
        ),
        SettingSearchItem(
            title = "Auto-scan Lyric Files",
            description = "Automatically associate external .lrc and .ttml sidecar files beside audio tracks",
            section = SettingsSection.LIBRARY,
            keywords = listOf("lrc", "ttml", "sidecar", "lyrics", "external", "files"),
            icon = Res.drawable.ic_lyrics
        ),
        SettingSearchItem(
            title = "Lyrics Romanization",
            description = "Show romaji, pinyin, or transliteration for Japanese/CJK text",
            section = SettingsSection.LIBRARY,
            keywords = listOf("romanization", "romaji", "hiragana", "katakana", "kanji", "pinyin", "japanese"),
            icon = Res.drawable.ic_rounded_auto_awesome
        ),

        // --- APPEARANCE ---
        SettingSearchItem(
            title = "Dark Mode",
            description = "Toggle dark theme or light theme appearance",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("dark", "light", "theme", "mode", "color", "night"),
            icon = Res.drawable.ic_rounded_dark_mode
        ),
        SettingSearchItem(
            title = "Display Scale",
            description = "Adjust application zoom and UI scaling level",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("scale", "zoom", "dpi", "size", "display", "resolution"),
            icon = Res.drawable.ic_rounded_fullscreen
        ),
        SettingSearchItem(
            title = "Dynamic Color",
            description = "Extract UI accents and theme colors from the currently playing album art",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("dynamic", "color", "accent", "album art", "monet", "material"),
            icon = Res.drawable.ic_rounded_brush
        ),
        SettingSearchItem(
            title = "Palette Style & Color Accuracy",
            description = "Fine-tune dynamic color scheme harmony style and sampling resolution",
            section = SettingsSection.APPEARANCE,
            subDetail = LibrarySubDetail.PALETTE_STYLE,
            keywords = listOf("palette", "tonal", "vibrant", "expressive", "accuracy", "sampling"),
            icon = Res.drawable.ic_rounded_palette
        ),
        SettingSearchItem(
            title = "Player File Info",
            description = "Display bitrate, format, sampling rate, and codec on the full-screen player",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("codec", "bitrate", "flac", "mp3", "sample rate", "audio info", "details"),
            icon = Res.drawable.ic_rounded_info
        ),
        SettingSearchItem(
            title = "Lyrics Text Alignment",
            description = "Align lyrics text to left, center, or right on the player screen",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("alignment", "center", "left", "right", "lyrics text"),
            icon = Res.drawable.ic_lyrics
        ),
        SettingSearchItem(
            title = "Compact Sidebar",
            description = "Collapse main navigation sidebar into icon-only compact rail",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("sidebar", "compact", "navigation", "rail", "narrow", "bar"),
            icon = Res.drawable.ic_rounded_view_week
        ),
        SettingSearchItem(
            title = "Default Startup Tab",
            description = "Select default landing page when application starts (Home, Songs, Albums...)",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("default", "startup", "landing", "tab", "initial", "home"),
            icon = Res.drawable.ic_rounded_tab
        ),

        // --- PLAYBACK ---
        SettingSearchItem(
            title = "ReplayGain Volume Normalization",
            description = "Normalize track loudness across different recordings to eliminate volume jumps",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("replaygain", "volume", "normalize", "loudness", "gain", "peak"),
            icon = Res.drawable.ic_rounded_tune
        ),
        SettingSearchItem(
            title = "ReplayGain Mode",
            description = "Choose between Track Gain (individual track loudness) or Album Gain (album coherence)",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("replaygain", "mode", "track", "album gain"),
            icon = Res.drawable.ic_rounded_tune
        ),
        SettingSearchItem(
            title = "Track Crossfade",
            description = "Smoothly blend outgoing and incoming songs with an audio crossfade transition",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("crossfade", "fade", "transition", "blend", "gapless", "overlap"),
            icon = Res.drawable.ic_rounded_music_note
        ),
        SettingSearchItem(
            title = "Crossfade Duration",
            description = "Set the crossfade overlap transition time in seconds",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("crossfade", "duration", "seconds", "length", "fade time"),
            icon = Res.drawable.ic_rounded_tune
        ),
        SettingSearchItem(
            title = "Show Queue History",
            description = "Display previously played tracks at the top of the queue side panel",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("history", "queue", "previous", "played", "tracks"),
            icon = Res.drawable.ic_automirrored_rounded_queue_music
        ),
        SettingSearchItem(
            title = "Saved Song Speeds",
            description = "View count of individual songs with custom playback speed and clear all overrides",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("speed", "playback speed", "tempo", "rate", "fast", "slow", "saved speed"),
            icon = Res.drawable.ic_rounded_speed
        ),
        SettingSearchItem(
            title = "Close to System Tray",
            description = "Keep music playing in background system tray when window is closed",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("tray", "background", "close", "minimize", "system tray", "exit"),
            icon = Res.drawable.ic_rounded_layers
        ),

        // --- EQUALIZER ---
        SettingSearchItem(
            title = "Equalizer",
            description = "10-band audio frequency equalizer with customizable slider levels",
            section = SettingsSection.EQUALIZER,
            keywords = listOf("equalizer", "eq", "frequency", "bands", "audio", "sound", "bass", "treble"),
            icon = Res.drawable.ic_rounded_graphic_eq
        ),
        SettingSearchItem(
            title = "Equalizer Presets",
            description = "Switch between audio presets (Flat, Bass Boost, Vocal, Rock, Electronic...)",
            section = SettingsSection.EQUALIZER,
            keywords = listOf("presets", "bass boost", "vocal", "rock", "profiles", "sound profiles"),
            icon = Res.drawable.ic_rounded_graphic_eq
        ),
        SettingSearchItem(
            title = "Equalizer Preamp",
            description = "Adjust global preamp compensation gain in decibels",
            section = SettingsSection.EQUALIZER,
            keywords = listOf("preamp", "gain", "decibels", "db", "boost", "loudness"),
            icon = Res.drawable.ic_rounded_tune
        ),

        // --- ACCOUNTS & STREAMING ---
        SettingSearchItem(
            title = "ListenBrainz Scrobbler",
            description = "Scrobble listened tracks and history to ListenBrainz profile",
            section = SettingsSection.ACCOUNTS,
            keywords = listOf("listenbrainz", "scrobble", "lastfm", "token", "history", "stats"),
            icon = Res.drawable.ic_rounded_account_circle
        ),
        SettingSearchItem(
            title = "Navidrome Subsonic Server",
            description = "Connect to self-hosted Navidrome or Subsonic server to stream remote library",
            section = SettingsSection.ACCOUNTS,
            keywords = listOf("navidrome", "subsonic", "stream", "server", "remote", "cloud"),
            icon = Res.drawable.ic_rounded_account_circle
        ),
        SettingSearchItem(
            title = "Jellyfin Media Server",
            description = "Connect to self-hosted Jellyfin server to stream remote music collection",
            section = SettingsSection.ACCOUNTS,
            keywords = listOf("jellyfin", "emby", "stream", "server", "remote", "cloud"),
            icon = Res.drawable.ic_rounded_account_circle
        ),

        // --- ABOUT ---
        SettingSearchItem(
            title = "About PixelPlayer",
            description = "Application version, developer links, build details, and open-source licenses",
            section = SettingsSection.ABOUT,
            keywords = listOf("about", "version", "update", "github", "license", "credits"),
            icon = Res.drawable.ic_rounded_info
        ),

        // --- BACKUP & RESTORE ---
        SettingSearchItem(
            title = "Backup & Restore",
            description = "Export and restore playlists, favorites, lyrics, and settings with .pxpl files",
            section = SettingsSection.BACKUP,
            keywords = listOf("backup", "restore", "export", "import", "pxpl", "data", "playlists", "favorites", "migration"),
            icon = Res.drawable.ic_rounded_settings_backup_restore
        )
    )

    fun search(query: String): List<SettingSearchItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return allSettings.filter { item ->
            val corpus = "${item.title} ${item.description} ${item.section.title} ${item.keywords.joinToString(" ")}".lowercase()
            tokens.all { token -> corpus.contains(token) }
        }
    }
}

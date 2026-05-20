package com.maxi.assistant.ai

import com.maxi.assistant.models.CommandType
import com.maxi.assistant.models.VoiceCommand

object IntentAnalyzer {

    private val OPEN_PATTERNS    = listOf("খোলো", "খোল", "চালু করো", "চালু কর", "ওপেন করো", "ওপেন কর", "open", "launch", "start", "দেখাও", "নিয়ে যাও")
    private val CALL_PATTERNS    = listOf("ফোন করো", "ফোন কর", "কল করো", "কল কর", "ফোন দাও", "call", "ডায়াল করো", "ring")
    private val WHATSAPP_CALL    = listOf("হোয়াটসঅ্যাপ কল", "ভিডিও কল", "whatsapp call", "video call", "wa call")
    private val MSG_PATTERNS     = listOf("মেসেজ করো", "মেসেজ পাঠাও", "পাঠাও", "লিখো", "message", "send", "sms")
    private val YOUTUBE_PATTERNS = listOf("ইউটিউব", "youtube")
    private val SPOTIFY_PATTERNS = listOf("স্পটিফাই", "spotify")
    private val VOLUME_UP        = listOf("ভলিউম বাড়াও", "আওয়াজ বাড়াও", "জোরে করো", "volume up", "louder")
    private val VOLUME_DOWN      = listOf("ভলিউম কমাও", "আওয়াজ কমাও", "আস্তে করো", "volume down", "lower")
    private val FLASHLIGHT_ON    = listOf("টর্চ জ্বালাও", "আলো জ্বালাও", "টর্চ চালু", "flashlight on", "torch on")
    private val FLASHLIGHT_OFF   = listOf("টর্চ নেভাও", "আলো নেভাও", "টর্চ বন্ধ", "flashlight off", "torch off")
    private val WIFI_ON          = listOf("ওয়াইফাই চালু", "wifi চালু", "wifi on", "wi-fi on")
    private val WIFI_OFF         = listOf("ওয়াইফাই বন্ধ", "wifi বন্ধ", "wifi off", "wi-fi off")
    private val BT_ON            = listOf("ব্লুটুথ চালু", "bluetooth on", "bt on")
    private val BT_OFF           = listOf("ব্লুটুথ বন্ধ", "bluetooth off", "bt off")
    private val REBOOT_PATTERNS  = listOf("রিস্টার্ট করো", "রিবুট করো", "reboot", "restart")
    private val POWER_OFF        = listOf("ফোন বন্ধ করো", "বন্ধ করো ফোন", "power off", "shutdown")
    private val WEBSITE_PATTERNS = listOf("ওয়েবসাইট খোলো", "ব্রাউজ করো", "open website", "go to", "browse")
    private val STATUS_PATTERNS  = listOf("ফোনের অবস্থা", "ব্যাটারি কত", "তাপমাত্রা", "র‍্যাম", "phone status", "battery", "ram", "temperature")
    private val SCREENSHOT_PATTERNS = listOf("স্ক্রিনশট নাও", "স্ক্রিনশট", "screenshot")
    private val SILENT_ON        = listOf("চুপ মোড চালু", "নীরব মোড চালু", "সাইলেন্ট চালু", "silent mode on")
    private val SILENT_OFF       = listOf("চুপ মোড বন্ধ", "নীরব মোড বন্ধ", "সাইলেন্ট বন্ধ", "silent mode off")

    // ✅ FIX: Noise words — app নামের আগে/পরে থাকলে কেটে ফেলবো
    private val NOISE_WORDS = setOf(
        // বাংলা
        "একটু", "একবার", "জলদি", "তাড়াতাড়ি", "please", "প্লিজ", "দয়া করে",
        "একটা", "ভাই", "দোস্ত", "আমার", "আমাকে", "তো", "না", "রে", "তোমার",
        "আমি", "চাই", "দাও", "করো", "কর", "একটু", "একটু করে", "এখন",
        "এখনি", "এখনই", "শীঘ্রই", "সরাসরি", "সাথে সাথে", "আবার", "ফের",
        // English
        "please", "now", "quickly", "fast", "just", "hey", "ok", "okay",
        "can", "you", "open", "me", "show", "the", "my", "bro", "dost"
    )

    // ✅ FIX: Unified alias map — IntentAnalyzer ও AppDetector দুজনেই এটা ব্যবহার করবে
    val APP_ALIASES = mapOf(
        // ─── Social Media ───
        "ফেসবুক"          to "facebook",
        "ফেইসবুক"         to "facebook",
        "facebook lite"   to "facebook lite",
        "ফেসবুক লাইট"     to "facebook lite",
        "ফেইসবুক লাইট"    to "facebook lite",
        "ইউটিউব"          to "youtube",
        "হোয়াটসঅ্যাপ"     to "whatsapp",
        "whatsapp"        to "whatsapp",
        "ইনস্টাগ্রাম"     to "instagram",
        "ইন্সটাগ্রাম"     to "instagram",
        "টিকটক"           to "tiktok",
        "টিক টক"          to "tiktok",
        "টুইটার"           to "twitter",
        "এক্স"             to "twitter",
        "মেসেঞ্জার"        to "messenger",
        "টেলিগ্রাম"        to "telegram",
        "ইমো"              to "imo",
        "স্ন্যাপচ্যাট"     to "snapchat",
        "snapchat"        to "snapchat",
        "লিংকডইন"          to "linkedin",
        "linkedin"        to "linkedin",

        // ─── Google Apps ───
        "গুগল"             to "google",
        "ক্রোম"            to "chrome",
        "জিমেইল"           to "gmail",
        "ম্যাপ"            to "maps",
        "মানচিত্র"         to "maps",
        "google maps"     to "maps",
        "গুগল ম্যাপ"       to "maps",
        "গুগল ম্যাপস"      to "maps",
        "গুগল ড্রাইভ"      to "drive",
        "ড্রাইভ"           to "drive",
        "গুগল ফটো"         to "photos",
        "ফটো"              to "photos",
        "photos"          to "photos",
        "গুগল মিট"         to "meet",
        "meet"            to "meet",
        "play store"      to "play store",
        "প্লে স্টোর"       to "play store",

        // ─── Phone Basics ───
        "ক্যামেরা"         to "camera",
        "গ্যালারি"         to "gallery",
        "ছবি"              to "gallery",
        "ফাইল"             to "files",
        "ফাইল ম্যানেজার"   to "files",
        "ক্যালকুলেটর"      to "calculator",
        "হিসাব"            to "calculator",
        "কন্টাক্ট"         to "contacts",
        "পরিচিতি"          to "contacts",
        "ফোন"              to "phone",
        "ডায়ালার"          to "phone",
        "সেটিং"            to "settings",
        "সেটিংস"           to "settings",
        "settings"        to "settings",
        "ঘড়ি"              to "clock",
        "clock"           to "clock",
        "অ্যালার্ম"        to "clock",
        "alarm"           to "clock",
        "ক্যালেন্ডার"      to "calendar",
        "calendar"        to "calendar",
        "নোট"              to "notes",
        "notes"           to "notes",

        // ─── Entertainment ───
        "স্পটিফাই"         to "spotify",
        "নেটফ্লিক্স"       to "netflix",
        "netflix"         to "netflix",
        "প্রাইম ভিডিও"     to "prime video",
        "amazon prime"    to "prime video",
        "হইচই"             to "hoichoi",
        "চরকি"             to "chorki",
        "বায়োস্কোপ"        to "bioscope",

        // ─── Gaming ───
        "ফ্রি ফায়ার"       to "free fire",
        "free fire"       to "free fire",
        "ফ্রিফায়ার"        to "free fire",
        "গারেনা"           to "free fire",
        "garena"          to "free fire",
        "বিজিএমআই"         to "bgmi",
        "bgmi"            to "bgmi",
        "পাবজি"            to "bgmi",
        "pubg"            to "bgmi",
        "ক্লাশ অফ ক্ল্যান" to "clash of clans",
        "clash of clans"  to "clash of clans",
        "coc"             to "clash of clans",
        "ক্যান্ডি ক্রাশ"   to "candy crush",
        "candy crush"     to "candy crush",
        "রোবলক্স"          to "roblox",
        "roblox"          to "roblox",
        "মাইনক্রাফট"       to "minecraft",
        "minecraft"       to "minecraft",
        "লুডো"             to "ludo",
        "ludo"            to "ludo",

        // ─── Banking / Mobile Financial ───
        "বিকাশ"            to "bkash",
        "bkash"           to "bkash",
        "নগদ"              to "nagad",
        "nagad"           to "nagad",
        "রকেট"             to "rocket",
        "উপায়"             to "upay",
        "upay"            to "upay",

        // ─── Productivity ───
        "মাইক্রোসফট ওয়ার্ড" to "word",
        "word"            to "word",
        "এক্সেল"           to "excel",
        "excel"           to "excel",
        "পাওয়ারপয়েন্ট"    to "powerpoint",
        "powerpoint"      to "powerpoint",
        "জুম"              to "zoom",
        "zoom"            to "zoom"
    )

    fun analyze(text: String): VoiceCommand {
        val lower = text.lowercase().trim()

        // ১. প্রথমে structured command check (Gemini থেকে আসা)
        val structured = parseStructured(text)
        if (structured != null) return structured

        // ২. বাকি pattern matching
        return when {
            FLASHLIGHT_ON.any      { lower.contains(it) } -> VoiceCommand(text, CommandType.FLASHLIGHT_ON)
            FLASHLIGHT_OFF.any     { lower.contains(it) } -> VoiceCommand(text, CommandType.FLASHLIGHT_OFF)
            VOLUME_UP.any          { lower.contains(it) } -> VoiceCommand(text, CommandType.VOLUME_UP)
            VOLUME_DOWN.any        { lower.contains(it) } -> VoiceCommand(text, CommandType.VOLUME_DOWN)
            WIFI_ON.any            { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "WIFI_ON"))
            WIFI_OFF.any           { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "WIFI_OFF"))
            BT_ON.any              { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "BLUETOOTH_ON"))
            BT_OFF.any             { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "BLUETOOTH_OFF"))
            REBOOT_PATTERNS.any    { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "REBOOT"))
            POWER_OFF.any          { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "POWER_OFF"))
            SCREENSHOT_PATTERNS.any{ lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "SCREENSHOT"))
            STATUS_PATTERNS.any    { lower.contains(it) } -> VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "PHONE_STATUS"))
            SILENT_ON.any          { lower.contains(it) } -> VoiceCommand(text, CommandType.SILENT_MODE_ON)
            SILENT_OFF.any         { lower.contains(it) } -> VoiceCommand(text, CommandType.SILENT_MODE_OFF)

            WHATSAPP_CALL.any { lower.contains(it) } -> {
                val name = extractBefore(lower, WHATSAPP_CALL)
                VoiceCommand(text, CommandType.WHATSAPP_CALL, mapOf("name" to name))
            }
            CALL_PATTERNS.any { lower.contains(it) } -> {
                val name = extractBefore(lower, CALL_PATTERNS)
                VoiceCommand(text, CommandType.CALL, mapOf("name" to name))
            }
            MSG_PATTERNS.any { lower.contains(it) } -> {
                val parts = lower.split(" কে ", " to ")
                val name = if (parts.size > 1) parts[1].split(" ")[0] else ""
                VoiceCommand(text, CommandType.WHATSAPP_MSG, mapOf("name" to name, "message" to text))
            }
            YOUTUBE_PATTERNS.any { lower.contains(it) } -> {
                val query = extractAfter(lower, YOUTUBE_PATTERNS)
                if (query.isBlank()) {
                    VoiceCommand(text, CommandType.OPEN_APP, mapOf("app" to "youtube"))
                } else {
                    VoiceCommand(text, CommandType.YOUTUBE_PLAY, mapOf("query" to query))
                }
            }
            SPOTIFY_PATTERNS.any { lower.contains(it) } -> {
                val query = extractAfter(lower, SPOTIFY_PATTERNS)
                VoiceCommand(text, CommandType.SPOTIFY_PLAY, mapOf("query" to query))
            }
            WEBSITE_PATTERNS.any { lower.contains(it) } -> {
                val url = extractAfter(lower, WEBSITE_PATTERNS)
                VoiceCommand(text, CommandType.UNKNOWN, mapOf("action" to "OPEN_WEBSITE", "url" to url))
            }

            // ✅ FIX: OPEN_APP — noise word বাদ দিয়ে app নাম বের করো
            OPEN_PATTERNS.any { lower.contains(it) } -> {
                val rawName = extractAppName(lower, OPEN_PATTERNS)
                val cleanName = removeNoiseWords(rawName)
                val resolvedName = resolveAlias(cleanName)
                VoiceCommand(text, CommandType.OPEN_APP, mapOf("app" to resolvedName))
            }

            else -> VoiceCommand(text, CommandType.CONVERSATION)
        }
    }

    // ✅ FIX: App নাম বের করো — pattern এর আগে বা পরে যেটাই থাকুক
    private fun extractAppName(text: String, patterns: List<String>): String {
        for (p in patterns) {
            val idx = text.indexOf(p)
            if (idx < 0) continue

            val before = text.substring(0, idx).trim()
            val after  = text.substring(idx + p.length).trim()

            if (before.isNotBlank()) return before
            if (after.isNotBlank()) return after
        }
        return ""
    }

    // ✅ FIX: Noise word গুলো app name থেকে সরিয়ে দাও
    // যেমন: "একটু ফেসবুক" → "ফেসবুক", "free fire তো" → "free fire"
    private fun removeNoiseWords(raw: String): String {
        if (raw.isBlank()) return raw
        var result = raw.trim()

        // প্রথমে alias এ direct match চেক করো — noise remove করার দরকার নেই
        val directAlias = APP_ALIASES[result.lowercase()]
        if (directAlias != null) return result

        // Token by token noise word সরাও
        val tokens = result.split(" ").toMutableList()

        // শুরু থেকে noise সরাও
        while (tokens.isNotEmpty() && NOISE_WORDS.contains(tokens.first().lowercase())) {
            tokens.removeAt(0)
        }

        // শেষ থেকে noise সরাও
        while (tokens.isNotEmpty() && NOISE_WORDS.contains(tokens.last().lowercase())) {
            tokens.removeAt(tokens.lastIndex)
        }

        result = tokens.joinToString(" ").trim()

        // ২+ token বাকি থাকলে আবার alias চেক করো (multi-word app name)
        val multiAlias = APP_ALIASES[result.lowercase()]
        if (multiAlias != null) return result

        // শেষ word টা noise হলে সরাও (যেমন "ফেসবুক না" → "ফেসবুক")
        val words = result.split(" ")
        if (words.size > 1 && NOISE_WORDS.contains(words.last().lowercase())) {
            result = words.dropLast(1).joinToString(" ")
        }

        return result.trim()
    }

    private fun extractBefore(text: String, patterns: List<String>): String {
        for (p in patterns) {
            val idx = text.indexOf(p)
            if (idx > 0) return text.substring(0, idx).trim()
        }
        return extractAfter(text, patterns)
    }

    private fun extractAfter(text: String, patterns: List<String>): String {
        for (p in patterns) {
            val idx = text.indexOf(p)
            if (idx >= 0) return text.substring(idx + p.length).trim()
        }
        return ""
    }

    // ✅ বাংলা/ইংরেজি → canonical app name
    fun resolveAlias(name: String): String {
        val lower = name.lowercase().trim()
        return APP_ALIASES[lower] ?: lower
    }

    private fun parseStructured(text: String): VoiceCommand? {
        val t = text.trim()
        return when {
            t.startsWith("OPEN_APP ") -> {
                val appName = t.removePrefix("OPEN_APP ").trim()
                val resolved = resolveAlias(appName)
                VoiceCommand(t, CommandType.OPEN_APP, mapOf("app" to resolved))
            }
            t.startsWith("CALL ")          -> VoiceCommand(t, CommandType.CALL,          mapOf("name"  to t.removePrefix("CALL ").trim()))
            t.startsWith("WHATSAPP_CALL ") -> VoiceCommand(t, CommandType.WHATSAPP_CALL, mapOf("name"  to t.removePrefix("WHATSAPP_CALL ").trim()))
            t.startsWith("YOUTUBE_PLAY ")  -> VoiceCommand(t, CommandType.YOUTUBE_PLAY,  mapOf("query" to t.removePrefix("YOUTUBE_PLAY ").trim()))
            t.startsWith("SPOTIFY_PLAY ")  -> VoiceCommand(t, CommandType.SPOTIFY_PLAY,  mapOf("query" to t.removePrefix("SPOTIFY_PLAY ").trim()))
            t == "FLASHLIGHT_ON"           -> VoiceCommand(t, CommandType.FLASHLIGHT_ON)
            t == "FLASHLIGHT_OFF"          -> VoiceCommand(t, CommandType.FLASHLIGHT_OFF)
            t == "VOLUME_UP"               -> VoiceCommand(t, CommandType.VOLUME_UP)
            t == "VOLUME_DOWN"             -> VoiceCommand(t, CommandType.VOLUME_DOWN)
            t == "SILENT_MODE_ON"          -> VoiceCommand(t, CommandType.SILENT_MODE_ON)
            t == "SILENT_MODE_OFF"         -> VoiceCommand(t, CommandType.SILENT_MODE_OFF)
            t.startsWith("WIFI_") || t.startsWith("BLUETOOTH_") || t == "REBOOT" ||
            t == "POWER_OFF" || t == "SCREENSHOT" || t.startsWith("OPEN_WEBSITE ") ||
            t == "PHONE_STATUS" || t.startsWith("SMS ") || t.startsWith("WHATSAPP_MSG ") -> {
                val parts = t.split(" ", limit = 2)
                VoiceCommand(t, CommandType.UNKNOWN, mapOf("action" to parts[0], "value" to (parts.getOrNull(1) ?: "")))
            }
            else -> null
        }
    }
}

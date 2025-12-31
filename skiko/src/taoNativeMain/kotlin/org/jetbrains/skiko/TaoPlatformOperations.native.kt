package org.jetbrains.skiko

import platform.Foundation.*
import platform.AppKit.*

/**
 * Kotlin/Native implementation of TaoPlatformOperations.
 * Uses direct platform APIs through cinterop.
 */
actual interface TaoPlatformOperations {
    actual fun getSystemTheme(): SystemTheme
    actual fun openUri(uri: String)
    actual fun getClipboardText(): String?
    actual fun setClipboardText(text: String)
    actual fun hasClipboardText(): Boolean
}

/**
 * macOS implementation of TaoPlatformOperations.
 */
object MacOSTaoPlatformOperations : TaoPlatformOperations {
    override fun getSystemTheme(): SystemTheme {
        val appearance = NSApp?.effectiveAppearance ?: return SystemTheme.UNKNOWN
        val bestMatch = appearance.bestMatchFromAppearancesWithNames(
            listOf(NSAppearanceNameAqua, NSAppearanceNameDarkAqua)
        )
        return when (bestMatch) {
            NSAppearanceNameDarkAqua -> SystemTheme.DARK
            NSAppearanceNameAqua -> SystemTheme.LIGHT
            else -> SystemTheme.UNKNOWN
        }
    }

    override fun openUri(uri: String) {
        NSWorkspace.sharedWorkspace.openURL(NSURL.URLWithString(uri)!!)
    }

    override fun getClipboardText(): String? {
        val pasteboard = NSPasteboard.generalPasteboard
        return pasteboard.stringForType(NSPasteboardTypeString)
    }

    override fun setClipboardText(text: String) {
        val pasteboard = NSPasteboard.generalPasteboard
        pasteboard.clearContents()
        pasteboard.setString(text, NSPasteboardTypeString)
    }

    override fun hasClipboardText(): Boolean {
        val pasteboard = NSPasteboard.generalPasteboard
        return pasteboard.types?.contains(NSPasteboardTypeString) == true
    }
}

/**
 * Returns the platform-specific TaoPlatformOperations implementation.
 */
fun getTaoPlatformOperations(): TaoPlatformOperations = MacOSTaoPlatformOperations

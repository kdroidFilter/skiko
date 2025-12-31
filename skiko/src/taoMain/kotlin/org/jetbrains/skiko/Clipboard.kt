package org.jetbrains.skiko

/**
 * Native clipboard functions via NSPasteboard.
 * These provide direct macOS clipboard access without spawning processes.
 */
internal external fun setClipboardTextNative(text: String)
internal external fun getClipboardTextNative(): String?
internal external fun hasClipboardTextNative(): Boolean

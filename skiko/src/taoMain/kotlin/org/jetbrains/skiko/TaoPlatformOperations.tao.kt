package org.jetbrains.skiko

/**
 * JVM implementation of TaoPlatformOperations.
 * Uses native JNI calls for platform-specific functionality.
 */
interface TaoPlatformOperationsJvm {
    fun getSystemTheme(): SystemTheme
    fun openUri(uri: String)
    fun getClipboardText(): String?
    fun setClipboardText(text: String)
    fun hasClipboardText(): Boolean
}

/**
 * Default JVM implementation of TaoPlatformOperations.
 */
object JvmTaoPlatformOperations : TaoPlatformOperationsJvm {
    override fun getSystemTheme(): SystemTheme = currentSystemTheme

    override fun openUri(uri: String) {
        URIHandler_openUri(uri)
    }

    override fun getClipboardText(): String? = ClipboardManager_getText()

    override fun setClipboardText(text: String) {
        ClipboardManager_setText(text)
    }

    override fun hasClipboardText(): Boolean = ClipboardManager_hasText()
}

package org.jetbrains.skiko

/**
 * Common declarations for Tao windowing backend.
 * This file contains shared code between JVM and Kotlin/Native implementations.
 */

/**
 * Tao-specific rendering delegate interface.
 * Extends the standard SkikoRenderDelegate with Tao-specific capabilities.
 */
interface TaoRenderDelegate : SkikoRenderDelegate {
    /**
     * Called when the window is about to be rendered.
     * Allows for pre-render setup.
     */
    fun onPreRender() {}

    /**
     * Called after the render is complete.
     * Allows for post-render cleanup or synchronization.
     */
    fun onPostRender() {}
}

/**
 * Configuration for Tao-based SkiaLayer.
 */
data class TaoLayerConfiguration(
    /**
     * Whether to use vsync for rendering.
     */
    val vsyncEnabled: Boolean = true,

    /**
     * Number of frame buffers (2 = double buffering, 3 = triple buffering).
     */
    val frameBufferCount: Int = 2,

    /**
     * Whether the window should be transparent.
     */
    val transparent: Boolean = false,

    /**
     * The preferred graphics API to use.
     */
    val preferredGraphicsApi: GraphicsApi = GraphicsApi.UNKNOWN
) {
    companion object {
        val Default = TaoLayerConfiguration()
    }
}

/**
 * Represents the state of a Tao window for rendering purposes.
 */
enum class TaoWindowState {
    /**
     * Window is visible and active.
     */
    ACTIVE,

    /**
     * Window is visible but not focused.
     */
    INACTIVE,

    /**
     * Window is minimized or hidden.
     */
    HIDDEN,

    /**
     * Window is being resized.
     */
    RESIZING
}

/**
 * Interface for platform-specific Tao operations.
 * Implementations provide platform-specific functionality.
 */
interface TaoPlatformOperations {
    /**
     * Gets the current system theme.
     */
    fun getSystemTheme(): SystemTheme

    /**
     * Opens a URI using the system's default handler.
     */
    fun openUri(uri: String)

    /**
     * Gets text from the system clipboard.
     */
    fun getClipboardText(): String?

    /**
     * Sets text to the system clipboard.
     */
    fun setClipboardText(text: String)

    /**
     * Checks if the clipboard has text content.
     */
    fun hasClipboardText(): Boolean
}

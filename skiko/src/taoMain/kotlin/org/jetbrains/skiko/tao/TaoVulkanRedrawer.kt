package org.jetbrains.skiko.tao

import io.github.kdroidfilter.taokt.tao.Window
import org.jetbrains.skia.*
import org.jetbrains.skiko.*
import org.jetbrains.skiko.redrawer.Redrawer

/**
 * Vulkan-based redrawer for Tao windows on Linux and Windows.
 */
internal class TaoVulkanRedrawer(
    private val layer: SkiaLayer,
    private val analytics: SkiaLayerAnalytics,
    private val properties: SkiaLayerProperties,
    private val window: Window,
) : Redrawer {

    private val backend = TaoVulkanBackend(window, layer.transparency)
    private val directContext = backend.makeContext()

    override val renderInfo: String
        get() = "Tao Vulkan"

    override fun needRender(throttledToVsync: Boolean) {
        renderImmediately()
    }

    override fun renderImmediately() {
        val size = window.innerSize()
        // innerSize() already returns physical pixels, no need to multiply by scale
        val width = size.width.toInt()
        val height = size.height.toInt()
        if (width <= 0 || height <= 0) return

        backend.resize(width, height)

        val renderTarget = backend.makeRenderTarget(width, height) ?: return
        val surface = Surface.makeFromBackendRenderTarget(
            directContext,
            renderTarget,
            SurfaceOrigin.TOP_LEFT,
            SurfaceColorFormat.BGRA_8888,
            ColorSpace.sRGB,
            SurfaceProps(pixelGeometry = layer.pixelGeometry)
        ) ?: return

        val canvas = surface.canvas
        layer.renderDelegate?.onRender(canvas, width, height, currentNanoTime())
        surface.flushAndSubmit()
        backend.present()
        renderTarget.close()
        surface.close()
    }

    override fun dispose() {
        backend.dispose()
    }

    override fun syncBounds() {
        val size = window.innerSize()
        // innerSize() already returns physical pixels
        backend.resize(size.width.toInt(), size.height.toInt())
    }

    override fun update(nanoTime: Long) {
        // No-op: rendering happens in renderImmediately
    }

    override fun setVisible(isVisible: Boolean) {
        // Layer visibility managed by Tao window
    }
}

/**
 * Native Vulkan backend for Tao windows.
 */
internal class TaoVulkanBackend(
    window: Window,
    transparency: Boolean,
) {
    companion object {
        init {
            Library.load()
        }
    }

    private val devicePtr: Long

    init {
        // Get platform-specific handles
        val rawHandle = window.rawWindowHandle()

        devicePtr = when {
            // Linux X11
            rawHandle.xlibWindow != null && rawHandle.xlibDisplay != null -> {
                createVulkanDeviceX11(
                    rawHandle.xlibWindow!!.toLong(),
                    rawHandle.xlibDisplay!!.toLong(),
                    transparency
                )
            }
            // Linux Wayland
            rawHandle.waylandSurface != null && rawHandle.waylandDisplay != null -> {
                createVulkanDeviceWayland(
                    rawHandle.waylandSurface!!.toLong(),
                    rawHandle.waylandDisplay!!.toLong(),
                    transparency
                )
            }
            // Windows
            rawHandle.hwnd != null && rawHandle.hinstance != null -> {
                createVulkanDeviceWin32(
                    rawHandle.hwnd!!.toLong(),
                    rawHandle.hinstance!!.toLong(),
                    transparency
                )
            }
            else -> error("Vulkan not supported on this platform configuration")
        }

        if (devicePtr == 0L) error("Failed to create Vulkan device")
    }

    fun makeContext(): DirectContext = DirectContext(makeVulkanContext(devicePtr))

    fun makeRenderTarget(width: Int, height: Int): BackendRenderTarget? {
        val ptr = makeVulkanRenderTarget(devicePtr, width, height)
        return if (ptr == 0L) null else BackendRenderTarget(ptr)
    }

    fun present() = finishFrame(devicePtr)

    fun resize(width: Int, height: Int) {
        resizeSwapchain(devicePtr, width, height)
    }

    fun dispose() {
        disposeDevice(devicePtr)
    }

    // Native methods for X11
    private external fun createVulkanDeviceX11(
        xlibWindow: Long,
        xlibDisplay: Long,
        transparency: Boolean
    ): Long

    // Native methods for Wayland
    private external fun createVulkanDeviceWayland(
        waylandSurface: Long,
        waylandDisplay: Long,
        transparency: Boolean
    ): Long

    // Native methods for Windows
    private external fun createVulkanDeviceWin32(
        hwnd: Long,
        hinstance: Long,
        transparency: Boolean
    ): Long

    // Common methods
    private external fun makeVulkanContext(device: Long): Long
    private external fun makeVulkanRenderTarget(device: Long, width: Int, height: Int): Long
    private external fun finishFrame(device: Long)
    private external fun resizeSwapchain(device: Long, width: Int, height: Int)
    private external fun disposeDevice(device: Long)
}

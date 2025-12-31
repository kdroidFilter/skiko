package org.jetbrains.skiko.tao

import io.github.kdroidfilter.taokt.tao.Window
import org.jetbrains.skia.*
import org.jetbrains.skiko.*
import org.jetbrains.skiko.redrawer.Redrawer

/**
 * DirectX 12-based redrawer for Tao windows on Windows.
 */
internal class TaoDirectXRedrawer(
    private val layer: SkiaLayer,
    private val analytics: SkiaLayerAnalytics,
    private val properties: SkiaLayerProperties,
    private val window: Window,
) : Redrawer {

    private val backend = TaoDirectXBackend(
        window,
        layer.transparency,
        properties.frameBuffering.numberOfBuffers() ?: 2
    )
    private val directContext = backend.makeContext()
    private var currentBufferIndex = 0

    override val renderInfo: String
        get() = "Tao DirectX 12"

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

        val renderTarget = backend.makeRenderTarget(width, height, currentBufferIndex) ?: return
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

        // Rotate buffer index
        currentBufferIndex = (currentBufferIndex + 1) % (properties.frameBuffering.numberOfBuffers() ?: 2)
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
 * Native DirectX 12 backend for Tao windows (Windows only).
 */
internal class TaoDirectXBackend(
    window: Window,
    transparency: Boolean,
    frameBuffering: Int,
) {
    companion object {
        init {
            Library.load()
        }
    }

    private val devicePtr: Long

    init {
        val rawHandle = window.rawWindowHandle()

        devicePtr = if (rawHandle.hwnd != null) {
            createDirectXDevice(
                rawHandle.hwnd!!.toLong(),
                transparency,
                frameBuffering
            )
        } else {
            error("DirectX 12 requires Windows HWND handle")
        }

        if (devicePtr == 0L) error("Failed to create DirectX 12 device")
    }

    fun makeContext(): DirectContext = DirectContext(makeDirectXContext(devicePtr))

    fun makeRenderTarget(width: Int, height: Int, bufferIndex: Int): BackendRenderTarget? {
        val ptr = makeDirectXRenderTarget(devicePtr, width, height, bufferIndex)
        return if (ptr == 0L) null else BackendRenderTarget(ptr)
    }

    fun present() = finishFrame(devicePtr)

    fun resize(width: Int, height: Int) {
        resizeSwapchain(devicePtr, width, height)
    }

    fun dispose() {
        disposeDevice(devicePtr)
    }

    private external fun createDirectXDevice(hwnd: Long, transparency: Boolean, frameBuffering: Int): Long
    private external fun makeDirectXContext(device: Long): Long
    private external fun makeDirectXRenderTarget(device: Long, width: Int, height: Int, bufferIndex: Int): Long
    private external fun finishFrame(device: Long)
    private external fun resizeSwapchain(device: Long, width: Int, height: Int)
    private external fun disposeDevice(device: Long)
}

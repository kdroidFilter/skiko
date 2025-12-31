package org.jetbrains.skiko.tao

import io.github.kdroidfilter.taokt.tao.Window
import org.jetbrains.skia.*
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.Library
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkiaLayerProperties
import org.jetbrains.skiko.currentNanoTime
import org.jetbrains.skiko.numberOfBuffers
import org.jetbrains.skiko.redrawer.Redrawer

internal class TaoMetalRedrawer(
    private val layer: SkiaLayer,
    private val analytics: SkiaLayerAnalytics,
    private val properties: SkiaLayerProperties,
    private val window: Window,
) : Redrawer {

    private val backend = TaoMetalBackend(window, layer.transparency, properties.frameBuffering.numberOfBuffers() ?: 0)
    private val directContext = backend.makeContext()
    override val renderInfo: String
        get() = "Tao Metal"

    override fun needRender(throttledToVsync: Boolean) {
        renderImmediately()
    }

    override fun renderImmediately() {
        val size = window.innerSize()
        val scale = window.scaleFactor()
        // innerSize() already returns physical pixels, no need to multiply by scale
        val width = size.width.toInt()
        val height = size.height.toInt()
        if (width <= 0 || height <= 0) return

        backend.resize(width, height, scale.toFloat())

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
        // Pass physical pixel dimensions to render delegate
        layer.renderDelegate?.onRender(canvas, width, height, currentNanoTime())
        surface.flushAndSubmit()
        directContext.flush()
        backend.present()
        renderTarget.close()
        surface.close()
    }

    override fun dispose() {
        backend.dispose()
    }

    override fun syncBounds() {
        val size = window.innerSize()
        val scale = window.scaleFactor()
        // innerSize() already returns physical pixels
        backend.resize(size.width.toInt(), size.height.toInt(), scale.toFloat())
    }

    override fun update(nanoTime: Long) {
        // No-op: rendering happens in renderImmediately
    }

    override fun setVisible(isVisible: Boolean) {
        // Layer visibility managed by Tao window; nothing to do.
    }
}

internal class TaoMetalBackend(
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
        val viewPtr = window.nsViewHandle().toLong()
        val windowPtr = window.nsWindowHandle().toLong()
        devicePtr = createMetalDevice(viewPtr, windowPtr, transparency, frameBuffering)
        if (devicePtr == 0L) error("Failed to create Metal device")
    }

    fun makeContext(): DirectContext = DirectContext(makeMetalContext(devicePtr))

    fun makeRenderTarget(width: Int, height: Int): BackendRenderTarget? {
        val ptr = makeMetalRenderTarget(devicePtr, width, height)
        return if (ptr == 0L) null else BackendRenderTarget(ptr)
    }

    fun present() = finishFrame(devicePtr)

    fun resize(width: Int, height: Int, contentScale: Float) {
        resizeLayer(devicePtr, width, height, contentScale)
    }

    fun dispose() {
        disposeDevice(devicePtr)
    }

    private external fun createMetalDevice(view: Long, window: Long, transparency: Boolean, frameBuffering: Int): Long
    private external fun makeMetalContext(device: Long): Long
    private external fun makeMetalRenderTarget(device: Long, width: Int, height: Int): Long
    private external fun finishFrame(device: Long)
    private external fun resizeLayer(device: Long, width: Int, height: Int, contentScale: Float)
    private external fun disposeDevice(device: Long)
}

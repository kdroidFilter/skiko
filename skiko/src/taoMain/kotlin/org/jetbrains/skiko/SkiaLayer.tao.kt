package org.jetbrains.skiko

import io.github.kdroidfilter.taokt.tao.Window
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skiko.redrawer.Redrawer

/**
 * SkiaLayer implementation backed by Tao windowing.
 */
actual open class SkiaLayer internal constructor(
    val properties: SkiaLayerProperties = SkiaLayerProperties(
        isVsyncEnabled = SkikoProperties.vsyncEnabled,
        isVsyncFramelimitFallbackEnabled = SkikoProperties.vsyncFramelimitFallbackEnabled,
        frameBuffering = SkikoProperties.frameBuffering,
        renderApi = SkikoProperties.renderApi
    ),
    actual val pixelGeometry: PixelGeometry = PixelGeometry.UNKNOWN,
    private val renderFactory: RenderFactory = RenderFactory.Default,
    private val analytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
) {
    /**
     * Public constructor for SkiaLayer with default settings.
     */
    constructor(
        properties: SkiaLayerProperties = SkiaLayerProperties(
            isVsyncEnabled = SkikoProperties.vsyncEnabled,
            isVsyncFramelimitFallbackEnabled = SkikoProperties.vsyncFramelimitFallbackEnabled,
            frameBuffering = SkikoProperties.frameBuffering,
            renderApi = SkikoProperties.renderApi
        ),
        pixelGeometry: PixelGeometry = PixelGeometry.UNKNOWN,
    ) : this(properties, pixelGeometry, RenderFactory.Default, SkiaLayerAnalytics.Empty)

    private var window: Window? = null
    private var redrawer: Redrawer? = null

    actual var renderApi: GraphicsApi = properties.renderApi
        set(value) {
            require(isRenderApiSupported(value)) { "Render API $value is not supported on $hostOs with Tao backend" }
            field = value
        }

    actual val contentScale: Float
        get() = window?.scaleFactor()?.toFloat() ?: 1.0f

    actual var fullscreen: Boolean
        get() = window?.fullscreen() != null
        set(value) {
            window?.setFullscreen(if (value) io.github.kdroidfilter.taokt.tao.Fullscreen.Borderless(null) else null)
        }

    private var _transparency: Boolean = false
    actual var transparency: Boolean
        get() = _transparency
        set(value) { _transparency = value }

    actual val component: Any?
        get() = window

    actual var renderDelegate: SkikoRenderDelegate? = null

    actual fun attachTo(container: Any) {
        check(container is Window) { "Tao SkiaLayer expects a taokt.tao.Window" }
        window = container
        val api = renderApi
        redrawer = renderFactory.createRedrawer(this, api, analytics, properties).also {
            it.syncBounds()
            it.needRender(throttledToVsync = true)
        }
    }

    actual fun detach() {
        redrawer?.dispose()
        redrawer = null
        window = null
    }

    actual fun needRender(throttledToVsync: Boolean) {
        redrawer?.needRender(throttledToVsync)
    }

    @Deprecated(
        message = "Use needRender() instead",
        replaceWith = ReplaceWith("needRender()")
    )
    actual fun needRedraw() = needRender()

    internal actual fun draw(canvas: Canvas) {
        // Render path handled inside the platform redrawer; this is a no-op.
    }

    private fun isRenderApiSupported(api: GraphicsApi): Boolean =
        when (hostOs) {
            OS.MacOS -> api == GraphicsApi.METAL || (SkikoProperties.macOsOpenGLEnabled && api == GraphicsApi.OPENGL)
            OS.Windows -> api == GraphicsApi.DIRECT3D || api == GraphicsApi.VULKAN || api == GraphicsApi.OPENGL
            OS.Linux -> api == GraphicsApi.VULKAN || api == GraphicsApi.OPENGL
            else -> false
        }
}

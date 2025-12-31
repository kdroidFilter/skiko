package org.jetbrains.skiko.tao

import io.github.kdroidfilter.taokt.tao.Window
import org.jetbrains.skia.*
import org.jetbrains.skiko.*
import org.jetbrains.skiko.redrawer.Redrawer

/**
 * OpenGL-based redrawer for Tao windows.
 * This serves as a fallback when Metal/Vulkan/DirectX are not available.
 */
internal class TaoOpenGLRedrawer(
    private val layer: SkiaLayer,
    private val analytics: SkiaLayerAnalytics,
    private val properties: SkiaLayerProperties,
    private val window: Window,
) : Redrawer {

    private val backend = TaoOpenGLBackend(window, layer.transparency)
    private val directContext = backend.makeContext()

    override val renderInfo: String
        get() = "Tao OpenGL"

    override fun needRender(throttledToVsync: Boolean) {
        renderImmediately()
    }

    override fun renderImmediately() {
        val size = window.innerSize()
        // innerSize() already returns physical pixels, no need to multiply by scale
        val width = size.width.toInt()
        val height = size.height.toInt()
        if (width <= 0 || height <= 0) return

        backend.makeCurrent()
        backend.resize(width, height)

        val renderTarget = backend.makeRenderTarget(width, height) ?: return
        val surface = Surface.makeFromBackendRenderTarget(
            directContext,
            renderTarget,
            SurfaceOrigin.BOTTOM_LEFT, // OpenGL uses bottom-left origin
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            SurfaceProps(pixelGeometry = layer.pixelGeometry)
        ) ?: return

        val canvas = surface.canvas
        layer.renderDelegate?.onRender(canvas, width, height, currentNanoTime())
        surface.flushAndSubmit()
        backend.swapBuffers()
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
 * Native OpenGL backend for Tao windows.
 * Supports multiple platforms: macOS (NSOpenGL), Linux (GLX/EGL), Windows (WGL).
 */
internal class TaoOpenGLBackend(
    window: Window,
    transparency: Boolean,
) {
    companion object {
        init {
            Library.load()
        }
    }

    private val contextPtr: Long

    init {
        val rawHandle = window.rawWindowHandle()

        contextPtr = when {
            // macOS - NSOpenGL
            rawHandle.nsView != null -> {
                createOpenGLContextMacOS(rawHandle.nsView!!.toLong(), transparency)
            }
            // Linux X11 - GLX
            rawHandle.xlibWindow != null && rawHandle.xlibDisplay != null -> {
                createOpenGLContextX11(
                    rawHandle.xlibWindow!!.toLong(),
                    rawHandle.xlibDisplay!!.toLong(),
                    transparency
                )
            }
            // Linux Wayland - EGL
            rawHandle.waylandSurface != null && rawHandle.waylandDisplay != null -> {
                createOpenGLContextWayland(
                    rawHandle.waylandSurface!!.toLong(),
                    rawHandle.waylandDisplay!!.toLong(),
                    transparency
                )
            }
            // Windows - WGL
            rawHandle.hwnd != null -> {
                createOpenGLContextWin32(rawHandle.hwnd!!.toLong(), transparency)
            }
            else -> error("OpenGL not supported on this platform configuration")
        }

        if (contextPtr == 0L) error("Failed to create OpenGL context")
    }

    fun makeContext(): DirectContext = DirectContext(makeOpenGLContext(contextPtr))

    fun makeRenderTarget(width: Int, height: Int): BackendRenderTarget? {
        val ptr = makeOpenGLRenderTarget(contextPtr, width, height)
        return if (ptr == 0L) null else BackendRenderTarget(ptr)
    }

    fun makeCurrent() = makeContextCurrent(contextPtr)

    fun swapBuffers() = swapBuffersNative(contextPtr)

    fun resize(width: Int, height: Int) {
        resizeContext(contextPtr, width, height)
    }

    fun dispose() {
        disposeContext(contextPtr)
    }

    // Platform-specific context creation
    private external fun createOpenGLContextMacOS(nsView: Long, transparency: Boolean): Long
    private external fun createOpenGLContextX11(xlibWindow: Long, xlibDisplay: Long, transparency: Boolean): Long
    private external fun createOpenGLContextWayland(waylandSurface: Long, waylandDisplay: Long, transparency: Boolean): Long
    private external fun createOpenGLContextWin32(hwnd: Long, transparency: Boolean): Long

    // Common methods
    private external fun makeOpenGLContext(context: Long): Long
    private external fun makeOpenGLRenderTarget(context: Long, width: Int, height: Int): Long
    private external fun makeContextCurrent(context: Long)
    private external fun swapBuffersNative(context: Long)
    private external fun resizeContext(context: Long, width: Int, height: Int)
    private external fun disposeContext(context: Long)
}

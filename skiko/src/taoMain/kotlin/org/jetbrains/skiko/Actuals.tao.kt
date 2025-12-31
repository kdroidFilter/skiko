package org.jetbrains.skiko

import io.github.kdroidfilter.taokt.tao.CursorIcon
import io.github.kdroidfilter.taokt.tao.Window
import org.jetbrains.skiko.tao.TaoDirectXRedrawer
import org.jetbrains.skiko.tao.TaoMetalRedrawer
import org.jetbrains.skiko.tao.TaoOpenGLRedrawer
import org.jetbrains.skiko.tao.TaoVulkanRedrawer

actual fun setSystemLookAndFeel() {
    // Tao does not rely on Swing LAF; nothing to do.
}

internal actual fun makeDefaultRenderFactory(): RenderFactory =
    RenderFactory { layer, renderApi, analytics, properties ->
        val window = layer.component as? Window
            ?: error("SkiaLayer must be attached to a Tao Window before rendering")

        when (hostOs) {
            OS.MacOS -> when (renderApi) {
                GraphicsApi.METAL -> TaoMetalRedrawer(layer, analytics, properties, window)
                GraphicsApi.OPENGL -> TaoOpenGLRedrawer(layer, analytics, properties, window)
                else -> TaoMetalRedrawer(layer, analytics, properties, window)
            }
            OS.Windows -> when (renderApi) {
                GraphicsApi.DIRECT3D -> TaoDirectXRedrawer(layer, analytics, properties, window)
                GraphicsApi.VULKAN -> TaoVulkanRedrawer(layer, analytics, properties, window)
                GraphicsApi.OPENGL -> TaoOpenGLRedrawer(layer, analytics, properties, window)
                else -> TaoDirectXRedrawer(layer, analytics, properties, window)
            }
            OS.Linux -> when (renderApi) {
                GraphicsApi.VULKAN -> TaoVulkanRedrawer(layer, analytics, properties, window)
                GraphicsApi.OPENGL -> TaoOpenGLRedrawer(layer, analytics, properties, window)
                else -> TaoVulkanRedrawer(layer, analytics, properties, window)
            }
            else -> throw UnsupportedOperationException("Tao backend not supported on ${hostOs}")
        }
    }

internal actual fun URIHandler_openUri(uri: String) {
    // Use platform opener; avoids AWT.
    Runtime.getRuntime().exec(arrayOf("open", uri))
}

internal actual fun ClipboardManager_setText(text: String) {
    setClipboardTextNative(text)
}

internal actual fun ClipboardManager_getText(): String? {
    return getClipboardTextNative()
}

internal actual fun ClipboardManager_hasText(): Boolean {
    return hasClipboardTextNative()
}

actual class Cursor internal constructor(internal val icon: CursorIcon)

internal actual fun CursorManager_setCursor(component: Any, cursor: Cursor) {
    if (component is Window) {
        component.setCursorIcon(cursor.icon)
    }
}

internal actual fun CursorManager_getCursor(component: Any): Cursor? = null

internal actual fun getCursorById(id: PredefinedCursorsId): Cursor =
    when (id) {
        PredefinedCursorsId.DEFAULT -> Cursor(CursorIcon.DEFAULT)
        PredefinedCursorsId.CROSSHAIR -> Cursor(CursorIcon.CROSSHAIR)
        PredefinedCursorsId.HAND -> Cursor(CursorIcon.HAND)
        PredefinedCursorsId.TEXT -> Cursor(CursorIcon.TEXT)
    }

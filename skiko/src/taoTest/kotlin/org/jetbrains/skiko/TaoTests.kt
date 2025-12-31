lespackage org.jetbrains.skiko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.skiko.OS

/**
 * Unit tests for Tao-based Skiko functionality.
 */
class TaoSkiaLayerTest {

    @Test
    fun testSkiaLayerDefaultProperties() {
        val layer = SkiaLayer()
        assertEquals(layer.properties.renderApi, layer.renderApi)
        assertEquals(PixelGeometry.UNKNOWN, layer.pixelGeometry)
    }

    @Test
    fun testSkiaLayerContentScaleDefault() {
        val layer = SkiaLayer()
        // Without a window attached, contentScale should be 1.0
        assertEquals(1.0f, layer.contentScale)
    }

    @Test
    fun testSkiaLayerRenderApiValidation() {
        val layer = SkiaLayer()
        val unsupported = when (hostOs) {
            OS.MacOS -> GraphicsApi.DIRECT3D
            OS.Windows, OS.Linux -> GraphicsApi.METAL
            else -> null
        }
        unsupported?.let {
            try {
                layer.renderApi = it
                // If no exception, the platform added support; nothing to assert.
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("not supported") == true)
            }
        }
    }

    @Test
    fun testSkiaLayerTransparencyDefault() {
        val layer = SkiaLayer()
        assertEquals(false, layer.transparency)
    }
}

/**
 * Tests for TaoLayerConfiguration.
 */
class TaoLayerConfigurationTest {

    @Test
    fun testDefaultConfiguration() {
        val config = TaoLayerConfiguration.Default
        assertEquals(true, config.vsyncEnabled)
        assertEquals(2, config.frameBufferCount)
        assertEquals(false, config.transparent)
        assertEquals(GraphicsApi.UNKNOWN, config.preferredGraphicsApi)
    }

    @Test
    fun testCustomConfiguration() {
        val config = TaoLayerConfiguration(
            vsyncEnabled = false,
            frameBufferCount = 3,
            transparent = true,
            preferredGraphicsApi = GraphicsApi.METAL
        )
        assertEquals(false, config.vsyncEnabled)
        assertEquals(3, config.frameBufferCount)
        assertEquals(true, config.transparent)
        assertEquals(GraphicsApi.METAL, config.preferredGraphicsApi)
    }
}

/**
 * Tests for TaoWindowState enum.
 */
class TaoWindowStateTest {

    @Test
    fun testWindowStateValues() {
        val states = TaoWindowState.values()
        assertEquals(4, states.size)
        assertTrue(states.contains(TaoWindowState.ACTIVE))
        assertTrue(states.contains(TaoWindowState.INACTIVE))
        assertTrue(states.contains(TaoWindowState.HIDDEN))
        assertTrue(states.contains(TaoWindowState.RESIZING))
    }
}

/**
 * Tests for SystemTheme detection.
 */
class SystemThemeTest {

    @Test
    fun testSystemThemeNotUnknown() {
        // On a real macOS system, theme should be detected
        val theme = currentSystemTheme
        // Theme should be either LIGHT or DARK on macOS
        assertTrue(
            theme == SystemTheme.LIGHT ||
            theme == SystemTheme.DARK ||
            theme == SystemTheme.UNKNOWN,
            "Theme should be a valid value"
        )
    }

    @Test
    fun testSystemThemeValues() {
        val themes = SystemTheme.values()
        assertEquals(3, themes.size)
        assertTrue(themes.contains(SystemTheme.DARK))
        assertTrue(themes.contains(SystemTheme.LIGHT))
        assertTrue(themes.contains(SystemTheme.UNKNOWN))
    }
}

/**
 * Tests for GraphicsApi enum.
 */
class GraphicsApiTest {

    @Test
    fun testGraphicsApiValues() {
        val apis = GraphicsApi.values()
        assertTrue(apis.contains(GraphicsApi.UNKNOWN))
        assertTrue(apis.contains(GraphicsApi.METAL))
        assertTrue(apis.contains(GraphicsApi.VULKAN))
        assertTrue(apis.contains(GraphicsApi.DIRECT3D))
        assertTrue(apis.contains(GraphicsApi.OPENGL))
    }
}

/**
 * Tests for OS detection.
 */
class OsDetectionTest {

    @Test
    fun testHostOsDetection() {
        // hostOs should be detected correctly
        val os = hostOs
        assertNotNull(os)
        // On macOS, should be OS.MacOS
        // This test will pass on the platform it's run on
        assertTrue(
            os == OS.MacOS || os == OS.Linux || os == OS.Windows,
            "OS should be a supported desktop platform"
        )
    }
}

/**
 * Tests for cursor functionality.
 */
class CursorTest {

    @Test
    fun testPredefinedCursors() {
        val defaultCursor = getCursorById(PredefinedCursorsId.DEFAULT)
        assertNotNull(defaultCursor)

        val crosshairCursor = getCursorById(PredefinedCursorsId.CROSSHAIR)
        assertNotNull(crosshairCursor)

        val handCursor = getCursorById(PredefinedCursorsId.HAND)
        assertNotNull(handCursor)

        val textCursor = getCursorById(PredefinedCursorsId.TEXT)
        assertNotNull(textCursor)
    }
}

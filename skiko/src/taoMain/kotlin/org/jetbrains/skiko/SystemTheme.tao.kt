package org.jetbrains.skiko

actual val currentSystemTheme: SystemTheme
    get() = when (getCurrentSystemThemeNative()) {
        "dark" -> SystemTheme.DARK
        "light" -> SystemTheme.LIGHT
        else -> SystemTheme.UNKNOWN
    }

private external fun getCurrentSystemThemeNative(): String

package com.pizzamania.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Tomato,
    onPrimary = Color.White,
    secondary = Basil,
    onSecondary = Color.White,
    surface = Mozzarella,
    onSurface = Char,
    background = Gray50,
    onBackground = Char,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = TomatoBright,
    onPrimary = Color.Black,
    secondary = BasilBright,
    onSecondary = Color.Black,
    surface = SurfaceDark2,
    onSurface = OnDarkHigh,
    background = SurfaceDark,
    onBackground = OnDarkHigh,
)

@Composable
fun PizzaManiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PizzaTypography,
        content = content
    )
}

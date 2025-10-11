package com.ak.orbitai.ui.theme

import androidx.compose.ui.graphics.Color

// --- BLACK THEME CORE DEFINITIONS (based on inverted XML colors) ---

// Primary Accent (purple_200 equivalent): Bright purple for prominent elements.
val DarkPrimary = Color(0xFFBB86FC)
// Secondary Accent (teal_200 equivalent): Bright teal for secondary elements.
val DarkSecondary = Color(0xFF03DAC5)

// Main Background (white in XML, which is very dark grey)
val BlackBackground = Color(0xFF121212)
// Main Surface (purple_500 in XML, which is dark grey)
val DarkSurface = Color(0xFF212121)

// Text Color (black in XML, which is white)
val TextOnColor = Color(0xFFFFFFFF)

// --- MATERIAL 3 NAMING CONVENTIONS MAPPED TO THEMES ---

// Dark Scheme Colors (Mapped to Black Theme)
// Purple80: Primary (Bright Accent)
val Purple80 = DarkPrimary
// PurpleGrey80: Secondary (Dark Surface/On-Color contrast)
val PurpleGrey80 = DarkSurface
// Pink80: Tertiary (Secondary Accent)
val Pink80 = DarkSecondary

// Light Scheme Colors (Mapped to Standard M3 Palettes)
val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

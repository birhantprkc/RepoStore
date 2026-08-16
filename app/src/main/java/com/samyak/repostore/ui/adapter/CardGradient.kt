package com.samyak.repostore.ui.adapter

import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils

/** The gradient and matching circle tint for one featured/trending card. */
data class CardPalette(
    val gradient: GradientDrawable,
    val circleTint: Int
)

/**
 * Builds the colour scheme shown behind a featured/trending card when the
 * repository has no banner image.
 *
 * Cards cycle through a full spread of hues — red, amber, green, teal, blue,
 * indigo, purple, pink — so a scrolling list stays visually varied. Rather than
 * hardcoding start/end pairs per hue, every gradient is generated from a single
 * base colour by scaling its lightness. That keeps contrast consistent across
 * all hues, so white text and the scrim behave the same on every card.
 *
 * The gradient runs left to right, dark to light: the text and icon sit on the
 * left over the deeper end, and it resolves into the lighter tone beneath the
 * decorative circles on the right.
 */
object CardGradient {

    /**
     * Saturated base hues spanning the colour wheel. Each yields one card
     * gradient; the list length sets how many cards pass before a hue repeats.
     */
    private val BASE_COLORS = intArrayOf(
        0xFFD32F2F.toInt(), // red
        0xFFF9A825.toInt(), // amber / yellow
        0xFF2E7D32.toInt(), // green
        0xFF00796B.toInt(), // teal
        0xFF1565C0.toInt(), // blue
        0xFF3F51B5.toInt(), // indigo
        0xFF7B1FA2.toInt(), // purple
        0xFFC2185B.toInt()  // pink
    )

    /** Lightness of the darker (left) end, relative to the base colour. */
    private const val START_LIGHTNESS_SCALE = 0.72f

    /** Lightness of the lighter (right) end, relative to the base colour. */
    private const val END_LIGHTNESS_SCALE = 1.34f

    /** Lightness of the circle tint: a pale wash of the card's own hue. */
    private const val CIRCLE_LIGHTNESS = 0.88f

    /** Saturation of the circle tint, kept low so the circles stay a highlight. */
    private const val CIRCLE_SATURATION_SCALE = 0.55f

    fun forPosition(position: Int): CardPalette {
        val base = BASE_COLORS[Math.floorMod(position, BASE_COLORS.size)]

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base, hsl)
        val hue = hsl[0]
        val saturation = hsl[1]
        val lightness = hsl[2]

        val start = ColorUtils.HSLToColor(
            floatArrayOf(hue, saturation, (lightness * START_LIGHTNESS_SCALE).coerceIn(0.12f, 0.9f))
        )
        val end = ColorUtils.HSLToColor(
            floatArrayOf(hue, saturation, (lightness * END_LIGHTNESS_SCALE).coerceIn(0.2f, 0.95f))
        )

        // The circles are a pale version of the same hue, so each card's
        // decoration belongs to that card instead of being a fixed colour.
        val circleTint = ColorUtils.HSLToColor(
            floatArrayOf(hue, saturation * CIRCLE_SATURATION_SCALE, CIRCLE_LIGHTNESS)
        )

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(start, end)
        ).apply { cornerRadius = 0f }

        return CardPalette(gradient, circleTint)
    }
}

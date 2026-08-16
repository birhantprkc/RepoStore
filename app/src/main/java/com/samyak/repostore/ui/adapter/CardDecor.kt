package com.samyak.repostore.ui.adapter

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View

/**
 * Recolours the decorative circles behind a featured/trending card to [tint].
 *
 * SRC_IN keeps the destination alpha, so the graduated 13% / 20% / 28% layering
 * baked into bg_card_circle_decor survives and only the hue changes. That lets
 * one drawable serve every card colour.
 */
fun View.applyCircleTint(tint: Int) {
    backgroundTintMode = PorterDuff.Mode.SRC_IN
    backgroundTintList = ColorStateList.valueOf(tint)
}

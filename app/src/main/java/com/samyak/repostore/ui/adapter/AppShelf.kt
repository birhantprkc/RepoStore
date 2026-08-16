package com.samyak.repostore.ui.adapter

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak.repostore.R

/**
 * Geometry and setup for the two shelf styles the Play Store home page uses.
 * Both scroll horizontally; they differ in what a column contains.
 */
object AppShelf {

    /**
     * [ROWS] stacks [ROW_COUNT] compact rows vertically in each horizontally
     * scrolling column, like the Play Store "For you" list. [TILES] is a single
     * row of artwork tiles, like the "Customise your device" strip.
     */
    enum class Style { ROWS, TILES }

    /** Rows stacked vertically inside one horizontally scrolling column. */
    const val ROW_COUNT = 3

    /** Horizontal padding on the shelf itself, matching the section header. */
    private const val SIDE_PADDING_DP = 8

    /** How much of the next column stays visible as a scroll affordance. */
    private const val PEEK_DP = 40

    /** Below this width only one column fits; wider screens get two. */
    private const val TWO_COLUMN_MIN_WIDTH_DP = 600

    /**
     * Width to assign to a single [Style.ROWS] item so that a partial next
     * column is always visible. Phones show one column plus a peek; tablets
     * show two. Tiles size themselves from their layout instead.
     */
    fun rowWidthPx(context: Context): Int {
        val metrics = context.resources.displayMetrics
        val screenWidthPx = metrics.widthPixels
        val screenWidthDp = screenWidthPx / metrics.density

        val columns = if (screenWidthDp >= TWO_COLUMN_MIN_WIDTH_DP) 2 else 1
        val usablePx = screenWidthPx - dp(context, SIDE_PADDING_DP)

        return (usablePx / columns) - dp(context, PEEK_DP)
    }

    /**
     * Configure a RecyclerView as a shelf of the given [style]. The height is
     * set here rather than in XML because the two styles need different
     * heights while sharing one section layout.
     *
     * Focus is blocked from descendants because a focusable child inside a
     * horizontally scrolling list makes the parent auto-scroll to reveal it,
     * which leaves the shelf resting part-way through the first column instead
     * of at its start.
     */
    fun setup(recyclerView: RecyclerView, style: Style) {
        val context = recyclerView.context

        recyclerView.layoutManager = when (style) {
            Style.ROWS -> GridLayoutManager(context, ROW_COUNT, GridLayoutManager.HORIZONTAL, false)
            Style.TILES -> LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        val heightRes = when (style) {
            Style.ROWS -> R.dimen.shelf_height
            Style.TILES -> R.dimen.shelf_tile_height
        }
        recyclerView.layoutParams = recyclerView.layoutParams.apply {
            height = context.resources.getDimensionPixelSize(heightRes)
        }

        recyclerView.descendantFocusability = RecyclerView.FOCUS_BLOCK_DESCENDANTS
        recyclerView.isNestedScrollingEnabled = false
    }

    /** Default number of apps to put on a shelf. */
    const val DEFAULT_MAX_ITEMS = 12

    /**
     * Cap a list at [maxItems] and, for [Style.ROWS], round down to a whole
     * number of columns so the shelf never ends with a ragged, half-filled
     * column. Tile shelves are a single row and only need capping.
     */
    fun <T> trimToShelf(
        items: List<T>,
        style: Style,
        maxItems: Int = DEFAULT_MAX_ITEMS
    ): List<T> {
        val capped = items.take(maxItems)
        if (style == Style.TILES) return capped

        val wholeColumns = capped.size / ROW_COUNT
        return if (wholeColumns == 0) capped else capped.take(wholeColumns * ROW_COUNT)
    }

    private fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
}

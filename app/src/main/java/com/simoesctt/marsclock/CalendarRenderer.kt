package com.simoesctt.marsclock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

object CalendarRenderer {

    private const val COLS = 7

    fun renderYear(
        ctx: Context,
        container: LinearLayout,
        year: Int,
        todaySol: Long,
        pendingSols: Set<Long>,
        onSolTap: (Long) -> Unit
    ) {
        container.removeAllViews()

        var runningSol = MarsTime.absoluteSolForDate(year, 1, 1)

        for (m in 1..24) {
            val len = MarsTime.monthLength(year, m)
            container.addView(monthView(ctx, year, m, len, runningSol, todaySol, pendingSols, onSolTap))
            runningSol += len
        }
    }

    private fun monthView(
        ctx: Context,
        year: Int,
        month: Int,
        length: Int,
        baseSol: Long,
        todaySol: Long,
        pendingSols: Set<Long>,
        onSolTap: (Long) -> Unit
    ): View {
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(ctx, 8), 0, dp(ctx, 8))
        }

        val header = TextView(ctx).apply {
            text = "${MarsTime.monthName(month)}  —  MY $year"
            setTextColor(Color.parseColor("#E8B27A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(ctx, 4))
        }
        wrap.addView(header)

        val grid = GridLayout(ctx).apply {
            columnCount = COLS
            rowCount = ((length + COLS - 1) / COLS)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for (day in 1..length) {
            val absSol = baseSol + (day - 1)
            val cell = dayCell(ctx, day, absSol == todaySol, absSol in pendingSols)
            cell.setOnClickListener { onSolTap(absSol) }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(ctx, 40)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(ctx, 2), dp(ctx, 2), dp(ctx, 2), dp(ctx, 2))
            }
            cell.layoutParams = lp
            grid.addView(cell)
        }

        wrap.addView(grid)
        return wrap
    }

    private fun dayCell(
        ctx: Context,
        day: Int,
        isToday: Boolean,
        hasPending: Boolean
    ): TextView {
        return TextView(ctx).apply {
            text = day.toString()
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#F5E6D3"))

            val bgColor = when {
                isToday -> Color.parseColor("#C1440E")
                hasPending -> Color.parseColor("#3A1F10")
                else -> Color.parseColor("#1F1208")
            }
            setBackgroundColor(bgColor)

            if (hasPending || isToday) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    private fun dp(ctx: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics).toInt()
}

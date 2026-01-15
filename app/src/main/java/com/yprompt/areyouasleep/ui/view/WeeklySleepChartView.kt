package com.yprompt.areyouasleep.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.yprompt.areyouasleep.data.model.DailyRecord
import java.text.SimpleDateFormat
import java.util.Locale

class WeeklySleepChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private var data: List<Pair<String, Boolean>> = emptyList()

    fun setData(records: List<DailyRecord>) {
        val sorted = records.sortedBy { it.date }.takeLast(7)
        data = sorted.map {
            val dateStr = try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val formatter = SimpleDateFormat("dd", Locale.US)
                formatter.format(parser.parse(it.date)!!)
            } catch (e: Exception) { "??" }
            Pair(dateStr, it.isStayUpLate)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val barWidth = width / 9f
        val spacing = (width - (barWidth * data.size)) / (data.size + 1)
        val maxBarHeight = height * 0.7f

        data.forEachIndexed { index, (date, isStayUpLate) ->
            val left = spacing + index * (barWidth + spacing)
            val right = left + barWidth

            barPaint.color = if (isStayUpLate) Color.parseColor("#FF3B30") else Color.parseColor("#34C759")

            val top = height - maxBarHeight - 40f
            val bottom = height - 40f

            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, barPaint)
            canvas.drawText(date, left + barWidth / 2, height - 10f, textPaint)
        }
    }
}

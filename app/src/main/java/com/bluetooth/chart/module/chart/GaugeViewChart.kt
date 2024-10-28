package com.bluetooth.chart.module.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.bluetooth.chart.R
import kotlin.math.cos
import kotlin.math.sin

class GaugeViewChart(context: Context, attrs: AttributeSet) : View(context, attrs) {
    // 페인트 초기화
    private val baseGaugePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 50f
        isAntiAlias = true
    }

    private val gaugePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 50f
        isAntiAlias = true
    }

    private val needlePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 10f
        isAntiAlias = true
    }

    var value = 0f
        set(value) {
            field = value
            invalidate() // 화면 갱신
        }

    init {
        // 커스텀 속성 적용
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.GaugeViewChart, 0, 0)
        try {
            // 기본 색상 및 게이지 색상 적용
            baseGaugePaint.color = typedArray.getColor(R.styleable.GaugeViewChart_baseGaugeColor, Color.GRAY)
            gaugePaint.color = typedArray.getColor(R.styleable.GaugeViewChart_gaugeColor, Color.GREEN)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        // 게이지의 범위 그리기 (기본 배경색)
        val rect = RectF(60f, 60f, width - 60f, height+80f)
        canvas.drawArc(rect, 180f, 180f, false, baseGaugePaint) // 전체 게이지

        // 값에 해당하는 부분 그리기 (동적 색상)
        val sweepAngle = (value / 100f) * 180f
        canvas.drawArc(rect, 180f, sweepAngle, false, gaugePaint) // 값에 따른 게이지

//        // 바늘 그리기 (수정된 위치 및 각도 계산)
//        val angle = 180f + sweepAngle
//        val centerX = width / 2f
//        val centerY = height - 50f // 바늘의 중심 Y 좌표 조정
//        val needleLength = width / 2.5f
//        val needleX = (centerX + cos(Math.toRadians(angle.toDouble())) * needleLength).toFloat()
//        val needleY = (centerY + sin(Math.toRadians(angle.toDouble())) * needleLength).toFloat()
//
//        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint) // 바늘 그리기

    }
}

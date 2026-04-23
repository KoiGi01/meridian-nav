package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class CustomKeyboardView(context: Context) : LinearLayout(context) {
    var onKey: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onSpace: (() -> Unit)? = null

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val density = resources.displayMetrics.density

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.CENTER
        val pad = (16 * density).toInt()
        setPadding(pad, pad, pad, pad)

        val rows = listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M", "⌫"),
            listOf("␣")
        )

        val btnHeight = (52 * density).toInt()
        val margin = (3 * density).toInt()

        for (rowKeys in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in rowKeys) {
                val isModifier = key == "⌫" || key == "␣"
                val btn = TextView(context).apply {
                    text = key
                    setTextColor(
                        if (isModifier) Color.parseColor("#B8F7FF")
                        else Color.parseColor("#E8FBFF")
                    )
                    textSize = if (key == "␣") 18f else 16f
                    typeface = monoTypeface
                    gravity = Gravity.CENTER
                    isClickable = true
                    letterSpacing = 0.2f

                    background = CyberBracketDrawable(
                        legLengthPx = 5f * density,
                        strokeWidthPx = 1f * density,
                        strokeColor = Color.parseColor(
                            if (isModifier) "#B8F7FF" else "#00E5FF"
                        ),
                        fillColor = Color.parseColor(
                            if (isModifier) "#33002030" else "#4D000A14"
                        )
                    )

                    setOnTouchListener { v, ev ->
                        when (ev.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                v.alpha = 0.55f
                                v.scaleX = 0.94f; v.scaleY = 0.94f
                            }
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
                            }
                        }
                        false
                    }

                    setOnClickListener {
                        when (key) {
                            "⌫" -> onBackspace?.invoke()
                            "␣" -> onSpace?.invoke()
                            else -> onKey?.invoke(key)
                        }
                    }
                }

                var w = (44 * density).toInt()
                if (key == "⌫") w = (92 * density).toInt()
                if (key == "␣") w = (280 * density).toInt()

                rowLayout.addView(btn, LayoutParams(w, btnHeight).apply {
                    setMargins(margin, margin, margin, margin)
                })
            }
            addView(rowLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }
}

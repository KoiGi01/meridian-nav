package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R
import com.mapbox.geojson.Point

data class SearchResult(val name: String, val address: String, val point: Point)

class SearchOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onQuery: ((String) -> Unit)? = null
    var onResultSelected: ((SearchResult) -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val searchField: EditText
    private val resultsList: LinearLayout
    private val debouncer = Handler(Looper.getMainLooper())
    private var pendingQuery: Runnable? = null
    private val customKeyboard: CustomKeyboardView

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.parseColor("#F2000A10"))
        translationY = 2000f
        visibility = View.GONE
        isMotionEventSplittingEnabled = false
        weightSum = 2f
        val outerPad = dipToPx(24)
        setPadding(outerPad, outerPad, outerPad, outerPad)

        // LEFT PANEL (Search & Results)
        val leftPanel = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        val searchContainer = FrameLayout(context).apply {
            background = CyberBracketDrawable(
                legLengthPx = 14 * resources.displayMetrics.density,
                strokeWidthPx = 1.5f * resources.displayMetrics.density,
                strokeColor = Color.parseColor("#00E5FF"),
                fillColor = Color.parseColor("#66001820")
            )
        }
        leftPanel.addView(searchContainer, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(72)).apply {
            bottomMargin = dipToPx(12)
        })

        searchField = EditText(context).apply {
            hint = "> DESTINATION_QUERY"
            setHintTextColor(Color.parseColor("#6600E5FF"))
            setTextColor(Color.parseColor("#E0FBFF"))
            textSize = 18f
            typeface = monoTypeface
            letterSpacing = 0.15f
            background = null
            setPadding(dipToPx(28), 0, dipToPx(28), 0)
            showSoftInputOnFocus = false // Disable native keyboard
            isFocusableInTouchMode = true
        }
        searchContainer.addView(searchField, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.CENTER_VERTICAL })

        resultsList = LinearLayout(context).apply { orientation = VERTICAL }
        val scroller = ScrollView(context).apply { addView(resultsList) }
        leftPanel.addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        addView(leftPanel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        // RIGHT PANEL (Custom Keyboard)
        customKeyboard = CustomKeyboardView(context)
        customKeyboard.onKey = { key ->
            val curr = searchField.text.toString()
            searchField.setText(curr + key)
            searchField.setSelection(curr.length + 1)
        }
        customKeyboard.onSpace = {
            val curr = searchField.text.toString()
            searchField.setText(curr + " ")
            searchField.setSelection(curr.length + 1)
        }
        customKeyboard.onBackspace = {
            val curr = searchField.text.toString()
            if (curr.isNotEmpty()) {
                searchField.setText(curr.dropLast(1))
                searchField.setSelection(curr.length - 1)
            }
        }
        
        val rightPanel = FrameLayout(context)
        rightPanel.addView(customKeyboard, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER })

        addView(rightPanel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { onQueryChanged(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun onQueryChanged(query: String) {
        pendingQuery?.let { debouncer.removeCallbacks(it) }
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            resultsList.removeAllViews()
            return
        }
        pendingQuery = Runnable { onQuery?.invoke(trimmed) }.also {
            debouncer.postDelayed(it, 250)
        }
    }

    fun showResults(results: List<SearchResult>) {
        resultsList.removeAllViews()
        val d = resources.displayMetrics.density
        results.forEachIndexed { idx, result ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dipToPx(22), dipToPx(14), dipToPx(22), dipToPx(14))
                background = CyberBracketDrawable(
                    legLengthPx = 6f * d,
                    strokeWidthPx = 1f * d,
                    strokeColor = Color.parseColor(if (idx == 0) "#00E5FF" else "#6600E5FF"),
                    fillColor = Color.parseColor("#33000A12")
                )
                setOnClickListener {
                    onResultSelected?.invoke(result)
                    dismiss()
                }
            }
            row.addView(TextView(context).apply {
                text = "› ${result.name}"
                setTextColor(Color.parseColor("#E8FBFF"))
                textSize = 15f
                typeface = monoTypeface
                letterSpacing = 0.08f
            })
            if (result.address.isNotEmpty()) {
                row.addView(TextView(context).apply {
                    text = result.address
                    setTextColor(Color.parseColor("#7A00E5FF"))
                    textSize = 11f
                    typeface = monoTypeface
                    letterSpacing = 0.12f
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
            }
            resultsList.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * d).toInt() })
        }
    }

    fun show() {
        visibility = View.VISIBLE
        translationY = height.toFloat().coerceAtLeast(600f)
        animate().translationY(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
        searchField.requestFocus()
    }

    fun dismiss() {
        searchField.text.clear()
        animate().translationY(height.toFloat().coerceAtLeast(600f)).setDuration(300)
            .withEndAction { visibility = View.GONE; onDismissed?.invoke() }.start()
    }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}

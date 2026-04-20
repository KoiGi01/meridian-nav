package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

data class SearchResult(val name: String, val address: String, val distanceKm: Float)

class SearchOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onResultSelected: ((SearchResult) -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val searchField: EditText
    private val resultsList: LinearLayout

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#F0000000"))
        translationY = 2000f
        visibility = View.GONE

        val searchContainer = FrameLayout(context).apply {
            val border = GradientDrawable().apply {
                setColor(Color.parseColor("#1A000000"))
                setStroke(1, Color.parseColor("#6b0919"))
                cornerRadius = 0f
            }
            background = border
        }
        addView(searchContainer, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(56)))

        searchField = EditText(context).apply {
            hint = "DESTINATION"
            setHintTextColor(Color.parseColor("#446b0919"))
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = monoTypeface
            background = null
            setPadding(dipToPx(16), 0, dipToPx(16), 0)
        }
        searchContainer.addView(searchField, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.CENTER_VERTICAL })

        resultsList = LinearLayout(context).apply { orientation = VERTICAL }
        addView(resultsList, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { onQueryChanged(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun onQueryChanged(query: String) {
        if (query.length >= 2) {
            showResults(listOf(
                SearchResult(query.uppercase(), "Search via Mapbox Search API", 0f)
            ))
        } else {
            resultsList.removeAllViews()
        }
    }

    fun showResults(results: List<SearchResult>) {
        resultsList.removeAllViews()
        results.forEach { result ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dipToPx(16), dipToPx(14), dipToPx(16), dipToPx(14))
                minimumHeight = dipToPx(56)
                val border = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(0, Color.parseColor("#336b0919"))
                }
                background = border
                setOnClickListener {
                    onResultSelected?.invoke(result)
                    dismiss()
                }
            }
            row.addView(TextView(context).apply {
                text = result.name
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = monoTypeface
            })
            row.addView(TextView(context).apply {
                text = result.address
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 10f
                typeface = monoTypeface
            })
            resultsList.addView(row)
            resultsList.addView(View(context).apply {
                setBackgroundColor(Color.parseColor("#1A6b0919"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            })
        }
    }

    fun show() {
        visibility = View.VISIBLE
        translationY = height.toFloat().coerceAtLeast(600f)
        animate().translationY(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
        searchField.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT)
    }

    fun dismiss() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(searchField.windowToken, 0)
        animate().translationY(height.toFloat().coerceAtLeast(600f)).setDuration(300)
            .withEndAction { visibility = View.GONE; onDismissed?.invoke() }.start()
    }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}

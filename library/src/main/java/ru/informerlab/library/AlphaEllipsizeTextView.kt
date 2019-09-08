package ru.informerlab.library

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat

private const val NEW_LINE_SYMBOL = "\n"
private const val UNBROKEN_SPACE = "\u00A0"

open class AlphaEllipsizeTextView(
        context: Context,
        attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private var mLinearGradientWidth: Int = 0
    private val mHiddenAreaPercent = 0.7

    var originalText: CharSequence = ""

    private var mEllipsizedTextSetting = false

    private val alphaEllipsizePaint: Paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        if (!mEllipsizedTextSetting) {
            originalText = text ?: ""
        }
        super.setText(text, type)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mEllipsizedTextSetting = true
        if (layout != null)
            text = getPreparedTextToViewBounds(text)
        mEllipsizedTextSetting = false
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null && originalText != text) {
            initPaintLinearGradient(width)
            canvas.drawRect(
                    (width * mHiddenAreaPercent).toFloat(),
                    (height - height / maxLines).toFloat(),
                    width.toFloat(),
                    height.toFloat(),
                    alphaEllipsizePaint)
        }
    }

    private fun initPaintLinearGradient(width: Int) {
        if (mLinearGradientWidth != width) {
            mLinearGradientWidth = width
            alphaEllipsizePaint.shader = LinearGradient(
                    (width * mHiddenAreaPercent).toFloat(),
                    0f,
                    (width - width * 0.01).toFloat(),
                    0f,
                    ContextCompat.getColor(context, android.R.color.white),
                    ContextCompat.getColor(context, android.R.color.transparent),
                    Shader.TileMode.CLAMP
            )
        }
    }

    private fun getPreparedTextToViewBounds(text: CharSequence): CharSequence {
        val oneLineLayout = initStaticLayout(
                text,
                paint,
                paint.measureText(text.toString()).toInt(),
                alignment = layout.alignment,
                spacingMult = layout.spacingMultiplier,
                spacingAdd = layout.spacingAdd
        )
        val maxCountSymbolsInLine = oneLineLayout.getOffsetForHorizontal(0, layout.width.toFloat())
        return if (isTruncated()) {
            if (maxLines > 1) {
                prepareTextToMultiLineDraw(text.toString(), maxCountSymbolsInLine, maxLines)
            } else {
                text.toString()
                        .substring(0, maxCountSymbolsInLine)
                        .replace(" ", UNBROKEN_SPACE)
            }
        } else {
            text
        }
    }

    private fun prepareTextToMultiLineDraw(
            text: String,
            maxCountSymbolsInLine: Int,
            maxLinesCount: Int
    ): String {
        val words = getWordsList(text)
        val resultString = StringBuilder()
        var newLineSymbolCount = 0
        words.forEach { word ->
            val lineLength = resultString.substring(
                    getLastLineSymbolPos(resultString),
                    resultString.length
            ).length
            if (resultString.isNotEmpty()) {
                if (lineLength + word.length < maxCountSymbolsInLine
                        || newLineSymbolCount >= maxLinesCount - 1) {
                    resultString.append(UNBROKEN_SPACE)
                } else {
                    newLineSymbolCount++
                    resultString.append(NEW_LINE_SYMBOL)
                }
            }
            resultString.append(word)
        }
        return resultString.toString()
    }

    private fun isTruncated() = maxLines > 0 && layout?.lineCount != null && layout.lineCount > maxLines

    private fun getLastLineSymbolPos(string: StringBuilder) =
            if (string.lastIndexOf(NEW_LINE_SYMBOL) == -1)
                0
            else
                string.lastIndexOf(NEW_LINE_SYMBOL)

    private fun getWordsList(text: String): MutableList<String> =
            text.split(" ", NEW_LINE_SYMBOL, UNBROKEN_SPACE)
                    .toMutableList()
                    .apply {
                        removeAll {
                            it == NEW_LINE_SYMBOL || it == " " || it == UNBROKEN_SPACE || it == ""
                        }
                    }

    private fun initStaticLayout(
            text: CharSequence,
            textPaint: TextPaint,
            width: Int,
            start: Int = 0,
            end: Int = text.length,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
            spacingMult: Float = 1f,
            spacingAdd: Float = 0f
    ): StaticLayout =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder
                        .obtain(text, start, end, textPaint, width)
                        .setAlignment(alignment)
                        .setLineSpacing(spacingAdd, spacingMult)
                        .build()
            } else {
                @Suppress("DEPRECATION")
                (StaticLayout(
                        text,
                        start,
                        end,
                        textPaint,
                        width,
                        alignment,
                        spacingMult,
                        spacingAdd,
                        false

                ))
            }
}
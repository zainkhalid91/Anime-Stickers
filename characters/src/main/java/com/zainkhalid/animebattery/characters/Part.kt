package com.zainkhalid.animebattery.characters

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp

/**
 * One layer of a character (hair, eyes, a mouth...). Authored as an [ImageVector],
 * then flattened once into plain Paths with their paint, so drawing it every frame
 * allocates nothing. All parts of a character share one viewport so they line up.
 */
class Part(val vector: ImageVector) {

    private class Shape(
        val path: Path,
        val fill: Brush?,
        val fillAlpha: Float,
        val stroke: Brush?,
        val strokeAlpha: Float,
        val strokeStyle: Stroke?,
    )

    private val shapes: List<Shape> = buildList { collect(vector.root, this) }

    val shapeCount get() = shapes.size

    private fun collect(group: VectorGroup, out: MutableList<Shape>) {
        for (node in group) when (node) {
            is VectorGroup -> collect(node, out)
            is VectorPath -> out += Shape(
                path = node.pathData.toPath(),
                fill = node.fill,
                fillAlpha = node.fillAlpha,
                stroke = node.stroke,
                strokeAlpha = node.strokeAlpha,
                strokeStyle = if (node.stroke != null && node.strokeLineWidth > 0f) {
                    Stroke(node.strokeLineWidth, node.strokeLineMiter, node.strokeLineCap, node.strokeLineJoin)
                } else null,
            )
        }
    }

    /** Draws just the outline of every shape with [brush], used for sticker borders and shadows. */
    fun drawSilhouette(scope: DrawScope, brush: Brush, style: Stroke, fill: Boolean, alpha: Float = 1f) {
        for (s in shapes) {
            if (s.fill == null) continue
            if (fill) scope.drawPath(s.path, brush, alpha = alpha)
            scope.drawPath(s.path, brush, alpha = alpha, style = style)
        }
    }

    /** Draws in viewport units; the caller sets up the transform. */
    fun draw(scope: DrawScope, alpha: Float = 1f) {
        if (alpha <= 0f) return
        for (s in shapes) {
            s.fill?.let { scope.drawPath(s.path, it, alpha = s.fillAlpha * alpha) }
            if (s.stroke != null && s.strokeStyle != null) {
                scope.drawPath(s.path, s.stroke, alpha = s.strokeAlpha * alpha, style = s.strokeStyle)
            }
        }
    }
}

/**
 * Tiny builder so parts can be written as SVG path strings:
 *
 *     part(20f, 28f) {
 *         shape("M3,11 L1,8 ... Z", fill = Hair, stroke = Ink, width = 1.3f)
 *     }
 */
class PartBuilder(private val builder: ImageVector.Builder) {

    fun shape(
        d: String,
        fill: Color? = null,
        stroke: Color? = null,
        width: Float = 0f,
        alpha: Float = 1f,
        cap: StrokeCap = StrokeCap.Round,
        join: StrokeJoin = StrokeJoin.Round,
    ) = shape(d, fill?.let(::SolidColor), stroke?.let(::SolidColor), width, alpha, cap, join)

    /** Same, with any brush (gradients use viewport coordinates). */
    fun shape(
        d: String,
        fill: Brush?,
        stroke: Brush?,
        width: Float = 0f,
        alpha: Float = 1f,
        cap: StrokeCap = StrokeCap.Round,
        join: StrokeJoin = StrokeJoin.Round,
    ) {
        builder.addPath(
            // Fresh parser per shape: toNodes() can hand back the parser's own list.
            pathData = PathParser().parsePathString(d).toNodes().toList(),
            fill = fill,
            fillAlpha = alpha,
            stroke = stroke,
            strokeAlpha = alpha,
            strokeLineWidth = width,
            strokeLineCap = cap,
            strokeLineJoin = join,
        )
    }

    /** Outline-only line work (whiskers, closed eyes, smiles). */
    fun line(d: String, color: Color, width: Float, alpha: Float = 1f) =
        shape(d, fill = null as Brush?, stroke = SolidColor(color), width = width, alpha = alpha)
}

fun part(width: Float, height: Float, name: String = "part", block: PartBuilder.() -> Unit): Part {
    val b = ImageVector.Builder(
        name = name,
        defaultWidth = width.dp,
        defaultHeight = height.dp,
        viewportWidth = width,
        viewportHeight = height,
    )
    PartBuilder(b).block()
    return Part(b.build())
}

/** SVG path for an ellipse, using two arcs. */
fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float = rx): String =
    "M${cx - rx},$cy A$rx,$ry 0 1,0 ${cx + rx},$cy A$rx,$ry 0 1,0 ${cx - rx},$cy Z"

/** Mirrors x coordinates of an absolute-command SVG path around [axis]. Good for left/right pairs. */
fun mirrorX(d: String, axis: Float): String {
    // Only handles M/L/Q/C/Z with absolute "x,y" pairs, which is all the art uses.
    val num = Regex("(-?\\d*\\.?\\d+),(-?\\d*\\.?\\d+)")
    return num.replace(d) { m ->
        val x = m.groupValues[1].toFloat()
        "${2 * axis - x},${m.groupValues[2]}"
    }
}

package oolong.client.ui.widgets.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

/** Where a Chinese character's square sits on its line: from this far above the baseline… */
private const val EmBoxTop = 0.88f

/** …to this far below it. */
private const val EmBoxBottom = 0.12f

/**
 * Lays a text out as tall as the characters it draws, from the top of its first line's
 * characters to the bottom of its last line's, rather than with the room its font and line
 * height leave above and below them. Spacing set around it is then the space seen, at every
 * text size. [fontSize] is the text's size; [top] and [bottom] choose which edges to trim.
 */
fun Modifier.trimToCharacters(fontSize: TextUnit, top: Boolean = true, bottom: Boolean = true): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0))
        val size = fontSize.toPx()
        val first = placeable[FirstBaseline]
        val last = placeable[LastBaseline]
        val above = if (top && first != AlignmentLine.Unspecified) (first - EmBoxTop * size).roundToInt().coerceAtLeast(0) else 0
        val below = if (bottom && last != AlignmentLine.Unspecified) (placeable.height - last - EmBoxBottom * size).roundToInt().coerceAtLeast(0) else 0
        layout(placeable.width, (placeable.height - above - below).coerceAtLeast(0)) {
            placeable.place(0, -above)
        }
    }

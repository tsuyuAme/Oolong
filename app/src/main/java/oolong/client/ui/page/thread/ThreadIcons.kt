package oolong.client.ui.page.thread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 只看楼主's icon: "LZ" in a rounded box, as a box and its letters apart, so that on, with the box
 * filled, the letters can be drawn in the colour under the icon and read as cut out of it. Drawn
 * on Material Symbols' 24dp grid with their stroke weight.
 */
internal object LzIcon {
    val Box: ImageVector by lazy { lz("LzBox", BoxPath, fill = false) }
    val BoxFill: ImageVector by lazy { lz("LzBoxFill", BoxPath, fill = true) }
    val Letters: ImageVector by lazy { lz("LzLetters", LettersPath, fill = false) }

    private const val BoxPath =
        "M6.2,5H17.8A3.6,3.6 0 0 1 21.4,8.6V15.4A3.6,3.6 0 0 1 17.8,19H6.2A3.6,3.6 0 0 1 2.6,15.4V8.6A3.6,3.6 0 0 1 6.2,5Z"
    private const val LettersPath = "M7,8.6V15.4H10.6M13.2,8.6H17.4L13.2,15.4H17.4"

    private fun lz(name: String, path: String, fill: Boolean) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(path),
            fill = if (fill) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}

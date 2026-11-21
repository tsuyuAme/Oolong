package oolong.client.ui.page.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import oolong.client.R
import oolong.client.model.HotTopic
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.TextStyles

/** 「热」 or 「新」 beside a topic, in a tint of its own colour; nothing for a topic without either. */
@Composable
fun TopicTag(tag: HotTopic.Tag, modifier: Modifier = Modifier) {
    val (text, color) = when (tag) {
        HotTopic.Tag.Hot -> R.string.topic_tag_hot to HotColor
        HotTopic.Tag.New -> R.string.topic_tag_new to NewColor
        HotTopic.Tag.None -> return
    }
    Text(
        text = stringResource(text),
        style = TextStyles.micro,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), Corners.xs)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

private val HotColor = Color(0xFFE5484D)
private val NewColor = Color(0xFF3D7BE0)

package oolong.client.api.models.protos

import oolong.client.utils.EmoticonManager

val List<Abstract>.abstractText: String
    get() = joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(Regex(" {2,}"), " ")
            4 -> it.text

            else -> ""
        }
    }

private val TEXT_ABSTRACT_TYPES = setOf(0, 40)

val ThreadInfo.abstractText: String
    get() = richAbstract.joinToString(separator = "") {
        when (it.type) {
            in TEXT_ABSTRACT_TYPES -> it.text.replace(Regex(" {2,}"), " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                "#(${it.c})"
            }

            else -> ""
        }
    }

val PostInfoList.abstractText: String
    get() = rich_abstract.joinToString(separator = "") {
        when (it.type) {
            in TEXT_ABSTRACT_TYPES -> it.text.replace(Regex(" {2,}"), " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                "#(${it.c})"
            }

            else -> ""
        }
    }

/**
 * Some responses leave author_id as 0 and only fill author.id.
 */
val Post.resolvedAuthorId: Long
    get() = author_id.takeIf { it != 0L } ?: author?.id ?: 0L

/**
 * Some responses leave author_id as 0 and only fill author.id.
 */
val SubPostList.resolvedAuthorId: Long
    get() = author_id.takeIf { it != 0L } ?: author?.id ?: 0L

val ThreadInfo.hasAgree: Int
    get() = agree?.hasAgree ?: 0
val ThreadInfo.hasAgreed: Boolean
    get() = hasAgree == 1
val ThreadInfo.hasAbstract: Boolean
    get() = richAbstract.any { (it.type in TEXT_ABSTRACT_TYPES && it.text.isNotBlank()) || it.type == 2 }

fun ThreadInfo.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree!!.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agreeNum = agreeNum + 1,
                agree = agree!!.copy(
                    agreeNum = agree!!.agreeNum + 1,
                    diffAgreeNum = agree!!.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agreeNum = agreeNum - 1,
                agree = agree!!.copy(
                    agreeNum = agree!!.agreeNum - 1,
                    diffAgreeNum = agree!!.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    copy(
        agreeNum = if (hasAgree == 1) agreeNum + 1 else agreeNum - 1
    )
}

fun ThreadInfo.updateCollectStatus(
    newStatus: Int,
    markPostId: Long
) = if (collectStatus != newStatus) {
    this.copy(
        collectStatus = newStatus,
        collectMarkPid = markPostId.toString()
    )
} else {
    this
}

fun PostInfoList.updateAgreeStatus(
    hasAgree: Int,
) = if (agree != null) {
    if (hasAgree != agree!!.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree!!.copy(
                    agreeNum = agree!!.agreeNum + 1,
                    diffAgreeNum = agree!!.diffAgreeNum + 1,
                    hasAgree = 1
                ),
                agree_num = agree_num + 1
            )
        } else {
            copy(
                agree = agree!!.copy(
                    agreeNum = agree!!.agreeNum - 1,
                    diffAgreeNum = agree!!.diffAgreeNum - 1,
                    hasAgree = 0
                ),
                agree_num = agree_num - 1
            )
        }
    } else {
        this
    }
} else {
    this
}

val User.bawuType: String?
    get() = if (is_bawu == 1) {
        if (bawu_type == "manager") "吧主" else "小吧主"
    } else null

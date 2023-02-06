package ru.yandex.market.logistics.mqm.utils

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode

fun createYtRow(data: Map<String, Any?>): YTreeMapNode {
    val mappedValues = data.mapValues { (_, value) ->
        if (value == null) {
            return@mapValues YTreeEntityNodeImpl(null)
        }

        when (value) {
            is String -> YTreeStringNodeImpl(value, null)
            is Long -> YTreeIntegerNodeImpl(true, value, null)
            else -> throw IllegalArgumentException("Unsupported type ${value::class}")
        }
    }

    return YTreeMapNodeImpl(mappedValues, null)
}

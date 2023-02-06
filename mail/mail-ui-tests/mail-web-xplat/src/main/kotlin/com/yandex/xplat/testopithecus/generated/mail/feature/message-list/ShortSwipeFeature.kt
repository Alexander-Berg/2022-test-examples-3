// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/message-list/short-swipe-feature.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.testopithecus.common.Feature

public open class ShortSwipeFeature private constructor(): Feature<ShortSwipe>("ShortSwipe", "Архивирование/Удаление через короткий свайп") {
    companion object {
        @JvmStatic var `get`: ShortSwipeFeature = ShortSwipeFeature()
    }
}

public interface ShortSwipe {
    fun deleteMessageByShortSwipe(order: Int): Unit
    fun archiveMessageByShortSwipe(order: Int): Unit
    fun markAsRead(order: Int): Unit
    fun markAsUnread(order: Int): Unit
}

// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/message-list/group-mode-feature.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.testopithecus.common.Feature

public open class GroupModeFeature private constructor(): Feature<GroupMode>("GroupMode", "Действия с письмами в режиме групповых операций." + "InitialSelectMessage переводит в компонент GroupMode и производится по лонг тапу." + "SelectMessage выделяет письма, если мы уже в режиме групповых операций по обычному тапу.") {
    companion object {
        @JvmStatic var `get`: GroupModeFeature = GroupModeFeature()
    }
}

public interface GroupMode {
    fun getNumberOfSelectedMessages(): Int
    fun isInGroupMode(): Boolean
    fun selectMessage(byOrder: Int): Unit
    fun selectAllMessages(): Unit
    fun initialMessageSelect(byOrder: Int): Unit
    fun getSelectedMessages(): YSSet<Int>
    fun markAsReadSelectedMessages(): Unit
    fun markAsUnreadSelectedMessages(): Unit
    fun deleteSelectedMessages(): Unit
    fun applyLabelsToSelectedMessages(labelNames: YSArray<LabelName>): Unit
    fun removeLabelsFromSelectedMessages(labelNames: YSArray<LabelName>): Unit
    fun markAsImportantSelectedMessages(): Unit
    fun markAsUnImportantSelectedMessages(): Unit
    fun markAsSpamSelectedMessages(): Unit
    fun markAsNotSpamSelectedMessages(): Unit
    fun moveToFolderSelectedMessages(folderName: FolderName): Unit
    fun archiveSelectedMessages(): Unit
    fun unselectMessage(byOrder: Int): Unit
    fun unselectAllMessages(): Unit
}

// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/messages-list/group-mode-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.YSSet

public open class GroupModeModel(private var markableModel: MarkableReadModel, private var deleteMessageModel: DeleteMessageModel, private var archive: ArchiveMessageModel, private var important: MarkableImportantModel, private var spam: SpamableModel, private var moveToFolder: MovableToFolderModel, private var messageListDisplay: MessageListDisplayModel, private var label: LabelModel): GroupMode {
    var selectedOrders: YSSet<Int> = YSSet<Int>()
    open override fun getNumberOfSelectedMessages(): Int {
        val selectedMessages = this.messageListDisplay.getMidsByOrders(this.selectedOrders)
        return selectedMessages.size
    }

    open override fun getSelectedMessages(): YSSet<Int> {
        return this.selectedOrders
    }

    open override fun isInGroupMode(): Boolean {
        return this.selectedOrders.size != 0
    }

    open override fun markAsReadSelectedMessages(): Unit {
        for (order in this.selectedOrders.values()) {
            this.markableModel.markAsRead(order)
        }
        this.selectedOrders = YSSet<Int>()
    }

    open override fun markAsUnreadSelectedMessages(): Unit {
        for (order in this.selectedOrders.values()) {
            this.markableModel.markAsUnread(order)
        }
        this.selectedOrders = YSSet<Int>()
    }

    open override fun deleteSelectedMessages(): Unit {
        this.deleteMessageModel.deleteMessages(this.selectedOrders)
        this.selectedOrders = YSSet<Int>()
    }

    open override fun selectMessage(byOrder: Int): Unit {
        this.selectedOrders.add(byOrder)
    }

    open override fun archiveSelectedMessages(): Unit {
        this.archive.archiveMessages(this.selectedOrders)
        this.selectedOrders = YSSet<Int>()
    }

    open override fun markAsImportantSelectedMessages(): Unit {
        for (order in this.selectedOrders.values()) {
            this.important.markAsImportant(order)
        }
        this.selectedOrders = YSSet<Int>()
    }

    open override fun markAsNotSpamSelectedMessages(): Unit {
        this.spam.moveFromSpamMessages(this.selectedOrders)
        this.selectedOrders = YSSet<Int>()
    }

    open override fun markAsSpamSelectedMessages(): Unit {
        this.spam.moveToSpamMessages(this.selectedOrders)
        this.selectedOrders = YSSet<Int>()
    }

    open override fun markAsUnImportantSelectedMessages(): Unit {
        for (order in this.selectedOrders.values()) {
            this.important.markAsUnimportant(order)
        }
        this.selectedOrders = YSSet<Int>()
    }

    open override fun moveToFolderSelectedMessages(folderName: FolderName): Unit {
        this.moveToFolder.moveMessagesToFolder(this.selectedOrders, folderName)
        this.selectedOrders = YSSet<Int>()
    }

    open override fun unselectAllMessages(): Unit {
        this.selectedOrders = YSSet<Int>()
    }

    open override fun unselectMessage(byOrder: Int): Unit {
        this.selectedOrders.delete(byOrder)
    }

    open fun copy(): GroupModeModel {
        val copy = GroupModeModel(this.markableModel, this.deleteMessageModel, this.archive, this.important, this.spam, this.moveToFolder, this.messageListDisplay, this.label)
        copy.selectedOrders = this.selectedOrders
        return copy
    }

    open override fun initialMessageSelect(byOrder: Int): Unit {
        this.selectedOrders = YSSet<Int>(mutableListOf(byOrder))
    }

    open override fun selectAllMessages(): Unit {
        val storedMessages: YSArray<Int> = mutableListOf()
        for (i in (0 until this.messageListDisplay.getMessageList(20).size step 1)) {
            storedMessages.add(i)
        }
        this.selectedOrders = YSSet<Int>(storedMessages)
    }

    open override fun applyLabelsToSelectedMessages(labelNames: YSArray<LabelName>): Unit {
        val mids = this.messageListDisplay.getMidsByOrders(this.selectedOrders)
        this.label.applyLabelsToMessages(YSSet(mids), labelNames)
        this.selectedOrders = YSSet<Int>()
    }

    open override fun removeLabelsFromSelectedMessages(labelNames: YSArray<LabelName>): Unit {
        val mids = this.messageListDisplay.getMidsByOrders(this.selectedOrders)
        this.label.removeLabelsFromMessages(YSSet(mids), labelNames)
        this.selectedOrders = YSSet<Int>()
    }

}

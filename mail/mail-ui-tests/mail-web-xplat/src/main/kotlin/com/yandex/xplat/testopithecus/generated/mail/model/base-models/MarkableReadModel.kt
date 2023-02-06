// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/base-models/markable-read-model.ts >>>

package com.yandex.xplat.testopithecus

public open class MarkableReadModel(private var model: MessageListDisplayModel, private var accHandler: MailAppModelHandler): MarkableRead {
    open override fun markAsRead(order: Int): Unit {
        for (mid in this.model.getThreadByOrder(order)) {
            this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read = true
        }
    }

    open override fun markAsUnread(order: Int): Unit {
        for (mid in this.model.getThreadByOrder(order)) {
            this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read = false
        }
    }

}

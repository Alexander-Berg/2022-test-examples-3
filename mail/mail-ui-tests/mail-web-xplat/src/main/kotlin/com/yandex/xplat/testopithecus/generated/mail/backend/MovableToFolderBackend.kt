// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/backend/movable-to-folder-backend.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.filter

public open class MovableToFolderBackend(private var mailListDisplayBackend: MessageListDisplayBackend, private var clientsHandler: MailboxClientHandler): MovableToFolder {
    open override fun moveMessageToFolder(order: Int, folderName: String): Unit {
        val tid = this.mailListDisplayBackend.getThreadMessage(order).tid!!
        val fid = this.clientsHandler.getCurrentClient().getFolderList().filter( {
            f ->
            f.name == folderName
        })[0].fid
        this.clientsHandler.getCurrentClient().moveThreadToFolder(tid, fid)
    }

}

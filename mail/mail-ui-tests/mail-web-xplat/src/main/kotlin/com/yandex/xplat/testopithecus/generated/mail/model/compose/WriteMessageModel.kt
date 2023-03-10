// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/compose/write-message-model.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.testopithecus.common.currentTimeMs

public open class WriteMessageModel(private var accHandler: MailAppModelHandler, private var messageNavigator: OpenMessageModel, private var compose: ComposeMessageModel, val accountDataHandler: MailAppModelHandler, var wysiwig: WysiwygModel): WriteMessage {
    open override fun openCompose(): Unit {
        this.compose.composeDraft = Draft(this.wysiwig)
    }

    open override fun sendMessage(to: String, subject: String): Unit {
        this.createAndAddSentReceivedMessage(to, subject)
    }

    open override fun replyMessage(): Unit {
        val openedMessage = this.accHandler.getCurrentAccount().messagesDB.storedMessage(this.messageNavigator.openedMessage).head
        val msgSentThread = this.createAndAddSentReceivedMessage(openedMessage.from, "Re: " + openedMessage.subject)
        this.accHandler.getCurrentAccount().messagesDB.addThreadMessagesToThreadWithMid(msgSentThread, this.messageNavigator.openedMessage)
    }

    open fun copy(): WriteMessageModel {
        return WriteMessageModel(this.accHandler, this.messageNavigator, this.compose, this.accountDataHandler, this.wysiwig)
    }

    open override fun sendPrepared(): Unit {
        val draft = this.compose.getDraft()
        val sentSubject = if (draft.subject == null) "" else draft.subject!!
        val sentTo = if (draft.to.size == 0) YSSet<String>() else draft.to
        val wysiwyg = draft.getWysiwyg()
        val sentBody: String = wysiwyg.getRichBody()
        val sentMsg = FullMessage(Message(this.accountDataHandler.getCurrentAccount().aliases[0], sentSubject, int64(YSDate.now()), sentBody, 1, true), sentTo, sentBody)
        val messages = this.accHandler.getCurrentAccount().messagesDB.getMessages()
        val sentMsgMid = int64(messages.size)
        this.accHandler.getCurrentAccount().messagesDB.addMessage(sentMsgMid, sentMsg, DefaultFolderName.sent)
        var isToIsSelf = false
        for (to in sentTo.values()) {
            if (this.accountDataHandler.getCurrentAccount().aliases.contains(to)) {
                isToIsSelf = true
            }
        }
        if (isToIsSelf) {
            val receivedMsg = FullMessage(Message(this.accountDataHandler.getCurrentAccount().aliases[0], sentSubject, int64(YSDate.now()), sentBody, 1, true), sentTo, sentBody)
            val receivedMsgMid = int64(messages.size)
            this.accHandler.getCurrentAccount().messagesDB.addMessage(receivedMsgMid, receivedMsg, DefaultFolderName.inbox)
            this.accHandler.getCurrentAccount().messagesDB.addThread(mutableListOf(sentMsgMid, receivedMsgMid))
        }
        this.compose.composeDraft = null
    }

    private fun createAndAddSentReceivedMessage(to: String, subject: String): YSArray<MessageId> {
        val sentFakeMid = this.createOnlySentMessage(to, subject)
        if (this.accountDataHandler.getCurrentAccount().aliases.contains(this.canonicalEmail(to))) {
            val selfEmail = this.accountDataHandler.getCurrentAccount().aliases[0]
            val timestamp = currentTimeMs()
            val receivedFakeMid = int64(this.accHandler.getCurrentAccount().messagesDB.getMessages().size + 1)
            val receivedMessage = FullMessage(Message(to, subject, timestamp + int64(1), "", 2, false), YSSet<String>(mutableListOf(selfEmail)))
            this.accHandler.getCurrentAccount().messagesDB.addMessage(receivedFakeMid, receivedMessage, DefaultFolderName.inbox)
            this.accHandler.getCurrentAccount().messagesDB.addThread(mutableListOf(sentFakeMid, receivedFakeMid))
            return mutableListOf(sentFakeMid, receivedFakeMid)
        }
        return mutableListOf(sentFakeMid)
    }

    private fun canonicalEmail(email: String): String {
        var cnt = 0
        for (i in (0 as Int until email.length step 1)) {
            if (email.slice(i, i + 1) == "-") {
                cnt += 1
            }
        }
        if (cnt <= 1) {
            return email
        }
        val pos = email.lastIndexOf("-")
        return "${email.slice(0, pos)}.${email.slice(pos + 1, email.length)}"
    }

    private fun createOnlySentMessage(to: String, subject: String): MessageId {
        val selfEmail = this.accountDataHandler.getCurrentAccount().aliases[0]
        val timestamp = currentTimeMs()
        val sentMessage = FullMessage(Message(selfEmail, subject, timestamp, "", 2, true), YSSet<String>(mutableListOf(to)))
        val msgSentMid = int64(this.accHandler.getCurrentAccount().messagesDB.getMessages().size + 1)
        this.accHandler.getCurrentAccount().messagesDB.addMessage(msgSentMid, sentMessage, DefaultFolderName.sent)
        return msgSentMid
    }

}


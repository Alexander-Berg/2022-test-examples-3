// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM events/push-events.ts >>>

package com.yandex.xplat.eventus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.eventus.common.ValueMapBuilder

public open class PushEvents {
    open fun messagesReceivedPushShown(uid: Long, fid: Long, mids: YSArray<Long>, repliesNumbers: YSArray<Int>? = null): EventusEvent {
        return EventusEvent.newClientEvent(EventNames.PUSH_MESSAGES_RECEIVED_SHOWN, ValueMapBuilder.systemEvent().addUid(uid).addFid(fid).addMids(mids).addRepliesNumbers(repliesNumbers))
    }

    open fun singleMessagePushClicked(uid: Long, mid: Long, fid: Long, repliesNumber: Int? = null): EventusEvent {
        return EventusEvent.newClientEvent(EventNames.PUSH_SINGLE_MESSAGE_CLICKED, ValueMapBuilder.userEvent().addUid(uid).addMid(mid).addFid(fid).addRepliesNumber(repliesNumber))
    }

    open fun replyMessagePushClicked(uid: Long, mid: Long, fid: Long, repliesNumber: Int? = null): EventusEvent {
        return EventusEvent.newClientEvent(EventNames.PUSH_REPLY_MESSAGE_CLICKED, ValueMapBuilder.userEvent().addUid(uid).addMid(mid).addFid(fid).addRepliesNumber(repliesNumber))
    }

    open fun smartReplyMessagePushClicked(uid: Long, mid: Long, fid: Long, order: Int, repliesNumber: Int? = null): EventusEvent {
        return EventusEvent.newClientEvent(EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED, ValueMapBuilder.userEvent().addUid(uid).addMid(mid).addFid(fid).addOrder(order).addRepliesNumber(repliesNumber))
    }

    open fun threadPushClicked(uid: Long, mids: YSArray<Long>, fid: Long, tid: Long, repliesNumber: Int? = null): EventusEvent {
        return EventusEvent.newClientEvent(EventNames.PUSH_THREAD_CLICKED, ValueMapBuilder.userEvent().addUid(uid).addMids(mids).addTid(tid).addFid(fid).addRepliesNumber(repliesNumber))
    }

    open fun folderPushClicked(uid: Long, mids: YSArray<Long>, fid: Long): EventusEvent {
        return EventusEvent.newClientEvent(EventNames.PUSH_FOLDER_CLICKED, ValueMapBuilder.userEvent().addUid(uid).addMids(mids).addFid(fid))
    }

}

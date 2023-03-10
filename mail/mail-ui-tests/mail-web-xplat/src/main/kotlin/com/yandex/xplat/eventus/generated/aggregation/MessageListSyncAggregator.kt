// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM aggregation/message-list-sync-aggregator.ts >>>

package com.yandex.xplat.eventus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.YSMap
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.eventus.common.Aggregator
import com.yandex.xplat.eventus.common.EventusEvent
import com.yandex.xplat.eventus.common.MessageDTO

public open class MessageListSyncAggregator: Aggregator {
    private var sentMessageIds: YSSet<Long> = YSSet<Long>()
    open override fun accept(event: EventusEvent): EventusEvent? {
        if (!this.accepts(event)) {
            return null
        }
        val messages = event.getAttributes().`get`("messages") as YSArray<Any?>
        val messagesDTO: YSArray<MessageDTO> = mutableListOf()
        for (anyMessage in messages) {
            val message = anyMessage as YSMap<String, Any>
            val messageDTO = MessageDTO.fromMap(message)
            if (!this.sentMessageIds.has(messageDTO.mid)) {
                this.sentMessageIds.add(messageDTO.mid)
                messagesDTO.add(messageDTO)
            }
        }
        return if (messagesDTO.size == 0) null else Eventus.modelSyncEvents.updateMessageList(messagesDTO)
    }

    open override fun accepts(event: EventusEvent): Boolean {
        return event.name == EventNames.MODEL_SYNC_MESSAGE_LIST
    }

    open override fun finalize(): EventusEvent? {
        return null
    }

}


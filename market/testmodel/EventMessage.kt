package ru.yandex.market.logistics.les.objectmapper.testmodel

import com.amazon.sqs.javamessaging.message.SQSTextMessage
import ru.yandex.market.logistics.les.base.Event

class EventMessage(eventStr: String) : SQSTextMessage(eventStr) {
    init {
        setStringProperty("_type", Event::class.java.name)
    }
}

package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import org.mockito.kotlin.mock
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent

fun createMessages(vararg events: LogisticEvent): List<MessageData> {
    return events
        .map { it.toByteArray() }
        .map { data ->
            MessageData(
                data,
                0,
                mock { on(it.codec).thenReturn(CompressionCodec.RAW) }
            )
        }
}

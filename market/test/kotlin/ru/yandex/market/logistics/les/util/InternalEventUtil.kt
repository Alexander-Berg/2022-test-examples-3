package ru.yandex.market.logistics.les.util

import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.mapper.component.MapperFactory
import ru.yandex.market.logistics.les.model.InternalEvent

private val clientObjectMapper = MapperFactory.getClientObjectMapper()

fun Event.toInternal(): InternalEvent = InternalEvent(
    rawEvent = clientObjectMapper.writeValueAsString(this),
    source = this.source!!,
    eventId = this.eventId!!,
    timestamp = this.timestamp!!,
    eventType = this.eventType!!,
    version = this.version!!,
    sensitive = this.payload?.isSensitive() ?: false,
    description = this.description,
    entityKeys = this.entityKeys ?: listOf()
)

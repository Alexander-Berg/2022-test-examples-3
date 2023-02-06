package ru.yandex.market.logistics.les.objectmapper.testmodel

import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload
import java.util.Optional

data class OptionalEventPayload(
    val optional: Optional<String>? = null,
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

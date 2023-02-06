package ru.yandex.market.logistics.les.objectmapper.testmodel

import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload
import java.time.Instant

data class InstantPayload(
    val instant: Instant? = null,
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

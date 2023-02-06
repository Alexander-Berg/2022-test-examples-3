package ru.yandex.market.logistics.les.objectmapper.testmodel

import org.springframework.data.geo.Point
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload

data class GeoPayload(
    val point: Point? = null,
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

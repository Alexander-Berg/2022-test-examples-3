package ru.yandex.market.logistics.les.objectmapper.testmodel

import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload

data class NoDefaultConstructorPayload(
    val str: String,
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

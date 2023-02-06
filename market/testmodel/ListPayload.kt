package ru.yandex.market.logistics.les.objectmapper.testmodel

import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload

data class ListPayload(
    val list: List<String> = emptyList()
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

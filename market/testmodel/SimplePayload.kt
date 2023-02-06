package ru.yandex.market.logistics.les.objectmapper.testmodel

import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EntityType
import ru.yandex.market.logistics.les.base.EventPayload

data class SimplePayload(
    val simpleType: SimplePayloadType? = null,
): EventPayload {
    override fun getEntityKeys(): List<EntityKey> = listOf(EntityKey("simpleId", EntityType.ORDER))
}

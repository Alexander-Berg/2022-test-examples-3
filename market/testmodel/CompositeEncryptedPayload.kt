package ru.yandex.market.logistics.les.objectmapper.testmodel

import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload
import ru.yandex.market.logistics.les.base.crypto.EncryptedString

data class CompositeEncryptedPayload(
    val key: EncryptedString,
    val value: String
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
    override fun isSensitive(): Boolean = true
}

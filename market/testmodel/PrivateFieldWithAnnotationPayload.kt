package ru.yandex.market.logistics.les.objectmapper.testmodel

import com.fasterxml.jackson.annotation.JsonProperty
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload

data class PrivateFieldWithAnnotationPayload(
    @JsonProperty("str")
    private val str: String? = null,
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

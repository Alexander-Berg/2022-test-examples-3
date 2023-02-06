package ru.yandex.market.logistics.les.objectmapper.testmodel

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload

data class NoDefaultConstructorWithAnnotationsPayload @JsonCreator constructor(
    @JsonProperty("string")
    val str: String,
) : EventPayload {
    override fun getEntityKeys(): List<EntityKey> = emptyList()
}

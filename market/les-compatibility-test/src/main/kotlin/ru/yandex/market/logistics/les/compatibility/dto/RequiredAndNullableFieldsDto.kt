package ru.yandex.market.logistics.les.compatibility.dto

import com.fasterxml.jackson.annotation.JsonProperty
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload

data class RequiredAndNullableFieldsDto constructor(
    @JsonProperty("requiredString") val requiredString: String,
    @JsonProperty("nullableString") val nullableString: String?,
    @JsonProperty("requiredList") val requiredList: List<Int>,
    @JsonProperty("nullableList") val nullableList: List<Int>?
) : EventPayload {

    override fun getEntityKeys(): List<EntityKey> = emptyList()

    companion object {
        val TEST_OBJECT = RequiredAndNullableFieldsDto(
            "requiredStr",
            null,
            listOf(5),
            null
        )
    }
}

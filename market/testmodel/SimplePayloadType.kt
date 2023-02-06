package ru.yandex.market.logistics.les.objectmapper.testmodel

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class SimplePayloadType {
    @JsonEnumDefaultValue
    SIMPLE,
    VERY_SIMPLE,
}

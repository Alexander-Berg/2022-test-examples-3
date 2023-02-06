package ru.yandex.market.tpl.e2e.data.feature.testcase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FilterExpressionDto(
    @Json(name = "type") val type: String,
    @Json(name = "key") val key: String,
    @Json(name = "value") val value: Any,
)

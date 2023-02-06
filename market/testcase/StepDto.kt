package ru.yandex.market.tpl.e2e.data.feature.testcase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StepDto(
    @Json(name = "step") val step: String,
    @Json(name = "expect") val expect: String,
)
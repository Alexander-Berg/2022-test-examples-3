package ru.yandex.market.tpl.e2e.data.feature.testcase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TestCaseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "stepsExpects") val stepsExpects: List<StepDto>,
    @Json(name = "preconditions") val preconditions: String,
)
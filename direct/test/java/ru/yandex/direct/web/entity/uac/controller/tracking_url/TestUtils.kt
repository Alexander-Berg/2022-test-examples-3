package ru.yandex.direct.web.entity.uac.controller.tracking_url

import org.assertj.core.api.Assertions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.utils.JsonUtils

const val URL_STR = "url"
const val APP_ID_STR = "app_id"
const val APP_INFO_ID_STR = "app_info_id"
const val VALIDATION_TYPE_STR = "validation_type"
const val TRACKING_URL_STR = "tracking_url"
const val REDIRECT_URL_STR = "redirect_url"
const val IMPRESSION_URL_STR = "impression_url"

fun checkCorrectRequest(mockMvc: MockMvc, request: Map<String, Any?>, expectedResponse: Map<String, Any?>) {
    val result = mockMvc.perform(
        MockMvcRequestBuilders
            .post("/uac/tracking_url/validate")
            .content(JsonUtils.toJson(request))
            .contentType(MediaType.APPLICATION_JSON)
    )
    val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
        .andReturn()
        .response
        .contentAsString
    Assertions.assertThat(JsonUtils.MAPPER.readTree(resultContent)["result"]).isEqualTo(
        JsonUtils.MAPPER.readTree(
            JsonUtils.toJson(expectedResponse)
        )
    )
}

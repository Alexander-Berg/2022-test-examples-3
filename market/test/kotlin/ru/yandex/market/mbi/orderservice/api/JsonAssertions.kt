package ru.yandex.market.mbi.orderservice.api

import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.junit.jupiter.api.fail
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import ru.yandex.market.mbi.helpers.defaultTestMapper

fun assertBody(response: HttpResponse, expected: String) {
    val bodyAsString = IOUtils.toString(response.entity.content)
    val result = defaultTestMapper.readTree(bodyAsString).get("result")?.toString()
        ?: fail { "Field 'result' is required in response body'" }
    JSONAssert.assertEquals(
        expected,
        result,
        JSONCompareMode.STRICT_ORDER
    )
}

fun assertErrorResponse(response: HttpResponse, expected: String) {
    val bodyAsString = IOUtils.toString(response.entity.content)
    val tree = defaultTestMapper.readTree(bodyAsString)
    require(tree.get("result") == null) {
        "Error response should not contain 'result' field"
    }
    val errors = defaultTestMapper.readTree(bodyAsString).get("errors")?.toString()
        ?: fail { "Field 'errors' is required in response body'" }
    JSONAssert.assertEquals(
        expected,
        errors,
        JSONCompareMode.STRICT_ORDER
    )
}

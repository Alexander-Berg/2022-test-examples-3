package ru.yandex.direct.jooqmapper.jsonread

import com.fasterxml.jackson.databind.ObjectMapper
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.jooqmapper.jsonread.JsonReaderBuilders.jsonNodeToInteger
import ru.yandex.direct.jooqmapper.jsonread.JsonReaderBuilders.jsonNodeToLong
import ru.yandex.direct.jooqmapper.jsonread.JsonReaderBuilders.readBigDecimalAnyOfType
import java.math.BigDecimal

@RunWith(JUnitParamsRunner::class)
internal class JsonReaderBuildersTest {
    companion object {

        private val mapper = ObjectMapper()

        private fun parseJson(jsonString: String) =
            mapper.readTree(jsonString)
    }

    fun bigDecimalTestCases() = listOf(
        listOf("Читать числовое целое", """{"value": 1000}""", BigDecimal.valueOf(1000)),
        listOf("Читать строковое целое", """{"value": "1000"}""", BigDecimal.valueOf(1000)),
        listOf("Читать строковое дробное", """{"value": "4.44"}""", BigDecimal.valueOf(4.44)),
        listOf("Читать числовое дробное", """{"value": 4.44}""", BigDecimal.valueOf(4.44)),
        listOf("Читать строчное дробное 0.001", """{"value": "0.001"}""", BigDecimal.valueOf(0.001)),
        listOf("Читать числовое дробное", """{"value": 0.001}""", BigDecimal.valueOf(0.001)),
        listOf("Читать числовое целое отрицательное", """{"value": -1000}""", BigDecimal.valueOf(-1000)),
        listOf("Читать строковое целое отрицательное", """{"value": "-1000"}""", BigDecimal.valueOf(-1000)),
        listOf("Читать строковое дробное отрицательно", """{"value": "-4.44"}""", BigDecimal.valueOf(-4.44)),
        listOf("Читать числовое дробное отрицательно", """{"value": -4.44}""", BigDecimal.valueOf(-4.44)),
        listOf("Читать ноль числовой", """{"value": 0}""", BigDecimal.ZERO),
        listOf("Читать ноль строковый", """{"value": "0"}""", BigDecimal.ZERO),
        listOf("Обрабатывать null", """{"value": null}""", null),
        listOf("Обрабатывать отсутствующее значение", """{}""", null),
    )

    fun longTestCases() = listOf(
        listOf("Читать числовое целое", """{"value": 1000}""", 1000L),
        listOf("Читать строковое целое", """{"value": "1000"}""", 1000L),
        listOf("Читать строковое целое Long.Max", """{"value": "${Long.MAX_VALUE}"}""", Long.MAX_VALUE),
        listOf("Читать строковое целое Long.Min", """{"value": "${Long.MIN_VALUE}"}""", Long.MIN_VALUE),
        listOf("Читать числовое целое отрицательное", """{"value": -1000}""", -1000L),
        listOf("Читать строковое целое отрицательное", """{"value": "-1000"}""", -1000L),
        listOf("Читать ноль числовой", """{"value": 0}""", 0L),
        listOf("Читать ноль строковый", """{"value": "0"}""", 0L),
        listOf("Обрабатывать null", """{"value": null}""", null),
        listOf("Обрабатывать отсутствующее значение", """{}""", null),
    )

    fun intTestCases() = listOf(
        listOf("Читать числовое целое", """{"value": 1000}""", 1000),
        listOf("Читать строковое целое", """{"value": "1000"}""", 1000),
        listOf("Читать строковое целое Int.Max", """{"value": "${Int.MAX_VALUE}"}""", Int.MAX_VALUE),
        listOf("Читать строковое целое Int.Min", """{"value": "${Int.MIN_VALUE}"}""", Int.MIN_VALUE),
        listOf("Читать числовое целое отрицательное", """{"value": -1000}""", -1000),
        listOf("Читать строковое целое отрицательное", """{"value": "-1000"}""", -1000),
        listOf("Читать ноль числовой", """{"value": 0}""", 0),
        listOf("Читать ноль строковый", """{"value": "0"}""", 0),
        listOf("Обрабатывать null", """{"value": null}""", null),
        listOf("Обрабатывать отсутствующее значение", """{}""", null),
    )

    fun bigDecimalNegativeTestCases() = listOf(
        listOf("Падать на нечисловом значении - строка", """{"value": "not numeric"}"""),
        listOf("Падать на нечисловом значении -массив", """{"value": [1, 2, 3]}"""),
    )

    @Test
    @Parameters(method = "bigDecimalTestCases")
    @TestCaseName("{0}")
    fun `correctly read big decimals`(description: String,
                                      jsonString: String,
                                      expected: BigDecimal?) {
        val json = parseJson(jsonString)
        val jsonNode = json.get("value")
        val actual = readBigDecimalAnyOfType(jsonNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @Parameters(method = "longTestCases")
    @TestCaseName("{0}")
    fun `correctly read long`(description: String,
                              jsonString: String,
                              expected: Long?) {
        val json = parseJson(jsonString)
        val jsonNode = json.get("value")
        val actual = jsonNodeToLong(jsonNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @Parameters(method = "intTestCases")
    @TestCaseName("{0}")
    fun `correctly read int`(description: String,
                             jsonString: String,
                             expected: Int?) {
        val json = parseJson(jsonString)
        val jsonNode = json.get("value")
        val actual = jsonNodeToInteger(jsonNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @Parameters(method = "bigDecimalNegativeTestCases")
    @TestCaseName("{0}")
    fun `read big decimals negative`(description: String,
                                     jsonString: String) {
        val json = parseJson(jsonString)
        val jsonNode = json.get("value")

        assertThatThrownBy { readBigDecimalAnyOfType(jsonNode) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}

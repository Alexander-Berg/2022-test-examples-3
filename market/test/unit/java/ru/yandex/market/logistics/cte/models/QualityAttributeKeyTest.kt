package ru.yandex.market.logistics.cte.models

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class QualityAttributeKeyTest {
    companion object {
        @JvmStatic
        fun generateQualityAttributeKeyArgs(): List<Arguments> {
            return listOf(
                Arguments.of("1.1. Заголовок 1.8.9. 1.5 ''", "1.1", "Заголовок 1.8.9. 1.5 ''"),
                Arguments.of("1.1       Заголовок 1.8.9. 1.5 ''", "1.1", "Заголовок 1.8.9. 1.5 ''"),
                Arguments.of("1.1.1      Заголовок 1.8.9. 1.5 ''", "1.1.1", "Заголовок 1.8.9. 1.5 ''"),
                Arguments.of("1.1.1.      Заголовок, 1.2.1.1. ''", "1.1.1", "Заголовок, 1.2.1.1. ''"),
                Arguments.of("1.1 fff", "1.1", "fff")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("generateQualityAttributeKeyArgs")
    fun tryBuildFromColumnNameSuccessTest(header: String, refId: String, name: String) {
        val qualityAttributeKey = QualityAttributeKey.tryBuildFromColumnName(header)

        requireNotNull(qualityAttributeKey)
        Assertions.assertEquals(qualityAttributeKey.refId, refId)
        Assertions.assertEquals(qualityAttributeKey.name, name)
    }
}

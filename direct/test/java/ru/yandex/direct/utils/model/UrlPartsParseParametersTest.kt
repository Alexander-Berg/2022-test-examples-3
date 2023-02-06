package ru.yandex.direct.utils.model

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class UrlPartsParseParametersTest {

    fun parameters() = listOf(
        listOf(
            "p=v", listOf(
                org.apache.commons.lang3.tuple.Pair.of("p", "v")
            )
        ),
        listOf(
            "   &p=v", listOf(
                org.apache.commons.lang3.tuple.Pair.of("p", "v")
            )
        ),
        listOf(
            "p=v&   ", listOf(
                org.apache.commons.lang3.tuple.Pair.of("p", "v")
            )
        ),
        listOf(
            "p1=v1&p2=v2", listOf(
                org.apache.commons.lang3.tuple.Pair.of("p1", "v1"),
                org.apache.commons.lang3.tuple.Pair.of("p2", "v2")
            )
        ),
        listOf("", emptyList<Pair<String, String>>()),
        listOf("   ", emptyList<Pair<String, String>>()),
        listOf("=", emptyList<Pair<String, String>>()),
        listOf(" = ", emptyList<Pair<String, String>>())
    )

    @Test
    @Parameters(method = "parameters")
    fun test(encodedParams: String, expectedParams: List<org.apache.commons.lang3.tuple.Pair<String, String>>) {
        val parsedParams = UrlParts.parseParameters(encodedParams)
        Assertions.assertThat(parsedParams).containsExactlyElementsOf(expectedParams)
    }
}

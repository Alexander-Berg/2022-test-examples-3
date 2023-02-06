package ru.yandex.market.tpl.courier.data.feature.mapping

import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.extensions.matchValidationFailure
import ru.yandex.market.tpl.courier.extensions.successWith

class BooleanMapperTest {

    private val mapper = BooleanMapper()

    @ParameterizedTest(name = "{index}: \"{0}\"")
    @MethodSource("data")
    fun `Результат должен совпадать с ожидаемым`(
        value: String,
        match: Matcher<Exceptional<Boolean>>
    ) {
        val mapped = mapper.map(value)
        mapped should match
    }

    companion object {

        @JvmStatic
        fun data() = listOf(
            args(
                value = "true",
                matchResult = successWith(true)
            ),

            args(
                value = "false",
                matchResult = successWith(false)
            ),

            args(
                value = "TRUE",
                matchResult = successWith(true)
            ),

            args(
                value = "FALSE",
                matchResult = successWith(false)
            ),

            args(
                value = "True",
                matchResult = successWith(true)
            ),

            args(
                value = "False",
                matchResult = successWith(false)
            ),

            args(
                value = "true",
                matchResult = successWith(true)
            ),

            args(
                value = "",
                matchResult = matchValidationFailure()
            ),

            args(
                value = "tru",
                matchResult = matchValidationFailure()
            ),

            args(
                value = "1",
                matchResult = successWith(true)
            ),

            args(
                value = "0",
                matchResult = successWith(false)
            ),
        )

        private fun args(
            value: String,
            matchResult: Matcher<Exceptional<Boolean>>
        ): Array<Any?> = arrayOf(value, matchResult)
    }
}
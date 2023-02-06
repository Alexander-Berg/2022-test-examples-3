package ru.yandex.direct.core.entity.keyword.processing.bsexport

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BsExportTextProcessorTest {
    @Autowired
    lateinit var processor: BsExportTextProcessor

    fun testDataProvider() = listOf(
        "Москву"
            to "Москву",
        "Москву -qwe"
            to "Москву -qwe",
        "Москву -!qwe -asdf"
            to "Москву -!qwe -asdf",

        "\"Москву\""
            to "Москву ~0",
        "\"Санкт-Петербург\""
            to "Санкт-Петербург ~0",
        "\"хочу в Москву\""
            to "хочу +в Москву ~0",
        "хочу !в Москву"
            to "хочу !в Москву",

        "хочу в +Москву"
            to "хочу в +Москву",
        "хочу +в Москву"
            to "хочу +в Москву",
        "\"хочу в +Москву\""
            to "хочу +в +Москву ~0",
        "\"хочу +в Москву\""
            to "хочу +в Москву ~0",

        "\"м'який\""
            to "м'який ~0",
        "\"+хочу !в +Москву\""
            to "+хочу !в +Москву ~0",
        "\"скажи мне\""
            to "скажи +мне ~0",
        "\"скажи +мне\""
            to "скажи +мне ~0",
        "\"скажи !мне\""
            to "скажи !мне ~0",
        "\"me 6p smeg  \""
            to "me 6p smeg ~0",

        "\"Генри О. Генри скачать О. Генри\""
            to "Генри О. Генри скачать О. Генри ~0",

        "\"Генри О Генри скачать О Генри\""
            to "Генри +О Генри скачать +О Генри ~0",
    )

    @ParameterizedTest
    @MethodSource("testDataProvider")
    fun processQuotedText(testCase: Pair<String, String>) {
        val (input, expected) = testCase
        assertThat(processor.processQuotedText(input))
            .isEqualTo(expected)
    }
}

package ru.yandex.direct.utils

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class StringUtilsCopyNamesTest {

    fun generateCopyNameParams() = arrayOf(
        arrayOf("(", "( (копия)", "копия"),
        arrayOf("", "(1)", ""),
        arrayOf(null, "(1)", null),
        arrayOf(null, "(копия)", "копия"),
        arrayOf("", "(копия)", "копия"),
        arrayOf("(1)", "(2)", ""),
        arrayOf("(1)", "(2)", null),
        arrayOf("(9999)", "(10000)", ""),
        arrayOf("(9999)", "(10000)", null),
        arrayOf("(99999.99999)", "(99999.99999) (1)", ""),
        arrayOf("(99999) (99999)", "(99999) (100000)", ""),
        arrayOf("Кампания (копия 99999) (99999)", "Кампания (копия 99999) (99999) (копия)", "копия"),
        arrayOf("Кампания (копия 99999) (99999) (копия)", "Кампания (копия 99999) (99999) (копия 2)", "копия"),
        arrayOf("Кампания (копия 99999) (99999)", "Кампания (копия 99999) (100000)", ""),
        arrayOf("Баннер с ёлками", "Баннер с ёлками (копия)", "копия"),
        arrayOf("Баннер с ёлками (копия)", "Баннер с ёлками (копия 2)", "копия"),
        arrayOf("Баннер с ёлками (копия 9283479837)", "Баннер с ёлками (копия 9283479837) (копия)", "копия"),
        arrayOf(
            "Баннер с ёлками (копия 9283479837) (копия 9876543)",
            "Баннер с ёлками (копия 9283479837) (копия 9876544)",
            "копия"
        ),
        arrayOf("Баннер с ёлками", "Баннер с ёлками (1)", ""),
        arrayOf("Баннер с ёлками (1)", "Баннер с ёлками (2)", ""),
        arrayOf("Баннер с ёлками (9283479837)", "Баннер с ёлками (9283479837) (1)", ""),
        arrayOf("Баннер с ёлками (копия 9283479837) (9876543)", "Баннер с ёлками (копия 9283479837) (9876544)", ""),
        arrayOf("Banner (копия)", "Banner (копия) (copy)", "copy"),
        arrayOf("Banner (копия) (copy)", "Banner (копия) (copy 2)", "copy"),
        arrayOf("копия", "копия (копия)", "копия"),
        arrayOf("копия (копия)", "копия (копия 2)", "копия"),
        arrayOf("Баннер с ёлками (копия 0)", "Баннер с ёлками (копия 0) (копия)", "копия"),
        arrayOf("Баннер с ёлками (копия -100)", "Баннер с ёлками (копия -100) (копия)", "копия"),
        arrayOf("Кампания супер (копия) пупер", "Кампания супер (копия) пупер (копия)", "копия"),
        arrayOf(
            "Кампания супер (копия 476494) пупер (копия)",
            "Кампания супер (копия 476494) пупер (копия 2)",
            "копия"
        ),
        arrayOf(
            "Заточенные палки (копия) - атрикул 20220111",
            "Заточенные палки (копия) - атрикул 20220111 (копия)",
            "копия"
        ),
        arrayOf(
            "Заточенные палки (копия) - атрикул 20220111 (копия)",
            "Заточенные палки (копия) - атрикул 20220111 (копия 2)",
            "копия"
        ),
        arrayOf("(копия555)", "(копия555) (копия)", "копия"),
        arrayOf("(копия555) (копия)", "(копия555) (копия 2)", "копия"),
        arrayOf("(0000)", "(0000) (1)", ""),
        arrayOf("(0)", "(0) (1)", ""),
        arrayOf("(-1)", "(-1) (1)", ""),
        arrayOf("(-1)", "(-1) (1)", ""),
        arrayOf("Кампания супер-пупер", "Кампания супер-пупер (новая копия)", "новая копия"),
        arrayOf("Кампания супер-пупер (новая копия 2)", "Кампания супер-пупер (новая копия 3)", "новая копия"),
        arrayOf("Кампания супер-пупер", "Кампания супер-пупер (( новая копия 555 ))", "( новая копия 555 )"),
        arrayOf(
            "Кампания супер-пупер (( новая копия 555 ))",
            "Кампания супер-пупер (( новая копия 555 ) 2)",
            "( новая копия 555 )"
        ),
        arrayOf(
            "Кампания супер-пупер (( новая копия 555 ) 592)",
            "Кампания супер-пупер (( новая копия 555 ) 593)",
            "( новая копия 555 )"
        ),
        arrayOf("Кампания супер-пупер", "Кампания супер-пупер ( ( ( )", " ( ( "),
        arrayOf("Кампания супер-пупер ( ( ( )", "Кампания супер-пупер ( ( (  2)", " ( ( "),
        arrayOf(
            "Кампания супер-пупер ( ((123456)) 123456 )",
            "Кампания супер-пупер ( ((123456)) 123456  2)",
            " ((123456)) 123456 "
        ),
        arrayOf(
            "Кампания супер-пупер ( ((123456)) 123456  123456)",
            "Кампания супер-пупер ( ((123456)) 123456  123457)",
            " ((123456)) 123456 "
        ),
        arrayOf(
            "Кампания супер-пупер ( ((123456)) 123456)",
            "Кампания супер-пупер ( ((123456)) 123456 2)",
            " ((123456)) 123456"
        ),
        arrayOf(
            "Кампания супер-пупер ( ((123456)) 123456 123456)",
            "Кампания супер-пупер ( ((123456)) 123456 123457)",
            " ((123456)) 123456"
        ),
        arrayOf("Кампания супер-пупер", "Кампания супер-пупер (121 копия)", "121 копия"),
        arrayOf("Кампания супер-пупер (121 копия)", "Кампания супер-пупер (121 копия 2)", "121 копия"),
        arrayOf("Кампания супер-пупер (121 копия 385678)", "Кампания супер-пупер (121 копия 385679)", "121 копия"),
    )

    @Test
    @Parameters(method = "generateCopyNameParams")
    fun testGenerateCopyName(originalName: String?, expectedName: String?, prefix: String?) {
        val actualName = StringUtils.generateCopyName(originalName, prefix)
        assertThat(actualName)
            .describedAs("Actual name not equal to expected. Prefix: $prefix, originalName: $originalName")
            .isEqualTo(expectedName)
    }

    @Test
    @Parameters(method = "generateCopyNameParams")
    fun testGenerateCopyNamesFromSingleName(originalName: String?, expectedName: String?, prefix: String?) {
        val actualNames = StringUtils.generateCopyNames(listOf(originalName), listOf(originalName), prefix)
        assertThat(actualNames[0])
            .describedAs("Actual name not equals to expected. Prefix: $prefix")
            .isEqualTo(expectedName)
    }

    fun generateCopyNamesParams() = arrayOf(
        arrayOf(
            null as String?,
            listOf<String?>(),
            listOf<String?>(null, null, null),
            listOf<String?>(null, "(1)", "(2)")
        ),
        arrayOf(
            "",
            listOf<String?>(),
            listOf<String?>(null, null, null),
            listOf<String?>(null, "(1)", "(2)")
        ),
        arrayOf(
            "",
            listOf<String?>(null, null, null),
            listOf<String?>(null, null, null),
            listOf<String?>("(1)", "(2)", "(3)")
        ),
        arrayOf(
            "",
            listOf<String?>(null, "", "тестовая кампания"),
            listOf<String?>(null, "", "тестовая кампания"),
            listOf<String?>("(1)", "(2)", "тестовая кампания (1)")
        ),
        arrayOf(
            "копия",
            listOf<String?>("", "тестовая кампания"),
            listOf<String?>(null, "", "тестовая кампания"),
            listOf<String?>(null, "(копия)", "тестовая кампания (копия)")
        ),
        arrayOf(
            "копия",
            listOf<String?>("тестовая кампания (копия)", "тестовая кампания (копия 2)"),
            listOf<String?>("тестовая кампания", "тестовая кампания (копия)"),
            listOf<String?>("тестовая кампания", "тестовая кампания (копия 3)")
        ),
        arrayOf(
            "копия",
            listOf<String?>("кампания", "кампания (копия)", "кампания (копия 3)", "кампания (копия 99)"),
            listOf<String?>("кампания", "кампания (копия 2)", "кампания (копия 3)", "кампания (копия 5)"),
            listOf<String?>("кампания (копия 100)", "кампания (копия 2)", "кампания (копия 101)", "кампания (копия 5)")
        ),
        arrayOf(
            "копия",
            listOf<String?>("тестовая кампания (копия)", "тестовая кампания (копия 2)"),
            listOf<String?>("тестовая кампания", "тестовая кампания (копия 3)"),
            listOf<String?>("тестовая кампания", "тестовая кампания (копия 3)")
        ),
    )

    @Test
    @Parameters(method = "generateCopyNamesParams")
    fun testGenerateCopyNames(
        prefix: String?,
        originalNames: List<String?>,
        newNames: List<String?>,
        expectedNames: List<String?>,
    ) {
        val actualNames = StringUtils.generateCopyNames(originalNames, newNames, prefix)
        assertThat(actualNames)
            .describedAs("Actual names not equals to expected. " +
                "Prefix: $prefix, originalNames: $originalNames, newNames: $newNames")
            .isEqualTo(expectedNames)
    }
}

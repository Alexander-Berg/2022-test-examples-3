package ru.yandex.market.contentmapping.utils

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.common.util.text.Charsets
import ru.yandex.market.mbo.excel.ExcelFile
import java.io.ByteArrayInputStream

/**
 * @author yuramalinov
 * @created 10.03.2020
 */
class CsvToExcelFileTest {
    @Test
    fun naiveTest() {
        val expected = ExcelFile.Builder.withHeaders("test", "данные").addLine("a", "b").build()
        Assertions.assertThat(parse("test,данные\na,b\n".toByteArray(Charsets.UTF_8))).isEqualTo(expected)
        Assertions.assertThat(parse("test,данные\na,b\n".toByteArray(Charsets.CP1251))).isEqualTo(expected)
        Assertions.assertThat(parse("test\tданные\na\tb\n".toByteArray(Charsets.CP1251))).isEqualTo(expected)
        Assertions.assertThat(parse("test;данные\na;b\n".toByteArray(Charsets.CP1251))).isEqualTo(expected)
    }

    @Test
    fun largeTest() {
        val largeStream = javaClass.classLoader.getResourceAsStream("data-files/large-csv.csv")!!
        val result = CsvToExcelFile.parseCsv(largeStream)
        result.headers shouldHaveSize 836
        result.lines shouldHaveSize 1
    }

    private fun parse(bytes: ByteArray): ExcelFile {
        val inputStream = ByteArrayInputStream(bytes)
        return CsvToExcelFile.parseCsv(inputStream)
    }
}

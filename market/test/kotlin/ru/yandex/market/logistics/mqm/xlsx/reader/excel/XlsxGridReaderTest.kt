package ru.yandex.market.logistics.mqm.xlsx.reader.excel

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader
import ru.yandex.market.logistics.mqm.dto.ClaimOrderCsvRecord
import ru.yandex.market.logistics.mqm.xlsx.model.XlsxGrid
import ru.yandex.market.logistics.mqm.xlsx.reader.ClaimOrdersFileReader
import ru.yandex.market.logistics.mqm.xlsx.reader.GridReader
import ru.yandex.market.logistics.mqm.xlsx.writer.ClaimOrderGridWriter
import ru.yandex.market.logistics.mqm.xlsx.writer.GridWriter
import ru.yandex.market.logistics.mqm.xlsx.writer.XLSGridWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@DisplayName("Проверка на корректность работы базового экзекьютера.")
class XlsxGridReaderTest{
    private fun getFile(url: String): ByteArrayInputStream {
        val input = this.javaClass.classLoader.getResource(url)!!
        val file = File(input.toURI())

        var content: ByteArray? = null
        try {
            content = file.readBytes()
        } catch (e: IOException) {
        }

        return ByteArrayInputStream(content)
    }

    @Test
    fun testReadWriteXlsx() {
        readWriteAndCompare("rw-test.xlsx", XlsxGridReader(), XLSGridWriter())
    }

    @Test
    fun testWriteAndReadXlsx() {
        val generatedCsv = listOf(
            ClaimOrderCsvRecord("1", Instant.parse("2021-12-20T17:00:00Z").toString(), "4", "COMPENSATE_SD", "6", "address", "7", "8", "9", "11"),
            ClaimOrderCsvRecord("2", Instant.parse("2021-11-20T17:00:00Z").toString(), "5", "DELETED", "6", "address", "7", "8", "9", "11"),
            ClaimOrderCsvRecord("3", Instant.parse("2021-10-20T17:00:00Z").toString(), "6", "MARKET_FAULT", "6", "address", "7", "8", "9", "11")
        )
        val byteArrayOutputStream = ByteArrayOutputStream()
        ClaimOrderGridWriter().writeFromCsv(generatedCsv,byteArrayOutputStream)
        val claimOrderCsvRecords = ClaimOrdersFileReader().read(byteArrayOutputStream.toByteArray().inputStream())
        assert(claimOrderCsvRecords == generatedCsv)
    }

    private fun readFileAsInputStream(name: String): InputStream {
        return getFile("new_claim_test.xlsx")
    }

    private fun readWriteAndCompare(fileName: String, reader: GridReader, writer: GridWriter) {
        val buf = ByteArrayOutputStream()
        val grid1: XlsxGrid? = reader.read(readFileAsInputStream(fileName))
        writer.write(grid1, buf)
        val grid2: XlsxGrid? = reader.read(ByteArrayInputStream(buf.toByteArray()))
        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(
                grid1
            ).isEqualToComparingFieldByFieldRecursively(grid2)
        }
    }
}


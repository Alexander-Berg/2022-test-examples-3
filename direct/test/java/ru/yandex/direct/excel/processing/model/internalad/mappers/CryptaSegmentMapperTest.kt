package ru.yandex.direct.excel.processing.model.internalad.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.direct.excel.processing.model.internalad.CryptaSegment
import ru.yandex.direct.excelmapper.ExcelMapper
import ru.yandex.direct.excelmapper.MapperTestUtils
import ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException

private const val TITLE = "CryptaSegmentMapper title"
private val MAPPER: ExcelMapper<CryptaSegment> = CryptaSegmentMapper(TITLE)

class CryptaSegmentMapperTest {

    @Test
    fun writeValueTest() {
        val cryptaSegment = CryptaSegment.create("12", "456")
        val sheetRange = MapperTestUtils.createEmptySheet()
        MAPPER.write(sheetRange, cryptaSegment)

        val expectedValue = cryptaSegment.convertToString()
        assertThat(MapperTestUtils.sheetToLists(sheetRange, 1))
            .isEqualTo(listOf(listOf(expectedValue)))
    }

    @Test
    fun writeNullValueTest() {
        val sheetRange = MapperTestUtils.createEmptySheet()
        val exception = Assertions.assertThrows(
            CantWriteEmptyException::class.java
        ) {
            MAPPER.write(sheetRange, null)
        }

        assertThat(exception.columns)
            .isEqualTo(listOf(TITLE))
    }

    @Test
    fun readValueTest() {
        val cryptaSegment = CryptaSegment.create("123", "45")
        val sheetRange = createStringSheetFromLists(listOf(listOf(cryptaSegment.convertToString())))
        val value = MAPPER.read(sheetRange).value

        assertThat(value)
            .isEqualTo(cryptaSegment)
    }

    @Test
    fun readInvalidValueTest() {
        val sheetRange = createStringSheetFromLists(listOf(listOf("123:4invalid5")))
        val exception = Assertions.assertThrows(
            CantReadFormatException::class.java
        ) {
            MAPPER.read(sheetRange)
        }

        assertThat(exception)
            .hasFieldOrPropertyWithValue("columns", listOf(TITLE))
            .hasFieldOrPropertyWithValue("rowIndex", 0)
            .hasFieldOrPropertyWithValue("columnIndex", 0)
    }

    @Test
    fun readEmptyValueTest() {
        val sheetRange = createStringSheetFromLists(listOf(listOf("")))
        val exception = Assertions.assertThrows(
            CantReadEmptyException::class.java
        ) {
            MAPPER.read(sheetRange)
        }

        assertThat(exception)
            .hasFieldOrPropertyWithValue("columns", listOf(TITLE))
            .hasFieldOrPropertyWithValue("rowIndex", 0)
            .hasFieldOrPropertyWithValue("columnIndex", 0)
    }

    private fun CryptaSegment.convertToString() = String.format("%s:%s", this.keywordId, this.segmentId)

}

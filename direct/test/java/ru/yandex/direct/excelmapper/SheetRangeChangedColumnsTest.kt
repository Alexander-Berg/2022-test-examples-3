package ru.yandex.direct.excelmapper

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.excelmapper.ExcelMappers.*
import ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet
import ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists
import ru.yandex.direct.excelmapper.mappers.ObjectExcelMapper


class SheetRangeChangedColumnsTest {

    @Test
    fun getChangedColumns_WithSimpleTypeExcelMapper() {
        val sheetRange = createEmptySheet()

        val intMapper = intMapper("testTitle")

        assertThat(sheetRange.columnsWithNotEmptyValue, equalTo(emptySet<Any>()))

        intMapper.write(sheetRange, 10)
        assertThat(sheetRange.columnsWithNotEmptyValue, equalTo(setOf(0)))

        intMapper.write(sheetRange, -10)
        assertThat(sheetRange.columnsWithNotEmptyValue, equalTo(setOf(0)))
    }

    @Test
    fun getChangedColumns_WithObjectTypeExcelMapper() {
        val sheetRange = createEmptySheet()

        Obj.mapper.write(sheetRange, Obj(15, "123", listOf(1, 2, 3)))

        assertThat(sheetRange.columnsWithNotEmptyValue, equalTo(setOf(0, 1, 2)))
    }

    @Test
    fun getChangedColumns_WithSubRanges() {
        val sheetRange = createEmptySheet()

        val subRange1 = sheetRange.makeSubRange(0, 1)
        val subRange2 = sheetRange.makeSubRange(1, 1)

        Obj.mapper.write(sheetRange, Obj(10, "20", listOf(30, 31, 32)))
        Obj.mapper.write(subRange1, Obj(100, "200", listOf(300, 301, 302)))
        intMapper("testTitle").write(subRange2, 1000)

        assertThat(subRange1.columnsWithNotEmptyValue, equalTo(setOf(1, 2, 3)))
        assertThat(subRange2.columnsWithNotEmptyValue, equalTo(setOf(1)))
        assertThat(sheetRange.columnsWithNotEmptyValue, equalTo(setOf(0, 1, 2, 3)))

        assertThat(sheetToLists(sheetRange, 4), equalTo(listOf(
            listOf("10", "100", "200", "300"),
            listOf("", "1000", "31", "301"),
            listOf("", "", "32", "302")
        )))
    }

    private data class Obj(var num: Int = 1,
                           var str: String = "",
                           var list: List<Int> = emptyList()) {

        companion object {
            val mapper: ObjectExcelMapper<Obj> = objectMapper { Obj() }
                .field(Obj::num, Obj::num.setter, intMapper("NUM"))
                .field(Obj::str, Obj::str.setter, stringMapper("STR"))
                .field(Obj::list, Obj::list.setter, listMapper(intMapper("LIST_OF_NUMS")))
                .build()
        }
    }
}

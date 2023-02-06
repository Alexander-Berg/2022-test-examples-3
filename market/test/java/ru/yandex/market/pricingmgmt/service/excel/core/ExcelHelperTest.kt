package ru.yandex.market.pricingmgmt.service.excel.core

import org.apache.poi.ss.usermodel.Cell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.yandex.market.pricingmgmt.config.WorkbookFactory

internal class ExcelHelperTest {
    data class ItemDto(
        var id: String? = null,
        var code: String? = null,
        var name: String? = null
    )

    data class ExcelRowDto(
        var column01: String? = null,
        var column02: String? = null,
        var column03: String? = null,
    )

    private val itemDtoHeaders = listOf(
        ColumnMetaData(
            title = "Id",
            required = true,
            getValue = { obj: ItemDto, value: Any? -> obj.id = value?.toString() },
            setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.id) }
        ),
        ColumnMetaData(
            title = "Code",
            required = false,
            getValue = { obj: ItemDto, value: Any? -> obj.code = value?.toString() },
            setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.code) }
        ),
        ColumnMetaData(
            title = "Name",
            required = true,
            getValue = { obj: ItemDto, value: Any? -> obj.name = value?.toString() },
            setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.name) }
        )
    )

    private val itemDtoHeadersWithInfo = listOf(
        ColumnMetaData(
            title = "Id",
            required = true,
            getValue = { obj: ItemDto, value: Any? -> obj.id = value?.toString() },
            setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.id) }
        ),
        ColumnMetaData(
            title = "Code",
            required = false,
            getValue = { obj: ItemDto, value: Any? -> obj.code = value?.toString() },
            setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.code) },
            info = VerticalRange(
                items = listOf(
                    RangeStringItem(value = "Code info 1"),
                    RangeStringItem(value = "Code info 2")
                ),
                alignment = VerticalRangeAlignment.Top
            )
        ),
        ColumnMetaData(
            title = "Name",
            required = true,
            getValue = { obj: ItemDto, value: Any? -> obj.name = value?.toString() },
            setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.name) },
            info = VerticalRange(
                items = listOf(
                    RangeStringItem(value = "Name info 1"),
                    RangeStringItem(value = "Name info 2")
                ),
                alignment = VerticalRangeAlignment.Bottom
            )
        )
    )

    private val excelRowDtoHeaders = listOf(
        ColumnMetaData(
            title = "column01",
            required = false,
            getValue = { obj: ExcelRowDto, value: Any? -> obj.column01 = value?.toString() },
            setValue = { cell: Cell, obj: ExcelRowDto -> cell.setCellValue(obj.column01) }
        ),
        ColumnMetaData(
            title = "column02",
            required = false,
            getValue = { obj: ExcelRowDto, value: Any? -> obj.column02 = value?.toString() },
            setValue = { cell: Cell, obj: ExcelRowDto -> cell.setCellValue(obj.column02) }
        ),
        ColumnMetaData(
            title = "column03",
            required = false,
            getValue = { obj: ExcelRowDto, value: Any? -> obj.column03 = value?.toString() },
            setValue = { cell: Cell, obj: ExcelRowDto -> cell.setCellValue(obj.column03) }
        ),
    )

    private val workbookFactory = WorkbookFactory()

    private val itemDtoHelper = ExcelHelper(workbookFactory, { ItemDto() })
    private val excelRowDtoHelper = ExcelHelper(workbookFactory, { ExcelRowDto() })

    private fun readTestCore(
        expected: List<ItemDto>,
        columns: List<ColumnMetaData<ItemDto>> = itemDtoHeaders,
        withoutHeaders: Boolean = false,
        filename: String,
        sheetEndMode: SheetEndMode = SheetEndMode.END,
        columnMapMode: ColumnMapMode
    ) {
        val inputStream = javaClass.getResourceAsStream(filename)!!
        val headerRow: Int = if (withoutHeaders) -1 else 0
        val startRow: Int = if (withoutHeaders) 0 else 1
        val actual = itemDtoHelper
            .read(
                inputStream = inputStream,
                headers = columns,
                headerRow = headerRow,
                startRow = startRow,
                sheetEndMode = sheetEndMode,
                columnMapMode = columnMapMode
            )
        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "false,/xlsx-template/excel-helper/empty.xlsx,END",
            "false,/xlsx-template/excel-helper/empty.xlsx,FIRST_EMPTY_ROW",
            "true,/xlsx-template/excel-helper/empty-without-headers.xlsx,END",
            "true,/xlsx-template/excel-helper/empty-without-headers.xlsx,FIRST_EMPTY_ROW"
        ]
    )
    fun read_empty(withoutHeaders: Boolean, filename: String, sheetEndMode: SheetEndMode) {
        val expected = emptyList<ItemDto>()
        readTestCore(
            expected = expected,
            filename = filename,
            withoutHeaders = withoutHeaders,
            sheetEndMode = sheetEndMode,
            columnMapMode = ColumnMapMode.ORDER
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "false,/xlsx-template/excel-helper/default.xlsx",
            "true,/xlsx-template/excel-helper/default-without-headers.xlsx",
            "false,/xlsx-template/excel-helper/empty-row.xlsx",
            "true,/xlsx-template/excel-helper/empty-row-without-headers.xlsx",
            "false,/xlsx-template/excel-helper/empty-row-after-header.xlsx",
            "true,/xlsx-template/excel-helper/empty-row-after-header-without-headers.xlsx",
        ]
    )
    fun read_end(withoutHeaders: Boolean, filename: String) {
        val expected = listOf(
            ItemDto(id = "1", code = "code01", name = "name01"),
            ItemDto(id = "2", code = "code02", name = "name02")
        )
        readTestCore(
            expected = expected,
            filename = filename,
            withoutHeaders = withoutHeaders,
            sheetEndMode = SheetEndMode.END,
            columnMapMode = ColumnMapMode.ORDER
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "false,/xlsx-template/excel-helper/empty-row.xlsx",
            "true,/xlsx-template/excel-helper/empty-row-without-headers.xlsx"
        ]
    )
    fun read_emptyRow_firstEmptyRow_ok(withoutHeaders: Boolean, filename: String) {
        val expected = listOf(
            ItemDto(id = "1", code = "code01", name = "name01")
        )
        readTestCore(
            expected = expected,
            filename = filename,
            withoutHeaders = withoutHeaders,
            sheetEndMode = SheetEndMode.FIRST_EMPTY_ROW,
            columnMapMode = ColumnMapMode.ORDER
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "false,/xlsx-template/excel-helper/empty-row-after-header.xlsx",
            "true,/xlsx-template/excel-helper/empty-row-after-header-without-headers.xlsx"
        ]
    )
    fun read_emptyRowAfterHeader_firstEmptyRow_ok(withoutHeaders: Boolean, filename: String) {
        val expected = emptyList<ItemDto>()
        readTestCore(
            expected = expected,
            filename = filename,
            withoutHeaders = withoutHeaders,
            sheetEndMode = SheetEndMode.FIRST_EMPTY_ROW,
            columnMapMode = ColumnMapMode.ORDER
        )
    }

    @Test
    fun read_requiredColumnAll_ok() {
        val expected = listOf(
            ItemDto(id = "1", name = "name01"),
            ItemDto(id = "2", name = "name02")
        )
        readTestCore(
            expected = expected,
            filename = "/xlsx-template/excel-helper/required-fields-all.xlsx",
            columnMapMode = ColumnMapMode.TITLE
        )
    }

    @Test
    fun createHelper_repeatColumnName_throws() {
        val e = assertThrows<RuntimeException> {
            readTestCore(
                expected = emptyList(),
                filename = "/xlsx-template/excel-helper/default.xlsx",
                columns = itemDtoHeaders.plus(ColumnMetaData(
                    title = "Id",
                    required = true,
                    getValue = { obj: ItemDto, value: Any? -> obj.id = value?.toString() },
                    setValue = { cell: Cell, obj: ItemDto -> cell.setCellValue(obj.id) }
                )),
                columnMapMode = ColumnMapMode.TITLE
            )
        }

        assertEquals("Некорректная структура столбцов: встречаются повторяющиеся заголовки", e.message)
    }

    @Test
    fun createHelper_noHeaderTitle_throws() {
        val e = assertThrows<RuntimeException> {
            readTestCore(
                expected = emptyList(),
                filename = "/xlsx-template/excel-helper/default-without-headers.xlsx",
                withoutHeaders = true,
                columnMapMode = ColumnMapMode.TITLE
            )
        }

        assertEquals("Режим TITLE не поддерживается без заголовков", e.message)
    }

    @Test
    fun read_requiredColumnAllShuffle_ok() {
        val expected = listOf(
            ItemDto(id = "1", name = "name01"),
            ItemDto(id = "2", name = "name02")
        )
        readTestCore(
            expected = expected,
            filename = "/xlsx-template/excel-helper/required-fields-all-shuffle.xlsx",
            columnMapMode = ColumnMapMode.TITLE
        )
    }

    @Test
    fun read_unknownColumn_ok() {
        val expected = listOf(
            ItemDto(id = "1", code = "code01", name = "name01"),
            ItemDto(id = "2", code = "code02", name = "name02")
        )
        readTestCore(
            expected = expected,
            filename = "/xlsx-template/excel-helper/unknown-column.xlsx",
            columnMapMode = ColumnMapMode.TITLE
        )
    }

    @Test
    fun read_requiredColumnMissing_throw() {
        val e = assertThrows<RuntimeException> {
            readTestCore(
                expected = emptyList(),
                filename = "/xlsx-template/excel-helper/required-fields-missing.xlsx",
                columnMapMode = ColumnMapMode.TITLE
            )
        }
        assertEquals("Неправильная структура файла, отсутствуют столбцы: Id", e.message)
    }

    @Test
    fun write_no_info_ok() {
        val response = itemDtoHelper.write(
            items = listOf(
                ItemDto(
                    id = "id1",
                    code = "code1",
                    name = "name1"
                ),
                ItemDto(
                    id = "id2",
                    code = "code2",
                    name = "name2"
                )
            ),
            headers = itemDtoHeaders,
            filename = "filename1",
            headerRow = 4,
            startRow = 5
        )

        val actual = excelRowDtoHelper.read(
            inputStream = response.inputStream,
            headers = excelRowDtoHeaders,
            headerRow = -1,
            startRow = 0
        )

        val expected = listOf(
            ExcelRowDto(column01 = "Id", column02 = "Code", column03 = "Name"),
            ExcelRowDto(column01 = "id1", column02 = "code1", column03 = "name1"),
            ExcelRowDto(column01 = "id2", column02 = "code2", column03 = "name2")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun write_info_ok() {
        val response = itemDtoHelper.write(
            items = listOf(
                ItemDto(
                    id = "id1",
                    code = "code1",
                    name = "name1"
                ),
                ItemDto(
                    id = "id2",
                    code = "code2",
                    name = "name2"
                )
            ),
            headers = itemDtoHeadersWithInfo,
            filename = "filename1",
            headerRow = 4,
            startRow = 5
        )

        val actual = excelRowDtoHelper.read(
            inputStream = response.inputStream,
            headers = excelRowDtoHeaders,
            headerRow = -1,
            startRow = 0
        )

        val expected = listOf(
            ExcelRowDto(column01 = null, column02 = "Code info 1", column03 = null),
            ExcelRowDto(column01 = null, column02 = "Code info 2", column03 = null),
            ExcelRowDto(column01 = null, column02 = null, column03 = "Name info 1"),
            ExcelRowDto(column01 = null, column02 = null, column03 = "Name info 2"),
            ExcelRowDto(column01 = "Id", column02 = "Code", column03 = "Name"),
            ExcelRowDto(column01 = "id1", column02 = "code1", column03 = "name1"),
            ExcelRowDto(column01 = "id2", column02 = "code2", column03 = "name2")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun write_info_noHeaders_ok() {
        val response = itemDtoHelper.write(
            items = listOf(
                ItemDto(
                    id = "id1",
                    code = "code1",
                    name = "name1"
                ),
                ItemDto(
                    id = "id2",
                    code = "code2",
                    name = "name2"
                )
            ),
            headers = itemDtoHeadersWithInfo,
            filename = "filename1",
            headerRow = -1,
            startRow = 0
        )

        val actual = excelRowDtoHelper.read(
            inputStream = response.inputStream,
            headers = excelRowDtoHeaders,
            headerRow = -1,
            startRow = 0
        )

        val expected = listOf(
            ExcelRowDto(column01 = "id1", column02 = "code1", column03 = "name1"),
            ExcelRowDto(column01 = "id2", column02 = "code2", column03 = "name2")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun write_info_throw() {
        val e = assertThrows<ExcelHelperWriteException> {
            itemDtoHelper.write(
                items = listOf(
                    ItemDto(
                        id = "id1",
                        code = "code1",
                        name = "name1"
                    ),
                    ItemDto(
                        id = "id2",
                        code = "code2",
                        name = "name2"
                    )
                ),
                headers = itemDtoHeadersWithInfo,
                filename = "filename1",
                headerRow = 1,
                startRow = 2
            )
        }

        assertEquals("Невозможно записать дополнительную информацию, недостаточно строк", e.message)
    }
}

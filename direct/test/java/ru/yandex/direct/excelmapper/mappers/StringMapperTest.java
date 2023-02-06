package ru.yandex.direct.excelmapper.mappers;

import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;
import ru.yandex.direct.excelmapper.exceptions.InvalidCellDataFormatException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.stringMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

class StringMapperTest {
    private static final String TITLE = "SomeString";
    private ExcelMapper<String> mapper;

    @BeforeEach
    void setUp() {
        mapper = stringMapper(TITLE);
    }

    @Test
    void writeValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, "xxx");

        assertThat(sheetToLists(sheetRange, 1), equalTo(List.of(
                List.of("xxx")
        )));
    }

    @Test
    void writeNullTest() {
        SheetRange sheetRange = createEmptySheet();

        CantWriteEmptyException exception = assertThrows(CantWriteEmptyException.class, () ->
                mapper.write(sheetRange, null)
        );
        assertThat(exception, hasProperty("columns", equalTo(List.of(TITLE))));
    }

    @Test
    void readValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("xxx")
        ));

        String value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo("xxx"));
    }

    @Test
    void readValueAndTrimTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("  x   ")
        ));

        String value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo("x"));
    }

    @Test
    void readEmptyTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("")
        ));

        CantReadEmptyException exception = assertThrows(CantReadEmptyException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readFromInvalidCellDataFormatTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("123")
        ));
        sheetRange.getCell(0, 0).setCellType(CellType.NUMERIC);

        InvalidCellDataFormatException exception = assertThrows(InvalidCellDataFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readEmptyValue_FromInvalidCellDataFormatTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("")
        ));
        sheetRange.getCell(0, 0).setCellType(CellType.NUMERIC);

        InvalidCellDataFormatException exception = assertThrows(InvalidCellDataFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

}

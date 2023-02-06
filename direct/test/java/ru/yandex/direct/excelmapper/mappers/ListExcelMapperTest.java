package ru.yandex.direct.excelmapper.mappers;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.excelmapper.exceptions.CantReadUnexpectedDataException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.listMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.longMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

class ListExcelMapperTest {
    private static final String TITLE = "Number";
    private ExcelMapper<List<Long>> mapper;

    @BeforeEach
    void setUp() {
        mapper = listMapper(longMapper(TITLE));
    }

    @Test
    void writeSingleValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, List.of(1L));

        assertThat(sheetToLists(sheetRange, 1), equalTo(List.of(
                List.of("1")
        )));
    }

    @Test
    void writeSeveralValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, List.of(1L, 2L, 3L));

        assertThat(sheetToLists(sheetRange, 1), equalTo(List.of(
                List.of("1"),
                List.of("2"),
                List.of("3")
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
    void writeEmptyListTest() {
        SheetRange sheetRange = createEmptySheet();

        CantWriteEmptyException exception = assertThrows(CantWriteEmptyException.class, () ->
                mapper.write(sheetRange, List.of())
        );
        assertThat(exception, hasProperty("columns", equalTo(List.of(TITLE))));
    }

    @Test
    void writeNullItemTest() {
        SheetRange sheetRange = createEmptySheet();

        CantWriteEmptyException exception = assertThrows(CantWriteEmptyException.class, () ->
                mapper.write(sheetRange, Collections.singletonList(null))
        );
        assertThat(exception, hasProperty("columns", equalTo(List.of(TITLE))));
    }

    @Test
    void readSingleValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("2")
        ));

        List<Long> value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(List.of(2L)));
    }

    @Test
    void readSeveralValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1"),
                List.of("2"),
                List.of("3")
        ));

        List<Long> value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(List.of(1L, 2L, 3L)));
    }

    @Test
    void readInvalidFirstValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("xx"),
                List.of("2")
        ));

        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readInvalidSecondValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1"),
                List.of("xx")
        ));

        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(1)),
                hasProperty("columnIndex", equalTo(0))));
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
    void readUnexpectedFieldValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1"),
                List.of(""),
                List.of("2")
        ));

        CantReadUnexpectedDataException exception = assertThrows(CantReadUnexpectedDataException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(2)),
                hasProperty("columnIndex", equalTo(0))));
    }
}

package ru.yandex.direct.excelmapper.mappers;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.intMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

class IntegerMapperTest {
    private static final String TITLE = "Number";
    private ExcelMapper<Integer> mapper;

    @BeforeEach
    void setUp() {
        mapper = intMapper(TITLE);
    }

    @Test
    void writeValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, 1);

        assertThat(sheetToLists(sheetRange, 1), equalTo(List.of(
                List.of("1")
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
                List.of("2")
        ));

        Integer value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(2));
    }

    @Test
    void readInvalidValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("xx")
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

}

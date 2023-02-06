package ru.yandex.direct.excelmapper.mappers;

import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;
import ru.yandex.direct.excelmapper.exceptions.InvalidCellDataFormatException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.enumMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

class EnumMapperTest {
    private static final String TITLE = "SomeEnum";

    private enum TestEnum {
        FIRST_VALUE,
        SECOND_VALUE,
    }

    private ExcelMapper<TestEnum> mapper;

    @BeforeEach
    void setUp() {
        mapper = enumMapper(TITLE, TestEnum.class);
    }

    @Test
    void writeValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, TestEnum.FIRST_VALUE);

        assertThat(sheetToLists(sheetRange, 1), equalTo(List.of(
                List.of(TestEnum.FIRST_VALUE.name())
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
                List.of(TestEnum.SECOND_VALUE.name())
        ));

        TestEnum value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(TestEnum.SECOND_VALUE));
    }

    @Test
    void readValueAndTrimTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of(String.format("  %s   ", TestEnum.FIRST_VALUE.name()))
        ));

        TestEnum value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(TestEnum.FIRST_VALUE));
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
    void readFromInvalidValue() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("invalid_enum_value")
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

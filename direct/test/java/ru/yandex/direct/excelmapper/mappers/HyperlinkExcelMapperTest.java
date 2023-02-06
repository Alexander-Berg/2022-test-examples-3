package ru.yandex.direct.excelmapper.mappers;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;
import ru.yandex.direct.excelmapper.exceptions.InvalidCellDataFormatException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.hyperlinkExcelMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createFormulasSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

public class HyperlinkExcelMapperTest {
    private static final String TITLE = "hyperlink";
    private static final String URL = "url";
    private static final String URL_TITLE = "title";

    private static final Pair<String, String> HYPERLINK_PAIR = Pair.of(URL, URL_TITLE);
    private static final String HYPERLINK_FORMULA = HyperlinkExcelMapper.makeHyperlink(URL, URL_TITLE);

    private ExcelMapper<String> mapper;

    @BeforeEach
    void setUp() {
        mapper = hyperlinkExcelMapper(TITLE);
    }

    @Test
    void writeValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, HyperlinkExcelMapper.makeHyperlink(URL, URL_TITLE));

        assertThat(sheetToLists(sheetRange, 1),
                equalTo(List.of(List.of(
                        HYPERLINK_FORMULA
                )))
        );
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
        SheetRange sheetRange = createFormulasSheetFromLists(List.of(List.of(
                HYPERLINK_FORMULA
        )));

        String value = mapper.read(sheetRange).getValue();

        assertThat(HyperlinkExcelMapper.getLinkAndTitle(value), equalTo(HYPERLINK_PAIR));
    }

    @Test
    void readInvalidTest() {
        SheetRange sheetRange = createFormulasSheetFromLists(List.of(
                List.of("SUM(C4:E4)")
        ));

        InvalidCellDataFormatException exception = assertThrows(InvalidCellDataFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }
}

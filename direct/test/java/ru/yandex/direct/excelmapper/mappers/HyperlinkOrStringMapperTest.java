package ru.yandex.direct.excelmapper.mappers;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.hyperlinkOrStringMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createFormulasSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

public class HyperlinkOrStringMapperTest {
    private static final String TITLE = "hyperlink";
    private static final String URL = "url";
    private static final String URL_TITLE = "title";

    private static final Pair<String, String> HYPERLINK_PAIR = Pair.of(URL, URL_TITLE);
    private static final String HYPERLINK_FORMULA = HyperlinkExcelMapper.makeHyperlink(URL, URL_TITLE);

    private ExcelMapper<String> mapper;

    @BeforeEach
    void setUp() {
        mapper = hyperlinkOrStringMapper(TITLE);
    }

    @Test
    void writeHyperlinkTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, HyperlinkExcelMapper.makeHyperlink(URL, URL_TITLE));

        assertThat(sheetToLists(sheetRange, 1),
                equalTo(List.of(List.of(
                        HYPERLINK_FORMULA
                )))
        );
    }

    @Test
    void writeStringTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, "keksik");

        assertThat(sheetToLists(sheetRange, 1),
                equalTo(List.of(List.of(
                        "keksik"
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
    void readHyperlinkTest() {
        SheetRange sheetRange = createFormulasSheetFromLists(List.of(List.of(
                HYPERLINK_FORMULA
        )));

        String value = mapper.read(sheetRange).getValue();

        assertThat(HyperlinkExcelMapper.getLinkAndTitle(value), equalTo(HYPERLINK_PAIR));
    }

    @Test
    void readStringTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("keksik")
        ));

        String value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo("keksik"));
    }
}

package ru.yandex.direct.excel.processing.model.internalad.mappers;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteFormatException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

public class GoalContextMapperTest {
    private static final String TITLE = "Ретаргетинг GoalContext";
    private ExcelMapper<List<Rule>> mapper;

    @BeforeEach
    void setUp() {
        mapper = new GoalContextMapper(TITLE);
    }

    @Test
    void writeValueTest() {
        SheetRange sheetRange = createEmptySheet();
        mapper.write(sheetRange, List.of(
                new Rule()
                        .withGoals(List.of(goal(20141023L, 3), goal(20141022L, 2)))
                        .withType(RuleType.OR)));
        assertThat(sheetToLists(sheetRange, 1), equalTo(List.of(
                List.of("(20141023:3|20141022:2)")
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
    @Disabled("https://st.yandex-team.ru/DIRECT-167976")
    void writeInvalidRuleTest() {
        SheetRange sheetRange = createEmptySheet();
        CantWriteFormatException exception = assertThrows(CantWriteFormatException.class, () ->
                mapper.write(sheetRange,
                        List.of(new Rule().withType(RuleType.ALL).withGoals(List.of(goal(20141021L, 6)))))
        );
        assertThat(exception, hasProperty("columns", equalTo(List.of(TITLE))));
    }

    @Test
    void readValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("(~20141023:6)&(20141021:6)&(20141024:78|20141026:456)")
        ));
        List<Rule> value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(List.of(
                new Rule().withType(RuleType.NOT).withGoals(List.of(goal(20141023L, 6))),
                new Rule().withType(RuleType.OR).withGoals(List.of(goal(20141021L, 6))),
                new Rule()
                        .withGoals(List.of(goal(20141024L, 78),
                                goal(20141026L, 456)))
                        .withType(RuleType.OR))));
    }

    @Test
    void readInvalidValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(List.of("xx")));
        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readInvalidValueWithAndSymbolTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(List.of("(1:1)&")));
        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readInvalidNumberFormatTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(List.of("(1:1234234234234234234)")));
        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readInvalidValueWithoutAndSymbolTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(List.of("(1:1)(1:1)")));
        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readInvalidValueWithNotAndOrSymbolTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(List.of("(~1:1|1:1)")));
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
        SheetRange sheetRange = createStringSheetFromLists(List.of(List.of("")));
        CantReadEmptyException exception = assertThrows(CantReadEmptyException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    private Goal goal(Long goalId, Integer time) {
        return (Goal) new Goal().withId(goalId).withTime(time);
    }

}

package ru.yandex.direct.grid.processing.util.findandreplace;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.findandreplace.ChangeMode;
import ru.yandex.direct.grid.model.findandreplace.ReplaceRule;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ReplaceWhenContainsRuleTest {
    private static final boolean CASE_SENSITIVE = true;
    private static final boolean CASE_INSENSITIVE = false;

    private ReplaceRule replaceRule;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String searchText;

    @Parameterized.Parameter(2)
    public String replaceText;

    @Parameterized.Parameter(3)
    public ChangeMode changeMode;

    @Parameterized.Parameter(4)
    public Boolean caseSensetive;

    @Parameterized.Parameter(5)
    public String expectedText;

    @Parameterized.Parameters(name = "text={0}, searchText={1}, replaceText={2}, changeMode={3}, caseSensetive={4}, " +
            "expectedText = {5}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"текст", "тек", "Начало ", ChangeMode.PREFIX, CASE_SENSITIVE, "Начало текст"},
                {"текст", "ТЕК", "Начало ", ChangeMode.PREFIX, CASE_SENSITIVE, "текст"},

                {"текст", "тек", "Начало ", ChangeMode.PREFIX, CASE_INSENSITIVE, "Начало текст"},
                {"текст", "ТЕК", "Начало ", ChangeMode.PREFIX, CASE_INSENSITIVE, "Начало текст"},
                {"текст", "АЛАРМ", "Начало ", ChangeMode.PREFIX, CASE_INSENSITIVE, "текст"},

                {"текст", null, "Начало ", ChangeMode.PREFIX, CASE_SENSITIVE, "Начало текст"},
                {"текст", null, "Начало ", ChangeMode.PREFIX, CASE_INSENSITIVE, "Начало текст"},

                {"текст", "ТЕК", " в конец", ChangeMode.POSTFIX, CASE_INSENSITIVE, "текст в конец"},
                {"текст", "ТЕК", "замена", ChangeMode.REPLACE, CASE_INSENSITIVE, "замена"},
        });
    }

    @Before
    public void initTestData() {
        replaceRule = new ReplaceWhenContainsRule(searchText, replaceText, changeMode, caseSensetive);
    }

    @Test
    public void checkReplaceRule() {
        assertThat(replaceRule.apply(text))
                .isEqualTo(expectedText);
    }

}

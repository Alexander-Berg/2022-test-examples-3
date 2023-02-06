package ru.yandex.direct.grid.processing.util.findandreplace;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.findandreplace.ReplaceRule;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class DeleteWhenContainsRuleTest {
    private static final boolean CASE_SENSITIVE = true;
    private static final boolean CASE_INSENSITIVE = false;

    private ReplaceRule replaceRule;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String searchText;

    @Parameterized.Parameter(2)
    public Boolean caseSensetive;

    @Parameterized.Parameter(3)
    public String expectedText;

    @Parameterized.Parameters(name = "text={0}, searchText={1}, caseSensetive={2}, expectedText = {5}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"текст", "тек", CASE_SENSITIVE, null},
                {"текст", "ТЕК", CASE_SENSITIVE, "текст"},
                {"текст", "слово", CASE_SENSITIVE, "текст"},

                {"текст", "тек", CASE_INSENSITIVE, null},
                {"текст", "ТЕК", CASE_INSENSITIVE, null},

                {"текст", null, CASE_SENSITIVE, null},
                {"текст", null, CASE_INSENSITIVE, null},
        });
    }

    @Before
    public void initTestData() {
        replaceRule = new DeleteWhenContainsRule(searchText, caseSensetive);
    }

    @Test
    public void checkReplaceRule() {
        assertThat(replaceRule.apply(text))
                .isEqualTo(expectedText);
    }
}

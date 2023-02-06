package ru.yandex.direct.ytwrapper.dynamic;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.ytwrapper.dynamic.dsl.YtDSL;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.ytwrapper.dynamic.YtQueryComposerTest.TestTable.TEST_TABLE;

@RunWith(Parameterized.class)
public class YtQueryComposerAllowedLettersTest {

    private YtQueryComposer queryComposer;
    private YtQueryComposerTest.TestTable table;
    private String expectedComposedQueryValue;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public String queryValue;

    @Parameterized.Parameters(name = "description: {0}, queryValue: {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{"Пустая строка", ""},
                new Object[]{"Одинарная кавычка", "'"},
                new Object[]{"Две одинарные кавычки", "''"},
                new Object[]{"Много одинарных кавычек", "'''', ''''"},
                new Object[]{"Одинарные кавычки в тексте", "a'abc' abc'a"},
                new Object[]{"Двойные кавычки", "abc\"abba\"abc"},
                new Object[]{"Восклицательный знак", "abc!abba!abc"},
                new Object[]{"Квадратные скобки", "abc[abba]abc"},
                new Object[]{"Косая черта", "abc/abba/abc"},
                new Object[]{"Обратная косая черта", "abc\\abba\\abc"},
                new Object[]{"banner_special_chars",
                        "\"!?\\\\()%\\$€;:\\/&'*_=#№«»\\x{00a0}–—−™®©’°⁰¹²³⁴⁵⁶⁷⁸⁹\\x{20bd}"}
        );
    }

    @Before
    public void initTestData() {
        TableMappings tableMappings = () -> ImmutableMap.of(
                TEST_TABLE, "/tmp/test_table"
        );
        queryComposer = new YtQueryComposer(tableMappings);
        table = TEST_TABLE.as("T");

        expectedComposedQueryValue = queryValue
                .replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("'", "\\\\'");
    }


    @Test
    public void testQueryCompose() {
        Select select = YtDSL.ytContext().select(table.id)
                .from(table)
                .where(table.text.eq(queryValue));

        String actualQuery = queryComposer.apply(select);
        String expectedQuery =
                format("T.ID FROM [/tmp/test_table] AS T WHERE T.TEXT = '%s'", expectedComposedQueryValue);

        assertThat(actualQuery)
                .describedAs(description)
                .isEqualTo(expectedQuery);
    }

    @Test
    public void testQueryComposeWithComplexConditions() {
        Long someId = 123L;
        Select select = YtDSL.ytContext().select(table.id)
                .from(table)
                .where(table.id.eq(someId)
                        .and(DSL.val(queryValue).eq(queryValue))
                        .and(YtDSL.isSubstring(queryValue, table.text))
                );

        String actualQuery = queryComposer.apply(select);
        String expectedQuery = format(
                "T.ID FROM [/tmp/test_table] AS T WHERE ( T.ID = %d AND '%s' = '%2$s' AND is_substr('%2$s',T.TEXT) )",
                someId, expectedComposedQueryValue
        );

        assertThat(actualQuery)
                .describedAs(description)
                .isEqualTo(expectedQuery);
    }
}

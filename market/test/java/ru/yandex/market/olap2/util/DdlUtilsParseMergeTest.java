package ru.yandex.market.olap2.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DdlUtilsParseMergeTest {

    private final String inputEngine;
    private final String expectedRegexp;

    public DdlUtilsParseMergeTest(String inputEngine, String expectedRegexp) {
        this.inputEngine = inputEngine;
        this.expectedRegexp = expectedRegexp;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // usual case
                {"Merge('cubes', '^(test_table_1|test_table_2)$')", "^(test_table_1|test_table_2)$"},
                // uses currentDatabase
                {"Merge(currentDatabase(), '^(test_table_1|test_table_2)$')", "^(test_table_1|test_table_2)$"},
                // different regex using brackets
                {"Merge('cubes', '^(test_table_([0-9]*))$')", "^(test_table_([0-9]*))$"},
                // parse with regex
                {"Merge(REGEX('^(cubes|errors)$'), '^(test_table_1|test_table_2)$')", "^(test_table_1|test_table_2)$"},
                // wrong engine name
                {"Merged(currentDatabase(), '^(test_table_1|test_table_2)$')", null},
                // no closed bracket
                {"Merge('cubes', '^(test_table_1|test_table_2)$'", null},
        });
    }

    @Test
    public void testParseTableRegexpFromMergeEngine() {
        String regexParsedExpression = DdlUtils.getTablesRegexpForMerge(inputEngine);
        assertEquals(expectedRegexp, regexParsedExpression);
    }
}

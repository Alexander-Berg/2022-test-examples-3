package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.format;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.parsers.formats.TsvFormatParser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class TsvFormatParserTest {

    @DataProvider
    public static Object[][] parseDataProvider() {
        return new Object[][]{
                new Object[]{"", emptyList(), ImmutableMap.of()},
                new Object[]{"value1\tvalue2\tvalue3", asList("field1", "field2", "field3"), ImmutableMap.of("field1", "value1", "field2", "value2", "field3", "value3")},
                new Object[]{"value1\tvalue2\tvalue3\tunknownfield\t", asList("field1", "field2", "field3"), ImmutableMap.of("field1", "value1", "field2", "value2", "field3", "value3")},
                new Object[]{"value1\tvalue2\tvalue3\t", asList("field1", "field2", "__OTHERS__"), ImmutableMap.of("field1", "value1", "field2", "value2", "__OTHERS__", "value3\t")},
                new Object[]{"value1\tvalue2\tvalue3\tunknownfield\t", asList("field1", "field2", "__OTHERS__"), ImmutableMap.of("field1", "value1", "field2", "value2", "__OTHERS__", "value3\tunknownfield\t")},
                new Object[]{"value1\tvalue2\tvalue3\tvalue4\tvalue5", asList("field1", "field2", "__OTHERS__"), ImmutableMap.of("field1", "value1", "field2", "value2", "__OTHERS__", "value3\tvalue4\tvalue5")},
                new Object[]{"value1\tvalue2\tvalue3\tunknownfield\t", asList("field1", "__OTHERS__", "field3"), ImmutableMap.of("field1", "value1", "__OTHERS__", "value2", "field3", "value3")}
        };
    }

    @DataProvider
    public static Object[][] commaDelimitedDataProvider() {
        return new Object[][]{
                new Object[]{"", emptyList(), ImmutableMap.of()},
                new Object[]{"value1,value2,value3,", asList("field1", "field2", "field3"), ImmutableMap.of("field1", "value1", "field2", "value2", "field3", "value3")},
                new Object[]{"value1,value2,value3,", asList("field1", "field2", "__OTHERS__"), ImmutableMap.of("field1", "value1", "field2", "value2", "__OTHERS__", "value3,")}
        };
    }

    @Test
    @UseDataProvider("parseDataProvider")
    public void parse(String line, List<String> fields, Map<String, String> expectedResult) {
        Map<String, String> result = new TsvFormatParser(fields).parse(line);

        assertThat(result, equalTo(expectedResult));
    }

    @Test
    @UseDataProvider("parseDataProvider")
    public void parseNoFields(String line, List<String> fields, Map<String, String> expectedResult) {
        Map<String, String> result = new TsvFormatParser(emptyList()).parse(line);

        assertThat(result, equalTo(Collections.emptyMap()));
    }

    @Test
    @UseDataProvider("commaDelimitedDataProvider")
    public void parseWithAnotherDelimiter(String line, List<String> fields, Map<String, String> expectedResult) {
        Map<String, String> result = new TsvFormatParser(fields, ',').parse(line);

        assertThat(result, equalTo(expectedResult));
    }
}

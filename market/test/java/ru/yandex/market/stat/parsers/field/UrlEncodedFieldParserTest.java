package ru.yandex.market.stat.parsers.field;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author dzvyagin
 */
@RunWith(DataProviderRunner.class)
public class UrlEncodedFieldParserTest {

    private UrlEncodedFieldParser parser;

    @Before
    public void init() {
        parser = new UrlEncodedFieldParser();
    }

    @DataProvider
    public static Object[][] goodValueDataProvider() {
        return new Object[][]{
                new Object[]{"test", "test"},
                new Object[]{"test%2Ftest", "test/test"},
                new Object[]{"dete23%2Fe2e%252d2", "dete23/e2e%2d2"},
                new Object[]{"test/test", "test/test"}
        };
    }

    @UseDataProvider("goodValueDataProvider")
    @Test
    public void parse(String value, String expected) {
        String result = parser.parse(null, null, value, null);

        assertThat(result, equalTo(expected));
    }

}

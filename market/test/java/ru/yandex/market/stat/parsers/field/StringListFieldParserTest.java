package ru.yandex.market.stat.parsers.field;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class StringListFieldParserTest {

    private StringListFieldParser parser;

    @Before
    public void init() {
        parser = new StringListFieldParser();
    }

    @DataProvider
    public static Object[][] goodValueDataProvider() {
        return new Object[][]{
                new Object[]{"[]", new ArrayList<>()},
                new Object[]{"[  ]", new ArrayList<>()},
                new Object[]{"[,,,]", new ArrayList<>()},
                new Object[]{"[ , , , ]", new ArrayList<>()},
                new Object[]{"[, , ,]", new ArrayList<>()},
                new Object[]{"['a']", singletonList("a")},
                new Object[]{"['a','b','c']", Arrays.asList("a", "b", "c")},
                new Object[]{"[, ,'a', 'b',,'c',]", Arrays.asList("a", "b", "c")},
                new Object[]{"['a, b,,c',]", singletonList("a, b,,c")},
                new Object[]{"['','a\\'b\\'']", Arrays.asList("", "a\\'b\\'")},
                new Object[]{"['a\\\\','\\b']", Arrays.asList("a\\\\", "\\b")},
        };
    }

    @DataProvider
    public static Object[][] badValueDataProvider() {
        return new Object[][]{
                new Object[]{""},
                new Object[]{"["},
                new Object[]{"]"},
                new Object[]{"[a]"},
                new Object[]{"['a]"},
                new Object[]{"[a']"},
                new Object[]{"[a,b,c]"},
                new Object[]{"['a',b,'c']"},
                new Object[]{"['a', 'b' ,c]"},
                new Object[]{"['a, 'b' ,c]"},
                new Object[]{"['a, b ,c]"},
        };
    }

    @UseDataProvider("goodValueDataProvider")
    @Test
    public void parse(String value, List<String> expected) {
        List<String> result = parser.parse(null, null, value, null);

        assertThat(result, equalTo(expected));
    }

    @UseDataProvider("badValueDataProvider")
    @Test(expected = IllegalStateException.class)
    public void parseWithError(String value) {
        parser.parse(null, null, value, null);
    }
}

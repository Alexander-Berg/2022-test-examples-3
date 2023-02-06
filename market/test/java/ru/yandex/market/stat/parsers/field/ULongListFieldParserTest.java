package ru.yandex.market.stat.parsers.field;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class ULongListFieldParserTest {

    private ULongListFieldParser parser;

    @Before
    public void init() {
        parser = new ULongListFieldParser();
    }

    @DataProvider
    public static Object[][] goodValueDataProvider() {
        return new Object[][]{
                new Object[]{"[]", emptyList()},
                new Object[]{"[  ]", emptyList()},
                new Object[]{"[,,,]", emptyList()},
                new Object[]{"[ , , , ]", emptyList()},
                new Object[]{"[, , ,]", emptyList()},
                new Object[]{"[1]", singletonList(1L)},
                new Object[]{"[1,2,3]", Arrays.asList(1L, 2L, 3L)},
                new Object[]{"[, ,1, 2,,3,]", Arrays.asList(1L, 2L, 3L)},
                new Object[]{"[123456789000]", singletonList(123456789000L)},

        };
    }

    @DataProvider
    public static Object[][] badValueDataProvider() {
        return new Object[][]{
                new Object[]{""},
                new Object[]{"["},
                new Object[]{"]"},
                new Object[]{"['1']"},
                new Object[]{"['1]"},
                new Object[]{"[1']"},
                new Object[]{"[-1]"},
                new Object[]{"[1,-1,1]"},
                new Object[]{"[a,b,c]"},
                new Object[]{"['a',b,'c']"},
                new Object[]{"[1, b ,7]"},
                new Object[]{"[1234566677899973739000]"},
        };
    }

    @UseDataProvider("goodValueDataProvider")
    @Test
    public void parse(String value, List<String> expected) {
        List<Long> result = parser.parse(null, null, value, null);

        assertThat(result, equalTo(expected));
    }

    @UseDataProvider("badValueDataProvider")
    @Test(expected = IllegalArgumentException.class)
    public void parseWithError(String value) {
        parser.parse(null, null, value, null);
    }
}

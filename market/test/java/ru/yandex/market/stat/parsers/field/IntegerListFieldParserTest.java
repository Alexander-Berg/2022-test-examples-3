package ru.yandex.market.stat.parsers.field;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Kate Lebedeva <kateleb@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class IntegerListFieldParserTest {

    private IntegerListFieldParser parser;

    private List<Integer> intField;

    @Before
    public void init() {
        parser = new IntegerListFieldParser();
    }

    @DataProvider
    public static Object[][] goodValueDataProvider() {
        return new Object[][]{
            new Object[]{"[]", emptyList()},
            new Object[]{"", emptyList()},
            new Object[]{"{}", emptyList()},
            new Object[]{"[  ]", emptyList()},
            new Object[]{"[,,,]", emptyList()},
            new Object[]{"[ , , , ]", emptyList()},
            new Object[]{"[, , ,]", emptyList()},
            new Object[]{"[1]", singletonList(1)},
            new Object[]{"[1,2,3]", Arrays.asList(1, 2, 3)},
            new Object[]{"['1','2','3']", Arrays.asList(1, 2, 3)},
            new Object[]{"[\"1\",\"2\",\"3\"]", Arrays.asList(1, 2, 3)},
            new Object[]{"{1,2,3}", Arrays.asList(1, 2, 3)},
            new Object[]{"[-1]", singletonList(-1)},
            new Object[]{"[1,-1,1]",Arrays.asList(1, -1, 1)},
            new Object[]{"[, ,1, 2,,3,]", Arrays.asList(1, 2, 3)},
        };
    }

    @DataProvider
    public static Object[][] badValueDataProvider() {
        return new Object[][]{
            new Object[]{"["},
            new Object[]{"]"},
            new Object[]{"}"},
            new Object[]{"['1]"},
            new Object[]{"[1']"},
            new Object[]{"[a,b,c]"},
            new Object[]{"['a',b,'c']"},
            new Object[]{"[1, b ,7]"},
            new Object[]{"[123456789000]"},
        };
    }

    @UseDataProvider("goodValueDataProvider")
    @Test
    public void parse(String value, List<String> expected) {
        List<Integer> result = parser.parse(null, getField(), value, null);

        assertThat(result, equalTo(expected));
    }

    @UseDataProvider("badValueDataProvider")
    @Test(expected = IllegalArgumentException.class)
    public void parseWithError(String value) {
        parser.parse(null, getField(), value, null);
    }

    private Field getField() {
        Field intField = null;
        try {
            intField = this.getClass().getDeclaredField("intField");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return intField;
    }
}

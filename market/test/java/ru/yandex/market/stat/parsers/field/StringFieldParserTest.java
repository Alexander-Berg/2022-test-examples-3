package ru.yandex.market.stat.parsers.field;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.parsers.annotations.Copy;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class StringFieldParserTest {

    private StringFieldParser parser;

    @Before
    public void init() {
        parser = new StringFieldParser();
    }

    @DataProvider
    public static Object[][] goodValueDataProvider() {
        return new Object[][]{
                new Object[]{testField("fieldWithoutCopy"), "", ""},
                new Object[]{testField("fieldWithoutCopy"), "a", "a"},
                new Object[]{testField("fieldWithoutCopy"), "aa", "aa"},
                new Object[]{testField("fieldWithoutCopy"), "aaa", "aaa"},

                new Object[]{testField("fieldWithEmptyCopy"), "", ""},
                new Object[]{testField("fieldWithEmptyCopy"), "a", "a"},
                new Object[]{testField("fieldWithEmptyCopy"), "aa", "aa"},
                new Object[]{testField("fieldWithEmptyCopy"), "aaa", "aaa"},

                new Object[]{testField("fieldWithMaxLength2"), "a", "a"},
                new Object[]{testField("fieldWithMaxLength2"), "aa", "aa"},

                new Object[]{testField("fieldWithTruncateTo2"), "", ""},
                new Object[]{testField("fieldWithTruncateTo2"), "a", "a"},
                new Object[]{testField("fieldWithTruncateTo2"), "ab", "ab"},
                new Object[]{testField("fieldWithTruncateTo2"), "abc", "ab"},

                new Object[]{testField("fieldWithSkipLeadingZeroes"), "", ""},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "0", "0"},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "00000", "0"},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "1", "1"},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "00000001", "1"},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "10", "10"},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "000000010", "10"},
                new Object[]{testField("fieldWithSkipLeadingZeroes"), "100000001", "100000001"},

                new Object[]{testField("fieldWithMaxLength2AndTruncateTo2"), "", ""},
                new Object[]{testField("fieldWithMaxLength2AndTruncateTo2"), "a", "a"},
                new Object[]{testField("fieldWithMaxLength2AndTruncateTo2"), "ab", "ab"},
                new Object[]{testField("fieldWithMaxLength2AndTruncateTo2"), "abc", "ab"},

                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "", ""},
                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "0", "0"},
                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "10", "10"},
                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "010", "10"},
                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "00000010", "10"},

                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "", ""},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "a", "a"},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "ab", "ab"},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "abc", "ab"},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "0", "0"},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "10", "10"},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "00000010", "10"},
                new Object[]{testField("fieldWithTruncateTo2AndSkipLeadingZeroes"), "000000101", "10"},

                new Object[]{testField("fieldWithAllConstrains2"), "", ""},
                new Object[]{testField("fieldWithAllConstrains2"), "a", "a"},
                new Object[]{testField("fieldWithAllConstrains2"), "ab", "ab"},
                new Object[]{testField("fieldWithAllConstrains2"), "abc", "ab"},
                new Object[]{testField("fieldWithAllConstrains2"), "00000010", "10"},
                new Object[]{testField("fieldWithAllConstrains2"), "000000101", "10"},
        };
    }

    @DataProvider
    public static Object[][] badValueDataProvider() {
        return new Object[][]{
                new Object[]{testField("fieldWithMaxLength2"), "aaa"},
                new Object[]{testField("fieldWithMaxLength2"), "aaaaaa"},

                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "101"},
                new Object[]{testField("fieldWithMaxLength2AndSkipLeadingZeroes"), "00000101"},
        };
    }

    @UseDataProvider("goodValueDataProvider")
    @Test
    public void parse(Field field, String value, String expected) {
        Copy copy = field.getAnnotation(Copy.class);
        String result = parser.parse(copy, field, value, null);

        assertThat(result, equalTo(expected));
    }

    @UseDataProvider("badValueDataProvider")
    @Test(expected = IllegalArgumentException.class)
    public void parseWithError(Field field, String value) {
        Copy copy = field.getAnnotation(Copy.class);
        parser.parse(copy, field, value, null);
    }

    private static Field testField(String fieldName) {
        try {
            return TestStringFieldContainer.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    static class TestStringFieldContainer {

        private String fieldWithoutCopy;

        @Copy
        private String fieldWithEmptyCopy;

        @Copy(maxLength = 2)
        private String fieldWithMaxLength2;

        @Copy(skipLeadingZeroes = true)
        private String fieldWithSkipLeadingZeroes;

        @Copy(truncateTo = 2)
        private String fieldWithTruncateTo2;

        @Copy(maxLength = 2, truncateTo = 2)
        private String fieldWithMaxLength2AndTruncateTo2;

        @Copy(maxLength = 2, skipLeadingZeroes = true)
        private String fieldWithMaxLength2AndSkipLeadingZeroes;

        @Copy(truncateTo = 2, skipLeadingZeroes = true)
        private String fieldWithTruncateTo2AndSkipLeadingZeroes;

        @Copy(maxLength = 2, truncateTo = 2, skipLeadingZeroes = true)
        private String fieldWithAllConstrains2;
    }
}

package ru.yandex.market.stat.parsers.field;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.parsers.annotations.Copy;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by kateleb on 07.04.17.
 */
@RunWith(DataProviderRunner.class)
public class BigDecimalFieldParserTest {

    private BigDecimalFieldParser parser;

    @Before
    public void init() {
        parser = new BigDecimalFieldParser();
    }

    @DataProvider
    public static Object[][] badValuesProvider() {
        return new Object[][]{
                new Object[]{testField("fieldWithoutCopy"), "aaa"},
                new Object[]{testField("fieldWithoutCopy"), ""},
                new Object[]{testField("fieldLengh6"), "-1295667"},
                new Object[]{testField("fieldLengh6"), "5295667"},
        };
    }

    @DataProvider
    public static Object[][] goodValuesProvider() {
        return new Object[][]{
                new Object[]{testField("fieldWithoutCopy"), "33", new BigDecimal(33)},
                new Object[]{testField("fieldWithoutCopy"), "0,33", new BigDecimal((float) 0.33).setScale(2, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldWithoutCopy"), "0.33", new BigDecimal((float)0.33).setScale(2, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldWithoutCopy"), "-195", new BigDecimal(-195)},
                new Object[]{testField("fieldWithEmptyCopy"), "35", new BigDecimal(35)},
                new Object[]{testField("fieldWithEmptyCopy"), "0,36", new BigDecimal((float) 0.36).setScale(2, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldWithEmptyCopy"), "0.36", new BigDecimal((float)0.36).setScale(2, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldWithEmptyCopy"), "-1295", new BigDecimal(-1295)},
                new Object[]{testField("fieldLengh6"), "0,37", new BigDecimal((float) 0.37).setScale(2, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldLengh6"), "0.37", new BigDecimal((float)0.37).setScale(2, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldLengh6"), "-1295", new BigDecimal(-1295)},
                new Object[]{testField("fieldLengh6"), "-129566", new BigDecimal(-129566)},
                new Object[]{testField("fieldLengh6"), "-0.129569", new BigDecimal(-0.129569).setScale(6, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldLengh6"), "-0.129567777777", new BigDecimal(-0.129567777777).setScale(12, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldLengh6"), "-0.129567777777", new BigDecimal(-0.129567777777).setScale(12, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldLengh6"), "-555.129567777777", new BigDecimal(-555.129567777777).setScale(12, BigDecimal.ROUND_FLOOR)},
                new Object[]{testField("fieldLengh6"), "999999.555", new BigDecimal(999999.555).setScale(3, BigDecimal.ROUND_FLOOR)},

        };
    }


    @UseDataProvider("goodValuesProvider")
    @Test
    public void parse(Field field, String value, BigDecimal expected) {
        Copy copy = field.getAnnotation(Copy.class);
        BigDecimal result = parser.parse(copy, field, value, null);
        assertThat(result, equalTo(expected));
    }

    @UseDataProvider("badValuesProvider")
    @Test(expected = IllegalArgumentException.class)
    public void parseWithError(Field field, String value) {
        Copy copy = field.getAnnotation(Copy.class);
        parser.parse(copy, field, value, null);
    }

    private static Field testField(String fieldName) {
        try {
            return BigDecimalFieldsFactory.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static class BigDecimalFieldsFactory {

        private BigDecimal fieldWithoutCopy;

        @Copy
        private BigDecimal fieldWithEmptyCopy;

        @Copy(maxLength = 6)
        private BigDecimal fieldLengh6;
    }
}

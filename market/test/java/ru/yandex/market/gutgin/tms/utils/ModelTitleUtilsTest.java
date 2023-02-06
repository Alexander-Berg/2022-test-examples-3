package ru.yandex.market.gutgin.tms.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author s-ermakov
 */
@RunWith(Parameterized.class)
public class ModelTitleUtilsTest {

    private final String title1;
    private final String title2;
    private final boolean result;

    public ModelTitleUtilsTest(String title1, String title2, boolean result) {
        this.title1 = title1;
        this.title2 = title2;
        this.result = result;
    }

    @Test
    public void test() {
        boolean isEqual = ModelTitleUtils.marketEqualTitles(title1, title2);
        Assert.assertEquals(result, isEqual);
    }

    @Parameterized.Parameters(name = "{index}: \"{0}\" == \"{1}\" ? It is {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "", "", true },
            { "Title 1", "Title 1", true },
            { "TiTle 1", "title 1", true },
            { "Title1", "Title   1", true },
            { "Title 165 7!?;", "Title, 165 7.:.", true },
            { "Title 165 7;", "Title, 165|7:..", false },
            { "Название 1", "Название 1", true },
            { "Название & Name 1", "название &  NAME 1;", true },
            { "Название & Name 1", "имя &  NAME 1;", false },
            { "Title 1 и русские слова", "Title 1 и нерусские слова", false },
            { "Title 1", "Title 2", false },
            { "title1", "TITLE_2", false },
            { "некоторое random title", "another случайное название", false },
            { "Модель +", "модель", false },
        });
    }
}

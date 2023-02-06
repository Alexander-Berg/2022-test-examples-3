package ru.yandex.direct.core.entity.auction.utils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class BsRequestUtilsReplaceTemplateTest {
    @Test
    @Parameters
    public void replaceTemplateTextWorksCorrect(String text, String expected) {
        assertThat(BsAuctionUtils.replaceTemplateText(text))
                .isEqualTo(expected);
    }

    public static Object[][] parametersForReplaceTemplateTextWorksCorrect() {
        return new Object[][]{
                {null, null},
                {"москва", "москва"},
                {" текст  баннера с #темплейтом#\t", "текст баннера с TMPLPHRASE"},
                {" текст  баннера с #темплейтом# и #еще одним#\t", "текст баннера с TMPLPHRASE и TMPLPHRASE"},
        };
    }
}

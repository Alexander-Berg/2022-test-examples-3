package ru.yandex.direct.core.entity.auction.utils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class BsAuctionUtilsTest {
    @Test
    @Parameters
    public void prepareTextWorksCorrect(String text, String expected) {
        assertThat(BsAuctionUtils.prepareText(text))
                .isEqualTo(expected);
    }

    public static Object[][] parametersForPrepareTextWorksCorrect() {
        return new Object[][]{
                {null, null},
                {"москва", "москва"},
                {"+из Питера !в +москву", "из Питера в москву"},
                {"мос\n\r   ква", "мос ква"},
                {"+из Питера -в -москву", "из Питера"},
                {"из-Питера-в-москву", "из Питера в москву"},
                {"  москва\n ", "москва"},
                {"  tramplin\n ", "tramplin"}, /* была проблема, когда вместо символов "\r\n" вырезали из фразы "\\r\\n" */
        };
    }

}

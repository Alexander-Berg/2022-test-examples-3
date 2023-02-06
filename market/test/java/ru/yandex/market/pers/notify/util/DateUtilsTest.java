package ru.yandex.market.pers.notify.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @author dinyat
 *         06/04/2017
 */
public class DateUtilsTest {

    @Test
    public void testFormatDate() throws Exception {
        String result = DateUtils.formatDate(new Date(0L));

        Assertions.assertEquals("01.01.1970", result);
    }

}

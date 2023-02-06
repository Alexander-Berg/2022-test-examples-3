package ru.yandex.market.markup2.utils.priorities;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author york
 * @since 19.01.2020
 */
public class YangPrioritiesHelperTest {

    @Test
    public void calculatePriority() {

        BigDecimal priority1 = getPriority("2021-01-21",true, false, false, 90401);
        BigDecimal priority2 = getPriority("2021-01-22",true, false, false, 90401);
        // deadline1 раньше чем deadline2
        Assert.assertTrue(priority1.doubleValue() > priority2.doubleValue());

        BigDecimal priority3 = getPriority("2021-01-22",true, false, false, 90401);
        BigDecimal priority4 = getPriority("2021-01-22",false, false, false, 90401);
        // critical с бОльшим приоритетом
        Assert.assertTrue(priority3.doubleValue() > priority4.doubleValue());

        BigDecimal priority5 = getPriority("2021-01-22",true, true, true, 90401);
        BigDecimal priority6 = getPriority("2021-01-22",true, false, true, 90401);
        //
        Assert.assertTrue(priority5.doubleValue() > priority6.doubleValue());

        BigDecimal priority7 = getPriority("2021-01-22",true, false, false, 90401);
        BigDecimal priority8 = getPriority("2021-01-22",false, true, true, 90401);
        //
        Assert.assertTrue(priority7.doubleValue() > priority8.doubleValue());



    }

    private BigDecimal getPriority(String date, boolean critical, boolean inspection, boolean custom, long categoryId) {
        return YangPrioritiesHelper.calculatePriority(
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay(),
                critical, inspection, custom, categoryId
        );
    }
}

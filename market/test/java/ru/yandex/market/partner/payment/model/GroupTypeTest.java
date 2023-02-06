package ru.yandex.market.partner.payment.model;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.core.stats.model.GroupType;

import static org.junit.Assert.assertEquals;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
class GroupTypeTest {

    @Test
    void testFloorMonth() throws Exception {
        Date firstDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-11");
        Date lastDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-15");
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-06-30"),
                GroupType.BY_MONTH.generateFloor(firstDay, lastDay));
        // also test date with seconds
        firstDay = new Date(DateUtil.DEFAULT_FORMAT.parse("2005-07-30").getTime() - 50000);
        lastDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-15");
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-07-31"),
                GroupType.BY_MONTH.generateFloor(firstDay, lastDay));
    }

    @Test
    void testCeilMonth() throws Exception {
        Date firstDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-11");
        Date lastDay = new Date(); // also test date with seconds
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-06-01"),
                GroupType.BY_MONTH.generateCeil(firstDay, lastDay));
        firstDay = lastDay;
        lastDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-15");
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-06-01"),
                GroupType.BY_MONTH.generateCeil(firstDay, lastDay));
    }

    @Test
    void testRoundWeek() throws Exception {
        Date firstDay = DateUtil.DEFAULT_FORMAT.parse("2005-07-13");
        Date lastDay = DateUtil.DEFAULT_FORMAT.parse("2005-07-15");
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-07-11"),
                GroupType.BY_WEEK.generateCeil(lastDay, firstDay));
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-07-17"),
                GroupType.BY_WEEK.generateFloor(firstDay, lastDay));
        firstDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-28");
        lastDay = DateUtil.DEFAULT_FORMAT.parse("2005-06-29");
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-06-27"),
                GroupType.BY_WEEK.generateCeil(lastDay, firstDay));
        assertEquals(
                DateUtil.DEFAULT_FORMAT.parse("2005-07-03"),
                GroupType.BY_WEEK.generateFloor(firstDay, lastDay));
    }

    @Test
    void testRoundDay() throws Exception {
        Date firstDay = DateUtil.DEFAULT_FORMAT.parse("2005-07-13");
        Date lastDay = DateUtil.DEFAULT_FORMAT.parse("2005-07-15");
        assertEquals(
                firstDay,
                GroupType.BY_DAY.generateCeil(lastDay, firstDay));
        assertEquals(
                lastDay,
                GroupType.BY_DAY.generateFloor(firstDay, lastDay));
    }
}

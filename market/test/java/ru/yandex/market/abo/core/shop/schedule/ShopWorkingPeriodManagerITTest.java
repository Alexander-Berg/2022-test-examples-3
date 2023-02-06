package ru.yandex.market.abo.core.shop.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 * @date 26.04.18.
 */
public class ShopWorkingPeriodManagerITTest extends EmptyTest {

    @Autowired
    private ShopWorkingPeriodManager shopWorkingPeriodManager;

    private static DayOfWeek[] DAYS_1 = {MONDAY, TUESDAY, WEDNESDAY};
    private static DayOfWeek[] DAYS_2 = {MONDAY, TUESDAY};
    private static DayOfWeek[] DAYS_3 = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY};
    private static DayOfWeek[] DAYS_4 = {SATURDAY, SUNDAY};
    private static DayOfWeek[] DAYS_5 = {MONDAY, TUESDAY};

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    public void testAdd() {
        shopWorkingPeriodManager.addWorkingPeriod(1, "12:00", "18:00", DAYS_1, null);

        List<ShopWorkingPeriod> periods = shopWorkingPeriodManager.loadWorkingPeriods(1);
        assertEquals(1, periods.size());

        ShopWorkingPeriod period = periods.get(0);
        assertEquals(1, period.getShopId());
        assertEquals(LocalTime.parse("12:00"), period.getFromTime());
        assertEquals(LocalTime.parse("18:00"), period.getToTime());
        assertArrayEquals(DAYS_1, period.getDaysOfWeek());
    }

    @Test
    public void testUpdate() {
        shopWorkingPeriodManager.addWorkingPeriod(2, "12:00", "18:00", DAYS_1, null);
        ShopWorkingPeriod period = shopWorkingPeriodManager.loadWorkingPeriods(2).get(0);

        shopWorkingPeriodManager.updateWorkingPeriod(period.getId(), "10:01", "17:00", DAYS_2, null);
        period = shopWorkingPeriodManager.loadWorkingPeriods(2).get(0);
        assertEquals(LocalTime.parse("10:01"), period.getFromTime());
        assertEquals(LocalTime.parse("17:00"), period.getToTime());
        assertArrayEquals(DAYS_2, period.getDaysOfWeek());
    }

    @Test
    public void testNotDefined() {
        List<ShopWorkingPeriod> periods = shopWorkingPeriodManager.loadWorkingPeriods(3);
        assertEquals(1, periods.size());
        ShopWorkingPeriod period = periods.get(0);
        assertEquals(ShopWorkingPeriodManager.DEFAULT_START, period.getFromTime());
        assertEquals(ShopWorkingPeriodManager.DEFAULT_END, period.getToTime());
        assertArrayEquals(ShopWorkingPeriodManager.WORKDAYS, period.getDaysOfWeek());
    }

    @Test
    public void testDelete() {
        shopWorkingPeriodManager.addWorkingPeriod(4, "10:00", "18:00", DAYS_3, null);
        shopWorkingPeriodManager.addWorkingPeriod(4, "10:00", "14:00", DAYS_4, null);
        List<ShopWorkingPeriod> periods = shopWorkingPeriodManager.loadWorkingPeriods(4);
        assertEquals(2, periods.size());

        ShopWorkingPeriod period = periods.stream()
                .filter(p -> Arrays.equals(DAYS_4, p.getDaysOfWeek()))
                .findFirst().orElse(null);

        assertNotNull(period);
        shopWorkingPeriodManager.deleteWorkingPeriod(period.getId(), null);

        periods = shopWorkingPeriodManager.loadWorkingPeriods(4);
        assertEquals(1, periods.size());
    }

    @Test
    public void testLoadTodayPeriods() {
        long shopId = 5;
        int regionId = 1;

        // ПН, ВТ
        shopWorkingPeriodManager.addWorkingPeriod(shopId, "03:00", "10:00", DAYS_5, null);

        // difference with MSK timezone: 8 - 3 = 5 hours
        pgJdbcTemplate.update("INSERT INTO region (id, tz_offset) VALUES (?, 8)", regionId);
        pgJdbcTemplate.update("INSERT INTO ext_shop_region (datasource_id, region_id) VALUES (?, ?)",
                shopId, regionId);

        // ПН-ВТ [03:00 - 10:00] => ВС-ВТ [22:00 - 05:00] ~ (ПН-ВТ [00:00 - 05:00], ВС-ПН [22:00 - 23:59])
        List<ShopWorkingPeriod> periodsInMskTz =
                shopWorkingPeriodManager.loadWorkingPeriodInMskTimezone(shopId, DayOfWeek.MONDAY);
        assertEquals(2, periodsInMskTz.size());

        ShopWorkingPeriod first = new ShopWorkingPeriod(shopId, LocalTime.parse("22:00"), LocalTime.parse("23:59"),
                new DayOfWeek[]{MONDAY, SUNDAY});
        ShopWorkingPeriod second = new ShopWorkingPeriod(shopId, LocalTime.parse("00:00"), LocalTime.parse("05:00"),
                new DayOfWeek[]{MONDAY, TUESDAY});
        assertTrue(periodsInMskTz.contains(first));
        assertTrue(periodsInMskTz.contains(second));
    }
}

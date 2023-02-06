package ru.yandex.market.abo.core.shop.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 25.04.18.
 */
public class ShopWorkingPeriodRepoTest extends EmptyTest {

    private static final LocalTime TEN_AM = LocalTime.of(10, 0);
    private static final LocalTime MIDDAY = LocalTime.of(12, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);

    @Autowired
    private ShopWorkingPeriodRepo shopScheduleRepo;

    @Test
    public void testRepo() {
        shopScheduleRepo.save(new ShopWorkingPeriod(1, TEN_AM, SIX_PM));
        shopScheduleRepo.save(new ShopWorkingPeriod(1, MIDDAY, SIX_PM));
        shopScheduleRepo.save(new ShopWorkingPeriod(2, TEN_AM, MIDDAY));

        assertEquals(2, load(1).size());
        assertEquals(1, load(2).size());
    }

    @Test
    public void testDaysOfWeek() {
        shopScheduleRepo.save(new ShopWorkingPeriod(3, TEN_AM, SIX_PM));
        ShopWorkingPeriod loaded = load(3).get(0);

        DayOfWeek[] workingDays = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY};
        loaded.setDaysOfWeek(workingDays);
        shopScheduleRepo.save(loaded);

        loaded = load(3).get(0);
        assertArrayEquals(workingDays, loaded.getDaysOfWeek());
    }

    private List<ShopWorkingPeriod> load(long shopId) {
        return shopScheduleRepo.findAllByShopIdIn(List.of(shopId));
    }
}

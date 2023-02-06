package ru.yandex.market.abo.core.billing.bonus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static java.time.Month.APRIL;
import static java.time.Month.MARCH;
import static java.time.Month.MAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 07.06.18.
 */
public class BillingBonusServiceTest extends EmptyTest {

    @Autowired
    private BillingBonusService billingBonusService;

    @Test
    public void testLoadByUserIdsAndDateBetween() {
        billingBonusService.save(1, 2018, APRIL, 15);
        billingBonusService.save(2, 2017, APRIL, 10); // another year
        billingBonusService.save(3, 2018, APRIL, 20); // not in searched userIds
        billingBonusService.save(4, 2018, MAY, 10); // another month
        Map<Long, Integer> loaded = billingBonusService.loadForSalary(Arrays.asList(1L, 2L, 4L, 5L),
                LocalDate.of(2018, MARCH, 31), LocalDate.of(2018, MAY, 3));
        assertEquals(1, loaded.size());
        assertEquals(15, (int) loaded.get(1L));

        loaded = billingBonusService.loadForSalary(Arrays.asList(1L, 2L, 4L, 5L),
                LocalDate.of(2018, APRIL, 5), LocalDate.of(2018, APRIL, 25));
        assertEquals(1, loaded.size());
        assertEquals(15, (int) loaded.get(1L));
    }
}

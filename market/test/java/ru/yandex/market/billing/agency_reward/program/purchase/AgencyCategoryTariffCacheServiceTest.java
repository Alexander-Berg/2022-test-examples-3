package ru.yandex.market.billing.agency_reward.program.purchase;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_reward.program.purchase.model.RewardTariff;
import ru.yandex.market.billing.agency_reward.program.purchase.tariff.AgencyCategoryTariffCacheService;
import ru.yandex.market.billing.agency_reward.program.purchase.tariff.AgencyCategoryTariffSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Тесты для {@link AgencyCategoryTariffCacheService}.
 */
public class AgencyCategoryTariffCacheServiceTest extends FunctionalTest {

    private static final LocalDate LD_2021_1_1 = LocalDate.of(2021, 1, 1);

    @Autowired
    private AgencyCategoryTariffSupplier tariffSupplier;

    private static AgencyCategoryTariffCacheService service;

    @Test
    void testGetTariff() {
        service = new AgencyCategoryTariffCacheService(tariffSupplier, LD_2021_1_1);

        Optional<RewardTariff> tariff = service.getTariff(198118L, null);
        assertTrue(tariff.isPresent());
        assertEquals(200, tariff.get().getValue());

        tariff = service.getTariff(198119L, null);
        assertTrue(tariff.isPresent());
        assertEquals(200, tariff.get().getValue());

        tariff = service.getTariff(90401, null);
        assertTrue(tariff.isPresent());
        assertEquals(400, tariff.get().getValue());
    }
}

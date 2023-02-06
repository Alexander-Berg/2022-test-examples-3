package ru.yandex.market.loyalty.core.dao;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.EmissionLogRecord;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.dao.EmissionLogCleaner.CLEAN_LOGS_AFTER_DAYS;
import static ru.yandex.market.loyalty.core.model.EmissionLogErrorType.COIN_CREATION_FAILURE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SHOP_PROMO_ID;

/**
 * @author artemmz
 */
public class EmissionLogCleanerTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final int EMISSION_LOG_CNT = 100;
    @Autowired
    EmissionLogCleaner emissionLogCleaner;
    @Autowired
    PromoManager promoManager;
    @Autowired
    PromoService promoService;
    @Autowired
    EmissionLogService emissionLogService;
    @Autowired
    EmissionLogDao emissionLogDao;

    @Before
    public void setUp() throws Exception {
        configurationService.set(ConfigurationService.EMISSION_LOG_CLEANING_ENABLED, true);
    }

    @Test
    public void testObsoleteLogCleaning() {
        Instant now = clock.instant();
        Promo activePromo = createSomePromo("foo", Date.from(now.plus(CLEAN_LOGS_AFTER_DAYS * 5, ChronoUnit.DAYS)));
        Promo notActivePromo = createSomePromo("bar", Date.from(now.minus(1, ChronoUnit.SECONDS)));

        promoService.updateStatus(notActivePromo, PromoStatus.INACTIVE);
        assertEquals(PromoStatus.ACTIVE, promoService.getPromo(activePromo.getPromoId().getId()).getStatus());
        assertEquals(PromoStatus.INACTIVE, promoService.getPromo(notActivePromo.getPromoId().getId()).getStatus());

        emissionLogService.addEmissionLog(
                Stream.iterate(0, i -> i + 1)
                        .limit(EMISSION_LOG_CNT)
                        .map(i -> i % 2 == 0 ? recordFrom(activePromo) : recordFrom(notActivePromo))
                        .collect(Collectors.toList())
        );

        Stream.of(activePromo, notActivePromo).forEach(p -> {
            assertTrue(emissionLogDao.anyEmissionLogRecordExists(p.getPromoId().getId()));
        });
        clock.spendTime(CLEAN_LOGS_AFTER_DAYS + 1, ChronoUnit.DAYS);

        emissionLogCleaner.cleanObsoleteLogs(Duration.ofSeconds(5)); //clean only !ACTIVE promo logs older than
        // CLEAN_LOGS_AFTER_DAYS

        assertTrue(emissionLogDao.anyEmissionLogRecordExists(activePromo.getPromoId().getId()));

        assertFalse(emissionLogDao.anyEmissionLogRecordExists(notActivePromo.getPromoId().getId()));
    }

    private Promo createSomePromo(String promocode, Date endDate) {
        return promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setClid(12345L)
                        .setShopPromoId(SHOP_PROMO_ID + promocode)
                        .setCode(promocode)
                        .setEndDate(endDate)
                        .setBudget(BigDecimal.valueOf(100_500))
                        .setEmissionBudget(BigDecimal.valueOf(100_500))
        );
    }

    private EmissionLogRecord recordFrom(Promo promo) {
        return new EmissionLogRecord(COIN_CREATION_FAILURE, "ups", "smth went wrong",
                RandomUtils.nextLong(),
                "request key " + promo.getPromoKey(),
                promo.getPromoId().getId()
        );
    }
}

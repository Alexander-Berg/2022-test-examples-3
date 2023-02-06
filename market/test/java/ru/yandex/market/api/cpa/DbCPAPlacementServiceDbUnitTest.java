package ru.yandex.market.api.cpa;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты на {@link DbCPAPlacementService}.
 *
 * @author stani
 */
@DbUnitDataSet(before = {
        "DbCPAPlacementServiceDbUnitTest.before.csv",
        "create_shop_meta_data.before.csv",
})
class DbCPAPlacementServiceDbUnitTest extends FunctionalTest {
    private static final long DATASOURCE_ID = 774L;

    @Autowired
    private DbCPAPlacementService cpaPlacementService;

    @BeforeEach
    void setUp() {
        cpaPlacementService.init();
    }

    @Test
    void isCpaAliveTest() {
        assertThat(cpaPlacementService.isCpaAlive(DATASOURCE_ID, null)).isTrue();
        assertThat(cpaPlacementService.isCpaAlive(DATASOURCE_ID, new Date())).isTrue();
        assertThat(cpaPlacementService.isCpaAlive(DATASOURCE_ID, Date.from(Instant.now().minus(Duration.ofDays(1))))).isTrue();
        assertThat(cpaPlacementService.isCpaAlive(DATASOURCE_ID, Date.from(Instant.now().minus(Duration.ofDays(31))))).isFalse();
        assertThat(cpaPlacementService.isCpaAlive(-1, null)).isFalse();

        var cpaPaymentTypes = cpaPlacementService.getCPAPaymentTypes(Set.of(DATASOURCE_ID));
        assertThat(cpaPaymentTypes)
                .containsOnlyKeys(DATASOURCE_ID)
                .hasEntrySatisfying(DATASOURCE_ID, pt -> {
                    assertThat(pt.getProdPaymentClass()).isEqualTo(PaymentClass.OFF);
                    assertThat(pt.getSbxPaymentClass()).isEqualTo(PaymentClass.SHOP);
                });
    }

    @Test
    void getAllCpaRegions() {
        Set<Long> shopDeliveryRegions = new HashSet<>(cpaPlacementService.getAllCpaRegions(777L));
        MatcherAssert.assertThat(shopDeliveryRegions, Matchers.containsInAnyOrder(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    void getAllCpaRegionsIfNull() {
        Set<Long> shopDeliveryRegions = new HashSet<>(cpaPlacementService.getAllCpaRegions(778L));
        Assertions.assertTrue(shopDeliveryRegions.isEmpty());
    }
}

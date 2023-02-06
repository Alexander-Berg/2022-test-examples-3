package ru.yandex.market.billing.sortingcenter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.sortingcenter.model.SCOperationCost;
import ru.yandex.market.billing.sortingcenter.model.SCServiceType;
import ru.yandex.market.billing.sortingcenter.model.SCTariffDTO;
import ru.yandex.market.billing.sortingcenter.tariff.SCTariffHolder;
import ru.yandex.market.billing.sortingcenter.utils.BillingUnit;
import ru.yandex.market.billing.sortingcenter.utils.SCOperationCounter;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.billing.sortingcenter.model.SCServiceType.ORDER_SHIPPED_TO_SO_FF;

class SCBillingServiceUnitTest {

    public static final long SORTING_CENTER_ID = 100;
    public static final SCServiceType SERVICE_TYPE = ORDER_SHIPPED_TO_SO_FF;
    public static final LocalDate BILLING_DATE = LocalDate.of(2022, 5, 1);
    public static final Long MGT_QTY = 990L;
    public static final Long KGT_QTY = 20L;
    public static final BigDecimal MGT_TARIFF = BigDecimal.valueOf(10L);

    private BillingUnit billingUnit;
    private SCOperationCounter operationCounter;
    private SCTariffHolder tariffHolder;

    @BeforeEach
    public void before() {
        billingUnit = new BillingUnit(
                "PartnerName",
                "SCName",
                SORTING_CENTER_ID,
                BILLING_DATE,
                SERVICE_TYPE
        );
        operationCounter = new SCOperationCounter();
        operationCounter.setMgt(MGT_QTY);
        operationCounter.setKgt(KGT_QTY);

        tariffHolder = new SCTariffHolder();
        tariffHolder.addMinimalCount(SORTING_CENTER_ID, SERVICE_TYPE, 1000);
        tariffHolder.addKGTTariff(SORTING_CENTER_ID, SERVICE_TYPE, BigDecimal.valueOf(100L));
        tariffHolder.add(SCTariffDTO.builder()
                .scId(SORTING_CENTER_ID)
                .serviceType(SERVICE_TYPE)
                .fromCount(1000L)
                .toCount(2001L)
                .value(MGT_TARIFF)
                .startTime(BILLING_DATE.minusDays(1))
                .build()
        );
    }

    @Test
    void billingWhenSumAboveMinimal() {
        SCOperationCost costs = SCBillingService.calculateCosts(
                billingUnit,
                operationCounter,
                tariffHolder,
                BILLING_DATE.atTime(OffsetTime.parse("00:00:00+00:00"))
        );
        Assertions.assertNotNull(costs);
        Assertions.assertEquals(MGT_QTY, costs.getMgtQty());
        assertThat(
                MGT_TARIFF.multiply(BigDecimal.valueOf(MGT_QTY)),
                Matchers.comparesEqualTo(costs.getMgtCost())
        );
        Assertions.assertEquals(KGT_QTY, costs.getKgtQty());

    }
}

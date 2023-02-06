package ru.yandex.market.billing.fulfillment.billing.surplus;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.util.EnvironmentAwareDateValidationService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.fulfillment.promo.SupplierPromoTariffService;
import ru.yandex.market.core.billing.fulfillment.supplies.dao.FulfillmentSupplyDbDao;
import ru.yandex.market.core.billing.fulfillment.surplus.SurplusSupplyBillingDao;
import ru.yandex.market.core.fulfillment.surplus.tariff.SurplusSupplyTariffService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * Тесты для {@link SurplusSupplyBillingService}.
 *
 * @author vbudnev
 */
class SurplusSupplyBillingServiceTest extends FunctionalTest {

    private SurplusSupplyBillingService surplusSupplyBillingService;
    @Autowired
    private SurplusSupplyTariffService surplusSupplyTariffService;
    @Autowired
    private SurplusSupplyBillingDao surplusSupplyBillingDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private FulfillmentSupplyDbDao fulfillmentSupplyDbDao;
    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private SupplierPromoTariffService supplierPromoTariffService;
    private final TransactionTemplate billingPgTransactionTemplate = Mockito.mock(TransactionTemplate.class);
    private final SurplusSupplyBillingDao pgSurplusSupplyBillingDao = Mockito.mock(SurplusSupplyBillingDao.class);

    @BeforeEach
    public void init() {
        Mockito.when(pgSurplusSupplyBillingDao.getCollectedToTlogAmounts(anyList(), any()))
                .thenReturn(List.of());

        surplusSupplyBillingService = new SurplusSupplyBillingService(
                surplusSupplyTariffService,
                surplusSupplyBillingDao,
                pgSurplusSupplyBillingDao,
                fulfillmentSupplyDbDao,
                transactionTemplate,
                billingPgTransactionTemplate,
                environmentAwareDateValidationService,
                supplierPromoTariffService,
                environmentService
        );
    }

    @DisplayName("Проверка обиливания излишков в поставках")
    @DbUnitDataSet(
            before = "db/SurplusSupplyBillingServiceTest.before.csv",
            after = "db/SurplusSupplyBillingServiceTest.after.csv"
    )
    @Test
    void test_billSurplusSupplyItems() {
        surplusSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("Излишки биллим только для типа поставка")
    @DbUnitDataSet(
            before = "db/SurplusSupplyBillingServiceTest.billable_operations.before.csv",
            after = "db/SurplusSupplyBillingServiceTest.billable_operations.after.csv"
    )
    @Test
    void test_billSurplusSupplyItems_billableOperationTypes() {
        surplusSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("Поставки со счетчиком излишков 0, не попадают в список обиленных")
    @DbUnitDataSet(
            before = "db/SurplusSupplyBillingServiceTest.explicit_surplus.before.csv",
            after = "db/SurplusSupplyBillingServiceTest.explicit_surplus.after.csv"
    )
    @Test
    void test_billSurplusSupplyItemsWithExplicitCount() {
        surplusSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("До момента активации услуги \"биллим\" бесплатно")
    @DbUnitDataSet(
            before = "db/SurplusSupplyBillingServiceTest.before_billing_start.before.csv",
            after = "db/SurplusSupplyBillingServiceTest.before_billing_start.after.csv"
    )
    @Test
    void test_billSurplusSupplyItems_free() {
        surplusSupplyBillingService.process(LocalDate.of(2019, 8, 31));
    }

    @DisplayName("Используется проверка EnvironmentAwareDateValidationService")
    @DbUnitDataSet(
            before = "db/SurplusSupplyBillingServiceTest.date_check.before.csv"
    )
    @Test
    void test_billSurplusSupplyItems_when_invalidBillingDate() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> surplusSupplyBillingService.process((LocalDate.of(2019, 1, 1)))
        );
        assertThat(ex.getMessage(), is("Required date 2019-01-01 cannot be used."));
    }

    @DisplayName("Поставщики с промо тарифами")
    @DbUnitDataSet(
            before = "db/SurplusSupplyBillingServiceTest.promo.before.csv",
            after = "db/SurplusSupplyBillingServiceTest.promo.after.csv"
    )
    @Test
    public void test_billSurplusSupplyItemsWithPromoTariff() {
        surplusSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }
}

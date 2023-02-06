package ru.yandex.market.billing.fulfillment.surplus;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.promo.SupplierPromoTariffDao;
import ru.yandex.market.billing.imports.supply.FulfillmentSupplyDbDao;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurplusSupplyBillingServiceTest extends FunctionalTest {

    private SurplusSupplyBillingService surplusSupplyBillingService;
    @Autowired
    private SupplierPromoTariffDao supplierPromoTariffDao;
    @Autowired
    private SurplusSupplyBillingDao surplusSupplyBillingDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private FulfillmentSupplyDbDao fulfillmentSupplyDbDao;
    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @BeforeEach
    public void init() {
        surplusSupplyBillingService = new SurplusSupplyBillingService(
                supplierPromoTariffDao,
                surplusSupplyBillingDao,
                fulfillmentSupplyDbDao,
                transactionTemplate,
                environmentAwareDateValidationService
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

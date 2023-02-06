package ru.yandex.market.billing.agency_commission;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_commission.tariff.AgencyCommissionTariffService;
import ru.yandex.market.billing.payout.control.dao.AccrualDao;
import ru.yandex.market.billing.payout.control.dao.PayoutFrequencyDao;
import ru.yandex.market.billing.util.EnvironmentAwareDateValidationService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.db.AgencyCommissionBillingDao;
import ru.yandex.market.core.billing.OrderTrantimeDao;
import ru.yandex.market.core.billing.dao.AgencyCommissionDao;
import ru.yandex.market.core.billing.fulfillment.promo.SupplierPromoTariffService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.mbi.environment.EnvironmentService;

/**
 * Tests for {@link AgencyCommissionBillingService}
 */
class AgencyCommissionBillingServiceTest extends FunctionalTest {
    @Autowired
    private OrderTrantimeDao orderTrantimeDao;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private AgencyCommissionDao agencyCommissionDao;
    @Autowired
    private AgencyCommissionBillingDao agencyCommissionBillingDao;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private SupplierPromoTariffService supplierPromoTariffService;
    @Autowired
    private AgencyCommissionBillingDao pgAgencyCommissionBillingDao;
    @Autowired
    private PayoutFrequencyDao pgPayoutFrequencyDao;
    @Autowired
    private AgencyCommissionTariffService agencyCommissionTariffService;

    @Autowired
    private TransactionTemplate billingPgTransactionTemplate;


    private AgencyCommissionBillingService agencyCommissionBillingService;

    @BeforeEach
    void init() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        agencyCommissionBillingService = new AgencyCommissionBillingService(
                orderService,
                orderTrantimeDao,
                agencyCommissionDao,
                agencyCommissionBillingDao,
                pgAgencyCommissionBillingDao,
                environmentAwareDateValidationService,
                transactionTemplate,
                billingPgTransactionTemplate,
                environmentService,
                supplierPromoTariffService,
                agencyCommissionTariffService,
                pgPayoutFrequencyDao,
                accrualDao);
    }


    @Test
    @DisplayName("Не падаем, когда нечего биллить.")
    @DbUnitDataSet(before = "AgencyCommissionTest.before.csv")
    void test_processDate_shouldNotFail_whenEmptyData() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 4, 1));
    }

    @Test
    @DisplayName("Биллим нужные данные.")
    @DbUnitDataSet(
            before = {"AgencyCommissionTest.before.csv", "AgencyCommissionBillingServiceProcessTest.before.csv"},
            after = "AgencyCommissionBillingServiceProcessTest.after.csv"
    )
    void test_processDate_shouldBillDate_whenTrantimesGiven() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 4, 30));
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
    }

    @Test
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_multipleDeliveriesForOrder.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_multipleDeliveriesForOrder.after.csv"
    )
    void test_multipleDeliveriesForOrder() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
    }

    @Test
    @DisplayName("Биллим только ДБС доставку")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_onlyDBSDelivery.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_onlyDBSDelivery.after.csv"
    )
    void test_billOnlyDBSDelivery() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
        agencyCommissionBillingService.process(LocalDate.of(2021, 7, 1));
    }

    @Test
    @DisplayName("Биллим только предоплату по ДБС за доставку")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_onlyDBSPrepaidDelivery.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_onlyDBSPrepaidDelivery.after.csv"
    )
    void test_billOnlyPrepaidDbsDelivery() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
        agencyCommissionBillingService.process(LocalDate.of(2021, 7, 1));
    }

    @Test
    @DisplayName("Биллим только для выбранных партнёров")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billSelectedPartnersOnly.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billSelectedPartnersOnly.after.csv"
    )
    void test_billSelectedPartnersOnly() {
        agencyCommissionBillingService.billForPartners(LocalDate.of(2021, 11, 1), Set.of(2L));
    }


    @Test
    @DisplayName("Биллим только выбранного партнера с неактивным договором")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billHardcodedPartnerWithInactiveContract.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billHardcodedPartnerWithInactiveContract.after.csv"
    )
        //TODO: удалить тест после удаления партнера в рамках тикета MBI-74136
    void test_billHardcodedPartnerWithInactiveContract() {
        agencyCommissionBillingService.billForPartners(LocalDate.of(2021, 11, 1), Set.of(560943L));
    }

    @Test
    @DisplayName("Биллим только партнеров с активными договорами ")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billPartnersWithActiveContract.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billPartnersWithActiveContract.after.csv"
    )
    void test_processDate_shouldBillDate_whenTrantimesGivenForPartnersWithActiveContract() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 4, 30));
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
    }

    @Test
    @DisplayName("Биллим по разным тарифам")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_differentTariffs.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_differentTariffs.after.csv"
    )
    void test_billDifferentTariffs() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 7, 1));
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 1));
    }

    @Test
    @DisplayName("Игнорировать Израиль при обиливании АВ")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_ignoreIsraelInBilling.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_ignoreIsraelInBilling.after.csv"
    )
    void test_ignoreIsraelInBilling() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 7, 1));
    }

    @Test
    @DisplayName("Проверка, что биллим АВ для СБП")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billForSbp.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billForSbp.after.csv"
    )
    void test_billForSbp() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
    }

    @Test
    @DisplayName("Биллим по временной схеме - все за 1 процент")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_allOnePercentTariffs.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_allOnePercentTariffs.after.csv"
    )
    void test_billAllOnePercentTariffs() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 7, 1));
    }

    @Test
    @DisplayName("Проверка, что биллим АВ, даже если есть дублирование bnpl с другим способом оплаты.")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billWhenDuplicateWithBnpl.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billWhenDuplicateWithBnpl.after.csv"
    )
    void test_billWhenDuplicateWithBnpl() {
        agencyCommissionBillingService.process(LocalDate.of(2021, 5, 1));
    }

}

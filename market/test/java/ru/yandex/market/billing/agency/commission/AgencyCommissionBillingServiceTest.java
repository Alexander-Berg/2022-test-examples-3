package ru.yandex.market.billing.agency.commission;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.agency.AgencyCommissionBillingDao;
import ru.yandex.market.billing.agency.AgencyCommissionDao;
import ru.yandex.market.billing.agency.model.AgencyCommissionTariff;
import ru.yandex.market.billing.config.OldFirstPartySuppliersIds;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.fulfillment.promo.SupplierPromoTariffDao;
import ru.yandex.market.billing.payment.dao.AccrualDao;
import ru.yandex.market.billing.payment.dao.PayoutFrequencyDao;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.BI_WEEKLY_AGENCY_COMMISSION_TARIFF;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.DAILY_AGENCY_COMMISSION_TARIFF;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.MONTHLY_AGENCY_COMMISSION_TARIFF;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.PROMO_TARIFF_CONTRACTS;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.WEEKLY_AGENCY_COMMISSION_TARIFF;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.ZERO;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.ZERO_AGENCY_COMMISSION_TARIFF;
import static ru.yandex.market.billing.agency.commission.AgencyCommissionTariffsInfoForTest.ZERO_TARIFF_CONTRACTS;

/**
 * Tests for {@link AgencyCommissionBillingService}
 */
class AgencyCommissionBillingServiceTest extends FunctionalTest {

    private static final List<AgencyCommissionTariff> TARIFF_LIST = Stream.concat(
            Stream.concat(
                PROMO_TARIFF_CONTRACTS.stream()
                .map(contractId -> AgencyCommissionTariff.builder()
                        .setValueType(ValueType.RELATIVE)
                        .setValue(BI_WEEKLY_AGENCY_COMMISSION_TARIFF)
                        .setContractId(contractId)
                        .build()),
                ZERO_TARIFF_CONTRACTS.stream()
                .map(contractId -> AgencyCommissionTariff.builder()
                        .setValueType(ValueType.ABSOLUTE)
                        .setValue(ZERO_AGENCY_COMMISSION_TARIFF)
                        .setContractId(contractId)
                        .build())),
            Stream.of(
                setRelativeTariff(PayoutFrequency.DAILY, DAILY_AGENCY_COMMISSION_TARIFF),
                setRelativeTariff(PayoutFrequency.WEEKLY, WEEKLY_AGENCY_COMMISSION_TARIFF),
                setRelativeTariff(PayoutFrequency.BI_WEEKLY, BI_WEEKLY_AGENCY_COMMISSION_TARIFF),
                setRelativeTariff(PayoutFrequency.MONTHLY, MONTHLY_AGENCY_COMMISSION_TARIFF),
                    ZERO))
            .collect(Collectors.toList());

    @Autowired
    private DSLContext dslContext;
    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;
    @Autowired
    private AgencyCommissionDao agencyCommissionDao;
    @Autowired
    private AgencyCommissionBillingDao agencyCommissionBillingDao;
    @Autowired
    private TransactionTemplate pgTransactionTemplate;
    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;
    @Autowired
    private SupplierPromoTariffDao supplierPromoTariffDao;
    @Autowired
    private PayoutFrequencyDao pgPayoutFrequencyDao;
    @Autowired
    private AgencyCommissionTariffService agencyCommissionTariffService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private OldFirstPartySuppliersIds oldFirstPartySuppliersIds;
    @Mock
    private AgencyCommissionTariffDao agencyCommissionTariffDao;

    private AgencyCommissionBillingService agencyCommissionBillingService;

    @BeforeEach
    void init() {
        AccrualDao accrualDao = new AccrualDao(pgNamedParameterJdbcTemplate, oldFirstPartySuppliersIds, dslContext);
        Mockito.when(agencyCommissionTariffDao.getAllAgencyCommissionTariffs(any())).thenReturn(TARIFF_LIST);
        agencyCommissionBillingService = new AgencyCommissionBillingService(
                agencyCommissionDao,
                agencyCommissionBillingDao,
                agencyCommissionTariffDao,
                environmentAwareDateValidationService,
                pgTransactionTemplate,
                supplierPromoTariffDao,
                agencyCommissionTariffService,
                pgPayoutFrequencyDao,
                accrualDao,
                environmentService);
    }


    @Test
    @DisplayName("Не падаем, когда нечего биллить.")
    @DbUnitDataSet(before = "AgencyCommissionTest.before.csv")
    void test_processDate_shouldNotFail_whenEmptyData() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 1));
    }

    @Test
    @DisplayName("Биллим нужные данные.")
    @DbUnitDataSet(
            before = {"AgencyCommissionTest.before.csv", "AgencyCommissionBillingServiceProcessTest.before.csv"},
            after = "AgencyCommissionBillingServiceProcessTest.after.csv"
    )
    void test_processDate_shouldBillDate_whenTrantimesGiven() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 30));
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    @Test
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_multipleDeliveriesForOrder.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_multipleDeliveriesForOrder.after.csv"
    )
    void test_multipleDeliveriesForOrder() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    @Test
    @DisplayName("Биллим только ДБС доставку")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_onlyDBSDelivery.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_onlyDBSDelivery.after.csv"
    )
    void test_billOnlyDBSDelivery() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
        agencyCommissionBillingService.process(LocalDate.of(2022, 7, 1));
    }

    @Test
    @DisplayName("Биллим только предоплату по ДБС за доставку")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_onlyDBSPrepaidDelivery.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_onlyDBSPrepaidDelivery.after.csv"
    )
    void test_billOnlyPrepaidDbsDelivery() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
        agencyCommissionBillingService.process(LocalDate.of(2022, 7, 1));
    }

    @Test
    @DisplayName("Биллим только для выбранных партнёров")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billSelectedPartnersOnly.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billSelectedPartnersOnly.after.csv"
    )
    void test_billSelectedPartnersOnly() {
        agencyCommissionBillingService.billForPartners(LocalDate.of(2022, 11, 1), Set.of(2L));
    }

    @Test
    @DisplayName("Биллим только партнеров с активными договорами ")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billPartnersWithActiveContract.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billPartnersWithActiveContract.after.csv"
    )
    void test_processDate_shouldBillDate_whenTrantimesGivenForPartnersWithActiveContract() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 30));
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    @Test
    @DisplayName("Проверяем платформу, когда получаем частоту выплат ")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_twoPlatforms.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_twoPlatforms.after.csv"
    )
    void test_processDate_shouldNotFail_whenSeveralPayoutFrequenciesWithDifferentPlatformsGiven() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 30));
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    @Test
    @DisplayName("Игнорировать Израиль при обиливании АВ")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_ignoreIsraelInBilling.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_ignoreIsraelInBilling.after.csv"
    )
    void test_ignoreIsraelInBilling() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 7, 1));
    }

    @Test
    @DisplayName("Проверка, что биллим АВ для СБП")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billForSbp.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billForSbp.after.csv"
    )
    void test_billForSbp() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    @Test
    @DisplayName("Проверка, что биллим АВ, даже если есть дублирование bnpl с другим способом оплаты.")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billWhenDuplicateWithBnpl.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billWhenDuplicateWithBnpl.after.csv"
    )
    void test_billWhenDuplicateWithBnpl() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 3, 1));
    }

    @Test
    @DisplayName("Ходить в тарифницу за тарифами при энв=true")
    @DbUnitDataSet(before = "AgencyCommissionTest.test_use_tariffDao_on_env_true.before.csv")
    void test_use_tariffDao_on_env_false() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 1));
        Mockito.verify(agencyCommissionTariffDao, Mockito.times(1)).getAllAgencyCommissionTariffs(any());
    }

    @Test
    @DisplayName("Не ходить в тарифницу за тарифами при энв=false")
    @DbUnitDataSet(before = "AgencyCommissionTest.test_dont_use_tariffDao_on_env_false.before.csv")
    void test_dont_use_tariffDao_by_default() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 4, 1));
        Mockito.verify(agencyCommissionTariffDao, Mockito.times(0)).getAllAgencyCommissionTariffs(any());
    }

    @Test
    @DisplayName("Проверка, что биллим АВ для B2b")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billForB2b.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billForB2b.after.csv"
    )
    void test_billForB2b() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    @Test
    @DisplayName("Проверка, что биллим АВ для 1p как 3p")
    @DbUnitDataSet(
            before = "AgencyCommissionBillingServiceTest.test_billFor1p3p.before.csv",
            after = "AgencyCommissionBillingServiceTest.test_billFor1p3p.after.csv"
    )
    void test_billFor1p3p() {
        agencyCommissionBillingService.process(LocalDate.of(2022, 5, 1));
    }

    private static AgencyCommissionTariff setRelativeTariff(PayoutFrequency frequency, long tariff) {
        return AgencyCommissionTariff.builder()
                .setFrequency(frequency)
                .setValueType(ValueType.RELATIVE)
                .setValue(tariff)
                .build();
    }
}

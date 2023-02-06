package ru.yandex.market.core.agency.db;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.matchers.AgencyCommissionBilledAmountMatcher;
import ru.yandex.market.core.billing.model.AgencyCommissionBilledAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

/**
 * Test for {@link AgencyCommissionBillingDao}
 */
class AgencyCommissionBillingDaoTest extends FunctionalTest {

    @Autowired
    private AgencyCommissionBillingDao agencyCommissionBillingDao;

    @Test
    @DbUnitDataSet(before = "csv/AgencyCommissionBillingDaoTest_insert.before.csv",
            after = "csv/AgencyCommissionBillingDaoTest_insert.after.csv")
    @DisplayName("Запись обилленых значений.")
    void test_shouldInsertBilledAmount() {
        List<AgencyCommissionBilledAmount> agencyCommissions = getAgencyCommissions();
        agencyCommissionBillingDao.persistAgencyCommissionBillingAmounts(agencyCommissions);
    }

    @Test
    @DbUnitDataSet(before = "csv/AgencyCommissionBillingDaoTest_select.before.csv",
    after = "csv/AgencyCommissionBillingDaoTest_select.after.csv")
    @DisplayName("Получить записи обилливания услуги АВ.")
    void test_shouldSelectAgencyCommissionWhenHaveRecords() {
        List<AgencyCommissionBilledAmount> billedAgencyCommission = agencyCommissionBillingDao.getBilledAgencyCommission(
                LocalDate.of(2021, 5, 1),
                LocalDate.of(2021, 6, 1)
        );

        assertThat(billedAgencyCommission, hasSize(3));
        assertThat(billedAgencyCommission, containsInAnyOrder(
                allOf(
                        AgencyCommissionBilledAmountMatcher.hasSupplierId(1L),
                        AgencyCommissionBilledAmountMatcher.hasOrderId(1L),
                        AgencyCommissionBilledAmountMatcher.hasItemId(1L),
                        AgencyCommissionBilledAmountMatcher.hasPaymentId(1L),
                        AgencyCommissionBilledAmountMatcher.hasTrustPaymentId("abcd"),
                        AgencyCommissionBilledAmountMatcher.hasTrantime(LocalDate.of(2021,5,1).atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant()),
                        AgencyCommissionBilledAmountMatcher.hasPaymentAmount(100000L),
                        AgencyCommissionBilledAmountMatcher.hasTariffValue(100L),
                        AgencyCommissionBilledAmountMatcher.hasAmount(1000L)
                ),
                allOf(
                        AgencyCommissionBilledAmountMatcher.hasSupplierId(1L),
                        AgencyCommissionBilledAmountMatcher.hasOrderId(2L),
                        AgencyCommissionBilledAmountMatcher.hasItemId(2L),
                        AgencyCommissionBilledAmountMatcher.hasPaymentId(2L),
                        AgencyCommissionBilledAmountMatcher.hasTrustPaymentId("bcde"),
                        AgencyCommissionBilledAmountMatcher.hasTrantime(LocalDate.of(2021,5,2).atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant()),
                        AgencyCommissionBilledAmountMatcher.hasPaymentAmount(300000L),
                        AgencyCommissionBilledAmountMatcher.hasTariffValue(100L),
                        AgencyCommissionBilledAmountMatcher.hasAmount(3000L)

                ),
                allOf(
                        AgencyCommissionBilledAmountMatcher.hasSupplierId(2L),
                        AgencyCommissionBilledAmountMatcher.hasOrderId(2L),
                        AgencyCommissionBilledAmountMatcher.hasItemId(3L),
                        AgencyCommissionBilledAmountMatcher.hasPaymentId(3L),
                        AgencyCommissionBilledAmountMatcher.hasTrustPaymentId("bcde"),
                        AgencyCommissionBilledAmountMatcher.hasTrantime(LocalDate.of(2021,5,1).atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant()),
                        AgencyCommissionBilledAmountMatcher.hasPaymentAmount(200000L),
                        AgencyCommissionBilledAmountMatcher.hasTariffValue(100L),
                        AgencyCommissionBilledAmountMatcher.hasAmount(2000L)
                )
        ));
    }

    private List<AgencyCommissionBilledAmount> getAgencyCommissions() {
        AgencyCommissionBilledAmount agencyCommission = AgencyCommissionBilledAmount.builder()
                .setPartnerId(1L)
                .setOrderId(1L)
                .setItemId(1L)
                .setTrantime(LocalDate.of(2021, 11, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .plusHours(12L)
                        .toInstant())
                .setPaymentAmount(100000L)
                .setTariffValue(100L)
                .setAmount(1000L)
                .setPaymentId(1L)
                .setTrustPaymentId("abcd")
                .build();

        AgencyCommissionBilledAmount agencyCommission2 = AgencyCommissionBilledAmount.builder()
                .setPartnerId(1L)
                .setOrderId(2L)
                .setItemId(2L)
                .setTrantime(LocalDate.of(2021, 11, 2)
                        .atStartOfDay(ZoneId.systemDefault())
                        .plusHours(12L)
                        .toInstant())
                .setPaymentAmount(300000L)
                .setTariffValue(100L)
                .setAmount(3000L)
                .setPaymentId(2L)
                .setTrustPaymentId("bcde")
                .build();

        AgencyCommissionBilledAmount agencyCommission3 = AgencyCommissionBilledAmount.builder()
                .setPartnerId(2L)
                .setOrderId(2L)
                .setItemId(3L)
                .setTrantime(LocalDate.of(2021, 11, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .plusHours(12L)
                        .toInstant())
                .setPaymentAmount(200000L)
                .setTariffValue(100L)
                .setAmount(2000L)
                .setPaymentId(3L)
                .setTrustPaymentId("bcde")
                .build();

        return List.of(agencyCommission, agencyCommission2, agencyCommission3);
    }
}

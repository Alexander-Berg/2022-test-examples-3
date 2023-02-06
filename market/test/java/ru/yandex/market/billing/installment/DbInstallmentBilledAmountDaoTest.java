package ru.yandex.market.billing.installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.dao.DbInstallmentBilledAmountDao;
import ru.yandex.market.core.billing.model.InstallmentBilledAmount;
import ru.yandex.market.core.billing.model.InstallmentReturnBilledAmount;
import ru.yandex.market.core.billing.model.InstallmentType;
import ru.yandex.market.core.billing.model.report.InstallmentBilledAmountReportItem;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.ValueType;

/**
 * Test for {@link DbInstallmentBilledAmountDao}
 */
class DbInstallmentBilledAmountDaoTest extends FunctionalTest {

    @Autowired
    private DbInstallmentBilledAmountDao dbInstallmentBilledAmountDao;

    @Test
    @DisplayName("Сохранить обилленое значение по рассрочке.")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.before.csv",
            after = "DbInstallmentBilledAmountDaoPersistTest.after.csv")
    void test_persistInstallmentBilledAmount() {
        dbInstallmentBilledAmountDao.persist(getInstallments());
    }

    @Test
    @DisplayName("Получить пустой список обилленых значений")
    void testGetInstallmentBilledAmountReportItemsEmpty() {
        List<InstallmentBilledAmountReportItem> result =
                dbInstallmentBilledAmountDao.getInstallmentBilledAmountReportItems(
                        123L, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 12, 1)
                );
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получить список обилленых значений с фильтрацией по поставщику")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.partner.before.csv")
    void testGetInstallmentBilledAmountReportItemsPartner() {
        List<InstallmentBilledAmountReportItem> result =
                dbInstallmentBilledAmountDao.getInstallmentBilledAmountReportItems(
                        1L, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 12, 1)
                );
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(78559164L, result.get(0).getOrderId());
    }

    @Test
    @DisplayName("Получить список обилленых значений с фильтрацией по времени")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.trantime.before.csv")
    void testGetInstallmentBilledAmountReportItemsTrantime() {
        List<InstallmentBilledAmountReportItem> reportItems =
                dbInstallmentBilledAmountDao.getInstallmentBilledAmountReportItems(
                        1L, LocalDate.of(2021, 11, 27), LocalDate.of(2021, 12, 28)
                );
        List<Long> result = reportItems.stream().map(InstallmentBilledAmountReportItem::getOrderId).collect(Collectors.toList());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(78559310L));
        Assertions.assertTrue(result.contains(78559164L));
    }

    @Test
    @DisplayName("Получить список обилленых значений с фильтрацией по amount")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.amount.before.csv")
    void testGetInstallmentBilledAmountReportItemsAmount() {
        List<InstallmentBilledAmountReportItem> reportItems =
                dbInstallmentBilledAmountDao.getInstallmentBilledAmountReportItems(
                        1L, LocalDate.of(2021, 11, 27), LocalDate.of(2021, 12, 28)
                );
        List<Long> result = reportItems.stream().map(InstallmentBilledAmountReportItem::getOrderId).collect(Collectors.toList());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(78559164L));
    }

    @Test
    @DisplayName("Получить список обилленых значений с корректировками")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.correction.before.csv")
    void testGetInstallmentBilledAmountReportItemsCorrection() {
        List<InstallmentBilledAmountReportItem> reportItems =
                dbInstallmentBilledAmountDao.getInstallmentBilledAmountReportItems(
                        1L, LocalDate.of(2021, 11, 27), LocalDate.of(2021, 12, 28)
                );
        List<String> result = reportItems.stream().map(InstallmentBilledAmountReportItem::getType).collect(Collectors.toList());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains("Начисление"));
        Assertions.assertTrue(result.contains("Корректировка"));
    }

    @Test
    @DisplayName("Получить список обилленых значений с возвратам")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.return.before.csv")
    void testGetInstallmentBilledAmountReportItemsReturn() {
        List<InstallmentBilledAmountReportItem> reportItems =
                dbInstallmentBilledAmountDao.getInstallmentBilledAmountReportItems(
                        1L, LocalDate.of(2021, 11, 27), LocalDate.of(2021, 12, 28)
                );
        List<BillingServiceType> result = reportItems.stream().map(InstallmentBilledAmountReportItem::getServiceType).collect(Collectors.toList());
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(BillingServiceType.INSTALLMENT_CANCELLATION));
        Assertions.assertTrue(result.contains(BillingServiceType.INSTALLMENT));
    }

    @Test
    @DisplayName("Получение комиссии")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.commissions.before.csv")
    void testGetCommissions() {
        Map<Long, BigDecimal> commissions = dbInstallmentBilledAmountDao.getCommissions(
                1L,
                LocalDate.of(2021, 11, 29),
                LocalDate.of(2021, 11, 30),
                Set.of(78559163L, 78559164L)
        );
        Assertions.assertFalse(commissions.isEmpty());
        Assertions.assertEquals(2, commissions.size());
        Assertions.assertEquals(400L, commissions.get(78559163L).longValue());
        Assertions.assertEquals(200L, commissions.get(78559164L).longValue());
    }

    @Test
    @DisplayName("Получение комиссии с корректировками и возвратами")
    @DbUnitDataSet(before = "DbInstallmentBilledAmountDaoPersistTest.commissionsCorrReturn.before.csv")
    void testGetCommissionsCorrReturn() {
        Map<Long, BigDecimal> commissions = dbInstallmentBilledAmountDao.getCommissions(
                1L,
                LocalDate.of(2021, 11, 29),
                LocalDate.of(2021, 11, 30),
                Set.of(78559163L, 78559164L)
        );
        Assertions.assertFalse(commissions.isEmpty());
        Assertions.assertEquals(2, commissions.size());
        Assertions.assertEquals(100L, commissions.get(78559163L).longValue());
        Assertions.assertEquals(100L, commissions.get(78559164L).longValue());
    }

    @Test
    @DisplayName("Сохранение и обновление возвратов рассрочки")
    @DbUnitDataSet(
            before = "DbInstallmentBilledAmountDaoPersistTest.testUpsertInstallmentReturn.before.csv",
            after = "DbInstallmentBilledAmountDaoPersistTest.testUpsertInstallmentReturn.after.csv"
    )
    void testUpsertInstallmentReturn() {
        dbInstallmentBilledAmountDao.upsertInstallmentReturn(getInstallmentReturns());
    }

    private List<InstallmentBilledAmount> getInstallments() {
        InstallmentBilledAmount.Builder builder = InstallmentBilledAmount.builder();
        builder.setOrderId(1L)
                .setOrderItemId(1L)
                .setPartnerId(1L)
                .setTariffValueType(ValueType.RELATIVE)
                .setCount(1)
                .setTrantime(LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.MIN)
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
                .setInstallmentType(InstallmentType.INSTALLMENT_6)
                .setServiceType(BillingServiceType.INSTALLMENT)
                .setTariffValue(600)
                .setAmount(600L)
                .setRawAmount(600L);
        return List.of(
                builder.build(),
                builder.setOrderId(11L)
                        .setOrderItemId(11L)
                        .setServiceType(BillingServiceType.INSTALLMENT_FINE)
                        .build(),
                builder.setOrderId(2L)
                        .setOrderItemId(2L)
                        .setServiceType(BillingServiceType.INSTALLMENT)
                        .setInstallmentType(InstallmentType.INSTALLMENT_12)
                        .setTariffValue(1200)
                        .setRawAmount(1200L)
                        .setAmount(1200L)
                        .build(),
                builder.setOrderId(22L)
                        .setOrderItemId(22L)
                        .setRawAmount(-1200L)
                        .setAmount(-1200L)
                        .setTariffValue(1200)
                        .setServiceType(BillingServiceType.INSTALLMENT_CANCELLATION)
                        .setInstallmentType(InstallmentType.INSTALLMENT_12)
                        .build(),
                builder.setOrderId(3L)
                        .setOrderItemId(3L)
                        .setServiceType(BillingServiceType.INSTALLMENT)
                        .setInstallmentType(InstallmentType.INSTALLMENT_24)
                        .setTariffValue(1800)
                        .setRawAmount(1800L)
                        .setAmount(1800L)
                        .build()
        );
    }

    private List<InstallmentReturnBilledAmount> getInstallmentReturns() {
        InstallmentReturnBilledAmount.Builder builder = InstallmentReturnBilledAmount.builder()
                .setOrderId(78559164L)
                .setOrderItemId(136510647L)
                .setPartnerId(1L)
                .setServiceType(BillingServiceType.INSTALLMENT_RETURN_CANCELLATION)
                .setInstallmentType(InstallmentType.INSTALLMENT_12)
                .setTrantime(LocalDateTime.of(LocalDate.of(2021, 11, 29), LocalTime.of(16, 0))
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
                .setCount(1)
                .setTariffValue(1200)
                .setTariffValueType(ValueType.RELATIVE)
                .setExportedToTlog(true);
        return List.of(
                builder
                        .setReturnItemId(2)
                        .setRawAmount(-304880L)
                        .setAmount(-304880L)
                        .build(),
                builder
                        .setReturnItemId(3)
                        .setRawAmount(-303880L)
                        .setAmount(-303880L)
                        .build()
        );
    }
}

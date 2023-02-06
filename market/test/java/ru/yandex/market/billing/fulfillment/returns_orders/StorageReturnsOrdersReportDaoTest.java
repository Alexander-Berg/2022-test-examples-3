package ru.yandex.market.billing.fulfillment.returns_orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.orderservicereturn.model.OrderServiceReturnType;
import ru.yandex.market.billing.tlogreport.marketplace.mappers.resupply.ResupplyKeyDto;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

class StorageReturnsOrdersReportDaoTest extends FunctionalTest {
    private static final String BILLING = "Начисление";

    @Autowired
    StorageReturnsOrdersReportDao storageReturnsOrdersReportDao;

    static Set<StorageReturnsItem> getTestData() {
        return Set.of(
                StorageReturnsItem.builder()
                        .setReturnId(1244252L)
                        .setOrderId(67280235L)
                        .setOrderItemId(121350460L)
                        .setCount(1L)
                        .setBillingDate(LocalDate.of(2021, 11, 1))
                        .setCalculatedAmount(BigDecimal.valueOf(0L))
                        .setTariff(BigDecimal.valueOf(0L))
                        .setRecordType(BILLING)
                        .build(),
                StorageReturnsItem.builder()
                        .setReturnId(1390906L)
                        .setOrderId(76264152L)
                        .setOrderItemId(133496391L)
                        .setCount(1L)
                        .setBillingDate(LocalDate.of(2021, 12, 11))
                        .setCalculatedAmount(BigDecimal.valueOf(0L))
                        .setTariff(BigDecimal.valueOf(0L))
                        .setRecordType(BILLING)
                        .build(),
                StorageReturnsItem.builder()
                        .setReturnId(1244632L)
                        .setOrderId(67496761L)
                        .setOrderItemId(121609342L)
                        .setCount(1L)
                        .setBillingDate(LocalDate.of(2021, 11, 1))
                        .setCalculatedAmount(BigDecimal.valueOf(0L))
                        .setTariff(BigDecimal.valueOf(0L))
                        .setRecordType(BILLING)
                        .build()
        );
    }

    static Set<StorageReturnsItem> getTestReturnOrderData() {
        return Set.of(
                StorageReturnsItem.builder()
                        .setOrderId(561783L)
                        .setBillingDate(LocalDate.of(2022, 3, 9))
                        .setCalculatedAmount(BigDecimal.valueOf(1500L))
                        .setTariff(BigDecimal.valueOf(1500L))
                        .setRecordType(BILLING)
                        .setReturnType(OrderServiceReturnType.RETURN)
                        .setLogisticReturnId(145437L)
                        .setServiceType("return_order_billing")
                        .build(),
                StorageReturnsItem.builder()
                        .setOrderId(561784L)
                        .setBillingDate(LocalDate.of(2022, 3, 5))
                        .setCalculatedAmount(BigDecimal.valueOf(0L))
                        .setTariff(BigDecimal.valueOf(0L))
                        .setRecordType(BILLING)
                        .setReturnType(OrderServiceReturnType.UNREDEEMED)
                        .setLogisticReturnId(145439L)
                        .setServiceType("unredeemed_order_billing")
                        .build()
        );
    }


    @Test
    @DisplayName("Чтение данных о возвратах")
    @DbUnitDataSet(before = "StorageReturnsOrdersReportDaoTest.getReturnsBilling.before.csv")
    void getReturnsBilling() {
        List<StorageReturnsItem> returnsBilling = storageReturnsOrdersReportDao.getReturnsBilling(
                Set.of(
                        ResupplyKeyDto.builder()
                                .withBillingDate(LocalDate.of(2021, 11, 1))
                                .withReturnId(1244252L)
                                .withOrderItemId(121350460L)
                                .build(),
                        ResupplyKeyDto.builder()
                                .withBillingDate(LocalDate.of(2021, 12, 11))
                                .withReturnId(1390906L)
                                .withOrderItemId(133496391L)
                                .build(),
                        ResupplyKeyDto.builder()
                                .withBillingDate(LocalDate.of(2021, 11, 1))
                                .withReturnId(1244632L)
                                .withOrderItemId(121609342L)
                                .build()
                )
        );

        assertThat(returnsBilling).hasSize(3);
        assertThat(returnsBilling).containsExactlyInAnyOrderElementsOf(getTestData());
    }


    @Test
    @DisplayName("Чтение данных о возвратах/невыкупах из новой таблицы")
    @DbUnitDataSet(before = "StorageReturnsOrdersReportDaoTest.getReturnOrderBilling.before.csv")
    void getReturnOrderBilling() {
        List<StorageReturnsItem> returnOrderBilling = storageReturnsOrdersReportDao.getReturnOrderBilling(
                Set.of(
                        ResupplyKeyDto.builder()
                                .withBillingDate(LocalDate.of(2022, 3, 9))
                                .withLogisticReturnId(145437L)
                                .withServiceType("return_order_billing")
                                .build(),
                        ResupplyKeyDto.builder()
                                .withBillingDate(LocalDate.of(2022, 3, 9))
                                .withLogisticReturnId(145438L)
                                .withServiceType("return_order_billing")
                                .build(),
                        ResupplyKeyDto.builder()
                                .withBillingDate(LocalDate.of(2022, 3, 5))
                                .withLogisticReturnId(145439L)
                                .withServiceType("unredeemed_order_billing")
                                .build()
                )
        );

        assertThat(returnOrderBilling).hasSize(2);
        assertThat(returnOrderBilling).containsExactlyInAnyOrderElementsOf(getTestReturnOrderData());
    }
}

package ru.yandex.market.billing.fulfillment.orders;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Tests for {@link GlobalOrderDeliveryBillingService}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GlobalOrderDeliveryBillingServiceTest extends FunctionalTest {

    private static final LocalDate LOCAL_DATE_2021_12_08 = LocalDate.of(2021, 12, 8);

    @Autowired
    private GlobalOrderDeliveryBillingService globalOrderDeliveryBillingService;

    @Test
    @DisplayName("Не падаем когда нечего биллить")
    @DbUnitDataSet(before = "GlobalOrderDeliveryBillingService_empty.before.csv", after = "")
    void shouldNotFailWhenNoBillingDataGiven() {
        globalOrderDeliveryBillingService.process(LOCAL_DATE_2021_12_08);
    }

    @Test
    @DisplayName("Обилливаем нужную дату")
    @DbUnitDataSet(before = "GlobalOrderDeliveryBillingService.before.csv",
            after = "GlobalOrderDeliveryBillingService.after.csv")
    void shouldBillChosenDateWhenBillingDataGiven() {
        globalOrderDeliveryBillingService.process(LOCAL_DATE_2021_12_08);
    }

    @Test
    @DisplayName("Биллим доставку в декабре не смотря на free_delivery_for_shop.")
    @DbUnitDataSet(before = "GlobalOrderDeliveryBillingService.free_delivery.before.csv",
            after = "GlobalOrderDeliveryBillingService.free_delivery.after.csv")
    void shouldNotBillFreeDelivery() {
        globalOrderDeliveryBillingService.process(LOCAL_DATE_2021_12_08);
    }

    @Test
    @DisplayName("Биллим бесплатную доставку.")
    @DbUnitDataSet(before = "GlobalOrderDeliveryBillingService.real_free_delivery.before.csv",
            after = "GlobalOrderDeliveryBillingService.real_free_delivery.after.csv")
    void shouldBillFreeDelivery() {
        globalOrderDeliveryBillingService.process(LocalDate.of(2022, 6, 8));
    }

    @Test
    @DisplayName("Перебилливаем существующие записи")
    @DbUnitDataSet(before = "GlobalOrderDeliveryBillingService.rebill_existing.before.csv",
            after = "GlobalOrderDeliveryBillingService.after.csv")
    void shouldRebillExistingRecordsWhenBilledTableNotEmpty() {
        globalOrderDeliveryBillingService.process(LOCAL_DATE_2021_12_08);
    }
}

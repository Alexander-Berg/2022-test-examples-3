package ru.yandex.market.billing.payment.model;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.core.payment.AccrualProductType;
import ru.yandex.market.core.payment.EntityType;
import ru.yandex.market.core.payment.PaymentOrderCurrency;
import ru.yandex.market.core.payment.PayoutStatus;
import ru.yandex.market.core.payment.PaysysTypeCc;

class AccrualWithPayoutTrantimeTest extends FunctionalTest {
    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2021, 11, 11, 11, 30, 0);

    @DisplayName("Нельзя создать пустой AccrualWithPayoutTrantime")
    @Test
    void testFailEmptyAccrualWithPayoutTrantime() {
        AccrualWithPayoutTrantime.AccrualWithPayoutTrantimeBuilder builder = AccrualWithPayoutTrantime.builder();
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @DisplayName("Нельзя создать AccrualWithPayoutTrantime с null полем")
    @Test
    void testFailNullFieldAccrualWithPayoutTrantime() {
        AccrualWithPayoutTrantime.AccrualWithPayoutTrantimeBuilder builder = AccrualWithPayoutTrantime.builder()
                .setId(4L)
                .setEntityId(4L)
                .setEntityType(EntityType.ITEM)
                .setAccrualProductType(AccrualProductType.ACC_SUBSIDY)
                .setPaysysType(PaysysTypeCc.ACC_SBERBANK)
                .setCheckouterId(4L)
                .setTransactionType(null)
                .setOrderId(40L)
                .setPartnerId(400L)
                .setAmount(4000L)
                .setCurrency(PaymentOrderCurrency.ILS)
                .setPaysysPartnerId(40000L)
                .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                .setPayoutStatus(PayoutStatus.CANCELED)
                .setOrderPayoutTrantime(TEST_DATE_TIME.plusHours(1).atZone(ZoneId.systemDefault()).toInstant());

        Exception exception = Assertions.assertThrows(NullPointerException.class, builder::build);
        Assertions.assertEquals("transactionType is marked non-null but is null", exception.getMessage());
    }

}

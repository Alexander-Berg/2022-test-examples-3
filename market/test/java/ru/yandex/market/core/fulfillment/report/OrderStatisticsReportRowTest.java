package ru.yandex.market.core.fulfillment.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.core.order.model.MbiOrderStatus;

class OrderStatisticsReportRowTest {

    private OrderStatisticsReportRow setUpReportRowWithResupplyCount(
            int countInDelivery, int deliveredCount, int unredeemedCount, int returnCount) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal spasibo = BigDecimal.TEN;
        BigDecimal price = BigDecimal.TEN;
        BigDecimal subsidy = BigDecimal.TEN;
        BigDecimal cashback = BigDecimal.ONE;
        return OrderStatisticsReportRow.builder()
                .setPartnerId(1L)
                .setOrderId(1L)
                .setCreationDate(now)
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setOfferName("offerName")
                .setShopSku("shopSku")
                .setMbiOrderStatus(MbiOrderStatus.DELIVERED)
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(countInDelivery, price, subsidy, spasibo,
                        cashback))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(deliveredCount, price, subsidy, spasibo,
                        cashback))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(unredeemedCount, price, subsidy, spasibo,
                        cashback))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(returnCount, price, subsidy, spasibo, cashback))
                .setPaymentSubmethod(PaymentSubmethod.BNPL)
                .build();
    }

    @Test
    @DisplayName("Количество возвращённых не может быть больше общего кол-ва товаров")
    void testResupplyCountNotGreatThenCount() {
        var reportRow = setUpReportRowWithResupplyCount(
                5, 5,0, 5);

        Assertions.assertThat(reportRow.getReturnedPrices().getCount()).isEqualTo(
                reportRow.getDeliveredPrices().getCount());
    }

    @Test
    @DisplayName("Сумма без скидок = сумма со всеми скидками - скидки")
    void testCorrectDiscountValue() {
        var reportRow = setUpReportRowWithResupplyCount(5, 1, 4,0);

        OrderStatisticsReportRowPrices deliveryPrices = reportRow.getDeliveryPrices();
        Assertions.assertThat(deliveryPrices.getTotalPriceIncludingDiscounts()
                .subtract(deliveryPrices.getTotalDiscounts()))
                .isEqualTo(deliveryPrices.getTotalPriceExcludingDiscounts());

        OrderStatisticsReportRowPrices deliveredPrices = reportRow.getDeliveredPrices();
        Assertions.assertThat(deliveredPrices.getTotalPriceIncludingDiscounts()
                        .subtract(deliveredPrices.getTotalDiscounts()))
                .isEqualTo(deliveredPrices.getTotalPriceExcludingDiscounts());

        OrderStatisticsReportRowPrices unredeemedPrices = reportRow.getUnredeemedPrices();
        Assertions.assertThat(unredeemedPrices.getTotalPriceIncludingDiscounts()
                        .subtract(unredeemedPrices.getTotalDiscounts()))
                .isEqualTo(unredeemedPrices.getTotalPriceExcludingDiscounts());

        OrderStatisticsReportRowPrices returnedPrices = reportRow.getReturnedPrices();
        Assertions.assertThat(returnedPrices.getTotalPriceIncludingDiscounts())
                .isEqualTo(new BigDecimal(0));
        Assertions.assertThat(returnedPrices.getTotalPriceExcludingDiscounts())
                .isEqualTo(new BigDecimal(0));
        Assertions.assertThat(returnedPrices.getTotalDiscounts())
                .isEqualTo(new BigDecimal(0));
    }
}

package ru.yandex.market.core.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.model.OrderItemBilledAmount;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.ValueType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

class OrderBillingCorrectionServiceTest extends FunctionalTest {

    private static final LocalDate DATE_FROM = LocalDate.of(2017, 9, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2017, 9, 30);

    @Autowired
    private OrderBillingCorrectionService orderBillingCorrectionService;

    private static List<Object[]> testAddCorrectionExpected() {
        return Arrays.asList(
                new Object[]{3, 3, 1_10, "offer1", 460002, BillingServiceType.FF_PROCESSING_CORRECTION, new BigDecimal
                        ("10"), "заметка 1", "user1"},
                new Object[]{3, 3, 1_10, "offer1", 460002, BillingServiceType.FEE_CORRECTION, new BigDecimal
                        ("20"), "заметка 2", "user1"},
                new Object[]{4, 4, 1_10, "offer1", 460003, BillingServiceType.FEE_CORRECTION, new BigDecimal
                        ("11"), "заметка 3", "user1"},
                new Object[]{4, 4, 1_10, "offer1", 460003, BillingServiceType.FEE_CORRECTION, new BigDecimal
                        ("12"), "заметка 4", "user1"},
                new Object[]{4, 4, 1_10, "offer1", 460003, BillingServiceType.FEE_CORRECTION, new BigDecimal("-100.0"),
                        "заметка 5", "user1"}
        );
    }

    private static OrderItemBilledAmount[] testGetCorrectedOrderItemBilledAmountsFilteredExpected() {
        return new OrderItemBilledAmount[]{
                // Услуга без корректировки.
                OrderItemBilledAmount.builder()
                        .setItemId(2)
                        .setOrderId(2)
                        .setFeedId(1)
                        .setOfferId("1")
                        .setSupplierId(3301)
                        .setServiceType(BillingServiceType.FF_PROCESSING)
                        .setTrantime(LocalDateTime.of(2017, 9, 15, 0, 0))
                        .setRawAmount(new BigDecimal(200))
                        .setCount(1)
                        .setTariffValue(200)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote(null)
                        .build(),
                // Услуга с двумя корректировками.
                OrderItemBilledAmount.builder()
                        .setItemId(3)
                        .setOrderId(3)
                        .setFeedId(1)
                        .setOfferId("1")
                        .setSupplierId(3301)
                        .setServiceType(BillingServiceType.FF_PROCESSING)
                        .setTrantime(LocalDateTime.of(2017, 9, 16, 0, 0))
                        .setRawAmount(new BigDecimal(205))
                        .setCount(1)
                        .setTariffValue(200)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote(null)
                        .build(),
                // Корректировка в запрашиваемом периоде, но сам заказ был раньше.
                OrderItemBilledAmount.builder()
                        .setItemId(4)
                        .setOrderId(4)
                        .setFeedId(1)
                        .setOfferId("1")
                        .setSupplierId(3301)
                        .setServiceType(BillingServiceType.FF_PROCESSING_CORRECTION)
                        .setTrantime(LocalDateTime.of(2017, 9, 17, 0, 0))
                        .setRawAmount(new BigDecimal(19))
                        .setCount(1)
                        .setTariffValue(19)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote("заметка 3")
                        .setLogin("user1")
                        .build(),
                // Для заказа есть корректировка в будущем, здесь ее не учитываем.
                OrderItemBilledAmount.builder()
                        .setItemId(5)
                        .setOrderId(5)
                        .setFeedId(1)
                        .setOfferId("1")
                        .setSupplierId(3301)
                        .setServiceType(BillingServiceType.FEE)
                        .setTrantime(LocalDateTime.of(2017, 9, 18, 0, 0))
                        .setRawAmount(new BigDecimal(29))
                        .setCount(1)
                        .setTariffValue(29)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote(null)
                        .build()
        };
    }

    private static OrderItemBilledAmount[] testGetCorrectionsListExpected() {
        return new OrderItemBilledAmount[]{
                OrderItemBilledAmount.builder()
                        .setItemId(3)
                        .setOrderId(3L)
                        .setFeedId(1L)
                        .setOfferId("1")
                        .setSupplierId(3301L)
                        .setServiceType(BillingServiceType.FF_PROCESSING_CORRECTION)
                        .setTrantime(LocalDateTime.of(2017, 9, 18, 0, 0))
                        .setRawAmount(new BigDecimal(50))
                        .setCount(1)
                        .setTariffValue(50)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote("заметка 1")
                        .setLogin("user1")
                        .build(),

                OrderItemBilledAmount.builder()
                        .setItemId(3)
                        .setOrderId(3L)
                        .setFeedId(1L)
                        .setOfferId("1")
                        .setSupplierId(3301L)
                        .setServiceType(BillingServiceType.FF_PROCESSING_CORRECTION)
                        .setTrantime(LocalDateTime.of(2017, 9, 19, 0, 0))
                        .setRawAmount(new BigDecimal(-45))
                        .setCount(1)
                        .setTariffValue(-45)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote("заметка 2")
                        .setLogin("user1")
                        .build(),

                OrderItemBilledAmount.builder()
                        .setItemId(3)
                        .setOrderId(3L)
                        .setFeedId(1L)
                        .setOfferId("1")
                        .setSupplierId(3301L)
                        .setServiceType(BillingServiceType.FF_PROCESSING_CORRECTION)
                        .setTrantime(LocalDateTime.of(2017, 11, 19, 0, 0))
                        .setRawAmount(new BigDecimal(-45))
                        .setCount(1)
                        .setTariffValue(-45)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote("заметка 100")
                        .setLogin("user1")
                        .build()
        };
    }

    private static OrderItemBilledAmount[] testGetCorrectionInFutureExpected() {
        return new OrderItemBilledAmount[]{
                OrderItemBilledAmount.builder()
                        .setItemId(7)
                        .setOrderId(77)
                        .setFeedId(475690)
                        .setOfferId("687722.125361")
                        .setSupplierId(3304)
                        .setServiceType(BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN)
                        .setTrantime(LocalDateTime.of(2020, 8, 27, 0, 0))
                        .setRawAmount(BigDecimal.ZERO)
                        .setCount(1)
                        .setTariffValue(3000)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote(null)
                        .setPaymentLimitation(null)
                        .build(),
                OrderItemBilledAmount.builder()
                        .setItemId(7)
                        .setOrderId(77)
                        .setFeedId(475690)
                        .setOfferId("687722.125361")
                        .setSupplierId(3304)
                        .setServiceType(BillingServiceType.DELIVERY_TO_CUSTOMER)
                        .setTrantime(LocalDateTime.of(2020, 8, 27, 0, 0))
                        .setRawAmount(BigDecimal.ZERO)
                        .setCount(1)
                        .setTariffValue(0)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .setNote(null)
                        .setPaymentLimitation(null)
                        .build()
        };
    }

    @Test
    @DbUnitDataSet(
            before = "OrderBillingCorrectionServiceTest.testAddCorrection.before.csv",
            after = "OrderBillingCorrectionServiceTest.testAddCorrection.after.csv"
    )
    void testAddCorrection() {
        final LocalDateTime trantime = LocalDateTime.of(2017, 8, 25, 10, 11, 12);

        List<OrderItemBilledAmount> list = testAddCorrectionExpected().stream()
                .map(
                        s ->
                                OrderItemBilledAmount.builder()
                                        .setItemId((int) s[0])
                                        .setOrderId((int) s[1])
                                        .setFeedId((int) s[2])
                                        .setOfferId((String) s[3])
                                        .setSupplierId((int) s[4])
                                        .setServiceType((BillingServiceType) s[5])
                                        .setTrantime(trantime)
                                        .setRawAmount((BigDecimal) s[6])
                                        .setCount(1)
                                        .setTariffValue(1)
                                        .setTariffValueType(ValueType.ABSOLUTE)
                                        .setTariffBillingUnit(BillingUnit.ITEM)
                                        .setNote((String) s[7])
                                        .setLogin((String) s[8])
                                        .build()
                )
                .collect(Collectors.toList());

        list.forEach(s -> orderBillingCorrectionService.addCorrection(s, 0));
    }

    @Test
    @DbUnitDataSet(before = "OrderBillingCorrectionServiceTest.before.csv")
    void testGetCorrectedOrderItemBilledAmountsFiltered() {
        List<OrderItemBilledAmount> amounts = orderBillingCorrectionService.getCorrectedOrderItemBilledAmountsFiltered(
                null,
                null,
                Collections.emptySet(),
                DATE_FROM,
                DATE_TO
        );

        assertThat(amounts, containsInAnyOrder(testGetCorrectedOrderItemBilledAmountsFilteredExpected()));
    }

    @Test
    @DbUnitDataSet(before = "OrderBillingCorrectionServiceTest.testCorrectionInFuture.before.csv")
    void testCorrectionInFuture() {
        List<OrderItemBilledAmount> amounts = orderBillingCorrectionService.getCorrectedOrderItemBilledAmountsFiltered(
                null,
                null,
                Collections.emptySet(),
                LocalDate.of(2020, 8, 2),
                LocalDate.of(2020, 9, 1)
        );

        OrderItemBilledAmount[] expected = testGetCorrectionInFutureExpected();
        assertThat(amounts, containsInAnyOrder(expected));
    }

    @Test
    @DbUnitDataSet(before = "OrderBillingCorrectionServiceTest.before.csv")
    void testGetCorrectionsList() {
        List<OrderItemBilledAmount> list = orderBillingCorrectionService.getCorrectionsList(3L);

        assertThat(list, containsInAnyOrder(testGetCorrectionsListExpected()));
    }

}

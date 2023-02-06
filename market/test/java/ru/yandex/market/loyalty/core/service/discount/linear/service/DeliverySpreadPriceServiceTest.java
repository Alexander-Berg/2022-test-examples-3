package ru.yandex.market.loyalty.core.service.discount.linear.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;
import ru.yandex.market.loyalty.core.service.discount.linear.model.Bucket;
import ru.yandex.market.loyalty.core.service.discount.linear.model.Delivery;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeliverySpreadPriceServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final AtomicLong ID_GENERATOR = new AtomicLong();


    @Autowired
    private DeliveryPriceSpreadService deliveryPriceSpreadService;

    @Test
    public void splitEquallyTest() {
        List<Bucket> buckets = Arrays.asList(
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(99, BigDecimal.valueOf(50), 30)
        );
        deliveryPriceSpreadService.evaluateDiscounts(buckets, true);

        // по 33 каждому заказу
        assertEquals(66L, buckets.get(0).getDeliveries().get(0).getDiscount());
        assertEquals(66L, buckets.get(1).getDeliveries().get(0).getDiscount());
        assertEquals(66L, buckets.get(2).getDeliveries().get(0).getDiscount());
    }

    @Test
    public void raminOneTest() {
        List<Bucket> buckets = Arrays.asList(
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(298, BigDecimal.valueOf(50), 30) // 3 * 99 + 1
        );
        deliveryPriceSpreadService.evaluateDiscounts(buckets, true);

        // при размазывании остается остаток от деления = 1. Он попадает 1му заказу (в порядке очереди)
        assertEquals(39L, buckets.get(0).getDeliveries().get(0).getDiscount());
        assertEquals(40L, buckets.get(1).getDeliveries().get(0).getDiscount());
        assertEquals(119L, buckets.get(2).getDeliveries().get(0).getDiscount());
    }

    @Test
    public void overLimitTest() {
        List<Bucket> buckets = Arrays.asList(
                createBucket(4, BigDecimal.valueOf(50), 30),
                createBucket(4, BigDecimal.valueOf(50), 30),
                createBucket(4, BigDecimal.valueOf(50), 30),
                createBucket(4, BigDecimal.valueOf(50), 30),
                createBucket(4, BigDecimal.valueOf(50), 30)
        );
        deliveryPriceSpreadService.evaluateDiscounts(buckets, true);

        // всем по 1 рублю (поскольку 4 рубля не разделить на 5х)
        assertEquals(3, buckets.get(0).getDeliveries().get(0).getDiscount());
        assertEquals(3, buckets.get(1).getDeliveries().get(0).getDiscount());
        assertEquals(3, buckets.get(2).getDeliveries().get(0).getDiscount());
        assertEquals(3, buckets.get(3).getDeliveries().get(0).getDiscount());
        assertEquals(3, buckets.get(4).getDeliveries().get(0).getDiscount());
    }

    @Test
    public void oneOrderTest() {
        List<Bucket> buckets = Arrays.asList(
                createBucket(97, BigDecimal.valueOf(50), 30)
        );
        deliveryPriceSpreadService.evaluateDiscounts(buckets, true);

        // скидки нет
        assertEquals(0, buckets.get(0).getDeliveries().get(0).getDiscount());
    }

    @Test
    public void raminTwoTest() {
        List<Bucket> buckets = Arrays.asList(
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(299, BigDecimal.valueOf(50), 30) // 3 * 99 + 2
        );
        deliveryPriceSpreadService.evaluateDiscounts(buckets, true);

        // при размазывании остается остаток от деления = 2. Он попадает 1му и 2му заказу (в порядке очереди)
        assertEquals(39L, buckets.get(0).getDeliveries().get(0).getDiscount());
        assertEquals(39L, buckets.get(1).getDeliveries().get(0).getDiscount());
        // здесь увеличение на 1 поскольку максимальная сумма увеличилась на 1 (а остаток на предыдущие 2 заказа)
        assertEquals(120L, buckets.get(2).getDeliveries().get(0).getDiscount());
    }


    @Test
    public void emptyListTest() {
        List<Bucket> buckets = Collections.emptyList();

        assertDoesNotThrow(() -> deliveryPriceSpreadService.evaluateDiscounts(buckets, true));
    }

    @Test
    public void volumeOverLimitTest() {
        List<Bucket> buckets = Arrays.asList(
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(99, BigDecimal.valueOf(50), 30),
                createBucket(299, BigDecimal.valueOf(50), 50 * 10_000), // 0 ном кг
                createBucket(299, BigDecimal.valueOf(50), 1 * 1_000_000) // 200 ном кг
        );
        deliveryPriceSpreadService.evaluateDiscounts(buckets, true);

        // при размазывании остается остаток от деления = 2. Он попадает 1му и 2му
        assertEquals(39L, buckets.get(0).getDeliveries().get(0).getDiscount());
        assertEquals(39L, buckets.get(1).getDeliveries().get(0).getDiscount());
        assertEquals(120L, buckets.get(2).getDeliveries().get(0).getDiscount());
        // превысили 100 кг (200 кг)
        assertEquals(0L, buckets.get(3).getDeliveries().get(0).getDiscount());
    }

    private Bucket createBucket(long cost, BigDecimal weight, long volume) {
        String cartId = Long.toString(ID_GENERATOR.incrementAndGet());
        return new Bucket(cartId,
                weight,
                volume,
                Collections.singletonList(new Delivery(
                        Collections.singletonList(new DiscountCalculationRequest.DeliveryOption(
                                cartId,
                                PaymentType.CASH_ON_DELIVERY,
                                Long.toString(ID_GENERATOR.incrementAndGet()),
                                DeliveryType.COURIER,
                                BigDecimal.valueOf(cost),
                                null,
                                false,
                                null,
                                Collections.emptySet()
                        )),
                        cost
                )), null);
    }
}

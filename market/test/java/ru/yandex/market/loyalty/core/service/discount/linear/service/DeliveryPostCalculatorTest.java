package ru.yandex.market.loyalty.core.service.discount.linear.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.service.discount.DiscountFactory;
import ru.yandex.market.loyalty.core.service.discount.PromoCalculationList;
import ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator;
import ru.yandex.market.loyalty.core.service.discount.linear.model.Bucket;
import ru.yandex.market.loyalty.core.service.discount.linear.model.Delivery;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.service.discount.linear.service.DeliveryPostCalculator.LP_EPS;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@Log4j2
public class DeliveryPostCalculatorTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    @Autowired
    private DeliveryPostCalculator deliveryPostCalculator;
    @Autowired
    private DeliveryDiscountCalculator deliveryDiscountCalculator;

    @Test
    public void shouldDiscountFourDeliveriesWithTrace() {
        List<Bucket> buckets = ImmutableList.of(
                createBucket(100, 300),
                createBucket(200, 400)
        );

        deliveryPostCalculator.evaluateDiscounts(buckets);

        log.info("{}", buckets);

        checkBounds(buckets);

        assertThat(
                BigDecimal.valueOf(buckets.get(0).getDeliveries().get(0).getDiscount())
                        .setScale(DeliveryPostCalculator.SCALE, RoundingMode.HALF_EVEN),
                comparesEqualTo(BigDecimal.valueOf(12))
        );
        assertThat(
                BigDecimal.valueOf(buckets.get(0).getDeliveries().get(1).getDiscount())
                        .setScale(DeliveryPostCalculator.SCALE, RoundingMode.HALF_EVEN),
                comparesEqualTo(BigDecimal.valueOf(162))
        );
        assertThat(
                BigDecimal.valueOf(buckets.get(1).getDeliveries().get(0).getDiscount())
                        .setScale(DeliveryPostCalculator.SCALE, RoundingMode.HALF_EVEN),
                comparesEqualTo(BigDecimal.valueOf(62))
        );
        assertThat(
                BigDecimal.valueOf(buckets.get(1).getDeliveries().get(1).getDiscount())
                        .setScale(DeliveryPostCalculator.SCALE, RoundingMode.HALF_EVEN),
                comparesEqualTo(BigDecimal.valueOf(113))
        );
    }

    @Test
    public void shouldDiscountExponentBuckets() {
        List<Bucket> buckets = ImmutableList.of(
                createBucket(1),
                createBucket(2),
                createBucket(4),
                createBucket(8),
                createBucket(16),
                createBucket(32),
                createBucket(64),
                createBucket(128),
                createBucket(256),
                createBucket(512)
        );

        deliveryPostCalculator.evaluateDiscounts(buckets);

        log.info("{}", buckets);

        checkBounds(buckets);
    }

    @Test
    public void shouldDiscountSmallBuckets() {
        List<Bucket> buckets = ImmutableList.of(
                createBucket(1),
                createBucket(1),
                createBucket(1),
                createBucket(1),
                createBucket(1),
                createBucket(32),
                createBucket(64),
                createBucket(128),
                createBucket(256),
                createBucket(512)
        );

        deliveryPostCalculator.evaluateDiscounts(buckets);

        log.info("{}", buckets);

        checkBounds(buckets);
    }

    @Test
    public void stochasticTest() {
        for (int i = 0; i < 50; ++i) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<Bucket> buckets = Stream.generate(() ->
                    createBucket(random, random.nextInt(1, 6)))
                    .limit(random.nextInt(2, 6))
                    .collect(ImmutableList.toImmutableList());

            deliveryPostCalculator.evaluateDiscounts(buckets);

            checkBounds(buckets);
        }
    }

    @Test
    public void durationDeliveryCalculationTest() {

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Bucket> buckets = Stream.generate(() ->
                createBucketWithMultipleDeliveryOptions(random, 70))
                .limit(6)
                .collect(ImmutableList.toImmutableList());

        deliveryPostCalculator.evaluateDiscounts(buckets);

        checkBounds(buckets);

        LocalDateTime start = LocalDateTime.now();
        PromoCalculationList result = deliveryDiscountCalculator.getDeliveryDiscountList(buckets, false);
        LocalDateTime end = LocalDateTime.now();

        long duration = Duration.between(ZonedDateTime.of(start, ZoneId.of("Europe/Moscow")), ZonedDateTime.of(end,
                ZoneId.of("Europe/Moscow"))).toMillis();
        assertThat(duration, lessThanOrEqualTo(500L));
        assertThat(result.getPromoCalculations().size(), greaterThan(0));
    }

    @Test
    public void eightByEight() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Bucket> buckets = Stream.generate(() ->
                createBucket(random, 8))
                .limit(8)
                .collect(ImmutableList.toImmutableList());

        deliveryPostCalculator.evaluateDiscounts(buckets);

        checkBounds(buckets);
    }


    @Test
    public void hundredByTwenty() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Bucket> buckets = Stream.generate(() ->
                createBucket(random, 20))
                .limit(100)
                .collect(ImmutableList.toImmutableList());

        deliveryPostCalculator.evaluateDiscounts(buckets);

        log.info("{}", buckets);

        assertFalse(
                "Buckets' deliveries contains negative discount",
                buckets.stream()
                        .map(Bucket::getDeliveries)
                        .flatMap(List::stream)
                        .mapToDouble(Delivery::getDiscount)
                        .anyMatch(discount -> discount < 0.0)
        );

        checkBounds(buckets);
    }

    private static void checkBounds(List<Bucket> buckets) {
        buckets.stream()
                .map(Bucket::getDeliveries)
                .flatMap(List::stream)
                .forEach(delivery ->
                        assertThat(delivery.getDiscount(), lessThanOrEqualTo(delivery.getCost() + LP_EPS))
                );

        assertTrue(
                "Buckets' deliveries doesn't contains discount > 0",
                buckets.stream()
                        .map(Bucket::getDeliveries)
                        .flatMap(List::stream)
                        .map(Delivery::getDiscount)
                        .anyMatch(discount -> discount > 0)
        );
    }

    private static Bucket createBucket(double... costs) {
        return DeliveryPostCalculator.createBucket(ID_GENERATOR, costs);
    }

    private static Bucket createBucketWithMultipleDeliveryOptions(long deliveryOptions, double... costs) {
        return DeliveryPostCalculator.createBucket(ID_GENERATOR, deliveryOptions, costs);
    }

    private static Bucket createBucket(ThreadLocalRandom random, int deliveryCount) {
        return DeliveryPostCalculatorTest.createBucket(
                LongStream.generate(() -> random.nextLong(1, 1001))
                        .limit(deliveryCount)
                        .mapToDouble(t -> t)
                        .toArray());
    }

    private static Bucket createBucketWithMultipleDeliveryOptions(ThreadLocalRandom random, int deliveryCount) {
        return DeliveryPostCalculatorTest.createBucketWithMultipleDeliveryOptions(
                100,
                LongStream.generate(() -> random.nextLong(1, 1001))
                        .limit(deliveryCount)
                        .mapToDouble(t -> t)
                        .toArray());
    }
}

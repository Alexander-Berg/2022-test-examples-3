package ru.yandex.market.global.checkout.order;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderCommandService;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.util.OffsetDateTimeComparator;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderCommandServiceTest extends BaseFunctionalTest {
    private final TestOrderFactory testOrderFactory;
    private final OrderCommandService orderCommandService;
    private final OrderRepository orderRepository;

    @Test
    public void testPatch() {
        Order order = testOrderFactory.createOrder().getOrder();
        Order patch = new Order()
                .setId(order.getId())
                .setVersion(order.getVersion())
                .setCancelReason("some reason")
                .setYaTaxiUserId("123");

        orderCommandService.patch(patch);
        Order patched = orderRepository.fetchOneById(order.getId());

        Assertions.assertThat(patched).usingRecursiveComparison(
                RecursiveComparisonConfiguration.builder()
                        .withComparedFields("cancelReason", "yaTaxiUserId")
                        .withComparatorForType(
                                Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class
                        )
                        .withIgnoreCollectionOrder(true)
                        .build()
        ).isEqualTo(patch);


        Assertions.assertThat(patched).usingRecursiveComparison(
                RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("cancelReason", "yaTaxiUserId", "version")
                        .withComparatorForType(
                                new OffsetDateTimeComparator(Duration.ofSeconds(1)), OffsetDateTime.class
                        )
                        .withIgnoreCollectionOrder(true)
                        .build()
        ).isEqualTo(order);
    }
}

package ru.yandex.market.delivery.mdbapp.integration.service;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Delivery;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequestStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.RequestCondition;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.godd.OrderRequestRepository;

public class OrderDeliveryDateServiceTest extends MockContextualTest {
    @Autowired
    private OrderDeliveryDateService orderDeliveryDateService;

    @Autowired
    private OrderRequestRepository orderRequestRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    final LocalDate today = LocalDate.of(2020, 7, 14);
    final LocalDate yesterday = today.minusDays(1);
    final LocalDate tommorow = today.plusDays(1);

    final Instant now = LocalDateTime.of(today, LocalTime.NOON)
        .toInstant(OffsetDateTime.now().getOffset());

    @Test
    @Sql({
        "/data/clean-get-delivery-date-order-store.sql",
        "/data/create-get-delivery-date-order-store.sql",
        "/data/create-order-delivery-date-requests-for-clean.sql",
    })
    public void cleanOldRequests() {
        orderDeliveryDateService.cleanOldData(now);

        RequestCondition requestCondition = new RequestCondition().setId(1L);
        softly
            .assertThat(orderRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(OrderRequest::getId))
                .collect(Collectors.toList()))
            .isEqualTo(List.of(
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(yesterday, 10, 0))
                    .setStatus(OrderRequestStatus.SENT),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 10, 0))
                    .setStatus(OrderRequestStatus.RECEIVED),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 10, 0))
                    .setStatus(OrderRequestStatus.FAIL),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 10, 0))
                    .setStatus(OrderRequestStatus.SENT),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 10, 0))
                    .setStatus(OrderRequestStatus.NEW)
            ));
    }
    @Test
    @Sql({
        "/data/clean-get-delivery-date-order-store.sql",
        "/data/create-get-delivery-date-order-store.sql",
        "/data/create-order-delivery-date-order-for-clean.sql",
    })
    public void cleanOldOrders() {
        softly.assertThat(orderRepository.findById(122L)).isPresent();
        orderDeliveryDateService.cleanOldData(now);
        softly.assertThat(orderRepository.findById(122L)).isEmpty();
    }


    @Sql({
        "/data/clean-get-delivery-date-order-store.sql",
        "/data/create-get-delivery-date-order-store.sql",
        "/data/create-order-delivery-date-requests.sql",
    })
    @Test
    public void makeDeliveryDateRequests() {
        orderDeliveryDateService.makeDeliveryDateRequests(now);

        RequestCondition requestCondition = new RequestCondition().setId(1L);
        softly
            .assertThat(orderRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(OrderRequest::getId))
                .collect(Collectors.toList()))
            .isEqualTo(List.of(
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 10, 59))
                    .setStatus(OrderRequestStatus.NEW),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 11, 1))
                    .setStatus(OrderRequestStatus.SENT)
                    .setProcessId("processId"),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 12, 0))
                    .setStatus(OrderRequestStatus.SENT),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 12, 0))
                    .setStatus(OrderRequestStatus.RECEIVED),
                new OrderRequest()
                    .setRequestCondition(requestCondition)
                    .setProcessTime(createDateTime(today, 12, 0))
                    .setStatus(OrderRequestStatus.SENT)
                    .setProcessId("processId")
            ));
    }

    @Sql({
        "/data/clean-get-delivery-date-order-store.sql",
        "/data/create-get-delivery-date-order-store.sql",
        "/data/create-order-delivery-date-requests.sql",
    })
    @Test
    public void reCreateRequestsCleanOld() throws NoSuchFieldException, IllegalAccessException {
        Order order = orderRepository.findByIds(Collections.singletonList(123L)).stream().findFirst().orElseThrow();

        RequestCondition condition = order.getDelivery().getRequestConditions().stream().findFirst().get();

        Field f = Delivery.class.getDeclaredField("requestConditions");
        f.setAccessible(true);
        f.set(order.getDelivery(), Collections.emptySet());

        order.setFromDate(tommorow).setToDate(tommorow);
        // check has old requests
        softly.assertThat(orderRequestRepository.findAll().stream()
            .filter(x -> x.getStatus() == OrderRequestStatus.NEW).count())
            .isEqualTo(3);

        orderDeliveryDateService.reCreateRequests(List.of(order), now);

        // old requests removed, no new created because of no rules
        softly.assertThat(orderRequestRepository.findAll().stream()
            .filter(x -> x.getStatus() == OrderRequestStatus.NEW)
            .collect(Collectors.toList()))
            .isEqualTo(List.of(
                new OrderRequest() // only old request
                    .setId(1L)
                    .setOrder(order)
                    .setStatus(OrderRequestStatus.NEW)
                    .setProcessTime(createDateTime(today, 10, 59))
                    .setRequestCondition(condition)
            ));
    }

    @Sql({
        "/data/clean-get-delivery-date-order-store.sql",
        "/data/create-get-delivery-date-order-store.sql",
        "/data/create-order-delivery-date-requests.sql",
    })
    @Test
    public void reCreateRequestsCleanOldAndCreateNew() throws NoSuchFieldException, IllegalAccessException {
        Order order = orderRepository.findByIds(Collections.singletonList(123L)).stream().findFirst().orElseThrow();

        RequestCondition condition = order.getDelivery().getRequestConditions().stream().findFirst().get();

        order.setFromDate(tommorow).setToDate(tommorow);
        // check has old requests
        softly.assertThat(orderRequestRepository.findAll().stream()
            .filter(x -> x.getStatus() == OrderRequestStatus.NEW).count())
            .isEqualTo(3);

        orderDeliveryDateService.reCreateRequests(List.of(order), now);

        // old requests removed, no new created because of no rules
        softly.assertThat(orderRequestRepository.findAll().stream()
            .filter(x -> x.getStatus() == OrderRequestStatus.NEW)
            .collect(Collectors.toList()))
            .isEqualTo(List.of(
                new OrderRequest() // only old request
                    .setId(1L)
                    .setOrder(order)
                    .setStatus(OrderRequestStatus.NEW)
                    .setProcessTime(createDateTime(today, 10, 59))
                    .setRequestCondition(condition),
                new OrderRequest()
                    .setId(6L)
                    .setOrder(order)
                    .setStatus(OrderRequestStatus.NEW)
                    .setProcessTime(createDateTime(order, tommorow, 10, 0))
                    .setRequestCondition(condition),
                new OrderRequest()
                    .setId(7L)
                    .setOrder(order)
                    .setStatus(OrderRequestStatus.NEW)
                    .setProcessTime(createDateTime(order, tommorow, 20, 30))
                    .setRequestCondition(condition)
            ));
    }

    @Nonnull
    private OffsetDateTime createDateTime(LocalDate today, int hour, int minute) {
        return OffsetDateTime.of(today, LocalTime.of(hour, minute), OffsetDateTime.now().getOffset());
    }

    @Nonnull
    private OffsetDateTime createDateTime(Order order, LocalDate date, int hour, int minute) {
        return OffsetDateTime.of(date, LocalTime.of(hour, minute), order.getZoneOffset())
            .atZoneSameInstant(ZoneId.systemDefault())
            .toOffsetDateTime();
    }

}

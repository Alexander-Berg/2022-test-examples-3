package ru.yandex.market.tpl.core.domain.test_sc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.request.CreateReturnRegisterRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.partner.Partner;

/**
 * Простая реализация работы СЦ для тестирования - хранит минимальные данные в одной таблице {@link TestScOrder}.
 *
 * @author kukabara
 */
@Service
@RequiredArgsConstructor
public class ScLogisticService {

    public static final EnumSet<OrderStatusType> NOT_UPDATE_STATUSES_STRICT = EnumSet.of(
            OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF,        // 120
            OrderStatusType.ORDER_SHIPPED_TO_SO_FF,                 // 130
            OrderStatusType.SO_GOT_INFO_ABOUT_PLANNED_RETURN,       // 160
            OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE,         // 170
            OrderStatusType.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,  // 175
            OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM          // 180
    );

    private static final EnumSet<OrderStatusType> NOT_UPDATE_STATUSES_WEAK = EnumSet.of(
            OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF,        // 120
            OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE,         // 170
            OrderStatusType.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,  // 175
            OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM          // 180
    );

    public static final EnumSet<OrderStatusType> ORDER_ARRIVED_STATUSES = EnumSet.of(
            OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE,         // 170
            OrderStatusType.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,  // 175
            OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM          // 180
    );

    private final TestScOrderRepository testScOrderRepository;
    private final TestScOrderHistoryRepository testScOrderHistoryRepository;

    public static boolean notUpdateStatusesContains(OrderStatusType status, boolean isStrict) {
        return (isStrict && ScLogisticService.NOT_UPDATE_STATUSES_STRICT.contains(status)) ||
                (!isStrict && ScLogisticService.NOT_UPDATE_STATUSES_WEAK.contains(status));
    }

    @Transactional
    public TestScOrder createOrder(Order order, Partner partner) {
        Optional<TestScOrder> existingOrder = findByResource(order.getOrderId());
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        TestScOrder scOrder = new TestScOrder();
        scOrder.setYandexId(order.getOrderId().getYandexId());
        scOrder.setPartnerId(partner.getId());
        scOrder.setOrderPartnerId(generateOrderPartnerId());
        scOrder.setExternalOrderId(Optional.ofNullable(order.getExternalId()).map(ResourceId::getYandexId).orElse(null));
        scOrder.setStatus(OrderStatusType.ORDER_CREATED_FF);
        scOrder.setDeliveryDate(order.getDeliveryDate().getOffsetDateTime().toInstant());
        scOrder.setCourier(getCourier(order));
        testScOrderRepository.saveAndFlush(scOrder);
        testScOrderHistoryRepository.saveAndFlush(new TestScOrderHistory(scOrder));
        return scOrder;
    }

    @Transactional
    public TestScOrder updateOrder(Order order, Partner partner) {
        TestScOrder existingOrder = findByResource(order.getOrderId()).orElseThrow();
        if (existingOrder.getPartnerId() != partner.getId()) {
            throw new RuntimeException("Wrong partnerId " + partner.getId());
        }

        checkOrderStatus(existingOrder);

        existingOrder.setDeliveryDate(order.getDeliveryDate().getOffsetDateTime().toInstant());
        existingOrder.setCourier(getCourier(order));
        TestScOrder updatedOrder = testScOrderRepository.save(existingOrder);

        testScOrderHistoryRepository.save(new TestScOrderHistory(updatedOrder));
        return updatedOrder;
    }

    @Transactional
    public void updateStatus(ResourceId order, OrderStatusType status, boolean checkStatus) {
        TestScOrder scOrder = findByResource(order).orElseThrow();
        updateStatus(scOrder, status, checkStatus);
    }

    private void updateStatus(TestScOrder scOrder, OrderStatusType newStatus, boolean checkStatus) {
        if (checkStatus) {
            checkOrderStatus(scOrder);
        }
        if (scOrder.getStatus() == newStatus) {
            return;
        }
        scOrder.setStatus(newStatus);
        testScOrderRepository.save(scOrder);
        testScOrderHistoryRepository.save(new TestScOrderHistory(scOrder));
    }

    @Transactional
    public void createReturnRegister(CreateReturnRegisterRequest request) {
        request.getOrdersId()
                .forEach(o -> updateStatus(o, OrderStatusType.SO_GOT_INFO_ABOUT_PLANNED_RETURN, true));
    }

    public List<OrderStatus> getOrderHistory(ResourceId orderId, Partner partner) {
        return getOrderHistory(orderId)
                .stream()
                .filter(o -> o.getPartnerId() == partner.getId())
                .map(oh -> getOrderStatus(oh.getStatus(), oh.getUpdatedAt()))
                .collect(Collectors.toMap(OrderStatus::getStatusCode, s -> s, (s1, s2) -> s1, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(OrderStatus::getSetDate))
                .collect(Collectors.toList());
    }

    public List<TestScOrderHistory> getOrderHistory(ResourceId orderId) {
        return orderId.getPartnerId() != null ?
                testScOrderHistoryRepository.findByOrderPartnerIdOrderById(orderId.getPartnerId()) :
                testScOrderHistoryRepository.findByYandexIdOrderById(orderId.getYandexId());
    }

    public List<OrderStatusHistory> getOrdersStatus(List<ResourceId> resourceIds, Partner partner) {
        Set<String> orderPartnerIds = resourceIds.stream()
                .map(ResourceId::getPartnerId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<TestScOrder> orders;
        if (!orderPartnerIds.isEmpty()) {
            orders = testScOrderRepository.findByOrderPartnerIdIn(orderPartnerIds);
        } else {
            orders = testScOrderRepository.findAllById(resourceIds.stream()
                    .map(ResourceId::getYandexId).filter(Objects::nonNull).collect(Collectors.toSet()));
        }
        return orders.stream()
                .filter(o -> o.getPartnerId() == partner.getId())
                .map(o -> new OrderStatusHistory(
                        List.of(getOrderStatus(o.getStatus(), o.getUpdatedAt())),
                        new ResourceId(o.getYandexId(), o.getOrderPartnerId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Void cancelOrder(ResourceId order, Partner partner) {
        Optional<TestScOrder> scOrderO = findByResource(order).filter(o -> o.getPartnerId() == partner.getId());
        if (scOrderO.isEmpty()) {
            return null;
        }
        updateStatus(scOrderO.get(), OrderStatusType.ORDER_CANCELLED_FF, true);
        return null;
    }

    private void checkOrderStatus(TestScOrder order) {
        if (NOT_UPDATE_STATUSES_STRICT.contains(order.getStatus())) {
            throw new RuntimeException("Can't update order " + order.getYandexId() + " in status " + order.getStatus());
        }
    }

    private OrderStatus getOrderStatus(OrderStatusType status, Instant instant) {
        return new OrderStatus(status,
                DateTime.fromLocalDateTime(LocalDateTime.ofInstant(instant, DateTimeUtil.DEFAULT_ZONE_ID)),
                status.name());
    }

    private String generateOrderPartnerId() {
        return "TPL_TEST_SC_" + RandomStringUtils.randomAlphanumeric(7);
    }

    public Optional<TestScOrder> findByResource(ResourceId order) {
        return order.getPartnerId() != null ?
                testScOrderRepository.findByOrderPartnerId(order.getPartnerId()) :
                testScOrderRepository.findById(order.getYandexId());
    }

    private String getCourier(Order order) {
        return Optional.ofNullable(order.getDelivery().getCourier())
                .flatMap(c -> c.getPersons().stream().findFirst()
                        .map(p -> StreamEx.of(p.getName(), p.getPatronymic(), p.getSurname())
                                .filter(Objects::nonNull)
                                .joining(" ")))
                .orElse(null);
    }

}

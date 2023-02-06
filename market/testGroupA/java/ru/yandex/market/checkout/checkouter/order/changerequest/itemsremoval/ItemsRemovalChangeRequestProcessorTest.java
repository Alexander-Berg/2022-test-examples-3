package ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemChangeRequest;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.changerequest.ItemsRemovalChangeRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.item.MissingItemsRemovalService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.DELIVERY_SERVICE_DELAYED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.USER_REQUESTED_REMOVE;

public class ItemsRemovalChangeRequestProcessorTest {

    @Mock
    private OrderService orderService;
    @Mock
    private OrderUpdateService orderUpdateService;
    @Mock
    private CheckouterProperties properties;
    @Mock
    private ClientInfo clientInfo;
    @Mock
    private MissingItemsRemovalService missingItemsRemovalService;
    @Captor
    private ArgumentCaptor<Long> idCaptor;
    @Captor
    private ArgumentCaptor<Collection<OrderItemChangeRequest>> requestCaptor;
    @Captor
    private ArgumentCaptor<ClientInfo> clientInfoCaptor;
    @Captor
    private ArgumentCaptor<HistoryEventReason> reasonCaptor;
    private ItemsRemovalChangeRequestProcessor itemsRemovalChangeRequestProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        itemsRemovalChangeRequestProcessor = new ItemsRemovalChangeRequestProcessor(orderService, orderUpdateService,
                missingItemsRemovalService);
        when(properties.getItemsRemoveAllow()).thenReturn(true);
        when(missingItemsRemovalService.isOrderStateAvailableRemove(any(Order.class))).thenReturn(true);
    }

    @Test
    @DisplayName("validate: проставление дефолтного reason. Для ff заказов")
    //как только mdb будет проставлять причины удаления товаров, то тест нужно удалить
    public void mustSetDefaultReasonForFFOrder() {
        Order order = initOrderWithType(OrderType.FF);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(), null);

        itemsRemovalChangeRequestProcessor.validate(order, payload, null, null);

        assertThat(payload.getReason()).isEqualTo(ITEMS_NOT_FOUND);
    }

    @ParameterizedTest
    @DisplayName("validate: ок с валидной причиной")
    @EnumSource(value = OrderType.class, names = {"FF", "FBS", "DBS"})
    public void okWithValidReason(OrderType type) {
        Order order = initOrderWithType(type);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(),
                USER_REQUESTED_REMOVE);

        itemsRemovalChangeRequestProcessor.validate(order, payload, null, null);
    }

    @ParameterizedTest
    @DisplayName("validate: exception при отсутствии причины удаления")
    @EnumSource(value = OrderType.class, names = {"FBS", "DBS"})
    public void exceptionWhenReasonIsNullForNotFFOrders(OrderType type) {
        Order order = initOrderWithType(type);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(), null);

        Exception exception = Assertions.assertThrows(InvalidRequestException.class, () ->
                itemsRemovalChangeRequestProcessor.validate(order, payload, null, null));
        assertThat(exception.getMessage()).isEqualTo("Change request for remove items must contains reason");
    }

    @ParameterizedTest
    @DisplayName("validate: exception при невалидной причине удаления")
    @EnumSource(value = OrderType.class, names = {"FF", "FBS", "DBS"})
    public void exceptionWhenReasonIsInvalid(OrderType type) {
        Order order = initOrderWithType(type);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(),
                DELIVERY_SERVICE_DELAYED);

        Exception exception = Assertions.assertThrows(InvalidRequestException.class, () ->
                itemsRemovalChangeRequestProcessor.validate(order, payload, null, null));
        assertThat(exception.getMessage()).isEqualTo("Only items Update related reasons should be provided for item " +
                "removal");
    }

    @Test
    @DisplayName("validate: exception при невалидных статусах заказов")
    public void exceptionWhenOrderStatusIsInvalidForFF() {
        Order order = initOrderWithType(OrderType.FF);
        order.setStatus(OrderStatus.PROCESSING);
        when(missingItemsRemovalService.isOrderStateAvailableRemove(any(Order.class))).thenReturn(false);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(),
                ITEMS_NOT_FOUND);

        CannotRemoveItemException exception = Assertions.assertThrows(CannotRemoveItemException.class, () ->
                itemsRemovalChangeRequestProcessor.apply(order, payload, null));
        assertThat(exception.getCode()).isEqualTo(OrderStatusNotAllowedException.NOT_ALLOWED_CODE);
    }

    @ParameterizedTest
    @DisplayName("canBeAppliedNow: true для dbs/fbs заказов")
    @EnumSource(value = OrderType.class, names = {"FBS", "DBS"})
    public void canBeAppliedForNotFFOrders(OrderType type) {
        Order order = initOrderWithType(type);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(),
                USER_REQUESTED_REMOVE);

        assertThat(itemsRemovalChangeRequestProcessor.canBeAppliedNow(order, payload, null)).isTrue();
    }

    @Test()
    @DisplayName("canBeAppliedNow: false для ff заказов на PROCESSING")
    public void cannotBeAppliedForFFOrdersOnProcessing() {
        Order order = initOrderWithType(OrderType.FF);
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(),
                USER_REQUESTED_REMOVE);

        assertThat(itemsRemovalChangeRequestProcessor.canBeAppliedNow(order, payload, null)).isFalse();
    }

    @ParameterizedTest()
    @DisplayName("canBeAppliedNow: true для ff заказов на Delivery")
    @EnumSource(value = OrderStatus.class, names = {"DELIVERY", "PICKUP"})
    public void cannotBeAppliedForFFOrdersOnDelivery(OrderStatus status) {
        Order order = OrderBuilder.init(1L, OrderType.FF)
                .setStatus(status)
                .build();
        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(List.of(), List.of(),
                USER_REQUESTED_REMOVE);

        assertThat(itemsRemovalChangeRequestProcessor.canBeAppliedNow(order, payload, null)).isTrue();
    }

    @Test
    @DisplayName("apply: проверка правильности вызова orderUpdateService.updateOrderItems")
    void checkApply() {
        Order order = OrderBuilder.init(16L, OrderType.FF)
                .addItem(100L, 2)
                .addItem(101L, 3)
                .addItem(102L, 4)
                .build();
        ItemsRemovalChangeRequestPayload payload = initPayload(ITEMS_NOT_FOUND, order.getItem(100L),
                order.getItem(102L));

        itemsRemovalChangeRequestProcessor.apply(order, payload, clientInfo);

        verify(orderUpdateService, times(1))
                .updateOrderItems(idCaptor.capture(), requestCaptor.capture(), reasonCaptor.capture(),
                        clientInfoCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(order.getId());
        assertThat(requestCaptor.getValue())
                .hasSize(2)
                .extracting(OrderItemChangeRequest::getItemId)
                .containsExactlyInAnyOrder(100L, 102L);
        assertThat(reasonCaptor.getValue()).isEqualTo(ITEMS_NOT_FOUND);
        assertThat(clientInfoCaptor.getValue()).isEqualTo(clientInfo);
    }

    private ItemsRemovalChangeRequestPayload initPayload(HistoryEventReason reason, OrderItem... items) {
        return new ItemsRemovalChangeRequestPayload(Set.of(items),
                Collections.emptySet(), reason);
    }

    private enum OrderType {
        FF, FBS, DBS
    }

    private static class OrderBuilder {

        private final Order order;

        private OrderBuilder() {
            order = new Order();
            order.setStatus(OrderStatus.PROCESSING);
            order.setDelivery(new Delivery());
        }

        public static OrderBuilder init(long orderId, OrderType type) {
            OrderBuilder builder = new OrderBuilder();
            builder.setOrderId(orderId);
            builder.setType(type);
            return builder;
        }

        private void setOrderId(Long orderId) {
            order.setId(orderId);
        }

        private void setType(OrderType type) {
            if (type == OrderType.FF) {
                order.setFulfilment(true);
                order.getDelivery().setDeliveryPartnerType(YANDEX_MARKET);
            } else if (type == OrderType.FBS) {
                order.setFulfilment(false);
                order.getDelivery().setDeliveryPartnerType(YANDEX_MARKET);
            } else {
                order.setFulfilment(false);
                order.getDelivery().setDeliveryPartnerType(SHOP);
            }
        }

        public OrderBuilder addItem(Long itemId, int count) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(itemId);
            orderItem.setCount(count);
            orderItem.setOfferItemKey(new OfferItemKey("offerId", itemId, "bundleId" + itemId));
            order.addItem(orderItem);
            return this;
        }

        public OrderBuilder setStatus(OrderStatus status) {
            order.setStatus(status);
            return this;
        }

        public Order build() {
            return order;
        }
    }

    private Order initOrderWithType(OrderType orderType) {
        return OrderBuilder.init(1L, orderType).build();
    }
}

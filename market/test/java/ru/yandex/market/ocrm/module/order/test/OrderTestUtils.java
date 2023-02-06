package ru.yandex.market.ocrm.module.order.test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.entity.HasYymmId;
import ru.yandex.market.jmf.trigger.EntityEvent;
import ru.yandex.market.jmf.trigger.TriggerConstants;
import ru.yandex.market.jmf.trigger.TriggerService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.date.Dates;
import ru.yandex.market.ocrm.module.checkouter.test.MockCheckouterAPI;
import ru.yandex.market.ocrm.module.order.domain.DeliveryService;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderItem;
import ru.yandex.market.ocrm.module.order.domain.OrderItemHistory;
import ru.yandex.market.ocrm.module.order.domain.SortingCenter;
import ru.yandex.market.ocrm.module.order.impl.event.OrderEventImpl;

import static ru.yandex.market.jmf.utils.Maps.unsafeGet;

@Component
public class OrderTestUtils {

    @Inject
    BcpService bcpService;
    @Inject
    TriggerService triggerService;
    @Inject
    MockCheckouterAPI mockCheckouterAPI;
    @Inject
    EntityAdapterService entityAdapterService;

    public void clearCheckouterAPI() {
        mockCheckouterAPI.clear();
    }

    public void fireOrderImportedEvent(@Nonnull Order order) {
        fireOrderImportedEvent(order, ClientRole.SHOP, HistoryEventType.ORDER_STATUS_UPDATED);
    }

    public void fireOrderImportedEvent(@Nonnull Order oldOrder,
                                       @Nonnull Order newOrder) {
        fireOrderImportedEvent(newOrder, oldOrder, ClientRole.SHOP, HistoryEventType.ORDER_STATUS_UPDATED, null);
    }

    public void fireOrderImportedEvent(
            @Nonnull Order order,
            @Nonnull ClientRole role,
            @Nonnull HistoryEventType eventType
    ) {
        fireOrderImportedEvent(order, null, role, eventType, null);
    }

    public void fireOrderImportedEvent(
            @Nonnull Order order,
            @Nonnull ClientRole role,
            @Nonnull HistoryEventType eventType,
            Long returnId
    ) {
        fireOrderImportedEvent(order, null, role, eventType, returnId);
    }

    public void fireOrderImportedEvent(
            @Nonnull Order order,
            Order oldOrder,
            @Nonnull ClientRole role,
            @Nonnull HistoryEventType eventType,
            Long returnId
    ) {
        EntityEvent event = new EntityEvent(order.getMetaclass(), TriggerConstants.IMPORTED, order, oldOrder);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setAuthor(new ClientInfo(role, Randoms.longValue()));
        orderHistoryEvent.setType(eventType);
        orderHistoryEvent.setReturnId(returnId);
        Entity orderEvent = entityAdapterService.wrap(new OrderEventImpl(Optional.of(orderHistoryEvent)));

        event.addVariable("event", orderEvent);

        triggerService.execute(event);
    }

    public Order createOrder() {
        return createOrder(Map.of());
    }

    public Order createOrder(Map<String, Object> parameters) {
        Long number = (Long) parameters.get(Order.NUMBER);
        if (number == null) {
            number = Randoms.positiveLongValue();
        }

        OffsetDateTime creationDate = OffsetDateTime.now();
        return createOrder(number, creationDate, parameters);
    }

    @NotNull
    public Order createOrder(long number, OffsetDateTime creationDate, Map<String, Object> parameters) {
        Map<String, Object> p = Maps.of(
                "id", HasYymmId.idOf(creationDate, number),
                Order.BUYER_EMAIL, Randoms.email(),
                Order.BUYER_FIRST_NAME, Randoms.string(),
                Order.BUYER_LAST_NAME, Randoms.string(),
                Order.BUYER_MIDDLE_NAME, Randoms.string(),
                Order.BUYER_MUID, Randoms.unsignedLongValue(),
                Order.BUYER_PHONE, Randoms.phoneNumber(),
                Order.BUYER_UID, 0, // TODO
                Order.BUYER_UUID, Randoms.string(),
                Order.BUYER_YANDEX_UID, Randoms.unsignedLongValue(),
                Order.COLOR, "BLUE",
                Order.CREATION_DATE, creationDate,
                Order.CUSTOMER, null, // TODO
                Order.DELIVERY_FROM_DATE, Randoms.date(),
                Order.DELIVERY_TO_DATE, Randoms.date(),
                Order.NUMBER, number,
                Order.RECIPIENT_FIRST_NAME, Randoms.string(),
                Order.RECIPIENT_LAST_NAME, Randoms.string(),
                Order.RECIPIENT_MIDDLE_NAME, Randoms.string(),
                Order.STATUS, OrderStatus.PLACING,
                Order.PAYMENT_METHOD, PaymentMethod.CASH_ON_DELIVERY,
                Order.SHOP_ID, Randoms.longValue()
        );

        // Что бы в mockOrder() можно было изменять мапу
        // Это нужно что бы убрать из parameters атрибуты для которых нет сеттера (store у которых script)
        parameters = new HashMap<>(parameters);
        mockOrder(number, parameters);

        return bcpService.create(Order.FQN, mergeParams(p, parameters));
    }

    public void mockOrder(Long orderId, Map<String, Object> parameters) {
        var order = new ru.yandex.market.checkout.checkouter.order.Order();
        order.setId(orderId);
        order.setNotes(Randoms.string());
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        order.setBuyerCurrency(Currency.RUR);
        order.setBuyerItemsTotal(BigDecimal.ZERO);
        order.setBuyerTotal(BigDecimal.ZERO);
        order.setCurrency(Currency.RUR);
        order.setDeliveryCurrency(Currency.RUR);
        order.setItemsTotal(BigDecimal.ZERO);
        order.setPreorder(false);

        order.setProperty(OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD, false);

        order.setDelivery(new Delivery());
        Delivery delivery = order.getDelivery();
        delivery.setBuyerPrice(BigDecimal.ZERO);

        order.getDelivery().setBuyerAddress(new AddressImpl());
        AddressImpl buyerAddress = (AddressImpl) order.getDelivery().getBuyerAddress();
        buyerAddress.setApartment(Randoms.stringNumber());
        buyerAddress.setBlock(Randoms.stringNumber());
        buyerAddress.setBuilding(Randoms.stringNumber());
        buyerAddress.setCity(Randoms.string());
        buyerAddress.setCountry(Randoms.string());
        buyerAddress.setEntrance(Randoms.stringNumber());
        buyerAddress.setEntryPhone(Randoms.stringNumber());
        buyerAddress.setEstate(Randoms.stringNumber());
        buyerAddress.setFloor(Randoms.stringNumber());
        buyerAddress.setHouse(Randoms.stringNumber());
        buyerAddress.setNotes(Randoms.string());
        buyerAddress.setPhone(Randoms.phoneNumber());
        buyerAddress.setPostcode(Randoms.stringNumber());
        buyerAddress.setRecipient(Randoms.string());
        buyerAddress.setScheduleString(Randoms.string());
        buyerAddress.setStreet(Randoms.string());
        buyerAddress.setSubway(Randoms.string());
        buyerAddress.setSubway(Randoms.string());

        order.setBuyer(new Buyer());
        Buyer buyer = order.getBuyer();
        buyer.setIp(Randoms.ip());
        buyer.setBeenCalled(false);
        buyer.setDontCall(false);

        for (Map.Entry<String, Object> entry : List.copyOf(parameters.entrySet())) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case Order.DELIVERY_FEATURES:
                    // Удаляем что бы потом не было попытки сохранить это поле, т.к. store у него script
                    parameters.remove(key);

                    if (((Collection<?>) value).size() > 0 && ((Collection<?>) value).iterator().next() instanceof DeliveryFeature) {
                        order.getDelivery().setFeatures(Set.of(((Collection<DeliveryFeature>) value).toArray(DeliveryFeature[]::new)));
                    } else if (((Collection<?>) value).size() > 0 && ((Collection<?>) value).iterator().next() instanceof String) {
                        Set<DeliveryFeature> features =
                                Arrays.stream(DeliveryFeature.values())
                                        .filter(f -> ((Collection<String>) value).contains(f.name()))
                                        .collect(Collectors.toSet());
                        order.getDelivery().setFeatures(features);
                    }
                    break;
                case Order.DELIVERY_PARTNER_TYPE:
                    // Удаляем что бы потом не было попытки сохранить это поле, т.к. store у него script
                    parameters.remove(key);

                    order.getDelivery().setDeliveryPartnerType((DeliveryPartnerType) value);
                    break;
                case Order.PREORDER:
                    // Удаляем что бы потом не было попытки сохранить это поле, т.к. store у него script
                    parameters.remove(key);

                    order.setPreorder((Boolean) value);
                    break;
                case Order.STATUS:
                    if (value instanceof OrderStatus status) {
                        order.setStatus(status);
                    } else if (value instanceof String status) {
                        order.setStatus(OrderStatus.valueOf(status));
                    }
                    break;
                case Order.SUB_STATUS:
                    if (value instanceof OrderSubstatus substatus) {
                        order.setSubstatus(substatus);
                    } else if (value instanceof String substatus) {
                        order.setSubstatus(OrderSubstatus.valueOf(substatus));
                    }
                    break;
            }
        }

        mockCheckouterAPI.mockGetOrder(orderId, order);
    }

    public void mockGetOrderHistory(Order order, Collection<OrderHistoryEvent> orderItems) {
        mockCheckouterAPI.mockGetOrderHistory(order.getOrderId(), orderItems);
    }

    public Map<String, Object> mockOrderItem(Order order, Map<String, Object> parameters) {
        Map<String, Object> defaultParams = generateOrderItemParams(order);

        var resultParams = mergeParams(defaultParams, parameters);
        var checkouterOrderItem = createCheckouterOrderItem(resultParams);
        mockCheckouterAPI.mockGetOrderItems(order.getOrderId(), Collections.singletonList(checkouterOrderItem));

        return resultParams;
    }

    private Map<String, Object> generateOrderItemParams(Order order) {
        return Maps.of(
                OrderItem.PARENT, order,
                OrderItem.CHECKOUTER_ID, Randoms.longValue(),
                OrderItem.SUPPLIER_TYPE, SupplierType.FIRST_PARTY,
                OrderItem.BUYER_DISCOUNT, Randoms.bigDecimal(),
                OrderItem.BUYER_PRICE, Randoms.bigDecimal(),
                OrderItem.BUYER_PRICE_BEFORE_DISCOUNT, Randoms.bigDecimal(),
                OrderItem.BUYER_PRICE_NOMINAL, Randoms.bigDecimal(),
                OrderItem.AT_SUPPLIER_WAREHOUSE, Boolean.FALSE,
                OrderItem.BUYER_SUBSIDY, Randoms.bigDecimal(),
                OrderItem.CATEGORY_ID, Randoms.intValue(),
                OrderItem.CATEGORY_NAME, Randoms.string(),
                OrderItem.COUNT, Randoms.intValue(),
                OrderItem.DEPTH, Randoms.longValue(),
                OrderItem.DESCRIPTION, Randoms.string(),
                OrderItem.FEED_PRICE, Randoms.bigDecimal(),
                OrderItem.HEIGHT, Randoms.longValue(),
                OrderItem.MODEL_ID, Randoms.longValue(),
                OrderItem.MSKU, Randoms.longValue(),
                OrderItem.OLD_MIN, Randoms.bigDecimal(),
                OrderItem.PARTNER_PRICE, Randoms.bigDecimal(),
                OrderItem.PICTURE_URL, Randoms.string(),
                OrderItem.SUPPLIER_ID, Randoms.longValue(),
                OrderItem.TITLE, Randoms.string(),
                OrderItem.VENDOR_ID, Randoms.longValue(),
                OrderItem.WAREHOUSE_ID, Randoms.intValue(),
                OrderItem.WEIGHT, Randoms.longValue(),
                OrderItem.WIDTH, Randoms.longValue()
        );
    }

    public ru.yandex.market.checkout.checkouter.order.OrderItem createCheckouterOrderItem(Map<String, Object> props) {
        var orderItem = new ru.yandex.market.checkout.checkouter.order.OrderItem();

        var prices = orderItem.getPrices();
        prices.setBuyerDiscount(unsafeGet(OrderItem.BUYER_DISCOUNT, props));
        prices.setBuyerSubsidy(unsafeGet(OrderItem.BUYER_SUBSIDY, props));
        prices.setBuyerPriceNominal(unsafeGet(OrderItem.BUYER_PRICE_NOMINAL, props));
        prices.setBuyerPriceBeforeDiscount(unsafeGet(OrderItem.BUYER_PRICE_BEFORE_DISCOUNT, props));
        prices.setFeedPrice(unsafeGet(OrderItem.FEED_PRICE, props));
        prices.setPartnerPrice(unsafeGet(OrderItem.PARTNER_PRICE, props));
        prices.setOldMin(unsafeGet(OrderItem.OLD_MIN, props));

        orderItem.setId(unsafeGet(OrderItem.CHECKOUTER_ID, props));
        orderItem.setOfferName(unsafeGet(OrderItem.TITLE, props));
        orderItem.setBuyerPrice(unsafeGet(OrderItem.BUYER_PRICE, props));
        orderItem.setCategoryId(unsafeGet(OrderItem.CATEGORY_ID, props));
        orderItem.setCategoryFullName(unsafeGet(OrderItem.CATEGORY_NAME, props));
        orderItem.setCount(unsafeGet(OrderItem.COUNT, props));
        orderItem.setDepth(unsafeGet(OrderItem.DEPTH, props));
        orderItem.setDescription(unsafeGet(OrderItem.DESCRIPTION, props));
        orderItem.setHeight(unsafeGet(OrderItem.HEIGHT, props));
        orderItem.setModelId(unsafeGet(OrderItem.MODEL_ID, props));
        orderItem.setMsku(unsafeGet(OrderItem.MSKU, props));
        orderItem.setPictureURL(unsafeGet(OrderItem.PICTURE_URL, props));
        orderItem.setSupplierId(unsafeGet(OrderItem.SUPPLIER_ID, props));
        orderItem.setVendorId(unsafeGet(OrderItem.VENDOR_ID, props));
        orderItem.setWarehouseId(unsafeGet(OrderItem.WAREHOUSE_ID, props));
        orderItem.setWeight(unsafeGet(OrderItem.WEIGHT, props));
        orderItem.setWidth(unsafeGet(OrderItem.WIDTH, props));
        orderItem.setAtSupplierWarehouse(unsafeGet(OrderItem.AT_SUPPLIER_WAREHOUSE, props));
        orderItem.setSupplierType(unsafeGet(OrderItem.SUPPLIER_TYPE, props));

        return orderItem;
    }

    @NotNull
    public DeliveryService createDeliveryService(String title) {
        Map<String, Object> defaultParams = Map.of(
                DeliveryService.CODE, Randoms.stringNumber(),
                DeliveryService.TITLE, title,
                DeliveryService.URL, Randoms.string()
        );

        return bcpService.create(DeliveryService.FQN, defaultParams);
    }

    @NotNull
    public SortingCenter createSortingCenter(String title) {
        Map<String, Object> defaultParams = Map.of(
                SortingCenter.CODE, Randoms.stringNumber(),
                SortingCenter.TITLE, title,
                SortingCenter.URL, Randoms.url()
        );

        return bcpService.create(SortingCenter.FQN, defaultParams);
    }

    private Map<String, Object> mergeParams(Map<String, Object> defaultParams, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        result.putAll(defaultParams);
        result.putAll(parameters);
        return result;
    }

    public void createOrderTrackCheckpoint(Order order,
                                           int checkpointStatus,
                                           OffsetDateTime checkpointDate) {
        TrackCheckpoint trackCheckpoint = new TrackCheckpoint(
                null, null, null, null, null, null, Dates.date(checkpointDate), checkpointStatus);
        trackCheckpoint.setId(123L);
        mockCheckouterAPI.mockGetOrder(order.getOrderId(), trackCheckpoint);
    }

    public OrderItemHistory createOrderItemHistory(Order order, Map<String, Object> params) {
        Map<String, Object> defaultParams = Maps.of(
                OrderItemHistory.CHECKOUTER_ID, Randoms.positiveLongValue(),
                OrderItemHistory.INITIAL_COUNT, Randoms.positiveLongValue(),
                OrderItemHistory.MISSING_COUNT, Randoms.positiveLongValue()
        );
        return createEmptyOrderItemHistory(order, mergeParams(defaultParams, params));
    }

    public OrderItemHistory createEmptyOrderItemHistory(Order order, Map<String, Object> params) {
        Map<String, Object> defaultParams = Maps.of(
                OrderItemHistory.PARENT, order,
                OrderItemHistory.COUNT_CHANGE_REASON, Randoms.string()
        );
        return bcpService.create(OrderItemHistory.FQN, mergeParams(defaultParams, params));
    }

    public Map<String, Object> mockCheckouterOrderHistory_lostItem(Order order,
                                                                   Map<String, Object> parameters) {
        Map<String, Object> defaultParams = generateOrderItemParams(order);
        var checkouterOrderItemParams = mergeParams(defaultParams, parameters);
        var checkouterOrderItem = createCheckouterOrderItem(checkouterOrderItemParams);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        var orderBefore = new ru.yandex.market.checkout.checkouter.order.Order();
        orderBefore.addItem(checkouterOrderItem);
        var orderAfter = new ru.yandex.market.checkout.checkouter.order.Order();
        orderAfter.setItems(List.of());

        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);
        mockCheckouterAPI.mockGetOrderHistory(order.getOrderId(), Collections.singletonList(orderHistoryEvent));

        return checkouterOrderItemParams;
    }
}

package ru.yandex.market.checkout.checkouter.tasks.eventexport.erp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentProperties;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.returns.ReturnHistory;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.tdb.ErpOrderEvent;
import ru.yandex.market.checkout.checkouter.tdb.ErpOrderItem;
import ru.yandex.market.checkout.checkouter.tdb.ErpReturn;
import ru.yandex.market.checkout.test.builders.OrderHistoryEventBuilder;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.tasks.eventexport.erp.ErpOrderEventMapper.mapper;
import static ru.yandex.market.checkout.test.builders.OrderHistoryEventBuilder.anOrderHistoryEvent;

public class ErpOrderEventMapperTest {

    private final PersonalDataService personalDataService = Mockito.mock(PersonalDataService.class);

    @Test
    public void testMapper() {
        mockPersonalDataService(AddressProvider.getAddress());

        Order order = OrderProvider.getFulfillmentOrderWithYandexDelivery();
        order.setId(123L);
        order.setUid(5555L);
        order.setPaymentType(PaymentType.PREPAID);
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreationDate(new Date());
        order.getDelivery().setPrice(BigDecimal.valueOf(100));
        order.getDelivery().setDeliveryServiceId(777L);
        order.getDelivery().setShopAddress(AddressProvider.getAddress());
        order.setPayment(new Payment());
        order.getPayment().setProperties(new PaymentProperties());
        order.getPayment().getProperties().setCession(true);
        Order orderBefore = OrderProvider.getFulfillmentOrderWithYandexDelivery();
        orderBefore.setStatus(OrderStatus.PLACING);
        OrderHistoryEvent event = anOrderHistoryEvent().withId(12345L)
                .withOrderBefore(orderBefore)
                .withOrderAfter(order)
                .withEventType(HistoryEventType.ORDER_STATUS_UPDATED)
                .build();

        List<ErpOrderEvent> result = mapper().map(singletonList(
                new ErpExportOrderEvent(event, Collections.emptyList())), personalDataService);

        assertThat(result, hasSize(1));
        ErpOrderEvent resEvent = result.get(0);
        assertThat(resEvent.getOrderId(), equalTo(order.getId()));
        assertThat(resEvent.getEventId(), equalTo(event.getId()));
        assertThat(resEvent.getEventType(), equalTo(event.getType().name()));
        assertThat(resEvent.getCreatedAt(), equalTo(event.getOrderAfter().getCreationDate()));
        assertThat(resEvent.getUpdatedAt(), equalTo(event.getFromDate()));
        assertThat(resEvent.getPrevStatus(), equalTo(orderBefore.getStatus().name()));
        assertThat(resEvent.getStatus(), equalTo(order.getStatus().name()));
        assertThat(resEvent.getBuyerCurrency(), equalTo(event.getOrderAfter().getBuyerCurrency().name()));
        assertThat(resEvent.getPaymentType(), equalTo(event.getOrderAfter().getPaymentType().name()));
        assertThat(resEvent.getDeliveryType(), equalTo(event.getOrderAfter().getDelivery().getType().name()));
        assertThat(resEvent.getDeliveryDate(),
                equalTo(event.getOrderAfter().getDelivery().getDeliveryDates().getFromDate()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getBuyerAddress().getCountry()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getBuyerAddress().getCity()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getBuyerAddress().getStreet()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getBuyerAddress().getBuilding()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getBuyerAddress().getBlock()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getBuyerAddress().getApartment()));
        assertThat(resEvent.getDeliveryRegionId(),
                equalTo(event.getOrderAfter().getDelivery().getRegionId().intValue()));
        assertThat(resEvent.getDeliveryCity(),
                equalTo(event.getOrderAfter().getDelivery().getBuyerAddress().getCity()));
        assertThat(resEvent.getDeliveryPrice(),
                equalTo(event.getOrderAfter().getDelivery().getPrice()));
        assertThat(resEvent.getDeliveryServiceId(),
                equalTo(event.getOrderAfter().getDelivery().getDeliveryServiceId().intValue()));
        assertThat(resEvent.getUserId(), equalTo(event.getOrderAfter().getUid()));
        assertThat(resEvent.getPaymentProperties(), equalTo("{\"cession\":true}"));
    }

    @Test
    public void useShopAddressIfNoBuyerAddress() {
        mockPersonalDataService(AddressProvider.getAnotherAddress());

        Order order = OrderProvider.getFulfillmentOrderWithYandexDelivery();
        order.setId(123L);
        order.setUid(5555L);
        order.getDelivery().setBuyerAddress(null);
        order.getDelivery().setShopAddress(AddressProvider.getAnotherAddress());
        OrderHistoryEvent event = anOrderHistoryEvent().withId(12345L)
                .withOrderAfter(order)
                .withEventType(HistoryEventType.ORDER_STATUS_UPDATED)
                .build();

        List<ErpOrderEvent> result = mapper().map(
                singletonList(new ErpExportOrderEvent(event, Collections.emptyList())), personalDataService);

        assertThat(result, hasSize(1));
        ErpOrderEvent resEvent = result.get(0);
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getShopAddress().getCountry()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getShopAddress().getCity()));
        assertThat(resEvent.getDeliveryAddress(),
                containsString(event.getOrderAfter().getDelivery().getShopAddress().getStreet()));
        assertThat(resEvent.getDeliveryCity(),
                equalTo(event.getOrderAfter().getDelivery().getShopAddress().getCity()));
    }

    @Test
    public void testItemsMapper() {
        mockPersonalDataService(AddressProvider.getAnotherAddress());

        Order order = OrderProvider.getFulfillmentOrderWithYandexDelivery();
        order.setId(123L);
        order.setUid(5555L);
        order.setPaymentType(PaymentType.PREPAID);
        order.setStatus(OrderStatus.PROCESSING);

        OrderItem firstPartyOrderItem = OrderItemProvider.defaultOrderItem();
        firstPartyOrderItem.setSupplierType(SupplierType.FIRST_PARTY);
        firstPartyOrderItem.setSupplierId(777L);
        firstPartyOrderItem.setSku("333");
        OrderItem thirdPartyOrderItem = OrderItemProvider.defaultOrderItem();
        thirdPartyOrderItem.setSupplierType(SupplierType.THIRD_PARTY);
        order.setItems(Arrays.asList(firstPartyOrderItem, thirdPartyOrderItem));

        OrderHistoryEvent event = anOrderHistoryEvent().withId(12345L)
                .withOrderAfter(order)
                .withEventType(HistoryEventType.ORDER_STATUS_UPDATED)
                .build();

        List<ErpOrderEvent> result = mapper().map(
                singletonList(new ErpExportOrderEvent(event, singletonList(
                        new ErpExportOrderItem(firstPartyOrderItem)))), personalDataService);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getItems(), hasSize(1));
        ErpOrderItem resItem = result.get(0).getItems().get(0);
        assertThat(resItem.getOrderId(), equalTo(order.getId()));
        assertThat(resItem.getEventId(), equalTo(event.getId()));
        assertThat(resItem.getItemId(), equalTo(firstPartyOrderItem.getId()));
        assertThat(resItem.getSupplierId(), equalTo(firstPartyOrderItem.getSupplierId()));
        assertThat(resItem.getSupplierType(), equalTo(firstPartyOrderItem.getSupplierType().name()));
        assertThat(resItem.getSsku(), equalTo(firstPartyOrderItem.getShopSku()));
        assertThat(resItem.getMsku(), equalTo(Long.valueOf(firstPartyOrderItem.getSku())));
        assertThat(resItem.getCount(), equalTo(firstPartyOrderItem.getCount()));
        assertThat(resItem.getPrice(), equalTo(firstPartyOrderItem.getPrice()));
        assertThat(resItem.getVat(), equalTo(firstPartyOrderItem.getVat().name()));
    }

    @Test
    public void testReturnsMapper() {
        mockPersonalDataService(AddressProvider.getAnotherAddress());

        final Long returnId = 7777L;
        final OrderHistoryEvent orderHistoryEvent = getOrderReturnCreatedEvent(returnId);
        final Collection<ReturnItem> itemsToExport = Collections.emptyList();
        final ErpExportReturnEvent returnEvent = new ErpExportReturnEvent(orderHistoryEvent,
                fromOrderHistoryEvent(orderHistoryEvent), itemsToExport);

        List<ErpReturn> result = mapper().mapReturns(singletonList(returnEvent));

        assertEquals(1, result.size());
        final ErpReturn r = result.get(0);
        assertTrue(r.getPayOffline());
        assertThat(r.getReturnId(), equalTo(returnId));
    }

    private static OrderHistoryEvent getOrderReturnCreatedEvent(Long returnId) {
        Instant now = Instant.now();
        Order order = OrderProvider.getBlueOrder();
        order.getItems().forEach(i -> i.setCount(2));
        order.setId(123L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreationDate(Date.from(now));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        order.setRgb(Color.BLUE);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(now))
                .withEventType(HistoryEventType.ORDER_RETURN_CREATED)
                .withReturnId(returnId)
                .withOrder(order)
                .build();
    }

    private static ReturnHistory fromOrderHistoryEvent(OrderHistoryEvent orderHistoryEvent) {
        Order order = orderHistoryEvent.getOrderAfter();
        ReturnHistory rh = new ReturnHistory();
        rh.setOrderId(order.getId());
        rh.setPayOffline(true);
        rh.setId(orderHistoryEvent.getReturnId());
        rh.setCreatedAt(order.getCreationDate().toInstant());
        return rh;
    }

    private void mockPersonalDataService(Address baseAddress) {
        PersAddress address = new PersAddress();
        address.setCountry(baseAddress.getCountry());
        address.setCity(baseAddress.getCity());
        address.setStreet(baseAddress.getStreet());
        address.setBuilding(baseAddress.getBuilding());
        address.setHouse(baseAddress.getHouse());
        address.setApartment(baseAddress.getApartment());
        address.setBlock(baseAddress.getBlock());

        when(personalDataService.getPersAddress(any())).thenReturn(address);
    }
}

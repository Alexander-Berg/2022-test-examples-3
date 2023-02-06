package ru.yandex.market.partner.mvc.controller.delivery;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.shipment.FirstMileShipmentErrorUtil;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.error.ErrorType;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceNotFoundError;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatusChange;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;

/**
 * Функциональный тест для {@link OrdersShipmentInfoController}.
 */
@DbUnitDataSet(before = "OrdersShipmentInfoControllerTest.before.csv")
class OrdersShipmentInfoControllerTest extends FunctionalTest {

    private static final long USER_ID = 100500L;
    private static final long SHIPMENT_ID = 999L;
    private static final long SUPPLIER_ID = 101L;
    private static final long CAMPAIGN_ID = 101101L;

    @Autowired
    private CheckouterClient checkouterClient;

    @Autowired
    private NesuClient nesuClient;

    @Test
    @DisplayName("Получение информации о заказах в подтвержденной отгрузке")
    void testGetOrdersConfirmedShipment() {
        mockNesuClientConfirmedShipment();
        //language=json
        String expected = "{\"orderIdsWithLabels\":[5,6,7],\"orderIdsWithoutLabels\":[]}";
        ResponseEntity<String> response = getResponse();
        JsonTestUtil.assertEquals(response, expected);
        Mockito.verifyZeroInteractions(checkouterClient);
    }

    @Test
    @DisplayName("Получение информации о заказах в подтвержденной отгрузке, передан список orderId")
    void testGetOrdersConfirmedShipmentWithOrderId() {
        mockNesuClientConfirmedShipment();
        //language=json
        String expected = "{\"orderIdsWithLabels\":[5,7],\"orderIdsWithoutLabels\":[]}";
        ResponseEntity<String> response = getResponse(List.of(5L, 7L));
        JsonTestUtil.assertEquals(response, expected);
        Mockito.verifyZeroInteractions(checkouterClient);
    }

    @Test
    @DisplayName("Получение информации о заказах в неподтвержденной отгрузке")
    void testGetOrdersNotConfirmedShipment() {
        mockNesuClientNotConfirmedShipment();
        mockCheckouterClient(Set.of(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        //language=json
        String expected = "{\"orderIdsWithLabels\":[6,7],\"orderIdsWithoutLabels\":[3,4,5,8,9,10]}";
        ResponseEntity<String> response = getResponse();
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о заказах в неподтвержденной отгрузке, передан список orderId")
    void testGetOrdersNotConfirmedShipmentWithOrderId() {
        mockNesuClientNotConfirmedShipment();
        mockCheckouterClient(Set.of(3L, 6L, 8L));
        //language=json
        String expected = "{\"orderIdsWithLabels\":[6],\"orderIdsWithoutLabels\":[3,8]}";
        ResponseEntity<String> response = getResponse(List.of(6L, 3L, 8L));
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о заказах в пустой отгрузке")
    void testGetOrdersEmptyShipment() {
        mockNesuClientEmptyShipment();
        //language=json
        String expected = "{\"orderIdsWithLabels\":[],\"orderIdsWithoutLabels\":[]}";
        ResponseEntity<String> response = getResponse();
        JsonTestUtil.assertEquals(response, expected);
        Mockito.verifyZeroInteractions(checkouterClient);
    }

    @Test
    @DisplayName("Ошибка, если клиент несу возвращает 404")
    void testNotFoundFromNesu() throws JsonProcessingException {
        mockNesuClientError();
        HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class,
                this::getResponse);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    private void mockNesuClientConfirmedShipment() {
        Mockito.when(nesuClient.getShipment(Mockito.eq(USER_ID), Mockito.eq(SUPPLIER_ID), Mockito.eq(SHIPMENT_ID)))
                .thenReturn(
                        getPartnerShipmentDtoBuilder()
                                .orderIds(List.of(5L, 6L, 7L, 8L, 9L))
                                .confirmedOrderIds(List.of(5L, 6L, 7L))
                                .build());
    }

    private void mockNesuClientNotConfirmedShipment() {
        Mockito.when(nesuClient.getShipment(Mockito.eq(USER_ID), Mockito.eq(SUPPLIER_ID), Mockito.eq(SHIPMENT_ID)))
                .thenReturn(
                        getPartnerShipmentDtoBuilder()
                                .orderIds(List.of(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L))
                                .confirmedOrderIds(Collections.emptyList())
                                .build());
    }

    private void mockNesuClientEmptyShipment() {
        Mockito.when(nesuClient.getShipment(Mockito.eq(USER_ID), Mockito.eq(SUPPLIER_ID), Mockito.eq(SHIPMENT_ID)))
                .thenReturn(
                        getPartnerShipmentDtoBuilder()
                                .orderIds(Collections.emptyList())
                                .confirmedOrderIds(Collections.emptyList())
                                .build());
    }

    private void mockNesuClientError() throws JsonProcessingException {
        final ResourceNotFoundError notFoundError = new ResourceNotFoundError();
        notFoundError.setType(ErrorType.RESOURCE_NOT_FOUND);
        notFoundError.setResourceType(ResourceType.PARTNER_SHIPMENT);

        Mockito.when(nesuClient.getShipment(Mockito.eq(USER_ID), Mockito.eq(SUPPLIER_ID), Mockito.eq(SHIPMENT_ID)))
                .thenThrow(new HttpTemplateException(HttpStatus.NOT_FOUND.value(),
                        FirstMileShipmentErrorUtil.getNesuErrorObjectMapper().writeValueAsString(notFoundError)));
    }

    private PartnerShipmentDto.PartnerShipmentDtoBuilder getPartnerShipmentDtoBuilder() {
        return PartnerShipmentDto.builder()
                .id(999L)
                .planIntervalFrom(LocalDateTime.parse("2021-05-08T10:15:00"))
                .planIntervalTo(LocalDateTime.parse("2021-05-26T10:15:00"))
                .shipmentType(ShipmentType.WITHDRAW)
                .partner(NamedEntity.builder().id(12348L).name("Del Service").build())
                .currentStatus(PartnerShipmentStatusChange.builder()
                        .code(PartnerShipmentStatus.MOVEMENT_COURIER_FOUND)
                        .datetime(LocalDateTime.parse("2021-05-16T10:15:00").toInstant(ZoneOffset.UTC))
                        .description("Some text")
                        .build());
    }

    private void mockCheckouterClient(Set<Long> orderIds) {
        var validDelivery = prepareValidDelivery();
        //без доставки
        Order order3 = new Order();
        order3.setShopId(SUPPLIER_ID);
        order3.setId(3L);
        order3.setStatus(OrderStatus.PROCESSING);
        order3.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order3.setShopOrderId("номер3");
        order3.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));

        //без сервисИд в доставке
        Order order4 = new Order();
        order4.setShopId(SUPPLIER_ID);
        order4.setId(4L);
        order4.setStatus(OrderStatus.PROCESSING);
        order4.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order4.setShopOrderId("номер4");
        order4.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order4.setDelivery(prepareInvalidDelivery());

        //не пройдет по статусу
        Order order5 = new Order();
        order5.setShopId(SUPPLIER_ID);
        order5.setId(5L);
        order5.setStatus(OrderStatus.CANCELLED);
        order5.setShopOrderId("номер5");
        order5.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order5.setDelivery(validDelivery);

        //корректный
        Order order6 = new Order();
        order6.setShopId(SUPPLIER_ID);
        order6.setId(6L);
        order6.setStatus(OrderStatus.PROCESSING);
        order6.setSubstatus(OrderSubstatus.SHIPPED);
        order6.setShopOrderId("номер6");
        order6.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order6.setDelivery(validDelivery);

        //корректный
        Order order7 = new Order();
        order7.setShopId(SUPPLIER_ID);
        order7.setId(7L);
        order7.setStatus(OrderStatus.PROCESSING);
        order7.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order7.setShopOrderId("номер7");
        order7.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order7.setDelivery(validDelivery);

        //некорректный подстатус
        Order order8 = new Order();
        order8.setShopId(SUPPLIER_ID);
        order8.setId(8L);
        order8.setStatus(OrderStatus.PROCESSING);
        order8.setSubstatus(OrderSubstatus.PACKAGING);
        order8.setShopOrderId("номер8");
        order8.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order8.setDelivery(validDelivery);

        //нет коробок
        Order order9 = new Order();
        order9.setShopId(SUPPLIER_ID);
        order9.setId(9L);
        order9.setStatus(OrderStatus.PROCESSING);
        order9.setSubstatus(OrderSubstatus.SHIPPED);
        order9.setShopOrderId("номер9");
        order9.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order9.setDelivery(prepareDeliveryWithoutBoxes());

        //нет fulfillment_id в коробках
        Order order10 = new Order();
        order10.setShopId(SUPPLIER_ID);
        order10.setId(10L);
        order10.setStatus(OrderStatus.PROCESSING);
        order10.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order10.setShopOrderId("номер10");
        order10.setCreationDate(Date.from(LocalDateTime.parse("2021-05-08T10:15:00").toInstant(ZoneOffset.UTC)));
        order10.setDelivery(prepareDeliveryWithoutFFId());

        var orders = Stream.of(order3, order4, order5, order6, order7, order8, order9, order10)
                .filter(o -> orderIds.contains(o.getId())).collect(Collectors.toList());

        Mockito.when(checkouterClient.getOrders(any(), any()))
                .thenReturn(new PagedOrders(orders,
                        Pager.atPage(1, 50)));
    }

    private String buildRequest(List<Long> orderIds) {
        var url = baseUrl + "/campaigns/{campaignId}/shipments/{shipmentId}/orders/info?_user_id=12345&euid=100500";
        if (orderIds != null && !orderIds.isEmpty()) {
            url += orderIds.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining("&orderId=", "&orderId=", ""));
        }
        return url;
    }

    private ResponseEntity<String> getResponse() {
        return getResponse(null);
    }

    private ResponseEntity<String> getResponse(List<Long> orderIds) {
        return FunctionalTestHelper.get(
                buildRequest(orderIds),
                CAMPAIGN_ID, SHIPMENT_ID);
    }

    private Delivery prepareValidDelivery() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(25L);

        Parcel parcel = new Parcel();
        ParcelBox box = new ParcelBox();
        box.setFulfilmentId("31-1");
        box.setId(31L);
        parcel.setBoxes(Collections.singletonList(box));
        delivery.setParcels(Collections.singletonList(parcel));
        return delivery;
    }

    private Delivery prepareInvalidDelivery() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(null);
        return delivery;
    }

    private Delivery prepareDeliveryWithoutBoxes() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(25L);

        Parcel parcel = new Parcel();
        parcel.setBoxes(Collections.emptyList());
        delivery.setParcels(Collections.singletonList(parcel));
        return delivery;
    }

    private Delivery prepareDeliveryWithoutFFId() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(25L);

        Parcel parcel = new Parcel();
        ParcelBox box1 = new ParcelBox();
        box1.setId(31L);
        ParcelBox box2 = new ParcelBox();
        box2.setId(32L);
        parcel.setBoxes(List.of(box1, box2));
        delivery.setParcels(Collections.singletonList(parcel));
        return delivery;
    }

}

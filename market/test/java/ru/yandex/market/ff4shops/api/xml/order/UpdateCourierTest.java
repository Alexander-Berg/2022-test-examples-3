package ru.yandex.market.ff4shops.api.xml.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class UpdateCourierTest extends FunctionalTest {
    private static final long ORDER_ID = 10380891L;
    private static final long ORDER_ID_2 = 33010139L;
    private static final long YANDEX_ID = 43079868L;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private PushApi pushApi;

    @Autowired
    private TestableClock clock;

    @Test
    @DisplayName("Заказ не существует")
    void orderDoesNotExist() {
        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
                .thenThrow(new OrderNotFoundException(YANDEX_ID));
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_error.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Запрос без курьера")
    void courierNull() {
        mockCheckouterApiGetOrder(getOrder(ORDER_ID));
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_null.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_null_error.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Запрос без кодов")
    void codesNull() {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_codes_null.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Запрос без информации о ФИО курьера (persons = null)")
    @DbUnitDataSet(after = "csv/UpdateCourierTest.namesNull.after.csv")
    void personsNull() {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_persons_null.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Запрос без информации о машине")
    @DbUnitDataSet(after = "csv/UpdateCourierTest.carNull.after.csv")
    void carNull() {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_car_null.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Запрос без информации о телефоне")
    @DbUnitDataSet(after = "csv/UpdateCourierTest.phoneNull.after.csv")
    void phoneNull(String requestFile) {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                requestFile,
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    public static Stream<Arguments> phoneNull() {
        return Stream.of(
                Arguments.of("ru/yandex/market/ff4shops/api/xml/order/request/update_courier_phone_null.xml"),
                Arguments.of("ru/yandex/market/ff4shops/api/xml/order/request/update_courier_phone_empty.xml")
        );
    }

    @Test
    @DisplayName("Создание новой сущности курьера заказа")
    @DbUnitDataSet(after = "csv/UpdateCourierTest.createNewEntity.after.csv")
    void createNewEntity() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Обновление существующей сущности курьера заказа-1")
    @DbUnitDataSet(
            before = "csv/UpdateCourierTest.updateExistingEntity-1.before.csv",
            after = "csv/UpdateCourierTest.updateExistingEntity-1.after.csv"
    )
    void updateExistingEntity1() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Обновление существующей сущности курьера заказа (одинаковый код ПЭП)")
    @DbUnitDataSet(
            before = "csv/UpdateCourierTest.updateExistingEntity-1.before.csv",
            after = "csv/UpdateCourierTest.updateExistingEntity-2.after.csv"
    )
    void updateExistingEntity2() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_7.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Обновление существующей сущности курьера заказа (кода ПЭП не было - появился)")
    @DbUnitDataSet(
            before = "csv/UpdateCourierTest.updateExistingEntity-3.before.csv",
            after = "csv/UpdateCourierTest.updateExistingEntity-3.after.csv"
    )
    void updateExistingEntity3() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_7.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Обновление существующей сущности курьера заказа (без кодов ПЭП)")
    @DbUnitDataSet(
            before = "csv/UpdateCourierTest.updateExistingEntity-4.before.csv",
            after = "csv/UpdateCourierTest.updateExistingEntity-4.after.csv"
    )
    void updateExistingEntity4() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_8.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
    }

    @Test
    @DisplayName("Обновление существующей сущности курьера заказа (код ПЭП повторно обновляется) - отправляем статус курьера, " +
                 "флаг обновления ПЭП - true")
    @DbUnitDataSet(
            before = "csv/UpdateCourierTest.updateExistingEntity-5.before.csv",
            after = "csv/UpdateCourierTest.updateExistingEntity-5.after.csv"
    )
    void updateExistingEntity5() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_7.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyCheckouterApiGetOrder(YANDEX_ID);
        verifyPushApiOrderStatus();
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при получении кода ЭАПП (без автомобиля)")
    void orderStatusSendingWithCertificateCode() {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_car_null.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyPushApiOrderStatus();
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при получении номера автомобиля (без кода ЭАПП)")
    void orderStatusSendingWithCarNumber() {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_codes_null_3.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyPushApiOrderStatus();
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при получении номера автомобиля и кода ЭАПП")
    void orderStatusSendingWithCertificateCodeAndCarNumber() {
        mockPushApiAndCheckouterApi(ORDER_ID);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_2.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        verifyPushApiOrderStatus();
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при изменении ФИО курьера - 1")
    @DbUnitDataSet(before = "csv/UpdateCourierTest.updateFio-1.before.csv",
            after = "csv/UpdateCourierTest.updateFio.after.csv")
    void orderStatusSendingWithNonEmptyFioCourierChanged() {
        mockPushApiAndCheckouterApi(ORDER_ID_2);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_3.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        PushApiOrder actualOrder = verifyPushApiOrderStatus();
        assertCourier(actualOrder,
                "Иванов Иван Иванович",
                "+79120995513",
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при изменении ФИО курьера - 2")
    @DbUnitDataSet(before = "csv/UpdateCourierTest.updateFio-2.before.csv",
            after = "csv/UpdateCourierTest.updateFio.after.csv")
    void orderStatusSendingWithEmptyFioCourierChanged() {
        mockPushApiAndCheckouterApi(ORDER_ID_2);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_3.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        PushApiOrder actualOrder = verifyPushApiOrderStatus();
        assertCourier(actualOrder,
                "Иванов Иван Иванович",
                "+79120995513",
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при изменении описания автомобиля")
    @DbUnitDataSet(before = "csv/UpdateCourierTest.carDescAdd.before.csv",
            after = "csv/UpdateCourierTest.carDescAdd.after.csv")
    void orderStatusSendingWithCarDescAdd() {
        mockPushApiAndCheckouterApi(ORDER_ID_2);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_4.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        PushApiOrder actualOrder = verifyPushApiOrderStatus();
        assertCourier(actualOrder,
                "Петухов Родион Димитриевич",
                "+79120995513",
                null,
                "К097КЕ77",
                "Dacia Logan желтый",
                null);
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при добавлении телефона курьера")
    @DbUnitDataSet(before = "csv/UpdateCourierTest.phoneAdd.before.csv",
            after = "csv/UpdateCourierTest.phoneAdd.after.csv")
    void orderStatusSendingWithPhoneAdd() {
        mockPushApiAndCheckouterApi(ORDER_ID_2);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_5.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        PushApiOrder actualOrder = verifyPushApiOrderStatus();
        assertCourier(actualOrder,
                "Петухов Родион Димитриевич",
                "+79120995514",
                "123",
                null,
                null,
                null);
    }

    @Test
    @DisplayName("Проверка отправки статуса заказа при изменении добавочного номера курьера")
    @DbUnitDataSet(before = "csv/UpdateCourierTest.phoneAdd.after.csv",
            after = "csv/UpdateCourierTest.phoneExtChange.after.csv")
    void orderStatusSendingWithPhoneExtChange() {
        mockPushApiAndCheckouterApi(ORDER_ID_2);
        assertUpdateCourier(
                "ru/yandex/market/ff4shops/api/xml/order/request/update_courier_6.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/update_courier_success.xml"
        );
        PushApiOrder actualOrder = verifyPushApiOrderStatus();
        assertCourier(actualOrder,
                "Петухов Родион Димитриевич",
                "+79120995514",
                "456",
                null,
                null,
                null);
    }

    private void mockPushApiOrderStatus(Order order) {
        doNothing().when(pushApi).orderStatus(eq(1940L), any(), eq(order.isFake()),
                eq(order.getContext()), eq(ApiSettings.STUB), isNull());
    }

    private void mockCheckouterApiGetOrder(Order order) {
        when(checkouterAPI.getOrder(anyLong(), eq(ClientRole.SYSTEM), isNull())).thenReturn(order);
    }

    private void mockPushApiAndCheckouterApi(long orderId) {
        Order order = getOrder(orderId);
        mockPushApiOrderStatus(order);
        mockCheckouterApiGetOrder(order);
    }

    private void verifyCheckouterApiGetOrder(long orderId) {
        verify(checkouterAPI).getOrder(eq(orderId), eq(ClientRole.SYSTEM), isNull());
    }

    private PushApiOrder verifyPushApiOrderStatus() {
        ArgumentCaptor<PushApiOrder> captor = ArgumentCaptor.forClass(PushApiOrder.class);
        verify(pushApi).orderStatus(ArgumentMatchers.anyLong(), captor.capture(), anyBoolean(), any(), any(), isNull());
        return captor.getValue();

    }

    private void assertCourier(PushApiOrder actualOrder,
                               String expectedFullName,
                               String expectedPhone,
                               String expectedPhoneExtension,
                               String expectedVehicleNumber,
                               String expectedVehicleDescription,
                               String expectedElectronicAcceptCode) {
        Courier actualCourier = actualOrder.getDelivery().getCourier();
        Assertions.assertEquals(actualCourier.getFullName(), expectedFullName);
        Assertions.assertEquals(actualCourier.getPhone(), expectedPhone);
        Assertions.assertEquals(actualCourier.getPhoneExtension(), expectedPhoneExtension);
        Assertions.assertEquals(actualCourier.getVehicleNumber(), expectedVehicleNumber);
        Assertions.assertEquals(actualCourier.getVehicleDescription(), expectedVehicleDescription);
        Assertions.assertEquals(actualOrder.getElectronicAcceptanceCertificateCode(), expectedElectronicAcceptCode);
    }

    private Order getOrder(long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setShopId(1940L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(OrderSubstatus.COURIER_FOUND);
        order.setItems(new ArrayList<>());
        order.setContext(Context.SANDBOX);
        order.setFake(false);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        order.setCurrency(Currency.RUR);
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setPrice(new BigDecimal("3.0"));
        delivery.setServiceName("testService");
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setRegionId(51L);
        order.setDelivery(delivery);
        Address address = new AddressImpl();
        delivery.setAddressForJson(address);
        OrderItem item = new OrderItem();
        item.setId(185L);
        item.setOrderId(ORDER_ID);
        item.setSupplierId(1950L);
        item.setSupplierCurrency(Currency.RUR);
        item.setFeedId(4593L);
        item.setOfferId("3910");
        item.setOfferName("testOffer");
        item.setCount(2);
        item.setPrice(new BigDecimal("4.0"));
        order.setItems(List.of(item));
        return order;
    }

    private void assertUpdateCourier(String requestPath, String responsePath) {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.postForXml(
                urlBuilder.url("orders", "updateCourier"),
                extractFileContent(requestPath)
        );
        assertXmlEquals(extractFileContent(responsePath), responseEntity.getBody());
    }
}

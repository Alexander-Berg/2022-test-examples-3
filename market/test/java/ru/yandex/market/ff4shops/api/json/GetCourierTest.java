package ru.yandex.market.ff4shops.api.json;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.delivery.courier.CourierService;
import ru.yandex.market.ff4shops.delivery.courier.DeliveryClient;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;
import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetCourierResponse;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff4shops.delivery.LgwSolomonMetrics.LGW_FOLLBACK_ACCEPT_CODE;

@DbUnitDataSet(before = "GetCourierTest.before.csv")
class GetCourierTest extends AbstractJsonControllerFunctionalTest {

    @Autowired
    private CheckouterAPI checkouterAPI;
    @Autowired
    private LomClient lomClient;
    @Autowired
    private DeliveryClient deliveryClient;
    @Autowired
    private CourierService courierService;

    @ParameterizedTest
    @DisplayName("Информация о курьере по заказу не найдена")
    @MethodSource
    void courierNotFound(long orderId, String responsePath) {
        HttpClientErrorException.NotFound exception = catchThrowableOfType(
                () -> getCourier(orderId),
                HttpClientErrorException.NotFound.class
        );
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertResponseBody(
                exception.getResponseBodyAsString(),
                responsePath
        );
    }

    private static Stream<Arguments> courierNotFound() {
        return Stream.of(
                Arguments.of(10380892L, "ru/yandex/market/ff4shops/api/json/getCourier.error.json"),
                Arguments.of(10380893L,
                        "ru/yandex/market/ff4shops/api/json/getCourierWithAcceptCodeProperty.error.json")
        );
    }


    @Test
    @DisplayName("Получение информация о курьере")
    void getCourier() throws JsonProcessingException {
        ResponseEntity<String> response = getCourier(10380891);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(response.getBody(), "ru/yandex/market/ff4shops/api/json/getCourier.success.json");
    }

    @ParameterizedTest
    @DisplayName("Получение информация о курьере со статусом кода передачи")
    @MethodSource
    void courierWithAcceptCodeStatus(long orderId, String responsePath,
                                     OrderSubstatus substatus) throws JsonProcessingException {
        Order order = mock(Order.class);
        WaybillSegmentDto ws = getWaybillSegmentDto();

        when(checkouterAPI.getOrder(orderId, ClientRole.SYSTEM, null))
                .thenReturn(order);
        when(order.getSubstatus()).thenReturn(substatus);
        when(lomClient.searchOrders(any(),eq(Pageable.unpaged()))).thenReturn(createPageOrderDto(ws));
        ResponseEntity<String> responseEmpty = getCourierWithAcceptCodeStatus(orderId, false);
        assertThat(responseEmpty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(responseEmpty.getBody(), responsePath);
    }

    @Test
    @DbUnitDataSet(after = "GetCourierTest-1.after.csv")
    @DisplayName("Получение информация о курьере с фоллбэком - обновление записи в БД (первый код ПЭП)")
    void forceGetAcceptCodeUpdate1() throws JsonProcessingException {
        long orderId = 10380894L;
        String responsePath = "ru/yandex/market/ff4shops/api/json/getForceCourierWithStatusAcceptCode.ok_status.json";
        OrderSubstatus substatus = OrderSubstatus.COURIER_SEARCH;
        Order order = mock(Order.class);
        courierService.setForceFullAccept(true);
        when(checkouterAPI.getOrder(eq(orderId), eq(ClientRole.SYSTEM), isNull()))
                .thenReturn(order);
        when(lomClient.searchOrders(any(), eq(Pageable.unpaged())))
                .thenReturn(createPageOrderDto(getWaybillSegmentDto()));
        when(deliveryClient.getAcceptCodeFromTaxi(anyList()))
                .thenReturn("123QWEasd");
        when(order.getSubstatus()).thenReturn(substatus);

        ResponseEntity<String> responseEmpty = getCourierWithAcceptCodeStatus(orderId, false);

        assertThat(responseEmpty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(responseEmpty.getBody(), responsePath);
        courierService.setForceFullAccept(false);
    }

    @Test
    @DbUnitDataSet(before = "GetCourierTest-1.before.csv", after = "GetCourierTest-2.after.csv")
    @DisplayName("Получение информация о курьере с фоллбэком - обновление записи в БД (второй код ПЭП)")
    void forceGetAcceptCodeUpdate2() throws JsonProcessingException {
        long orderId = 10380895L;
        String responsePath = "ru/yandex/market/ff4shops/api/json/getForceCourierWithStatusAcceptCode2.ok_status.json";
        OrderSubstatus substatus = OrderSubstatus.COURIER_FOUND;
        Order order = mock(Order.class);
        courierService.setForceFullAccept(true);
        when(checkouterAPI.getOrder(eq(orderId), eq(ClientRole.SYSTEM), isNull()))
                .thenReturn(order);
        when(lomClient.searchOrders(any(), eq(Pageable.unpaged())))
                .thenReturn(createPageOrderDto(getWaybillSegmentDto()));
        when(deliveryClient.getAcceptCodeFromTaxi(anyList()))
                .thenReturn("404040");
        when(order.getSubstatus()).thenReturn(substatus);

        ResponseEntity<String> responseEmpty = getCourierWithAcceptCodeStatus(orderId, true);

        assertThat(responseEmpty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(responseEmpty.getBody(), responsePath);
        courierService.setForceFullAccept(false);
    }

    @Test
    @DbUnitDataSet(before = "GetCourierTest-2.before.csv", after = "GetCourierTest-2.after.csv")
    @DisplayName("Повторное получение информации о курьере с фоллбэком - обновление записи в БД (второй код ПЭП)")
    void forceGetAcceptCodeUpdate3() throws JsonProcessingException {
        long orderId = 10380895L;
        String responsePath = "ru/yandex/market/ff4shops/api/json/getForceCourierWithStatusAcceptCode2.ok_status.json";
        OrderSubstatus substatus = OrderSubstatus.COURIER_FOUND;
        Order order = mock(Order.class);
        courierService.setForceFullAccept(true);
        when(checkouterAPI.getOrder(eq(orderId), eq(ClientRole.SYSTEM), isNull()))
                .thenReturn(order);
        when(lomClient.searchOrders(any(), eq(Pageable.unpaged())))
                .thenReturn(createPageOrderDto(getWaybillSegmentDto()));
        when(deliveryClient.getAcceptCodeFromTaxi(anyList()))
                .thenReturn("404040");
        when(order.getSubstatus()).thenReturn(substatus);

        ResponseEntity<String> responseEmpty = getCourierWithAcceptCodeStatus(orderId, true);

        assertThat(responseEmpty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(responseEmpty.getBody(), responsePath);
        courierService.setForceFullAccept(false);
    }

    @Test
    @DisplayName("показываем код ПЭП если он сменился независимо от статуса заказа")
    void getCourierWithChangedAcceptCode() throws JsonProcessingException {
        long orderId = 10380896L;
        String responsePath = "ru/yandex/market/ff4shops/api/json/getForceCourierWithStatusAcceptCode3.ok_status.json";
        OrderSubstatus substatus = OrderSubstatus.SHIPPED;
        Order order = mock(Order.class);
        when(checkouterAPI.getOrder(orderId, ClientRole.SYSTEM, null))
                .thenReturn(order);
        when(order.getSubstatus()).thenReturn(substatus);
        ResponseEntity<String> responseEmpty = getCourierWithAcceptCodeStatus(orderId, false);
        assertThat(responseEmpty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(responseEmpty.getBody(), responsePath);
    }


    private WaybillSegmentDto getWaybillSegmentDto() {
        return WaybillSegmentDto.builder()
                .partnerId(1006360L)
                .partnerSubtype(PartnerSubtype.TAXI_EXPRESS)
                .externalId("a158f73e-6613-46d3-8eb1-e55500ef0d58")
                .build();
    }

    @Test
    @DbUnitDataSet(after = "GetCourierTest-1.after.csv")
    public void testLgwSensors() throws IOException {
        forceGetAcceptCodeUpdate1();
        var response = ru.yandex.market.common.test.spring.FunctionalTestHelper.get(FF4ShopsUrlBuilder.getSolomonUrl(randomServerPort), String.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        HashMap sensorsMap = new ObjectMapper().readValue(response.getBody(), HashMap.class);
        List<Map<String, Object>> sensors = (List<Map<String, Object>>) sensorsMap.get("sensors");
        Assert.assertTrue(sensorsMap.containsKey("sensors"));
        Assert.assertTrue(sensors.size() > 0);

        Set sensorNames = new TreeSet<String>();
        for (Map<String, Object> sensor: sensors) {
            Assert.assertTrue(sensor.containsKey("kind"));
            Assert.assertTrue(sensor.containsKey("labels"));
            Assert.assertTrue(sensor.containsKey("value"));

            var labels = (Map<String, String>) sensor.get("labels");
            String sensorName = labels.get("sensor");
            sensorNames.add(sensorName);
            Assert.assertTrue(StringUtils.isNotBlank(sensorName));
            Assert.assertTrue(StringUtils.isNotBlank((String) sensor.get("kind")));

            Assert.assertTrue(sensor.get("value") instanceof Number);

            switch (sensorName) {
                case LGW_FOLLBACK_ACCEPT_CODE:
                    Assert.assertTrue((int) sensor.get("value") > 0);
            }
        }
    }

    private PageResult<OrderDto> createPageOrderDto(WaybillSegmentDto waybillSegmentDto) {
        PageResult<OrderDto> result = new PageResult<>();
        OrderDto lomOrder = new OrderDto();
        lomOrder.setWaybill(List.of(waybillSegmentDto, waybillSegmentDto));
        result.setData(Collections.singletonList(lomOrder));
        return result;
    }

    private static Stream<Arguments> courierWithAcceptCodeStatus() {
        return Stream.of(
                Arguments.of(10380891L, "ru/yandex/market/ff4shops/api/json/getCourier_WithStatusCode.success.json",
                        OrderSubstatus.COURIER_FOUND),
                Arguments.of(10380891L,
                        "ru/yandex/market/ff4shops/api/json/getCourier_WithStatusCodeHide.success_1.json",
                        OrderSubstatus.COURIER_SEARCH),
                Arguments.of(10380894L,
                        "ru/yandex/market/ff4shops/api/json/getCourierWithStatusAcceptCode.error_status.json",
                        OrderSubstatus.COURIER_FOUND),
                Arguments.of(10380897L,
                        "ru/yandex/market/ff4shops/api/json/getCourier_WithStatusCodeHide.success_2.json",
                        OrderSubstatus.READY_TO_SHIP)
        );
    }

    private GetCourierResponse createCourierResponse() {

        Person person = new Person("Имя", "Фамилия", "Отчество");
        Courier courier = Courier.builder()
            .setPartnerId(ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setPartnerId("1111")
                .setYandexId("1111")
                .build())
            .setPersons(Collections.singletonList(person))
            .setPhone(Phone.builder("123456789").setAdditional("123").build())
            .setCar(Car.builder("O123OO799").setDescription("descriptoin").build())
            .setUrl("url")
            .build();
        OrderTransferCodes codes = new OrderTransferCodes.OrderTransferCodesBuilder()
                .setOutbound(
                        new OrderTransferCode.OrderTransferCodeBuilder()
                                .setVerification("1234")
                                .setElectronicAcceptanceCertificate("123QWEasd")
                                .build()
                )
                .build();
        return new GetCourierResponse(courier, codes);
    }

    @Nonnull
    private ResponseEntity<String> getCourier(long orderId) throws JsonProcessingException {
        String referenceUrl = FF4ShopsUrlBuilder.getCourierUrl(randomServerPort, orderId);
        return FunctionalTestHelper.getForEntity(
                referenceUrl,
                FunctionalTestHelper.jsonHeaders()
        );
    }

    @Nonnull
    private ResponseEntity<String> getCourierWithAcceptCodeStatus(long orderId, boolean forceGetAcceptCode) {
        String referenceUrl = FF4ShopsUrlBuilder.getCourierWithAcceptCodeStatusUrl(randomServerPort, orderId, forceGetAcceptCode);
        return FunctionalTestHelper.getForEntity(
                referenceUrl,
                FunctionalTestHelper.jsonHeaders()
        );
    }
}

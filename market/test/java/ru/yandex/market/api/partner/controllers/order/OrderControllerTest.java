package ru.yandex.market.api.partner.controllers.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import ru.yandex.market.api.partner.apisupport.ErrorRestModelCode;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.model.DispatchType;
import ru.yandex.market.api.partner.controllers.util.checkouter.CheckouterMockHelper;
import ru.yandex.market.api.partner.request.InvalidRequestException;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.personal_market.PersonalAddress;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveRequestBuilder;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DbUnitDataSet(before = "OrderControllerTest.before.csv")
class OrderControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {
    private static final long DROPSHIP_CAMPAIGN_ID = 1000571241L;
    private static final long DROPSHIP_BY_SELLER_CAMPAIGN_ID = 20001L;
    private static final long ORDER_ID = 123L;
    private static final long DROPSHIP_ORDER_ID = 124L;
    private static final long DROPSHIP_ARCHIVED_ORDER_ID = 130L;
    private static final long DROPSHIP_CLIENT_ID = 666L;
    private static final long DROPSHIP_BY_SELLER_ID = 2001L;

    private static final String DEFAULT_TEST_JSON_REQUEST_AND_RESPONSE =
            //language=json
            "{\n" +
                    "  \"items\": [\n" +
                    "    {\n" +
                    "      \"id\": 774,\n" +
                    "      \"count\": 2\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    @Qualifier("ff4ShopsRestTemplate")
    private RestTemplate ff4ShopsRestTemplate;

    @Value("${ff4shops.client.http.url:}")
    private String ff4shopsUrl;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    @Autowired
    private CommunicationProxyClient communicationProxyClient;

    @Autowired
    private PersonalMarketService personalMarketService;

    private CheckouterMockHelper checkouterMockHelper;

    @BeforeEach
    void setUp() {
        checkouterMockHelper = new CheckouterMockHelper(checkouterRestTemplate, checkouterUrl);
        when(personalMarketService.retrieve(any()))
                .thenReturn(CompletableFuture.completedFuture(PersonalRetrieveResponse.builder().build()));
    }

    static Stream<Arguments> cancelledStatuses() {
        return Stream.of(
                Arguments.of(OrderSubstatus.RESERVATION_EXPIRED),
                Arguments.of(OrderSubstatus.USER_NOT_PAID),
                Arguments.of(OrderSubstatus.USER_UNREACHABLE)
        );
    }

    static Stream<Arguments> substatuses() {
        return Stream.of(
                Arguments.of(OrderSubstatus.READY_TO_SHIP),
                Arguments.of(OrderSubstatus.SHIPPED)
        );
    }

    /**
     * Получение заказа с доставкой мультишипментом в формате json
     * <p>
     * Подготовка
     * 1. Настроить мок чекаутера так чтобы на запрос заказа 123 приходил ответ с телом "expected-order.json",
     * содержащим заказ, доставляемый с несколькими посылками
     * <p>
     * Действие
     * 1. Выполняем запрос GET /campaigns/20001/orders/123.json
     * <p>
     * Проверки
     * 1. в теле ответа у товаров в заказе есть заполненное поле "id"
     * 2. В теле ответа присутствуют все посылки заказа с их треками и наполеннием
     */
    @Test
    void testGetMultiShipmentOrderJson() {
        prepareCheckouterMock("order-with-delivery.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-dbs-order.json",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @DisplayName("Получение заказа с доставкой мультишипментом для дропшипа, JSON")
    @Test
    void testGetMultiShipmentSupplierOrderJson() {
        prepareCheckouterMock("order-with-delivery.json", DROPSHIP_ORDER_ID, DROPSHIP_CLIENT_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order.json",
                DROPSHIP_CAMPAIGN_ID, DROPSHIP_ORDER_ID);
    }

    @DisplayName("Проверяет получение заказа для многоскладового партнера по схеме 1:N в процессе миграции")
    @Test
    @DbUnitDataSet(before = "testGetOrderForMultiFbs.before.csv")
    void testGetOrderForMultiFbs() {
        prepareCheckouterMock("order-with-delivery.json", DROPSHIP_ORDER_ID, List.of(DROPSHIP_CLIENT_ID, 667L));
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order.json",
                DROPSHIP_CAMPAIGN_ID, DROPSHIP_ORDER_ID);
    }

    @ParameterizedTest
    @MethodSource("testGetSupplierOrderWithCourierInfo")
    @DisplayName("Получение заказа с доставкой с инфо о курьере")
    void testGetSupplierOrderWithCourierInfo(String fileToExpectedAnswer,
                                             String ff4ShopsAnswer,
                                             Format format) {
        prepareCheckouterMock("order-with-delivery-features.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        prepareFF4ShopsCourierInfoMock(ff4ShopsAnswer);
        requestOrderAndAssertResponseNoStrictEqual(format, fileToExpectedAnswer,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);

    }

    static Stream<Arguments> testGetSupplierOrderWithCourierInfo() {
        return Stream.of(
                Arguments.of("expected-order-with-courier-empty.json", "ff4shops/order_courier_empty.json",
                        Format.JSON),
                Arguments.of("expected-order-with-courier-info.json", "ff4shops/order_courier_info.json", Format.JSON),
                Arguments.of("expected-order-with-courier-info.xml", "ff4shops/order_courier_info.json", Format.XML)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForFakeSupplierOrderWithDeadline")
    @DisplayName("Получение заказа для самопроверки с доставкой с дедлайном, JSON")
    void testGetFakeSupplierOrderWithDeadline(String expectedCheckouterAnswer,
                                              String ff4ShopsAnswer,
                                              String expectedControllerAnswer,
                                              boolean isFakeOrder) {
        prepareCheckouterMock(expectedCheckouterAnswer, ORDER_ID, DROPSHIP_BY_SELLER_ID);
        if (!isFakeOrder) {
            prepareFF4ShopsCourierInfoMock(ff4ShopsAnswer);
        }
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, expectedControllerAnswer,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    static Stream<Arguments> getArgumentsForFakeSupplierOrderWithDeadline() {
        return Stream.of(
                Arguments.of("fake-fbs-order-with-delivery.json", null, "expected-fake-order-with-courier-info.json",
                        true),
                Arguments.of("order-with-delivery-features.json", "ff4shops/order_courier_info.json", "expected-order" +
                        "-with-courier-info.json", false),
                Arguments.of("fake-fby-order-with-delivery.json", null, "expected-fake-order-no-deadline.json", true)
        );
    }

    /**
     * Получение заказа с доставкой мультишипментом в формате xml
     * <p>
     * Подготовка
     * 1. Настроить мок чекаутера так чтобы на запрос заказа 123 приходил ответ с телом "expected-order.json",
     * содержащим заказ, доставляемый с несколькими посылками
     * <p>
     * Действие
     * 1. Выполняем запрос GET /campaigns/20001/orders/123.xml
     * <p>
     * Проверки
     * 1. в теле ответа у товаров в заказе есть заполненное поле "id"
     * 2. В теле ответа присутствуют все посылки заказа с их треками и наполеннием
     */
    @Test
    void testGetMultiShipmentOrderXml() {
        prepareCheckouterMock("order-with-delivery.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.XML, "expected-dbs-order.xml",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @DisplayName("Получение заказа с доставкой мультишипментом для дропшипа, XML")
    @Test
    void testSupplierGetMultiShipmentOrderXml() {
        prepareCheckouterMock("order-with-delivery.json", DROPSHIP_ORDER_ID, DROPSHIP_CLIENT_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.XML, "expected-order.xml",
                DROPSHIP_CAMPAIGN_ID, DROPSHIP_ORDER_ID);
    }

    /**
     * Подготовка
     * 1. Настроить мок чекаутера так чтобы на запрос заказа 123 приходил ответ с телом "order-without-shipments.json",
     * содержащим заказ без посылок
     * <p>
     * Действие
     * 1. Выполняем запрос GET /campaigns/20001/orders/123.json
     * <p>
     * Проверки
     * 1. в теле ответа у товаров в заказе есть заполненное поле "id"
     * 2. В теле ответа присутствуют доставка без посылок
     */
    @DisplayName("Получение заказа без посылок в формате json")
    @Test
    void testGetOrderWithoutShipmentsJson() {
        prepareCheckouterMock("order-without-shipments.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-dbs-order-without-shipments.json",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @DisplayName("Получение заказа без посылок в формате json")
    @Test
    void testGetOrderWithRealDeliveryDateJson() {
        prepareCheckouterMock("order-with-real-delivery-date-2.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order-with-real-delivery-date-2.json",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @DisplayName("Получение заказа с признаком маркетного ПВЗ (isMarketBranded)")
    @Test
    void testGetOrderWithIsMarketBrandedJson() {
        prepareCheckouterMock("order-with-is-market-branded.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order-with-is-market-branded.json",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @DisplayName("Получение заказа со сроком хранения в ПВЗ")
    @Test
    void testGetOrderWithOutletStorageLimitDateJson() {
        prepareCheckouterMock("order-with-outlet-storage-limit-date.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);

        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order-with-outlet-storage-limit-date.json",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @DisplayName("Получение заказа без посылок для дропшипа")
    @Test
    void testDropshipGetOrderWithoutShipmentsJson() {
        prepareCheckouterMock("order-without-shipments.json", DROPSHIP_ORDER_ID, DROPSHIP_CLIENT_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order-without-shipments.json",
                DROPSHIP_CAMPAIGN_ID, DROPSHIP_ORDER_ID);
    }

    @DisplayName("Получение заказа без посылок для поставщика")
    @Test
    void testSupplierGetOrderWithoutShipmentsJson() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "", 10667L, 125L, 67282296L))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    @DisplayName("Получение заказа с признаком неуточнённой даты доставки (estimated)")
    @Test
    void testGetOrderWithEstimatedJson() {
        prepareCheckouterMock("order-with-estimated.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.JSON, "expected-order-with-estimated.json",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    /**
     * Получение заказа без посылок в формате xml
     * <p>
     * Подготовка
     * 1. Настроить мок чекаутера так чтобы на запрос заказа 123 приходил ответ с телом "order-without-shipments.json",
     * содержащим заказ без посылок
     * <p>
     * Действие
     * 1. Выполняем запрос GET /campaigns/20001/orders/123.xml
     * <p>
     * Проверки
     * 1. в теле ответа у товаров в заказе есть заполненное поле "id"
     * 2. В теле ответа присутствуют доставка без посылок
     */
    @Test
    void testGetOrderWithoutShipmentsXml() {
        prepareCheckouterMock("order-without-shipments.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.XML, "expected-dbs-order-without-shipments.xml",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    /**
     * В случае если у посылки нет товаров или треков соответствующие поля не попадают в выдачу в формате json
     * <p>
     * Подготовка
     * 1. Настроить мок чекаутера так чтобы на запрос заказа 123 приходил ответ с телом "order-with-empty-shipment
     * .json",
     * содержащим заказ c посылкой с пустыми массивами товаров и треков
     * <p>
     * Действие
     * 1. Выполняем запрос GET /campaigns/20001/orders/123.json
     * <p>
     * Проверки
     * 1. В теле ответа присутствует посылка
     * 2. У посылки отсутсвуют поля items и tracks
     */
    @Test
    void testNoItemsAndTracksInJsonResponseIfCollectionsAreEmpty() {
        prepareCheckouterMock("order-with-empty-shipment.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        String url = String.format("%s/campaigns/%s/orders/%s.%s",
                urlBasePrefix, DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID, Format.JSON.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        Format.JSON.assertResult(resourceAsString("order-with-empty-shipment-no-tracks-no-items.json"),
                response.getBody());
    }

    /**
     * В случае если у посылки нет товаров или треков соответствующие поля не попадают в выдачу в формате xml
     * <p>
     * Подготовка
     * 1. Настроить мок чекаутера так чтобы на запрос заказа 123 приходил ответ с телом "order-with-empty-shipment
     * .json",
     * содержащим заказ c посылкой с пустыми массивами товаров и треков
     * <p>
     * Действие
     * 1. Выполняем запрос GET /campaigns/20001/orders/123.xml
     * <p>
     * Проверки
     * 1. В теле ответа присутствует посылка
     * 2. У посылки отсутсвуют элементы items и tracks
     */
    @Test
    void testNoItemsAndTracksInXmlResponseIfCollectionsAreEmpty() {
        prepareCheckouterMock("order-with-empty-shipment.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.XML, "expected-order-with-empty-shipment.xml",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @Test
    void testGetOrderWithEstimatedXml() {
        prepareCheckouterMock("order-with-estimated.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        requestOrderAndAssertResponseNoStrictEqual(Format.XML, "expected-order-with-estimated.xml",
                DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    /**
     * Пытаемся получить ордеры за один день, проверяем что в запросе таймстемпы не одинаковы
     * и совпадают с orders-different-timestamps-body.json
     */
    @Test
    void testEqualFromAndToDateOrdersRequest() {
        LocalDateTime startOfCurrentDate = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfCurrentDate.plusDays(1);

        checkouterMockHelper.mockGetOrders(DROPSHIP_BY_SELLER_ID)
                .andExpect(json(String.format("{\n" +
                                "  \"fromDate\": %d,\n" +
                                "  \"toDate\": %d\n" +
                                "}",
                        startOfCurrentDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        startOfNextDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        ))
                )
                // ответ не важен поэтому возвращаем простой валидный ответ без заказов
                .andRespond(withSuccess("{\"orders\": []}", MediaType.APPLICATION_JSON));


        String currentDay = startOfCurrentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        String url = String.format("%s/campaigns/%s/orders.json?fromDate=%s&toDate=%s",
                urlBasePrefix, DROPSHIP_BY_SELLER_CAMPAIGN_ID, currentDay, currentDay);
        assertStatusOk(doGet(url));
    }

    /**
     * Тест проверяет корректный вывод поля {@code deliveryServiceId} в формате JSON.
     */
    @Test
    void testDeliveryServiceIdForJson() {
        prepareCheckouterMock("checkouter/get_order_with_fulfilment_warehouse.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        preparePersonalMarketMock();
        requestOrderAndAssertResponseNoStrictEqual(
                Format.JSON, "asserts/get_order_with_fulfilment_warehouse.json", DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                ORDER_ID);
    }

    /**
     * Тест проверяет корректный вывод поля {@code subsidyTotal} в формате JSON.
     */
    @Test
    void testSubsidyTotalFieldCalculationForJson() {
        prepareCheckouterMock("checkouter/get_order_with_promo.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        preparePersonalMarketMock();
        requestOrderAndAssertResponseNoStrictEqual(
                Format.JSON, "asserts/expected-dbs-order-with-subsidy-total-field.json", DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                ORDER_ID);
    }

    /**
     * Тест проверяет корректный вывод атрибута {@code subsidy-total} в формате XML.
     */
    @Test
    void testSubsidyTotalFieldCalculationForXML() {
        prepareCheckouterMock("checkouter/get_order_with_promo.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        preparePersonalMarketMock();
        requestOrderAndAssertResponseNoStrictEqual(
                Format.XML, "asserts/expected-order-with-subsidy-total-field.xml", DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                ORDER_ID);
    }

    /**
     * Тест проверяет, что из чекаутера забираются заказы всех цветов. Конкретный ответ не важен.
     * Главное - правильная передача фильтра по цветам заказов.
     */
    @Test
    void testGetOrdersAllColors() {
        // language=json
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";
        checkouterMockHelper.mockGetOrders(DROPSHIP_CLIENT_ID)
                .andExpect(jsonPath("$.rgbs", containsInAnyOrder(
                        Color.BLUE.name(),
                        Color.WHITE.name(),
                        Color.TURBO.name())))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));
        doGet(new UriTemplate("{base}/v2/campaigns/{campaignId}/orders")
                .expand(urlBasePrefix, DROPSHIP_CAMPAIGN_ID).toString());
    }

    @Test
    void testGetOrdersByStatus() {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";
        OrderStatus status = OrderStatus.RESERVED;

        checkouterMockHelper.mockGetOrders(DROPSHIP_CLIENT_ID)
                .andExpect(jsonPath("$.statuses", contains(status.name())))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?status=%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, status.name());

        assertStatusOk(doGet(url));
    }

    @Test
    void testGetOrdersByWrongStatus() {
        String wrongStatus = "WRONG_STATUS";
        String expectedResponse = resourceAsString("wrongStatusResponse.json");

        String url = String.format("%s/campaigns/%s/orders.json?status=%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, wrongStatus);

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> doGet(url))
                .satisfies(ex -> JsonTestUtil.assertEquals(expectedResponse, ex.getResponseBodyAsString()));
    }

    @Test
    void testGetOrdersBySubstatus() {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";
        OrderSubstatus substatus = OrderSubstatus.RESERVATION_EXPIRED;

        checkouterMockHelper.mockGetOrders(DROPSHIP_CLIENT_ID)
                .andExpect(jsonPath("$.substatuses", contains(substatus.name())))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?substatus=%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, substatus.name());

        assertStatusOk(doGet(url));

        // проверяем, что в OrderSearchRequest.notSubstatuses передаём OrderSubstatus.ANTIFRAUD
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        verify(checkouterClient).getOrders(any(), captor.capture());
        assertEquals(EnumSet.of(OrderSubstatus.ANTIFRAUD), captor.getValue().notSubstatuses);
    }

    @Test
    void testGetOrdersByWrongSubstatus() {
        String wrongSubstatus = "WRONG_SUBSTATUS";
        String expectedResponse = resourceAsString("wrongSubstatusResponse.json");

        String url = String.format("%s/campaigns/%s/orders.json?substatus=%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, wrongSubstatus);

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> doGet(url))
                .satisfies(ex -> JsonTestUtil.assertEquals(expectedResponse, ex.getResponseBodyAsString()));
    }

    @ParameterizedTest
    @DisplayName("Проверяем, что переданный фильтр по типу отгрузки никак не влияет на фильтрацию для не-ДБС заказов")
    @EnumSource(DispatchType.class)
    void testGetOrdersByDispatchTypeDropship(DispatchType dispatchType) {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";

        checkouterMockHelper.mockGetOrders(DROPSHIP_CLIENT_ID)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?dispatchType=%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, dispatchType.name());

        assertStatusOk(doGet(url));

        // проверяем, что в OrderSearchRequest.marketBranded и OrderSearchRequest.deliveryPartnerTypes
        // не заполняются для поиска по не-ДБС заказам
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        verify(checkouterClient).getOrders(any(), captor.capture());
        Assertions.assertNull(captor.getValue().marketBranded);
        Assertions.assertNull(captor.getValue().deliveryPartnerTypes);
    }

    @ParameterizedTest(name = "{0} => marketBranded={1}, deliveryPartnerType={2}")
    @DisplayName("Проверяем формирование фильтра в зависимости от типа отгрузки")
    @CsvSource({
            "UNKNOWN,,,",
            "BUYER,,DELIVERY,SHOP",
            "MARKET_BRANDED_OUTLET,true,PICKUP,SHOP"
    })
    void testGetOrdersByDispatchTypeDropshipBySeller(
            DispatchType dispatchType,
            Boolean marketBranded,
            DeliveryType deliveryType,
            DeliveryPartnerType deliveryPartnerType
    ) {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";

        checkouterMockHelper.mockGetOrders(DROPSHIP_BY_SELLER_ID)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?dispatchType=%s",
                urlBasePrefix, DROPSHIP_BY_SELLER_CAMPAIGN_ID, dispatchType.name());

        assertStatusOk(doGet(url));

        // проверяем, что в OrderSearchRequest.marketBranded и OrderSearchRequest.deliveryPartnerTypes
        // не заполняются для поиска по не-ДБС заказам
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        verify(checkouterClient).getOrders(any(), captor.capture());
        Assertions.assertEquals(marketBranded, captor.getValue().marketBranded);

        if (deliveryType == null) {
            assertThat(captor.getValue().deliveryTypes).isNullOrEmpty();
        } else {
            assertThat(captor.getValue().deliveryTypes).contains(deliveryType);
        }

        if (deliveryPartnerType == null) {
            assertThat(captor.getValue().deliveryPartnerTypes).isNullOrEmpty();
        } else {
            assertThat(captor.getValue().deliveryPartnerTypes).contains(deliveryPartnerType);
        }
    }

    @Test
    void testGetOrdersWithOpenedCancellation() {
        String meaninglessResponse = "{\"pager\":{}, \"orders\":[]}";

        checkouterMockHelper.mockGetOrders(DROPSHIP_CLIENT_ID)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(meaninglessResponse));

        String url = String.format("%s/campaigns/%s/orders.json?onlyWaitingForCancellationApprove=true",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID);

        assertStatusOk(doGet(url));

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        verify(checkouterClient).getOrders(any(), captor.capture());
        assertEquals(EnumSet.of(ChangeRequestType.CANCELLATION), captor.getValue().havingChangeRequestTypes);
        assertEquals(EnumSet.of(ChangeRequestStatus.NEW, ChangeRequestStatus.PROCESSING),
                captor.getValue().havingChangeRequestStatuses);
        assertEquals(EnumSet.of(ClientRole.USER), captor.getValue().havingChangeRequestAuthorRoles);
    }

    @Test
    void testEqualsShipmentDateOrdersRequest() {
        LocalDateTime startOfCurrentDate = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfCurrentDate.plusDays(1);
        String currentDay = startOfCurrentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String nextDay = startOfNextDay.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        checkouterMockHelper.mockGetOrders(DROPSHIP_BY_SELLER_ID)
                .andExpect(json(String.format("{\n" +
                                "  \"shipmentDateBySupplierFrom\": %s,\n" +
                                "  \"shipmentDateBySupplierTo\": %s\n" +
                                "}",
                        currentDay,
                        nextDay
                        ))
                )
                // ответ не важен поэтому возвращаем простой валидный ответ без заказов
                .andRespond(withSuccess("{\"orders\": []}", MediaType.APPLICATION_JSON));

        String urlEqualDate = String.format("%s/campaigns/%s/orders" +
                        ".json?supplierShipmentDateFrom=%s&supplierShipmentDateTo=%s",
                urlBasePrefix, DROPSHIP_BY_SELLER_CAMPAIGN_ID, currentDay, currentDay);
        assertStatusOk(doGet(urlEqualDate));
    }

    @Test
    void testShipmentDateOrdersRequest() {
        LocalDateTime startOfCurrentDate = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfCurrentDate.plusDays(3);
        String currentDay = startOfCurrentDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String nextDay = startOfNextDay.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        checkouterMockHelper.mockGetOrders(DROPSHIP_BY_SELLER_ID)
                .andExpect(json(String.format("{\n" +
                                "  \"shipmentDateBySupplierFrom\": %s,\n" +
                                "  \"shipmentDateBySupplierTo\": %s\n" +
                                "}",
                        currentDay,
                        nextDay
                        ))
                )
                // ответ не важен поэтому возвращаем простой валидный ответ без заказов
                .andRespond(withSuccess("{\"orders\": []}", MediaType.APPLICATION_JSON));
        String url = String.format("%s/campaigns/%s/orders" +
                        ".json?supplierShipmentDateFrom=%s&supplierShipmentDateTo=%s",
                urlBasePrefix, DROPSHIP_BY_SELLER_CAMPAIGN_ID, currentDay, nextDay);
        assertStatusOk(doGet(url));
    }

    @ParameterizedTest
    @MethodSource("substatuses")
    void putProcessingSubstatus(OrderSubstatus substatus) {
        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer.expect(requestTo("http://localhost/orders/100/status?clientRole=SHOP" +
                "&clientId=" + DROPSHIP_BY_SELLER_ID +
                "&shopId=" +
                "&status=PROCESSING" +
                "&substatus=" + substatus.name()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        resourceAsString("order-with-empty-shipment-and-statuses.json")
                                .replace("${STATUS}", substatus.getStatus().name())
                                .replace("${SUBSTATUS}", substatus.name()),
                        MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                HttpMethod.PUT,
                Format.JSON,
                //language=json
                "{\"order\": {\"status\": \"PROCESSING\", \"substatus\": \"${STATUS}\"}}"
                        .replace(//language=none
                                "${STATUS}", substatus.name())
        ).getBody();

        // Проверяем в ответе статусы
        assertThat(JsonPath.<String>read(actual, "$.order.status")).isEqualTo(substatus.getStatus().name());
        assertThat(JsonPath.<String>read(actual, "$.order.substatus")).isEqualTo(substatus.name());
        assertThatExceptionOfType(PathNotFoundException.class)
                .isThrownBy(() -> JsonPath.read(actual, "$.order.cancelRequested"));

        // Проверяем в ответе КИЗы
        assertThat(JsonPath.<Collection<?>>read(actual, "$.order.items")).hasSize(1);
        assertThat(JsonPath.<Collection<?>>read(actual, "$.order.items[0].instances")).hasSize(1);
        assertThat(JsonPath.<String>read(actual, "$.order.items[0].instances[0].cisFull")).isEqualTo("testCis1Full");
    }

    @Test
    void testNotGoingToTelephony() {
        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer.expect(requestTo("http://localhost/orders/123/status?clientRole=SHOP" +
                "&clientId=" + DROPSHIP_BY_SELLER_ID +
                "&shopId=" +
                "&status=PROCESSING" +
                "&substatus=READY_TO_SHIP"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        resourceAsString("order-without-buyer-phone.json"),
                        MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/123/status",
                HttpMethod.PUT,
                Format.JSON,
                "{\"order\": {\"status\": \"PROCESSING\", \"substatus\": \"READY_TO_SHIP\"}}"
        ).getBody();

        assertThat(JsonPath.<String>read(actual, "$.order.status")).isEqualTo("PROCESSING");

        verify(communicationProxyClient, never()).getRedirect(any());
    }

    @Test
    void putRealDeliveryDateJson() {
        OrderSubstatus substatus = OrderSubstatus.DELIVERED_USER_RECEIVED;
        String realDeliveryDate = "01-01-2017";

        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer.expect(requestTo(
                "http://localhost/orders/100/status/delivered-with-real-delivery-date" +
                        "?clientRole=SHOP" +
                        "&clientId=" + DROPSHIP_BY_SELLER_ID +
                        "&shopId=" +
                        "&status=" + substatus.getStatus().name() +
                        "&substatus=" + substatus.name()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        resourceAsString("order-with-real-delivery-date.json")
                                .replace("${STATUS}", substatus.getStatus().name())
                                .replace("${SUBSTATUS}", substatus.name())
                                .replace("${REAL_DELIVERY_DATE}", realDeliveryDate),
                        MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                HttpMethod.PUT,
                Format.JSON,
                //language=json
                ("{\"order\": {" +
                        "\"status\": \"${STATUS}\", " +
                        "\"substatus\": \"${SUBSTATUS}\", " +
                        "\"delivery\": { " +
                        "\"dates\": { " +
                        "\"realDeliveryDate\": \"" + realDeliveryDate + "\" " +
                        "}}}}")
                        //language=none
                        .replace("${STATUS}", substatus.getStatus().name())
                        .replace("${SUBSTATUS}", substatus.name())
                        .replace("${REAL_DELIVERY_DATE}", realDeliveryDate)
        ).getBody();

        assertThat(JsonPath.<String>read(actual, "$.order.delivery.dates.realDeliveryDate"))
                .isEqualTo(realDeliveryDate);
    }

    @Test
    void putRealDeliveryDateXml() {
        OrderSubstatus substatus = OrderSubstatus.DELIVERED_USER_RECEIVED;
        String realDeliveryDate = "01-01-2017";

        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer.expect(requestTo(
                "http://localhost/orders/100/status/delivered-with-real-delivery-date" +
                        "?clientRole=SHOP" +
                        "&clientId=" + DROPSHIP_BY_SELLER_ID +
                        "&shopId=" +
                        "&status=" + substatus.getStatus().name() +
                        "&substatus=" + substatus.name()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        resourceAsString("order-with-real-delivery-date.json")
                                .replace("${STATUS}", substatus.getStatus().name())
                                .replace("${SUBSTATUS}", substatus.name())
                                .replace("${REAL_DELIVERY_DATE}", realDeliveryDate),
                        MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                HttpMethod.PUT,
                Format.XML,
                //language=xml
                ("<order status=\"${STATUS}\" substatus=\"${SUBSTATUS}\">" +
                        "<delivery>" +
                        "<dates real-delivery-date=\"${REAL_DELIVERY_DATE}\"/>" +
                        "</delivery>" +
                        "</order>")
                        //language=none
                        .replace("${STATUS}", substatus.getStatus().name())
                        .replace("${SUBSTATUS}", substatus.name())
                        .replace("${REAL_DELIVERY_DATE}", realDeliveryDate)
        ).getBody();
        Format.XML.assertResult(resourceAsString("expected-order-with-real-delivery-date.xml"), actual);
    }


    @Test
    void shouldNotFailOnPutWithEmptyRealDeliveryDateJson() {
        OrderSubstatus substatus = OrderSubstatus.DELIVERED_USER_RECEIVED;


        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer.expect(requestTo(
                "http://localhost/orders/100/status/delivered-with-real-delivery-date" +
                        "?clientRole=SHOP" +
                        "&clientId=" + DROPSHIP_BY_SELLER_ID +
                        "&shopId=" +
                        "&status=" + substatus.getStatus().name() +
                        "&substatus=" + substatus.name()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        resourceAsString("order-with-empty-shipment-and-statuses.json")
                                .replace("${STATUS}", substatus.getStatus().name())
                                .replace("${SUBSTATUS}", substatus.name()),
                        MediaType.APPLICATION_JSON));

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                HttpMethod.PUT,
                Format.JSON,
                //language=json
                ("{\"order\": {" +
                        "\"status\": \"${STATUS}\", " +
                        "\"substatus\": \"${SUBSTATUS}\", " +
                        "\"delivery\": { " +
                        "\"dates\": { " +
                        "}}}}")
                        //language=none
                        .replace("${STATUS}", substatus.getStatus().name())
                        .replace("${SUBSTATUS}", substatus.name())
        );
    }

    @Test
    void putRealDeliveryDateWithValidationErrorJson() {
        OrderSubstatus substatus = OrderSubstatus.DELIVERED_USER_RECEIVED;
        String realDeliveryDate = "01-01-2017";

        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer.expect(requestTo(
                        "http://localhost/orders/100/status/delivered-with-real-delivery-date" +
                                "?clientRole=SHOP" +
                                "&clientId=" + DROPSHIP_BY_SELLER_ID +
                                "&shopId=" +
                                "&status=" + substatus.getStatus().name() +
                                "&substatus=" + substatus.name()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest()
                        .body("{" +
                                "\"status\":400," +
                                "\"code\":\"INVALID_REQUEST\"," +
                                "\"message\":\"Real delivery date should be from order creation date to " +
                                "current date inclusively\"" +
                                "}"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                );

        var error = assertThrows(HttpClientErrorException.class, () ->
                FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                        HttpMethod.PUT,
                        Format.JSON,
                        //language=json
                        ("{\"order\": {" +
                                "\"status\": \"${STATUS}\", " +
                                "\"substatus\": \"${SUBSTATUS}\", " +
                                "\"delivery\": { " +
                                "\"dates\": { " +
                                "\"realDeliveryDate\": \"" + realDeliveryDate + "\" " +
                                "}}}}")
                                //language=none
                                .replace("${STATUS}", substatus.getStatus().name())
                                .replace("${SUBSTATUS}", substatus.name())
                                .replace("${REAL_DELIVERY_DATE}", realDeliveryDate)
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        var code = JsonPath.<String>read(error.getResponseBodyAsString(), "$.errors[0].code");
        assertEquals(ErrorRestModelCode.BAD_REQUEST.name(), code);

        var message = JsonPath.<String>read(error.getResponseBodyAsString(), "$.errors[0].message");
        assertEquals("Real delivery date should be from order creation date to current date inclusively", message);
    }

    static Stream<Arguments> cancelledStatusesForBlue() {
        var fewSuppliers = Map.of(
                10668L, 668L
        );
        return cancelledStatuses()
                .flatMap(args -> fewSuppliers.entrySet().stream().map(e -> {
                    var argsExtended = new ArrayList<>(List.of(args.get()));
                    argsExtended.add(e.getKey());
                    argsExtended.add(e.getValue());
                    return Arguments.of(argsExtended.toArray());
                }));
    }

    @ParameterizedTest
    @MethodSource("cancelledStatusesForBlue")
    void putCanceledSubstatusBlueShop(OrderSubstatus cancelledStatuses, long campaignId, long supplierId) {
        var checkouterServer = checkouterMockHelper.getServer();

        checkouterServer.expect(requestTo("http://localhost/orders/100/cancellation-request" +
                "?clientRole=SHOP" +
                "&clientId=" + supplierId +
                "&shopId=" +
                "&rgb=BLUE"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        resourceAsString("order-with-cancelled.json")
                                .replace("${SUBSTATUS}", cancelledStatuses.name()),
                        MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + campaignId + "/orders/100/status",
                HttpMethod.PUT,
                Format.JSON,
                //language=json
                ("{\"order\": {\"status\": \"CANCELLED\", \"substatus\": \"${SUBSTATUS}\"}}")
                        .replace(//language=none
                                "${SUBSTATUS}", cancelledStatuses.name())).getBody();

        assertThat(JsonPath.<Boolean>read(actual, "$.order.cancelRequested")).isTrue();
    }

    @ParameterizedTest
    @MethodSource("cancelledStatuses")
    void putCanceledSubstatusShop(OrderSubstatus cancelledStatuses) {
        checkouterMockHelper.mockOrderStatusChange(100L, DROPSHIP_BY_SELLER_ID, cancelledStatuses, withSuccess(
                resourceAsString("order-with-empty-shipment-and-statuses.json")
                        .replace("${STATUS}", cancelledStatuses.getStatus().name())
                        .replace("${SUBSTATUS}", cancelledStatuses.name()),
                MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                HttpMethod.PUT,
                Format.JSON,
                //language=json
                "{\"order\": {\"status\": \"CANCELLED\", \"substatus\": \"${SUBSTATUS}\"}}"
                        .replace(//language=none
                                "${SUBSTATUS}", cancelledStatuses.name())
        ).getBody();

        assertThatExceptionOfType(PathNotFoundException.class)
                .isThrownBy(() -> JsonPath.read(actual, "$.order.cancelRequested"));
    }

    @Test
    void testUserUnreachableCancellationError() {
        checkouterMockHelper.mockOrderStatusChange(100L, DROPSHIP_BY_SELLER_ID, OrderSubstatus.USER_UNREACHABLE,
                withBadRequest().body("{" +
                        "\"status\":400," +
                        "\"code\":\"USER_UNREACHABLE_NOT_ALLOWED\"," +
                        "\"message\":\"Substatus USER_UNREACHABLE not allowed!\"" +
                        "}"
                )
        );

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                        HttpMethod.PUT,
                        Format.JSON,
                        //language=json
                        "{\"order\": {\"status\": \"CANCELLED\", \"substatus\": \"USER_UNREACHABLE\"}}"
                ))
                .has(HamcrestCondition.matching(allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                                HttpClientErrorMatcher.errorMatches("USER_UNREACHABLE_NOT_ALLOWED", null, null)
                        ),
                        HttpClientErrorMatcher.hasErrorMessage(
                                "Substatus USER_UNREACHABLE not allowed!"
                        )
                )));
    }

    @Test
    void testStatusUpdateCisesValidationError() {
        checkouterMockHelper.mockOrderStatusChange(100L, DROPSHIP_BY_SELLER_ID, OrderSubstatus.USER_UNREACHABLE,
                withBadRequest().body("{" +
                        "\"status\":400," +
                        "\"code\":\"CISES_VALIDATION_ERROR\"," +
                        "\"message\":\"No permission to set status PROCESSING and substatus READY_TO_SHIP for order " +
                        "111763808 with status PROCESSING and substatus STARTED by reason: cises not loaded\"" +
                        "}"
                )
        );
        var error = assertThrows(HttpClientErrorException.class, () ->
                FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100/status",
                        HttpMethod.PUT,
                        Format.JSON,
                        //language=json
                        "{\"order\": {\"status\": \"CANCELLED\", \"substatus\": \"USER_UNREACHABLE\"}}"
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        var code = JsonPath.<String>read(error.getResponseBodyAsString(), "$.errors[0].code");
        assertEquals(ErrorRestModelCode.INVALID_CIS.name(), code);
    }

    @ParameterizedTest
    @MethodSource("substatuses")
    void getProcessingOrderWithSubstatuses(OrderSubstatus substatus) {
        checkouterMockHelper.mockGetOrder(100L, DROPSHIP_BY_SELLER_ID, withSuccess(
                resourceAsString("order-with-empty-shipment-and-statuses.json")
                        .replace("${STATUS}", substatus.getStatus().name())
                        .replace("${SUBSTATUS}", substatus.name()),
                MediaType.APPLICATION_JSON));

        String actual = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + DROPSHIP_BY_SELLER_CAMPAIGN_ID + "/orders/100",
                HttpMethod.GET,
                Format.JSON
        ).getBody();

        assertThat(JsonPath.<String>read(actual, "$.order.status")).isEqualTo(substatus.getStatus().name());
        assertThat(JsonPath.<String>read(actual, "$.order.substatus")).isEqualTo(substatus.name());
        assertThat(JsonPath.<String>read(actual, "$.order.delivery.shipments[0].shipmentDate")).isEqualTo("12-12-2019");
    }

    /**
     * Тестирует, что ручка {@link OrderControllerV2#updateOrderItems} недоступна для красных и синих не дропшипов.
     */
    @Test
    void checkPartnerTypeIsAllowedTest() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() ->
                        FunctionalTestHelper.makeRequest(
                                urlBasePrefix + "/campaigns/10702/orders/100/items",
                                HttpMethod.PUT,
                                Format.JSON,
                                DEFAULT_TEST_JSON_REQUEST_AND_RESPONSE
                        ))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));

    }

    /**
     * Тест проверяет корректный вывод данных об экземплярах товаров в формате JSON.
     */
    @Test
    void testOrderItemInstanceReturnForJson() {
        prepareCheckouterMock("checkouter/get_order_with_item_instance.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        preparePersonalMarketMock();
        requestOrderAndAssertResponseNoStrictEqual(
                Format.JSON, "asserts/expected-order-with-item-instances.json", DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                ORDER_ID);
    }

    /**
     * Тест проверяет корректный вывод данных об экземплярах товаров в формате XML.
     */
    @Test
    void testOrderItemInstanceForXML() {
        prepareCheckouterMock("checkouter/get_order_with_item_instance.json", ORDER_ID, DROPSHIP_BY_SELLER_ID);
        preparePersonalMarketMock();
        requestOrderAndAssertResponseNoStrictEqual(
                Format.XML, "asserts/expected-order-with-item-instances.xml", DROPSHIP_BY_SELLER_CAMPAIGN_ID, ORDER_ID);
    }

    @Test
    void moveSpasiboToSubsidy() {
        prepareCheckouterMock("checkouter/get_order_with_spasibo.json", 6922861, DROPSHIP_CLIENT_ID);
        preparePersonalMarketMock();
        String url = String.format("%s/campaigns/%s/orders/%s.%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, 6922861, Format.JSON.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        assertResponseJsonEqualsResource(response, "get_order_with_spasibo_response.json");
    }

    @Test
    void moveCashbackAndSpasiboToSubsidy() {
        prepareCheckouterMock("checkouter/get_order_with_spasibo_cashback.json", 6922861, DROPSHIP_CLIENT_ID);
        String url = String.format("%s/campaigns/%s/orders/%s.%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, 6922861, Format.JSON.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        assertResponseJsonEqualsResource(response, "get_order_with_spasibo_cashback_response.json");
    }

    @Test
    void doGetNotArchivedOrder() {
        prepareCheckouterMock("not-archived-order.json", DROPSHIP_ORDER_ID, DROPSHIP_CLIENT_ID);

        String url = String.format("%s/campaigns/%s/orders/%s.%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, DROPSHIP_ORDER_ID, Format.JSON.formatName());
        assertStatusOk(doGet(url));
    }

    @Test
    void doGetArchivedOrder() {
        var checkouterServer = checkouterMockHelper.getServer();
        checkouterServer
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(checkouterUrl + "/orders/" + DROPSHIP_ARCHIVED_ORDER_ID
                        + "?clientRole=SHOP&clientId=" + DROPSHIP_CLIENT_ID + "&shopId=&archived=true"))
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(resourceAsString("archived-order.json"))
                );

        String url = String.format("%s/campaigns/%s/orders/%s.%s",
                urlBasePrefix, DROPSHIP_CAMPAIGN_ID, DROPSHIP_ARCHIVED_ORDER_ID, Format.JSON.formatName());
        assertStatusOk(doGet(url));
    }

    @Test
    void testSCDropshipRejectedJson() {
        prepareCheckouterMock("checkouter/get_order_125.json", 669);
        String url = String.format("%s/campaigns/%s/orders.%s", urlBasePrefix, 10669, Format.JSON.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        assertResponseJsonEqualsResource(response, "asserts/get_orders_125.json");
    }

    @Test
    void testSCDropshipRejectedXml() {
        prepareCheckouterMock("checkouter/get_order_125.json", 669);
        String url = String.format("%s/campaigns/%s/orders.%s", urlBasePrefix, 10669, Format.XML.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        Format.XML.assertResult(resourceAsString("asserts/get_orders_125.xml"), response.getBody());
    }

    @Test
    void testDSDropshipRejected() {
        prepareCheckouterMock("checkouter/get_order_126.json", 126, 670);
        String url = String.format("%s/campaigns/%s/orders/%s.%s", urlBasePrefix, 10670, 126, Format.JSON.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        assertResponseJsonEqualsResource(response, "asserts/get_order_126.json");
    }

    @Test
    void testDSDropshipRejectedXML() {
        prepareCheckouterMock("checkouter/get_order_126.json", 126, 670);
        String url = String.format("%s/campaigns/%s/orders/%s.%s", urlBasePrefix, 10670, 126, Format.XML.formatName());
        ResponseEntity<String> response = doGet(url);
        assertStatusOk(response);
        Format.XML.assertResult(resourceAsString("asserts/get_order_126.xml"), response.getBody());
    }

    /**
     * Тестирует работу ручки /get-orders с фильтром по КИЗам - правильное прокидывание параметра в чекаутер.
     */
    @Test
    void getOrdersWithCises() {
        String url = String.format("%s/campaigns/%s/orders.%s?%s", urlBasePrefix, 10670, Format.JSON.formatName(),
                "hasCis=true");

        var checkouterServer = checkouterMockHelper.getServer();
        checkouterMockHelper.mockGetOrdersReturnsBody(670, resourceAsString("checkouter/get_orders_with_cises.json"));
        checkouterServer.expect(jsonPath("withCises", equalTo(true)));

        final ResponseEntity<String> responseEntity = doGet(url);
        assertResponseJsonEqualsResource(responseEntity, "asserts/get_orders_with_cises.json");
    }

    private static ResponseEntity<String> doGet(String url) {
        return FunctionalTestHelper.makeRequest(url, HttpMethod.GET, String.class);
    }

    private static ResponseEntity<String> doGet(String url, long uid) {
        return FunctionalTestHelper.makeRequest(url, HttpMethod.GET, String.class, uid);
    }

    private static void assertStatusOk(ResponseEntity<?> responseEntity) {
        assertThat(responseEntity.getStatusCode())
                .as("actual body is " + responseEntity.getBody())
                .isEqualTo(HttpStatus.OK);
    }

    private void prepareFF4ShopsCourierInfoMock(String responcePath) {
        MockRestServiceServer server = MockRestServiceServer.createServer(ff4ShopsRestTemplate);
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format("%s/orders/extend-info/get-by-ids?orderIds=%d",
                                ff4shopsUrl, ORDER_ID
                        )))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(responcePath)));
    }

    private void prepareCheckouterMock(String bodyPath, long orderId, long clientId) {
        checkouterMockHelper.mockGetOrderReturnsBody(orderId, clientId, resourceAsString(bodyPath));
    }

    private void prepareCheckouterMock(String bodyPath, long orderId, List<Long> clientIds) {
        checkouterMockHelper.mockGetOrderReturnsBody(orderId, clientIds, resourceAsString(bodyPath));
    }

    private void prepareCheckouterMock(String bodyPath, long clientId) {
        checkouterMockHelper.mockGetOrdersReturnsBody(clientId, resourceAsString(bodyPath));
    }

    private void requestOrderAndAssertResponseNoStrictEqual(Format format, String expectedBodyPath, long campaignId,
                                                            long orderId) {
        requestOrderAndAssertResponseNoStrictEqual(format, expectedBodyPath, campaignId, orderId, null);
    }

    private void requestOrderAndAssertResponseNoStrictEqual(Format format, String expectedBodyPath, long campaignId,
                                                            long orderId, Long uid) {
        String url = String.format("%s/campaigns/%s/orders/%s.%s",
                urlBasePrefix, campaignId, orderId, format.formatName());
        ResponseEntity<String> response;
        if (uid == null) {
            response = doGet(url);
        } else {
            response = doGet(url, uid);
        }

        assertStatusOk(response);
        format.assertResult(resourceAsString(expectedBodyPath), response.getBody());
    }

    private void assertResponseJsonEqualsResource(ResponseEntity<String> response, String resourcePath) {
        JsonTestUtil.assertEquals(resourceAsString(resourcePath), Objects.requireNonNull(response.getBody()));
    }

    @Test
    void tuneDateTo() {
        var someDate = LocalDate.of(2021, 2, 20);
        assertThat(OrderController.checkAndAdjustDateTo(
                Optional.empty(),
                Optional.empty()
        )).isEmpty();
        assertThat(OrderController.checkAndAdjustDateTo(
                Optional.of(someDate),
                Optional.empty()
        )).isEmpty();
        assertThat(OrderController.checkAndAdjustDateTo(
                Optional.empty(),
                Optional.of(someDate)
        )).hasValue(someDate);
        assertThat(OrderController.checkAndAdjustDateTo(
                Optional.of(someDate),
                Optional.of(someDate)
        )).as("дата to должна быть больше, чтобы поиск сработал").hasValue(someDate.plusDays(1));
        assertThat(OrderController.checkAndAdjustDateTo(
                Optional.of(someDate),
                Optional.of(someDate.plusDays(30))
        )).hasValue(someDate.plusDays(30));
        assertThatExceptionOfType(InvalidRequestException.class)
                .as("задокументированное ограничение")
                .isThrownBy(() -> OrderController.checkAndAdjustDateTo(
                        Optional.of(someDate),
                        Optional.of(someDate.plusDays(61)) // см реализацию, потом чиселку подправим
                ));
    }

    private void preparePersonalMarketMock() {
        when(personalMarketService.retrieve(new PersonalRetrieveRequestBuilder()
                .fullName("fullname_079afb9888baf7b98cf1ac9ab79b4aee")
                .phone("phone_785956a16c1246bd0bf7162d066a2b4f")
                .email("email_12c772b71dae393caafc1c6a47d9e895")
                .address("address_f10d15da7864c7751876ee550bc10741")
                .fullName("addressFullName_07c5eb7048ff0b32fecf541331c69357")
                .phone("addressPhone_0c3e45dcf839afa7a0d463bc4ca0cb98")
        )).thenReturn(CompletableFuture.completedFuture(
                PersonalRetrieveResponse.builder()
                        .fullName("fullname_079afb9888baf7b98cf1ac9ab79b4aee", "Даша", "Тимофеева", "Ивановна")
                        .phone("phone_785956a16c1246bd0bf7162d066a2b4f", "+7 921 301-69-09")
                        .email("email_12c772b71dae393caafc1c6a47d9e895", "spbtester@yandex.ru")
                        .address("address_f10d15da7864c7751876ee550bc10741", PersonalAddress.builder()
                                .withCountry("Russia")
                                .withPostcode("117465")
                                .withCity("Moscow")
                                .withStreet("ulitsa Tyoplyj Stan")
                                .withHouse("9k5")
                                .withApartment("42")
                                .build())
                        .fullName("addressFullName_07c5eb7048ff0b32fecf541331c69357", "Igor'", "Demishev", null)
                        .phone("addressPhone_0c3e45dcf839afa7a0d463bc4ca0cb98", "+79165401875")
                        .build()
        ));
    }
}

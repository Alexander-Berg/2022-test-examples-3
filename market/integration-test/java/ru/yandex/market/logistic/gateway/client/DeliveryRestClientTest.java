package ru.yandex.market.logistic.gateway.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.util.BiConsumer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistic.gateway.client.config.ClientTestConfig;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.client.service.delivery.DataExchangeService;
import ru.yandex.market.logistic.gateway.client.service.delivery.MovementService;
import ru.yandex.market.logistic.gateway.client.service.delivery.OrderService;
import ru.yandex.market.logistic.gateway.client.service.delivery.TripService;
import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.common.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistic.gateway.common.model.common.response.GetMovementStatusHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.common.response.GetMovementStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPoint;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.CallCourierResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetCourierResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetExternalOrderHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetExternalOrdersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrderResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrdersDeliveryDateResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.gateway.utils.CommonDtoFactory;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;
import static ru.yandex.market.logistics.util.client.HttpTemplate.USER_TICKET_HEADER;

public class DeliveryRestClientTest extends AbstractRestTest {

    private DeliveryClient deliveryClient;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        deliveryClient = new DeliveryClientImpl(
            new OrderService(null, httpTemplate),
            new DataExchangeService(httpTemplate, null),
            new MovementService(httpTemplate, null),
            new TripService(httpTemplate, null),
            null
        );
    }

    @Test
    public void getReferencePickupPointsSuccess() throws IOException, GatewayApiException {
        mock.expect(requestTo(uri + "/delivery/getReferencePickupPoints"))
            .andExpect(
                content().string(JsonMatcher.getMatcherFunction().apply(
                    getFileContent("delivery/get_reference_pickup_points/get_reference_pickup_points_request.json"))
                )
            )
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(getFileContent(
                        "delivery/get_reference_pickup_points/get_reference_pickup_points_response.json"
                    ))
            );

        List<PickupPoint> expected = Collections.singletonList(
            PickupPoint.builder()
                .setCode("test-code")
                .setAddress(DeliveryDtoFactory.createLocation())
                .setPhones(Collections.singletonList(DeliveryDtoFactory.createPhone()))
                .setActive(true)
                .setSchedule(Collections.singletonList(DeliveryDtoFactory.createWorkTime()))
                .setCashAllowed(true)
                .setPrepayAllowed(false)
                .setAvailableForC2C(true)
                .setServices(Collections.emptyList())
                .setCalendar(Collections.emptyList())
                .setWorkDays(Collections.emptyList())
                .setDayOffs(Collections.emptyList())
                .build()
        );

        List<PickupPoint> actual = deliveryClient.getReferencePickupPoints(
            Collections.singletonList(CommonDtoFactory.createLocationFilterRussia()),
            Collections.singletonList("test-code"),
            DeliveryDtoFactory.createDateTimeInterval(),
            DeliveryDtoFactory.createPartner()
        );

        assertions.assertThat(actual).isNotNull();
        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getOrder() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "delivery/getOrder",
            "delivery/get_order/request.json",
            "delivery/get_order/response.json",
            GetOrderResponse.class,
            () -> deliveryClient.getOrderSync(orderId, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not null")
                    .isNotNull();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse.getOrder());
            }
        );
    }

    @Test
    public void getOrderHistory() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "delivery/getOrderHistory",
            "delivery/get_order_history/request.json",
            "delivery/get_order_history/response.json",
            GetOrderHistoryResponse.class,
            () -> deliveryClient.getOrderHistory(orderId, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not null")
                    .isNotNull();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .isEqualToComparingFieldByFieldRecursively(expectedResponse.getOrderStatusHistory());
            }
        );
    }

    @Test
    public void getOrdersStatus() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "delivery/getOrdersStatus",
            "delivery/get_orders_status/request.json",
            "delivery/get_orders_status/response.json",
            GetOrdersStatusResponse.class,
            () -> deliveryClient.getOrdersStatus(Collections.singletonList(orderId), partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getOrderStatusHistories());
            }
        );
    }

    @Test
    public void getExternalOrderHistory() throws Exception {
        ExternalResourceId orderId = new ExternalResourceId("12345", "ABC12345", "DSID12345");
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "delivery/getExternalOrderHistory",
            "delivery/get_external_order_history/request.json",
            "delivery/get_external_order_history/response.json",
            GetExternalOrderHistoryResponse.class,
            () -> deliveryClient.getExternalOrderHistory(orderId, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not null")
                    .isNotNull();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .isEqualToComparingFieldByFieldRecursively(expectedResponse.getExternalOrderStatusHistory());
            }
        );
    }

    @Test
    public void getExternalOrdersStatus() throws Exception {
        ExternalResourceId orderId = new ExternalResourceId("12345", "ABC12345", "DSID12345");
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "delivery/getExternalOrdersStatus",
            "delivery/get_external_orders_status/request.json",
            "delivery/get_external_orders_status/response.json",
            GetExternalOrdersStatusResponse.class,
            () -> deliveryClient.getExternalOrdersStatus(Collections.singletonList(orderId), partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getExternalOrderStatusHistories());
            }
        );
    }

    @Test
    public void getTransactionsOrders() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "delivery/getTransactionsOrders",
            "delivery/get_transactions_orders/request.json",
            "delivery/get_transactions_orders/response.json",
            GetTransactionsOrdersResponse.class,
            () -> deliveryClient.getTransactionsOrders(orderId, 1, 0, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getTransactions());
            }
        );
    }

    @Test
    public void getTransactionsOrdersByInterval() throws Exception {
        DateTimeInterval dateTimeInterval =
            DateTimeInterval.fromFormattedValue("2019-08-02T00:00:00+03:00/2019-08-10T00:00:00+03:00");
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "delivery/getTransactionsOrders",
            "delivery/get_transactions_orders/request_by_interval.json",
            "delivery/get_transactions_orders/response.json",
            GetTransactionsOrdersResponse.class,
            () -> deliveryClient.getTransactionsOrders(dateTimeInterval, 1, 0, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getTransactions());
            }
        );
    }

    @Test
    public void getOrdersDeliveryDate() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "delivery/getOrdersDeliveryDate",
            "delivery/get_orders_delivery_date/request.json",
            "delivery/get_orders_delivery_date/response.json",
            GetOrdersDeliveryDateResponse.class,
            () -> deliveryClient.getOrdersDeliveryDate(Collections.singletonList(orderId), partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getOrderDeliveryDates());
            }
        );
    }

    @Test
    public void getMovementStatus() throws Exception {
        List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> resourceIds = createEntityResourceIds();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "delivery/getMovementStatus",
            "common/get_movement_status/request.json",
            "common/get_movement_status/response.json",
            GetMovementStatusResponse.class,
            () -> deliveryClient.getMovementStatus(resourceIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getMovementStatuses());
            }
        );
    }

    @Test
    public void getMovementStatusHistory() throws Exception {
        List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> resourceIds = createEntityResourceIds();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "delivery/getMovementStatusHistory",
            "common/get_movement_status_history/request.json",
            "common/get_movement_status_history/response.json",
            GetMovementStatusHistoryResponse.class,
            () -> deliveryClient.getMovementStatusHistory(resourceIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getMovementStatusHistories());
            }
        );
    }

    @Test
    public void callCourier() throws Exception {
        ru.yandex.market.logistic.gateway.common.model.common.ResourceId orderId =
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("yId123")
                .setPartnerId("pId123")
                .build();
        Duration waitingTime = Duration.ofSeconds(30);
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "delivery/callCourier",
            "delivery/call_courier/request.json",
            "delivery/call_courier/response.json",
            CallCourierResponse.class,
            () -> deliveryClient.callCourier(orderId, waitingTime, partner),
            (expectedResponse, actualResult) -> assertions.assertThat(actualResult)
                .as("Asserting that actual result is correct")
                .isEqualTo(expectedResponse.getOrderId())
        );
    }

    private List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> createEntityResourceIds() {
        return Arrays.asList(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("111424")
                .setPartnerId("111525")
                .build(),
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("222424")
                .setPartnerId("222525")
                .build(),
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("333424")
                .setPartnerId("333525")
                .build()
        );
    }

    private <ExpectedResponseType, ActualResultType> void executePositiveScenario(
        String method,
        String expectedRequestPath,
        String responsePath,
        Class<ExpectedResponseType> responseTypeClass,
        Supplier<ActualResultType> clientCall,
        BiConsumer<ExpectedResponseType, ActualResultType> responseMatchingFunction
    ) throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent(responsePath));

        mock.expect(requestTo(uri + "/" + method))
            .andExpect(content().string(JsonMatcher.getMatcherFunction()
                .apply(getFileContent(expectedRequestPath))))
            .andExpect(header(SERVICE_TICKET_HEADER, ClientTestConfig.TVM_SERVICE_TICKET))
            .andExpect(header(USER_TICKET_HEADER, ClientTestConfig.TVM_USER_TICKET))
            .andRespond(taskResponseCreator);

        ActualResultType actualResult = clientCall.get();
        ExpectedResponseType expectedResponse =
            objectMapper.readValue(getFileContent(responsePath), responseTypeClass);

        responseMatchingFunction.accept(expectedResponse, actualResult);
    }

    @Test
    public void getCourierSuccess() throws IOException, GatewayApiException {
        mock.expect(requestTo(uri + "/delivery/getCourier"))
            .andExpect(content().string(JsonMatcher.getMatcherFunction().apply(
                getFileContent("delivery/get_courier/get_courier_request.json"))
                )
            )
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                        .body(getFileContent(
                            "delivery/get_courier/get_courier_response.json"
                            ))
            );
        Courier courier = Courier.builder()
            .setPartnerId(ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                    .setPartnerId("1006360")
                    .setYandexId("12345")
                    .build())
            .setPersons(Collections.singletonList(
                Person.builder("Иван")
                    .setSurname("Иванов")
                    .setPatronymic("Иванович")
                    .build()
                )
            )
            .setPhone(Phone.builder("+79991234567").setAdditional("123").build())
            .setCar(Car.builder("А001AA001")
                    .setDescription("Вишневая девятка")
                    .setModel("девяточка")
                    .setColor("вишнёвенькая")
                    .build())
            .setLegalEntity(LegalEntity.builder()
                    .setLegalName("legalName")
                    .setName("name")
                    .setLegalForm(LegalForm.OOO)
                    .setOgrn("1223455")
                    .setInn("5543221")
                    .setKpp("123")
                    .setBank("bank")
                    .setAccount("1234")
                    .setBik("123456")
                    .setCorrespondentAccount("1234567")
                    .build())
            .setUrl("https://go.yandex/route/6ea161f870ba6574d3bd9bdd19e1e9d8?lang=ru")
            .build();
        OrderTransferCodes codes = new OrderTransferCodes.OrderTransferCodesBuilder()
            .setOutbound(
                new OrderTransferCode.OrderTransferCodeBuilder()
                    .setVerification("303030")
                    .setElectronicAcceptanceCertificate("202020")
                    .build()
            )
            .setInbound(
                new OrderTransferCode.OrderTransferCodeBuilder()
                    .setVerification("313131")
                        .setElectronicAcceptanceCertificate("212121")
                        .build()
            )
            .build();

        GetCourierResponse actual = deliveryClient.getCourier(
            ResourceId.builder()
                .setYandexId("12345").setPartnerId("1006360")
                .build(),
                new Partner(654L)
        );

        assertions.assertThat(actual.getCourier()).isEqualTo(courier);
        assertions.assertThat(actual.getCodes()).isEqualTo(codes);
    }
}

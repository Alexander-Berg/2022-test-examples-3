package ru.yandex.market.pvz.internal.controller.pi.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderCashboxTransactionRepository;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.OrderCashboxTransaction;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderSimpleParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.common.web.go_zora.GoZoraClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.INCOMPLETE_FASHION_ORDER_THRESHOLD;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.NEW_ORDER_PARTNER_SERVICE_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_CLIENT_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_ON_DEMAND_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.model.OrderVerification.DEFAULT_ORDER_VERIFICATION_CODE_LIMIT;
import static ru.yandex.market.pvz.core.domain.sms.SmsLogCommandService.DEFAULT_SMS_VERIFICATION_CODE_LIMIT;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_ITEMS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.R18_ITEM;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.getDefaultPhoneTail;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderPersonalParams.DEFAULT_RECIPIENT_FULL_NAME_ID;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderPersonalParams.DEFAULT_RECIPIENT_PHONE_ID;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrdersControllerTest extends BaseShallowTest {

    public static final String COURIER_ID = "courier_id_from_test";
    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final OrderQueryService orderQueryService;
    private final TestOrderFactory orderFactory;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final OrderCashboxTransactionRepository cashboxTransactionRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final GoZoraClient goZoraClient;

    @MockBean
    private PersonalExternalService personalExternalService;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_CLIENT_ENABLED, true);
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_ON_DEMAND_ENABLED, true);

        when(personalExternalService.getMultiTypePersonalByIds(List.of(
                Pair.of(DEFAULT_RECIPIENT_FULL_NAME_ID, CommonTypeEnum.FULL_NAME),
                Pair.of(DEFAULT_RECIPIENT_PHONE_ID, CommonTypeEnum.PHONE)))
        ).thenReturn(List.of(
                new MultiTypeRetrieveResponseItem()
                        .id(DEFAULT_RECIPIENT_FULL_NAME_ID)
                        .type(CommonTypeEnum.FULL_NAME)
                        .value(new CommonType().fullName(new FullName().forename("Пупкин").surname("Василий"))),
                new MultiTypeRetrieveResponseItem()
                        .id(DEFAULT_RECIPIENT_PHONE_ID)
                        .type(CommonTypeEnum.PHONE)
                        .value(new CommonType().phone(DEFAULT_RECIPIENT_PHONE))
        ));
    }

    @Test
    @SneakyThrows
    void testGetOrders() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        var order = createAndReceiveOrder(pickupPoint);
        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_orders_without_search.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getPvzMarketId()), true
                ));
    }

    @Test
    @SneakyThrows
    void testGetOrdersTrueSearch() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        var order = createAndReceiveOrder(pickupPoint);
        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders?search=true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_orders_search_true.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getPvzMarketId()), true
                ));
    }

    @Test
    @SneakyThrows
    void testGetOrdersTrueSearchByPhoneTail() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        var order = createAndReceiveOrder(pickupPoint);
        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders?search=true&commonQuery=" +
                        getDefaultPhoneTail())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_orders_search_true.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getPvzMarketId()), true
                ));
    }

    @Test
    @SneakyThrows
    void testGetOrdersTrueSearchByExternalId() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        var order = createAndReceiveOrder(pickupPoint);
        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders?search=true&commonQuery=" + order.getExternalId().substring(2, 5))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_orders_search_true.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getPvzMarketId()), true
                ));
    }

    @ParameterizedTest
    @CsvSource({"true, response_orders", "false, response_orders_without_search"})
    @SneakyThrows
    void testGetOrdersTrueSearchWithoutPersonalToggle(boolean search, String fileName) {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        var order = createAndReceiveOrder(pickupPoint);
        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders?search=" + search)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/" + fileName + ".json"),
                        order.getId(), order.getExternalId(), pickupPoint.getPvzMarketId()), true
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testGetOrdersWithSiblings(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        long pvzId = pickupPoint.getPvzMarketId();
        Order almostDeliveredOrder = createAndReceiveAndVerifyOrder(pickupPoint);
        Order sibling1 = createAndReceiveOrder(pickupPoint);
        Order sibling2 = createAndReceiveOrder(pickupPoint);
        Order deliveredOrder = orderFactory.deliverOrder(almostDeliveredOrder.getId(),
                OrderDeliveryType.VERIFICATION_CODE, OrderPaymentType.PREPAID);

        mockMvc.perform(get("/v1/pi/pickup-points/" + pvzId + "/orders/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        String.format(getFileContent("order/response_orders_with_siblings.json"),
                                sibling2.getId(), sibling2.getExternalId(), pvzId, sibling1.getExternalId(),
                                sibling1.getId(), sibling1.getExternalId(), pvzId, sibling2.getExternalId(),
                                deliveredOrder.getId(), deliveredOrder.getExternalId(), pvzId), true
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testGetOrderWithSiblings(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order deliveredOrder = createAndReceiveAndVerifyOrder(pickupPoint);
        Order arrivedOrder = createAndReceiveOrder(pickupPoint);
        Order siblingOrder = createAndReceiveOrder(pickupPoint);
        orderFactory.deliverOrder(deliveredOrder.getId(), OrderDeliveryType.VERIFICATION_CODE,
                OrderPaymentType.PREPAID);

        mockMvc.perform(get(String.format("/v1/pi/pickup-points/%d/orders/%s",
                        pickupPoint.getPvzMarketId(), arrivedOrder.getId()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_order_with_siblings.json"),
                        arrivedOrder.getId(), arrivedOrder.getExternalId(), pickupPoint.getPvzMarketId(),
                        pickupPoint.getId(), arrivedOrder.getAssessedCost(), siblingOrder.getExternalId()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testSimpleDeliverOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Instant deliveredAt = Instant.parse("2021-01-15T12:00:00Z");

        Order deliveredSibling = createAndReceiveAndVerifyOrder(pickupPoint);
        Order order = createAndReceiveOrder(pickupPoint);

        clock.setFixed(deliveredAt.minus(10, ChronoUnit.MINUTES), zone);
        orderFactory.deliverOrder(deliveredSibling.getId(), OrderDeliveryType.VERIFICATION_CODE,
                OrderPaymentType.PREPAID);

        clock.setFixed(deliveredAt, zone);
        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%d/orders/%d/deliver", pickupPoint.getPvzMarketId(),
                        order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"SIMPLIFIED_DELIVERY\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_simple_deliver.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(), pickupPoint.getPvzMarketId(),
                        order.getCreatedAt(), order.getAssessedCost()), true
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testSimpleDeliverOrderWithSiblings(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Instant deliveredAt = Instant.parse("2021-01-15T12:00:00Z");

        Order deliveredSibling = createAndReceiveAndVerifyOrder(pickupPoint);
        Order sibling1 = createAndReceiveOrder(pickupPoint);
        Order sibling2 = createAndReceiveOrder(pickupPoint);
        Order order = createAndReceiveOrder(pickupPoint);

        clock.setFixed(deliveredAt.minus(10, ChronoUnit.MINUTES), zone);
        orderFactory.deliverOrder(deliveredSibling.getId(), OrderDeliveryType.VERIFICATION_CODE,
                OrderPaymentType.PREPAID);

        clock.setFixed(deliveredAt, zone);
        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%d/orders/%d/deliver", pickupPoint.getPvzMarketId(),
                        order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"SIMPLIFIED_DELIVERY\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_simple_deliver_with_siblings.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(), pickupPoint.getPvzMarketId(),
                        order.getCreatedAt(), order.getAssessedCost(),
                        sibling1.getExternalId(), sibling2.getExternalId()), true
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testSimpleDeliverPostPaidOrderFails(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        Order deliveredSibling = createAndReceiveAndVerifyOrder(pickupPoint);
        Order order = createAndReceiveOrder(pickupPoint, OrderType.CLIENT, OrderPaymentType.CARD,
                DEFAULT_VERIFICATION_CODE);

        clock.setFixed(Instant.now().minus(10, ChronoUnit.MINUTES), zone);
        orderFactory.deliverOrder(deliveredSibling.getId(), OrderDeliveryType.VERIFICATION_CODE,
                OrderPaymentType.PREPAID);

        Instant deliveredAt = Instant.now();
        clock.setFixed(deliveredAt, zone);

        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%d/orders/%d/deliver", pickupPoint.getPvzMarketId(),
                        order.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"deliveryType\": \"SIMPLIFIED_DELIVERY\"}"))
                .andExpect(status().is4xxClientError());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testSimpleDeliverOnDemandOrderFails(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        Order deliveredSibling = createAndReceiveAndVerifyOrder(pickupPoint);
        Order order = createAndReceiveOrder(pickupPoint, OrderType.ON_DEMAND, OrderPaymentType.PREPAID,
                DEFAULT_VERIFICATION_CODE);

        clock.setFixed(Instant.now().minus(10, ChronoUnit.MINUTES), zone);
        orderFactory.deliverOrder(deliveredSibling.getId(), OrderDeliveryType.VERIFICATION_CODE,
                OrderPaymentType.PREPAID);

        Instant deliveredAt = Instant.now();
        clock.setFixed(deliveredAt, zone);

        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%d/orders/%d/deliver", pickupPoint.getPvzMarketId(),
                        order.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"deliveryType\": \"SIMPLIFIED_DELIVERY\"}"))
                .andExpect(status().is4xxClientError());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testSimpleDeliverR18OrderFails(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        Order deliveredSibling = createAndReceiveAndVerifyOrder(pickupPoint);
        Order order = createAndReceiveOrder(pickupPoint, RandomStringUtils.randomAlphanumeric(6), OrderType.CLIENT,
                OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE, true);

        clock.setFixed(Instant.now().minus(10, ChronoUnit.MINUTES), zone);
        orderFactory.deliverOrder(deliveredSibling.getId(), OrderDeliveryType.VERIFICATION_CODE,
                OrderPaymentType.PREPAID);

        Instant deliveredAt = Instant.now();
        clock.setFixed(deliveredAt, zone);

        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%d/orders/%d/deliver", pickupPoint.getPvzMarketId(),
                        order.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"deliveryType\": \"SIMPLIFIED_DELIVERY\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void sendCashboxReceipt() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .externalId("12345")
                        .paymentType(OrderPaymentType.CARD)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(order.getId());

        var response = getFileContent("order/cashbox/cashbox_send_receipt_response.json");
        when(goZoraClient.post(any(), any())).thenReturn(ResponseEntity.ok(response));

        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%s/orders/%s/send-cashbox-receipt",
                pickupPoint.getPvzMarketId(), order.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("order/cashbox/cashbox_send_receipt_request.json")))
                .andExpect(status().is(200));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void successDeliver(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        var order = createAndReceiveAndVerifyOrder(pickupPoint);

        OffsetDateTime deliveredTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 7, 17, 30, 0),
                zone);
        clock.setFixed(deliveredTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void successDeliverOnDemandOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        var order = createAndReceiveAndVerifyOrder(pickupPoint, OrderType.ON_DEMAND);

        OffsetDateTime deliveredTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 7, 17, 30, 0),
                zone);
        clock.setFixed(deliveredTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver_on_demand.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void invalidOrderId(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/orderId/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void invalidOrderStatus(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void orderIsNotFound(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());
        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + (order.getId() + 100) + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verificationCodeIsNotAcceptedForDeliver(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = createAndReceiveOrder(pickupPoint);
        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verificationCodeIsNotAcceptedForDeliverOnDemand(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = createAndReceiveOrder(
                pickupPoint, OrderType.ON_DEMAND, OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE);
        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void successDeliverOnDemandWithNotAcceptedAndDisabledCode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_ON_DEMAND_ENABLED, String.valueOf(false));
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        var order = createAndReceiveOrder(
                pickupPoint, OrderType.ON_DEMAND, OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE);

        OffsetDateTime deliveredTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 7, 17, 30, 0),
                zone);
        clock.setFixed(deliveredTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver_on_demand_without_code_verification.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void successDeliverOnDemandWithNotAcceptedAndNullCode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        var order = createAndReceiveOrder(pickupPoint, OrderType.ON_DEMAND, OrderPaymentType.PREPAID, null);

        OffsetDateTime deliveredTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 7, 17, 30, 0),
                zone);
        clock.setFixed(deliveredTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver_on_demand_without_code_verification.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyValidBarcode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        clock.setFixed(Instant.parse("2021-12-05T12:00:00Z"), ZoneOffset.ofHours(3));

        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(
                pickupPoint, "id-with-hyphens", OrderType.CLIENT,
                OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE, false);

        String barcode = order.getExternalId() + "-" + DEFAULT_VERIFICATION_CODE;

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/verify-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("order/request_verify_barcode.json"), barcode)))
                .andExpect(content().json(String.format(
                        getFileContent("order/response_verify_barcode.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt(),
                        true, DEFAULT_ORDER_VERIFICATION_CODE_LIMIT - 1), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyInvalidBarcode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        String barcode = order.getExternalId() + "-" + "10";

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/verify-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("order/request_verify_barcode.json"), barcode)))
                .andExpect(content().json(String.format(
                        getFileContent("order/response_verify_barcode.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt(),
                        false, DEFAULT_ORDER_VERIFICATION_CODE_LIMIT - 1), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyValidCode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("order/request_verify_code.json"), DEFAULT_VERIFICATION_CODE)))
                .andExpect(content().json(String.format(
                        getFileContent("order/response_verify_code.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt(),
                        true, DEFAULT_ORDER_VERIFICATION_CODE_LIMIT - 1), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyInvalidCode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("order/request_verify_code.json"), "99")))
                .andExpect(content().json(String.format(
                        getFileContent("order/response_verify_code.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt(),
                        false, DEFAULT_ORDER_VERIFICATION_CODE_LIMIT - 1), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyCodeTillLimit(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);

        for (int i = 0; i < DEFAULT_ORDER_VERIFICATION_CODE_LIMIT; i++) {
            mockMvc.perform(
                    patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                            "/orders/" + order.getId() + "/verify-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(getFileContent("order/request_verify_code.json"), "99")))
                    .andExpect(status().is2xxSuccessful());
        }
        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("order/request_verify_code.json"), "99")))
                .andExpect(status().isForbidden());

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyCodeForInvalidOrderStatus(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("order/request_verify_code.json"), "99")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void verifyCodeForOrderWithoutVerificationCode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .verificationCode(null)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("order/request_verify_code.json"), "99")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void sendCodeViaSms(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveOrder(pickupPoint);
        OffsetDateTime smsSendTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 11, 16, 50, 40),
                zone);
        clock.setFixed(smsSendTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/send-code-via-sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(String.format(
                        getFileContent("order/response_send_code_via_sms.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void sendCodeViaSmsAndExceedLimit(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveOrder(pickupPoint);
        OffsetDateTime smsSendTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 11, 16, 50, 40),
                zone);
        clock.setFixed(smsSendTime.toInstant(), zone);

        for (int i = 0; i < DEFAULT_SMS_VERIFICATION_CODE_LIMIT - 1; i++) {
            mockMvc.perform(
                    post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                            "/orders/" + order.getId() + "/send-code-via-sms")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(String.format(
                            getFileContent("order/response_send_code_via_sms.json"),
                            order.getId(), order.getExternalId(), pickupPoint.getId(),
                            pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                    ));
        }

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/send-code-via-sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(String.format(
                        getFileContent("order/response_send_code_via_sms_exceed_limit.json"),
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void sendCodeViaSmsForPostpaidOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveOrder(
                pickupPoint, OrderType.CLIENT, OrderPaymentType.CASH, DEFAULT_VERIFICATION_CODE);
        OffsetDateTime smsSendTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 11, 16, 50, 40),
                zone);
        clock.setFixed(smsSendTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/send-code-via-sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void sendCodeViaSmsForOnDemandOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveOrder(
                pickupPoint, OrderType.ON_DEMAND, OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE);
        OffsetDateTime smsSendTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 11, 16, 50, 40),
                zone);
        clock.setFixed(smsSendTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/send-code-via-sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void sendCodeViaSmsForAlreadyDeliveredOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveAndVerifyOrder(pickupPoint);
        OffsetDateTime smsSendTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 11, 16, 50, 40),
                zone);
        clock.setFixed(smsSendTime.toInstant(), zone);

        order = orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/send-code-via-sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void sendCodeViaSmsForOrderWithoutVerificationCode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveOrder(pickupPoint, OrderType.CLIENT, OrderPaymentType.PREPAID, null);
        OffsetDateTime smsSendTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 11, 16, 50, 40),
                zone);
        clock.setFixed(smsSendTime.toInstant(), zone);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/send-code-via-sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void orderNotFoundForVerifyOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + (order.getId() + 100) + "/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("order/request_verify_code.json"), "99")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCashboxStatuses() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        var orders = createOrders(pickupPoint);
        var orderId = orders.get(0).getId();

        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                "/orders/cashbox-payment-status?orderIds=" + orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful()).andExpect(content().json(String.format(
                getFileContent("order/cashbox_statuses.json"), orderId), false));
    }

    @Test
    void getCashboxStatusesAwaitingForPayment() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        var orders = createOrders(pickupPoint);

        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                "/orders/awaiting-payment-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful()).andExpect(content().json(String.format(
                getFileContent("order/cashbox_statuses_awaiting_payment.json"),
                orders.get(0).getId(), orders.get(1).getId(), false)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void notAllowedSystemDeliveryType(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/" + order.getId() + "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"SYSTEM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetIncompleteFashionOrderIds() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        var order = orderFactory.createSimpleFashionOrder(false, pickupPoint);
        orderFactory.receiveOrder(order.getId());

        clock.clearFixed();
        configurationGlobalCommandService.setValue(INCOMPLETE_FASHION_ORDER_THRESHOLD, 15);
        orderDeliveryResultCommandService.startFitting(order.getId());
        Instant createdAt = Instant.now();

        for (int i = 0; i < 5; i++) {
            orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
        }

        clock.setFixed(createdAt.plus(14, ChronoUnit.MINUTES), zone);
        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/incomplete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("order/response_no_incomplete.json"), true));


        clock.setFixed(createdAt.plus(16, ChronoUnit.MINUTES), zone);
        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/incomplete")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_incomplete.json"), order.getId(), order.getExternalId()), true
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testFindByBarcode(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        Order order = createAndReceiveAndVerifyOrder(pickupPoint);

        OffsetDateTime deliveredTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 7, 17, 30, 0),
                zone);
        clock.setFixed(deliveredTime.toInstant(), zone);
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, order.getPaymentType());

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/orders/find-by-barcode" +
                        "?filters=PLACE_BARCODE,EXTERNAL_ID&query=SOME_RANDOM_PLACE," + order.getExternalId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        "[" + getFileContent("order/response_deliver.json") + "]",
                        order.getId(), order.getExternalId(), pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(), order.getCreatedAt()), false
                ));
    }

    private Order createAndReceiveOrder(PickupPoint pickupPoint, OrderType orderType,
                                        OrderPaymentType paymentType, String verificationCode) {
        String externalId = RandomStringUtils.randomAlphanumeric(6);
        return createAndReceiveOrder(pickupPoint, externalId, orderType, paymentType, verificationCode, false);
    }

    private Order createAndReceiveOrder(PickupPoint pickupPoint, String externalId, OrderType orderType,
                                        OrderPaymentType paymentType, String verificationCode, boolean adult) {
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        OffsetDateTime creationTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 5, 17, 50, 0),
                zone);
        clock.setFixed(creationTime.toInstant(), zone);
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId(externalId)
                        .deliveryDate(LocalDate.of(2021, 5, 6))
                        .type(orderType)
                        .paymentType(paymentType)
                        .items(adult ? R18_ITEM : DEFAULT_ITEMS)
                        .places(List.of(
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("P001248514")
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("P160750615")
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("P160758752")
                                        .build()
                        ))
                        .verificationCode(verificationCode)
                        .build())
                .build());
        orderFactory.receiveByCourier(order, COURIER_ID);

        OffsetDateTime arrivedTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 6, 17, 40, 0),
                zone);
        clock.setFixed(arrivedTime.toInstant(), zone);
        return orderFactory.receiveOrder(order.getId());
    }

    private Order createAndReceiveOrder(PickupPoint pickupPoint) {
        return createAndReceiveOrder(
                pickupPoint, OrderType.CLIENT, OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE
        );
    }

    private Order createAndReceiveAndVerifyOrder(PickupPoint pickupPoint, OrderType orderType) {
        var order = createAndReceiveOrder(pickupPoint, orderType, OrderPaymentType.PREPAID, DEFAULT_VERIFICATION_CODE);
        return orderFactory.verifyOrder(order.getId());
    }

    private Order createAndReceiveAndVerifyOrder(PickupPoint pickupPoint) {
        return createAndReceiveAndVerifyOrder(pickupPoint, OrderType.CLIENT);
    }

    private List<Order> createOrders(PickupPoint pickupPoint) {
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).params(
                        TestOrderFactory.OrderParams.builder().cashboxPaymentStatus(CashboxPaymentStatus.PENDING)
                                .build()).build());
        var order2 = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).params(
                        TestOrderFactory.OrderParams.builder().cashboxPaymentStatus(CashboxPaymentStatus.AWAITING_CASHBOX_STATUS)
                                .build()).build());
        var order3 = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).params(
                        TestOrderFactory.OrderParams.builder().cashboxPaymentStatus(CashboxPaymentStatus.SUCCESS)
                                .build()).build());
        return List.of(order, order2, order3);
    }

    @ParameterizedTest
    @CsvSource({"200, 'LERA_TOP', 'PAYMENT'", "200, 'LERA_TOP', 'VERIFICATION_CODE'", "403, 'sadafa', 'PAYMENT'"})
    void commitPaymentStatus(int httpStatus, String callbackToken, OrderDeliveryType deliveryType) throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .externalId("12345")
                        .paymentType(OrderPaymentType.CARD)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(order.getId());
        if (deliveryType == OrderDeliveryType.VERIFICATION_CODE) {
            orderFactory.verifyOrder(order.getId());
        }

        cashboxTransactionRepository.save(new OrderCashboxTransaction(order.getId(), "LERA_TOP", null, null,
                deliveryType, null, null, true, null, "LERA_TOP"));

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/orders/cashbox-payment-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("callback-token", callbackToken)
                        .content(String.format(
                                getFileContent("order/cashbox/cashbox_response_request.json"),
                                order.getExternalId())))
                .andExpect(status().is(httpStatus));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testVerifyCodeEnablesSimplifiedDeliveryForVerifiedOrder(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        List<OrderSimpleParams> orders = createAndReceiveOrderWithSiblings();
        OrderSimpleParams sibling = orders.get(1);

        mockMvc.perform(patch("/v1/pi/pickup-points/{id}/orders/{id}/verify-code",
                        sibling.getPvzMarketId(), sibling.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        getFileContent("order/request_verify_code.json"), DEFAULT_VERIFICATION_CODE)));

        mockMvc.perform(post(String.format("/v1/pi/pickup-points/%d/orders/%d/deliver", sibling.getPvzMarketId(),
                        sibling.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"SIMPLIFIED_DELIVERY\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testVerifyCodeEnablesSimplifiedDelivery(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        List<OrderSimpleParams> orders = createAndReceiveOrderWithSiblings();
        OrderSimpleParams orderWithSiblings = orders.get(0);
        OrderSimpleParams sibling = orders.get(1);
        long orderId = orderWithSiblings.getId();

        mockMvc.perform(get("/v1/pi/pickup-points/{id}/orders/{id}" , orderWithSiblings.getPvzMarketId(), orderId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sf(
                        getFileContent("order/response_get_order_page_with_sibling.json"),
                        orderWithSiblings.getExternalId(), sibling.getExternalId())));

        mockMvc.perform(patch("/v1/pi/pickup-points/{id}/orders/{id}/verify-code",
                        sibling.getPvzMarketId(), sibling.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        getFileContent("order/request_verify_code.json"), DEFAULT_VERIFICATION_CODE)))
                .andExpect(content().json(sf(
                        getFileContent("order/response_get_order_with_simplified.json"),
                        sibling.getExternalId(), orderWithSiblings.getExternalId())));

        mockMvc.perform(get("/v1/pi/pickup-points/{id}/orders/{id}" , orderWithSiblings.getPvzMarketId(), orderId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sf(
                        getFileContent("order/response_get_order_page_with_simplified.json"),
                        orderWithSiblings.getExternalId(), sibling.getExternalId())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testVerifyWrongCodeWithSiblings(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        List<OrderSimpleParams> orders = createAndReceiveOrderWithSiblings();
        OrderSimpleParams orderWithSiblings = orders.get(0);
        OrderSimpleParams sibling = orders.get(1);
        long orderId = orderWithSiblings.getId();

        mockMvc.perform(patch("/v1/pi/pickup-points/{id}/orders/{id}/verify-code",
                        sibling.getPvzMarketId(), sibling.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("order/request_verify_code.json"), "aaaaa")))
                .andExpect(content().json(sf(
                        getFileContent("order/response_verify_wrong_code_with_siblings.json"),
                        sibling.getExternalId(), orderWithSiblings.getExternalId())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SneakyThrows
    void testVerifyBarcodeEnablesSimplifiedDelivery(boolean newOrderPartnerServiceEnabled) {
        configurationGlobalCommandService.setValue(NEW_ORDER_PARTNER_SERVICE_ENABLED, newOrderPartnerServiceEnabled);
        List<OrderSimpleParams> orders = createAndReceiveOrderWithSiblings();
        OrderSimpleParams orderWithSiblings = orders.get(0);
        OrderSimpleParams sibling = orders.get(1);
        long orderId = orderWithSiblings.getId();

        mockMvc.perform(get("/v1/pi/pickup-points/{id}/orders/{id}" , orderWithSiblings.getPvzMarketId(), orderId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sf(
                        getFileContent("order/response_get_order_page_with_sibling.json"),
                        orderWithSiblings.getExternalId(), sibling.getExternalId())));

        String barcode = sibling.getExternalId() + "-" + DEFAULT_VERIFICATION_CODE;
        mockMvc.perform(patch("/v1/pi/pickup-points/{id}/orders/verify-barcode",  sibling.getPvzMarketId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        getFileContent("order/request_verify_barcode.json"), barcode)))
                .andExpect(content().json(sf(
                        getFileContent("order/response_get_order_with_simplified.json"),
                        sibling.getExternalId(), orderWithSiblings.getExternalId())));

        mockMvc.perform(get("/v1/pi/pickup-points/{id}/orders/{id}" , orderWithSiblings.getPvzMarketId(), orderId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sf(
                        getFileContent("order/response_get_order_page_with_simplified.json"),
                        orderWithSiblings.getExternalId(), sibling.getExternalId())));
    }

    private List<OrderSimpleParams> createAndReceiveOrderWithSiblings() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder().build());

        Order orderWithSibling = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        Order siblingOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.receiveOrder(orderWithSibling.getId());
        orderFactory.receiveOrder(siblingOrder.getId());

        return List.of(orderQueryService.getSimple(orderWithSibling.getId()),
                orderQueryService.getSimple(siblingOrder.getId()));
    }
}

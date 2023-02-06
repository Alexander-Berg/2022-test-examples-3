package ru.yandex.market.logistics.lom.controller.order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.Validator;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.delivery.trust.client.model.request.CreateOrderRequest;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.shipment.ShipmentTestUtil;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.jobs.consumer.OrderExternalValidationConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.trust.CreateTrustOrderConsumer;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.lom.utils.OrderFlowUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.utils.LmsFactory.createPartnerResponse;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест для заказов")
class OrderIntegrationTest extends AbstractContextualTest {

    private static final long ORDER_ID = 1L;
    private static final long PARTNER_MARKET_ID = 50;
    private static final long DELIVERY_SERVICE_ID = 48;
    private static final long SORTING_CENTER_ID = 100136;
    private static final String TRANSFER_CODE = "12345";

    private static final OrderIdPayload VALIDATE_ORDER_PAYLOAD = PayloadFactory.createOrderIdPayload(ORDER_ID, 1L);

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private MarketIdService marketIdService;

    @Autowired
    private TrustClient trustClient;

    @Autowired
    private OrderExternalValidationConsumer validationConsumer;

    @Autowired
    private CreateTrustOrderConsumer createTrustOrderConsumer;

    @Autowired
    private Validator validator;

    @Autowired
    private OrderFlowUtils.FlowCreatorFactory flowCreatorFactory;
    private OrderFlowUtils.FlowCreator flowCreator;

    @Autowired
    private TransferCodesService transferCodesService;

    @BeforeEach
    void setup() {
        clock.setFixed(
            LocalDateTime.parse("2020-07-20T12:00").toInstant(ZoneOffset.of("+03:00")),
            DateTimeUtils.MOSCOW_ZONE
        );
        mockExternalValidation();

        when(lmsClient.getLogisticsPoint(1111L)).thenReturn(Optional.of(
            LmsFactory.createLogisticsPointResponse(
                1111L,
                111L,
                "Return warehouse",
                PointType.WAREHOUSE
            )
                .build())
        );

        when(lmsClient.getScheduleDay(1L))
            .thenReturn(Optional.of(new ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(15, 0))));

        flowCreator = flowCreatorFactory.create("LOinttest-1", ORDER_ID, true, true, 3);

        when(lmsClient.searchPartnerRelation(any(), any()))
            .thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
                List.of(PartnerRelationEntityDto.newBuilder()
                    .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(18, 0)).build()))
                    .build())
                )
            );
    }

    @Nonnull
    private static Stream<Arguments> orderSource() {
        return Stream.of(
            Arguments.of(
                ShipmentType.WITHDRAW,
                "controller/order/request/create_withdraw_order.json",
                "controller/order/response/create_withdraw_order.json",
                false
            ),
            Arguments.of(
                ShipmentType.IMPORT,
                "controller/order/request/create_import_order.json",
                "controller/order/response/create_import_order.json",
                false
            ),
            Arguments.of(
                ShipmentType.WITHDRAW,
                "controller/order/request/create_sorting_center_withdraw_order.json",
                "controller/order/response/create_sorting_center_withdraw_order.json",
                true
            )
        );
    }

    @ParameterizedTest
    @DisplayName("Оформление заказа")
    @MethodSource("orderSource")
    void createAndCommitOrder(
        ShipmentType shipmentType,
        String request,
        String response,
        boolean hasSortingCenter
    )
        throws Exception {
        createOrder(request, response);

        Long warehouseTo;
        long warehouseFrom;
        if (hasSortingCenter) {
            warehouseFrom = 10000001756L;
            warehouseTo = 10000001755L;
        } else {
            warehouseFrom = 10000001755L;
            warehouseTo = shipmentType == ShipmentType.IMPORT ? 10000001756L : null;
        }

        validateShipment(shipmentType, warehouseFrom, warehouseTo);
        validateOrderCommit();
        externalValidation();
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

        var returnSegmentId = hasSortingCenter ? 3L : 2L;
        var dsSegmentId = hasSortingCenter ? 2L : 1L;
        var ffSegmentId = hasSortingCenter ? 1L : null;
        flowCreator
            .start(returnSegmentId)
            .createFfOrder(returnSegmentId, 1111, dsSegmentId)
            .createDsOrder(dsSegmentId, 48, ffSegmentId);

        if (hasSortingCenter) {
            flowCreator.createFfOrder(1, 100136);
        }
        validateSendingToDeliveryService(1, false);
        validateDeliveryServiceSuccessResponse(hasSortingCenter);
        validateTrustOrderCreated();
        validateSendingToSortingCenter(hasSortingCenter);

        flowCreator.checkFlow();
    }

    @Test
    @DisplayName("Оформление заказа со смежными сегментами требующими коды верификации")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_transfer_codes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createAndCommitOrderWithTransferCodes()
        throws Exception {

        createOrder(
            "controller/order/request/create_order_with_transfer_codes.json",
            "controller/order/response/create_order_with_transfer_codes.json"
        );

        validateOrderCommit();
        externalValidation();
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

        var returnSegmentId = 3L;
        var dsSegmentId2 = 2L;
        var dsSegmentId1 = 1L;
        flowCreator
            .start(returnSegmentId)
            .createFfOrder(returnSegmentId, 1111, dsSegmentId1)
            .createDsOrder(dsSegmentId1, 49, null)
            .createDsOrder(dsSegmentId2, 50, null);

        validateSendingToDeliveryService(2, true);
        validateDeliveryServiceSuccessResponse(false);
        validateTrustOrderCreated();
        validateSendingToSortingCenter(false);
    }

    private void createOrder(String request, String response) throws Exception {
        OrderTestUtil.createOrder(mockMvc, request)
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(response)));
    }

    private void validateShipment(ShipmentType type, Long warehouseFrom, Long warehouseTo) throws Exception {
        ShipmentTestUtil.searchShipmentByRawRequestResponse(
            mockMvc,
            "{\"marketIdFrom\": 1, \"shipmentType\": \"" + type + "\"}",
            "{\"data\":[{"
                + "\"id\":1,"
                + "\"marketIdFrom\":1,"
                + "\"marketIdTo\":50,"
                + "\"shipmentType\":\"" + type + "\","
                + "\"shipmentDate\":\"2019-06-10\","
                + "\"warehouseFrom\":" + warehouseFrom + ","
                + "\"warehouseTo\":" + warehouseTo + "}]}",
            false,
            null,
            status().isOk()
        );
    }

    private void validateOrderCommit() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(QueueType.VALIDATE_ORDER_EXTERNAL, VALIDATE_ORDER_PAYLOAD);
    }

    private void externalValidation() throws Exception {
        validationConsumer.execute(TaskFactory.createTask(
            queueTaskChecker.getProducedTaskPayload(QueueType.VALIDATE_ORDER_EXTERNAL, OrderIdPayload.class)
        ));
        checkOrderStatus(OrderStatus.ENQUEUED);
    }

    private void validateSendingToDeliveryService(int times, boolean withTransferCodes) throws Exception {
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<ClientRequestMeta> clientRequestMetaCaptor = ArgumentCaptor.forClass(ClientRequestMeta.class);
        ArgumentCaptor<CreateOrderRestrictedData> createOrderRestrictedDataCaptor =
            ArgumentCaptor.forClass(CreateOrderRestrictedData.class);

        verify(deliveryClient, times(times)).createOrder(
            orderCaptor.capture(),
            partnerCaptor.capture(),
            createOrderRestrictedDataCaptor.capture(),
            clientRequestMetaCaptor.capture()
        );

        softly.assertThat(validator.validate(orderCaptor.getValue())).isEmpty();
        softly.assertThat(validator.validate(partnerCaptor.getValue())).isEmpty();
        if (withTransferCodes) {
            validateTransferCodes(createOrderRestrictedDataCaptor);
        } else {
            softly.assertThat(createOrderRestrictedDataCaptor.getValue()).isNull();
        }
        softly.assertThat(validator.validate(clientRequestMetaCaptor.getValue())).isEmpty();

        checkOrderStatus(OrderStatus.PROCESSING);
    }

    private void validateTransferCodes(ArgumentCaptor<CreateOrderRestrictedData> createOrderRestrictedDataCaptor) {
        softly.assertThat(validator.validate(createOrderRestrictedDataCaptor.getAllValues().get(0).getTransferCodes()))
            .isEmpty();
        softly.assertThat(validator.validate(createOrderRestrictedDataCaptor.getAllValues().get(1).getTransferCodes()))
            .isEmpty();
        softly.assertThat(createOrderRestrictedDataCaptor
            .getAllValues()
            .get(0)
            .getTransferCodes()
            .getOutbound()
            .getVerification()
        ).isEqualTo("12345");
        softly.assertThat(createOrderRestrictedDataCaptor
            .getAllValues()
            .get(1)
            .getTransferCodes()
            .getInbound()
            .getVerification()
        ).isEqualTo("12345");
    }

    private void validateTrustOrderCreated() {
        createTrustOrderConsumer.execute(TaskFactory.createTask(
            queueTaskChecker.getProducedTaskPayload(QueueType.CREATE_TRUST_ORDER, OrderIdPayload.class)
        ));

        ArgumentCaptor<CreateOrderRequest> requestCaptor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(trustClient).createOrder(eq("test-token"), requestCaptor.capture());

        CreateOrderRequest request = requestCaptor.getValue();
        softly.assertThat(request.getProductId()).isEqualTo("product-200");
        softly.assertThat(request.getCommission()).isEqualTo(170);
    }

    private void validateDeliveryServiceSuccessResponse(boolean hasSortingCenter) throws Exception {
        String deliveryServiceExternalId = hasSortingCenter ? "waybill[1].externalId" : "waybill[0].externalId";
        checkOrderStatus(OrderStatus.PROCESSING)
            .andExpect(jsonPath(deliveryServiceExternalId).value("ds-external-id"));
    }

    private void validateSendingToSortingCenter(boolean hasSortingCenter) throws Exception {
        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        ArgumentCaptor<ClientRequestMeta> clientRequestMetaCaptor = ArgumentCaptor.forClass(ClientRequestMeta.class);

        var orderCaptor = ArgumentCaptor.forClass(
            ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class
        );
        int times = hasSortingCenter ? 2 : 1;
        verify(fulfillmentClient, times(times)).createOrder(
            orderCaptor.capture(),
            partnerCaptor.capture(),
            eq(null),
            clientRequestMetaCaptor.capture()
        );

        partnerCaptor.getAllValues().forEach(partner -> softly.assertThat(validator.validate(partner)).isEmpty());
        clientRequestMetaCaptor.getAllValues().forEach(clientRequestMeta ->
            softly.assertThat(validator.validate(clientRequestMeta)).isEmpty());
        orderCaptor.getAllValues().forEach(order -> softly.assertThat(validator.validate(order)).isEmpty());

        checkOrderStatus(OrderStatus.PROCESSING);
    }

    private ResultActions checkOrderStatus(OrderStatus status) throws Exception {
        return OrderTestUtil.getOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value(status.name()));
    }

    private void mockExternalValidation() {
        when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of(
            LmsFactory.createLogisticsPointResponse(10000001755L, 2L, "warehouse1", PointType.WAREHOUSE).build(),
            LmsFactory.createLogisticsPointResponse(10000001756L, 3L, "warehouse2", PointType.WAREHOUSE).build()
        ));

        List<PartnerResponse> partnerResponseList = List.of(
            createPartnerResponse(DELIVERY_SERVICE_ID, PARTNER_MARKET_ID),
            createPartnerResponse(1111, "SC", PARTNER_MARKET_ID),
            createPartnerResponse(2, "D", PARTNER_MARKET_ID),
            createPartnerResponse(3, "SC", PARTNER_MARKET_ID),
            createPartnerResponse(SORTING_CENTER_ID, PARTNER_MARKET_ID, PartnerType.SORTING_CENTER),
            createPartnerResponse(49, PARTNER_MARKET_ID, PartnerType.DELIVERY,
                new PartnerExternalParam("OUTBOUND_VERIFICATION_CODE_REQUIRED", null, "true"),
                new PartnerExternalParam("INBOUND_VERIFICATION_CODE_REQUIRED", null, "true")
            ),
            createPartnerResponse(50, PARTNER_MARKET_ID, PartnerType.DELIVERY,
                new PartnerExternalParam("OUTBOUND_VERIFICATION_CODE_REQUIRED", null, "true"),
                new PartnerExternalParam("INBOUND_VERIFICATION_CODE_REQUIRED", null, "true")
            )
        );

        when(transferCodesService.generateCode()).thenReturn(TRANSFER_CODE);

        when(lmsClient.searchPartners(any())).thenReturn(partnerResponseList);
        when(marketIdService.findAccountById(1))
            .thenReturn(Optional.of(MarketIdFactory.marketAccount()));

        partnerResponseList.forEach(r -> when(lmsClient.getPartner(eq(r.getId()))).thenReturn(Optional.of(r)));

        when(marketIdService.findAccountById(PARTNER_MARKET_ID))
            .thenReturn(Optional.of(
                MarketIdFactory.marketAccount(PARTNER_MARKET_ID, MarketIdFactory.anotherLegalInfoBuilder().build()))
            );
    }
}

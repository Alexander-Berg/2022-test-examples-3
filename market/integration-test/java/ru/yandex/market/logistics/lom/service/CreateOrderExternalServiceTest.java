package ru.yandex.market.logistics.lom.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Supplier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnType;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.DeliveryServiceCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.order.create.DeliveryServiceCreateOrderExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.order.create.FulfillmentCreateOrderExternalService;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.OrderFlowUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createOrderRestrictedDataWithPromise;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartnerRelationFilter;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createBarcodeResourceIdNew;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createCourierLocationTo;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createDsRestrictedData;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createFfRestrictedData;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createItemBuilder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrderDeliveryTax;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrderFullyPrepaid;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrderGoPlatform;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrderWithTariff;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createRecipientLocation;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createDelivery;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createResourceId;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createScOrderWithTariff;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class CreateOrderExternalServiceTest extends AbstractContextualYdbTest {

    private static final ClientRequestMeta EXPECTED_CLIENT_REQUEST_META = new ClientRequestMeta("1");
    private static final UUID ROUTE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private OrderFlowUtils.FlowCreatorFactory flowCreatorFactory;
    private OrderFlowUtils.FlowCreator flowCreatorDaas;
    private OrderFlowUtils.FlowCreator flowCreatorBeru;

    @Autowired
    private DeliveryServiceCreateOrderExternalService deliveryServiceCreateOrderExternalService;

    @Autowired
    private DeliveryServiceCreateOrderExternalConsumer deliveryServiceCreateOrderExternalConsumer;

    @Autowired
    private FulfillmentCreateOrderExternalService fulfillmentCreateOrderExternalService;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private LMSClient lmsClient;

    @Captor
    private ArgumentCaptor<Order> deliveryOrderCaptor;

    @Captor
    private ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> fulfillmentOrderCaptor;

    private boolean checkFlow = true;

    @Autowired
    private OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTableDescription;

    @Autowired
    private OrderCombinedRouteHistoryYdbConverter routeHistoryConverter;

    @Autowired
    private OrderCombinedRouteHistoryYdbRepository newRouteRepository;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTableDescription);
    }

    @BeforeEach
    void setup() {
        flowCreatorDaas = flowCreatorFactory.create(
            "2-LOinttest-1",
            1L,
            true,
            true,
            1
        );

        flowCreatorBeru = flowCreatorFactory.create(
            "2-LOinttest-1",
            1L,
            false,
            false,
            1
        );

        when(lmsClient.searchPartnerRelation(any(), any()))
            .thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
                    List.of(PartnerRelationEntityDto.newBuilder()
                        .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(18, 0)).build()))
                        .build())
                )
            );
    }

    @AfterEach
    void tearDown() {
        if (checkFlow) {
            flowCreatorDaas.checkFlow();
            flowCreatorBeru.checkFlow();
        }
        checkFlow = true;
        clock.clearFixed();
        verifyNoMoreInteractions(deliveryClient, fulfillmentClient, lmsClient, newRouteRepository);
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД), объявленная ценность < 1000")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_assessed_value_999.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessAssessedValue999() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder().setAssessedCost(BigDecimal.valueOf(999)).build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД)")
    @DatabaseSetup("/service/externalvalidation/before/creating_order.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccess() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );

    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД) с GO сегментом")
    @DatabaseSetup(
        value = {
            "/service/externalvalidation/before/creating_order_with_go_segment.xml",
            "/service/common/before/order_items_units.xml"
        }
    )
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessWithGoSegment() throws GatewayApiException, IOException {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(
                new CombinedRoute()
                    .setOrderId(1L)
                    .setSourceRoute(objectMapper.readTree(extractFileContent(
                        "controller/order/combined/combined_route_for_order_with_promise.json"
                    )))
                    .setRouteUuid(ROUTE_UUID)
            ),
            routeHistoryConverter::mapToItem
        );
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            createOrderGoPlatform().build(),
            createPartner(),
            createOrderRestrictedDataWithPromise("promise"),
            EXPECTED_CLIENT_REQUEST_META
        );

        verify(newRouteRepository, times(1)).getRouteByUuid(ROUTE_UUID);
    }

    @Test
    @DisplayName("Ошибка создания заказа в LGW (СД) с GO сегментом. Отсутствует route.")
    @DatabaseSetup(
        value = {
            "/service/externalvalidation/before/creating_order_with_go_segment.xml",
            "/service/common/before/order_items_units.xml"
        }
    )
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/creating_order_with_go_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingFaultWithGoSegment() {
        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L)
            )))
            .isEqualTo(TaskExecutionResult.finish());
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_CREATE_ORDER);
    }

    @Test
    @DisplayName(
        "Правильное заполнение параметра locationTo заказа с самопривозом из магазина в СД и курьерской доставкой"
    )
    @DatabaseSetup("/service/externalvalidation/before/creating_order_ds_import_courier.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingImportToDeliveryServiceOrderLocationTo() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).createOrder(
            orderCaptor.capture(),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        softly.assertThat(orderCaptor.getValue().getLocationTo())
            .usingRecursiveComparison()
            .isEqualTo(createRecipientLocation());
    }

    @Test
    @DisplayName("Правильное заполнение параметра locationTo заказа с отсутствующим locationTo сегмента")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_ds_withdraw_courier_no_location_to.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingWithdrawToDeliveryServiceNoLocationTo() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(deliveryClient).createOrder(
            orderCaptor.capture(),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        softly.assertThat(orderCaptor.getValue().getLocationTo())
            .usingRecursiveComparison()
            .isEqualTo(createRecipientLocation());

    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД) для заказа Беру, без Траста и places")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_beru.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order_beru.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessForBeru() throws GatewayApiException {
        flowCreatorBeru.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder().setPlaces(null).build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Создание заказа в LGW (СД) для отменённого заказа")
    @DatabaseSetup("/service/externalvalidation/before/creating_cancelled_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/creating_cancelled_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingForCancelledOrder() {
        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L)
            )))
            .isEqualTo(TaskExecutionResult.finish());
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_CREATE_ORDER);
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД), нет данных о поставщике")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_without_supplier_info.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order_with_supplier_inn.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessWithoutSupplierInfo() throws GatewayApiException {
        flowCreatorBeru.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder(
                createBarcodeResourceIdNew(),
                List.of(createItemBuilder(1).setSupplier(null).build())
            )
                .setPlaces(null)
                .build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД), у поставщика заполнен только ИНН")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_supplier_inn_only.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order_with_supplier_inn.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessWithSupplierInn() throws GatewayApiException {
        flowCreatorBeru.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder(
                createBarcodeResourceIdNew(),
                List.of(createItemBuilder(1).setSupplier(Supplier.builder().setInn("1231231234").build()).build())
            )
                .setPlaces(null)
                .build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД) с налогом на услугу доставки")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_delivery_tax.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessDeliveryTax() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrderDeliveryTax().build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД) с фиксированной стоимостью доставки для покупателя")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_fixed_customer_delivery_cost.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessWithFixedCustomerCost() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Удачное создание заказа в LGW (СД) с полной предоплатой")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_fully_prepaid.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessFullyPrepaid() throws GatewayApiException {
        flowCreatorDaas.start(1).createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrderFullyPrepaid().build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Неудачное создание заказа из-за ошибки LGW (СД). Финальное падение")
    @DatabaseSetup("/service/externalvalidation/before/creating_order.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingFinalFailure() throws GatewayApiException {
        doThrow(new GatewayApiException("Exception")).when(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );
        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L)
            )))
            .isEqualTo(TaskExecutionResult.finish());
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_CREATE_ORDER);
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );
    }

    @Test
    @DisplayName("Неудачное создание заказа из-за ошибки LGW (СД). Нефинальное падение")
    @DatabaseSetup("/service/externalvalidation/before/creating_order.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingFailure() throws GatewayApiException {
        doThrow(new GatewayApiException("Exception")).when(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );
        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L),
                2
            )))
            .isEqualTo(TaskExecutionResult.fail());
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_CREATE_ORDER);
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );
    }

    @Test
    @DisplayName("Неудачное создание заказа из-за ошибки LGW (СД). Ретраи до финального падения")
    @DatabaseSetup("/service/externalvalidation/before/creating_order.xml")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/order_processing_error.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_creating_order_with_retries.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingFailureRetry() throws GatewayApiException {
        doThrow(new GatewayApiException("Exception")).when(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );

        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L),
                1
            )))
            .isEqualTo(TaskExecutionResult.fail());

        clock.setFixed(Instant.parse("2019-06-12T01:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L)
            )))
            .isEqualTo(TaskExecutionResult.finish());
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_CREATE_ORDER);

        verify(deliveryClient, times(2)).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );
    }

    @Test
    @DisplayName("Неудачное, затем удачное создание заказа из-за ошибки LGW (СД).")
    @DatabaseSetup("/service/externalvalidation/before/creating_order.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/successful_retry_creating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCreatingSuccessfulRetry() throws GatewayApiException {
        doThrow(new GatewayApiException("Exception"))
            .doNothing()
            .when(deliveryClient).createOrder(
                eq(createOrder().build()),
                eq(createPartner()),
                eq(null),
                eq(new ClientRequestMeta("1001"))
            );

        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L),
                1
            )))
            .isEqualTo(TaskExecutionResult.fail());

        clock.setFixed(Instant.parse("2019-06-12T01:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);

        softly.assertThat(deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                createWaybillSegmentPayload(1L, 1L, "1001", 1001L)
            )))
            .isEqualTo(TaskExecutionResult.finish());

        verify(deliveryClient, times(2)).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1001"))
        );
    }

    @Test
    @DisplayName("Удачное создание заказов в СД и СЦ через LGW")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void validateTwoStepExternalOrderCreationSuccess() throws Exception {
        flowCreatorBeru
            .start(2)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrderWithoutPartnerLogisticPoint().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("5"))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Удачное создание заказа с кодом тарифа в СД и СЦ через LGW")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff.xml")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/order_with_tariff_code.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void validateTwoStepExternalOrderWithTariffCodeCreationSuccess() throws Exception {
        flowCreatorBeru
            .start(2)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);
        verify(deliveryClient).createOrder(
            eq(createOrderWithTariff("custom-tariff-code").build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(fulfillmentClient).createOrder(
            eq(createScOrderWithTariff("custom-tariff-code").build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("5"))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Удачное создание заказов в СД и СЦ через LGW + возвратный сц")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff_return.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void validateTwoStepExternalOrderCreationSuccessReturnSc() throws Exception {
        flowCreatorBeru
            .start(3)
            .createFfOrder(3, 48, 2L)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseOrder(null)),
            eq(createPartner(48L)),
            eq(null),
            eq(new ClientRequestMeta("1"))
        );
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("8"))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа с сегментом типа FULFILLMENT для партнёра типа DROPSHIP через LGW")
    @DatabaseSetup(
        "/service/externalvalidation/before/creating_order_with_segment_type_fulfillment_partner_type_dropship.xml"
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessFulfillmentSegmentDropshipPartner() throws Exception {
        long dropshipPartnerId = 47755L;
        String dropshipPartnerIdString = String.valueOf(dropshipPartnerId);
        flowCreatorBeru
            .start(1)
            .createFfOrder(1, dropshipPartnerId, null, PartnerType.DROPSHIP)
            .start(4)
            .createFfOrder(4, 48, 3L)
            .createDsOrder(3, 20, 2L)
            .createFfOrder(2, 21);

        verify(fulfillmentClient).createOrder(
            eq(
                CreateLgwFulfillmentEntitiesUtils.createDropshipOrder(dropshipPartnerIdString)
                    .setTags(Set.of("EXPRESS"))
                    .build()
            ),
            eq(createPartner(dropshipPartnerId)),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseOrder(dropshipPartnerIdString)),
            eq(createPartner(48L)),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("7"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrderWithSenderPartnerId(dropshipPartnerIdString).build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("11"))
        );

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(47755, 21)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа с роутом с сегментом типа FULFILLMENT для партнёра типа DROPSHIP через LGW")
    @DatabaseSetup(
        "/service/externalvalidation/before/creating_order_with_tag_with_partner_type_dropship.xml"
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderWithTagSuccessFulfillmentSegmentDropshipPartner() throws Exception {
        flowCreatorBeru
            .start(1)
            .createFfOrder(1, 47755, null, PartnerType.DROPSHIP)
            .start(4)
            .createFfOrder(4, 48, 3L)
            .createDsOrder(3, 20, 2L)
            .createFfOrder(2, 21);

        verify(fulfillmentClient).createOrder(
            fulfillmentOrderCaptor.capture(),
            eq(createPartner(47755L)),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );

        verify(fulfillmentClient).createOrder(
            fulfillmentOrderCaptor.capture(),
            eq(createPartner(48L)),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );

        softly.assertThat(fulfillmentOrderCaptor.getValue().getWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(CreateLgwFulfillmentEntitiesUtils.createShopWarehouse().build());

        verify(deliveryClient).createOrder(
            deliveryOrderCaptor.capture(),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("7"))
        );

        softly.assertThat(deliveryOrderCaptor.getValue().getWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(CreateLgwDeliveryEntitiesUtils.createReturnWarehouse().build());

        verify(fulfillmentClient).createOrder(
            fulfillmentOrderCaptor.capture(),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("11"))
        );

        softly.assertThat(fulfillmentOrderCaptor.getValue().getWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(CreateLgwFulfillmentEntitiesUtils.createShopWarehouse().build());

        softly.assertThat(fulfillmentOrderCaptor.getValue().getWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(CreateLgwFulfillmentEntitiesUtils.createShopWarehouse().build());

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(47755, 21)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание для дбс")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_dbs_pvz.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderDbsPvz() throws GatewayApiException {
        flowCreatorBeru
            .start(1)
            .createDbsOrder(1)
            .start(2)
            .createDsOrder(2, 21);

        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("2"))
        );
    }

    @Test
    @DisplayName("Успешное создание заказа с сегментом типа FULFILLMENT для партнёра типа FULFILLMENT через LGW")
    @DatabaseSetup(
        "/service/externalvalidation/before/creating_order_with_segment_type_fulfillment_partner_type_fulfillment.xml"
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessFulfillmentSegmentFulfillmentPartner() throws Exception {
        clock.setFixed(Instant.parse("2019-06-11T19:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.searchPartnerRelation(eq(createPartnerRelationFilter(172, 21)), eq(new PageRequest(0, 1))))
            .thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
                    List.of(PartnerRelationEntityDto.newBuilder()
                        .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(18, 0)).build()))
                        .build())
                )
            );

        flowCreatorBeru
            .start(4)
            .createFfOrder(4, 48, 3L)
            .createDsOrder(3, 20, 2L)
            .createFfOrder(2, 21, 1L)
            .createFfOrder(1, 172);
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseOrder(null)),
            eq(createPartner(48L)),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("8"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createFfOrder(null).build()),
            eq(createPartner(172L)),
            eq(null),
            eq(new ClientRequestMeta("11"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_FF_ORDER_AFTER_CUTOFF\t" +
                "payload=Creating FF order after cutoff time and shipment date being today or tomorrow\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/3\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:2,lom_order:1\t" +
                "extra_keys=partnerSegment,deliveryServiceId,warehouseId,cutoffTime,ffSegment,shipmentDate\t" +
                "extra_values=2,21,172,18:00,1,2019-06-11"
            ));

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_FF_ORDER_AFTER_CUTOFF_SHIPMENT_DT\t" +
                "payload=Creating FF order after ff segment shipment date time\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/3\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:2,lom_order:1\t" +
                "extra_keys=partnerSegment,deliveryServiceId,warehouseId,cutoffTime,createdTime,ffSegment,"
                + "shipmentDate,shipmentDateTime\t" +
                "extra_values=2,21,172,18:00,2021-06-11T14:00,1,2019-06-11,2019-06-11T10:54"
            ));

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(172, 21)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа ДО катофа с сегментом типа FULFILLMENT через LGW ")
    @DatabaseSetup(
        "/service/externalvalidation/before/creating_order_with_segment_type_fulfillment_partner_type_fulfillment.xml"
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessFulfillmentSegmentFulfillmentPartnerBeforeCutoff() throws Exception {
        clock.setFixed(Instant.parse("2019-06-11T19:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.searchPartnerRelation(eq(createPartnerRelationFilter(172, 21)), eq(new PageRequest(0, 1))))
            .thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
                    List.of(PartnerRelationEntityDto.newBuilder()
                        .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(3, 0)).build()))
                        .build())
                )
            );

        flowCreatorBeru
            .start(4)
            .createFfOrder(4, 48, 3L)
            .createDsOrder(3, 20, 2L)
            .createFfOrder(2, 21, 1L)
            .createFfOrder(1, 172);
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseOrder(null)),
            eq(createPartner(48L)),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("8"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createFfOrder(null).build()),
            eq(createPartner(172L)),
            eq(null),
            eq(new ClientRequestMeta("11"))
        );
        softly.assertThat(backLogCaptor.getResults())
            .noneMatch(line -> line.contains("level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_FF_ORDER_AFTER_CUTOFF\t" +
                "payload=Creating FF order after cutoff time and shipment date being today or tomorrow\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/3\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:2,lom_order:1\t" +
                "extra_keys=partnerSegment,deliveryServiceId,warehouseId,cutoffTime,ffSegment,shipmentDate\t" +
                "extra_values=2,21,172,03:00,1,2019-06-11"
            ));

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(172, 21)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа FULFILLMENT ДО катофа через LGW склад с дефолтным временем смены суток")
    @DatabaseSetup({
        "/service/externalvalidation/before/creating_order_with_segment_type_fulfillment_partner_type_ff_173.xml",
        "/service/common/before/order_items_units.xml"
    })
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessFulfillmentSegmentFulfillmentPartnerBeforeCutoffWithDefaultOffset() throws Exception {
        int nonExistingPartner = 173;
        clock.setFixed(Instant.parse("2019-06-11T19:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.searchPartnerRelation(
            eq(createPartnerRelationFilter(nonExistingPartner, 21)),
            eq(new PageRequest(0, 1))
        ))
            .thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
                    List.of(PartnerRelationEntityDto.newBuilder()
                        .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(3, 0)).build()))
                        .build())
                )
            );

        flowCreatorBeru
            .start(4)
            .createFfOrder(4, 48, 3L)
            .createDsOrder(3, 20, 2L)
            .createFfOrder(2, 21, 1L)
            .createFfOrder(1, nonExistingPartner);
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseOrder(null)),
            eq(createPartner(48L)),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("8"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createFfOrder(null).build()),
            eq(createPartner(nonExistingPartner)),
            eq(null),
            eq(new ClientRequestMeta("11"))
        );
        softly.assertThat(backLogCaptor.getResults())
            .noneMatch(line -> line.contains("level=INFO\t" +
                "format=plain\t" +
                "code=CREATE_FF_ORDER_AFTER_CUTOFF\t" +
                "payload=Creating FF order after cutoff time and shipment date being today or tomorrow\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/3\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:2,lom_order:1\t" +
                "extra_keys=partnerSegment,deliveryServiceId,warehouseId,cutoffTime,ffSegment,shipmentDate\t" +
                "extra_values=2,21,173,03:00,1,2019-06-11"
            ));

        verify(lmsClient).searchPartnerRelation(
            eq(createPartnerRelationFilter(nonExistingPartner, 21)), eq(new PageRequest(0, 1))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа с сегментом типа PICKUP через LGW")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_segment_type_pickup.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessPickupSegment() throws Exception {
        flowCreatorBeru
            .start(3)
            .createFfOrder(3, 48, 2L)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);

        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseOrder(null)),
            eq(createPartner(48L)),
            eq(null),
            eq(new ClientRequestMeta("1"))
        );
        verify(deliveryClient).createOrder(
            eq(
                createOrder()
                    .setPickupPointCode("pickup-point-code-1234567890")
                    .setPickupPointId(
                        ResourceId.builder()
                            .setYandexId("1234567890")
                            .setPartnerId("pickup-point-code-1234567890")
                            .build()
                    )
                    .setShipmentDate(null)
                    .build()
            ),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("4"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("8"))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа с сегментом типа COURIER через LGW")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_segment_type_courier.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessCourierSegment() throws Exception {
        flowCreatorBeru
            .start(2)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);
        verify(deliveryClient).createOrder(
            eq(
                createOrder(DeliveryType.COURIER, createCourierLocationTo().build())
                    .setShipmentDate(null)
                    .setPickupPointCode(null)
                    .setPickupPointId(null)
                    .build()
            ),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(fulfillmentClient).createOrder(
            eq(
                CreateLgwFulfillmentEntitiesUtils.createScOrder(
                        ru.yandex.market.logistic.gateway.common.model.fulfillment.DeliveryType.COURIER
                    )
                    .setPickupPointCode(null)
                    .build()
            ),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("5"))
        );

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Успешное создание заказа с единственным сегментом типа COURIER через LGW")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_segment_type_courier_single.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessSingleCourierSegment() throws Exception {
        flowCreatorBeru
            .start(1)
            .createDsOrder(1, 20);
        verify(deliveryClient).createOrder(
            eq(createOrder(DeliveryType.COURIER, createCourierLocationTo().build())
                .setShipmentDate(null)
                .setPickupPointCode(null)
                .setPickupPointId(null)
                .build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Успешное создание заказа с двумя сегментами с партнёрами типа DELIVERY через LGW")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_two_delivery_segments.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void commitOrderSuccessTwoDeliverySegments() throws Exception {
        flowCreatorBeru
            .start(3)
            .createDsOrder(3, 22, 2L)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);
        verify(deliveryClient).createOrder(
            eq(createOrder(PaymentMethod.CARD).setShipmentDate(null).build()),
            eq(createPartner(22L)),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(deliveryClient).createOrder(
            eq(createOrder(PaymentMethod.PREPAID, DeliveryType.COURIER).setShipmentDate(null).build()),
            eq(createPartner(20L)),
            eq(null),
            eq(new ClientRequestMeta("5"))
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createFfOrder().build()),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("9"))
        );

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание уже созданного заказа")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff_return.xml")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/waybill_segment_with_external_id.xml",
        type = DatabaseOperation.UPDATE
    )
    void createAlreadyCreatedOrder() {
        var payload = PayloadFactory.createWaybillSegmentPayload(1L, 2L, "1");

        softly.assertThat(deliveryServiceCreateOrderExternalService.processPayload(Objects.requireNonNull(payload)))
            .isEqualTo(ProcessingResult.unprocessed(
                "Create order (1) restricted. Order has already been created in segment (2)."
            ));

        verifyZeroInteractions(deliveryClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание заказа при активной отмене")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff_return.xml")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/active_cancellation_request.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderWithActiveCancellation() {
        var payload = PayloadFactory.createWaybillSegmentPayload(1L, 2L, "1");

        softly.assertThat(deliveryServiceCreateOrderExternalService.processPayload(Objects.requireNonNull(payload)))
            .isEqualTo(ProcessingResult.unprocessed(
                "Create order (1) restricted. Active cancellation request (segment 2)."
            ));

        verifyZeroInteractions(deliveryClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание заказа при активной отмене всего заказа")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff_return.xml")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/success_order_cancellation_request.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderWithActiveOrderCancellation() {
        var payload = PayloadFactory.createWaybillSegmentPayload(1L, 1L, "1");

        softly.assertThat(fulfillmentCreateOrderExternalService.processPayload(Objects.requireNonNull(payload)))
            .isEqualTo(ProcessingResult.unprocessed(
                "Create order (1) restricted. Active cancellation request (segment 1)."
            ));

        verifyZeroInteractions(deliveryClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание возвратного сегмента при активной отмене всего заказа")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff_return.xml")
    @DatabaseSetup(
        value = "/service/externalvalidation/before/success_order_cancellation_request.xml",
        type = DatabaseOperation.INSERT
    )
    void createReturnWaybillSegmentWithActiveOrderCancellation() {
        var payload = PayloadFactory.createWaybillSegmentPayload(1L, 3L, "1");
        fulfillmentCreateOrderExternalService.processPayload(Objects.requireNonNull(payload));

        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> captor =
            ArgumentCaptor.forClass(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class);

        verify(fulfillmentClient).createOrder(
            captor.capture(),
            eq(createPartner(48L)),
            eq(null),
            eq(new ClientRequestMeta("1"))
        );

        softly.assertThat(captor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(
                CreateLgwFulfillmentEntitiesUtils.createOrder(
                        CreateLgwFulfillmentEntitiesUtils.createBarcodeResourceIdNew(),
                        null,
                        ru.yandex.market.logistic.gateway.common.model.fulfillment.DeliveryType.PICKUP_POINT,
                        CreateLgwFulfillmentEntitiesUtils.createReturnWarehouse().build(),
                        CreateLgwFulfillmentEntitiesUtils.createShopWarehouse().build(),
                        CreateLgwFulfillmentEntitiesUtils.createReturnFfLocation().build(),
                        CreateLgwFulfillmentEntitiesUtils.createShopLocation().build(),
                        null,
                        CreateLgwFulfillmentEntitiesUtils.createReturnWarehouseResourceId(),
                        null
                    )
                    .setExternalId(CreateLgwFulfillmentEntitiesUtils.createBarcodeResourceIdNew().build())
                    .setKorobyte(null)
                    .setShipmentDate(new DateTime("2019-06-11T00:00:00+03:00"))
                    .setShipmentDateTime(new DateTime("2019-06-11T09:21:00+03:00"))
                    .setPlaces(null)
                    .build()
            );

        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание заказа при завершённой отмене")
    @DatabaseSetup("/service/externalvalidation/before/cancelled_order.xml")
    void createOrderWithSuccessCancellation() {
        var payload = PayloadFactory.createWaybillSegmentPayload(1L, 1L, "1");

        softly.assertThat(deliveryServiceCreateOrderExternalService.processPayload(Objects.requireNonNull(payload)))
            .isEqualTo(ProcessingResult.unprocessed(
                "Create order (1) restricted. Active cancellation request (segment 1)."
            ));

        verifyZeroInteractions(deliveryClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Создание заказа при активной отмене другого заказа и того же партнера")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/externalvalidation/before/cancelling_order_2.xml",
        type = DatabaseOperation.INSERT
    )
    void createOrderWithActiveCancellationAnotherOrder() throws GatewayApiException {
        flowCreatorBeru.start(2).createDsOrder(2, 20);

        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Удачное создание заказа с требованием кодов валидации")
    @DatabaseSetup("/service/externalvalidation/before/creating_order_with_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_transfer_codes.xml",
        type = DatabaseOperation.UPDATE
    )
    void validateCreatingSuccessWithTransferCodes() throws GatewayApiException {
        flowCreatorBeru
            .start(2)
            .createDsOrder(2, 20, 1L)
            .createFfOrder(1, 21);
        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(createDsRestrictedData()),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils.createScOrderWithoutPartnerLogisticPoint().build()),
            eq(createPartner(21L)),
            eq(createFfRestrictedData()),
            eq(new ClientRequestMeta("5"))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 20)), eq(new PageRequest(0, 1)));
    }

    @Test
    @DisplayName("Удачное создание заказов ЯДо")
    @DatabaseSetup("/service/externalvalidation/before/creating_yado_order.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void validateCreatingYadoOrder() throws Exception {
        flowCreatorDaas
            .start(3)
            .createDsOrder(3, 20, 2L)
            .createFfOrder(2, 48, 1L)
            .createFfOrder(1, 21);

        verify(deliveryClient).createOrder(
            eq(createOrder().build()),
            eq(createPartner()),
            eq(null),
            eq(new ClientRequestMeta("1"))
        );

        verify(fulfillmentClient).createOrder(
            eq(CreateLgwFulfillmentEntitiesUtils
                .createYadoMidScOrder()
                .setReturnInfo(
                    new ReturnInfo(
                        new PartnerInfo("21", "sc-credentials-incorporation"),
                        null,
                        ReturnType.WAREHOUSE
                    )
                )
                .build()
            ),
            eq(createPartner(48L)),
            eq(null),
            eq(new ClientRequestMeta("7"))
        );

        verify(fulfillmentClient).createOrder(
            eq(
                CreateLgwFulfillmentEntitiesUtils
                    .createYadoMidScOrder(createDelivery("сортировочный центр твоего парсела", "48"))
                    .setReturnInfo(
                        new ReturnInfo(
                            new PartnerInfo("1", "sender-name"),
                            null,
                            ReturnType.SHOP
                        )
                    )
                    .setExternalId(createResourceId("2-LOinttest-1", "1001").build())
                    .build()
            ),
            eq(createPartner(21L)),
            eq(null),
            eq(new ClientRequestMeta("10"))
        );
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(48, 20)), eq(new PageRequest(0, 1)));
        verify(lmsClient).searchPartnerRelation(eq(createPartnerRelationFilter(21, 48)), eq(new PageRequest(0, 1)));
    }
}

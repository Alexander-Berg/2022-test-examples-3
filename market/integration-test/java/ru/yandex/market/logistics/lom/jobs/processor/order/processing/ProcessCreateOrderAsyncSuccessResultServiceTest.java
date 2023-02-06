package ru.yandex.market.logistics.lom.jobs.processor.order.processing;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.exception.http.InappropriateOrderStateException;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.exception.http.base.BadRequestException;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderSuccessPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.client.LMSClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.LmsFactory.createPickupPointResponseBuilder;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createOrder;

class ProcessCreateOrderAsyncSuccessResultServiceTest extends AbstractBusinessProcessStateYdbServiceTest {

    @Autowired
    private ProcessCreateOrderAsyncSuccessResultService processCreateOrderAsyncSuccessResultService;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository ydbHistoryRepository;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState, с еще незаполненным externalId.
     * После теста у сегмента появляется значение externalId. История изменений сохраняется.
     */
    @Test
    @DisplayName("Успешный сценарий для СД")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccess() {
        processDefault();

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1009L, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(-1L)
                    .setSequenceId(1009L)
                    .setStatus(BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)
                    .setCreated(clock.instant())
            ));
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState, с еще незаполненным externalId.
     * Для этого сегмента существует заявка на отмену.
     * После теста у сегмента появляется значение externalId. История изменений сохраняется.
     * Появляется задача на отмену.
     */
    @Test
    @DisplayName("Успешный сценарий для СД, есть заявка на отмену на сегменте")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/controller/order/processing/create/success/before/segment_cancellation_request.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_with_cancellation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessWithCancellation() {
        processDefault();

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1009L, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(-1L)
                    .setSequenceId(1009L)
                    .setStatus(BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)
                    .setCreated(clock.instant())
            ));
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState, с еще незаполненным externalId.
     * Для этого сегмента существует заявка на изменение, в статусе WAITING_FOR_PROCESSING_AVAILABILITY.
     * После теста у сегмента появляется значение externalId. История изменений сохраняется.
     * Появляется задача на обработку заявки на изменение, заявка переводится в PROCESSING.
     */
    @Test
    @DisplayName("Запуск обработки заявки на изменение сегмента, ожидающей создания в партнере")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/controller/order/processing/create/success/before/segment_change_request.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_with_change_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessWithChangeRequest() {
        processDefault();

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1009L, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(-1L)
                    .setSequenceId(1009L)
                    .setStatus(BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)
                    .setCreated(clock.instant())
            ));
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует PREPARING сегмент путевого листа с еще незаполненным externalId.
     * Для этого сегмента существует заявка на изменение товаров, в статусе WAITING_FOR_PROCESSING_AVAILABILITY.
     * После создания заказа у сегмента появляется значение externalId. История изменений сохраняется.
     * Появляется задача на обработку заявки на изменение, заявка переводится в PROCESSING.
     */
    @Test
    @DisplayName("Запуск обработки заявки на обновление товаров на сегменте, ожидающем создания в партнере")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/controller/order/processing/create/success/before/update_items_segment_request.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_with_update_items_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessWithOrderChangedByPartnerSegmentRequest() {
        processDefault();

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1009L, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(-1L)
                    .setSequenceId(1009L)
                    .setStatus(BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)
                    .setCreated(clock.instant())
            ));
    }

    /**
     * В заказе существует PICKUP сегмент с еще незаполненным externalId в статусе PREPARING.
     * Для этого сегмента существует заявка на изменение типа доставки на ПВЗ.
     * После создания заказа у сегмента появляется externalId и заявка переходит в статус
     * SUCCESS_RESPONSE_PROCESSING_SUCCEEDED. Создается задача REGISTER_DELIVERY_TRACK.
     * Возобновляется обработка заявки на изменения типа доставки для сегмента MOVEMENT.
     */
    @Test
    @DisplayName("Запуск обработки заявки на изменение типа доставки на ПВЗ после создания заказа в партнере")
    @DatabaseSetup({
        "/controller/order/processing/change_last_mile_to_pickup/setup.xml",
        "/controller/order/processing/change_last_mile_to_pickup/create/before/segment_request.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/"
            + "order_create_success_change_last_mile_to_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createOrderSuccessWithChangeLastMileToPickupSegmentRequest() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                50L,
                4L,
                1L,
                new CreateOrderSuccessDto(
                    "pickup-external-id",
                    50L,
                    "1001",
                    10L
                )
            )
        );
    }

    /**
     * В заказе существует PICKUP сегмент с еще незаполненным externalId в статусе PREPARING.
     * Для этого сегмента существует заявка на изменение последней мили с самовывоза на самовывоз.
     * После создания заказа у сегмента появляется externalId и заявка переходит в статус
     * SUCCESS_RESPONSE_PROCESSING_SUCCEEDED. Создается задача REGISTER_DELIVERY_TRACK.
     * Возобновляется обработка заявки для сегмента MOVEMENT.
     * Заявка для сегмента бывшей последней мили PICKUP все еще ожидает обработки.
     */
    @Test
    @DisplayName("Запуск обработки заявки на изменение последней мили с самовывоза на самовывоз после создания заказа")
    @DatabaseSetup({
        "/controller/order/processing/change_last_mile_from_pickup_to_pickup/setup.xml",
        "/controller/order/processing/change_last_mile_from_pickup_to_pickup/create/before/segment_requests.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/"
            + "order_create_success_change_last_mile_from_pickup_to_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createOrderSuccessWithChangeLastMileFromPickupToPickupSegmentRequest() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                51L,
                6L,
                1L,
                new CreateOrderSuccessDto(
                    "new-pickup-external-id",
                    51L,
                    "1001",
                    10L
                )
            )
        );
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState, с еще незаполненным externalId.
     * Для этого сегмента существует заявка на изменение, в статусе WAITING_FOR_PROCESSING_AVAILABILITY.
     * Для заявок этого типа не указан producer тасок.
     * После теста у сегмента появляется значение externalId. История изменений сохраняется.
     * Задача на обработку заявки на изменение не ставится, статус заявки не изменяется, пишется лог.
     */
    @Test
    @DisplayName("У ожидающей создания в партнере заявки на изменение сегмента не указан producer тасок на обработку")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/controller/order/processing/create/success/before/segment_change_request_unsupported_type.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessWithChangeRequestUnsupportedType() {
        processDefault();

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1009L, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(-1L)
                    .setSequenceId(1009L)
                    .setStatus(BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)
                    .setCreated(clock.instant())
            ));

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN\t" +
            "format=plain\t" +
            "payload=Could not resume processing of change segment request after order creation: unsupported type\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "entity_types=ORDER,CHANGE_ORDER_SEGMENT_REQUEST\t" +
            "entity_values=ORDER:1,CHANGE_ORDER_SEGMENT_REQUEST:1\t" +
            "extra_keys=change_request_type\t" +
            "extra_values=LAST_MILE"
        );
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState, с еще незаполненным externalId.
     * После теста у сегмента появляется значение externalId. История изменений сохраняется.
     */
    @Test
    @DisplayName("Успешный сценарий для СД без активных план-фактов")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success_without_active_plan_facts.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_without_active_plan_facts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessWithoutActivePlanFacts() {
        processDefault();
    }

    @Test
    @DisplayName("Успешный сценарий для СД - отмененный заказ")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success_cancelled.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessCancelled() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                1L,
                2L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    2L,
                    "LOinttest-1",
                    1009L
                )
            )
        );
    }

    @Test
    @DisplayName("Успешный сценарий для СД - возвратный СЦ")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success_return.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml",
        "/service/business_process_state/fulfillment_return_create_order_external_async_request_sent.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessReturn() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-2",
                    2L,
                    "LOinttest-1",
                    1009L
                )
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.REGISTER_DELIVERY_TRACK,
            PayloadFactory.createOrderIdPartnerIdWaybillSegmentIdPayload(1L, 2L, 1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CREATE_TRUST_ORDER,
            PayloadFactory.createOrderIdPayload(1L, "2", 2L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.GET_WW_ORDER_LABEL,
            PayloadFactory.createOrderIdPartnerIdPayload(1L, 2L, "3", 3L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_CREATE_ORDER,
            PayloadFactory.createWaybillSegmentPayload(1L, 2L, "4", 4L)
        );

        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    1L,
                    "LOinttest-1",
                    1010L
                )
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.REGISTER_DELIVERY_TRACK,
            PayloadFactory.createOrderIdPartnerIdWaybillSegmentIdPayload(1L, 1L, 2L, "5", 5L)
        );
    }

    @Test
    @DisplayName("Идемпотентность метода")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/service/business_process_state/create_order_2_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void alreadyReported() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                2L,
                3L,
                2L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    2L,
                    "LOinttest-2",
                    1009L
                )
            )
        );
    }

    @Test
    @DisplayName("Неподходящий статус заказа")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_in_invalid_status.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/order_in_invalid_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderInInvalidStatus() {
        softly.assertThatThrownBy(() -> processCreateOrderAsyncSuccessResultService.processPayload(
                new CreateOrderSuccessPayload(
                    "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                    ApiType.DELIVERY,
                    1L,
                    1L,
                    1L,
                    new CreateOrderSuccessDto(
                        "test-external-id-2",
                        1L,
                        "LOinttest-1",
                        1009L
                    )
                )
            ))
            .isInstanceOf(InappropriateOrderStateException.class);
    }

    @Test
    @DisplayName("Создание возвратного сегмента с тегом RECREATED успешно даже для заказа в неподходящем статусе")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_1.xml",
        "/controller/order/processing/create/success/before/order_with_recreated_waybill_segment.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_with_recreated_waybill_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recreatedCreateSuccessWillLeaveStatusCancelled() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                ApiType.DELIVERY,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    1L,
                    "LOinttest-1",
                    1009L
                )
            )
        );
    }

    @Test
    @DisplayName(
        "Успешное создание возвратного сегмента с тегом RECREATED у заказа с активной заявкой на отмену для ФФ"
    )
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_return_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/success/before/cancelled_order_with_recreated_wb_segment.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/ff_cancelled_order_with_recreated_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recreatedReturnWaybillSegmentCancelledOrder() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                ApiType.FULFILLMENT,
                2L,
                2L,
                1L,
                new CreateOrderSuccessDto("test-external-id-2", 2L, "LOinttest-1", 1010L)
            )
        );
    }

    @Test
    @DisplayName(
        "Успешное создание возвратного сегмента с тегом RECREATED у заказа с неактивной заявкой на отмену для ФФ"
    )
    @DatabaseSetup({
        "/service/business_process_state/fulfillment_return_create_order_external_async_request_sent.xml",
        "/controller/order/processing/create/success/before/cancelled_order_with_recreated_wb_segment_inactive.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/ff_cancelled_order_with_recreated_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recreatedReturnWaybillSegmentCancelledOrderInactive() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                ApiType.FULFILLMENT,
                2L,
                2L,
                1L,
                new CreateOrderSuccessDto("test-external-id-2", 2L, "LOinttest-1", 1010L)
            )
        );
    }

    @Test
    @DisplayName("Успешное создание возвратного сегмента с тегом RECREATED у заказа для ФФ без заявки на отмену")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_1.xml",
        "/controller/order/processing/create/success/before/order_with_recreated_waybill_segment.xml",
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/ff_order_with_recreated_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recreatedReturnWaybillSegmentOrder() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                ApiType.FULFILLMENT,
                2L,
                2L,
                1L,
                new CreateOrderSuccessDto("test-external-id-2", 2L, "LOinttest-1", 1010L)
            )
        );
    }

    @Test
    @DisplayName(
        "Создание возвратного сегмента с тегом RECREATED переводит заказ из статуса PROCESSING_ERROR в предыдущий"
    )
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_2.xml",
        "/controller/order/processing/create/success/before/order_with_recreated_waybill_segment.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_with_recreated_waybill_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void recreatedCreateSuccessWillUpdateStatusToPrevious() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                ApiType.DELIVERY,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    2L,
                    "LOinttest-1",
                    1009L
                )
            )
        );
    }

    @Test
    @DisplayName("Сегмент отменён")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_in_cancelled_status.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_in_cancelled_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderInCancelledStatus() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
                ApiType.DELIVERY,
                1L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-2",
                    1L,
                    "LOinttest-1",
                    1009L
                )
            )
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Все сегменты уже заполнены")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/waybill_segments_are_filled.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/waybill_segments_are_filled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void waybillSegmentsAreFilled() {
        softly.assertThatThrownBy(() -> processCreateOrderAsyncSuccessResultService.processPayload(
                new CreateOrderSuccessPayload(
                    "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                    ApiType.DELIVERY,
                    1L,
                    1L,
                    1L,
                    new CreateOrderSuccessDto(
                        "test-external-id-2",
                        1L,
                        "LOinttest-1",
                        1009L
                    )
                )
        ))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("ExternalId of order with id=1 and partnerId=1 was already reported with different value");
    }

    @Test
    @DisplayName("Заказ в DROPSHIP партнёре был создан с другим externalId")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success_dropship.xml",
        "/service/business_process_state/fulfillment_create_order_external_async_request_sent.xml",
    })
    @DatabaseSetup(
        value = "/controller/order/processing/create/success/before/waybill_segment_already_created.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_already_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropshipSegmentCreatedWithAnotherExternalId() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.FULFILLMENT,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-2",
                    2L,
                    "LOinttest-1",
                    1010L
                )
            )
        );
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN\t" +
                "format=plain\t" +
                "code=DROPSHIP_CREATE_ORDER_NOT_IDEMPOTENT_ERROR\t" +
                "payload=Order was already created with another externalId\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order,partner\t" +
                "entity_values=order:1,lom_order:1,partner:2"
        );
    }

    @Test
    @DisplayName("Сегмент не найден")
    @DatabaseSetup("/service/business_process_state/create_order_external_async_request_sent.xml")
    void segmentNotFound() {
        softly.assertThatThrownBy(() -> processCreateOrderAsyncSuccessResultService.processPayload(
                new CreateOrderSuccessPayload(
                    "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                    ApiType.DELIVERY,
                    1L,
                    1L,
                    1L,
                    new CreateOrderSuccessDto(
                        "test-external-id-2",
                        1L,
                        "LOinttest-1",
                        1009L
                    )
                )
            ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [WAYBILL_SEGMENT] with id [1]");
    }

    @Test
    @DisplayName(
        "Заказ с доставкой по клику через собственные ПВЗ маркета создан в партнере последней мили, " +
            "который осуществляет доставку от ПВЗ до пользователя. " +
            "Локации сегментов известны при создании заказа, обогащение не ожидается"
    )
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/market_pickup_point_on_demand_order.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/market_pickup_point_on_demand_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void marketPickupPointOnDemandOrder() {
        when(deliveryClient.getOrderSync(any(), any())).thenReturn(createOrder().build());
        when(lmsClient.getLogisticsPoints(any())).thenReturn(List.of(createPickupPointResponseBuilder(1L).build()));

        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                1L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    1L,
                    "1446543",
                    1009L
                )
            )
        );
    }

    @Test
    @DisplayName("Когда обрабатываем первый DS-сегмент не ставим таску на создание заказа на следующих сегментах")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @DatabaseSetup(
        value = "/controller/order/processing/create/success/before/dropship.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_without_new_bp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fisrstDropship() {
        processDefault();
    }

    @Test
    @DisplayName("Когда обрабатываем первый FF-сегмент ставим таску на создание заказа на следующих сегментах")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @DatabaseSetup(
        value = "/controller/order/processing/create/success/before/fulfillment.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void firstFF() {
        processDefault();
    }

    @Test
    @DisplayName("Когда обрабатываем не первый сегмент ставим таску на создание заказа на следующих сегментах")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void middleSegment() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                1L,
                2L,
                1L,
                new CreateOrderSuccessDto("test-external-id-1", 1L, "LOinttest-1", 1009L)
            )
        );
    }

    @Test
    @DisplayName("Успешный сценарий для СД (платформа YANDEX_GO)")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success_yago.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_yago.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessYandexGo() {
        processDefault();

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1009L, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(-1L)
                    .setSequenceId(1009L)
                    .setStatus(BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)
                    .setCreated(clock.instant())
            ));
    }

    @Test
    @DisplayName("У товара проставлены маркировки, после создания отправляется запрос на обновление маркировок")
    @DatabaseSetup({
        "/controller/order/processing/create/success/before/order_create_success_with_instances.xml",
        "/service/business_process_state/create_order_external_async_request_sent.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/order_create_success_with_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderWithInstances() {
        processDefault();
    }

    private void processDefault() {
        processCreateOrderAsyncSuccessResultService.processPayload(
            new CreateOrderSuccessPayload(
                "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd",
                ApiType.DELIVERY,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    2L,
                    "LOinttest-1",
                    1009L
                )
            )
        );
    }
}

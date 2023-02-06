package ru.yandex.market.logistics.lom.jobs.consumer;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateFilter;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.TaskType;
import ru.yandex.market.logistics.lom.jobs.processor.BillingTransactionSegmentStatusesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.CallCourierProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.CancelOrderReturnsService;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileChangeOrderSegmentRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileFromPickupToPickupUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileToCourierUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileToPickupUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderShipmentDateProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderToOnDemandProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderToOnDemandRequestStatusUpdateProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderToOnDemandSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.CommitOrderProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ConvertRouteToWaybillProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.CreateReturnRegistryProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.CreateSegmentCancellationRequestsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.DeliveryServiceShipmentProcessingService;
import ru.yandex.market.logistics.lom.jobs.processor.FulfillmentShipmentProcessingService;
import ru.yandex.market.logistics.lom.jobs.processor.GetAcceptanceCertificateService;
import ru.yandex.market.logistics.lom.jobs.processor.GetCourierProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.GetLabelWwService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessDeliveryDateUpdatedByDsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessDeliveryTrackerTrackService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderLostService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderPlacesChangedService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderProcessingDelayService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderReadyToShipService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentCancelledService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentCheckpointsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentCourierNotFoundService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentReturnArrivedService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentReturnPreparingService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentReturnedService;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessWaybillService;
import ru.yandex.market.logistics.lom.jobs.processor.PublishLogbrokerHistoryEventsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.PushCancellationReturnDeliveryServiceStatusesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.QueueProcessingService;
import ru.yandex.market.logistics.lom.jobs.processor.RecalculateOrderDatesSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.RecalculateOrderDeliveryDateProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.RecalculateOrderRouteProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.RecalculatedOrderDatesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.RegisterDeliveryTrackService;
import ru.yandex.market.logistics.lom.jobs.processor.SaveLockerCodeProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.SaveTplAddressProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.SendCancellationToLrmService;
import ru.yandex.market.logistics.lom.jobs.processor.SendProcessingErrorToMqmProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.StartDeliveryTracksService;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateCancellationOrderRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateCourierProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateCourierRequestStatusUpdateProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateCourierSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileFromPickupToPickupProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileToCourierProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileToPickupProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateMissingItemsRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderDeliveryDateProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderDeliveryDateSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderDeliveryDateUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderItemsInstancesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderItemsInstancesSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderItemsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderItemsSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderPlacesSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderPlacesUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderRecipientProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderRecipientSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderRecipientUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateRecalculatedOrderDatesRequestStatusProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateTransferCodesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateTransferCodesRequestStatusUpdateProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateTransferCodesSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.WaybillSegmentCancellationProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.WithdrawTransactionProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.admin.MultipleChangeDeliveryOptionsProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.admin.MultipleChangeOrderReturnSegmentProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.admin.MultipleChangeOrderReturnSegmentViaFileProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.admin.MultipleChangeOrderShipmentDatesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.admin.MultipleRecallCourierProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.admin.MultipleRetryBusinessProcessStatesProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderArrivedPickupPointProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderBoxesChangedProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderCancelledProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderDeliveredProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderEnqueuedLesEventProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderFromShipmentExclusionFinishedProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderReturningProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportOrderTransportationRecipientProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.les.ExportShopReadyToCreateOrderProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.order.create.DeliveryServiceCreateOrderExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.order.create.FulfillmentCreateOrderExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.order.processing.ProcessCreateOrderAsyncErrorResultService;
import ru.yandex.market.logistics.lom.jobs.processor.order.processing.ProcessCreateOrderAsyncSuccessResultService;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.DeliveryServiceUpdateOrderProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.FulfillmentUpdateOrderProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.OrderErrorMonitoringService;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.UpdateRecipientErrorMonitoringService;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.items.ProcessOrderChangedByPartnerRequestService;
import ru.yandex.market.logistics.lom.jobs.processor.order.update.items.ProcessOrderItemNotFoundRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.processor.registry.DeliveryServiceCreateRegistryExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.registry.FulfillmentCreateRegistryExternalService;
import ru.yandex.market.logistics.lom.jobs.processor.trust.CheckTrustBasketService;
import ru.yandex.market.logistics.lom.jobs.processor.trust.CreateTrustBasketService;
import ru.yandex.market.logistics.lom.jobs.processor.trust.CreateTrustOrderService;
import ru.yandex.market.logistics.lom.jobs.processor.trust.PayTrustBasketService;
import ru.yandex.market.logistics.lom.jobs.processor.trust.SaveTrustOrderService;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderExternalValidationAndEnrichingService;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderValidationErrorMonitoringService;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.logistics.lom.specification.BusinessProcessStateSpecification;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@MockBean({
    BillingTransactionSegmentStatusesProcessor.class,
    CheckTrustBasketService.class,
    CreateReturnRegistryProcessor.class,
    CreateTrustBasketService.class,
    CreateTrustOrderService.class,
    DeliveryServiceCreateOrderExternalService.class,
    DeliveryServiceCreateRegistryExternalService.class,
    DeliveryServiceShipmentProcessingService.class,
    FulfillmentCreateOrderExternalService.class,
    FulfillmentCreateRegistryExternalService.class,
    FulfillmentShipmentProcessingService.class,
    GetAcceptanceCertificateService.class,
    GetLabelWwService.class,
    OrderExternalValidationAndEnrichingService.class,
    PayTrustBasketService.class,
    RegisterDeliveryTrackService.class,
    StartDeliveryTracksService.class,
    WithdrawTransactionProcessor.class,
    SaveTrustOrderService.class,
    ProcessWaybillService.class,
    CreateSegmentCancellationRequestsProcessor.class,
    WaybillSegmentCancellationProcessor.class,
    UpdateCancellationOrderRequestProcessor.class,
    DeliveryServiceUpdateOrderProcessor.class,
    FulfillmentUpdateOrderProcessor.class,
    ProcessOrderChangedByPartnerRequestService.class,
    ProcessOrderReadyToShipService.class,
    ProcessOrderPlacesChangedService.class,
    ProcessDeliveryTrackerTrackService.class,
    UpdateOrderItemsSegmentProcessor.class,
    UpdateOrderItemsProcessor.class,
    UpdateOrderUpdatingRequestProcessor.class,
    ProcessCreateOrderAsyncSuccessResultService.class,
    ProcessCreateOrderAsyncErrorResultService.class,
    ProcessSegmentReturnedService.class,
    ProcessSegmentReturnArrivedService.class,
    ProcessSegmentReturnPreparingService.class,
    ProcessSegmentCancelledService.class,
    ProcessOrderLostService.class,
    ProcessSegmentCourierNotFoundService.class,
    CommitOrderProcessor.class,
    ConvertRouteToWaybillProcessor.class,
    ProcessOrderItemNotFoundRequestProcessor.class,
    MultipleChangeOrderShipmentDatesProcessor.class,
    ChangeOrderShipmentDateProcessor.class,
    MultipleRetryBusinessProcessStatesProcessor.class,
    MultipleChangeDeliveryOptionsProcessor.class,
    MultipleChangeOrderReturnSegmentProcessor.class,
    MultipleChangeOrderReturnSegmentViaFileProcessor.class,
    MultipleRecallCourierProcessor.class,
    CallCourierProcessor.class,
    ProcessDeliveryDateUpdatedByDsProcessor.class,
    UpdateOrderDeliveryDateProcessor.class,
    UpdateOrderDeliveryDateSegmentProcessor.class,
    UpdateOrderDeliveryDateUpdatingRequestProcessor.class,
    UpdateOrderRecipientProcessor.class,
    UpdateOrderRecipientSegmentProcessor.class,
    UpdateOrderRecipientUpdatingRequestProcessor.class,
    UpdateLastMileProcessor.class,
    UpdateLastMileToPickupProcessor.class,
    UpdateLastMileUpdatingRequestProcessor.class,
    UpdateLastMileSegmentProcessor.class,
    UpdateOrderItemsInstancesProcessor.class,
    UpdateOrderItemsInstancesSegmentProcessor.class,
    UpdateTransferCodesProcessor.class,
    UpdateTransferCodesSegmentProcessor.class,
    OrderValidationErrorMonitoringService.class,
    UpdateRecipientErrorMonitoringService.class,
    OrderErrorMonitoringService.class,
    GetCourierProcessor.class,
    UpdateMissingItemsRequestProcessor.class,
    UpdateTransferCodesRequestStatusUpdateProcessor.class,
    UpdateCourierProcessor.class,
    UpdateCourierSegmentProcessor.class,
    UpdateCourierRequestStatusUpdateProcessor.class,
    PublishLogbrokerHistoryEventsProcessor.class,
    ChangeOrderToOnDemandProcessor.class,
    ChangeOrderToOnDemandSegmentProcessor.class,
    ChangeOrderToOnDemandRequestStatusUpdateProcessor.class,
    RecalculateOrderDeliveryDateProcessor.class,
    SendProcessingErrorToMqmProcessor.class,
    ChangeOrderRequestProcessingService.class,
    RecalculateOrderDatesSegmentProcessor.class,
    UpdateRecalculatedOrderDatesRequestStatusProcessor.class,
    SaveLockerCodeProcessor.class,
    SaveTplAddressProcessor.class,
    RecalculatedOrderDatesProcessor.class,
    ExportOrderEnqueuedLesEventProcessor.class,
    ExportOrderArrivedPickupPointProcessor.class,
    ExportOrderDeliveredProcessor.class,
    ExportOrderReturningProcessor.class,
    ExportOrderCancelledProcessor.class,
    ProcessOrderProcessingDelayService.class,
    ExportOrderTransportationRecipientProcessor.class,
    ExportOrderFromShipmentExclusionFinishedProcessor.class,
    ExportShopReadyToCreateOrderProcessor.class,
    ExportOrderBoxesChangedProcessor.class,
    SendCancellationToLrmService.class,
    CancelOrderReturnsService.class,
    UpdateOrderPlacesSegmentProcessor.class,
    UpdateOrderPlacesUpdatingRequestProcessor.class,
    ChangeLastMileToPickupUpdatingRequestProcessor.class,
    UpdateLastMileToCourierProcessor.class,
    ChangeLastMileToCourierUpdatingRequestProcessor.class,
    RecalculateOrderRouteProcessor.class,
    ProcessSegmentCheckpointsProcessor.class,
    UpdateLastMileFromPickupToPickupProcessor.class,
    ChangeLastMileChangeOrderSegmentRequestProcessor.class,
    ChangeLastMileFromPickupToPickupUpdatingRequestProcessor.class,
    PushCancellationReturnDeliveryServiceStatusesProcessor.class,
})
@DisplayName("Тест на базовый класс консьюмеров задач из очереди")
@DatabaseSetup("/jobs/consumer/before/all_queue_tasks.xml")
class BaseQueueConsumerTest extends AbstractContextualYdbTest {

    @Autowired
    private List<BaseQueueConsumer<?>> queueConsumers;

    @Autowired
    private BusinessProcessStateRepository businessProcessStateRepository;

    @Autowired
    private BusinessProcessStateSpecification businessProcessStateSpecification;

    @Autowired
    private BusinessProcessStateTableDescription ydbTable;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription ydbHistoryTable;

    @Autowired
    private BusinessProcessStateEntityIdTableDescription ydbEntityIdTable;

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository ydbHistoryRepository;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-08-30T11:12:13.00Z"), clock.getZone());
    }

    @Test
    @DisplayName("Все типы задач, использующие бизнес процессы, покрыты тестами")
    void allQueueTypesCoveredWithTests() {
        Set<QueueType> businessProcessStateQueueTypes = businessProcessStateRepository.findAll().stream()
            .map(BusinessProcessState::getQueueType)
            .collect(Collectors.toSet());
        Set<QueueType> allQueueTypesWithBusinessProcesses = QueueType.getNotDeprecatedQueueTypes()
            .filter(QueueType::isBusinessProcessNeeded)
            .collect(Collectors.toSet());
        softly.assertThat(businessProcessStateQueueTypes).containsExactlyInAnyOrderElementsOf(
            allQueueTypesWithBusinessProcesses
        );
    }

    @DisplayName("После успешного выполнения задачи из очереди бизнес-процесс переводится в нужный статус")
    @ParameterizedTest
    @MethodSource("queueTypesWithBusinessProcesses")
    void queueTaskCorrectProcessStatusTransition(QueueType queueType) {
        BaseQueueConsumer<?> consumer = getConsumer(queueType);
        doReturnWhenProcessPayload(consumer.getProcessingService());
        doExecute(consumer);
        BusinessProcessStatus status = queueType.getTaskType() == TaskType.SYNC
            ? BusinessProcessStatus.SYNC_PROCESS_SUCCEEDED
            : BusinessProcessStatus.ASYNC_REQUEST_SENT;
        BusinessProcessState businessProcessState = assertBusinessProcessStatus(queueType, status, null);
        assertBusinessProcessStatusYdbHistory(
            businessProcessState.getId(),
            businessProcessState.getSequenceId(),
            status,
            null
        );
    }

    @DisplayName("После ошибки выполнения задачи из очереди бизнес-процесс переходит в нужный статус")
    @ParameterizedTest
    @MethodSource("queueTypesWithBusinessProcesses")
    void queueTaskFailed(QueueType queueType) {
        BaseQueueConsumer<?> consumer = getConsumer(queueType);
        doThrowWhenProcessPayload(consumer.getProcessingService());
        doExecute(consumer);
        BusinessProcessState businessProcessState = assertBusinessProcessStatus(
            queueType,
            BusinessProcessStatus.QUEUE_TASK_ERROR,
            "Ooops... something went wrong"
        );
        assertBusinessProcessStatusYdbHistory(
            businessProcessState.getId(),
            businessProcessState.getSequenceId(),
            BusinessProcessStatus.QUEUE_TASK_ERROR,
            "Ooops... something went wrong"
        );
    }

    @DisplayName("После неуспешного выполнения задачи из очереди бизнес-процесс переходит в нужный статус")
    @ParameterizedTest
    @MethodSource("queueTypesWithBusinessProcesses")
    void queueTaskUnprocessed(QueueType queueType) {
        BaseQueueConsumer<?> consumer = getConsumer(queueType);
        doReturnWhenProcessPayloadUnprocessed(consumer.getProcessingService());
        doExecute(consumer);
        BusinessProcessState businessProcessState = assertBusinessProcessStatus(
            queueType,
            BusinessProcessStatus.UNPROCESSED,
            "Ooops... something went wrong"
        );
        assertBusinessProcessStatusYdbHistory(
            businessProcessState.getId(),
            businessProcessState.getSequenceId(),
            BusinessProcessStatus.UNPROCESSED,
            "Ooops... something went wrong"
        );
    }

    @Test
    @DisplayName("У всех не deprecated типов тасок есть консьюмеры")
    void allNotDeprecatedTasksHasConsumers() {
        QueueType.getNotDeprecatedQueueTypes().forEach(
            type -> softly.assertThat(getOptConsumer(type)).isPresent()
        );
    }

    @Test
    @DisplayName("У всех deprecated типов тасок нет консьюмеров")
    void allDeprecatedTasksDoesntHaveConsumers() {
        Set<QueueType> notDeprecatedTypes = QueueType.getNotDeprecatedQueueTypes().collect(Collectors.toSet());
        Arrays.stream(QueueType.values())
            .filter(queueType -> !notDeprecatedTypes.contains(queueType))
            .forEach(
                type -> softly.assertThat(getOptConsumer(type)).isEmpty()
            );
    }

    private BusinessProcessState assertBusinessProcessStatus(
        QueueType queueType,
        BusinessProcessStatus status,
        @Nullable String comment
    ) {
        Optional<BusinessProcessState> businessProcessStateOptional = businessProcessStateRepository.findOne(
            businessProcessStateSpecification.fromFilter(
                BusinessProcessStateFilter.builder()
                    .queueTypes(EnumSet.of(queueType))
                    .comment(comment)
                    .build()
            )
        );
        softly.assertThat(businessProcessStateOptional).isNotEmpty();
        softly.assertThat(businessProcessStateOptional).map(BusinessProcessState::getStatus).contains(status);

        return businessProcessStateOptional.get();
    }

    private void assertBusinessProcessStatusYdbHistory(
        long id,
        Long sequenceId,
        BusinessProcessStatus status,
        @Nullable String comment
    ) {
        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(sequenceId, Pageable.unpaged()))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(id)
                    .setSequenceId(sequenceId)
                    .setStatus(status)
                    .setMessage(comment)
                    .setCreated(clock.instant())
            ));
    }

    @Nonnull
    private BaseQueueConsumer<?> getConsumer(QueueType queueType) {
        return getOptConsumer(queueType)
            .orElseThrow();
    }

    @Nonnull
    private Optional<BaseQueueConsumer<?>> getOptConsumer(QueueType queueType) {
        return queueConsumers.stream()
            .filter(c -> c.getQueueType() == queueType)
            .findFirst();
    }

    @Nonnull
    private static Stream<Arguments> queueTypesWithBusinessProcesses() {
        return QueueType.getNotDeprecatedQueueTypes().filter(QueueType::isBusinessProcessNeeded).map(Arguments::of);
    }

    private void doThrowWhenProcessPayload(QueueProcessingService<?> service) {
        doThrow(new RuntimeException("Ooops... something went wrong")).when(service).processPayload(any());
    }

    private void doReturnWhenProcessPayloadUnprocessed(QueueProcessingService<?> service) {
        doReturn(ProcessingResult.unprocessed("Ooops... something went wrong")).when(service).processPayload(any());
    }

    private void doReturnWhenProcessPayload(QueueProcessingService<?> service) {
        doReturn(ProcessingResult.success()).when(service).processPayload(any());
    }

    private <T extends ExecutionQueueItemPayload> void doExecute(BaseQueueConsumer<T> consumer) {
        QueueType queueType = consumer.getQueueType();
        @SuppressWarnings("unchecked")
        Class<T> payloadClass = (Class<T>) queueType.getPayloadClass();
        consumer.execute(TaskFactory.createTask(
            queueType,
            queueTaskChecker.getProducedTaskPayload(queueType, payloadClass)
        ));
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(ydbTable, ydbHistoryTable, ydbEntityIdTable);
    }
}

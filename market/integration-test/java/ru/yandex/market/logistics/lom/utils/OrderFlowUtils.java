package ru.yandex.market.logistics.lom.utils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;

import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.BusinessProcessStateEntityId;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.DeliveryServiceCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.FulfillmentCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.LesWaybillEventPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessWaybillService;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.service.async.DeliveryServiceCreateOrderAsyncResultService;
import ru.yandex.market.logistics.lom.service.async.FulfillmentCreateOrderAsyncResultService;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static ru.yandex.market.logistics.lom.AbstractContextualTest.REQUEST_ID;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPartnerIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPartnerIdWaybillSegmentIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

/**
 * Набор утилит для "проталкивания" флоу обработки вейбила в тестах
 * и проверки корректности постановки задач в dbqueue.
 */
public class OrderFlowUtils {

    @RequiredArgsConstructor
    public static class FlowCreatorFactory {
        private final QueueTaskChecker queueTaskChecker;
        private final ProcessWaybillService processWaybillService;
        private final DeliveryServiceCreateOrderExternalConsumer deliveryServiceCreateOrderExternalConsumer;
        private final DeliveryServiceCreateOrderAsyncResultService deliveryServiceCreateOrderAsyncResultService;
        private final FulfillmentCreateOrderExternalConsumer fulfillmentCreateOrderExternalConsumer;
        private final FulfillmentCreateOrderAsyncResultService fulfillmentCreateOrderAsyncResultService;

        public FlowCreator create(String barcode, long orderId, boolean createTrust, boolean getLabel) {
            return create(barcode, orderId, createTrust, getLabel, 1);
        }

        public FlowCreator create(String barcode, long orderId, boolean createTrust, boolean getLabel, long seqId) {
            return new FlowCreator(
                seqId,
                barcode,
                orderId,
                createTrust,
                getLabel,
                queueTaskChecker,
                processWaybillService,
                deliveryServiceCreateOrderExternalConsumer,
                deliveryServiceCreateOrderAsyncResultService,
                fulfillmentCreateOrderExternalConsumer,
                fulfillmentCreateOrderAsyncResultService
            );
        }
    }

    @RequiredArgsConstructor
    private static class ExecItem {
        private final QueueType queueType;
        private final ExecutionQueueItemPayload payload;
    }

    @RequiredArgsConstructor
    public static class FlowCreator {
        private static final EnumSet<PartnerType> EXPORT_SHOP_READY_TO_CREATE_ORDER_PARTNER_TYPES =
            EnumSet.of(PartnerType.DROPSHIP, PartnerType.DROPSHIP_BY_SELLER);
        private long subReqId = 1;
        private long eventId = 1;
        private long changeReqId = 1;

        @Nonnull
        private Long seqId;
        private final String barcode;
        private final long orderId;
        private final boolean createTrust;
        private final boolean getLabel;

        private final QueueTaskChecker queueTaskChecker;
        private final ProcessWaybillService processWaybillService;
        private final DeliveryServiceCreateOrderExternalConsumer deliveryServiceCreateOrderExternalConsumer;
        private final DeliveryServiceCreateOrderAsyncResultService deliveryServiceCreateOrderAsyncResultService;
        private final FulfillmentCreateOrderExternalConsumer fulfillmentCreateOrderExternalConsumer;
        private final FulfillmentCreateOrderAsyncResultService fulfillmentCreateOrderAsyncResultService;

        private final List<ExecItem> execItems = new ArrayList<>();

        private void putExecItem(QueueType queueType, ExecutionQueueItemPayload payload) {
            execItems.add(new ExecItem(queueType, payload));
        }

        public FlowCreator start(long waybillSegmentId) {
            var payload = createWaybillSegmentPayload(orderId, waybillSegmentId);
            processWaybillService.processPayload(payload);
            return this;
        }

        @Nonnull
        public FlowCreator createDsOrder(long dsSegmentId, long partnerId, @Nullable Long ffSegmentId) {
            var orderSeq = getNextSeq();
            var orderPl = createWaybillSegmentPayload(orderId, dsSegmentId, String.valueOf(orderSeq), subReqId);
            putExecItem(QueueType.CREATE_ORDER_EXTERNAL, orderPl);

            deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.CREATE_ORDER_EXTERNAL,
                orderPl,
                1
            ));

            //В BaseQueueConsumer чистится контекст
            RequestContextHolder.setContext(new RequestContext(REQUEST_ID));

            subReqId = 1;

            putExecItem(
                QueueType.CHANGE_ORDER_REQUEST,
                createChangeOrderRequestPayload(
                    getNextChangeReqId(),
                    String.valueOf(getNextSeq()),
                    subReqId++
                )
            );

            putExecItem(
                QueueType.REGISTER_DELIVERY_TRACK,
                createOrderIdPartnerIdWaybillSegmentIdPayload(
                    orderId,
                    partnerId,
                    dsSegmentId,
                    String.valueOf(getNextSeq()),
                    subReqId++
                )
            );

            deliveryServiceCreateOrderAsyncResultService.processSuccess(
                new BusinessProcessState()
                    .setEntityIds(List.of(
                        new BusinessProcessStateEntityId()
                            .setEntityType(EntityType.WAYBILL_SEGMENT)
                            .setEntityId(dsSegmentId)
                    )),
                new CreateOrderSuccessDto("ds-external-id", partnerId, barcode, orderSeq)
            );

            if (createTrust) {
                putExecItem(
                    QueueType.CREATE_TRUST_ORDER,
                    createOrderIdPayload(orderId, String.valueOf(getNextSeq()), subReqId++)
                );
            }

            if (getLabel) {
                putExecItem(
                    QueueType.GET_WW_ORDER_LABEL,
                    createOrderIdPartnerIdPayload(orderId, partnerId, String.valueOf(getNextSeq()), subReqId++)
                );
            }

            if (ffSegmentId != null) {
                createAndProcessPayload(ffSegmentId);
            }

            return this;
        }

        @Nonnull
        public FlowCreator createDsOrder(long segmentId, long partnerId) {
            return createDsOrder(segmentId, partnerId, null);
        }

        @Nonnull
        public FlowCreator createFfOrder(long segmentId, long partnerId, @Nullable Long processWaybillSegmentId) {
            return createFfOrder(segmentId, partnerId, processWaybillSegmentId, null);
        }

        @Nonnull
        public FlowCreator createDbsOrder(long segmentId) {
            var orderLes = new LesWaybillEventPayload(REQUEST_ID + "/" + subReqId++, eventId++, segmentId);

            orderLes.setSequenceId(getNextSeq());
            putExecItem(QueueType.EXPORT_SHOP_READY_TO_CREATE_ORDER, orderLes);

            //В BaseQueueConsumer чистится контекст
            RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
            subReqId = 1;
            return this;
        }

        @Nonnull
        public FlowCreator createFfOrder(
            long segmentId,
            long partnerId,
            @Nullable Long processWaybillSegmentId,
            @Nullable PartnerType segmentPartnerType
        ) {
            var orderSeq = getNextSeq();
            var orderPl = createWaybillSegmentPayload(orderId, segmentId, String.valueOf(orderSeq), subReqId++);

            putExecItem(QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL, orderPl);

            fulfillmentCreateOrderExternalConsumer.execute(TaskFactory.createTask(
                QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
                orderPl,
                1
            ));

            if (
                segmentPartnerType != null
                    && EXPORT_SHOP_READY_TO_CREATE_ORDER_PARTNER_TYPES.contains(segmentPartnerType)
            ) {
                var orderLes = new LesWaybillEventPayload(REQUEST_ID + "/" + subReqId++, eventId++, segmentId);

                orderLes.setSequenceId(getNextSeq());
                putExecItem(QueueType.EXPORT_SHOP_READY_TO_CREATE_ORDER, orderLes);
            }

            //В BaseQueueConsumer чистится контекст
            RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
            subReqId = 1;

            putExecItem(
                QueueType.REGISTER_DELIVERY_TRACK,
                createOrderIdPartnerIdWaybillSegmentIdPayload(
                    orderId,
                    partnerId,
                    segmentId,
                    String.valueOf(getNextSeq()),
                    subReqId++
                )
            );

            fulfillmentCreateOrderAsyncResultService.processSuccess(
                new BusinessProcessState()
                    .setEntityIds(List.of(
                        new BusinessProcessStateEntityId()
                            .setEntityType(EntityType.WAYBILL_SEGMENT)
                            .setEntityId(segmentId)
                    )),
                new CreateOrderSuccessDto("1001", partnerId, barcode, orderSeq)
            );
            if (processWaybillSegmentId != null) {
                createAndProcessPayload(processWaybillSegmentId);
            }
            return this;
        }

        private void createAndProcessPayload(long processWaybillSegmentId) {
            var payload = createWaybillSegmentPayload(
                orderId,
                processWaybillSegmentId,
                String.valueOf(getNextSeq()),
                subReqId++
            );
            putExecItem(QueueType.PROCESS_WAYBILL_CREATE_ORDER, payload);
            processWaybillService.processPayload(payload);
        }

        @Nonnull
        public FlowCreator createFfOrder(long segmentId, long partnerId) {
            return createFfOrder(segmentId, partnerId, null);
        }

        public void checkFlow() {
            execItems
                .forEach(i -> {
                    queueTaskChecker.assertQueueTaskCreated(i.queueType, i.payload);
                });

            if (!execItems.isEmpty() && !createTrust) {
                queueTaskChecker.assertQueueTaskNotCreated(QueueType.CREATE_TRUST_ORDER);
            }
        }

        private long getNextSeq() {
            return seqId++;
        }

        private long getNextChangeReqId() {
            return changeReqId++;
        }
    }
}

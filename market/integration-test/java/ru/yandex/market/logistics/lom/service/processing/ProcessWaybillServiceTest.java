package ru.yandex.market.logistics.lom.service.processing;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.LesWaybillEventPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessWaybillService;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DisplayName("Обработка задачи обновления сегмента вейбилла")
public class ProcessWaybillServiceTest extends AbstractContextualTest {
    private static final String DBS_STUB_API = "dbs-stub-api";
    private static final String DS_API = "ds-api";
    private static final String FF_API = "ff-api";
    private static final String CREATE = "create";
    private static final String UPDATE = "update";

    private static final OrderIdWaybillSegmentPayload PAYLOAD_WS_1 = payload(1);

    private static final Set<PartnerType> EXPORT_SHOP_READY_TO_CREATE_ORDER_PARTNER_TYPES = EnumSet.of(
        PartnerType.DROPSHIP,
        PartnerType.DROPSHIP_BY_SELLER
    );

    private static final Map<Pair<String, String>, QueueType> QUEUE_TYPES = Map.of(
        Pair.of(DS_API, CREATE), QueueType.CREATE_ORDER_EXTERNAL,
        Pair.of(DS_API, UPDATE), QueueType.DELIVERY_SERVICE_UPDATE_ORDER,
        Pair.of(FF_API, CREATE), QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
        Pair.of(FF_API, UPDATE), QueueType.FULFILLMENT_UPDATE_ORDER
    );

    @Autowired
    private ProcessWaybillService processWaybillService;

    @Autowired
    private LMSClient lmsClient;

    @DisplayName("Создание заказа")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DatabaseSetup("/service/process/before/waybill_processing.xml")
    void createOrderQueueTaskCreated(
        @SuppressWarnings("unused") SegmentType displayName,
        PartnerType partnerType,
        @Nullable String api,
        OrderIdWaybillSegmentPayload payload
    ) {
        processWaybillService.processPayload(payload);
        boolean needToExport = EXPORT_SHOP_READY_TO_CREATE_ORDER_PARTNER_TYPES.contains(partnerType);
        QueueType queueType = QUEUE_TYPES.get(Pair.of(api, CREATE));

        if (queueType == null && !needToExport) {
            queueTaskChecker.assertNoQueueTasksCreated();
            return;
        }

        long requestId = 1;

        if (queueType != null) {
            checkQueueTask(queueType, payload);
            requestId++;
        }

        if (!needToExport) {
            return;
        }

        checkQueueTask(
            QueueType.EXPORT_SHOP_READY_TO_CREATE_ORDER,
            lesWaybillEventPayload(requestId, payload.getWaybillSegmentId())
        );
    }

    @Test
    @DisplayName("Все типы сегментов покрыты тестами")
    void assertAllSegmentTypesChecked() {
        Set<SegmentType> segmentTypes = argumentTriples().map(Quadruple::getFirst).collect(Collectors.toSet());

        Set<SegmentType> notTestedSegmentTypes = Arrays.stream(SegmentType.values())
            .filter(Predicate.not(segmentTypes::contains))
            .collect(Collectors.toSet());

        softly.assertThat(notTestedSegmentTypes)
            .withFailMessage("Following segment types are not covered with tests: %s", notTestedSegmentTypes)
            .isEmpty();
    }

    private void mockGetPartner() {
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(
            PartnerResponse.newBuilder()
                .params(List.of(new PartnerExternalParam("UPDATE_ORDER_WITH_ONE_BOX_ENABLED", "", "1")))
                .build()
        ));
    }

    private void checkQueueTask(@Nullable QueueType queueType, ExecutionQueueItemPayload payload) {
        if (queueType == null) {
            queueTaskChecker.assertNoQueueTasksCreated();
        } else {
            queueTaskChecker.assertQueueTaskCreated(queueType, payload);
        }
    }

    @Nonnull
    private static Stream<Quadruple<SegmentType, PartnerType, String, OrderIdWaybillSegmentPayload>> argumentTriples() {
        return Stream.of(
            Quadruple.of(SegmentType.FULFILLMENT, PartnerType.DROPSHIP, FF_API, payload(1)),
            Quadruple.of(SegmentType.MOVEMENT, PartnerType.DELIVERY, DS_API, payload(2)),
            Quadruple.of(SegmentType.SORTING_CENTER, PartnerType.SORTING_CENTER, FF_API, payload(3)),
            Quadruple.of(SegmentType.COURIER, PartnerType.DELIVERY, DS_API, payload(4)),
            Quadruple.of(SegmentType.PICKUP, PartnerType.DELIVERY, DS_API, payload(5)),
            Quadruple.of(SegmentType.SUPPLIER, PartnerType.SUPPLIER, null, payload(6)),
            Quadruple.of(SegmentType.POST, PartnerType.DELIVERY, DS_API, payload(7)),
            Quadruple.of(SegmentType.GO_PLATFORM, PartnerType.DELIVERY, DS_API, payload(8)),
            Quadruple.of(SegmentType.NO_OPERATION, PartnerType.DROPSHIP_BY_SELLER, DBS_STUB_API, payload(9))
        );
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return argumentTriples().map(t -> Arguments.of(t.getFirst(), t.getSecond(), t.getThird(), t.getFourth()));
    }

    @Nonnull
    private static OrderIdWaybillSegmentPayload payload(long waybillSegmentId) {
        return PayloadFactory.createWaybillSegmentPayload(1, waybillSegmentId, 1);
    }

    @Nonnull
    private static ExecutionQueueItemPayload lesWaybillEventPayload(long eventId, long waybillSegmentId) {
        return new LesWaybillEventPayload(REQUEST_ID + "/" + eventId, 1, waybillSegmentId).setSequenceId(eventId);
    }
}

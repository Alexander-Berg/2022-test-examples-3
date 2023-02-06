package ru.yandex.market.logistics.lom.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.jobs.model.QueueType.CHANGE_ORDER_REQUEST;
import static ru.yandex.market.logistics.lom.jobs.model.QueueType.PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DatabaseSetup("/controller/order/recalculateRouteDates/prepare_data.xml")
@DisplayName("Обновление даты доставки заказа из-за пересчёта даты в Комбинаторе (RECALCULATE_ROUTE_DATES)")
class RecalculatedOrderDatesProcessorTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @AfterEach
    void recalculatedOrderDatesProcessorTestTearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName(
        "У заказа два сегмента - начинаем с сегмента ff, создаем заявки на все сегменты, в сегменте нет параметров"
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/single_ds_from_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentsFromFFSegmentWithoutParameters() throws Exception {
        try (var ignored = mockLmsSearchPartners(Map.of(172L, true))) {
            processor.processPayload(getChangeOrderRequestPayload(1));
            queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 2);
        }
    }

    @Test
    @DisplayName(
        "У заказа два сегмента - начинаем с сегмента ff, создаем заявки на все сегменты - параметр из настроек"
    )
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/can_update_shipment_date_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/single_ds_from_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentsFromFFNoParamInSettings() throws Exception {
        try (var ignored = mockLmsSearchPartners(Map.of(172L, true))) {
            processor.processPayload(getChangeOrderRequestPayload(1));
            queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 2);
        }
    }

    @Test
    @DisplayName(
        "У заказа два сегмента - начинаем с сегмента ff, создаем заявки на все сегменты - параметр из настроек"
    )
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/can_update_shipment_date_true.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/single_ds_from_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentsFromFFParamInSettings() {
        processor.processPayload(getChangeOrderRequestPayload(1));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 2);
    }

    @Test
    @DisplayName("У заказа два сегмента - начинаем с сегмента ff, в настройках нет параметра, в LMS нет партнера")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/single_ds_from_ff_only_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentsFromFFSegmentWithoutParam() {
        SearchPartnerFilter filter = SearchPartnerFilter.builder().setIds(Set.of(172L)).build();
        when(lmsClient.searchPartners(filter)).thenReturn(List.of());

        processor.processPayload(getChangeOrderRequestPayload(1));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 1);

        verify(lmsClient).searchPartners(safeRefEq(filter));
    }

    @Test
    @DisplayName("У заказа два сегмента - начинаем с сегмента сд, создаем заявку только на сд")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/single_ds_from_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoSegmentsFromSD() {
        processor.processPayload(getChangeOrderRequestPayload(2));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 1);
    }

    @Test
    @DisplayName("У заказа три сегмента СД. Обновление с первого")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/all_sd_from_first.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalSegmentsFromFirst() {
        processor.processPayload(getChangeOrderRequestPayload(3));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 3);
    }

    @Test
    @DisplayName("У заказа три сегмента СД. Обновление с последнего")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/all_sd_from_last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalSegmentsFromLast() {
        processor.processPayload(getChangeOrderRequestPayload(4));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 1);
    }

    @Test
    @DisplayName("У заказа три сегмента СД. Обновление с первого. "
        + "Т.к. изменения cor не видны пользователю - обновлять сегменты СД не надо.")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/cor_invisible_for_user.xml",
        type = DatabaseOperation.UPDATE
    )
    void doNotUpdateDeliverySegmentForInvisibleCor() {
        processor.processPayload(getChangeOrderRequestPayload(3));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName(
        "У заказа три сегмента СД и два FF. Обновление с первого FF, " +
            "но только второй FF может обновлять дату отгрузки - нет параметра в настройках, в LMS есть параметры"
    )
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/can_update_shipment_date_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/several_from_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalSegmentsFromFFNoSettings() throws Exception {
        try (var ignored = mockLmsSearchPartners(Map.of(172L, false, 75035L, true))) {
            processor.processPayload(getChangeOrderRequestPayload(5));
            queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 3);
        }
    }

    @Test
    @DisplayName(
        "У заказа три сегмента СД и два FF. Обновление с первого FF, " +
            "но только второй FF может обновлять дату отгрузки - для первого сегмента есть настройка, "
            + "в LMS есть параметр для второго"
    )
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/can_update_shipment_date_true_8.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/several_from_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalSegmentsFromFFOneFromSettingOneFromLms() throws Exception {
        try (var ignored = mockLmsSearchPartners(Map.of(172L, false))) {
            processor.processPayload(getChangeOrderRequestPayload(5));
            queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 3);
        }
    }

    @Test
    @DisplayName("У заказа три сегмента СД и два FF. Обновление с СД")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/all_sd_from_last.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalSegmentsFromSd() {
        processor.processPayload(getChangeOrderRequestPayload(4));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 1);
    }

    @Test
    @DisplayName("Запрос в невалидном статусе")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidRequestStatus() {
        processor.processPayload(getChangeOrderRequestPayload(7));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Запрос другого типа")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidRequestType() {
        processor.processPayload(getChangeOrderRequestPayload(8));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderInWrongStatus() {
        ProcessingResult processingResult = processor.processPayload(getChangeOrderRequestPayload(10));
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 5 with status VALIDATION_ERROR, task will be retried");
    }

    @Test
    @DisplayName("Заказ в процессе создания")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderInEnqueuedStatus() {
        ChangeOrderRequestPayload payload = getChangeOrderRequestPayload(11);

        ProcessingResult processingResult = processor.processPayload(payload);

        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 6 with status ENQUEUED, task will be retried");
        queueTaskChecker.assertQueueTaskCreatedWithDelay(CHANGE_ORDER_REQUEST, payload, Duration.ofHours(1));
    }

    @Test
    @DisplayName("У заказа есть возвратный сегмент")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/with_return_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderWithReturnSegment() {
        processor.processPayload(getChangeOrderRequestPayload(9));
        queueTaskChecker.assertQueueTasksCreated(PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES, 1);
    }

    @Test
    @DisplayName("В реквесте нет сегмента")
    @ExpectedDatabase(
        value = "/controller/order/recalculateRouteDates/after/cor_12_success_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestWithoutSegment() {
        processor.processPayload(getChangeOrderRequestPayload(12));
        queueTaskChecker.assertQueueTasksCreated(QueueType.EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED, 1);
        queueTaskChecker.assertNoQueueTasksCreatedExcept(
            Set.of(QueueType.EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED)
        );
    }

    @Test
    @DisplayName("Неподдерживаемый подтип партнёра")
    @DatabaseSetup("/controller/order/recalculateRouteDates/unsupported_partner_subtype.xml")
    void unsupportedPartnerSubtype() {
        processor.processPayload(getChangeOrderRequestPayload(1));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Неподдерживаемый партнёр")
    @DatabaseSetup("/controller/order/recalculateRouteDates/unsupported_partner_subtype.xml")
    @DatabaseSetup(
        value = "/controller/order/recalculateRouteDates/unsupported_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    void unsupportedPartner() {
        processor.processPayload(getChangeOrderRequestPayload(1));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private static ChangeOrderRequestPayload getChangeOrderRequestPayload(long requestId) {
        return PayloadFactory.createChangeOrderRequestPayload(requestId, "1", 1L);
    }

    @Nonnull
    private AutoCloseable mockLmsSearchPartners(Map<Long, Boolean> partnersToCanUpdateShipmentDate) {
        SearchPartnerFilter filter = SearchPartnerFilter.builder()
            .setIds(partnersToCanUpdateShipmentDate.keySet())
            .build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(
                EntryStream.of(partnersToCanUpdateShipmentDate)
                    .mapKeyValue((partnerId, canUpdateShipmentDate) -> PartnerResponse.newBuilder().id(partnerId)
                        .params(List.of(
                            new PartnerExternalParam("CAN_UPDATE_SHIPMENT_DATE", "", canUpdateShipmentDate ? "1" : "0")
                        ))
                        .build())
                    .collect(Collectors.toList())
            );
        return () -> verify(lmsClient).searchPartners(safeRefEq(filter));
    }
}

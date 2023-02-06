package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.RegisterDeliveryTrackConsumer;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.OrderIdPartnerIdWaybillSegmentIdPayload;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.domain.enums.DeliveryType.DELIVERY;

@DisplayName("Зарегистрировать трек заказа в трекере")
public class RegisterDeliveryTrackTest extends AbstractContextualTest {
    private static final String ORDER_BARCODE = "LOinttest-1";
    private static final long ORDER_ID = 1;
    private static final long FULFILLMENT_WAYBILL_SEGMENT_ID = 1;
    private static final long SUPPLIER_WAYBILL_SEGMENT_ID = 3;
    private static final String TRACK_CODE = "waybill-segment-external-id-1";
    private static final String ORDER_EXTERNAL_ID = "6969697";
    private static final long DELIVERY_SERVICE_ID = 1;

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Autowired
    private RegisterDeliveryTrackConsumer registerDeliveryTrackConsumer;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(trackerApiClient);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/tracker/before/register.xml")
    @DatabaseSetup("/service/business_process_state/register_delivery_track_1_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_1_1_sync_process_succeeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(value = "/service/tracker/after/register.xml", assertionMode = NON_STRICT)
    void registerDeliveryTrack() {
        executeRegisterOrderDeliveryTrack(TRACK_CODE);
    }

    @Test
    @DisplayName("Зарегистрировать трек для дропшип сегмента использовать externalId заказа")
    @DatabaseSetup("/service/tracker/before/register_dropship_segment.xml")
    @DatabaseSetup("/service/business_process_state/register_delivery_track_1_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/service/tracker/after/register_dropship_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_1_1_sync_process_succeeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void registerDeliveryTrackForDropship() {
        executeRegisterOrderDeliveryTrack(ORDER_EXTERNAL_ID);
    }

    private void executeRegisterOrderDeliveryTrack(String externalId) {
        clock.setFixed(Instant.parse("2020-11-07T10:11:50Z"), ZoneOffset.UTC);
        DeliveryTrackRequest request = deliveryTrackRequest(externalId);
        when(trackerApiClient.registerDeliveryTrack(request)).thenReturn(createDeliveryTrackMeta());
        registerDeliveryTrackConsumer.execute(createTask(FULFILLMENT_WAYBILL_SEGMENT_ID));

        verify(trackerApiClient).registerDeliveryTrack(request);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=TRACK_CODE_REGISTRATION_SUCCESS\t" +
                    "payload=Track code registered\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=BUSINESS_ORDER_EVENT\t" +
                    "entity_types=order,lom_order,waybillSegment,partner,track\t" +
                    "entity_values=order:6969697,lom_order:1,waybillSegment:1,partner:1,track:4\n"
            );
    }

    @Test
    @DisplayName("Ошибка")
    @DatabaseSetup("/service/tracker/before/register.xml")
    @DatabaseSetup("/service/business_process_state/register_delivery_track_1_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_1_1_queue_task_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(value = "/service/tracker/before/register.xml", assertionMode = NON_STRICT)
    void registerDeliveryTrackError() {
        DeliveryTrackRequest request = deliveryTrackRequest(TRACK_CODE);
        when(trackerApiClient.registerDeliveryTrack(request))
            .thenThrow(new RuntimeException("Ooops... Delivery tracker is not responding"));
        registerDeliveryTrackConsumer.execute(createTask(FULFILLMENT_WAYBILL_SEGMENT_ID));

        verify(trackerApiClient).registerDeliveryTrack(request);
    }

    @Test
    @DisplayName("Не указана версия API в SegmentType")
    @DatabaseSetup("/service/tracker/before/register.xml")
    @DatabaseSetup("/service/business_process_state/register_delivery_track_1_3_enqueued.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_1_3_queue_task_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(value = "/service/tracker/before/register.xml", assertionMode = NON_STRICT)
    void registerDeliveryTrackWithoutApiVersion() {
        registerDeliveryTrackConsumer.execute(createTask(SUPPLIER_WAYBILL_SEGMENT_ID));
    }

    @Nonnull
    private DeliveryTrackRequest deliveryTrackRequest(String trackCode) {
        return DeliveryTrackRequest.builder()
            .trackCode(trackCode)
            .deliveryServiceId(DELIVERY_SERVICE_ID)
            .consumerId(2L)
            .entityId(ORDER_BARCODE)
            .deliveryType(DELIVERY)
            .isGlobalOrder(false)
            .entityType(EntityType.ORDER)
            .apiVersion(ApiVersion.FF)
            .build();
    }

    @Nonnull
    private DeliveryTrackMeta createDeliveryTrackMeta() {
        DeliveryTrackMeta response = new DeliveryTrackMeta();
        response.setId(4L);
        response.setTrackCode(TRACK_CODE);
        response.setDeliveryServiceId(1L);
        response.setEntityId(ORDER_BARCODE);
        response.setDeliveryType(DELIVERY);
        response.setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        response.setEntityType(EntityType.ORDER);
        return response;
    }

    @Nonnull
    private Task<OrderIdPartnerIdWaybillSegmentIdPayload> createTask(long waybillSegmentId) {
        return TaskFactory.createTask(
            QueueType.REGISTER_DELIVERY_TRACK,
            PayloadFactory.createOrderIdPartnerIdWaybillSegmentIdPayload(ORDER_ID, 1L, waybillSegmentId, "1001")
        );
    }
}

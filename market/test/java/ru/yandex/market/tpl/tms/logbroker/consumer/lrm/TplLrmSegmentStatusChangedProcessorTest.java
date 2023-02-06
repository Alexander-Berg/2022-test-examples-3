package ru.yandex.market.tpl.tms.logbroker.consumer.lrm;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ReturnSegmentStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerMessage;
import ru.yandex.market.tpl.common.lrm.client.model.LogisticPointType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.DELIVERED_TO_SC;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RECEIVED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CLIENT_RETURN_UPDATE_STATUS_FROM_LOGBROKER_DISABLED;

@RequiredArgsConstructor
class TplLrmSegmentStatusChangedProcessorTest extends TplTmsAbstractTest {

    private final TplLrmReturnEventConsumer tplLrmReturnEventConsumer;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnRepository clientReturnRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void before() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CLIENT_RETURN_UPDATE_STATUS_FROM_LOGBROKER_DISABLED))
                .thenReturn(false);
    }

    @ParameterizedTest
    @EnumSource(value = ReturnSegmentStatus.class, names = {"IN", "OUT", "TRANSIT_PREPARED"})
    void processSuccess(ReturnSegmentStatus returnSegmentStatus) {
        var cr = clientReturnGenerator.generateReturnFromClient();
        cr.setStatus(RECEIVED);
        cr.setExternalReturnId(String.valueOf(cr.getId()));
        cr = clientReturnRepository.save(cr);

        LogbrokerMessage message = getMessage(
                ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED, cr.getExternalReturnId(),
                LogisticPointType.SORTING_CENTER, returnSegmentStatus
        );
        tplLrmReturnEventConsumer.accept(message);

        var updatedCr = clientReturnRepository.findByIdOrThrow(cr.getId());
        assertEquals(DELIVERED_TO_SC, updatedCr.getStatus());
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 1);
    }

    @ParameterizedTest
    @EnumSource(value = ReturnSegmentStatus.class, names = {"IN", "OUT", "TRANSIT_PREPARED"},
            mode = EnumSource.Mode.EXCLUDE)
    void processFailure_whenInappropriateSegmentStatus(ReturnSegmentStatus returnSegmentStatus) {
        var cr = clientReturnGenerator.generateReturnFromClient();
        cr.setStatus(RECEIVED);
        cr.setExternalReturnId(String.valueOf(cr.getId()));
        cr = clientReturnRepository.save(cr);

        LogbrokerMessage message = getMessage(
                ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED, cr.getExternalReturnId(),
                LogisticPointType.SORTING_CENTER, returnSegmentStatus
        );
        tplLrmReturnEventConsumer.accept(message);

        var updatedCr = clientReturnRepository.findByIdOrThrow(cr.getId());
        assertEquals(RECEIVED, updatedCr.getStatus());
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 0);
    }

    @Test
    void processFailure_whenInappropriateFlowStatus() {
        var cr = clientReturnGenerator.generateReturnFromClient();
        cr.setStatus(DELIVERED_TO_SC);
        cr.setExternalReturnId(String.valueOf(cr.getId()));
        cr = clientReturnRepository.save(cr);

        LogbrokerMessage message = getMessage(ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED, cr.getExternalReturnId()
                , LogisticPointType.SORTING_CENTER, ReturnSegmentStatus.IN);
        tplLrmReturnEventConsumer.accept(message);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 0);
    }

    @Test
    void processFailure_whenInappropriateLogisticPointType() {
        var cr = clientReturnGenerator.generateReturnFromClient();
        cr.setStatus(RECEIVED);
        cr.setExternalReturnId(String.valueOf(cr.getId()));
        cr = clientReturnRepository.save(cr);

        LogbrokerMessage message = getMessage(ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED, cr.getExternalReturnId()
                , LogisticPointType.DROPOFF, ReturnSegmentStatus.IN);
        tplLrmReturnEventConsumer.accept(message);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 0);
    }

    @Test
    void processFailure_whenCrNotExists() {
        clientReturnGenerator.generateReturnFromClient();

        LogbrokerMessage message = getMessage(ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED, "12121212",
                LogisticPointType.SORTING_CENTER, ReturnSegmentStatus.IN);
        tplLrmReturnEventConsumer.accept(message);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 0);
    }

    @Test
    void processFailure_whenWrongEventType() {
        var cr = clientReturnGenerator.generateReturnFromClient();
        cr.setStatus(RECEIVED);
        cr.setExternalReturnId(String.valueOf(cr.getId()));
        cr = clientReturnRepository.save(cr);

        LogbrokerMessage message = getMessage(ReturnEventType.RETURN_COMMITTED, cr.getExternalReturnId(),
                LogisticPointType.SORTING_CENTER, ReturnSegmentStatus.IN);
        tplLrmReturnEventConsumer.accept(message);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 0);
    }


    private LogbrokerMessage getMessage(ReturnEventType eventType, String returnId,
                                        LogisticPointType logisticPointType, ReturnSegmentStatus returnSegmentStatus) {
        return new LogbrokerMessage(
                "",
                "{" +
                        "\"id\":294," +
                        "\"returnId\": " + returnId + "," +
                        "\"eventType\":\"" + eventType + "\"," +
                        "\"orderExternalId\":\"111\"," +
                        "\"created\":\"2021-09-28T11:40:07.969476Z\"," +
                        "\"payload\": {\n" +
                        "    \"type\": \"RETURN_SEGMENT_STATUS_CHANGED_PAYLOAD\",\n" +
                        "    \"id\": 1,\n" +
                        "    \"status\": \"" + returnSegmentStatus.name() + "\",\n" +
                        "    \"shipmentFieldsInfo\": {\n" +
                        "      \"shipmentTime\": \"2022-03-04T05:06:07Z\",\n" +
                        "      \"destinationInfo\": {\n" +
                        "        \"type\": \"DROPOFF\",\n" +
                        "        \"partnerId\": 1,\n" +
                        "        \"logisticPointId\": 2,\n" +
                        "        \"name\": \"destination-name\",\n" +
                        "        \"returnSegmentId\": 3\n" +
                        "      },\n" +
                        "      \"recipient\": {\n" +
                        "        \"partnerId\": 1,\n" +
                        "        \"partnerType\": \"DELIVERY_SERVICE\",\n" +
                        "        \"name\": \"recipient-name\",\n" +
                        "        \"courier\": {\n" +
                        "          \"id\": 10,\n" +
                        "          \"uid\": 20,\n" +
                        "          \"name\": \"courier-name\",\n" +
                        "          \"carNumber\": \"car-number\",\n" +
                        "          \"carDescription\": \"car-description\",\n" +
                        "          \"phoneNumber\": \"phone-number\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"logisticPointInfo\": {\n" +
                        "      \"partnerId\": 1,\n" +
                        "      \"logisticPointId\": 1,\n" +
                        "      \"logisticPointExternalId\": \"1\",\n" +
                        "      \"type\": \"" + logisticPointType.getValue() + "\"\n" +
                        "    }\n" +
                        "  }" +
                        "}"
        );
    }
}

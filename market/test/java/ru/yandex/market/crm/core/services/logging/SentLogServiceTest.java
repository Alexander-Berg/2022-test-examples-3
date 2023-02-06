package ru.yandex.market.crm.core.services.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.crm.core.jackson.CustomObjectMapperFactory;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.json.serialization.JsonSerializerImpl;
import ru.yandex.market.crm.platform.commons.SendingPayload;
import ru.yandex.market.crm.util.tskv.TskvParser;

import static org.junit.Assert.assertEquals;

public class SentLogServiceTest {

    private final JsonSerializer jsonSerializer = new JsonSerializerImpl(
            CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper()
    );

    private final SentLogService sentLogService = new SentLogService(
            jsonSerializer,
            "greenslug",
            new SentLogWriterImpl()
    );

    private final PromoLogSource promoLogSource = new
            PromoLogSource(
            "sendingIdValue",
            "variantIdValue",
            "segmentIdValue",
            List.of("TAG", "BAG"),
            Collections.singletonList("YOLO"),
            "shopPromoId"
    );

    private final long timestamp = System.currentTimeMillis();

    private static void assertTskv(String actualTskv, String... expectedMapValues) throws JSONException {
        Map<String, String> actualMap = TskvParser.parse(actualTskv);
        Map<String, String> expectedMap = new HashMap<>();

        Iterator<String> it = Arrays.asList(expectedMapValues).iterator();
        while (it.hasNext()) {
            expectedMap.put(it.next(), it.next());
        }

        assertEquals(expectedMap.size(), actualMap.size());

        for (var e : expectedMap.entrySet()) {
            var key = e.getKey();
            var expectedValue = e.getValue();
            var actualValue = actualMap.get(key);

            if (expectedValue.startsWith("[") || expectedValue.startsWith("{")) {
                JSONAssert.assertEquals(expectedValue, actualValue, false);
            } else {
                assertEquals(expectedValue, actualValue);
            }
        }
    }

    @Test
    public void testEmailLogEntry() throws Exception {
        String logEntry = sentLogService.emailEntry(
                false,
                false, "email@ya.ru",
                "Email Subject",
                triggerLogSource(Map.of(
                        SendingPayload.Key.ORDER_ID.name(), "567",
                        SendingPayload.Key.SKU_IDS.name(), "some_model_id1,some_model_id2"
                )),
                "<email-message-id:123456>",
                11211L,
                timestamp
        ).orElse(null);

        assertTskv(logEntry,
                "control", "false",
                "globalControl", "false",
                "type", "2",
                "triggerId", "triggerIdValue",
                "blockId", "blockIdValue",
                "processId", "processIdValue",
                "segmentId", "segmentIdValue",
                "templateId", "templateIdValue",
                "email", "email@yandex.ru",
                "originalEmail", "email@ya.ru",
                "subject", "Email Subject",
                "messageId", "<email-message-id:123456>",
                "senderAccount", "greenslug",
                "campaignId", "11211",
                "timestamp", String.valueOf(timestamp),
                "payloadJson", "[" +
                        "{\"name\":\"ORDER_ID\",\"value\":\"567\"}," +
                        "{\"name\":\"SKU_IDS\",\"value\":\"some_model_id1,some_model_id2\"}]"
        );
    }

    @Test
    public void testEmailNullableLogEntry() throws Exception {
        String logEntry = sentLogService.emailEntry(
                true,
                false, "email@yandex.kz",
                null,
                promoLogSource,
                null,
                null,
                timestamp
        ).orElse(null);

        assertTskv(logEntry,
                "control", "true",
                "globalControl", "false",
                "type", "1",
                "sendingId", "sendingIdValue",
                "variantId", "variantIdValue",
                "segmentId", "segmentIdValue",
                "email", "email@yandex.ru",
                "originalEmail", "email@yandex.kz",
                "promo_tags", "TAG,BAG",
                "cat_stream", "YOLO",
                "shopPromoId", "shopPromoId",
                "timestamp", String.valueOf(timestamp)
        );
    }

    @Test
    public void testGncLogEntry() throws Exception {
        String logEntry = sentLogService.gncEntry(
                false,
                "new-answer",
                "market",
                123123,
                456456L,
                "actionLink",
                "resourceImgLink",
                Map.of("model", "iphone"),
                triggerLogSource(Map.of(SendingPayload.Key.ORDER_ID.name(), "567")),
                timestamp
        ).orElse(null);

        assertTskv(logEntry,
                "control", "false",
                "type", "2",
                "triggerId", "triggerIdValue",
                "blockId", "blockIdValue",
                "processId", "processIdValue",
                "segmentId", "segmentIdValue",
                "templateId", "templateIdValue",
                "gncType", "new-answer",
                "service", "market",
                "puid", "123123",
                "actor", "456456",
                "action", "actionLink",
                "entity", "resourceImgLink",
                "templateVarsJson", "[{\"name\":\"model\",\"value\":\"iphone\"}]",
                "timestamp", String.valueOf(timestamp),
                "payloadJson", "[{\"name\":\"ORDER_ID\",\"value\":\"567\"}]"
        );
    }

    @Test
    public void testGncNullableLogEntry() throws Exception {

        String logEntry = sentLogService.gncEntry(
                true,
                "some-type",
                "market",
                321321,
                null,
                null,
                null,
                null,
                promoLogSource,
                timestamp
        ).orElse(null);

        assertTskv(logEntry,
                "control", "true",
                "type", "1",
                "sendingId", "sendingIdValue",
                "variantId", "variantIdValue",
                "segmentId", "segmentIdValue",
                "gncType", "some-type",
                "service", "market",
                "promo_tags", "TAG,BAG",
                "cat_stream", "YOLO",
                "shopPromoId", "shopPromoId",
                "puid", "321321",
                "timestamp", String.valueOf(timestamp)
        );
    }

    @Test
    public void testSmsLogEntry() throws Exception {
        String logEntry = sentLogService.smsEntry(
                "text!!!",
                "70001112233",
                null,
                null,
                118L,
                triggerLogSource(Map.of(
                        SendingPayload.Key.ORDER_ID.name(), "567",
                        SendingPayload.Key.SKU_IDS.name(), "some_model_id1,some_model_id2"
                )),
                timestamp
        ).orElse(null);

        assertTskv(logEntry,
                "type", "2",
                "triggerId", "triggerIdValue",
                "blockId", "blockIdValue",
                "processId", "processIdValue",
                "segmentId", "segmentIdValue",
                "templateId", "templateIdValue",
                "phone", "70001112233",
                "text", "text!!!",
                "smsId", "118",
                "timestamp", String.valueOf(timestamp),
                "payloadJson", "[" +
                        "{\"name\":\"ORDER_ID\",\"value\":\"567\"}," +
                        "{\"name\":\"SKU_IDS\",\"value\":\"some_model_id1,some_model_id2\"}]"
        );
    }

    private TriggerLogSource triggerLogSource(Map<String, String> payload) {
        return new TriggerLogSource(
                "triggerIdValue", "blockIdValue", "processIdValue", "segmentIdValue", "templateIdValue", payload, jsonSerializer
        );
    }
}

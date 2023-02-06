package ru.yandex.market.crm.operatorwindow.external;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.CallAggregatedResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.CallDataResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.CallGeneralInfoResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.ParsedJson;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SearchCallsResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartCallsSerializationHelper;
import ru.yandex.market.crm.operatorwindow.services.fraud.SmartcallsResultsTest;
import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.ResourceHelpers;

public class SearchCallsDeserializationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SmartcallsResultsTest.class);
    private static final SmartCallsSerializationHelper helper = SmartcallsTestUtils.createSerializationHelper();
    private static final String EXAMPLE_JSON_PATH = "search-calls.json";
    private static final String SEARCH_CALL_EMPTY_RECORD_JSON_PATH = "search-calls-empty-record.json";
    private static final String UNDEFINED_CALL_RECORD_LIST = "call_records_field_is_undefined.json";
    private static final String SIP_PHONES_JSON_PATH = "search-calls-sip-phones.json";

    @Test
    public void listExample() {
        final SearchCallsResult searchCallsResult = getExampleSearchCallsResult();

        Assertions.assertEquals(3, searchCallsResult.getMeta().getTotalCount());

        List<CallAggregatedResult> expected = Arrays.asList(
                build("25881"),
                build("26045"),
                build("26046")
        );
        assertCollection(expected, searchCallsResult.getResult(), this::simpleAssertCallResultItem);
    }

    @Test
    public void sourceJson() {
        final SearchCallsResult searchCallsResult = getExampleSearchCallsResult();

        final CallAggregatedResult firstItem = searchCallsResult.getResult().get(0);
        Assertions.assertEquals("{\"id\":25881,\"domain_id\":856,\"session_id\":632231565,\"scenario_id\":2820," +
                        "\"datetime_start\":\"2019-05-20 11:37:16\",\"phone_a\":\"79057966465\"," +
                        "\"phone_b\":\"74999387647\"," +
                        "\"is_incoming\":true,\"call_result_code\":3,\"duration\":25," +
                        "\"record_url\":\"https://storage-ru1-radosgw2.voximplant" +
                        ".com/vox-records/2019/05/20" +
                        "/ZjhhMWRkNmNjYzFjM2Y4MDNmN2EyN2JiZWU1Mzg3OTYvaHR0cDovL3d3dy1ydS0yNS0yMzEudm94aW1wbGFudC" +
                        "5jb20vcmVjb3Jkcy8yMDE5LzA1LzIwL2NjMzM0YjFmM2YzZGEzZDkuMTU1ODM1MjI0MS4zNjUyNzExLm1wMw--?rec" +
                        "ord_id=133729395\",\"call_cost\":1.9152421,\"call_data\":\"{\\\"issueId\\\": 298597, " +
                        "\\\"orderDd\\\": \\\"18 мая\\\", \\\"orderId\\\": 6556624, \\\"aMainMenu\\\": \\\"1\\\", " +
                        "\\\"contactId\\\": \\\"370\\\", \\\"clientCity\\\": \\\"Москва\\\", \\\"clientName\\\": " +
                        "\\\"Григорий\\\", \\\"dd_changed\\\": 0, \\\"aChoosePath\\\": \\\"1\\\"," +
                        " \\\"clientphone\\\": \\\"79057966465\\\", \\\"orderStatus\\\": \\\"PICKUP\\\"," +
                        " \\\"orderBonuses\\\": 1, \\\"interactionId\\\": \\\"370\\\"," +
                        " \\\"response_code\\\": 200, \\\"clientsMatched\\\": 1, " +
                        "\\\"aClientsMatched\\\": \\\"1\\\", " +
                        "\\\"orderCheckpoint\\\": \\\"DELIVERY_ARRIVED_PICKUP_POINT\\\"," +
                        " \\\"orderDaysBeforeDd\\\": 0, \\\"aActiveOrdersCount\\\": \\\"больше 0\\\"," +
                        " \\\"orderPaymentMethod\\\": \\\"YANDEX\\\", \\\"clientActiveOrdersCount\\\": 1," +
                        " \\\"clientDeliveredOrdersCount\\\": 10}\"," +
                        "\"call_resources\":\"[{\\\"cost\\\": 0.20339, \\\"unit\\\": \\\"\\\", " +
                        "\\\"used_at\\\": \\\"2019-05-20 11:37:24\\\", \\\"description\\\": \\\"TextToSpeech\\\"," +
                        " \\\"resource_type\\\": \\\"TTS_YANDEX\\\", \\\"transaction_id\\\": 11483728867," +
                        " \\\"resource_quantity\\\": 17, \\\"resource_usage_id\\\": 43635247}," +
                        " {\\\"cost\\\": 1.314564, \\\"unit\\\": \\\"\\\", " +
                        "\\\"used_at\\\": \\\"2019-05-20 11:37:25\\\", \\\"description\\\": \\\"ASR\\\"," +
                        " \\\"resource_type\\\": \\\"ASR\\\", \\\"transaction_id\\\": 11483729197," +
                        " \\\"resource_quantity\\\": 17, \\\"resource_usage_id\\\": 43635245}," +
                        " {\\\"cost\\\": 0.20339, \\\"unit\\\": \\\"\\\"," +
                        " \\\"used_at\\\": \\\"2019-05-20 11:37:44\\\"," +
                        " \\\"description\\\": \\\"TextToSpeech\\\", \\\"resource_type\\\": \\\"TTS_YANDEX\\\", " +
                        "\\\"transaction_id\\\": 11483736237, \\\"resource_quantity\\\": 3, " +
                        "\\\"resource_usage_id\\\": 43635246}]\",\"call_calls\":\"[{\\\"cost\\\": 0.155118," +
                        " \\\"call_id\\\": 555701821, \\\"duration\\\": 25, \\\"incoming\\\": true," +
                        " \\\"start_time\\\": \\\"2019-05-20 11:37:21\\\", \\\"successful\\\": true, " +
                        "\\\"local_number\\\": \\\"74999387647\\\", \\\"remote_number\\\": \\\"79057966465\\\"," +
                        " \\\"transaction_id\\\": 11483724017, \\\"remote_number_type\\\": \\\"pstn\\\"}]\"," +
                        "\"call_records\":\"[{\\\"cost\\\": 0.03878, \\\"duration\\\": 25, \\\"file_size\\\": 0, " +
                        "\\\"record_id\\\": 133729395, \\\"record_url\\\": " +
                        "\\\"https://storage-ru1-radosgw2.voximplant.com/vox-records/2019/05/20/ZjhhMWRkNmNjYzFj" +
                        "M2Y4MDNmN2EyN2JiZWU1Mzg3OTYvaHR0cDovL3d3dy1ydS0yNS0yMzEudm94aW1wbGFudC5jb20vcmVjb3Jkc" +
                        "y8yMDE5LzA1LzIwL2NjMzM0YjFmM2YzZGEzZDkuMTU1ODM1MjI0MS4zNjUyNzExLm1wMw--?record_id=133729395" +
                        "\\\"," +
                        " \\\"start_time\\\": \\\"2019-05-20 11:37:21\\\", \\\"record_name\\\": \\\"Call " +
                        "Recorder\\\", " +
                        "\\\"transaction_id\\\": 11483723957}]\",\"recalc\":true}",
                firstItem.getSourceJson());
    }

    @Test
    public void checkFields() {
        final SearchCallsResult exampleSearchCallsResult = getExampleSearchCallsResult();

        final CallAggregatedResult item = exampleSearchCallsResult.getResult().get(0);

        final CallGeneralInfoResult historyItem = item.getHistoryItem();
        Assertions.assertEquals("25881", historyItem.getId());
        Assertions.assertEquals("856", historyItem.getDomainId());
        Assertions.assertEquals("632231565", historyItem.getSessionId());
        Assertions.assertEquals("2820", historyItem.getScenarioId());
        Assertions.assertEquals("2019-05-20 11:37:16", historyItem.getDatetimeStart());

        Assertions.assertEquals("79057966465", historyItem.getPhoneA());
        Assertions.assertEquals("74999387647", historyItem.getPhoneB());
        Assertions.assertTrue(historyItem.isIncoming());
        Assertions.assertEquals(3, historyItem.getCallResultCode());
        Assertions.assertEquals(25, historyItem.getDuration());
        Assertions.assertEquals("https://storage-ru1-radosgw2.voximplant.com/vox-records/" +
                "2019/05/20/ZjhhMWRkNmNjYzFjM2Y4MDNmN2EyN2JiZWU1Mzg3OTYvaHR0cDovL3d3dy1ydS0yNS0y" +
                "MzEudm94aW1wbGFudC5jb20vcmVjb3Jkcy8yMDE5LzA1LzIwL2NjMzM0YjFmM2YzZGEzZDkuMTU1ODM" +
                "1MjI0MS4zNjUyNzExLm1wMw--?record_id=133729395", historyItem.getRecordUrl());
        Assertions.assertEquals("1.9152421", historyItem.getCallCost());
        Assertions.assertEquals("{\"issueId\": 298597, \"orderDd\": \"18 мая\"," +
                        " \"orderId\": 6556624, \"aMainMenu\": \"1\", \"contactId\": \"370\"," +
                        " \"clientCity\": \"Москва\", \"clientName\": \"Григорий\", \"dd_changed\": 0," +
                        " \"aChoosePath\": \"1\", \"clientphone\": \"79057966465\", \"orderStatus\": \"PICKUP\"," +
                        " \"orderBonuses\": 1, \"interactionId\": \"370\", \"response_code\": 200, " +
                        "\"clientsMatched\": 1, \"aClientsMatched\": \"1\", " +
                        "\"orderCheckpoint\": \"DELIVERY_ARRIVED_PICKUP_POINT\", \"orderDaysBeforeDd\": 0, " +
                        "\"aActiveOrdersCount\": \"больше 0\", \"orderPaymentMethod\": \"YANDEX\"," +
                        " \"clientActiveOrdersCount\": 1, \"clientDeliveredOrdersCount\": 10}",
                historyItem.getCallData());
        Assertions.assertEquals("[{\"cost\": 0.20339, \"unit\": \"\", \"used_at\": \"2019-05-20 11:37:24\", " +
                        "\"description\": \"TextToSpeech\", \"resource_type\": \"TTS_YANDEX\", \"transaction_id\": " +
                        "11483728867, \"resource_quantity\": 17, \"resource_usage_id\": 43635247}, {\"cost\": 1" +
                        ".314564, " +
                        "\"unit\": \"\", \"used_at\": \"2019-05-20 11:37:25\", \"description\": \"ASR\", " +
                        "\"resource_type\": " +
                        "\"ASR\", \"transaction_id\": 11483729197, \"resource_quantity\": 17, \"resource_usage_id\": " +
                        "43635245}, {\"cost\": 0.20339, \"unit\": \"\", \"used_at\": \"2019-05-20 11:37:44\", " +
                        "\"description\": \"TextToSpeech\", \"resource_type\": \"TTS_YANDEX\", \"transaction_id\": " +
                        "11483736237, \"resource_quantity\": 3, \"resource_usage_id\": 43635246}]",
                historyItem.getCallResources());
        Assertions.assertEquals("[{\"cost\": 0.155118, \"call_id\": 555701821, \"duration\": 25, \"incoming\": true, " +
                "\"start_time\": \"2019-05-20 11:37:21\", \"successful\": true, \"local_number\": \"74999387647\", " +
                "\"remote_number\": \"79057966465\", \"transaction_id\": 11483724017, \"remote_number_type\": " +
                "\"pstn\"}]", historyItem.getCalls());
        Assertions.assertEquals("[{\"cost\": 0.03878, \"duration\": 25, \"file_size\": 0, \"record_id\": 133729395, " +
                "\"record_url\": \"https://storage-ru1-radosgw2.voximplant" +
                ".com/vox-records/2019/05/20" +
                "/ZjhhMWRkNmNjYzFjM2Y4MDNmN2EyN2JiZWU1Mzg3OTYvaHR0cDovL3d3dy1ydS0yNS0yMzEudm94aW1wbGFudC5jb20vcmVjb3Jkcy" +
                "8yMDE5LzA1LzIwL2NjMzM0YjFmM2YzZGEzZDkuMTU1ODM1MjI0MS4zNjUyNzExLm1wMw--?record_id=133729395\", " +
                "\"start_time\": \"2019-05-20 11:37:21\", \"record_name\": \"Call Recorder\", " +
                "\"transaction_id\": 11483723957}]", historyItem.getCallRecords());
        Assertions.assertEquals("133729395.mp3", item.getAudioFileName().get());
        Assertions.assertFalse(item.getCallData().isCancelOrderConfirmed());

        final CallAggregatedResult secondItem = exampleSearchCallsResult.getResult().get(1);
        Assertions.assertTrue(secondItem.getCallData().isCancelOrderConfirmed());
        Assertions.assertTrue(secondItem.getCallData().getIssueId().isPresent());
        Assertions.assertEquals(298986L, (long) secondItem.getCallData().getIssueId().get());
        Assertions.assertEquals(6556624, (long) secondItem.getCallData().getOrderId());
    }

    @Test
    public void emptyCallResult() {
        final SearchCallsResult exampleSearchCallsResult = loadSearchCallsResult(SEARCH_CALL_EMPTY_RECORD_JSON_PATH);

        final CallAggregatedResult item = exampleSearchCallsResult.getResult().get(0);

        final CallGeneralInfoResult historyItem = item.getHistoryItem();
        Assertions.assertEquals("24856", historyItem.getId());
        Assertions.assertEquals("856", historyItem.getDomainId());
        Assertions.assertEquals("627423265", historyItem.getSessionId());
        Assertions.assertEquals("2539", historyItem.getScenarioId());
        Assertions.assertEquals("2019-05-16 10:15:53", historyItem.getDatetimeStart());

        Assertions.assertEquals("79202279510", historyItem.getPhoneA());
        Assertions.assertEquals("74999384635", historyItem.getPhoneB());
        Assertions.assertTrue(historyItem.isIncoming());
        Assertions.assertEquals(2, historyItem.getCallResultCode());
        Assertions.assertEquals(0, historyItem.getDuration());
        Assertions.assertEquals("", historyItem.getRecordUrl());
        Assertions.assertEquals("0", historyItem.getCallCost());
        Assertions.assertEquals("{\"clientphone\": \"79202279510\"}",
                historyItem.getCallData());
        Assertions.assertEquals("[]",
                historyItem.getCallResources());
        Assertions.assertEquals("[{\"cost\": 0, \"call_id\": 551458600, \"duration\": 0, \"incoming\": true, " +
                        "\"start_time\": \"2019-05-16 10:15:53\", \"successful\": false, \"local_number\": " +
                        "\"74999384635\", " +
                        "\"remote_number\": \"79202279510\", \"transaction_id\": 0, \"remote_number_type\": \"pstn\"}]",
                historyItem.getCalls());
        Assertions.assertEquals("[]", historyItem.getCallRecords());
        Assertions.assertFalse(item.getCallData().isCancelOrderConfirmed());
    }

    @Test
    public void callRecordsFieldContainsNotListButSingleEmptyObject_waitEmptyCallRecordsInfo() {
        // в поле call_records вместо списка/пустого списка - пустой объект
        final SearchCallsResult exampleSearchCallsResult = loadSearchCallsResult(UNDEFINED_CALL_RECORD_LIST);

        final List<CallAggregatedResult> result = exampleSearchCallsResult.getResult();
        Assertions.assertEquals(2, result.size());

        Assertions.assertFalse(result.get(0).getAudioFileName().isPresent());
        Assertions.assertFalse(result.get(1).getAudioFileName().isPresent());
    }

    @Test
    public void parseSipPhones() {
        List<CallAggregatedResult> res = loadSearchCallsResult(SIP_PHONES_JSON_PATH).getResult();
        Assertions.assertEquals(3, res.size());
        Assertions.assertEquals(Arrays.asList(79057966465L, 79522229954L, 79124686363L),
                CrmCollections.transform(res, CallAggregatedResult::getPhoneAsLong));
    }

    private void assertCollection(List<CallAggregatedResult> expected,
                                  List<CallAggregatedResult> actual,
                                  ItemAssertion<CallAggregatedResult> assertionFn) {
        Assertions.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); ++i) {
            assertionFn.assertEquals(i, expected.get(i), actual.get(i));
        }
    }

    private void simpleAssertCallResultItem(int index,
                                            CallAggregatedResult expected,
                                            CallAggregatedResult actual) {
        Assertions.assertEquals(
                expected.getHistoryItem().getId(),
                actual.getHistoryItem().getId(), String.format("compare history itemId position = %d", index));
    }

    private CallAggregatedResult build(String historyItemId) {
        final CallGeneralInfoResult historyItem = new CallGeneralInfoResult(
                historyItemId,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                0,
                0,
                null,
                null,
                "",
                "",
                "",
                "");
        final CallDataResult callData = new CallDataResult(
                "",
                "",
                null,
                null);
        return new CallAggregatedResult(
                dummyParsedJson(historyItem),
                dummyParsedJson(Collections.emptyList()),
                dummyParsedJson(callData));
    }

    private <T> ParsedJson<T> dummyParsedJson(T value) {
        return new ParsedJson<>(JsonNodeFactory.instance.objectNode(),
                value);
    }

    private SearchCallsResult getExampleSearchCallsResult() {
        return loadSearchCallsResult(EXAMPLE_JSON_PATH);
    }

    private SearchCallsResult loadSearchCallsResult(String jsonPath) {
        byte[] resource = ResourceHelpers.getResource(jsonPath);
        LOG.debug("resource {}: {}", jsonPath, CrmStrings.valueOf(resource));
        return helper.readSearchCalls(resource);
    }

    @FunctionalInterface
    private interface ItemAssertion<T> {
        void assertEquals(int index, T expected, T actual);
    }
}

package ru.yandex.market.crm.operatorwindow.external;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.AttemptResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.AttemptResultBuilder;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.ConfirmFraudCallData;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.MetaResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.PagedResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SearchAttemptsResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartCallsSerializationHelper;
import ru.yandex.market.crm.operatorwindow.util.AssertHelpers;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.utils.serialize.CustomJsonDeserializer;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

public class SearchAttemptsResultParseTest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchAttemptsResultParseTest.class);

    private static final ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory(Optional.empty());

    private static final CustomJsonDeserializer deserializer
            = new CustomJsonDeserializer(objectMapperFactory);

    private static final SmartCallsSerializationHelper serializationHelper =
            SmartcallsTestUtils.createSerializationHelper();


    // ./bin/smartcalls/search-attempts.sh \
    //    > src/test/resources/ru/yandex/market/crm/operatorwindow/external/smartcalls/attempts.json
    private static final String RESPONSE_PATH = "attempts.json";

    @Test
    public void parseAttempts() {
        byte[] resource = ResourceHelpers.getResource(RESPONSE_PATH);
        LOG.trace("resource {}: {}", RESPONSE_PATH, CrmStrings.valueOf(resource));
        MetaResult expectedMeta = new MetaResult(5, 2, 1, 20);
        AttemptResult expectedAttempt = new AttemptResultBuilder()
                .withAttemptCost("0.122034")
                .withAttemptNum(1)
                .withStatus(false)
                .withCallData("{"
                        + "\"UTC\": \"3\", "
                        + "\"date\": \"27 февраля\", "
                        + "\"name\": \"Григорий\", "
                        + "\"phone\": \"79057966465\", "
                        + "\"orderId\": \"1234\", \"order_real\": \"1\", "
                        + "\"order_amount\": \"100\", "
                        + "\"successful_call\": \"\", "
                        + "\"ticket\": \"ticket@12345\""
                        + "}")
                .withCallResources("[{"
                        + "\"cost\": 0.122034, "
                        + "\"unit\": \"\", "
                        + "\"used_at\": \"2019-02-26 14:17:11\", "
                        + "\"description\": \"VoiceMail\", "
                        + "\"resource_type\": \"VOICEMAILDETECTION\", "
                        + "\"transaction_id\": 9729756597, "
                        + "\"resource_quantity\": 1, "
                        + "\"resource_usage_id\": 31178912"
                        + "}]")
                .withCallResultCode(603)
                .withDatetimeEnd("2019-02-26 14:17:44")
                .withDatetimeStart("2019-02-26 14:17:04")
                .withDuration(10)
                .withId("1776685")
                .withRecalc(true)
                .withRecordUrl("")
                .build();
        SearchAttemptsResult expected = new SearchAttemptsResult(new PagedResult<>(true,
                expectedMeta, ImmutableList.of(expectedAttempt)));
        SearchAttemptsResult actual = serializationHelper.readSearchAttempts(resource);
        assertSearchResultMatches(expected, actual);

        ConfirmFraudCallData expectedCallData = new ConfirmFraudCallData(
                "79057966465", "1234", null, "1", "ticket@12345");
        ConfirmFraudCallData actualCallData = deserializer.readObject(
                ConfirmFraudCallData.class, actual.getResult().get(0).getCallData());
        assertAttemptResultCallDataMatches("SearchAttemptsResult.result[0].callData",
                expectedCallData, actualCallData);

    }

    private void assertSearchResultMatches(SearchAttemptsResult expected,
                                           SearchAttemptsResult actual) {
        String prefix = "SearchAttemptsResult";
        Assertions.assertEquals(expected.isSuccess(), actual.isSuccess(), prefix + ".success");
        assertMetaMatches(prefix + ".meta", expected.getMeta(), actual.getMeta());
        AssertHelpers.assertListMatches(prefix + ".result", this::assertAttemptResultMatches,
                expected.getResult(), actual.getResult());
    }

    private void assertMetaMatches(String prefix, MetaResult expected, MetaResult actual) {
        Assertions.assertEquals(expected.getTotalCount(), actual.getTotalCount(), prefix + ".totalCount");
        Assertions.assertEquals(expected.getPageCount(), actual.getPageCount(), prefix + ".pageCount");
        Assertions.assertEquals(expected.getCurrentPage(), actual.getCurrentPage(), prefix + ".currentPage");
        Assertions.assertEquals(expected.getPerPage(), actual.getPerPage(), prefix + ".perPage");
    }

    private void assertAttemptResultMatches(String prefix, AttemptResult expected, AttemptResult actual) {
        Assertions.assertEquals(expected.getId(), actual.getId(), prefix + ".id");
        Assertions.assertEquals(expected.getAttemptNum(), actual.getAttemptNum(), prefix + ".attemptNum");
        Assertions.assertEquals(expected.getRecordUrl(), actual.getRecordUrl(), prefix + ".recordUrl");
        Assertions.assertEquals(expected.isAttemptSuccessful(),
                actual.isAttemptSuccessful(), prefix + ".attemptSuccessful");
        Assertions.assertEquals(expected.getCallData(), actual.getCallData(), prefix + ".callData");
        Assertions.assertEquals(expected.getAttemptCost(), actual.getAttemptCost(), prefix + ".attemptCost");
        Assertions.assertEquals(expected.getDuration(), actual.getDuration(), prefix + ".duration");
        Assertions.assertEquals(expected.isRecalc(), actual.isRecalc(), prefix + ".recalc");
        Assertions.assertEquals(expected.getCallResultCode(), actual.getCallResultCode(), prefix + ".callResultCode");
        Assertions.assertEquals(expected.getCallResources(), actual.getCallResources(), prefix + ".callResources");
        Assertions.assertEquals(expected.getDatetimeStart(), actual.getDatetimeStart(), prefix + ".datetimeStart");
        Assertions.assertEquals(expected.getDatetimeEnd(), actual.getDatetimeEnd(), prefix + ".datetimeEnd");
    }

    private void assertAttemptResultCallDataMatches(String prefix,
                                                    ConfirmFraudCallData expected,
                                                    ConfirmFraudCallData actual) {
        Assertions.assertEquals(expected.getPhone(), actual.getPhone(), prefix + ".phone");
        Assertions.assertEquals(expected.getOrderId(), actual.getOrderId(), prefix + ".orderId");
        Assertions.assertEquals(expected.getApprove(), actual.getApprove(), prefix + ".approve");
        Assertions.assertEquals(expected.getOrderReal(), actual.getOrderReal(), prefix + ".orderReal");
    }

}

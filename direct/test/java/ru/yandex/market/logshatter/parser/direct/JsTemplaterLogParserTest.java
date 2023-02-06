package ru.yandex.market.logshatter.parser.direct;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class JsTemplaterLogParserTest {
    @Test
    public void testParseFailure() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":1," +
                "\"timestamp\":1588130722005,\"scarab:type\":\"COMMON_FAILURE_EVENT\",\"scarab:stable\":true," +
                "\"diagnosis\":\"Error: Field userId is absent\"," +
                "\"failed_event_info\":{\"scarab_type\":\"RENDERER_PROFILE_EVENT\",\"scarab_version\":1," +
                "\"properties\":{\"hinternal\":false,\"render\":31880,\"reportUserTime\":0," +
                "\"responseId\":\"da39a3ee5e6b4b0d3255bfef95601890afd80709-618903-1588130721971-74\"," +
                "\"responseSize\":98772,\"requestBodyParse\":1616,\"requestId\":\"2950880247058043727\"," +
                "\"templatePath\":\"template\",\"templateErrors\":0},\"properties_format\":\"JSON\"}," +
                "\"properties\":\"{\\\"hinternal\\\":false,\\\"render\\\":31880,\\\"reportUserTime\\\":0," +
                "\\\"responseId\\\":\\\"da39a3ee5e6b4b0d3255bfef95601890afd80709-618903-1588130721971-74\\\"," +
                "\\\"responseSize\\\":98772,\\\"requestBodyParse\\\":1616," +
                "\\\"requestId\\\":\\\"2950880247058043727\\\",\\\"templatePath\\\":\\\"template\\\"," +
                "\\\"templateErrors\\\":0}\"}";

        LogParserChecker checker = new LogParserChecker(new JsTemplaterLogParser());

        checker.check(line,
                new Date(1588130722005L),
                5,
                "COMMON_FAILURE_EVENT",
                "hostname.test",
                2950880247058043727L,
                "{\"diagnosis\":\"Error: Field userId is absent\"," +
                        "\"failed_event_info\":{\"scarab_type\":\"RENDERER_PROFILE_EVENT\",\"scarab_version\":1," +
                        "\"properties\":{\"hinternal\":false,\"render\":31880,\"reportUserTime\":0," +
                        "\"responseId\":\"da39a3ee5e6b4b0d3255bfef95601890afd80709-618903-1588130721971-74\"," +
                        "\"responseSize\":98772,\"requestBodyParse\":1616,\"requestId\":\"2950880247058043727\"," +
                        "\"templatePath\":\"template\",\"templateErrors\":0},\"properties_format\":\"JSON\"}," +
                        "\"properties\":\"{\\\"hinternal\\\":false,\\\"render\\\":31880,\\\"reportUserTime\\\":0," +
                        "\\\"responseId\\\":\\\"da39a3ee5e6b4b0d3255bfef95601890afd80709-618903-1588130721971-74\\\"," +
                        "\\\"responseSize\\\":98772,\\\"requestBodyParse\\\":1616," +
                        "\\\"requestId\\\":\\\"2950880247058043727\\\",\\\"templatePath\\\":\\\"template\\\"," +
                        "\\\"templateErrors\\\":0}\"}"
        );
    }

    @Test
    public void testParseProfile() throws Exception {
        String line = "{\n" +
                "  \"hrobot\": false,\n" +
                "  \"hinternal\": false,\n" +
                "  \"responseId\": \"da39a3ee5e6b4b0d3255bfef95601890afd80709-768696-1588130730898-39\",\n" +
                "  \"requestBodyParse\": 2572,\n" +
                "  \"render\": 22511,\n" +
                "  \"provider\": \"RENDERER\",\n" +
                "  \"scarab:stable\": true,\n" +
                "  \"scarab:type\": \"RENDERER_PROFILE_EVENT\",\n" +
                "  \"timestamp\": 1588130730921,\n" +
                "  \"scarab:version\": 1,\n" +
                "  \"scarab:format\": {\n" +
                "    \"version\": 2,\n" +
                "    \"type\": \"json\"\n" +
                "  },\n" +
                "  \"reqCanceled\": false,\n" +
                "  \"responseSize\": 16257,\n" +
                "  \"templatePath\": \"template\",\n" +
                "  \"templateErrors\": 0,\n" +
                "  \"reportUserTime\": 1588130730,\n" +
                "  \"userId\": {},\n" +
                "  \"userInterface\": \"UNKNOWN\",\n" +
                "  \"requestId\": \"2950889906438221254\"\n" +
                "}";

        LogParserChecker checker = new LogParserChecker(new JsTemplaterLogParser());

        checker.check(line,
                new Date(1588130730921L),
                921,
                "RENDERER_PROFILE_EVENT",
                "hostname.test",
                2950889906438221254L,
                "{\"hrobot\":false,\"hinternal\":false,\"responseId\":\"da39a3ee5e6b4b0d3255bfef95601890afd80709" +
                        "-768696-1588130730898-39\",\"requestBodyParse\":2572,\"render\":22511," +
                        "\"provider\":\"RENDERER\",\"reqCanceled\":false,\"responseSize\":16257," +
                        "\"templatePath\":\"template\",\"templateErrors\":0,\"reportUserTime\":1588130730," +
                        "\"userId\":{},\"userInterface\":\"UNKNOWN\",\"requestId\":\"2950889906438221254\"}"
        );
    }

}

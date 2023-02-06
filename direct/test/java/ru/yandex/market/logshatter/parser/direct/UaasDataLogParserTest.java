package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class UaasDataLogParserTest {
    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "{\"log_time\":\"2020-01-10 18:09:50\",\"method\":\"feature_dev.abt_info\",\"service\":\"direct" +
            ".intapi\",\"ip\":\"87.250.243.51\",\"reqid\":1770965300481066087,\"log_hostname\":\"ppcdev6.yandex.ru\"," +
            "\"log_type\":\"uaas_data\",\"data\":{\"ClientID\":46151699,\"yandexuid\":\"3912107481532678204\"," +
            "\"exp_boxes\":\"102710,0,16;194899,0,31;190412,0,56\",\"features\":\"test_feature_client_3, " +
            "test_feature_client_4, test_feature_1, test_feature_2\"}}";

        LogParserChecker checker = new LogParserChecker(new UaasDataLogParser());

        dateFormat = new SimpleDateFormat(UaasDataLogParser.DATE_PATTERN);

        checker.check(line,
            dateFormat.parse("2020-01-10 18:09:50"),
            1770965300481066087L,
            46151699L,
            "3912107481532678204",
            "102710,0,16;194899,0,31;190412,0,56",
            "test_feature_client_3, test_feature_client_4, test_feature_1, test_feature_2"
        );
    }

    @Test
    public void testNullYandexuidParse() throws Exception {
        String line = "{\"log_time\":\"2020-01-10 18:09:50\",\"method\":\"feature_dev.abt_info\",\"service\":\"direct" +
            ".intapi\",\"ip\":\"87.250.243.51\",\"reqid\":1770965300481066087,\"log_hostname\":\"ppcdev6.yandex.ru\"," +
            "\"log_type\":\"uaas_data\",\"data\":{\"ClientID\":46151699,\"yandexuid\":null," +
            "\"exp_boxes\":\"102710,0,16;194899,0,31;190412,0,56\",\"features\":\"test_feature_client_3, " +
            "test_feature_client_4, test_feature_1, test_feature_2\"}}";

        LogParserChecker checker = new LogParserChecker(new UaasDataLogParser());

        dateFormat = new SimpleDateFormat(UaasDataLogParser.DATE_PATTERN);

        checker.check(line,
            dateFormat.parse("2020-01-10 18:09:50"),
            1770965300481066087L,
            46151699L,
            "",
            "102710,0,16;194899,0,31;190412,0,56",
            "test_feature_client_3, test_feature_client_4, test_feature_1, test_feature_2"
        );
    }
}

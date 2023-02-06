package ru.yandex.market.api.partner.controllers.feed;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;

@ParametersAreNonnullByDefault
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
@DbUnitDataSet(before = {
        "FeedLogControllerV2Test.csv",
        "FeedLogControllerV2FitersTest.csv"})
class FeedLogControllerV2FitersTest extends AbstractFeedLogControllerV2Test {

    @Test
    void testSuccessFeedLogDateRangeXml1NewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        576483,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T08:20:00+03:00",
                                "published_time_to", "2019-07-08T23:59:59+03:00"));
        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>3</total>" +
                "        <feed id=\"576483\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2722\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:57:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2720\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:25:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2718\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:50:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testSuccessFeedLogDateRangeXml2NewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        576483,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T00:00:00+03:00",
                                "published_time_to", "2019-07-08T08:19:59+03:00"));
        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>2</total>" +
                "        <feed id=\"576483\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2717\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:15:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2715\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T07:44:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testSuccessFeedLogPaginationXml1NewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        576483,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T00:00:00+03:00",
                                "published_time_to", "2019-07-08T23:59:59+03:00",
                                "limit", "3"));
        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>5</total>" +
                "        <feed id=\"576483\"/>" +
                "         <index-log-records>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2722\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:57:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2720\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:25:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2718\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:50:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testSuccessFeedLogPaginationXml2NewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        576483,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T00:00:00+03:00",
                                "published_time_to", "2019-07-08T23:59:59+03:00",
                                "limit", "3",
                                "offset", "1"));
        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>5</total>" +
                "        <feed id=\"576483\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2720\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:25:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2718\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:50:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2717\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:15:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testSuccessFeedLogPaginationXml3NewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        576483,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T00:00:00+03:00",
                                "published_time_to", "2019-07-08T23:59:59+03:00",
                                "limit", "3",
                                "offset", "2"));
        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>5</total>" +
                "        <feed id=\"576483\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2718\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:50:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2717\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T08:15:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "            <index-log-record" +
                "                    status=\"ERROR\"" +
                "                    generation-id=\"2715\"" +
                "                    index-type=\"FULL\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T07:44:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }
}

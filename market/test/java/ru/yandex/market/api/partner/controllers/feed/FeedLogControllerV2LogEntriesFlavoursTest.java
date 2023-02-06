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
        "FeedLogControllerV2LogEntriesFlavoursTest.csv"})
public class FeedLogControllerV2LogEntriesFlavoursTest extends AbstractFeedLogControllerV2Test {
    @Test
    public void testSuccessFeedLogFullGenNotCachedXml() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        479438,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));
        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"479438\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"OK\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <offers" +
                "                        total-count=\"10046\"" +
                "                        rejected-count=\"0\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testSuccessFeedLogFullGenNotCachedJson() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        479438,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":479438" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"OK\"," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"," +
                "                \"offers\":{" +
                "                    \"totalCount\":10046," +
                "                    \"rejectedCount\":0" +
                "                }" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testSuccessFeedLogFullGenCachedXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        479633,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"479633\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"OK\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <offers" +
                "                        total-count=\"11\"" +
                "                        rejected-count=\"0\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testSuccessFeedLogFullGenCachedJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        479633,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":479633" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"OK\"," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"," +
                "                \"offers\":{" +
                "                    \"totalCount\":11," +
                "                    \"rejectedCount\":0" +
                "                }" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testFeedIsEmptyXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        473943,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"473943\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testFeedIsEmptyJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        473943,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{\"id\":473943}," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"ERROR\"," +
                "                \"error\":{" +
                "                    \"type\":\"PARSE_ERROR\"" +
                "                }," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testDownloadHttpErrorXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        471534,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"471534\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <error type=\"DOWNLOAD_HTTP_ERROR\" http-status-code=\"500\" />" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testDownloadHttpErrorJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        471534,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":471534" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"ERROR\"," +
                "                \"error\":{" +
                "                    \"type\":\"DOWNLOAD_HTTP_ERROR\"," +
                "                    \"httpStatusCode\":500" +
                "                }," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testDownloadGenericErrorXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        451775,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"451775\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <error type=\"DOWNLOAD_ERROR\">" +
                "                    <description>" +
                "server status is no status code; " +
                "ERR: errcode: 28, msg: " +
                "Operation timed out after 900000 milliseconds " +
                "with 62689562 out of 79971500 bytes received" +
                "</description>" +
                "                </error>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testDownloadGenericErrorJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        451775,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":451775" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"ERROR\"," +
                "                \"error\":{" +
                "                    \"type\":\"DOWNLOAD_ERROR\"," +
                "                    \"description\":" +
                "                            \"server status is no status code;" +
                " ERR: errcode: 28, msg: " +
                "Operation timed out after 900000 milliseconds " +
                "with 62689562 out of 79971500 bytes received\"" +
                "                }," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testParseGenericErrorXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        436295,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"436295\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\" />" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testParseGenericErrorJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        436295,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":436295" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"ERROR\"," +
                "                \"error\":{" +
                "                    \"type\":\"PARSE_ERROR\"" +
                "                }," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testParseXmlErrorXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        476483,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"476483\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <error type=\"PARSE_XML_ERROR\" />" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testParseXmlErrorJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        476483,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":476483" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"ERROR\"," +
                "                \"error\":{" +
                "                    \"type\":\"PARSE_XML_ERROR\"" +
                "                }," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    public void testToManyRejectedOffersErrorXml() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        476101,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"476101\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"322121\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2017-10-12T13:45:12+03:00\"" +
                "                    published-time=\"2017-10-12T15:45:36+03:00\"" +
                "                    file-time=\"2017-10-12T11:23:00+03:00\">" +
                "                <error type=\"TOO_MANY_REJECTED_OFFERS\" />" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    public void testToManyRejectedOffersErrorJson() throws Exception {

        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        10774,
                        476101,
                        Format.JSON,
                        ImmutableMap.of(
                                "published_time_from", "2017-10-04T00:00:00+03:00",
                                "published_time_to", "2017-10-12T23:59:59+03:00"));

        //language=json
        String expected = "" +
                "{" +
                "    \"status\":\"OK\"," +
                "    \"result\":{" +
                "        \"total\":1," +
                "        \"feed\":{" +
                "            \"id\":476101" +
                "        }," +
                "        \"indexLogRecords\":[" +
                "            {" +
                "                \"generationId\":322121," +
                "                \"indexType\":\"FULL\"," +
                "                \"status\":\"ERROR\"," +
                "                \"error\":{" +
                "                    \"type\":\"TOO_MANY_REJECTED_OFFERS\"" +
                "                }," +
                "                \"downloadTime\":\"2017-10-12T13:45:12+03:00\"," +
                "                \"publishedTime\":\"2017-10-12T15:45:36+03:00\"," +
                "                \"fileTime\":\"2017-10-12T11:23:00+03:00\"" +
                "            }" +
                "        ]" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }
}

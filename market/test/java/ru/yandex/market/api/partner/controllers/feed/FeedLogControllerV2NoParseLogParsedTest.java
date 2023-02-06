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

/**
 * Проверка случаев, когда в поле ParseLogParsed стоит <tt>{@code null}</tt>.
 * <p>
 * В FeedLogControllerV2NoParseLogParsedTest.csv в поле parse_log_parsed указано <tt>{@code null}</tt>.
 * <p>
 * В этом тесте проверяются два случая, так как поле ParseLogParsed используется для
 * <ul>
 * <li>определения типа ошибки;
 * <li>уточнения количества оферов.
 * </ul>
 * <p>
 */
@ParametersAreNonnullByDefault
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
@DbUnitDataSet(before = {
        "FeedLogControllerV2Test.csv",
        "FeedLogControllerV2NoParseLogParsedTest.csv"})
class FeedLogControllerV2NoParseLogParsedTest extends AbstractFeedLogControllerV2Test {

    @Test
    void testSuccessFeedLogFullGenNotCachedXmlNewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        579438,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T00:00:00+03:00",
                                "published_time_to", "2019-07-08T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"579438\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"2722\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"OK\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:57:00+03:00\">" +
                "                <offers" +
                "                        total-count=\"1558\"" +
                "                        rejected-count=\"0\"/>" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    /**
     * Проверка что не падает с NPE попытка определить тип ошибки.
     * <p>
     * В <tt>FeedLogControllerV2NoParseLogParsedTest.csv</tt> для данного теста стоит
     * <ul>
     * <li><tt>{@code download_retcode = 0}</tt>
     * <li><tt>{@code parse_retcode = 2}</tt>
     * <li><tt>{@code parse_log_parse = null}</tt>
     * </ul>
     * <p>
     * Так как <tt>{@code parse_retcode = 2}</tt>, то
     * делается попытка более точно определить тип ошибки
     * по информации из <tt>{@code parse_log_parse }</tt>.
     * <p>
     * В случае если <tt>{@code parse_log_parse = null}</tt> мы не должны падать, а
     * должны показывать обобщённый тип ошибки парсинга
     * {@link ru.yandex.market.api.partner.controllers.feed.model.FeedIndexLogErrorTypeDTO#PARSE_ERROR}
     */
    @Test
    void testParseGenericErrorXmlNewSchema() throws Exception {
        ResponseEntity<String> response =
                requestCampaignFeedIndexLogs(
                        1543192,
                        571534,
                        Format.XML,
                        ImmutableMap.of(
                                "published_time_from", "2019-07-08T00:00:00+03:00",
                                "published_time_to", "2019-07-08T23:59:59+03:00"));

        //language=xml
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "    <result>" +
                "        <total>1</total>" +
                "        <feed id=\"571534\"/>" +
                "        <index-log-records>" +
                "            <index-log-record" +
                "                    generation-id=\"2722\"" +
                "                    index-type=\"FULL\"" +
                "                    status=\"ERROR\"" +
                "                    download-time=\"2019-07-07T23:51:00+03:00\"" +
                "                    published-time=\"2019-07-08T09:57:00+03:00\">" +
                "                <error type=\"PARSE_ERROR\" />" +
                "            </index-log-record>" +
                "        </index-log-records>" +
                "    </result>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }
}

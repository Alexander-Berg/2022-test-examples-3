package ru.yandex.market.api.partner.controllers.feed;

import javax.annotation.ParametersAreNonnullByDefault;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;

@ParametersAreNonnullByDefault
@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
@DbUnitDataSet(before = {"FeedLogControllerV2Test.csv"})
public class FeedControllerV2PublicationTest extends AbstractFeedControllerV2Test {

    @Test
    @DbUnitDataSet(before = {"FeedControllerV2PublicationTest.LastSuccessFullGen.csv"})
    void testLastSuccessFullGenXmlNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 551775, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"551775\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"ERROR\">" +
                "            <error type=\"DOWNLOAD_ERROR\">" +
                "                <description>" +
                "server status is no status code; ERR: errcode: 28, msg: " +
                "Operation timed out after 900000 milliseconds " +
                "with 62689562 out of 79971500 bytes received" +
                "</description>" +
                "             </error>" +
                "        </download>" +
                "        <content status=\"NA\"/>" +
                "        <publication status=\"OK\">" +
                "            <full" +
                "                published-time=\"2019-07-08T09:25:00+03:00\"/>" +
                "            <price-and-stock-update" +
                "                published-time=\"2019-07-08T09:25:00+03:00\"/>" +
                "        </publication>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    @DbUnitDataSet(before = {"FeedControllerV2PublicationTest.LastSuccessDiffGen.csv"})
    void testLastSuccessDiffGenXmlNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 551775, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"551775\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"ERROR\">" +
                "            <error type=\"DOWNLOAD_ERROR\">" +
                "                <description>" +
                "server status is no status code; ERR: errcode: 28, msg: " +
                "Operation timed out after 900000 milliseconds " +
                "with 62689562 out of 79971500 bytes received" +
                "</description>" +
                "             </error>" +
                "        </download>" +
                "        <content status=\"NA\"/>" +
                "        <publication status=\"OK\">" +
                "            <full" +
                "                published-time=\"2019-07-08T08:50:00+03:00\"/>" +
                "            <price-and-stock-update" +
                "                published-time=\"2019-07-08T08:50:00+03:00\"/>" +
                "        </publication>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }
}

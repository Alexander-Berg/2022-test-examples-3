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
@DbUnitDataSet(before = {
        "FeedLogControllerV2Test.csv",
        "FeedControllerV2MultipleFeedsTest.csv"})
class FeedControllerV2MultipleFeedsTest extends AbstractFeedControllerV2Test {

    @Test
    void testControllerQueriesReportXmlNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeeds(1543192, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feeds>" +
                "        <feed id=\"1\">" +
                "            <download status=\"NA\"/>" +
                "            <content status=\"NA\"/>" +
                "            <publication status=\"NA\"/>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "        <feed id=\"551775\" url=\"http://nowhere.com/feed.xml\">" +
                "            <download status=\"NA\"/>" +
                "            <content status=\"NA\"/>" +
                "            <publication status=\"NA\"/>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "        <feed id=\"571534\" url=\"http://nowhere.com/feed.xml\">" +
                "            <download status=\"NA\"/>" +
                "            <content status=\"NA\"/>" +
                "            <publication status=\"NA\"/>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "        <feed id=\"576101\" url=\"http://nowhere.com/feed.xml\">" +
                "            <download status=\"NA\"/>" +
                "            <content status=\"NA\"/>" +
                "            <publication status=\"NA\"/>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "        <feed id=\"576476\" url=\"http://nowhere.com/feed.xml\">" +
                "            <download status=\"NA\"/>" +
                "            <content status=\"NA\"/>" +
                "            <publication status=\"NA\"/>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "        <feed id=\"576483\" url=\"http://nowhere.com/feed.xml\">" +
                "            <download status=\"NA\"/>" +
                "            <content status=\"NA\"/>" +
                "            <publication status=\"NA\"/>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "        <feed id=\"579438\" url=\"http://nowhere.com/feed.xml\">" +
                "            <download status=\"OK\"/>" +
                "            <content status=\"OK\" total-offers-count=\"1668\" rejected-offers-count=\"110\"/>" +
                "            <publication status=\"OK\">" +
                "                <full" +
                "                    published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "                <price-and-stock-update" +
                "                    published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "            </publication>" +
                "            <placement status=\"NA\"/>" +
                "        </feed>" +
                "    </feeds>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }
}

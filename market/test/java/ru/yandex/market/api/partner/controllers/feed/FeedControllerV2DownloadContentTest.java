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
        "FeedLogControllerV2LogEntriesFlavoursTest.csv"})
class FeedControllerV2DownloadContentTest extends AbstractFeedControllerV2Test {

    @Test
    void testSuccessFeedLogFullGenNotCachedXmlNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 579438, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"579438\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"OK\"/>" +
                "        <content status=\"OK\" total-offers-count=\"1668\" rejected-offers-count=\"110\"/>" +
                "        <publication status=\"OK\">" +
                "            <full" +
                "                published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "            <price-and-stock-update" +
                "                published-time=\"2019-07-08T09:57:00+03:00\"/>" +
                "        </publication>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testSuccessFeedLogFullGenNotCachedJsonNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 579438, Format.JSON);
        //language=json
        String expected = "" +
                "{" +
                "    \"feed\":{" +
                "        \"id\":579438," +
                "        \"url\":\"http://nowhere.com/feed.xml\"," +
                "        \"download\":{" +
                "            \"status\":\"OK\"" +
                "        }," +
                "        \"content\":{" +
                "            \"status\":\"OK\"," +
                "            \"totalOffersCount\":1668," +
                "            \"rejectedOffersCount\":110" +
                "        }," +
                "        \"publication\":{" +
                "            \"full\":{" +
                "                \"publishedTime\":\"2019-07-08T09:57:00+03:00\"" +
                "            }," +
                "            \"priceAndStockUpdate\":{" +
                "                \"publishedTime\":\"2019-07-08T09:57:00+03:00\"" +
                "            }," +
                "            \"status\":\"OK\"" +
                "        }," +
                "        \"placement\":{" +
                "            \"status\":\"NA\"" +
                "        }" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    void testDownloadHttpErrorXmlNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 571534, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"571534\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"ERROR\">" +
                "            <error type=\"DOWNLOAD_HTTP_ERROR\" http-status-code=\"500\"/>" +
                "        </download>" +
                "        <content status=\"NA\"/>" +
                "        <publication status=\"NA\"/>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testDownloadHttpErrorJsonNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 571534, Format.JSON);
        //language=json
        String expected = "" +
                "{" +
                "    \"feed\":{" +
                "        \"id\":571534," +
                "        \"url\":\"http://nowhere.com/feed.xml\"," +
                "        \"download\":{" +
                "            \"status\":\"ERROR\"," +
                "            \"error\":{" +
                "                \"type\":\"DOWNLOAD_HTTP_ERROR\"," +
                "                \"httpStatusCode\":500" +
                "            }" +
                "        }," +
                "        \"content\":{" +
                "            \"status\":\"NA\"" +
                "        }," +
                "        \"publication\":{" +
                "            \"status\":\"NA\"" +
                "        }," +
                "        \"placement\":{" +
                "            \"status\":\"NA\"" +
                "        }" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    void testToManyRejectedOffersErrorXmlNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 576101, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"576101\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"OK\"/>" +
                "        <content status=\"ERROR\">" +
                "            <error type=\"TOO_MANY_REJECTED_OFFERS\"/>" +
                "        </content>" +
                "        <publication status=\"NA\"/>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testToManyRejectedOffersErrorJsonNewSchema() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 576101, Format.JSON);
        //language=json
        String expected = "" +
                "{" +
                "    \"feed\":{" +
                "        \"id\":576101," +
                "        \"url\":\"http://nowhere.com/feed.xml\"," +
                "        \"download\":{" +
                "            \"status\":\"OK\"" +
                "        }," +
                "        \"content\":{" +
                "            \"status\":\"ERROR\"," +
                "            \"error\":{" +
                "                \"type\":\"TOO_MANY_REJECTED_OFFERS\"" +
                "            }" +
                "        }," +
                "        \"publication\":{" +
                "            \"status\":\"NA\"" +
                "        }," +
                "        \"placement\":{" +
                "            \"status\":\"NA\"" +
                "        }" +
                "    }" +
                "}\n";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }
}

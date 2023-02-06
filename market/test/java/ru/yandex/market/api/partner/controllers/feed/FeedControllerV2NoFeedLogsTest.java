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
class FeedControllerV2NoFeedLogsTest extends AbstractFeedControllerV2Test {

    @Test
    void testNoFeedLogsXml() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(10774, 476476, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"476476\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"NA\"/>" +
                "        <content status=\"NA\"/>" +
                "        <publication status=\"NA\"/>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testNoFeedLogsJson() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(10774, 476476, Format.JSON);
        //language=json
        String expected = "" +
                "{" +
                "    \"feed\":{" +
                "        \"id\":476476," +
                "        \"url\":\"http://nowhere.com/feed.xml\"," +
                "        \"download\":{" +
                "            \"status\":\"NA\"" +
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
    void testNoFeedLogsXmlRed() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 576476, Format.XML);
        //language=xml
        String expected = "" +
                "<response>" +
                "    <feed id=\"576476\" url=\"http://nowhere.com/feed.xml\">" +
                "        <download status=\"NA\"/>" +
                "        <content status=\"NA\"/>" +
                "        <publication status=\"NA\"/>" +
                "        <placement status=\"NA\"/>" +
                "    </feed>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    void testNoFeedLogsJsonRed() throws Exception {
        ResponseEntity<String> response = requestCampaignFeed(1543192, 576476, Format.JSON);
        //language=json
        String expected = "" +
                "{" +
                "    \"feed\":{" +
                "        \"id\":576476," +
                "        \"url\":\"http://nowhere.com/feed.xml\"," +
                "        \"download\":{" +
                "            \"status\":\"NA\"" +
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
}

package ru.yandex.market.crm.mapreduce.util;

import java.util.Map;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUser;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
public class JsonTest {

    @Test
    public void testDeserializeJsonToYson() {
        String value =
                "[{\n" +
                "    \"end_date\": \"2019-04-30T17:17:13.251\", \n" +
                "    \"image\": \"http://yandex.ru/image\",\n" +
                "    \"reason\": \"EMAIL_COMPANY\",\n" +
                "    \"images\": {},\n" +
                "    \"background_color\": \"#ffffff\", \n" +
                "    \"nominal\": 0.0,\n" +
                "    \"subtitle\": \"Coin\",\n" +
                "    \"require_auth\": false,\n" +
                "    \"creation_date\": \"2019-04-29T17:17:13.251\",\n" +
                "    \"id\": 8218,\n" +
                "    \"title\": \"Coin\",\n" +
                "    \"status\": \"ACTIVE\"\n" +
                "}]";

        YTreeNode node = Json.fromJson(YTreeNode.class, value);
        assertNotNull(node);
        assertTrue(node.isListNode());

        assertTrue(node.asList().get(0).isMapNode());

        YTreeMapNode coin = node.asList().get(0).mapNode();
        assertEquals("EMAIL_COMPANY", coin.getString("reason"));
        assertEquals(8218, coin.getInt("id"));
    }

    @Test
    public void testDeserializeCampaignData() {
        String line =
                "{\n" +
                "    \"user\": {\n" +
                "        \"name\": \"\",\n" +
                "        \"idsGraph\": {\n" +
                "            \"nodes\": [\n" +
                "                {\n" +
                "                    \"idType\": \"PUID\",\n" +
                "                    \"idValue\": \"3\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"edges\": []\n" +
                "        },\n" +
                "        \"geo_id\": 0, \n" +
                "        \"cryptaYuids\": [],\n" +
                "        \"cryptaSegments\": [],\n" +
                "        \"segment\": \"variant_a\",\n" +
                "        \"email\": \"email3@yandex.ru\", \n" +
                "        \"email_valid\": true\n" +
                "    },\n" +
                "    \"excludedModels\":[],\n" +
                "    \"vars\": {\n" +
                "      \"COINS\": [\n" +
                "        {\n" +
                "          \"end_date\": \"2019-04-30T17:17:13.251\",\n" +
                "          \"image\": \"http://yandex.ru/image\",\n" +
                "          \"reason\": \"EMAIL_COMPANY\",\n" +
                "          \"images\": {},\n" +
                "          \"background_color\": \"#ffffff\",\n" +
                "          \"nominal\": 0.0,\n" +
                "          \"subtitle\": \"Coin\",\n" +
                "          \"require_auth\": false,\n" +
                "          \"creation_date\": \"2019-04-29T17:17:13.251\",\n" +
                "          \"id\": 8218,\n" +
                "          \"title\": \"Coin\",\n" +
                "          \"status\": \"ACTIVE\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "}";

        CampaignUserData data = Json.fromJson(CampaignUserData.class, line);
        assertNotNull(data);

        CampaignUser user = data.getUserInfo();
        assertNotNull(user);

        Map<String, YTreeNode> vars = data.getVars();
        assertNotNull(vars);
        assertEquals(1, vars.size());

        YTreeNode coins = vars.get("COINS");
        assertNotNull(coins);
        assertTrue(coins.isListNode());
    }
}

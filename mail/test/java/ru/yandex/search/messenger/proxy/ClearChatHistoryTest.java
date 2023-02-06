package ru.yandex.search.messenger.proxy;

import java.util.Arrays;
import java.util.Collections;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.test.util.TestBase;

public class ClearChatHistoryTest extends TestBase {
    @Test
    public void testSuggestAndRecents() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String guid1 = "18bba94a-537f-4e4c-9b79-f313f312ef45";
            String guid2 = "33bba94a-537f-4e4c-9b79-f313f312ef33";

            cluster.addUser(guid1, "Karl");
            JsonMap selfUserData = cluster.addUser(guid2, "Klara");
            String chatId = guid1 + "_" + guid2;

            cluster.updateChat(chatId, 1L);
            cluster.chatMembers(
                chatId,
                Arrays.asList(guid1, guid2),
                Collections.emptyList(),
                1L,
                0L);

            cluster.addTextMessage(
                chatId,
                10L,
                guid1,
                "шуры муры надо скрыть");

            String messageInChatBaseUri =
                cluster.moxy().host()
                    + "/api/search/messenger/suggest?suggest-types=messages"
                    + "&uid=0&length=10&message_get=id&timeout=10s"
                    + "&chat-id-filter=" + chatId;

            String globalMessageBaseUri =
                cluster.moxy().host()
                    + "/api/search/messenger/suggest?suggest-types=messages"
                    + "&uid=0&length=10&message_get=id&timeout=10s&global";

            String recentsBaseUri =
                cluster.moxy().host()
                    + "/api/search/messenger/recents?&length=20&allow_cached=false&guid=";

            HttpAssert.assertJsonResponse(
                client,
                messageInChatBaseUri + "&user-id=" + guid2 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"шуры\"]},"
                    + "\"id\":\"message_" + chatId + "/10\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                globalMessageBaseUri + "&user-id=" + guid2 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"шуры\"]},"
                    + "\"id\":\"message_" + chatId + "/10\"}]}");

//            HttpAssert.assertJsonResponse(
//                client,
//                recentsBaseUri + guid2,
//                "{\"recents\":[{\"chat_last_message_timestamp\":\"10\"," +
//                    "\"chat_members\":\"18bba94a-537f-4e4c-9b79-f313f312ef45\\n33bba94a-537f-4e4c-9b79-f313f312ef33" +
//                    "\\n\",\"chat_message_count\":\"85\",\"user_id\":\"18bba94a-537f-4e4c-9b79-f313f312ef45\"," +
//                    "\"user_data\":{\"org_id\":0,\"guid\":\"18bba94a-537f-4e4c-9b79-f313f312ef45\"," +
//                    "\"is_robot\":false,\"display_name\":\"Karl\",\"passport_display_name\":\"Karl\",\"version\":0," +
//                    "\"is_display_restricted\":false}}]}");

            cluster.clearChatHistory(guid2, chatId, 11L);
            // no messages after clear
            HttpAssert.assertJsonResponse(
                client,
                recentsBaseUri + guid2 + "&debug=true",
                RecentsTest.resp(
                    RecentsTest.userResp(selfUserData)));
            System.out.println(cluster.chatsBackend().getSearchOutput("/search?prefix=0&text=id:*&get=*&hr"));
            HttpAssert.assertJsonResponse(
                client,
                messageInChatBaseUri + "&user-id=" + guid2 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":[]}");

            HttpAssert.assertJsonResponse(
                client,
                globalMessageBaseUri + "&user-id=" + guid2 + "&request=шуры&debug=true",
                "{\"retry-suggest-types\":[],\"suggest\":[]}");

            // test that for other user for chat message will be visible
            HttpAssert.assertJsonResponse(
                client,
                messageInChatBaseUri + "&user-id=" + guid1 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"шуры\"]},"
                    + "\"id\":\"message_" + chatId + "/10\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                globalMessageBaseUri + "&user-id=" + guid1 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"шуры\"]},"
                    + "\"id\":\"message_" + chatId + "/10\"}]}");

            // test that next message will be visible
            cluster.addTextMessage(
                chatId,
                12L,
                guid1,
                "шуры муры вернулись");

            HttpAssert.assertJsonResponse(
                client,
                messageInChatBaseUri + "&user-id=" + guid2 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"шуры\"]},"
                    + "\"id\":\"message_" + chatId + "/12\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                globalMessageBaseUri + "&user-id=" + guid2 + "&request=шуры",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"шуры\"]},"
                    + "\"id\":\"message_" + chatId + "/12\"}]}");
        }
    }

    @Test
    public void testChatMediaClearHistory() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String chatId = "0/0/83625ba8-6075-4504-bc15-237670a4e710";
            String guid = "18bba94a-537f-4e4c-9b79-f313f312ef45";

            cluster.updateChat(chatId);
            cluster.addUser(guid, "Shura");
            cluster.addTextMessage(
                chatId,
                10L,
                guid,
                "наши шуры опять хмуры");

            String uri = cluster.moxy().host() +
                "/api/search/messenger/chat-media?chat-id=" + chatId
                + "&types=text_message&print-data=false&user-id=" + guid;
            HttpAssert.assertJsonResponse(
                client,
                uri + "&near=10&next=1&prev=1",
                "{\"info\":{\"prev\":0,\"next\":0," +
                "\"has_prev\":false,\"has_next\":false},\"messages\":[{\"timestamp\":10}]}");

            cluster.addTextMessage(
                chatId,
                9L,
                guid,
                "наши шуры опять хмуры");

            cluster.addTextMessage(
                chatId,
                11L,
                guid,
                "наши шуры опять хмуры");

            cluster.addTextMessage(
                chatId,
                12L,
                guid,
                "наши шуры опять хмуры");

            cluster.clearChatHistory(guid, chatId, 10L);

            HttpAssert.assertJsonResponse(
                client,
                uri + "&near=10&next=1&prev=1&debug=true",
                "{\"info\":{\"prev\":0,\"next\":1,\"has_prev\":false,\"has_next\":true}," +
                    "\"messages\":[{\"timestamp\":11}]}");

            HttpAssert.assertJsonResponse(
                client,
                uri + "&near=10&next=0&prev=1&debug=true",
                "{\"info\":{\"prev\":0,\"next\":0,\"has_prev\":false,\"has_next\":true}," +
                    "\"messages\":[]}");
            HttpAssert.assertJsonResponse(
                client,
                uri + "&near=7&next=0&prev=1&debug=true",
                "{\"info\":{\"prev\":0,\"next\":0,\"has_prev\":false,\"has_next\":true}," +
                    "\"messages\":[]}");
            HttpAssert.assertJsonResponse(
                client,
                uri + "&near=7&next=1&prev=1&debug=true",
                "{\"info\":{\"prev\":0,\"next\":1,\"has_prev\":false,\"has_next\":true}," +
                    "\"messages\":[{\"timestamp\":11}]}");
            HttpAssert.assertJsonResponse(
                client,
                uri + "&near=12&next=3&prev=3&debug=true",
                "{\"info\":{\"prev\":1,\"next\":0,\"has_prev\":false,\"has_next\":false}," +
                    "\"messages\":[{\"timestamp\":11},{\"timestamp\":12}]}");
        }
    }
}

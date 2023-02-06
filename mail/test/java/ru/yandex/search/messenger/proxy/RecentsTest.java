package ru.yandex.search.messenger.proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonNull;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.PositionSavingContainerFactory;
import ru.yandex.json.writer.JsonType;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.messenger.SearchPrivacy;
import ru.yandex.test.util.TestBase;

public class RecentsTest extends TestBase {
    public static JsonMap chatResp(
        final JsonMap userData,
        final String member1,
        final String member2,
        final Long lastMesTs,
        final Long messageCount)
        throws Exception
    {
        JsonMap result = userResp(userData);
        if (lastMesTs != null) {
            result.put("chat_last_message_timestamp", new JsonString(lastMesTs.toString()));
        } else {
            result.put("chat_last_message_timestamp", JsonNull.INSTANCE);
        }
        if (messageCount != null) {
            result.put("chat_message_count", new JsonString(messageCount.toString()));
        } else {
            result.put("chat_message_count", JsonNull.INSTANCE);
        }
        result.put("chat_members", new JsonString(member1 + '\n' + member2 + '\n'));
        return result;
    }

    public static JsonMap userResp(
        final JsonMap userData,
        final String contactName,
        final long lastSeen)
        throws Exception
    {
        JsonMap map = userResp(userData, contactName);
        map.put("last_online_ts", new JsonLong(lastSeen));
        return map;
    }

    public static JsonMap userResp(
        final JsonMap userData,
        final String contactName)
        throws Exception
    {
        JsonMap map = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        map.put("user_id", userData.get("guid"));
        map.put("user_data", userData);
        if (contactName != null) {
            map.put("contact_name", new JsonString(contactName));
        }
        return map;
    }

    public static JsonMap userResp(final JsonMap userData) throws Exception {
        return userResp(userData, null);
    }

    public static String resp(final JsonMap... items) throws Exception {
        JsonMap result = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        JsonList recents = new JsonList(PositionSavingContainerFactory.INSTANCE);
        recents.addAll(Arrays.asList(items));
        result.put("recents",  recents);
        return JsonType.HUMAN_READABLE.toString(result);
    }

    public static String usersResp(final JsonMap... userDatas) throws Exception {
        JsonMap result = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        JsonList recents = new JsonList(PositionSavingContainerFactory.INSTANCE);
        for (JsonMap userData: userDatas) {
            recents.add(userResp(userData));
        }

        result.put("recents",  recents);
        return JsonType.HUMAN_READABLE.toString(result);
    }

    @Test
    public void testRecents() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String userId = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";
            String noToShowUser1 = "94283279-b06d-45f9-b5fe-340e85af77bf";
            String publicChat1 = "0/0/b5976bcc-03b8-4c20-9ed2-223c485e7d80";
            String contact1 = "be53278b-9813-4716-b56c-4cc508fa8269";
            String contact2 = "9a811962-9e57-85a0-ffb8-50477eaef56d";
            String contact3 = "5e4f355e-f235-4710-92ec-f17943ed8ba2";

            JsonMap selfUserData = cluster.addUser(userId, "А");

            cluster.updateChat(publicChat1);
            cluster.chatMembers(
                publicChat1,
                Collections.singleton(noToShowUser1),
                Collections.emptyList(),
                1,
                0);

            String uri =
                cluster.moxy().host()
                    + "/api/search/messenger/recents?guid=" + userId + "&length=20&allow_cached=false";
            HttpAssert.assertJsonResponse(client, uri, "{\"recents\":[]}");

            String contact1Name = "Владислав Таболин";
            JsonMap contact1Data = cluster.addUser(contact1, "Владислав Т.");
            cluster.addUser(noToShowUser1, "Wrong User");
            cluster.addContact(userId, contact1, contact1Name, 1L);
            cluster.lastSeen(Collections.emptyMap());
            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(userResp(selfUserData)));
            cluster.lastSeen(
                Collections.singletonMap(contact1, System.currentTimeMillis()));

            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    userResp(contact1Data, contact1Name)));
            // test on 3 contacts
            Map<String, Long> lastSeenMap = new LinkedHashMap<>();
            lastSeenMap.put(contact1, 10L);
            lastSeenMap.put(contact2, 20L);
            lastSeenMap.put(contact3, 15L);

            String contact2Name = "Олег Кузнецов";
            JsonMap contact2Data = cluster.addUser(contact2, "Олег К.");
            cluster.addContact(userId, contact2, contact2Name, 2L, 1L);

            String contact3Name = "Мария Смолина";
            JsonMap contact3Data = cluster.addUser(contact3, "Мария Г.");
            cluster.addContact(userId, contact3, contact3Name, 3L, 2L);
            cluster.lastSeen(lastSeenMap, 7);
            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    userResp(contact2Data, contact2Name),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));
            // test search privacy, i'm - not in user contacts
            cluster.setUserPrivacy(contact3, SearchPrivacy.CONTACTS.value());
            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    userResp(contact2Data, contact2Name),
                    userResp(contact1Data, contact1Name)));

            String myContactName = "Иван Дудинов";
            // test search privacy, added to contacts - should return me to recents
            cluster.addContact(contact3, userId, myContactName,1L);
            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    userResp(contact2Data, contact2Name),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));

            // test nobody privacy
            cluster.setUserPrivacy(contact2, SearchPrivacy.NOBODY.value());

            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));
            // test chats
            String user4 = "2f5fdfab-1aef-4d4e-81f9-7cb05e092196";
            JsonMap user4Data = cluster.addUser(user4, "Леонид Ш.");
            JsonMap chat4data = cluster.updatePvpChat(userId, user4, 0L);
            String userUser4Chat = chat4data.getString("chat_id");
            cluster.chatMembers(
                userUser4Chat,
                Arrays.asList(userId, user4),
                Collections.emptyList(),
                1L,
                0L);
//            // Print out current index state for debug
//            System.out.println(
//               cluster.chatsBackend().getSearchOutput("/search?text=id:*&get=*&hr"));

            // checking that we not consider hided chat without messages
            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    chatResp(user4Data, userId, user4, null, null),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));

            // now hide chat, and check that it gone from  recents
            long hideTs = 1588015772438011L;
            cluster.addTextMessage(chat4data.getString("chat_id"), hideTs - 1, userId, "");
            cluster.hiddenChats(userId, Collections.singletonMap(user4, hideTs));

            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));

            // now we send message to chat, it should be unhidden
            //cluster.hiddenChats(userId, Collections.singletonMap(userUser4Chat, hideTs));
            cluster.addTextMessage(chat4data.getString("chat_id"), hideTs + 1, userId, "");

            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(selfUserData),
                    chatResp(user4Data, userId, user4, hideTs + 1, 85L),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));

            // test no contacts, just chats

            // test chats
            JsonMap meData = cluster.addUser(userId, "Иван Дудинов");

            HttpAssert.assertJsonResponse(
                client,
                cluster.moxy().host()
                    + "/api/search/messenger/recents?guid="
                    + user4 + "&length=20&allow_cached=false"
                    + "&debug=true",
                resp(
                    userResp(user4Data),
                    chatResp(meData, userId, user4, hideTs + 1, 85L)));

            // test skipping is_display_restricted true
            String contact5 = "52cac36a-d9cd-47ca-b373-ba99bb6d1385";
            cluster.addUser(
                contact5,
                "Вася К.",
                "vasyatka",
                false,
                true,
                1L);

            cluster.addContact(userId, contact5, "Вася Кроликов", 4L, 3L);
            lastSeenMap.put(contact5, 20000000L);
            cluster.lastSeen(lastSeenMap);

            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(meData),
                    chatResp(user4Data, userId, user4, hideTs + 1, 85L),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));

            // test filter is_robot
            String contact6 = "70ea7ff6-2f37-445a-8c51-add68e88b0cb";
            cluster.addUser(
                contact6,
                "Яков Н.",
                "ayanus",
                true,
                false,
                1L);

            cluster.addContact(userId, contact6, "Яков Невструев", 5L, 4L);
            lastSeenMap.put(contact6, 20000000L);
            cluster.lastSeen(lastSeenMap);

            HttpAssert.assertJsonResponse(
                client,
                uri,
                resp(
                    userResp(meData),
                    chatResp(user4Data, userId, user4, hideTs + 1, 85L),
                    userResp(contact3Data, contact3Name),
                    userResp(contact1Data, contact1Name)));
        }
    }

    @Test
    public void test400() throws Exception {
        QueryConstructor qc = new QueryConstructor("");
        qc.append("text", "(((contact_name_tokenized_p:отф?^1.0) AND (contact_name_tokenized_p:ропаггшгшшпмч8^1.0) AND (contact_name_tokenized_p:3?^1.0)))");
        qc.toString();

    }

    @Test
    public void testDefaultRecents() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String userId = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";

            String contact1 = "be53278b-9813-4716-b56c-4cc508fa8269";
            String contact1Name = "Владислав Таболин";
            JsonMap contact1Data = cluster.addUser(contact1, "Владислав Т.");
            cluster.addContact(userId, contact1, contact1Name, 1L);

            String contact2 = "9a811962-9e57-85a0-ffb8-50477eaef56d";
            String contact2Name = "Олег Кузнецов";
            JsonMap contact2Data = cluster.addUser(contact2, "Олег К.");
            cluster.addContact(userId, contact2, contact2Name, 2L, 1L);

            String contact3 = "5e4f355e-f235-4710-92ec-f17943ed8ba2";
            String contact3Name = "Мария Смолина";
            JsonMap contact3Data = cluster.addUser(contact3, "Мария Г.");
            cluster.addContact(userId, contact3, contact3Name, 3L, 2L);

            Map<String, Long> lastSeenMap = new LinkedHashMap<>();
            lastSeenMap.put(contact1, 10L);
            lastSeenMap.put(contact2, 20L);
            lastSeenMap.put(contact3, 15L);
            cluster.lastSeen(lastSeenMap, 4);

            String user4 = "2f5fdfab-1aef-4d4e-81f9-7cb05e092196";
            JsonMap user4Data = cluster.addUser(user4, "Леонид Ш.");
            JsonMap chat4data = cluster.updatePvpChat(userId, user4, 0L);
            String userUser4Chat = chat4data.getString("chat_id");
            cluster.chatMembers(
                userUser4Chat,
                Arrays.asList(userId, user4),
                Collections.emptyList(),
                1L,
                0L);

            String result = resp(
                chatResp(user4Data, userId, user4, null, null),
                userResp(contact2Data, contact2Name),
                userResp(contact3Data, contact3Name),
                userResp(contact1Data, contact1Name));

            String baseUri = cluster.moxy().host()
                + "/api/search/messenger/recents?guid=" + userId
                + "&length=20&allow_cached=false";

            String uri = baseUri;
            HttpAssert.assertJsonResponse(client, uri, result);

            uri = baseUri + "&ranking=messenger_search_ranking";
            HttpAssert.assertJsonResponse(client, uri, result);

            uri = baseUri + "&ranking=messenger_search_ranking:default";
            HttpAssert.assertJsonResponse(client, uri, result);

            uri = baseUri + "&ranking=messenger_search_ranking:hdsh74h";
            HttpAssert.assertJsonResponse(client, uri, result);
        }
    }

    @Test
    public void testSelfRecent() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String userId = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";

            String contact1 = "be53278b-9813-4716-b56c-4cc508fa8269";
            String contact1Name = "Владислав Таболин";
            JsonMap contact1Data = cluster.addUser(contact1, "Владислав Т.");
            cluster.addContact(userId, contact1, contact1Name, 1L);

            String contact2 = "9a811962-9e57-85a0-ffb8-50477eaef56d";
            String contact2Name = "Олег Кузнецов";
            JsonMap contact2Data = cluster.addUser(contact2, "Олег К.");
            cluster.addContact(userId, contact2, contact2Name, 2L, 1L);

            String contact3 = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";
            String contact3Name = "Мария Смолина";
            JsonMap contact3Data = cluster.addUser(contact3, "Мария Г.");
            cluster.addContact(userId, contact3, contact3Name, 3L, 2L);

            Map<String, Long> lastSeenMap = new LinkedHashMap<>();
            lastSeenMap.put(contact1, 10L);
            lastSeenMap.put(contact2, 20L);
            lastSeenMap.put(contact3, 15L);
            cluster.lastSeen(lastSeenMap);

            String result = resp(
                userResp(contact3Data),
                userResp(contact2Data, contact2Name),
                userResp(contact1Data, contact1Name));

            String uri = cluster.moxy().host()
                + "/api/search/messenger/recents?guid=" + userId
                + "&length=20&allow_cached=false";
            HttpAssert.assertJsonResponse(client, uri, result);
        }
    }
}

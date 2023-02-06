package ru.yandex.search.messenger.proxy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import NMessengerProtocol.Client;
import NMessengerProtocol.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.json.dom.JsonBoolean;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.PositionSavingContainerFactory;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.TestBase;

public class ForwardTest extends TestBase {
    private static final long DAY_MILLIS = 86400000000L;

    private static JsonMap chat(final JsonMap chatData) {
        JsonMap map = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        map.put("chat_id", chatData.get("chat_id"));
        map.put("chat_data", chatData);
        return map;
    }

    private static JsonMap user(final JsonMap userData) {
        return user(userData, null);
    }

    private static JsonMap user(
        final JsonMap userData,
        final String contactName)
    {
        JsonMap map = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        map.put("user_id", userData.get("guid"));
        map.put("user_data", userData);
        if (contactName != null) {
            map.put("contact_name", new JsonString(contactName));
        }
        return map;
    }

    private static JsonMap user(
        final JsonMap userData,
        final String contactName,
        final long lastSeen)
    {
        JsonMap map = user(userData, contactName);
        map.put("last_online_ts", new JsonLong(lastSeen));
        return map;
    }

    private static JsonMap selfChat(final JsonMap chatData) {
        JsonMap map = chat(chatData);
        map.put("saved_messages", JsonBoolean.TRUE);
        return map;
    }

    private static String resp(final JsonMap... items) {
        JsonList resultList = new JsonList(PositionSavingContainerFactory.INSTANCE);
        resultList.addAll(Arrays.asList(items));
        JsonMap result = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        result.put("result",  resultList);
        return JsonType.HUMAN_READABLE.toString(result);
    }

    private static void addLastMessage(
        final MoxyCluster cluster,
        final String from,
        final String chatId,
        final Long ts)
        throws Exception
    {
        String msg = "{\n"
            + "  \"ErrorInfo\": {},\n"
            + "  \"Message\": {\n"
            + "    \"Meta\": {\n"
            + "      \"Origin\": 24\n"
            + "    },\n"
            + "    \"ServerMessage\": {\n"
            + "      \"ServerMessageInfo\": {\n"
            + "        \"From\": {\n"
            + "          \"Version\": " + ts + ",\n"
            + "          \"PhoneId\": \"a2145bb4-1e35-4e6c-9c7b-4c91a2ca4320\",\n"
            + "          \"Guid\": \"" + from + "\",\n"
            + "          \"DisplayName\": \"user\"\n"
            + "        },\n"
            + "        \"Version\": 1,\n"
            + "        \"SeqNo\": 85,\n"
            + "        \"PrevTimestamp\": " + ts + ",\n"
            + "        \"Timestamp\": " + ts + "\n"
            + "      },\n"
            + "      \"ClientMessage\": {\n"
            + "        \"Plain\": {\n"
            + "          \"PayloadId\": \"bba8d681-45eb-4e9a-8203-516c730bf47a\",\n"
            + "          \"ChatId\": \"" + chatId + "\",\n"
            + "          \"Text\": {\n"
            + "            \"MessageText\": \"\"\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        Message.TMessageInfoResponse.Builder builder =
            Message.TMessageInfoResponse.newBuilder();
        new JsonFormat().merge(
            new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8)),
            builder);
        Client.TServerMessage.Builder smb =
            builder.getMessageBuilder().getServerMessageBuilder();
        smb.getServerMessageInfoBuilder().getFromBuilder().setGuid(from);
        smb.getServerMessageInfoBuilder().setTimestamp(ts);
        smb.getClientMessageBuilder().getPlainBuilder().getTextBuilder().setMessageText("");
        cluster.addMessage(builder.build(), chatId, ts);
    }

    @Test
    public void testForwardSuggest() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String userId = "user0";
            cluster.addUser(userId, "Я");
            long userTs = 20L;
            cluster.addContact(userId, userId, "Мой номер", 1L);


            // add contacts

            String contact1 = "user1";
            String contact1Name = "Владислав";
            long contact1Ts = 10L;
            JsonMap contact1Data = cluster.addUser(contact1, "Владислав");
            cluster.addContact(userId, contact1, contact1Name, 2L, 1L);

            String contact2 = "user2";
            String contact2Name = "Олег";
            long contact2Ts = 20L;
            JsonMap contact2Data = cluster.addUser(contact2, "Олег");
            cluster.addContact(userId, contact2, contact2Name, 3L, 2L);

            String contact3 = "user3";
            String contact3Name = "Мария";
            long contact3Ts = 15L;
            JsonMap contact3Data = cluster.addUser(contact3, "Мария");
            cluster.addContact(userId, contact3, contact3Name, 4L, 3L);


            // add pvp chats

            String user4 = "user4";
            JsonMap user4Data = cluster.addUser(user4, "Леонид");
            String contact4Name = "Леня";
            long lastMsgTs4 = System.currentTimeMillis() * 1000 - 30 * DAY_MILLIS;
            JsonMap chat4Data = cluster.updatePvpChat(userId, user4, 0L);
            String chat4Id = chat4Data.getString("chat_id");
            cluster.chatMembers(
                chat4Id,
                Arrays.asList(userId, user4),
                Collections.emptyList(),
                1L,
                0L);
            cluster.addContact(userId, user4, contact4Name, 5L, 4L);

            String user5 = "user5";
            JsonMap user5Data = cluster.addUser(user5, "Юлия");
            long lastMsgTs5 = System.currentTimeMillis() * 1000 - 7 * DAY_MILLIS;
            JsonMap chat5Data = cluster.updatePvpChat(userId, user5, 0L);
            String chat5Id = chat5Data.getString("chat_id");
            cluster.chatMembers(
                chat5Id,
                Arrays.asList(userId, user5),
                Collections.emptyList(),
                1L,
                0L);

            String user6 = "user6";
            JsonMap user6Data = cluster.addUser(user6, "Анна");
            long lastMsgTs6 = System.currentTimeMillis() * 1000 - 31 * DAY_MILLIS;
            JsonMap chat6Data = cluster.updatePvpChat(userId, user6, 0L);
            String chat6Id = chat6Data.getString("chat_id");
            cluster.chatMembers(
                chat6Id,
                Arrays.asList(userId, user6),
                Collections.emptyList(),
                1L,
                0L);

            String user9 = "user9";
            String contact9Name = "Арина";
            long contact9Ts = 5L;
            JsonMap user9Data = cluster.addUser(user9, contact9Name);
            long lastMsgTs9 = System.currentTimeMillis() * 1000 - 56 * DAY_MILLIS;
            JsonMap chat9Data = cluster.updatePvpChat(userId, user9, 0L);
            String chat9Id = chat9Data.getString("chat_id");
            cluster.chatMembers(
                chat9Id,
                Arrays.asList(userId, user9),
                Collections.emptyList(),
                1L,
                0L);
            cluster.addContact(userId, user9, contact9Name, 6L, 5L);

            String user10 = "user10";
            String contact10Name = "Алина";
            long contact10Ts = 40L;
            JsonMap user10Data = cluster.addUser(user10, contact10Name);
            long lastMsgTs10 = System.currentTimeMillis() * 1000 - 90 * DAY_MILLIS;
            JsonMap chat10Data = cluster.updatePvpChat(userId, user10, 0L);
            String chat10Id = chat10Data.getString("chat_id");
            cluster.chatMembers(
                chat10Id,
                Arrays.asList(userId, user10),
                Collections.emptyList(),
                1L,
                0L);
            cluster.addContact(userId, user10, contact10Name, 7L, 6L);


            // add pvp with contact, pvp in result, contact is ignored

            long lastMsgTs3 = System.currentTimeMillis() * 1000 - 8 * DAY_MILLIS;
            JsonMap chat3Data = cluster.updatePvpChat(userId, contact3, 0L);
            String chat3Id = chat3Data.getString("chat_id");
            cluster.chatMembers(
                chat3Id,
                Arrays.asList(userId, contact3),
                Collections.emptyList(),
                1L,
                0L);

            cluster.chatsBackend().flush();
            cluster.messagesBackend().flush();


            // add hidden and unhidden chat

            String user7 = "user7";
            JsonMap user7Data = cluster.addUser(user7, "Антон");
            long lastMsgTs7 = System.currentTimeMillis() * 1000 - 9 * DAY_MILLIS;
            JsonMap chat7Data = cluster.updatePvpChat(userId, user7, 0L);
            String chat7Id = chat7Data.getString("chat_id");
            cluster.chatMembers(
                chat7Id,
                Arrays.asList(userId, user7),
                Collections.emptyList(),
                1L,
                0L);
            cluster.addContact(userId, user7, "Друг", 8L, 7L);

            String user8 = "user8";
            JsonMap user8Data = cluster.addUser(user8, "Оля");
            long lastMsgTs8 = System.currentTimeMillis() * 1000 - 5 * DAY_MILLIS;
            JsonMap chat8Data = cluster.updatePvpChat(userId, user8, 0L);
            String chat8Id = chat8Data.getString("chat_id");
            cluster.chatMembers(
                chat8Id,
                Arrays.asList(userId, user8),
                Collections.emptyList(),
                1L,
                0L);

            Map<String, Long> hidden = new HashMap<>();
            hidden.put(user7, lastMsgTs7);
            hidden.put(user8, lastMsgTs8 - DAY_MILLIS);
            cluster.hiddenChats(userId, hidden);


            // add group chat

            String groupChat1Id = "0/9/00092";
            long lastMsgTsGroupChat1 = System.currentTimeMillis() * 1000 - 3 * DAY_MILLIS;
            JsonMap groupChat1Data = cluster.updateChat(groupChat1Id);
            cluster.chatMembers(
                groupChat1Id,
                Collections.singleton(userId),
                Collections.emptyList(),
                1,
                0);

            String groupChat2Id = "0/9/123";
            long lastMsgTsGroupChat2 = System.currentTimeMillis() * 1000 - 100 * DAY_MILLIS;
            JsonMap groupChat2Data = cluster.updateChat(groupChat2Id);
            cluster.chatMembers(
                groupChat2Id,
                Collections.singleton(userId),
                Collections.emptyList(),
                1,
                0);

            String groupChat3Id = "0/9/00077";
            long lastMsgTsGroupChat3 = System.currentTimeMillis() * 1000 - DAY_MILLIS;
            JsonMap groupChat3Data = cluster.updateChat(groupChat3Id);
            cluster.chatMembers(
                groupChat3Id,
                Collections.singleton(userId),
                Collections.emptyList(),
                1,
                0);


            // add channel, ignored

            String channelId = "1/0/b0082f13-a9cb-4de1-be28-26e81b1cbd54";
            long lastMsgTsChannel = System.currentTimeMillis() * 1000 - DAY_MILLIS;
            cluster.updateChat(channelId);
            cluster.chatMembers(
                channelId,
                Collections.singleton(userId),
                Collections.emptyList(),
                1,
                0);


            // add self chat

            JsonMap selfChatData = cluster.updatePvpChat(userId, userId, 0L);
            long lastMsgSelfChat = System.currentTimeMillis() * 1000 - DAY_MILLIS;
            String selfChatId = selfChatData.getString("chat_id");
            cluster.chatMembers(
                selfChatId,
                Arrays.asList(userId, userId),
                Collections.emptyList(),
                1L,
                0L);


            // add last messages for chats

            addLastMessage(cluster, userId, selfChatId, lastMsgSelfChat); // actual chat
            addLastMessage(cluster, userId, chat4Id, lastMsgTs4); // actual chat
            addLastMessage(cluster, userId, chat5Id, lastMsgTs5); // actual chat
            addLastMessage(cluster, userId, chat6Id, lastMsgTs6); // old chat
            addLastMessage(cluster, userId, chat3Id, lastMsgTs3); // actual chat
            addLastMessage(cluster, "1234", groupChat1Id, lastMsgTsGroupChat1); // actual chat
            addLastMessage(cluster, "0000", groupChat2Id, lastMsgTsGroupChat2); // old chat
            addLastMessage(cluster, "9999", groupChat3Id, lastMsgTsGroupChat3); // actual chat
            addLastMessage(cluster, "3456", channelId, lastMsgTsChannel); // actual chat
            addLastMessage(cluster, userId, chat7Id, lastMsgTs7); // actual chat
            addLastMessage(cluster, userId, chat8Id, lastMsgTs8); // actual chat
            addLastMessage(cluster, userId, chat9Id, lastMsgTs9); // old chat
            addLastMessage(cluster, userId, chat10Id, lastMsgTs10); // old chat

            Map<String, Long> lastSeenMap = new LinkedHashMap<>();
            lastSeenMap.put(contact1, contact1Ts);
            lastSeenMap.put(contact2, contact2Ts);
            lastSeenMap.put(contact3, contact3Ts);
            lastSeenMap.put(user9, contact9Ts);
            lastSeenMap.put(user10, contact10Ts);
            lastSeenMap.put(userId, userTs);
            cluster.lastSeen(lastSeenMap, 4);

            cluster.chatsBackend().flush();
            cluster.messagesBackend().flush();


            String result = resp(
                // pvp
                user(contact3Data, contact3Name, contact3Ts),
                user(user8Data, null),
                user(user5Data, null),
                user(user4Data, contact4Name),
                // group chats
                chat(groupChat3Data),
                chat(groupChat1Data),
                // self chat
                selfChat(selfChatData),
                // contacts
                user(user10Data, contact10Name, contact10Ts),
                user(contact2Data, contact2Name, contact2Ts),
                user(contact1Data, contact1Name, contact1Ts),
                user(user9Data, contact9Name, contact9Ts));

            String uri = cluster.moxy().host()
                + "/api/search/messenger/forward/suggest?guid=" + userId
                + "&length=20&debug=true";

            HttpAssert.assertJsonResponse(client, uri, result);


            // test zen

            String zenResult = resp(
                // pvp
                user(contact3Data, contact3Name, contact3Ts),
                user(user8Data, null),
                user(user5Data, null),
                user(user4Data, contact4Name),
                // group chats
                chat(groupChat3Data),
                chat(groupChat1Data),
                // self chat
                selfChat(selfChatData),
                // contacts with pvp chats
                user(user10Data, contact10Name, contact10Ts),
                user(user9Data, contact9Name, contact9Ts));

            HttpAssert.assertJsonResponse(client, uri + "&sort=zen", zenResult);
        }
    }
}

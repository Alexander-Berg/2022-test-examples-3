package ru.yandex.search.messenger.proxy;

import java.util.Arrays;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class ChatMembersTest extends MoxyTestBase {
    // CSOFF: MultipleStringLiterals
    // CSOFF: ParameterNumber
    // CSOFF: MagicNumber
    // CSOFF: MethodLength

    private static final String PRODUCER_URI =
        "/_status?service=messenger_users&prefix=0&all&json-type=dollar";
    private static final String PRODUCER_RESPONSE = "[{$localhost\0:100500}]";
    private static final long BACKEND_POS = 100500L;
    private static final User USER =
        new User("messenger_users", new LongPrefix(0L));
    private static final String SUGGEST_PREFIX =
        "{\"retry-suggest-types\":[],\"suggest\":[";
    private static final String ID = ",\"id\":\"";

    private static String usersSuggest(
        final String matches,
        final String displayName,
        final String id)
    {
        StringBuilder sb =
            new StringBuilder("{\"type\":\"users\"");
        sb.append(ID);
        sb.append(id);
        if (displayName != null) {
            sb.append("\",\"user_display_name\":\"");
            sb.append(displayName);
        }
        sb.append('"');
        if (matches != null) {
            sb.append(",\"matches\":{");
            sb.append(matches);
            sb.append('}');
        }
        sb.append('}');
        return new String(sb);
    }

    private static String contactsSuggest(
        final String matches,
        final String displayName,
        final String contactName,
        final String id)
    {
        StringBuilder sb =
            new StringBuilder("{\"type\":\"contacts\"");
        sb.append(ID);
        sb.append(id);
        if (displayName != null) {
            sb.append("\",\"user_display_name\":\"");
            sb.append(displayName);
        }
        if (contactName != null) {
            sb.append("\",\"contact_name\":\"");
            sb.append(contactName);
        }
        sb.append('"');
        if (matches != null) {
            sb.append(",\"matches\":{");
            sb.append(matches);
            sb.append('}');
        }
        sb.append('}');
        return new String(sb);
    }

    @Test
    public void testGroupChat() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);

            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);

            String user1 = "Анна Сидоровна";
            String user1Chats = "0/0/123\n1/0/345\n0/0/99";
            String user2 = "Василий";
            String user2Chats = "1/0/345\n0/0/123\n0/0/99";
            String user3 = "AL";
            String user3Chats = "1/0/345\n1/9/555\n3_1\n0/0/123\n0/0/99";
            String user4 = "Юрий Сидоров";
            String user4Chats = "1/0/345\n0/0/99\n1/0/123";
            String user5 = "Аня Иванова";
            String user5Chats = "1/0/00220\n1/0/55383\n0/0/123\n0/0/123";
            String user6 = "Анна Сергеева";
            String user6Chats = "1/0/3737\n1/0/3939\n123_123\n0/9/123\n0/0/9077\n1/0/123";
            cluster.backend().add(
                MoxyTestBase.userDoc(1, user1, "anyasid", null, null, null, user1Chats),
                MoxyTestBase.userDoc(2, user2, "vasya", null, null, null, user2Chats),
                MoxyTestBase.userDoc(3, user3, "anna-l", null, null, null, user3Chats),
                MoxyTestBase.userDoc(4, user4, "yorik", null, null, null, user4Chats),
                MoxyTestBase.userDoc(5, user5, "kitty99", null, null, null, user5Chats),
                MoxyTestBase.userDoc(6, user6, "anna", null, null, null, user6Chats)
            );
            cluster.backend().flush();

            String uri =
                "/api/search/messenger/chat-members?"
                    + "&chat-id-filter=0%2F0%2F123"
                    + "&length=10"
                    + "&timeout=10s"
                    + "&user_get=user_display_name"
                    + "&request=";
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "анна")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                            + usersSuggest("\"user_nickname\":[\"anna\"]", user3, "3") + ','
                            + usersSuggest("\"user_display_name\":[\"Анна\"]", user1, "1") + ','
                            + usersSuggest("", user5, "5")
                            + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testChannel() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);

            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);

            String user1 = "Анна Сидоровна";
            String user1Chats = "0/0/123\n1/0/345\n0/0/99";
            String user2 = "Василий";
            String user2Chats = "1/0/345\n0/0/123\n0/0/99";
            String user3 = "AL";
            String user3Chats = "1/0/345\n1/9/555\n3_1\n0/0/123\n0/0/99";
            String user4 = "Юрий Сидоров";
            String user4Chats = "1/0/990\n0/0/99\n1/0/123";
            String user5 = "Аня Иванова";
            String user5Chats = "1/0/00220\n1/0/55383\n0/0/123\n0/0/123\n1/0/345";
            String user6 = "Анна Сергеева";
            String user6Chats = "1/0/3737\n1/0/3939\n123_123\n0/9/345\n0/0/9077\n0/0/345";
            cluster.backend().add(
                MoxyTestBase.userDoc(1, user1, "anyasid", null, null, null, user1Chats),
                MoxyTestBase.userDoc(2, user2, "vasya", null, null, null, user2Chats),
                MoxyTestBase.userDoc(3, user3, "anna-l", null, null, null, user3Chats),
                MoxyTestBase.userDoc(4, user4, "yorik", null, null, null, user4Chats),
                MoxyTestBase.userDoc(5, user5, "kitty99", null, null, null, user5Chats),
                MoxyTestBase.userDoc(6, user6, "anna", null, null, null, user6Chats)
            );
            cluster.backend().flush();

            String uri =
                "/api/search/messenger/chat-members?"
                    + "&chat-id-filter=1%2F0%2F345"
                    + "&length=10"
                    + "&timeout=10s"
                    + "&user_get=user_display_name"
                    + "&request=";
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "Анна")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                            + usersSuggest("\"user_nickname\":[\"anna\"]", user3, "3") + ','
                            + usersSuggest("\"user_display_name\":[\"Анна\"]", user1, "1") + ','
                            + usersSuggest("", user5, "5")
                            + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testContacts() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String userId = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";
            String chatId = "0/0/b5976bcc-03b8-4c20-9ed2-223c485e7d80";
            String contact1 = "be53278b-9813-4716-b56c-4cc508fa8269";
            String contact2 = "9a811962-9e57-85a0-ffb8-50477eaef56d";
            String contact3 = "5e4f355e-f235-4710-92ec-f17943ed8ba2";
            String contact4 = "dlqqfgs332-f235-4710-92ec-dj7s3hw8w8";
            String notContact = "74hf4f55-f235-4710-92ec-464732ha8i76";

            String contact1Name = "Анна Смирнова";
            cluster.addUser(contact1, "ks1234");
            cluster.addContact(userId, contact1, contact1Name, 1L);

            String contact2Name = "Алиса Смирнова";
            cluster.addUser(contact2, "alice");
            cluster.addContact(userId, contact2, contact2Name, 2L, 1L);

            String contact3Name = "Анна Иванова";
            cluster.addUser(contact3, "an0345");
            cluster.addContact(userId, contact3, contact3Name, 3L, 2L);

            String contact4Name = "Александр";
            cluster.addUser(contact4, "anna-alex");
            cluster.addContact(userId, contact4, contact4Name, 4L, 3L);

            cluster.addUser(notContact, "anna-k");

            cluster.updateChat(chatId);
            cluster.chatMembers(
                chatId,
                Arrays.asList(contact1, contact2, contact4, notContact),
                Collections.emptyList(),
                1,
                0);

            cluster.messagesBackend().flush();
            cluster.chatsBackend().flush();

            String uri =
                cluster.moxy().host()
                    + "/api/search/messenger/chat-members?"
                    + "&chat-id-filter=" + chatId
                    + "&length=10"
                    + "&timeout=10s"
                    + "&user_get=user_display_name"
                    + "&request-user-id=" + userId
                    + "&request=";
            HttpAssert.assertJsonResponse(
                client,
                uri + "анна",
                SUGGEST_PREFIX
                    + contactsSuggest("\"contact_name\":[\"Анна\"]", "ks1234", contact1Name, contact1) + ','
                    + usersSuggest("\"user_display_name\":[\"anna\"]", "anna-alex", contact4) + ','
                    + usersSuggest("\"user_display_name\":[\"anna\"]", "anna-k", notContact)
                    + ']' + '}');
        }
    }
    // CSON: MagicNumber
    // CSON: MultipleStringLiterals
    // CSON: MethodLength
    // CSON: ParameterNumber
}


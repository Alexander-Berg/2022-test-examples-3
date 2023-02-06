package ru.yandex.search.messenger.proxy;

import java.util.Arrays;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.JsonBoolean;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonNull;
import ru.yandex.json.dom.JsonString;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.Prefix;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SuggestTest extends MoxyTestBase {
    // CSOFF: MultipleStringLiterals
    private static final String PRODUCER_URI =
        "/_status?service=messenger_users&prefix=0&all&json-type=dollar";
    private static final String CHATS_PRODUCER_URI =
        "/_status?service=messenger_chats&prefix=0&all&json-type=dollar";
    private static final String PRODUCER_RESPONSE = "[{$localhost\0:100500}]";
    private static final long BACKEND_POS = 100500L;
    private static final User USER =
        new User("messenger_users", new LongPrefix(0L));
    private static final User CHATS_USER =
        new User("messenger_chats", new LongPrefix(0L));
    private static final User MESSAGES_USER =
        new User("messenger_messages", new StringPrefix("0"));
    private static final String SUGGEST_PREFIX =
        "{\"retry-suggest-types\":[],\"suggest\":[";
    private static final String GROUPED_SUGGEST_PREFIX =
        "{\"retry-suggest-types\":[],\"suggest\":{";
    private static final String ID = ",\"id\":\"";

    // CSOFF: ParameterNumber
    private static String usersSuggest(
        final String matches,
        final String displayName,
        final String nickname,
        final String position,
        final String website,
        final int id)
    {
        StringBuilder sb =
            new StringBuilder("{\"type\":\"users\"");
        sb.append(ID);
        sb.append(id);
        if (displayName != null) {
            sb.append("\",\"user_display_name\":\"");
            sb.append(displayName);
        }
        if (nickname != null) {
            sb.append("\",\"user_nickname\":\"");
            sb.append(nickname);
        }
        if (position != null) {
            sb.append("\",\"user_position\":\"");
            sb.append(position);
        }
        if (website != null) {
            sb.append("\",\"user_website\":\"");
            sb.append(website);
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

    private static String chatsSuggest(
        final String matches,
        final String name,
        final String description,
        final int id)
    {
        return chatsSuggest(
            matches,
            name,
            description,
            Integer.toString(id));
    }

    private static String chatsSuggest(
        final String matches,
        final String name,
        final String description,
        final String id)
    {
        StringBuilder sb =
            new StringBuilder("{\"type\":\"chats\"");
        sb.append(ID);
        sb.append(id);
        if (name != null) {
            sb.append("\",\"chat_name\":\"");
            sb.append(name);
        }
        if (description != null) {
            sb.append("\",\"chat_description\":\"");
            sb.append(description);
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

    private static String channelsSuggest(
        final String matches,
        final String id)
    {
        StringBuilder sb =
            new StringBuilder("{\"type\":\"channels\"");
        sb.append(ID);
        sb.append(id);
        sb.append('"');
        if (matches != null) {
            sb.append(",\"matches\":{");
            sb.append(matches);
            sb.append('}');
        }
        sb.append('}');
        return new String(sb);
    }
    // CSON: ParameterNumber

    // CSOFF: MagicNumber
    // CSOFF: MethodLength
    @Test
    public void testUsers() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);

            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);

            String ivanPetrov = "Ivan Petrov";
            String petrIvanov = "Petr Ivanov";
            String petya = "petya";
            String ivanRus = "Иван";
            String vasyaShmasya = "Вася Шмася";
            String vasiliy = "vasiliy";
            String annaSidorovna = "Анна Сидоровна";
            String anyasid = "anyasid";
            String buh = "Бухгалтер по работе с нервами";
            String company = "Компания 2319";
            String website = "http://anna.sidor.ru";
            String minuses = "pet-vas-m";
            String rusB = "%D0%B1";
            cluster.backend().add(
                MoxyTestBase.userDoc(0, ivanPetrov),
                MoxyTestBase.userDoc(1, petrIvanov, petya, ivanRus),
                MoxyTestBase.userDoc(2, vasyaShmasya, vasiliy, "Директор"),
                MoxyTestBase.userDoc(
                    3,
                    annaSidorovna,
                    anyasid,
                    buh,
                    company,
                    website,
                    "0/0/123\n1/0/345\n0/0/99"),
                MoxyTestBase.userDoc(4, null, minuses),
                MoxyTestBase.userDoc(5, rusB));
            cluster.backend().flush();
            String uri =
                "/api/search/messenger/suggest?salt=1&length=6"
                + "&suggest-types=users&timeout=10s&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "ivan")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + usersSuggest(
                            "\"user_display_name\":[\"Ivan\"]",
                            null,
                            null,
                            null,
                            null,
                            0)
                        + ','
                        + usersSuggest(
                            "\"user_position\":[\"Иван\"]",
                            null,
                            null,
                            null,
                            null,
                            1)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            uri = "/api/search/messenger/suggest?uid=0&length=6&"
                + "suggest-types=users&timeout=10s&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "pet")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                            + usersSuggest(
                            "\"user_display_name\":[\"Pet\"],"
                                + "\"user_nickname\":[\"pet\"]",
                            null,
                            null,
                            null,
                            null,
                            1)
                            + ','
                            + usersSuggest(
                            "\"user_nickname\":[\"pet\"]",
                            null,
                            null,
                            null,
                            null,
                            4)
                            + ','
                            + usersSuggest(
                            "\"user_display_name\":[\"Pet\"]",
                            null,
                            null,
                            null,
                            null,
                            0)
                            + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }

            uri = "/api/search/messenger/suggest?uid=0&length=6&timeout=10s"
                + "&suggest-types=users&user_get=user_display_name&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "ann")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + usersSuggest(
                            "\"user_display_name\":[\"Анн\"]",
                            annaSidorovna,
                            null,
                            null,
                            null,
                            3)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "pet-")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + usersSuggest(
                            "\"user_nickname\":[\"pet-\"]",
                            null,
                            null,
                            null,
                            null,
                            4)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "pet-v")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + usersSuggest(
                            "\"user_nickname\":[\"pet-v\"]",
                            null,
                            null,
                            null,
                            null,
                            4)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + rusB)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
/*
                Should not find this
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + usersSuggest(
                            "\"user_display_name\":[\"B\"]",
                            rusB,
                            null,
                            null,
                            null,
                            5)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
*/
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
            //@mention
            String mentionUri =
                "/api/search/messenger/suggest?uid=0&chat-id-filter=0%2F0%2F123"
                + "&length=6&timeout=10s&suggest-types=users"
                + "&user_get=user_display_name&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + mentionUri + "@any")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + usersSuggest(
                            "\"user_nickname\":[\"any\"]",
                            annaSidorovna,
                            null,
                            null,
                            null,
                            3)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    /**
     * We disabled suggest by chat description, so ignoring test for a while
     * @throws Exception
     */
    @Ignore
    @Test
    public void testSuggestChatsByDescriptions() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(CHATS_USER, BACKEND_POS);
            cluster.producer().add(CHATS_PRODUCER_URI, PRODUCER_RESPONSE);

            String chatName = "chat-name";
            String superName = "super-name";
            String chatDescription = "chat description";
            cluster.backend().add(
                MoxyTestBase.chatDoc(0, chatName, null),
                MoxyTestBase.chatDoc(1, superName, chatDescription));
            cluster.backend().flush();
            String uri =
                "/api/search/messenger/suggest?uid=0&length=6"
                + "&request-user-id=123&timeout=10s"
                + "&suggest-types=chats&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "cha")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + chatsSuggest(
                            "\"chat_name\":[\"cha\"]",
                            null,
                            null,
                            0)
                        + ','
                        + chatsSuggest(
                            "\"chat_description\":[\"cha\"]",
                            null,
                            null,
                            1)
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "favor")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                        + chatsSuggest(
                            "\"chat_name\":[\"favor\"]",
                            null,
                            null,
                            "123_123")
                        + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testChannels() throws Exception {
        try (ProxyCluster cluster =
            new ProxyCluster(this, ProxyCluster.CHATS_LUCENE_CONF, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(CHATS_USER, BACKEND_POS);
            cluster.producer().add(CHATS_PRODUCER_URI, PRODUCER_RESPONSE);

            String channel1Id = "1/0/7d23d2b3-2779-49c5-a481-4ac18f80ba39";
            String channel1 = "\"id\":\"" + channel1Id
                + "\",\"chat_id\":\"" + channel1Id
                + "\",\"chat_name\":\"Английский в картинках (English)"
                + "\",\"chat_description\":\"Учимся английскому!"
                + "\",\"chat_public\":\"true"
                + "\",\"chat_channel\":\"true"
                + "\",\"chat_show_on_morda\":\"true"
                + "\",\"chat_member_count\":\"702"
                + "\",\"#score\":\"4.064614"
                + '"';
            cluster.backend().add(channel1);

            String channel2Id = "1/0/5996dcc3-b637-47be-9c93-e2d5c32636e7";
            String channel2 = "\"id\":\"" + channel2Id
                + "\",\"chat_id\":\"" + channel2Id
                + "\",\"chat_name\":\"Английский легко и просто!"
                + "\",\"chat_description\":\"Канал с курсом самостоятельного "
                    + "изучения английского языка"
                + "\",\"chat_public\":\"true"
                + "\",\"chat_channel\":\"true"
                + "\",\"chat_show_on_morda\":\"true"
                + "\",\"chat_member_count\":\"386"
                + "\",\"#score\":\"5.064614"
                + '"';
            cluster.backend().add(channel2);

            String channel3Id = "1/0/ac8fca05-eb8b-4aa2-86dd-a9807665ee59";
            String channel3 = "\"id\":\"" + channel3Id
                + "\",\"chat_id\":\"" + channel3Id
                + "\",\"chat_name\":\"Новости дня"
                + "\",\"chat_description\":\"Канал про новости"
                + "\",\"chat_public\":\"true"
                + "\",\"chat_channel\":\"true"
                + "\",\"chat_show_on_morda\":\"true"
                + "\",\"chat_member_count\":\"193844"
                + "\",\"#score\":\"10.064614"
                + '"';
            cluster.backend().add(channel3);

            String channel4Id = "1/0/ea7ca3db-bc2c-495a-a35a-7f3a827228c2";
            String channel4 = "\"id\":\"" + channel4Id
                + "\",\"chat_id\":\"" + channel4Id
                + "\",\"chat_name\":\"Английский | Учим языки"
                + "\",\"chat_description\":\"Лингвистика. Изучаем языки Европы"
                + "\",\"chat_public\":\"true"
                + "\",\"chat_channel\":\"true"
                + "\",\"chat_show_on_morda\":\"true"
                + "\",\"chat_member_count\":\"1029"
                + "\",\"#score\":\"3.064614"
                + '"';
            cluster.backend().add(channel4);

            cluster.backend().flush();

            String uri =
                "/api/search/messenger/suggest?uid=0&length=6"
                    + "&request-user-id=123&timeout=10s"
                    + "&suggest-types=channels&request=";
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "англ")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                            + channelsSuggest(
                            "\"chat_name\":[\"Англ\"]",
                            channel4Id)
                            + ','
                            + channelsSuggest(
                            "\"chat_name\":[\"Англ\"],\"chat_description\":[\"англ\"]",
                            channel1Id)
                            + ','
                            + channelsSuggest(
                            "\"chat_name\":[\"Англ\"],\"chat_description\":[\"англ\"]",
                            channel2Id)
                            + ']' + '}'),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    /**
     * We disabled suggest by chat description, so ignoring test for a while
     * @throws Exception
     */
    @Ignore
    @Test
    public void testGroups() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(CHATS_USER, BACKEND_POS);
            cluster.producer().add(CHATS_PRODUCER_URI, PRODUCER_RESPONSE);
            cluster.backend().setQueueId(USER, BACKEND_POS);
            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);
            Prefix prefix = MESSAGES_USER.prefix();
            cluster.backend().setQueueId(
                MESSAGES_USER,
                BACKEND_POS);
            String producerUri =
                "/_status?service=messenger_messages&prefix=" + prefix.hash()
                    + "&all&json-type=dollar";
            cluster.producer().add(
                producerUri,
                PRODUCER_RESPONSE);

            String chatName = "chat-name1";
            String superName = "super-name1";
            String chatDescription = "chat description1";
            cluster.backend().add(
                MoxyTestBase.chatDoc(0, chatName, null),
                MoxyTestBase.chatDoc(1, superName, chatDescription));

            String vasyaShmasya = "Вася Шмася1 cha";
            String vasiliy = "vasiliy1";
            cluster.backend().add(
                MoxyTestBase.userDoc(2, vasyaShmasya, vasiliy, "Директор1"));
            cluster.backend().flush();

            String uri =
                "/api/search/messenger/suggest?uid=0&length=6"
                + "&timeout=10s"
                + "&user_get=user_display_name"
                + "&suggest-types=chats&suggest-types=users&request=";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri + "cha")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseString =
                    CharsetUtils.toString(response.getEntity());
                System.err.println("Response: " + responseString);
                YandexAssert.check(
                    new JsonChecker(
                        GROUPED_SUGGEST_PREFIX
                        + "\"0\":["
                        + chatsSuggest(
                            "\"chat_name\":[\"cha\"]",
                            null,
                            null,
                            0)
                        + ','
                        + chatsSuggest(
                            "\"chat_description\":[\"cha\"]",
                            null,
                            null,
                            1)
                        + "],\"1\":["
                        + usersSuggest(
                            "\"user_display_name\":[\"cha\"]",
                            vasyaShmasya,
                            null,
                            null,
                            null,
                            2)
                        + "]}}"),
                    responseString);
            }
        }
    }

    @Test
    public void testMessageSearch() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            Prefix prefix = MESSAGES_USER.prefix();
            cluster.messagesBackend().setQueueId(
                MESSAGES_USER,
                BACKEND_POS);

            cluster.messagesBackend().add(
                prefix,
                messageDoc(
                    "1",
                    "0",
                    "https://st.yandex-team.ru/MSSNGRBACKEND-891"),
                messageDoc("2", "0", "https://nda.ya.ru/3UWSiM"));

            String baseUri =
                cluster.moxy().host()
                    + "/api/search/messenger/suggest?suggest-types=messages"
                    + "&uid=0&length=10&message_get=id&timeout=10s&user-id=0"
                    + "&organizations=34&chat-id-filter=0";
            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&request=https://st.yandex-team.ru/MSSNGRBACKEND-891",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\n"
                    + "\"https://st.yandex-team.ru/MSSNGRBACKEND-891\"]},"
                    + "\"id\":\"1\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&request=MSSNGRBACKEND-891",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"MSSNGRBACKEND-891\"]},"
                    + "\"id\":\"1\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&request=MSSNGRBACKEND",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_text\": [\"MSSNGRBACKEND\"]},"
                    + "\"id\":\"1\"}]}");
        }
    }

    @Test
    public void testPollMessageSearch() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            Prefix prefix = MESSAGES_USER.prefix();
            cluster.messagesBackend().setQueueId(
                MESSAGES_USER,
                BACKEND_POS);
            String id = "1";
            String chatId = "0";

            String msg = "\"id\": \"" + id + "\",\"message_id\": \"" + id
                + "\",\"message_chat_id_hash\": \"1275\",\n"
                + "\"type\": \"text_message\",\n"
                + "\"message_data\": \"\","
                + "\"message_chat_id\": \"" + chatId
                + "\",\"message_timestamp\": \"1564656474387013\",\n"
                + "\"message_last_edit_timestamp\": \"0\",\n"
                + "\"message_seq_no\": \"13\",\n"
                + "\"message_text\": null,\n"
                + "\"message_poll_answers\": \"Juice\nMilk\nWater\nTea\nCoffee\",\n"
                + "\"message_poll_title\": \"Your favorite drink:\",\n"
                + "\"message_from_display_name\": \"Владислав Таболин\",\n"
                + "\"message_from_guid\": "
                + "\"7a87a9cb-17ce-4138-8928-de46c68ff919\",\n"
                + "\"message_from_phone_id\": \"\"";

            cluster.messagesBackend().add(
                prefix,
                msg,
                messageDoc(
                    "1",
                    "0",
                    "buy some juice"));

            String baseUri =
                cluster.moxy().host()
                    + "/api/search/messenger/suggest?suggest-types=messages"
                    + "&uid=0&length=10&message_get=id,message_poll_title,message_poll_answers,message_text"
                    + "&timeout=10s&user-id=0"
                    + "&organizations=34&chat-id-filter=0";
            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&request=milk",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_poll_answers\": [\"Milk\"]},"
                    + "\"id\":\"1\","
                    + "\"message_poll_answers\":[\"Juice\",\"Milk\",\"Water\",\"Tea\",\"Coffee\"],"
                    + "\"message_poll_title\":\"Your favorite drink:\""
                    + "}]}");

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&request=drink",
                "{\"retry-suggest-types\":[],\"suggest\":"
                    + "[{\"type\":\"messages\",\"matches\":{"
                    + "\"message_poll_title\": [\"drink\"]},"
                    + "\"id\":\"1\","
                    + "\"message_poll_answers\":[\"Juice\",\"Milk\",\"Water\",\"Tea\",\"Coffee\"],"
                    + "\"message_poll_title\":\"Your favorite drink:\""
                    + "}]}");
        }
    }

    @Test
    public void testUnavailableMispell() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this)) {
            cluster.start();
            prepareIndex(cluster);
            cluster.erratum().add("*", HttpStatus.SC_NOT_IMPLEMENTED);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                cluster.proxy().port(),
                URI + "text=some+request+here");
        }
    }

    @Test
    public void testMisspell() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(USER, BACKEND_POS);

            cluster.producer().add(PRODUCER_URI, PRODUCER_RESPONSE);

            cluster.erratum().add(
                "/misspell.json/check?srv=disk-search&options=321&text=ivn",
                "{\"code\":201,\"lang\":\"ru,en\",\"rule\":\"Misspell\","
                    + "\"flags\":0,\"r\":8000,\"srcText\":\"ivn\","
                    + "\"text\":\"ivan\"}");

            String ivanPetrov = "Ivan Petrov";
            cluster.backend().add(MoxyTestBase.userDoc(0, ivanPetrov));
            cluster.backend().flush();
            String uri =
                "/api/search/messenger/suggest?salt=1&length=6"
                    + "&suggest-types=users&timeout=10s&request=ivn";
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        SUGGEST_PREFIX
                            + usersSuggest(
                            "\"user_display_name\":[\"Ivan\"]", null, null, null, null, 0)
                            + "]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEncodingRequest() throws Exception {
        try (ProxyCluster cluster =
            new ProxyCluster(this, ProxyCluster.CHATS_LUCENE_CONF, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().setQueueId(CHATS_USER, BACKEND_POS);
            cluster.producer().add(CHATS_PRODUCER_URI, PRODUCER_RESPONSE);

            String uri =
                "/api/search/messenger/suggest?uid=0&length=6"
                    + "&request-user-id=123&timeout=10s"
                    + "&suggest-types=chats&request=";

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "запрос%F0%9F%8C%88")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "%F0%9F%8C%88запрос")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "%F0%9F%8C%88+%F0%9F%92%97")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "%F0%9F%8C%88%F0%9F%92%97")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            String request = "%D0%BC%D0%BE%D1%8F+Crazy+Alert+ZonaExtrimLab"
                + "EctactSryyLaboratoryDezaAutoAct%3F!!!$%26%2B-_100%25"
                + "/balanceAllSistemHigh%C2%A9%F0%9F%8E%A1AsimmilirovanXOn"
                + "/OffCrazyActiveAll%F0%9F%8C%88deaktiv/on%F0%9F%8C%9FCrazy"
                + "Girls%F0%9F%92%97DearKarina";

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + request)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }
    // CSON: MagicNumber
    // CSON: MultipleStringLiterals
    // CSON: MethodLength

    @Test
    public void testSuggestWithNamespaces() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String guid1 = "guid1";
            cluster.addUser(guid1, "lenina1");
            String groupChat1 = "0/1/chat1";
            String groupChat2 = "0/2/chat2";
            String groupChat3 = "0/0/chat3";

            JsonMap chatData1 = cluster.groupChatData(groupChat1, 1);
            chatData1.put("namespace", new JsonLong(1));
            chatData1.put("name", new JsonString("group first"));
            cluster.updateChat(groupChat1, chatData1);

            JsonMap chatData2 = cluster.groupChatData(groupChat2, 1);
            chatData2.put("namespace", new JsonLong(2));
            chatData2.put("name", new JsonString("group second"));
            cluster.updateChat(groupChat2, chatData2);

            JsonMap chatData3 = cluster.groupChatData(groupChat3, 1);
            chatData3.put("namespace", JsonNull.INSTANCE);
            chatData3.put("name", new JsonString("group third"));
            cluster.updateChat(groupChat3, chatData3);

            cluster.chatMembers(groupChat1, Collections.singleton(guid1), Collections.emptyList(), 1, 0);
            cluster.chatMembers(groupChat2, Collections.singleton(guid1), Collections.emptyList(), 1, 0);
            cluster.chatMembers(groupChat3, Collections.singleton(guid1), Collections.emptyList(), 1, 0);
            String baseUri =
                cluster.moxy().host()
                    + "/api/search/messenger/suggest?suggest-types=users_pvp%2Cchats%2Cchannels"
                    + "&uid=0&length=10&timeout=10s&user-id="
                    + guid1 + "&pvp-user-id=" + guid1 + "&request-user-id="
                    + guid1 + "&user_get=user_id&chat_get=chat_id";

            cluster.erratum().add("*", HttpStatus.SC_OK);

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&request=group&namespaces=1,2",
                "{\"retry-suggest-types" +
                "\":[],\"suggest\":[{\"type\":\"chats\",\"matches\":{\"chat_name\":[\"group\"]},\"id\":\"0/1/chat1\"," +
                "\"chat_id\":\"0/1/chat1\"},{\"type\":\"chats\",\"matches\":{\"chat_name\":[\"group\"]}," +
                "\"id\":\"0/2/chat2\",\"chat_id\":\"0/2/chat2\"}]}");
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&request=group&namespaces=0",
                "{\"retry-suggest-types" +
                    "\":[],\"suggest\":[{\"type\":\"chats\",\"matches\":{\"chat_name\":[\"group\"]},\"id\":\"0/0/chat3\"," +
                    "\"chat_id\":\"0/0/chat3\"}]}");

            String guid2 = "guid2";
            cluster.addUser(guid2, "lenina2");

            JsonMap pvpData = cluster.pvpChatData(guid1, guid2, 1);
            pvpData.put("namespace", new JsonLong(1));
            String pvp1 = pvpData.getString("chat_id");
            cluster.updateChat(pvp1, pvpData);
            cluster.chatMembers(pvp1, Arrays.asList(guid1, guid2), Collections.emptyList(), 1, 0);


            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&request=len",
                "{\"retry-suggest-types\":[],\"suggest\":[" +
                    "{\"type\":\"users_pvp\",\"matches\":{},\"id\":\"guid2\"," +
                    "\"user_id\":\"guid2\"}]}");
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&request=len&namespace=1",
                "{\"retry-suggest-types\":[],\"suggest\":[" +
                    "{\"type\":\"users_pvp\",\"matches\":{},\"id\":\"guid2\"," +
                    "\"user_id\":\"guid2\"}]}");
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&request=len&namespaces=2",
                "{\"retry-suggest-types\":[],\"suggest\":[]}");

            String channel1 = "1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6";
            JsonMap channel1Data = cluster.groupChatData(channel1, 1);
            channel1Data.put("namespace", new JsonLong(1));
            channel1Data.put("name", new JsonString("Пироги Обжоры: вы едите - мы печем"));
            channel1Data.put("show_on_morda", JsonBoolean.TRUE);
            channel1Data.put("channel", JsonBoolean.TRUE);
            cluster.updateChat(channel1, channel1Data);
            cluster.chatMembers(channel1, Arrays.asList(guid1, guid2), Collections.emptyList(), 1, 0);
            cluster.addTextMessage(channel1, 1L, guid1, "Покупайте наши пироги");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&request=pirog&namespaces=1,2,3",
                "{\"retry-suggest-types\":[],\"suggest\":[" +
                    "{\"chat_id\": \"1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6\",\n" +
                    "\"id\": \"1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6\",\n" +
                    "\"matches\": {\"chat_name\": [\"Пирог\"]},\"type\": \"chats\"}]}");

            String uri =
                cluster.moxy().host()
                    + "/api/search/messenger/suggest?suggest-types=messages"
                    + "&uid=0&length=10&timeout=10s&user-id="
                    + guid1 + "&pvp-user-id=" + guid1 + "&request-user-id="
                    + guid1 + "&user_get=user_id&chat_get=chat_id";
            HttpAssert.assertJsonResponse(
                client,
                uri + "&request=pirog&namespaces=1,2,3",
                "{\"retry-suggest-types\":[],\"suggest\":[{" +
                    "\"id\": \"1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6/1\",\n" +
                    "\"matches\": {\"message_text\": [\"пирог\"]}," +
                    "\"type\": \"messages\"}]}");
        }
    }
}


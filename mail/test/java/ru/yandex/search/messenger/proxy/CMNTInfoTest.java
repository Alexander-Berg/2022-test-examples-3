package ru.yandex.search.messenger.proxy;

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

public class CMNTInfoTest extends MoxyTestBase {
    // CSOFF: MultipleStringLiterals
    private static final String CHATS_PRODUCER_URI =
        "/_status?service=messenger_chats&prefix=0&all&json-type=dollar";
    private static final String PRODUCER_RESPONSE = "[{$localhost\0:100500}]";
    private static final long BACKEND_POS = 100500L;
    private static final User CHATS_USER =
        new User("messenger_chats", new LongPrefix(0L));

    @Test
    public void testChats() throws Exception {
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
                MoxyTestBase.chatBuilder()
                    .chatId("0")
                    .chatName(chatName)
                    .chatEntityId("e1")
                    .chatServiceId(7)
                    .chatMessageCount(0)
                    .chatTotalMessageCount(1)
                    .chatHiddenMessageCount(2)
                    .chatTotalIndexedMessageCount(3)
                    .build(),
                MoxyTestBase.chatBuilder()
                    .chatId("1")
                    .chatName(superName)
                    .chatDescription(chatDescription)
                    .chatEntityId("e2")
                    .chatServiceId(7)
                    .chatMessageCount(10)
                    .chatTotalMessageCount(11)
                    .chatHiddenMessageCount(12)
                    .chatTotalIndexedMessageCount(13)
                    .build(),
                MoxyTestBase.chatBuilder()
                    .chatId("2")
                    .chatName(superName)
                    .chatDescription(chatDescription)
                    .chatEntityId("e2")
                    .chatServiceId(15)
                    .chatMessageCount(20)
                    .chatTotalMessageCount(21)
                    .chatHiddenMessageCount(22)
                    .chatTotalIndexedMessageCount(23)
                    .chatParentUrl("yandex.taxi")
                    .build(),
                MoxyTestBase.chatBuilder()
                    .chatId("0/14/1")
                    .chatName(superName)
                    .chatDescription(chatDescription)
                    .chatEntityId("qwe")
                    .chatServiceId(15)
                    .chatMessageCount(20)
                    .chatTotalMessageCount(21)
                    .chatHiddenMessageCount(22)
                    .chatTotalIndexedMessageCount(23)
                    .chatParentUrl("yandex.shmandeks")
                    .build());
            String uri =
                "/api/search/messenger/cmnt-info?entity-id=e1&service-id=7";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{"
                            + "\"e1\":{"
                                + "\"chat_entity_id\":\"e1\""
                                + ",\"chat_id\":\"0\""
                                + ",\"chat_name\":\"chat-name\""
                                + ",\"chat_namespace\":null"
                                + ",\"chat_parent_url\":null"
                                + ",\"chat_subservice\":\"7\""
                                + ",\"chat_message_count\":\"0\""
                                + ",\"chat_total_message_count\":\"1\""
                                + ",\"chat_hidden_message_count\":\"2\""
                                + ",\"chat_total_indexed_message_count\":\"3\""
                            + "}"
                        + "}"
                    ),
                    CharsetUtils.toString(response.getEntity()));
            }
            uri =
                "/api/search/messenger/cmnt-info?entity-id=e1,e2&service-id=7";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{"
                            + "\"e1\":{"
                                + "\"chat_entity_id\":\"e1\""
                                + ",\"chat_id\":\"0\""
                                + ",\"chat_name\":\"chat-name\""
                                + ",\"chat_namespace\":null"
                                + ",\"chat_parent_url\":null"
                                + ",\"chat_subservice\":\"7\""
                                + ",\"chat_message_count\":\"0\""
                                + ",\"chat_total_message_count\":\"1\""
                                + ",\"chat_hidden_message_count\":\"2\""
                                + ",\"chat_total_indexed_message_count\":\"3\""
                            + "}"
                            + ",\"e2\":{"
                                + "\"chat_entity_id\":\"e2\""
                                + ",\"chat_id\":\"1\""
                                + ",\"chat_name\":\"super-name\""
                                + ",\"chat_namespace\":null"
                                + ",\"chat_parent_url\":null"
                                + ",\"chat_subservice\":\"7\""
                                + ",\"chat_message_count\":\"10\""
                                + ",\"chat_total_message_count\":\"11\""
                                + ",\"chat_hidden_message_count\":\"12\""
                                + ",\"chat_total_indexed_message_count\":\"13\""
                            + "}"
                        + "}"
                    ),
                    CharsetUtils.toString(response.getEntity()));
            }
            uri =
                "/api/search/messenger/cmnt-info?entity-id=e1,e2&service-id=15";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{"
                            + "\"e2\":{"
                                + "\"chat_entity_id\":\"e2\""
                                + ",\"chat_id\":\"2\""
                                + ",\"chat_name\":\"super-name\""
                                + ",\"chat_namespace\":null"
                                + ",\"chat_parent_url\":\"yandex.taxi\""
                                + ",\"chat_subservice\":\"15\""
                                + ",\"chat_message_count\":\"20\""
                                + ",\"chat_total_message_count\":\"21\""
                                + ",\"chat_hidden_message_count\":\"22\""
                                + ",\"chat_total_indexed_message_count\":\"23\""
                            + "}"
                        + "}"
                    ),
                    CharsetUtils.toString(response.getEntity()));
            }
            //search by chatId
            uri =
                "/api/search/messenger/cmnt-info?chat-id=0,1";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_BAD_REQUEST, response);
            }
            uri =
                "/api/search/messenger/cmnt-info?chat-id=0/14/1";
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(cluster.proxy().host() + uri)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{"
                            + "\"0/14/1\":{"
                                + "\"chat_entity_id\":\"qwe\""
                                + ",\"chat_id\":\"0/14/1\""
                                + ",\"chat_name\":\"super-name\""
                                + ",\"chat_namespace\":null"
                                + ",\"chat_parent_url\":\"yandex.shmandeks\""
                                + ",\"chat_subservice\":\"15\""
                                + ",\"chat_message_count\":\"20\""
                                + ",\"chat_total_message_count\":\"21\""
                                + ",\"chat_hidden_message_count\":\"22\""
                                + ",\"chat_total_indexed_message_count\":\"23\""
                            + "}"
                        + "}"
                    ),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}


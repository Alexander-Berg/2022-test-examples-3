package ru.yandex.search.messenger.proxy;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.util.TestBase;

public class ChatMediaTest extends TestBase {
    @Test
    public void testImportantMessage() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String chatId = "0/0/83ue3p11-f734-4083-bdfd-bb9ssd0kw3";

            String messageId1 = chatId + "/100";
            String timestamp1 = "1599064592266011";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId1 + "\",\n"
                + "\"type\":\"delete_message\",\n"
                + "\"message_id\":\"" + messageId1 + "\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"0\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp1 + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"Важное удаленное сообщение\",\n"
                + "\"message_from_display_name\":\"Olga\",\n"
                + "\"message_from_guid\":\"9272jd7-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"02is9112-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\",\n"
                + "\"message_important\":\"1\"\n");

            String messageId2 = chatId + "/101";
            String timestamp2 = "1599064592266013";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId2 + "\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId2 + "\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"0\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp2 + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"Просто важное сообщение\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\",\n"
                + "\"message_important\":\"1\"\n");

            String messageId3 = chatId + "/102";
            String timestamp3 = "1599064592266017";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId3 + "\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId3 + "\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"0\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp3 + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"Просто сообщение\",\n"
                + "\"message_from_display_name\":\"Alex\",\n"
                + "\"message_from_guid\":\"9u8f74-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"h8hy7ce4-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\"");

            cluster.messagesBackend().flush();
            String uri = cluster.moxy().host() +
                "/api/search/messenger/chat-media?chat-id=" + chatId
                + "&near=1599064592266020&next=0&prev=10"
                + "&types=voice_message%2Cimage_message%2Cimportant_message";

            String result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 0,"
                    + "\"prev\": 1"
                + "},"
                + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp2
                + "}]}";
            HttpAssert.assertJsonResponse(client, uri, result);
        }
    }

    @Test
    public void testLinkMessage() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            String chatId = "0/0/83ue3p11-f734-4083-bdfd-bb9ssd0kw3";

            String messageId = chatId + "/101";
            String timestamp = "1599064592266013";
            String rcaData = "{\\\"favicon\\\":\\\"https://yastat.net/favicon/v2/yandex\\\"}";
            String mapRcaData = "\"message_rca_data\":\"" + rcaData + "\"";
            String listRcaData = "\"message_rca_data\":\"[" + rcaData + "]\"";

            String doc = "\"id\":\"message_" + messageId + "\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId + "\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"0\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"Просто важное сообщение http://ya.ru\",\n"
                + "\"message_links\":\"Просто важное сообщение http://ya.ru\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\",\n"
                + "\"message_important\":\"1\",\n";

            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                doc + mapRcaData);

            cluster.messagesBackend().flush();
            String uri = cluster.moxy().host() +
                "/api/search/messenger/chat-media?chat-id=" + chatId
                + "&near=1599064592266020&next=0&prev=10"
                + "&types=link&get=message_rca_data";

            String result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 0,"
                    + "\"prev\": 1"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp + ","
                    + mapRcaData
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri, result);

            String result2 =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 0,"
                    + "\"prev\": 1"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp + ","
                    + listRcaData
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri + "&multiple_links", result2);

            cluster.messagesBackend().update(
                new StringPrefix(chatId),
                doc + listRcaData);
            cluster.messagesBackend().flush();

            HttpAssert.assertJsonResponse(client, uri, result);

            HttpAssert.assertJsonResponse(client, uri + "&multiple_links", result2);
        }
    }

    @Test
    public void testForwardedMessage() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String chatId = "0/0/83ue3p11-f734-4083-bdfd-bb9ssd0kw3";
            String timestamp = "1599064592268092";
            String messageId = chatId + "/" + timestamp;

            String doc = "\"id\":\"message_" + messageId + "\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId + "\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"0\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"google.com\",\n"
                + "\"message_links\":\"http://google.com\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\"";
            cluster.messagesBackend().add(new StringPrefix(chatId), doc);

            String forwarded = "\"id\":\"message_" + messageId + "/1\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId + "/1\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"1\",\n"
                + "\"message_forwarded\":\"true\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"yandex.ru\",\n"
                + "\"message_links\":\"http://yandex.ru\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\"";
            cluster.messagesBackend().add(new StringPrefix(chatId), forwarded);

            String forwarded2 = "\"id\":\"message_" + messageId + "/2\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId + "/2\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"2\",\n"
                + "\"message_forwarded\":\"true\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"ya.ru Еще ссылка: ya2.ru\",\n"
                + "\"message_links\":\"http://ya.ru\nhttp://ya2.ru\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\"";
            cluster.messagesBackend().add(new StringPrefix(chatId), forwarded2);

            String timestamp2 = "1599064592270142";
            String messageId2 = chatId + "/" + timestamp2;

            String doc2 = "\"id\":\"message_" + messageId2 + "\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId2 + "\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"0\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp2 + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"ozon.ru\",\n"
                + "\"message_links\":\"http://ozon.ru\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\"";
            cluster.messagesBackend().add(new StringPrefix(chatId), doc2);

            String replied = "\"id\":\"message_" + messageId2 + "/1\",\n"
                + "\"type\":\"text_message\",\n"
                + "\"message_id\":\"" + messageId2 + "/1\",\n"
                + "\"message_chat_id_hash\":\"-7850\",\n"
                + "\"message_hid\":\"1\",\n"
                + "\"message_forwarded\":\"false\",\n"
                + "\"message_chat_id\":\"" + chatId + "\",\n"
                + "\"message_timestamp\":\"" + timestamp2 + "\",\n"
                + "\"message_last_edit_timestamp\":\"0\",\n"
                + "\"message_seq_no\":\"2\",\n"
                + "\"message_text\":\"wildberries.ru\",\n"
                + "\"message_links\":\"http://wildberries.ru\",\n"
                + "\"message_from_display_name\":\"Maria\",\n"
                + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                + "\"message_moderation_action\":\"UNDEFINED\",\n"
                + "\"message_moderation_verdicts\":\"\",\n"
                + "\"message_multi_item\":\"false\",\n"
                + "\"message_data\":\"AA06\"";
            cluster.messagesBackend().add(new StringPrefix(chatId), replied);

            cluster.messagesBackend().flush();

            String uri = cluster.moxy().host() +
                "/api/search/messenger/chat-media?chat-id=" + chatId
                + "&near=1599064592266020&next=10&prev=10"
                + "&types=link&get=message_links";

            String result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 2,"
                    + "\"prev\": 0"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp + ","
                    + "\"message_links\": [\"http://google.com\",\"http://yandex.ru\",\"http://ya.ru\",\"http://ya2.ru\"]"
                    + "},{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp2 + ","
                    + "\"message_links\": [\"http://ozon.ru\"]"
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri, result);
        }
    }

    @Test
    public void testSearchByLinks() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            String chatId = "0/0/83ue3p11-f734-4083-bdfd-bb9ssd0kw3";

            String messageId1 = chatId + "/101";
            String timestamp1 = "1599064592266013";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId1 + "\",\n"
                    + "\"type\":\"text_message\",\n"
                    + "\"message_id\":\"" + messageId1 + "\",\n"
                    + "\"message_chat_id_hash\":\"-7850\",\n"
                    + "\"message_hid\":\"0\",\n"
                    + "\"message_forwarded\":\"false\",\n"
                    + "\"message_chat_id\":\"" + chatId + "\",\n"
                    + "\"message_timestamp\":\"" + timestamp1 + "\",\n"
                    + "\"message_last_edit_timestamp\":\"0\",\n"
                    + "\"message_seq_no\":\"2\",\n"
                    + "\"message_text\":\"Ссылка\",\n"
                    + "\"message_links\":\"yandex.ru\",\n"
                    + "\"message_from_display_name\":\"Maria\",\n"
                    + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                    + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                    + "\"message_moderation_action\":\"UNDEFINED\",\n"
                    + "\"message_moderation_verdicts\":\"\",\n"
                    + "\"message_multi_item\":\"false\",\n"
                    + "\"message_data\":\"AA06\",\n"
                    + "\"message_important\":\"1\"\n");

            String messageId2 = chatId + "/102";
            String timestamp2 = "1599064592266010";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId2 + "\",\n"
                    + "\"type\":\"text_message\",\n"
                    + "\"message_id\":\"" + messageId2 + "\",\n"
                    + "\"message_chat_id_hash\":\"-7850\",\n"
                    + "\"message_hid\":\"0\",\n"
                    + "\"message_forwarded\":\"false\",\n"
                    + "\"message_chat_id\":\"" + chatId + "\",\n"
                    + "\"message_timestamp\":\"" + timestamp2 + "\",\n"
                    + "\"message_last_edit_timestamp\":\"0\",\n"
                    + "\"message_seq_no\":\"2\",\n"
                    + "\"message_text\":\"Еще одна ссылка\",\n"
                    + "\"message_links\":\"google.com\",\n"
                    + "\"message_from_display_name\":\"Maria\",\n"
                    + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                    + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                    + "\"message_moderation_action\":\"UNDEFINED\",\n"
                    + "\"message_moderation_verdicts\":\"\",\n"
                    + "\"message_multi_item\":\"false\",\n"
                    + "\"message_data\":\"AA06\",\n"
                    + "\"message_important\":\"1\"\n");

            cluster.messagesBackend().flush();

            String uri = cluster.moxy().host() +
                "/api/search/messenger/chat-media?chat-id=" + chatId
                + "&near=1599064592266020&next=0&prev=10"
                + "&types=link&request=";

            String result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 0,"
                    + "\"prev\": 2"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp2
                    + "},{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp1
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri, result);

            result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 0,"
                    + "\"prev\": 1"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp1
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri + "yandex", result);
        }
    }

    @Test
    public void testSearchByFilename() throws Exception {
        try (MoxyCluster cluster = new MoxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            String chatId = "0/0/83ue3p11-f734-4083-bdfd-bb9ssd0kw3";

            String messageId1 = chatId + "/101";
            String timestamp1 = "1599064592266010";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId1 + "\",\n"
                    + "\"type\":\"file_message\",\n"
                    + "\"message_id\":\"" + messageId1 + "\",\n"
                    + "\"message_chat_id_hash\":\"-7850\",\n"
                    + "\"message_hid\":\"0\",\n"
                    + "\"message_forwarded\":\"false\",\n"
                    + "\"message_chat_id\":\"" + chatId + "\",\n"
                    + "\"message_timestamp\":\"" + timestamp1 + "\",\n"
                    + "\"message_last_edit_timestamp\":\"0\",\n"
                    + "\"message_seq_no\":\"2\",\n"
                    + "\"message_filename\":\"passport.pdf\",\n"
                    + "\"message_from_display_name\":\"Maria\",\n"
                    + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                    + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                    + "\"message_moderation_action\":\"UNDEFINED\",\n"
                    + "\"message_moderation_verdicts\":\"\",\n"
                    + "\"message_multi_item\":\"false\",\n"
                    + "\"message_data\":\"AA06\"\n");

            String messageId2 = chatId + "/102";
            String timestamp2 = "1599064592266015";
            cluster.messagesBackend().add(
                new StringPrefix(chatId),
                "\"id\":\"message_" + messageId2 + "\",\n"
                    + "\"type\":\"file_message\",\n"
                    + "\"message_id\":\"" + messageId2 + "\",\n"
                    + "\"message_chat_id_hash\":\"-7850\",\n"
                    + "\"message_hid\":\"0\",\n"
                    + "\"message_forwarded\":\"false\",\n"
                    + "\"message_chat_id\":\"" + chatId + "\",\n"
                    + "\"message_timestamp\":\"" + timestamp2 + "\",\n"
                    + "\"message_last_edit_timestamp\":\"0\",\n"
                    + "\"message_seq_no\":\"2\",\n"
                    + "\"message_filename\":\"svidetelstvo.pdf\",\n"
                    + "\"message_from_display_name\":\"Maria\",\n"
                    + "\"message_from_guid\":\"3838dh3-5000-4600-b23b-1123je899\",\n"
                    + "\"message_from_phone_id\":\"20d892-7967-4b5d-9155-239032iej93\",\n"
                    + "\"message_moderation_action\":\"UNDEFINED\",\n"
                    + "\"message_moderation_verdicts\":\"\",\n"
                    + "\"message_multi_item\":\"false\",\n"
                    + "\"message_data\":\"AA06\"\n");

            cluster.messagesBackend().flush();

            String uri = cluster.moxy().host() +
                "/api/search/messenger/chat-media?chat-id=" + chatId
                + "&near=1599064592266013&next=10&prev=10"
                + "&types=voice_message,file_message,image_message&request=";

            String result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 1,"
                    + "\"prev\": 1"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp1
                    + "},{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp2
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri, result);

            result =
                "{\"info\": {"
                    + "\"has_next\": false,"
                    + "\"has_prev\": false,"
                    + "\"next\": 0,"
                    + "\"prev\": 1"
                    + "},"
                    + "\"messages\": [{"
                    + "\"data\": \"qgY=\","
                    + "\"timestamp\": " + timestamp1
                    + "}]}";
            HttpAssert.assertJsonResponse(client, uri + "passport", result);
        }
    }
}

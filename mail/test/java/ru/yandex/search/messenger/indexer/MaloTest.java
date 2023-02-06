package ru.yandex.search.messenger.indexer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Set;

import NMessengerProtocol.Message;
import NMessengerProtocol.Search.TDocument;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonBoolean;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;

public class MaloTest extends TestBase {
    public static byte[] topicDoc(
            final String uuid,
            final String subType,
            final long version)
            throws Exception {
        TDocument document =
                TDocument.newBuilder()
                        .setUuid(uuid)
                        .setSubType(subType)
                        .setVersion(version).build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.writeTo(out);
        return out.toByteArray();
    }

    @Test
    public void testUpdatePolicy() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            String guid1 = "b8264d5d-b85c-456c-b64c-fec153fa2216";

            cluster.searchBackend().add(
                    "\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\"," +
                            "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"");
            String topic = "rt3.messenger--mssngr--test-search-buckets";
            String guid = "b8264d5d-b85c-456c-b64c-fec153fa2216";
            //&message-create-time=1586646097988&message-write-time=1586646098030
            HttpPost post = new HttpPost(
                    cluster.malo().host()
                            + "/index-search-bucket?&partition=20&offset=1&seqNo=1&topic=" + topic);
            post.setEntity(
                    new NByteArrayEntity(topicDoc(
                            guid,
                            "privacy",
                            1),
                            ContentType.DEFAULT_BINARY));

            String metaResponse =
                    "{\n" +
                            "  \"data\": {\n" +
                            "    \"bucket\": {\n" +
                            "      \"version\": 1,\n" +
                            "      \"bucket_value\": {\n" +
                            "        \"search\": 1,\n" +
                            "        \"online_status\": 1,\n" +
                            "        \"private_chats\": 0,\n" +
                            "        \"calls\": 0,\n" +
                            "        \"invites\": 0\n" +
                            "      },\n" +
                            "      \"bucket_name\": \"privacy\"\n" +
                            "    }\n" +
                            "  },\n" +
                            "  \"status\": \"ok\"\n" +
                            "}";
            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new StringChecker(
                            "request={\"method\":\"get_bucket\",\"params\":" +
                                "{\"guid\":\"" + guid + "\",\"bucket_name\":\"privacy\"}}"),
                        metaResponse),
                    "X-Version",
                    "6"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            cluster.searchBackend().checkSearch(
                    "/search?text=id:user_*&get=*&sync-searcher=true",
                    "{\"hitsCount\":1,\"hitsArray\":[" +
                            "{\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\", " +
                            "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"," +
                            "\"type\":\"user\"," +
                            "\"user_search_privacy\":\"1\"}]}");

            JsonMap expected0 = TypesafeValueContentHandler.parse(
                    "    {\n" +
                            "      \"id\": \"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\",\n" +
                            "      \"type\": \"user\",\n" +
                            "      \"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\",\n" +
                            "      \"user_version\": \"0\",\n" +
                            "      \"user_display_name\": \"Vasya\",\n" +
                            "      \"user_affiliation_nn\": \"na\",\n" +
                            "      \"user_geo\": \"false\",\n" +
                            "      \"user_is_robot\": \"false\",\n" +
                            "      \"user_data\": \"{\\\"org_id\\\":0," +
                            "\\\"guid\\\":\\\"b8264d5d-b85c-456c-b64c-fec153fa2216\\\"," +
                            "\\\"is_robot\\\":false,\\\"display_name\\\":\\\"Vasya\\\"," +
                            "\\\"passport_display_name\\\":\\\"Vasya\\\"," +
                            "\\\"version\\\":0,\\\"is_display_restricted\\\":false}\",\n" +
                            "      \"user_status\": \"NONE_STATUS\",\n" +
                            "      \"user_org_id\": \"0\",\n" +
                            "      \"user_has_service\": \"false\",\n" +
                            "      \"user_is_dismissed\": \"false\",\n" +
                            "      \"user_is_homeworker\": \"false\",\n" +
                            "      \"user_uid\": \"-1\",\n" +
                            "      \"user_search_privacy\": \"1\",\n" +
                            "      \"user_display_restricted\": \"false\"\n" +
                            "    }").asMap();

            expected0.put("user_search_privacy", new JsonString("1"));

            cluster.addUser(guid1, "Vasya");
            cluster.searchBackend().checkSearch(
                    "/search?text=id:user_*&get=*&sync-searcher=true",
                    "{\"hitsCount\":1,\"hitsArray\":[" + JsonType.NORMAL.toString(expected0) + "]}");

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.chatsBackend().indexerPort(), "/delete?prefix=0" +
                    "&text=id:*");
            cluster.chatsBackend().flush();
            JsonMap expected2 = TypesafeValueContentHandler.parse(
                    "    {\n" +
                            "      \"type\": \"user\",\n" +
                            "      \"user_id\": \"f61590b6-4076-49f7-bfd0-74dace3d8850\",\n" +
                            "      \"user_version\": \"0\",\n" +
                            "      \"user_display_name\": \"Petya\",\n" +
                            "      \"user_affiliation_nn\": \"na\",\n" +
                            "      \"user_geo\": \"false\",\n" +
                            "      \"user_is_robot\": \"false\",\n" +
                            "      \"user_data\": \"{\\\"org_id\\\":0,\\\"organizations\\\":[100500,100501]," +
                            "\\\"guid\\\":\\\"f61590b6-4076-49f7-bfd0-74dace3d8850\\\",\\\"is_robot\\\":false," +
                            "\\\"display_name\\\":\\\"Petya\\\",\\\"passport_display_name\\\":\\\"Petya\\\"," +
                            "\\\"version\\\":0,\\\"is_display_restricted\\\":false}\",\n" +
                            "      \"user_status\": \"NONE_STATUS\",\n" +
                            "      \"user_org_id\": \"0\",\n" +
                            "      \"user_has_service\": \"false\",\n" +
                            "      \"user_is_dismissed\": \"false\",\n" +
                            "      \"user_is_homeworker\": \"false\",\n" +
                            "      \"user_uid\": \"-1\",\n" +
                            "      \"user_organizations\": \"100500\\n100501\",\n" +
                            "      \"user_search_privacy\": \"2\",\n" +
                            "      \"user_display_restricted\": \"false\"\n" +
                            "    }").asMap();

            String guid2 = "f61590b6-4076-49f7-bfd0-74dace3d8850";
            JsonMap user2 = MaloCluster.defaultUser(guid2, "Petya", 0);
            user2.put("organizations", TypesafeValueContentHandler.parse("[100500, 100501]"));
            cluster.addUser(guid2, user2);
//            cluster.searchBackend().checkSearch(
//                "/search?prefix=100500&text=id:*&get=*,__prefix&sync-searcher=true",
//                "{\"hitsCount\":1,\"hitsArray\":[" + JsonType.NORMAL.toString(expected0) + "]}");

            cluster.setUserPrivacy(guid2, 2);
            //cluster.chatsBackend().flush();
            expected2.put("id", new JsonString("user_f61590b6-4076-49f7-bfd0-74dace3d8850@100500"));
            cluster.searchBackend().checkSearch(
                    "/search?prefix=100500&text=user_display_name_tokenized_p:Petya&get=*&sync-searcher=true",
                    "{\"hitsCount\":1,\"hitsArray\":[" + JsonType.NORMAL.toString(expected2) + "]}");
            expected2.put("id", new JsonString("user_f61590b6-4076-49f7-bfd0-74dace3d8850@100501"));
            cluster.searchBackend().checkSearch(
                    "/search?prefix=100501&text=user_display_name_tokenized_p:Petya&get=*&sync-searcher=true",
                    "{\"hitsCount\":1,\"hitsArray\":[" + JsonType.NORMAL.toString(expected2) + "]}");
            expected2.put("id", new JsonString("user_f61590b6-4076-49f7-bfd0-74dace3d8850@0"));
            cluster.searchBackend().checkSearch(
                    "/search?prefix=0&text=user_display_name_tokenized_p:Petya&get=*&sync-searcher=true",
                    "{\"hitsCount\":1,\"hitsArray\":[" + JsonType.NORMAL.toString(expected2) + "]}");
        }
    }

    protected static String chatMembersMoxyUri(final String chatId) {
        return "/sequential/search-malo-chat-members?chat-members-diff&service=chats_service&prefix=0&text=chat_id:("
                + chatId + ")&get=chat_members_version,chat_member_count&length=1&json-type=dollar";
    }

    @Test
    public void testHiddenChats() throws Exception {
        String metaResponse =
            "{\"status\":\"ok\",\"data\":{\"bucket\":{\"bucket_name\":\"hidden_private_chats\"," +
                "\"bucket_value\":{\"f61590b6-4076-49f7-bfd0-74dace3d8850\":1588015772438011}," +
                "\"version\":1588057896554722}}}";
        try (MaloCluster cluster = new MaloCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.chatsBackend().add(
                new LongPrefix(34),
                "\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@34\"," +
                    "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"");

            cluster.searchBackend().add(
                "\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\"," +
                    "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"");
            cluster.chatsBackend().flush();
            String topic = "rt3.messenger--mssngr--test-search-buckets";
            String guid = "b8264d5d-b85c-456c-b64c-fec153fa2216";
            //&message-create-time=1586646097988&message-write-time=1586646098030
            HttpPost post = new HttpPost(
                cluster.malo().host()
                    + "/index-search-bucket?&partition=20&offset=1&seqNo=1&topic=" + topic);
            post.setEntity(
                    new NByteArrayEntity(topicDoc(
                            guid,
                            "hidden_private_chats",
                            1),
                            ContentType.DEFAULT_BINARY));
            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new StringChecker(
                            "request={\"method\":\"get_bucket\",\"params\":" +
                                "{\"guid\":\"" + guid + "\",\"bucket_name" +
                                "\":\"hidden_private_chats\"}}"),
                        metaResponse),
                    "X-Version",
                    "6"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            cluster.chatsBackend().flush();
            // test for bug in non prefixed updater
            //https://st.yandex-team.ru/PS-3662
            HttpAssert.assertJsonResponse(client, cluster.searchBackend().searchUri()
                +"/printkeys?prefix=0&service=messenger_users&shard=0"
                + "&text=id:user_b8264d5d-b85c-456c-b64c-fec153fa2216@0&field=user_id_p&hr",
                "{\"0#b8264d5d-b85c-456c-b64c-fec153fa2216\": {}}");
            HttpAssert.assertJsonResponse(client, cluster.searchBackend().searchUri()
                    +"/printkeys?prefix=34&service=messenger_users&shard=0"
                    + "&text=id:user_b8264d5d-b85c-456c-b64c-fec153fa2216@34&field=user_id_p&hr",
                "{\"34#b8264d5d-b85c-456c-b64c-fec153fa2216\": {}}");

            cluster.searchBackend().checkSearch(
                "/search?text=id:user_*&get=*&sync-searcher=true&sort=id",
                "{\"hitsCount\":2,\"hitsArray\":[" +
                    "{\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@34\", " +
                    "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"," +
                    "\"user_hidden_pvp_chats\": \"f61590b6-4076-49f7-bfd0-74dace3d8850\t1588015772438011\"," +
                    "\"type\":\"user\"}," +
                    "{\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\", " +
                    "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"," +
                    "\"user_hidden_pvp_chats\": \"f61590b6-4076-49f7-bfd0-74dace3d8850\t1588015772438011\"," +
                    "\"type\":\"user\"}" +
                    "]}");
        }
    }

    @Test
    public void testRestrictions() throws Exception {
        String metaResponse =
                "{\"status\":\"ok\",\"data\":{\"bucket\":{\"bucket_name\":\"restrictions\"," +
                        "\"bucket_value\":{\"whitelist\":[],\"blacklist\":[" +
                        "\"68195171-3904-4120-a922-d832ee0a87c0\",\"68195171-1000-5000-a922-d832ee0a87c0\"]}," +
                        "\"version\":1544607241037037}}}";
        try (MaloCluster cluster = new MaloCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.searchBackend().add(
                    "\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\"," +
                            "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"");
            String topic = "rt3.messenger--mssngr--test-search-buckets";
            String guid = "b8264d5d-b85c-456c-b64c-fec153fa2216";
            //&message-create-time=1586646097988&message-write-time=1586646098030
            HttpPost post = new HttpPost(
                    cluster.malo().host()
                            + "/index-search-bucket?&partition=20&offset=1&seqNo=1&topic=" + topic);
            post.setEntity(
                    new NByteArrayEntity(topicDoc(
                            guid,
                            "restrictions",
                            1),
                            ContentType.DEFAULT_BINARY));
            cluster.metaApi().add(
                    "/meta_api/",
                    new ExpectingHeaderHttpItem(
                            new ExpectingHttpItem(
                                    new StringChecker(
                                            "request={\"method\":\"get_bucket\",\"params\":" +
                                                    "{\"guid\":\"" + guid + "\",\"bucket_name\":\"restrictions\"}}"),
                                    metaResponse),
                            "X-Version",
                            "6"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            cluster.searchBackend().checkSearch(
                    "/search?text=id:user_*&get=*&sync-searcher=true",
                    "{\"hitsCount\":1,\"hitsArray\":[" +
                            "{\"id\":\"user_b8264d5d-b85c-456c-b64c-fec153fa2216@0\", " +
                            "\"user_id\": \"b8264d5d-b85c-456c-b64c-fec153fa2216\"," +
                            "\"user_blacklisted_users\":\"68195171-3904-4120-a922-d832ee0a87c0\\n" +
                            "68195171-1000-5000-a922-d832ee0a87c0\\n\"," +
                            "\"user_whitelisted_users\": \"\"," +
                            "\"type\":\"user\"}]}");
        }
    }

    @Test
    public void testCreateChat() throws Exception {
        String chatTopic = "rt3.messenger--mssngr--prod-search-chats";
        String chatId = "0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36";
        String membersTopic = "rt3.messenger--mssngr--prod-search-chat-members";
        try (MaloCluster cluster = new MaloCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.moxy().add(chatMembersMoxyUri(chatId), "{\"hitsCount\": 0, \"hitsArray\":[]}");
            cluster.metaApi().add(
                    "/meta_api/",
                    new StaticHttpItem("{\"status\":\"ok\",\"data\":{}}"),
                    new ExpectingHttpItem(
                            new StringChecker("request={\"method\":\"get_chat_members_diff\"," +
                                    "\"params\":{\"chat_id\":\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"," +
                                    "\"version\":0}}"),
                            "{\"status\":\"ok\"," +
                                    "\"data\":{\"added\":[\"0a42889f-25bf-4ded-ac84-4bb506b708f9\"," +
                                    "\"292e606e-6466-4f4a-a1f0-0d0ecf02635f\"],\"removed\":[]," +
                                    "\"version\":1586772315422409}}"));

            HttpPost membersPost = new HttpPost(
                    cluster.malo().host() + "/?&partition=20&offset=1&seqNo=1&topic=" + membersTopic);
            membersPost.setEntity(
                    new NByteArrayEntity(topicDoc(
                            chatId,
                            "members",
                            1),
                            ContentType.DEFAULT_BINARY));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, membersPost);

            cluster.searchBackend().checkSearch(
                "/search?text=id:*&get=*&sync-searcher=true",
                "{\n" +
                    "  \"hitsCount\": 3,\n" +
                    "  \"hitsArray\": [\n" +
                    "    {\n" +
                    "      \"id\": \"chat_0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                    "      \"type\": \"chat\",\n" +
                    "      \"chat_id\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                    "      \"chat_members\": \"" +
                    "0a42889f-25bf-4ded-ac84-4bb506b708f9\n" +
                    "292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                    "      \"chat_members_version\": \"1586772315422409\",\n" +
                    "      \"chat_member_count\": \"2\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"user_chats_0a42889f-25bf-4ded-ac84-4bb506b708f9\",\n" +
                    "      \"type\": \"user_chats\",\n" +
                    "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                    "      \"user_chats_user_id\": \"0a42889f-25bf-4ded-ac84-4bb506b708f9\",\n" +
                    "      \"user_chats_tmp\": \"0\",\n" +
                    "      \"user_chats_versions\": " +
                    "\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\\t1586772315422409\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"user_chats_292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                    "      \"type\": \"user_chats\",\n" +
                    "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                    "      \"user_chats_user_id\": \"292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                    "      \"user_chats_tmp\": \"0\",\n" +
                    "      \"user_chats_versions\": " +
                    "\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\\t1586772315422409\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
            // now handling chat create event

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_chat\"," +
                        "\"params\":{\"chat_id\":\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"," +
                        "\"disable_members\":true}}"),
                    "{\"status\":\"ok\",\"data\":{\"entity_id\":null,\"resource_id\":null,\"org_id\":0," +
                        "\"geo_info\":null,\"private\":false,\"member1\":null,\"create_timestamp\":" +
                        "1586772315.4224090576,\"member2\":null,\"permission_users\":[" +
                        "\"292e606e-6466-4f4a-a1f0-0d0ecf02635f\"," +
                        "\"0a42889f-25bf-4ded-ac84-4bb506b708f9\"]," +
                        "\"exclude\":null," +
                        "\"geo_type\":\"chat\",\"id\":105949843,\"description\":\"\",\"org_version\":0," +
                        "\"permission_groups\":null,\"is_family\":null,\"namespace\":null," +
                        "\"version\":1586772315422409,\"latitude\":null,\"public\":false," +
                        "\"channel\":false," +
                        "\"metadata\":null,\"fork\":true,\"restriction_status\":\"active\"," +
                        "\"request_id\":\"158677231534524313\"," +
                        "\"disable_members_updated_notifications\":null," +
                        "\"parent_url\":null,\"moderation\":null," +
                        "\"role_users\":[\"292e606e-6466-4f4a-a1f0-0d0ecf02635f\"]," +
                        "\"chat_id\":\"0\\/0\\/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\"members\":[]," +
                        "\"is_public\":null,\"moderation_disabled\":true,\"name\":\"testtestonetwo\"," +
                        "\"subservice\":null,\"rate_limit\":null,\"sequence_id\":null,\"longitude\":null," +
                        "\"geo_id\":null,\"raw_avatar_id\":null,\"show_on_morda\":null," +
                        "\"invite_hash\":null," +
                        "\"pinned_messages\":null,\"permission_departments\":null}}"));
            HttpPost chatPost = new HttpPost(
                    cluster.malo().host()
                            + "/?&partition=20&offset=1&seqNo=1&topic=" + chatTopic);
            chatPost.setEntity(
                    new NByteArrayEntity(topicDoc(
                            chatId,
                            "chat",
                            1),
                            ContentType.DEFAULT_BINARY));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, chatPost);

            cluster.searchBackend().checkSearch(
                    "/search?text=id:*&get=*,-chat_data&sync-searcher=true&skip-deleted",
                    "{\n" +
                            "  \"hitsCount\": 3,\n" +
                            "  \"hitsArray\": [\n" +
                            "    {\n" +
                            "      \"id\": \"user_chats_0a42889f-25bf-4ded-ac84-4bb506b708f9\",\n" +
                            "      \"type\": \"user_chats\",\n" +
                            "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                            "      \"user_chats_user_id\": \"0a42889f-25bf-4ded-ac84-4bb506b708f9\",\n" +
                            "      \"user_chats_tmp\": \"0\",\n" +
                            "      \"user_chats_versions\": " +
                            "\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\\t1586772315422409\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"id\": \"user_chats_292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                            "      \"type\": \"user_chats\",\n" +
                            "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                            "      \"user_chats_user_id\": \"292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                            "      \"user_chats_tmp\": \"0\",\n" +
                            "      \"user_chats_versions\": " +
                            "\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\\t1586772315422409\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"id\": \"chat_0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                            "      \"type\": \"chat\",\n" +
                            "      \"chat_id\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\",\n" +
                            "      \"chat_version\": \"1586772315422409\",\n" +
                            "      \"chat_org_id\": \"0\",\n" +
                            "      \"chat_create_timestamp\": \"1.58677235E9\",\n" +
                            "      \"chat_private\": \"false\",\n" +
                            "      \"chat_public\": \"false\",\n" +
                            "      \"chat_show_on_morda\": \"false\",\n" +
                            "      \"chat_channel\": \"false\",\n" +
                            "      \"chat_name\": \"testtestonetwo\",\n" +
                            "      \"chat_description\": \"\",\n" +
                            "      \"chat_namespace\": \"0\",\n" +
                            "      \"chat_members\": " +
                            "\"0a42889f-25bf-4ded-ac84-4bb506b708f9\\n292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                            "      \"chat_members_version\": \"1586772315422409\",\n" +
                            "      \"chat_member_count\": \"2\",\n" +
                            "      \"chat_permissions_users\": " +
                            "\"292e606e-6466-4f4a-a1f0-0d0ecf02635f\\n0a42889f-25bf-4ded-ac84-4bb506b708f9\",\n" +
                            "      \"chat_permissions_groups\": \"\",\n" +
                            "      \"chat_permissions_departments\": \"\",\n" +
                            "      \"chat_roles_admin\": \"292e606e-6466-4f4a-a1f0-0d0ecf02635f\",\n" +
                            "      \"chat_geo\": \"false\",\n" +
                            "      \"chat_geo_type\": \"chat\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}");
        }
    }

    @Test
    public void testUserChatsUpdate() throws Exception {
        String chatId = "0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36";
        String membersTopic = "rt3.messenger--mssngr--prod-search-chat-members";
        try (MaloCluster cluster = new MaloCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            // add chat

            String args = "\"user_affiliation_nn\": \"na\",\n"
                + "\"user_geo\": \"false\",\n"
                + "\"user_is_robot\": \"false\",\n"
                + "\"user_is_homeworker\": \"false\",\n"
                + "\"user_gender\": \"0\"";
            String user1 = user("0a42889f-25bf-4ded-ac84-4bb506b708f9", "0", args);
            String user2 = user("0a42889f-25bf-4ded-ac84-4bb506b708f9", "34762", args);
            String user3 = user("292e606e-6466-4f4a-a1f0-0d0ecf02635f", "0", args);
            String user4 = user("292e606e-6466-4f4a-a1f0-0d0ecf02635f", "299", args);
            cluster.searchBackend().add(user1, user2, user3, user4);
            cluster.moxy().add(chatMembersMoxyUri(chatId), "{\"hitsCount\": 0, \"hitsArray\":[]}");
            cluster.metaApi().add(
                "/meta_api/",
                new StaticHttpItem("{\"status\":\"ok\",\"data\":{}}"),
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_chat_members_diff\"," +
                        "\"params\":{\"chat_id\":\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"," +
                        "\"version\":0}}"),
                    "{\"status\":\"ok\"," +
                        "\"data\":{\"added\":[\"0a42889f-25bf-4ded-ac84-4bb506b708f9\"," +
                        "\"292e606e-6466-4f4a-a1f0-0d0ecf02635f\"],\"removed\":[]," +
                        "\"version\":1586772315422409}}"));

            HttpPost membersPost = new HttpPost(
                cluster.malo().host() + "/?&partition=20&offset=1&seqNo=1&topic=" + membersTopic);
            membersPost.setEntity(
                new NByteArrayEntity(topicDoc(
                    chatId,
                    "members",
                    1),
                    ContentType.DEFAULT_BINARY));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, membersPost);

            cluster.searchBackend().checkSearch(
                "/search?text=type:user&get=*&sync-searcher=true",
                "{\n" +
                    "  \"hitsCount\": 4,\n" +
                    "  \"hitsArray\": [\n" +
                    "    {" + user2 + "},\n" +
                    "    {" + user4 + "},\n" +
                    "    {\n" +
                    user1 + ',' +
                    "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"\n" +
                    "    },\n" +
                    "    {\n" +
                    user3 + ',' +
                    "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");


            // remove chat

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_chat_members_diff\"," +
                        "\"params\":{\"chat_id\":\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"," +
                        "\"version\":1586772315422409}}"),
                    "{\"status\":\"ok\"," +
                        "\"data\":{\"added\":[]," +
                        "\"removed\":[\"0a42889f-25bf-4ded-ac84-4bb506b708f9\"]," +
                        "\"version\":1586772315422409}}"));

            HttpPost membersPost2 = new HttpPost(
                cluster.malo().host() + "/?&partition=20&offset=1&seqNo=1&topic=" + membersTopic);
            membersPost2.setEntity(
                new NByteArrayEntity(topicDoc(
                    chatId,
                    "members",
                    1),
                    ContentType.DEFAULT_BINARY));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, membersPost2);

            cluster.searchBackend().checkSearch(
                "/search?text=type:user&get=*&sync-searcher=true",
                "{\n" +
                    "  \"hitsCount\": 4,\n" +
                    "  \"hitsArray\": [\n" +
                    "    {" + user2 + "},\n" +
                    "    {" + user4 + "},\n" +
                    "    {\n" +
                    user3 + ',' +
                    "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"\n" +
                    "    },\n" +
                    "    {\n" +
                    user1 + ',' +
                    "      \"user_chats\": \"\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");


            // add few chats

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_chat_members_diff\"," +
                        "\"params\":{\"chat_id\":\"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"," +
                        "\"version\":1586772315422409}}"),
                    "{\"status\":\"ok\"," +
                        "\"data\":{\"added\":[\"0a42889f-25bf-4ded-ac84-4bb506b708f9\"]," +
                        "\"removed\":[]," +
                        "\"version\":1586772315422409}}"));

            HttpPost membersPost3 = new HttpPost(
                cluster.malo().host() + "/?&partition=20&offset=1&seqNo=1&topic=" + membersTopic);
            membersPost3.setEntity(
                new NByteArrayEntity(topicDoc(
                    chatId,
                    "members",
                    1),
                    ContentType.DEFAULT_BINARY));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, membersPost3);

            String chatId2 = "1/2/6049da1d-c5b8-4dcb-9657-fffdfd06c4c0";
            cluster.addChatOrgsResolve();
            cluster.metaApi().add(
                "/meta_api/",
                new StaticHttpItem("{\"status\":\"ok\",\"data\":{}}"),
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_chat_members_diff\"," +
                        "\"params\":{\"chat_id\":\"1/2/6049da1d-c5b8-4dcb-9657-fffdfd06c4c0\"," +
                        "\"version\":0}}"),
                    "{\"status\":\"ok\"," +
                        "\"data\":{\"added\":[\"0a42889f-25bf-4ded-ac84-4bb506b708f9\"]," +
                        "\"removed\":[]," +
                        "\"version\":1586772315422409}}"));

            HttpPost membersPost4 = new HttpPost(
                cluster.malo().host() + "/?&partition=20&offset=1&seqNo=1&topic=" + membersTopic);
            membersPost4.setEntity(
                new NByteArrayEntity(topicDoc(
                    chatId2,
                    "members",
                    1),
                    ContentType.DEFAULT_BINARY));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, membersPost4);

            cluster.searchBackend().checkSearch(
                "/search?text=type:user&get=*&sync-searcher=true",
                "{\n" +
                    "  \"hitsCount\": 4,\n" +
                    "  \"hitsArray\": [\n" +
                    "    {" + user2 + "},\n" +
                    "    {" + user4 + "},\n" +
                    "    {\n" +
                    user3 + ',' +
                    "      \"user_chats\": \"0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"\n" +
                    "    },\n" +
                    "    {\n" +
                    user1 + ',' +
                    "      \"user_chats\": \"1/2/6049da1d-c5b8-4dcb-9657-fffdfd06c4c0\n" +
                    "0/0/aecaa0e0-00f7-4310-b848-8e1e1f7ced36\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
        }
    }

    @Test
    public void testUserChatsPreserve() throws Exception {
        String guid = "b8264d5d-b85c-456c-b64c-fec153fa2216";
        String chats = "0/0/a0f9fa8a-6563-4063-81d2-d187f23306f1\\n"
            + "0/0/6ebc9de5-20a7-49ff-9c53-9c2c2e2d8d46\\n"
            + "0/0/17ff0d2c-ba49-4c2c-bac7-9c02ae27480e";
        String searchRequest = "/search?text=id:user_*&get=*&sync-searcher=true";
        try (MaloCluster cluster = new MaloCluster(this);
            CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.chatsBackend().add(
                new LongPrefix(0),
                "\"id\":\"user_" + guid + "@0\"," +
                    "\"user_id\": \"" + guid + "\"," +
                    "\"user_chats\": \"" + chats + "\"");

            // get user

            cluster.addUser(
                guid,
                MaloBaseCluster.defaultUser(guid, "Alex M", 1588057896554722L));

            String userResult =
                "\"id\":\"user_" + guid + "@0\", " +
                "\"user_id\": \"" + guid + "\"," +
                "\"user_chats\": \"" + chats + "\"," +
                "\"user_affiliation_nn\": \"na\"," +
                "\"type\":\"user\"," +
                "\"user_data\": \"{\\\"org_id\\\":0,"
                    + "\\\"guid\\\":\\\"" + guid + "\\\","
                    + "\\\"is_robot\\\":false,"
                    + "\\\"display_name\\\":\\\"Alex M\\\","
                    + "\\\"passport_display_name\\\":\\\"Alex M\\\","
                    + "\\\"version\\\":1588057896554722,"
                    + "\\\"is_display_restricted\\\":false}\","
                + "\"user_display_name\": \"Alex M\","
                + "\"user_display_restricted\": \"false\","
                + "\"user_geo\": \"false\","
                + "\"user_has_service\": \"false\","
                + "\"user_is_dismissed\": \"false\","
                + "\"user_is_homeworker\": \"false\","
                + "\"user_is_robot\": \"false\","
                + "\"user_org_id\": \"0\","
                + "\"user_status\": \"NONE_STATUS\","
                + "\"user_uid\": \"-1\","
                + "\"user_version\": \"1588057896554722\"";

            cluster.searchBackend().checkSearch(
                searchRequest,
                "{\"hitsCount\":1,\"hitsArray\":[{" + userResult + "}]}");

            // add hidden pvp

            cluster.chatsBackend().flush();
            String topic = "rt3.messenger--mssngr--test-search-buckets";
            HttpPost post = new HttpPost(
                cluster.malo().host()
                    + "/index-search-bucket?&partition=20&offset=1&seqNo=1&topic=" + topic);
            post.setEntity(
                new NByteArrayEntity(topicDoc(guid, "hidden_private_chats", 1),
                ContentType.DEFAULT_BINARY));
            String metaResponse =
                "{\"status\":\"ok\",\"data\":{\"bucket\":{\"bucket_name\":\"hidden_private_chats\"," +
                    "\"bucket_value\":{\"f61590b6-4076-49f7-bfd0-74dace3d8850\":1588015772438011}," +
                    "\"version\":1588057896554722}}}";
            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new StringChecker(
                            "request={\"method\":\"get_bucket\",\"params\":" +
                                "{\"guid\":\"" + guid + "\",\"bucket_name" +
                                "\":\"hidden_private_chats\"}}"),
                        metaResponse),
                    "X-Version",
                    "6"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            cluster.chatsBackend().flush();

            userResult += ",\"user_hidden_pvp_chats\": \"f61590b6-4076-49f7-bfd0-74dace3d8850\t1588015772438011\"";

            cluster.searchBackend().checkSearch(
                searchRequest,
                "{\"hitsCount\":1,\"hitsArray\":[{" + userResult + "}]}");

            // add blacklisted users

            metaResponse =
                "{\"status\":\"ok\",\"data\":{\"bucket\":{\"bucket_name\":\"restrictions\"," +
                    "\"bucket_value\":{\"whitelist\":[],\"blacklist\":[" +
                    "\"68195171-3904-4120-a922-d832ee0a87c0\",\"68195171-1000-5000-a922-d832ee0a87c0\"]}," +
                    "\"version\":1544607241037037}}}";

            post.setEntity(
                new NByteArrayEntity(topicDoc(guid, "restrictions", 1),
                ContentType.DEFAULT_BINARY));
            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new StringChecker(
                            "request={\"method\":\"get_bucket\",\"params\":" +
                                "{\"guid\":\"" + guid + "\",\"bucket_name\":\"restrictions\"}}"),
                        metaResponse),
                    "X-Version",
                    "6"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            userResult +=
                ",\"user_blacklisted_users\":" +
                "\"68195171-3904-4120-a922-d832ee0a87c0\\n" +
                "68195171-1000-5000-a922-d832ee0a87c0\\n\"," +
                "\"user_whitelisted_users\": \"\"";

            cluster.searchBackend().checkSearch(
                searchRequest,
                "{\"hitsCount\":1,\"hitsArray\":[{" + userResult + "}]}");

            // add user search privacy

            post.setEntity(
                new NByteArrayEntity(topicDoc(
                    guid,
                    "privacy",
                    1),
                    ContentType.DEFAULT_BINARY));

            metaResponse =
                "{\n" +
                    "  \"data\": {\n" +
                    "    \"bucket\": {\n" +
                    "      \"version\": 1,\n" +
                    "      \"bucket_value\": {\n" +
                    "        \"search\": 1,\n" +
                    "        \"online_status\": 1,\n" +
                    "        \"private_chats\": 0,\n" +
                    "        \"calls\": 0,\n" +
                    "        \"invites\": 0\n" +
                    "      },\n" +
                    "      \"bucket_name\": \"privacy\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"status\": \"ok\"\n" +
                    "}";
            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(
                        new StringChecker(
                            "request={\"method\":\"get_bucket\",\"params\":" +
                                "{\"guid\":\"" + guid + "\",\"bucket_name\":\"privacy\"}}"),
                        metaResponse),
                    "X-Version",
                    "6"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            userResult += ",\"user_search_privacy\":\"1\"";

            cluster.searchBackend().checkSearch(
                searchRequest,
                "{\"hitsCount\":1,\"hitsArray\":[{" + userResult + "}]}");
        }
    }

    @Test
    public void testAddMessage() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            Message.TMessageInfoResponse.Builder builder =
                    Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                    MaloTest.class.getResourceAsStream(
                            "channel_message_with_reactions.json"),
                    builder);


            String chatId = "1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6";
            long ts = 1589281178664043L;
            cluster.addMessage(builder.build(), chatId, ts);
            cluster.messagesBackend().checkSearch("/search?&text=id:*&get=*&hr&prefix=" + chatId, new JsonChecker("{ " +
                    "\"hitsCount\": 1,\"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"id\": \"message_1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6/1589281178664043\",\n" +
                    "            \"type\": \"gallery_message\",\n" +
                    "            \"message_id\": \"1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6/1589281178664043\",\n" +
                    "            \"message_chat_id_hash\": \"-7356\",\n" +
                    "            \"message_hid\": \"0\",\n" +
                    "            \"message_forwarded\": \"false\",\n" +
                    "            \"message_data\": " +
                    "\"AA06E50C0AD30A22D00A3AA7060ADA05E29AA1D092D0BDD0B8D0BCD0B0D0BDD0B8D0B53A20D0BAD0BED0BDD0BAD183D180D181210A0AD09CD18B20D180D0B5D188D0B8D0BBD0B820D0B4D0BED0B1D0B0D0B2D0B8D182D18C20D0B220D187D0B0D18220D0BDD0B5D0BCD0BDD0BED0B3D0BE20D0B0D0B7D0B0D180D182D0B02E20D0A0D0B0D0B7D18BD0B3D180D18BD0B2D0B0D0B5D0BC2035302028D0BFD18FD182D18CD0B4D0B5D181D18FD182212920D0BFD0BED0B4D0BFD0B8D181D0BED0BA20D0BDD0B020393020D0B4D0BDD0B5D0B920D0BDD0B020D09AD0B8D0BDD0BED09FD0BED0B8D181D0BA204844210AD0A7D182D0BE20D0BDD183D0B6D0BDD0BE20D181D0B4D0B5D0BBD0B0D182D18C3A200AF09F918920D0BFD0BED181D182D0B0D0B2D18CD182D0B520D0BBD0B0D0B9D0BA2028D0B8D0BBD0B820D0B4D180D183D0B3D183D18E20D180D0B5D0B0D0BAD186D0B8D18E2920D18DD182D0BED0BCD18320D0BFD0BED181D182D18320D0B4D0BE20313420D0BCD0B0D18F3B0AF09F918920D0BED182D0BFD180D0B0D0B2D18CD182D0B520D18DD182D0BED18220D0BFD0BED181D18220D0B4D180D183D0B7D18CD18FD0BC2C20D0BAD0BED182D0BED180D18BD0BC20D0B2D18B20D0B6D0B5D0BBD0B0D0B5D182D0B520D0BFD0BED0B1D0B5D0B4D18B20F09F98893B0AF09F918920353020D181D187D0B0D181D182D0BBD0B8D0B2D187D0B8D0BAD0BED0B220D0B1D183D0B4D183D18220D181D0BBD183D187D0B0D0B9D0BDD18BD0BC20D0BED0B1D180D0B0D0B7D0BED0BC20D0B2D18BD0B1D180D0B0D0BDD18B20D0BCD0B5D0B6D0B4D18320D0B2D181D0B5D0BCD0B82C20D0BAD182D0BE20D0BED182D180D0B5D0B0D0B3D0B8D180D183D0B5D18220D0BDD0B020D0BFD0BED181D1823B0AF09F918920D0BCD18B20D181D0B2D18FD0B6D0B5D0BCD181D18F20D18120D0BFD0BED0B1D0B5D0B4D0B8D182D0B5D0BBD18FD0BCD0B820313520D0BCD0B0D18F2E0AD096D0B5D0BBD0B0D0B5D0BC20D183D0B4D0B0D187D0B82112480A460A3E1205332E6A706718BFBB07223166696C652F323335353438302F38326435333139342D646331652D346531332D623731662D66386430343038653462636510800F18B808AA0628312F322F65376331333535362D386564372D346239372D623463642D353639663635343162316236CA06D4037B2273657276696365223A207B2270756964223A2022343239343033363835222C2022736572766963654E616D65223A2022574542222C2022726567696F6E223A20225C75303431635C75303433655C75303434315C75303433615C75303433325C7530343330222C202279756964223A202235393938353134313131353631333734333132222C20226973486973746F7279223A20747275652C20227569223A20226465736B746F70222C2022746573745F696473223A205B22313837323238222C2022323336303330222C2022323336383338222C2022323336363932222C2022323337383435222C2022323336373132222C2022323334343236222C2022313234303731222C20223330343931222C2022323337363436222C20223435393633222C2022323336353634222C20223436343531225D2C20227561223A20224D6F7A696C6C612F352E30202857696E646F7773204E542031302E303B2057696E36343B2078363429204170706C655765624B69742F3533372E333620284B48544D4C2C206C696B65204765636B6F29204368726F6D652F38312E302E343034342E313239205361666172692F3533372E3336222C20226964223A2022323332332D373239362D313735372D31623431227D7DDA061B31613532323935622D613933382D633166302D3938646236363964FA06026F6B12C90108EBF8E8E095AEE902108BAFC0EEEFADE90218B50A20DBD0EFF5C8AEE902280232A1010A59636861745F6176617461722F312F322F65376331333535362D386564372D346239372D623463642D3536396636353431623162362F37386232323134652D656337662D343539372D623631612D3938363264636362336463381215D09AD0B8D0BDD0BED09FD0BED0B8D181D0BA2048441A2465376331333535362D386564372D346239372D623463642D353639663635343162316236288BF292CEE3ECE802400358971B2A060886EC0710082A0608B7EC0710072A0608ADEC0710012A0708CDE80710FA012A0608CEE80710022A0608B2EC0710072A0508E44E10212A0608A5EA071026308205B20602081B\",\n" +
                    "            \"message_chat_id\": \"1/2/e7c13556-8ed7-4b97-b4cd-569f6541b1b6\",\n" +
                    "            \"message_timestamp\": \"1589281178664043\",\n" +
                    "            \"message_last_edit_timestamp\": \"1589294913022043\",\n" +
                    "            \"message_seq_no\": \"1333\",\n" +
                    "            \"message_text\": \"⚡Внимание: конкурс!\\n\\nМы решили добавить в чат немного азарта. " +
                    "Разыгрываем 50 (пятьдесят!) подписок на 90 дней на КиноПоиск HD!\\nЧто нужно сделать: " +
                    "\\n\uD83D\uDC49 поставьте лайк (или другую реакцию) этому посту до 14 мая;\\n\uD83D\uDC49 отправьте " +
                    "этот пост друзьям, которым вы желаете победы \uD83D\uDE09;\\n\uD83D\uDC49 50 счастливчиков будут " +
                    "случайным образом выбраны между всеми, кто отреагирует на пост;\\n\uD83D\uDC49 мы свяжемся с " +
                    "победителями 15 мая.\\nЖелаем удачи!\",\n" +
                    "            \"message_from_display_name\": \"КиноПоиск HD\",\n" +
                    "            \"message_from_guid\": \"e7c13556-8ed7-4b97-b4cd-569f6541b1b6\",\n" +
                    "            \"message_from_phone_id\": \"\",\n" +
                    "            \"message_moderation_action\": \"KEEP\",\n" +
                    "            \"message_moderation_verdicts\": \"ok\",\n" +
                    "            \"message_gallery_images\": \"0:3.jpg\\n\",\n" +
                    "            \"message_multi_item\": \"true\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"));
        }
    }

    @Test
    public void testAddImportantMessage() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            Message.TMessageInfoResponse.Builder builder =
                Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                MaloTest.class.getResourceAsStream(
                    "important_message.json"),
                builder);


            String chatId = "1b04a2d0-5000-4600-b23b-f5ac5ad9e754_4416df53-8761-441a-aa73-54e37fac6d09";
            long ts = 1599064592266011L;
            cluster.addMessage(builder.build(), chatId, ts);
            cluster.messagesBackend().checkSearch(
                "/search?&text=id:*&get=*,-message_data&hr&prefix=" + chatId,
                new JsonChecker("{\n" +
                    "    \"hitsCount\": 1,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"id\": \"message_1b04a2d0-5000-4600-b23b-f5ac5ad9e754_4416df53-8761-441a-aa73" +
                    "-54e37fac6d09/1599064592266011\",\n" +
                    "            \"type\": \"text_message\",\n" +
                    "            \"message_id\": \"1b04a2d0-5000-4600-b23b-f5ac5ad9e754_4416df53-8761-441a-aa73" +
                    "-54e37fac6d09/1599064592266011\",\n" +
                    "            \"message_chat_id_hash\": \"-7850\",\n" +
                    "            \"message_hid\": \"0\",\n" +
                    "            \"message_forwarded\": \"false\",\n" +
                    "            \"message_chat_id\": \"1b04a2d0-5000-4600-b23b-f5ac5ad9e754_4416df53-8761-441a-aa73" +
                    "-54e37fac6d09\",\n" +
                    "            \"message_timestamp\": \"1599064592266011\",\n" +
                    "            \"message_last_edit_timestamp\": \"0\",\n" +
                    "            \"message_seq_no\": \"2\",\n" +
                    "            \"message_text\": \"Вот это сообщение - важное\",\n" +
                    "            \"message_from_display_name\": \"Olga N.\",\n" +
                    "            \"message_from_guid\": \"1b04a2d0-5000-4600-b23b-f5ac5ad9e754\",\n" +
                    "            \"message_from_phone_id\": \"d744fe50-7967-4b5d-9155-bc01b9519be1\",\n" +
                    "            \"message_moderation_action\": \"UNDEFINED\",\n" +
                    "            \"message_moderation_verdicts\": \"\",\n" +
                    "            \"message_multi_item\": \"false\",\n" +
                    "            \"message_important\": \"1\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"));
        }
    }

    @Test
    public void testCorpUser() throws Exception {
        String guid = "18bba94a-537f-4e4c-9b79-f313f312ef45";
        try (MaloCluster cluster = new MaloCluster(this)) {

            // get user
            String corpUser = "{\n" +
                "    \"raw_employee_info\": [\n" +
                "      {\n" +
                "        \"department\": {\n" +
                "          \"id\": \"1749\",\n" +
                "          \"name\": \"Группа поиска и извлечения фактов\"\n" +
                "        },\n" +
                "        \"organization_id\": 34,\n" +
                "        \"position\": \"TechLead\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"mssngr_nickname\": \"vonidu\",\n" +
                "    \"nickname\": \"\",\n" +
                "    \"uid\": 1120000000040290,\n" +
                "    \"version\": 1591797653336649,\n" +
                "    \"gender\": 1,\n" +
                "    \"is_robot\": false,\n" +
                "    \"org_id\": 34,\n" +
                "    \"guid\": \"18bba94a-537f-4e4c-9b79-f313f312ef45\",\n" +
                "    \"raw_avatar_id\": \"1490194580\",\n" +
                "    \"is_onboarded\": true,\n" +
                "    \"is_dismissed\": false,\n" +
                "    \"display_name\": \"Иван Дудинов\",\n" +
                "    \"passport_display_name\": \"Иван Дудинов\",\n" +
                "    \"organizations\": [34]," +
                "    \"affiliation\": \"yandex\"}";
            cluster.addUser(
                guid,
                TypesafeValueContentHandler.parse(corpUser).asMap());


            cluster.chatsBackend().checkSearch(
                "/search?prefix=0&get=id,user_nickname&text=user_id:" + guid,
                "{\n" +
                    "  \"hitsCount\": 2,\n" +
                    "  \"hitsArray\": [" +
                    "{\"id\": \"user_18bba94a-537f-4e4c-9b79-f313f312ef45@0\"," +
                    "\"user_nickname\": \"vonidu\"}," +
                    "{\"id\": \"user_18bba94a-537f-4e4c-9b79-f313f312ef45@34\"," +
                    "\"user_nickname\": \"vonidu\"}" +
                    "]}");
        }
    }

    @Ignore
    @Test
    public void testContactsReindexSubtype() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String guid = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";
            String contact1Guid = "aaaaaaaa-af0a-469e-b5d9-668b270c6ba2";
            String indexUri = "/index-contacts?&partition=20&offset=1&seqNo=1"
                + "&topic=rt3.messenger--mssngr--prod-search-contacts"
                + "&user-id=" + guid;

            JsonList list = new JsonList(BasicContainerFactory.INSTANCE);
            JsonMap contact1 = new JsonMap(BasicContainerFactory.INSTANCE);
            contact1.put("contact_name", new JsonString("Дмитрий"));
            contact1.put("guid", new JsonString(contact1Guid));
            contact1.put("version", new JsonLong(20));
            contact1.put("deleted", JsonBoolean.valueOf(false));
            list.add(contact1);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 0 +  "}}"),
                    "{\"status\":\"ok\",\"data\":"+ JsonType.NORMAL.toString(list) + "}"));

            checkGlobalVersion(cluster, guid, null);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 20L);

            cluster.messagesBackend().checkSearch(
                "/search?text=contact_id:*&length=0&get=*&prefix=" + guid,
                "{\"hitsCount\":1, \"hitsArray\": []}");
            // subtype reindex version 0 request
            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 0 +  "}}"),
                    "{\"status\":\"ok\",\"data\":[]}"));

            HttpPost indexPost =
                new HttpPost(cluster.malo().host() + indexUri);
            indexPost.setEntity(
                new ByteArrayEntity(
                    topicDoc(guid, "reindex", 21)));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, indexPost);
            cluster.messagesBackend().flush();
            cluster.messagesBackend().checkSearch(
                "/search?text=contact_id:*&get=*&sync-searcher=true&prefix=" + guid,
                "{\"hitsCount\":0, \"hitsArray\": []}");
        }
    }

    @Test
    public void testChatNamespaceForAllChats() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.updateChat("1/0/chat1", 1L);
            HttpAssert.assertJsonResponse(
                client,
                cluster.chatsBackend().searchUri()
                    + "/search?text=id:chat_1/0/chat1&get=chat_namespace",
                "{\"hitsCount\":1, \"hitsArray\":[{\"chat_namespace\": \"0\"}]}");
            cluster.addMessage("1/0/chat1", 100500L, "guid1");
            HttpAssert.assertJsonResponse(
                client,
                cluster.chatsBackend().searchUri()
                    + "/search?text=id:chat_1/0/chat1&get=chat_namespace",
                "{\"hitsCount\":1, \"hitsArray\":[{\"chat_namespace\": \"0\"}]}");

            cluster.updatePvpChat("guid1", "guid2", 1L);
            HttpAssert.assertJsonResponse(
                client,
                cluster.chatsBackend().searchUri()
                    + "/search?text=id:chat_guid1_guid2&get=chat_namespace",
                "{\"hitsCount\":1, \"hitsArray\":[{\"chat_namespace\": \"0\"}]}");
        }
    }

    @Ignore
    @Test
    public void testContactsVersion() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            String guid = "f215e4fb-af0a-469e-b5d9-668b270c6ba2";

            JsonList list = new JsonList(BasicContainerFactory.INSTANCE);

            String contact1Guid = "4bd919b2-2d7f-4be6-8ef7-d7a3e60362b8";
            JsonMap contact1 = new JsonMap(BasicContainerFactory.INSTANCE);
            contact1.put("contact_name", new JsonString("Дмитрий"));
            contact1.put("guid", new JsonString(contact1Guid));
            contact1.put("version", new JsonLong(20));
            contact1.put("deleted", JsonBoolean.valueOf(false));
            list.add(contact1);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 0 +  "}}"),
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list) + "}"));


            // first empty global version

            checkGlobalVersion(cluster, guid, null);


            // after indexing - global version 20

            String indexUri = "/index-contacts?&partition=20&offset=1&seqNo=1"
                + "&topic=rt3.messenger--mssngr--prod-search-contacts"
                + "&user-id=" + guid;

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 20L);


            // data is empty - global version has not changed

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 20 +  "}}"),
                    "{\"status\":\"ok\",\"data\":[]}"));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 20L);


            // meta_api error without code - global version has not changed

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 20 +  "}}"),
                    "{\"status\":\"error\",\"data\":{}}"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                cluster.malo().port(),
                indexUri);
            checkGlobalVersion(cluster, guid, 20L);


            // meta_api error with code - drop contacts, global version 0

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 20 +  "}}"),
                    "{\"status\":\"error\",\"data\":{\"code\":\"unhandled\"}}"));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 0L);


            // few contacts - global version is max version

            String contact2Guid = "74h3627-2d7f-4be6-8ef7-d7a3e60362b8";
            JsonMap contact2 = new JsonMap(BasicContainerFactory.INSTANCE);
            contact2.put("contact_name", new JsonString("Иван"));
            contact2.put("guid", new JsonString(contact2Guid));
            contact2.put("version", new JsonLong(55));
            contact2.put("deleted", JsonBoolean.valueOf(false));
            list.add(contact2);

            String contact3Guid = "3837h6h-2d7f-4be6-8ef7-d7a3e60362b8";
            JsonMap contact3 = new JsonMap(BasicContainerFactory.INSTANCE);
            contact3.put("contact_name", new JsonString("Алексей"));
            contact3.put("guid", new JsonString(contact3Guid));
            contact3.put("version", new JsonLong(120));
            contact3.put("deleted", JsonBoolean.valueOf(true));
            list.add(contact3);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 0 + "}}"),
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list) + "}"));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 55L);


            // delete all contacts - global version 0

            list = new JsonList(BasicContainerFactory.INSTANCE);
            contact1.put("deleted", JsonBoolean.valueOf(true));
            contact2.put("deleted", JsonBoolean.valueOf(true));
            list.add(contact1);
            list.add(contact2);
            list.add(contact3);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 55L + "}}"),
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list) + "}"));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 0L);


            // no global version - max contacts version from existing contacts

            cluster.addUser(contact1Guid, "Дмитрий");
            cluster.addUser(contact2Guid, "Иван");
            cluster.addContact(guid, contact1Guid, "Дмитрий", 23L);
            cluster.addContact(guid, contact2Guid, "Иван", 78L, 23L);

            cluster.messagesBackend().flush();
            checkGlobalVersion(cluster, guid, 78L);

            cluster.messagesBackend().delete(
                new StringPrefix(guid),
                "\"id\":\"contact_global_version@" + guid + "\"");

            cluster.messagesBackend().flush();
            checkGlobalVersion(cluster, guid, null);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 78L + "}}"),
                    "{\"status\":\"ok\",\"data\":[]}"));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 78L);


            // no global version - max contacts version from meta_api

            cluster.addUser(contact1Guid, "Дмитрий");
            cluster.addUser(contact2Guid, "Иван");
            cluster.addContact(guid, contact1Guid, "Дмитрий", 79L, 78L);
            cluster.addContact(guid, contact2Guid, "Иван", 80L, 79L);

            cluster.messagesBackend().flush();
            checkGlobalVersion(cluster, guid, 80L);

            cluster.messagesBackend().delete(
                new StringPrefix(guid),
                "\"id\":\"contact_global_version@" + guid + "\"");

            cluster.messagesBackend().flush();
            checkGlobalVersion(cluster, guid, null);

            list = new JsonList(BasicContainerFactory.INSTANCE);
            contact1.put("deleted", JsonBoolean.valueOf(false));
            contact2.put("deleted", JsonBoolean.valueOf(false));
            contact3.put("deleted", JsonBoolean.valueOf(false));
            list.add(contact1);
            list.add(contact2);
            list.add(contact3);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 80L + "}}"),
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list) + "}"));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, cluster.malo().port(), indexUri);
            checkGlobalVersion(cluster, guid, 120L);


            // version param does not affect the global version

            list = new JsonList(BasicContainerFactory.INSTANCE);
            contact3.put("deleted", JsonBoolean.valueOf(true));
            list.add(contact1);
            list.add(contact2);
            list.add(contact3);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 123L + "}}"),
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list) + "}"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.malo().port(),
                indexUri + "&contacts-version=123");
            checkGlobalVersion(cluster, guid, 55L);


            cluster.messagesBackend().delete(
                new StringPrefix(guid),
                "\"id\":\"contact_global_version@" + guid + "\"");

            cluster.messagesBackend().flush();
            checkGlobalVersion(cluster, guid, null);

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 123L + "}}"),
                    "{\"status\":\"ok\",\"data\":[]}"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.malo().port(),
                indexUri + "&contacts-version=123");
            checkGlobalVersion(cluster, guid, null);


            // reindex, empty contacts, global version 0

            cluster.metaApi().add(
                "/meta_api/",
                new ExpectingHttpItem(
                    new StringChecker("request={\"method\":\"get_contacts\"," +
                        "\"params\":{\"guid\":\"" + guid + "\"," +
                        "\"version\":" + 55L + "}}"),
                    "{\"status\":\"ok\",\"data\":[]}"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.malo().port(),
                indexUri + "&reindex=true");
            checkGlobalVersion(cluster, guid, 0L);


            // test multipart

            String guid1 = "374682ud-af0a-469e-b5d9-668b270c6ba2";
            String guid2 = "64ehdkd6-af0a-469e-b5d9-668b270c6ba2";

            checkGlobalVersion(cluster, guid1, null);
            checkGlobalVersion(cluster, guid2, null);

            JsonList list1 = new JsonList(BasicContainerFactory.INSTANCE);
            contact1.put("deleted", JsonBoolean.valueOf(false));
            contact2.put("deleted", JsonBoolean.valueOf(false));
            contact3.put("deleted", JsonBoolean.valueOf(true));
            list1.add(contact1);
            list1.add(contact2);
            list1.add(contact3);

            cluster.metaApi().add(
                "/meta_api/",
                new StaticHttpItem(
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list1) + "}"),
                new StaticHttpItem(
                    "{\"status\":\"ok\",\"data\":"
                        + JsonType.NORMAL.toString(list1) + "}"));

            String indexMultiUri = "/index-contacts?&partition=20&offset=1&seqNo=1"
                + "&topic=rt3.messenger--mssngr--prod-search-contacts";

            TDocument document1 = TDocument.newBuilder().setUuid(guid1).build();
            TDocument document2 = TDocument.newBuilder().setUuid(guid2).build();

            HttpPost indexPost =
                new HttpPost(cluster.malo().host() + indexMultiUri);
            indexPost.setEntity(
                multipartEntity(
                    indexMultiUri,
                    document1,
                    document2));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, indexPost);

            checkGlobalVersion(cluster, guid1, 55L);
            checkGlobalVersion(cluster, guid2, 55L);
        }
    }

    @Test
    public void testExctractUrls() throws Exception {
        Set<String> set = MessengerMessageHandler.parseUrls(
            "vasya привет это http://ya.ru\n\nsdafa\nhttps://kolbasi.net/sovsem");

        System.out.println(set);
        Assert.assertEquals(2, set.size());
    }

    @Test
    public void testChatInfoUpdate() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            Message.TMessageInfoResponse.Builder builder =
                Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                MaloTest.class.getResourceAsStream(
                    "default_text_message.json"),
                builder);


            String chatId = "4416df53-8761-441a-aa73-54e37fac6d09_f61590b6-4076-49f7-bfd0-74dace3d8850";
            long ts = 1588748172324037L;
            cluster.addMessage(builder.build(), chatId, ts);

            String uri = "/search?&text=id:chat_" + chatId + "&get=*&hr&prefix=0";
            String expected = "{\n" +
                "    \"hitsCount\": 1,\n" +
                "    \"hitsArray\": [\n" +
                "        {\n" +
                "            \"id\": \"chat_" + chatId + "\",\n" +
                "            \"chat_id\": \"" + chatId + "\",\n" +
                "            \"chat_last_message_timestamp\": \"" + ts + "\",\n" +
                "            \"chat_members\": \"4416df53-8761-441a-aa73-54e37fac6d09\nf61590b6-4076-49f7-bfd0-74dace3d8850\",\n" +
                "            \"chat_message_count\": \"85\",\n" +
                "            \"chat_second_member_timestamp\": \"" + ts + "\",\n" +
                "            \"chat_namespace\": \"0\",\n" +
                "            \"type\": \"chat\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
            cluster.chatsBackend().checkSearch(uri, new JsonChecker(expected));
            cluster.addMessage(builder.build(), chatId, ts);
            cluster.chatsBackend().checkSearch(uri, new JsonChecker(expected));
        }
    }

    @Test
    public void testForwardedMessage() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            Message.TMessageInfoResponse.Builder builder =
                Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                MaloTest.class.getResourceAsStream(
                    "forwarded_message.json"),
                builder);
            String chatId = "4bd919b2-2d7f-4be6-8ef7-d7a3e60362b8_55324d0b-c320-484a-835e-3e17fb5ebaa8";
            long ts = 1638803950491033L;
            cluster.addMessage(builder.build(), chatId, ts);

            cluster.messagesBackend().flush();
            cluster.chatsBackend().flush();

            cluster.messagesBackend().checkSearch(
                "/search?&text=id:*&sort=message_hid&asc&get=id,message_forwarded,message_hid,message_links" +
                    "&hr&prefix=" + chatId,
                new JsonChecker("{\n" +
                    "    \"hitsCount\": 2,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"id\": \"message_" + chatId + "/" + ts + "\",\n" +
                    "            \"message_hid\": \"0\",\n" +
                    "            \"message_links\": null,\n" +
                    "            \"message_forwarded\": \"false\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"id\": \"message_" + chatId + "/" + ts + "/1\",\n" +
                    "            \"message_hid\": \"1\",\n" +
                    "            \"message_links\": \"https://pikabu.ru/story/obnaruzhen_sharik_morozhenogo_vesom_5_kilo_8665468\",\n" +
                    "            \"message_forwarded\": \"true\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"));

            Message.TMessageInfoResponse.Builder builderReplied =
                Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                MaloTest.class.getResourceAsStream(
                    "replied_message.json"),
                builderReplied);
            String chatId2 = "4bd919b2-2d7f-4be6-8ef7-d7a3e60362b8_77824d0b-c320-484a-835e-3e17fb5ebaa8";
            long ts2 = 1638804248428033L;
            cluster.addMessage(builderReplied.build(), chatId2, ts2);

            cluster.messagesBackend().flush();
            cluster.chatsBackend().flush();

            cluster.messagesBackend().checkSearch(
                "/search?&text=id:*&sort=message_hid&asc&get=id,message_forwarded,message_hid&hr&prefix=" + chatId2,
                new JsonChecker("{\n" +
                    "    \"hitsCount\": 2,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"id\": \"message_" + chatId2 + "/" + ts2 + "\",\n" +
                    "            \"message_hid\": \"0\",\n" +
                    "            \"message_forwarded\": \"false\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"id\": \"message_" + chatId2 + "/" + ts2 + "/1\",\n" +
                    "            \"message_hid\": \"1\",\n" +
                    "            \"message_forwarded\": \"false\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"));
        }
    }

    @Test
    public void testLinksMessage() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            cluster.rca().add(
                "/urls?*",
                new StaticHttpResource(HttpStatus.SC_OK, new FileEntity(
                    new File(getClass().getResource("rca.json").toURI()),
                    ContentType.APPLICATION_JSON)));

            String chatId = "1b04a2d0-5000-4600-b23b-f5ac5ad9e754_4416df53-8761-441a-aa73-54e37fac6d09";
            long ts = 1599064592266011L;

            Message.TMessageInfoResponse.Builder builder = Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                MaloTest.class.getResourceAsStream(
                    "links_message.json"),
                builder);

            cluster.addMessage(builder.build(), chatId, ts);

            cluster.chatsBackend().flush();
            cluster.messagesBackend().flush();

            cluster.messagesBackend().checkSearch(
                "/search?&text=id:*&get=message_links&hr&prefix=" + chatId,
                new JsonChecker("{\n" +
                    "    \"hitsCount\": 1,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"message_links\": \"http://ya.ru\nhttps://normlink.ru\n" +
                                    "http://google.com\nhttp://www.ozon.ru\nhttp://rw.by\n" +
                                    "https://wildberries.ru\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}"));

            // links_to_extract_max = 5 (default), but links size = 6
            Assert.assertEquals(5, cluster.rca().accessCount());
        }
    }

    @Test
    public void testPollMessage() throws Exception {
        try (MaloCluster cluster = new MaloCluster(this)) {
            String chatId = "0/0/4e3fcf75-4219-428d-9891-1b3bc242b6b2";
            long ts = 1599064592266011L;

            Message.TMessageInfoResponse.Builder builder = Message.TMessageInfoResponse.newBuilder();
            new JsonFormat().merge(
                MaloTest.class.getResourceAsStream(
                    "poll_message.json"),
                builder);

            cluster.addMessage(builder.build(), chatId, ts);

            cluster.chatsBackend().flush();
            cluster.messagesBackend().flush();

            cluster.messagesBackend().checkSearch(
                "/search?&text=id:*&get=message_poll_title,message_poll_answers,message_text,message_id&hr&prefix=" + chatId,
                new JsonChecker("{\n" +
                    "    \"hitsCount\": 1,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"message_id\": \"0/0/4e3fcf75-4219-428d-9891-1b3bc242b6b2/1637055442426010\"\n," +
                    "            \"message_poll_answers\": \"Juice\nMilk\nWater\nTea\nCoffee\"\n," +
                    "            \"message_poll_title\": \"Your favorite drink:\"\n," +
                    "            \"message_text\": null" +
                    "        }\n" +
                    "    ]\n" +
                    "}"));
        }
    }

    private HttpEntity multipartEntity(
        final String uri,
        final TDocument... documents)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMimeSubtype("mixed");
        for (TDocument document: documents) {
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.ZOO_SHARD_ID, String.valueOf(0))
                    .addField(YandexHeaders.URI, uri)
                    .setBody(new ByteArrayBody(
                        document.toByteArray(),
                        ContentType.APPLICATION_JSON,
                        null))
                    .setName("name")
                    .build());
        }
        return builder.build();
    }

    private void checkGlobalVersion(
        final MaloCluster cluster,
        final String guid,
        final Long version)
        throws Exception
    {
        cluster.messagesBackend().flush();
        String expected;
        if (version != null) {
            expected = "{"
                + "\"hitsCount\":1,"
                + "\"hitsArray\":["
                    + "{\"id\":\"contact_global_version@" + guid + "\","
                    + "\"contact_global_version\":\"" + version + "\"}"
                + "]}";
        } else {
            expected = "{\"hitsCount\":0,\"hitsArray\":[]}";
        }
        cluster.messagesBackend().checkSearch(
            "/search?text=id:contact_global_version@*&get=*&prefix=" + guid,
            expected);
    }

    private String user(final String guid, final String orgId, final String args) {
        return "\"id\":\"user_" + guid + '@' + orgId + "\"," +
            "\"user_id\": \"" + guid + "\"," +
            "\"type\": \"user\"," +
            args;
    }
}

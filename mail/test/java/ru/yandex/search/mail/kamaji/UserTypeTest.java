package ru.yandex.search.mail.kamaji;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.search.mail.kamaji.usertype.UserTypeConfigBuilder;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;

public class UserTypeTest extends KamajiTestBase {
    private static final String SALES = "sales";

    private static final String UID = "203889311";
    private static final String TO = "myuser@yandex.ru";
    private static final String SEARCH_PREFIX = "/search?prefix=";

    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=pg";

    private static final String FILTER_SEARCH =
        "/filter_search?order=default&full_folders_and_labels=1&uid=";

    // CSOFF: ParameterNumber
    private static String typeRequest(
        final String opid,
        final String lcn,
        final String email,
        final String userType)
    {
        return "{\"operation_id\""
            + ": \"" + opid + "\",\"uid\": \"" + UID + "\", \"lcn\": \""
            + lcn + "\",\"change_type\": \"user_type_update\","
            + "    \"operation_date\": \"1436951916.326374\","
            + "    \"hdr_from\":\"" + email + "\",\"userType\":\""
            + userType + "\"}";
    }
    // CSON: ParameterNumber

    @Test
    public void testBadFrom() throws Exception {
        KamajiConfigBuilder config = new KamajiConfigBuilder();
        config.userTypeConfig(
            new UserTypeConfigBuilder()
                .enabled(true)
                .reindexBatchSize(2));
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.kamaji().start();

            final long uid = 303889311;
            // first index user types

            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, TO));

            String store1 = "{\"operation_id\": \"250\","
                + "    \"uid\": \"303889311\","
                + "    \"lcn\": \"57\",\"change_type\":\"store\","
                + "    \"operation_date\": \"14387010955.326374\","
                + "    \"changed\": [{\"mid\": 261566636631916880}]}";
            String envelopeBase =
                "{\"envelopes\":[{\"fid\":1,\"folder\":"
                    + "{\"type\":{\"title\":\"user\"},\"name\":\"fld\"},\""
                    + "to\":[{\"displayName\":\"Other User\","
                    + "\"domain\":\"yandex.ru\",\"local\":\"otheruser\"}],";
            String envelope1 = envelopeBase
                + "\"stid\":\"1.20.11\",\"receiveDate\":14387010955,"
                + "\"from\":[{\"displayName\":\"One^\\\"two\","
                + "\"domain\":\"one.com^M\",\"local\":\"hot\\\"^&el\"}],"
                + "\"mid\":261566636631916880"
                + ",\"threadId\":261566636631916880}]}";
            HttpPost post1 = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post1.setEntity(new StringEntity(store1));

            cluster.filterSearch().add(
                FILTER_SEARCH + uid
                    + "&mdb=pg&suid=90000&lcn=57&operation-id=250"
                    + "&mids=261566636631916880",
                envelope1);

            cluster.tikaite().add(
                "/mail/handler?json-type=dollar&stid=1.20.11",
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"TTTText\"}]}");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post1);
        }
    }

    @Test
    public void testUserTypeOnIndex() throws Exception {
        KamajiConfigBuilder config = new KamajiConfigBuilder();
        config.userTypeConfig(
            new UserTypeConfigBuilder()
                .enabled(true)
                .reindexBatchSize(2));
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene, config);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.kamaji().start();

            final long uid = 203889311;
            // first index user types
            HttpPost utPost1 = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            utPost1.setEntity(
                new StringEntity(
                    typeRequest("111", "2222", "wine@onetwotrip.com", SALES)));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, utPost1);

            String allHotels = "allhotels@onetwotrip.com";
            HttpPost utPost2 = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            utPost2.setEntity(
                new StringEntity(
                    typeRequest("112", "2223", allHotels, "other")));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, utPost2);

            HttpPost utPost3 = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            utPost3.setEntity(
                new StringEntity(
                    typeRequest("113", "2224", "trappytrips@yandex.ru", "trips")
                ));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, utPost3);

            final int hitCount = 3;
            lucene.checkSearch(
                "/search?prefix=203889311"
                    + "&text=url:usrtype_*&get=url,user_types",
                TestSearchBackend.prepareResult(
                    hitCount,
                    "\"url\":\"usrtype_from_203889311_wine@onetwotrip.com\","
                        + "\"user_types\":\"sales\"",
                    "\"url\":"
                        + "\"usrtype_from_203889311_allhotels@onetwotrip.com\","
                        + "\"user_types\":\"other\"",
                    "\"url\":\"usrtype_from_203889311_trappytrips@yandex.ru\","
                        + "\"user_types\":\"trips\""));

            String bbUri = blackboxUri(UID_PARAM + uid);
            cluster.blackbox().add(
                bbUri,
                blackboxResponse(uid, TO));

            String store1 = "{\"operation_id\": \"150\","
                + "    \"uid\": \"203889311\",\"lcn\": \"47\","
                + "    \"change_type\":\"store\","
                + "    \"operation_date\": \"1436952904.326374\","
                + "    \"changed\": [{\"mid\": 161566636631916880}]}";
            String envelopeBase =
                "{\"envelopes\":["
                    + "{\"fid\":1,\"folder\":{\"type\":{\"title\":\"user\"}"
                    + ",\"name\":\"fld\"},\""
                    + "to\":[{\"displayName\":\"My User\","
                    + "\"domain\":\"ya.ru\",\"local\":\"myuser\"}],";

            String envelope1 = envelopeBase
                + "\"stid\":\"1.10.11\",\"receiveDate\":14387010855,"
                + "\"from\":[{\"displayName\":\"OneTwoTrip\","
                + "\"domain\":\"onetwotrip.com\",\"local\":\"allhotels\"}],"
                + "\"mid\":161566636631916880"
                + ",\"threadId\":161566636631916880}]}";
            String store2 = "{\"operation_id\": \"151\","
                + "    \"uid\": \"203889311\",\"lcn\": \"48\","
                + "    \"change_type\": \"store\","
                + "    \"operation_date\": \"1436952905.326374\","
                + "    \"changed\": [{\"mid\": 161566636631916881}]}";
            String envelope2 = envelopeBase
                + "\"stid\":\"1.10.12\",\"receiveDate\":14387010856,"
                + "\"from\":[{\"displayName\":\"Trap\","
                + "\"domain\":\"yandex.ru\","
                + "\"local\":\"trappytrips\"}],\"mid\":161566636631916881"
                + ",\"threadId\":161566636631916881}]}";
            HttpPost post1 = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post1.setEntity(new StringEntity(store1));

            HttpPost post2 = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY);
            post2.setEntity(new StringEntity(store2));

            cluster.filterSearch().add(
                FILTER_SEARCH + uid
                    + "&mdb=pg&suid=90000&lcn=47&operation-id=150"
                    + "&mids=161566636631916880",
                envelope1);

            cluster.filterSearch().add(
                FILTER_SEARCH + uid
                    + "&mdb=pg&suid=90000&lcn=48&operation-id=151"
                    + "&mids=161566636631916881",
                envelope2);

            cluster.tikaite().add(
                "/mail/handler?json-type=dollar&stid=1.10.11",
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"Text\"}]}");

            cluster.tikaite().add(
                "/mail/handler?json-type=dollar&stid=1.10.12",
                "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"Simple text\"}]}");

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post1);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post2);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post1);

            final String searchPrefix = SEARCH_PREFIX + uid;
            //Check that user type saved
            lucene.checkSearch(
                searchPrefix
                    + "&text=mid_p:161566636631916880"
                    + "&length=10&get=user_type,url&sort=url",
                TestSearchBackend.prepareResult(
                    2,
                    "\"url\": \"203889311_161566636631916880/1\",\""
                        + "user_type\":\"other\"",
                    "\"url\": \"203889311_161566636631916880/0\","
                        + "\"user_type\":\"other\""
                ));

            lucene.checkSearch(
                searchPrefix
                    + "&text=mid_p:161566636631916881&length=10&get"
                    + "=user_type,url&sort=url",
                TestSearchBackend.prepareResult(
                    2,
                    "\"url\": \"203889311_161566636631916881/1\",\""
                        + "user_type\":\"trips\"",
                    "\"url\": \"203889311_161566636631916881/0\","
                        + "\"user_type\":\"trips\""));
        }
    }
}

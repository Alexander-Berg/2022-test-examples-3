package ru.yandex.search.mail.kamaji;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.parser.email.types.MessageType;
import ru.yandex.parser.mail.senders.SenderType;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;

public class KamajiSendersTest extends KamajiTestBase {
    private static final long UID = 9007L;

    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    private void blackbox(
        final KamajiCluster cluster,
        final String... emails)
    {
        cluster.blackbox().add(
            blackboxUri(UID_PARAM + UID),
            blackboxResponse(UID, emails));
    }

    private void filterSearch(
        final KamajiCluster cluster,
        final FilterSearchResponseBuilder fsrb)
    {
        String fsUri =
            FILTER_SEARCH + UID
                + "&mdb=pg&suid=90000&lcn=" + fsrb.lcn()
                + "&operation-id=2&mids=" + fsrb.mid();
        cluster.filterSearch().add(
            fsUri,
            fsrb.toString());
    }

    //CSOFF: ParameterNumber
    private HttpPost notify(
        final KamajiCluster cluster,
        final String lcn,
        final String queueId,
        final String chType,
        final String mid)
        throws IOException
    {
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.kamaji().port() + NOTIFY
            + QUEUE_ID_P + queueId);
        post.addHeader(YandexHeaders.ZOO_QUEUE_ID, queueId);
        post.setEntity(
            new StringEntity(
                "{\"lcn\":" + lcn + ",\"operation_id\":2,\"uid\":" + UID
                    + ",\"operation_date\": \"1436810748.043102\",\""
                    + "change_type\":\"" + chType + "\",\"changed\":[{\"mid\":"
                    + mid + "}]}"));
        return post;
    }
    //CSON: ParameterNumber

    private String checkUri(final String email, final SenderType senderType) {
        return "/search?prefix=" + UID
                + "&text=url:" + senderType.sendersPrefix() + UID + '_' + email
                + "&get=*,-url,-senders_uid&sort=url";
    }

    private void checkCount(
        final TestSearchBackend lucene,
        final int expected)
        throws Exception
    {
        String uri = "/search?prefix=" + UID
            + "&text=url:senders_uid_" + UID + '_' + '*'
            + "&get=*&length=0";

        lucene.checkSearch(
            uri,
            new JsonChecker(
                "{\"hitsCount\":" + expected + ",\"hitsArray\":[]}"));
    }

    private void checkSenders(
        final TestSearchBackend lucene,
        final String email,
        final SenderType senderType,
        final String... docs)
        throws Exception
    {
        lucene.checkSearch(
            checkUri(email, senderType),
            new JsonChecker(TestSearchBackend.prepareResult(docs)));
    }

    // CSOFF: MethodLength
    @Test
    public void testSenders() throws Exception {
        FilterSearchResponseBuilder fs = new FilterSearchResponseBuilder();
        fs = fs.stid("1.stid").lcn("10")
            .folder("1", "system", "Inbox")
            .types(MessageType.PEOPLE, MessageType.S_TRAVEL);
        try (TestSearchBackend lucene = new TestMailSearchBackend(this);
             KamajiCluster cluster = new KamajiCluster(lucene);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.kamaji().start();

            final String user1 = "spammer@hotmail.com";
            final String mid18 = "18";
            FilterSearchResponseBuilder cfs =
                fs.receivedDate("1234567892")
                    .mid(mid18)
                    .lcn("1")
                    .from("spammer", "spam4u", "hotmail.com")
                    .setTo("user", "", "yandex.ru");

            filterSearch(cluster, cfs);
            blackbox(cluster, "user@yandex.ru");
            cluster.tikaite().add(
                "/mail/handler?json-type=dollar&stid=1.stid",
                "{\"docs\":[{\"hid\":\"1\",\"pure_body\":\"hello\",\"headers\""
                + ":\"x-beenthere: announce@Lists.adacore.com\n"
                + "x-yandex-header: yandex-header\n"
                + "list-owner: <mailto:pro@Subscribe.ru>\"}]}");

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "1", "45683", "store", mid18));

            // Check all sender types from this mail
            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567892\","
                    + "\"senders_lcn\":\"1\",\"senders_names\":\"spam4u\","
                    + "\"senders_mail_types\":\"people\t1\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t1\","
                    + "\"senders_received_count\":\"1\"");

            checkSenders(
                lucene,
                "announce@lists.adacore.com",
                SenderType.X_BEENTHERE,
                "\"senders_last_contacted\":\"1234567892\","
                    + "\"senders_lcn\":\"1\","
                    + "\"senders_mail_types\":\"people\t1\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"x_beenthere\","
                    + "\"senders_store_folders\":\"inbox\t1\","
                    + "\"senders_received_count\":\"1\"");

            checkSenders(
                lucene,
                "pro@subscribe.ru",
                SenderType.LIST_OWNER,
                "\"senders_last_contacted\":\"1234567892\","
                    + "\"senders_lcn\":\"1\","
                    + "\"senders_mail_types\":\"people\t1\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"list_owner\","
                    + "\"senders_store_folders\":\"inbox\t1\","
                    + "\"senders_received_count\":\"1\"");

            // Check that there no other senders counted
            checkSenders(
                lucene,
                "*",
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567892\","
                    + "\"senders_lcn\":\"1\",\"senders_names\":\"spam4u\","
                    + "\"senders_mail_types\":\"people\t1\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t1\","
                    + "\"senders_received_count\":\"1\"");

            checkSenders(
                lucene,
                "*",
                SenderType.X_BEENTHERE,
                "\"senders_last_contacted\":\"1234567892\","
                    + "\"senders_lcn\":\"1\","
                    + "\"senders_mail_types\":\"people\t1\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"x_beenthere\","
                    + "\"senders_store_folders\":\"inbox\t1\","
                    + "\"senders_received_count\":\"1\"");

            checkSenders(
                lucene,
                "*",
                SenderType.LIST_OWNER,
                "\"senders_last_contacted\":\"1234567892\","
                    + "\"senders_lcn\":\"1\","
                    + "\"senders_mail_types\":\"people\t1\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"list_owner\","
                    + "\"senders_store_folders\":\"inbox\t1\","
                    + "\"senders_received_count\":\"1\"");

            String mid17 = "17";
            cfs = cfs.receivedDate("1234567889").mid(mid17).lcn("3");
            filterSearch(cluster, cfs);
            // last contacted won't be changed, because of 'max'
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "3", "45684", "store", mid17));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567892\",\""
                    + "senders_lcn\":\"3\",\"senders_names\":\"spam4u\","
                    + "\"senders_mail_types\":\"people\t2\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t2\","
                    + "\"senders_received_count\":\"2\"");

            // It's time to increment last contacted and add new name
            String mid16 = "16";
            cfs = cfs
                .from("spammer", "spam for you", "hotmail.com")
                .receivedDate("1234567893")
                .lcn("4")
                .types(MessageType.NEWS)
                .mid(mid16);

            filterSearch(cluster, cfs);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "4", "45685", "store", mid16));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567893\",\""
                    + "senders_lcn\":\"4\",\"senders_names\":"
                    + "\"spam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t1\npeople\t2\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t3\","
                    + "\"senders_received_count\":\"3\"");

            // increment sent count
            String mid14 = "14";
            cfs = fs.lcn("5").mid(mid14).receivedDate("1234567895")
                .setTo("spammer", "dear spammer", "hotmail.com")
                .folder("4", "system", "Sent")
                .from("user", "", "yandex.ru");

            filterSearch(cluster, cfs);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "5", "45686", "store", mid14));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567895\",\""
                    + "senders_lcn\":\"5\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t1\npeople\t2\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t3\","
                    + "\"senders_received_count\":\"3\","
                    + "\"senders_sent_count\":\"1\"");
            checkCount(lucene, 1);

            // increment two at once

            String mid13 = "13";
            cfs = fs.lcn("6").mid(mid13)
                .receivedDate("1234567900")
                .folder("4", "system", "Sent")
                .setTo("spammer", "dear spammer", "hotmail.com")
                .addTo("second", "The Second", "yandex.ru")
                .addTo("usercopy", "Me", "yandex.ru")
                .from("user", "", "yandex.ru");
            blackbox(cluster, "user@yandex.ru", "usercopy@yandex.ru");

            filterSearch(cluster, cfs);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "6", "45687", "store", mid13));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"6\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t1\npeople\t2\","
                    + "\"senders_mail_type\":\"people\","
                    + "\"senders_message_types\":\"13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t3\","
                    + "\"senders_received_count\":\"3\","
                    + "\"senders_sent_count\":\"2\"");
            checkSenders(
                lucene,
                "second@yandex.ru",
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"6\",\"senders_names\":\"The Second\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_sent_count\":\"1\"");
            checkCount(lucene, 2);

            cfs =
                fs.receivedDate("1234567900")
                    .from("spammer", "spam4u", "hotmail.com")
                    .setTo("user", "", "yandex.ru")
                    .types(MessageType.NEWS, MessageType.S_EVENT);

            filterSearch(cluster, cfs.mid("12").lcn("7"));
            blackbox(cluster, "user@yandex.ru");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "7", "45688", "store", "12"));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"7\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t2\npeople\t2\","
                    + "\"senders_mail_type\":\"\","
                    + "\"senders_message_types\":\"48\n13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t4\","
                    + "\"senders_received_count\":\"4\","
                    + "\"senders_sent_count\":\"2\"");

            cfs = cfs.folder("15", "user", "FromSpammer");
            filterSearch(cluster, cfs.mid("11").lcn("8"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "8", "45689", "store", "11"));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"8\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t3\npeople\t2\","
                    + "\"senders_mail_type\":\"news\","
                    + "\"senders_message_types\":\"48\n13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t4\nuser\t1\","
                    + "\"senders_received_count\":\"5\","
                    + "\"senders_sent_count\":\"2\"");

            // check senders update
            filterSearch(cluster, cfs.mid("10").lcn("9"));
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port()
                    + NOTIFY + "&zoo-queue-id=45690");
            post.addHeader(YandexHeaders.ZOO_QUEUE, CHANGE_LOG_QUEUE);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, Long.toString(UID));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, "45690");
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\":2,\"lcn\":\"9\",\"uid\":9007,\"op"
                        + "eration_date\": \"1436810749.043123\","
                        + "\"change_type\":\"update\""
                        + ",\"changed\":["
                        + "{\"mid\":10}],\"arguments\":{\"seen\":true}}"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"9\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t3\npeople\t2\","
                    + "\"senders_mail_type\":\"news\","
                    + "\"senders_message_types\":\"48\n13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"inbox\t4\nuser\t1\","
                    + "\"senders_from_read_count\":\"1\","
                    + "\"senders_received_count\":\"5\","
                    + "\"senders_sent_count\":\"2\"");

            cfs = cfs.folder("2", "system", "Spam");
            filterSearch(cluster, cfs.mid("9").lcn("10"));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                notify(cluster, "10", "45691", "store", "9"));

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"10\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t4\npeople\t2\","
                    + "\"senders_mail_type\":\"news\","
                    + "\"senders_message_types\":\"48\n13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"spam\t1\ninbox\t4\nuser\t1"
                    + "\",\"senders_from_read_count\":\"1\","
                    // Received count won't be incremented, because it is spam
                    + "\"senders_received_count\":\"5\","
                    + "\"senders_sent_count\":\"2\"");

            filterSearch(cluster, cfs.mid("9").lcn("11"));
            post = new HttpPost(
                HTTP_LOCALHOST + cluster.kamaji().port()
                    + NOTIFY + "&zoo-queue-id=45692");
            post.addHeader(YandexHeaders.ZOO_QUEUE, CHANGE_LOG_QUEUE);
            post.addHeader(YandexHeaders.ZOO_SHARD_ID, Long.toString(UID));
            post.setHeader(YandexHeaders.ZOO_QUEUE_ID, "45692");
            post.setEntity(
                new StringEntity(
                    "{\"operation_id\":2,\"lcn\":\"11\",\"uid\":9007,"
                    + "\"operation_date\": \"1436810749.043123\","
                    + "\"change_type\":\"update\""
                    + ",\"changed\":["
                    + "{\"mid\":9}],\"arguments\":{\"seen\":true}}"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            checkSenders(
                lucene,
                user1,
                SenderType.FROM,
                "\"senders_last_contacted\":\"1234567900\",\""
                    + "senders_lcn\":\"11\",\"senders_names\":\""
                    + "dear spammer\nspam for you\nspam4u\","
                    + "\"senders_mail_types\":\"news\t4\npeople\t2\","
                    + "\"senders_mail_type\":\"news\","
                    + "\"senders_message_types\":\"48\n13\n4\n19\","
                    + "\"senders_sender_type\":\"from\","
                    + "\"senders_store_folders\":\"spam\t1\ninbox\t4\nuser\t1"
                    // Read count won't be incremented for spam mail
                    + "\",\"senders_from_read_count\":\"1\","
                    + "\"senders_received_count\":\"5\","
                    + "\"senders_sent_count\":\"2\"");

        }
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber
    // CSON: MethodLength
}

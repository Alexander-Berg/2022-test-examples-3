package ru.yandex.iex.proxy;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.client.so.shingler.config.ShinglerType;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.iex.proxy.complaints.Route;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.util.JsonChecker;

public class MoveHandlerTest extends MoveHandlerTestBase {
    private static final String MID_2 = Long.toString(MID0 + 1);
    private static final String MID_3 = Long.toString(MID0 + 2);

    @Override
    protected void mockStoreMsg() throws URISyntaxException {
        String stidPrefix = "320.mail:588355978.E764924:1937658048648101342";
        for (int i = 0; i < 5; i++) {
            cluster.storageCluster().put(stidPrefix + (63421668835L + i),
                new File(getClass().getResource(SAMPLE_EML).toURI()));
        }
    }

    @Test
    public void testOneMovedFromSpam() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp&uid="
                + UID,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(UID), EMAIL, SUID))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp&login="
                + EMAIL,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(UID), EMAIL, SUID))));
        filterSearchMock(STOREFS_SPAM_JSON, MID);
        storeMsgs(FACTS_EMPTY_JSON, SEARCH_SPAM_JSON, true, MID);
        final Map<ShinglerType, String> shinglersQueries = mockShinglersFromSolog(UID, QUEUEID, Route.IN, 1);

        // move msg from spam
        filterSearchMock(STOREFS_JSON, MID);
        String changed = '{'
            + MID_PARAM + MID + ','
            + FID_PARAM + '1' + ','
            + SRC_FID_PARAM + '2'    // spam folder
            + '}';
        sendNotify(changed, MOVE);
        waitAndCheckFacts(PATH + FACTS_JSON, MID);
        waitAndCheckSearch(PATH + SEARCH_JSON);
        waitAndCheckAbusesFromLucene(UID, Map.of(
            "type", HAM,
            "first_timestamp", 0,
            "last_timestamp", TIMESTAMP,
            "msg_date", MSG_DATE,
            "source", USER,
            "cnt", "1"
        ), HAM, STID);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID, 1);
        waitShinglersRequests(shinglersQueries, Route.IN, 1, false);
    }

    @Test
    public void testMovedNotFromSpam() throws Exception {
        filterSearchMock(STOREFS_SPAM_JSON, MID);
        storeMsgs(FACTS_EMPTY_JSON, SEARCH_SPAM_JSON, true, MID);
        final Map<ShinglerType, String> shinglersQueries = mockShinglersFromSolog(UID, QUEUEID, Route.IN, 1);

        // move msg from trash
        filterSearchMock(STOREFS_JSON, MID);
        String changed = "{"
            + MID_PARAM + MID + ','
            + FID_PARAM + '1' + ','
            + SRC_FID_PARAM + '3'   // trash folder
            + '}';
        sendNotify(changed, MOVE);
        waitAndCheckFacts(PATH + FACTS_JSON, MID);
        waitAndCheckSearch(PATH + SEARCH_JSON);
        waitAndCheckAbusesFromLucene(UID, Map.of(
            "type", HAM,
            "first_timestamp", 0,
            "last_timestamp", TIMESTAMP,
            "msg_date", MSG_DATE,
            "source", USER,
            "cnt", "1"
        ), HAM, STID);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID, 1);
        waitShinglersRequests(shinglersQueries, Route.IN, 1, false);
    }

    @Test
    public void testFewMovedFromSpam() throws Exception {
        filterSearchMock("storefs_spam_few_mids_1_2.json", MID, MID_2);
        filterSearchMock("storefs_spam_few_mids_3.json", MID_3);
        filterSearchMock(
            List.of("storefs_spam_few_mids_1_2.json", "storefs_spam_few_mids_3.json"),
            MID,
            MID_2,
            MID_3);
        storeMsgs(
            FACTS_EMPTY_FEW_JSON,
            SEARCH_SPAM_FEW_JSON,
            true,
            MID,
            MID_2,
            MID_3);

        // move msg from spam
        filterSearchMock("storefs_few_mids_1_3.json", MID, MID_3);
        filterSearchMock("storefs_few_mids_1_3.json", MID_3, MID);
        String changed = '{'
            + MID_PARAM + MID + ','
            + FID_PARAM + '1' + ','
            + SRC_FID_PARAM + "2},"     // spam folder
            + '{'
            + MID_PARAM + MID_2 + ','
            + FID_PARAM + '1' + ','
            + SRC_FID_PARAM + "4},"     // sent folder
            + '{'
            + MID_PARAM + MID_3 + ','
            + FID_PARAM + '1' + ','
            + SRC_FID_PARAM + "2}";     // spam folder
        sendNotify(changed, MOVE);
        testTrustedComplaintSignals(0, 0, 1, 0);
        waitAndCheckFacts(PATH + FACTS_FEW_JSON, MID, MID_2, MID_3);
        waitAndCheckSearch(PATH + SEARCH_FEW_JSON);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 2);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID, 1);
    }

    @Test
    public void testTrustedComplain() throws Exception {
        cluster.gettext().add("/get-text?mid=" + MID + "&uid=" + UID, "\nPONG\n");
        cluster.gettext().start();

        String mailReportUri = Files.readString(
            Path.of(getClass().getResource(PATH + MAIL_REPORT_URI).toURI()),
            Charset.defaultCharset());
        cluster.gatemail().add(mailReportUri, HttpStatus.SC_OK);
        cluster.gatemail().start();

        String neuroUri1 =
            "/score?b=-1.5&f=1.8&r=-0.17&t=-0.2&eps=0.15&hid=1.1"
            + "&word-count=54";
        cluster.neuroHards().add(
            neuroUri1 + '*',
            "{\"dist\":\"0.05\",\"score\":5}");
        String neuroUri2 =
            "/score?b=-1.5&f=1.8&r=-0.17&t=-0.2&eps=0.15&hid=1.2"
            + "&word-count=47";
        cluster.neuroHards().add(
            neuroUri2 + '*',
            "{\"dist\":\"0.15\",\"score\":4}");
        String modifyUri =
            "/modify?spam-samples&prefix=991949281&neuro-hard"
            + "&url=spam_samples_" + UID + "_neuro_hard_" + MID;
        cluster.producerAsyncClient().add(
            modifyUri,
            new StaticHttpResource(
                new ExpectingHttpItem(
                    new JsonChecker(
                        "{\"prefix\":991949281,\"docs\":["
                        + "{\"spam_sample_type\":\"neuro_hard\","
                        + "\"spam_sample_revision\":\"<any value>\","
                        + "\"spam_sample_stid\":\"" + STID
                        + "\",\"url\":\"spam_samples_" + UID
                        + "_so_neuro_hard_" + MID
                        + "\",\"spam_sample_data\":\""
                        + "{\\\"docs\\\":[{\\\"hid\\\":\\\"1.1\\\","
                        + "\\\"pure_body\\\":\\\"hello\\\"},{\\\"hid\\\":"
                        + "\\\"1.2.2\\\",\\\"pure_body\\\":\\\"world\\\"}]"
                        + "}\"}]}"),
                    HttpStatus.SC_OK)));

        filterSearchMock(STOREFS_JSON, MID);
        storeMsgs(FACTS_JSON, SEARCH_JSON, false, MID);

        // move msg to spam
        filterSearchMock(STOREFS_SPAM_JSON, MID);
        cluster.testLucene().add(
            new StringPrefix(UID),
            "\"url\" : \"so_trusted_complainer_" + UID + "\"");
        String changed = '{'
            + MID_PARAM + MID + ','
            + FID_PARAM + '2' + ','
            + SRC_FID_PARAM + '1'
            + '}';
        sendNotify(changed, MOVE, UID, MSG_DATE + SIX_HOURS - 1);
        testTrustedComplaintSignals(1, 1, 0, 1);
        waitAndCheckFacts(PATH + FACTS_JSON, MID);
        waitAndCheckSearch(PATH + SEARCH_JSON);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID, 1);
        // Should compare full uri, but there is problems with timezone
        Assert.assertEquals(
            1,
            cluster.gatemail().accessCount(
                mailReportUri.replaceAll("\\*$", "")));
        Assert.assertEquals(
            0,
            cluster.neuroHards().accessCount("/score?b="));

        // First sample is too short, so it will be ignored
        // Changed only eml (for headers)
        cluster.storageCluster().put(STID,
            new File(getClass().getResource(EML_WITH_X_YDX_SPAM_4).toURI()));
        sendNotify(changed, MOVE);
        Assert.assertEquals(
            2,
            cluster.gatemail().accessCount(
                mailReportUri.replaceAll("\\*$", "")));
        Assert.assertEquals(
            1,
            cluster.neuroHards().accessCount(neuroUri1));
        Assert.assertEquals(
            1,
            cluster.neuroHards().accessCount(neuroUri2));
        cluster.testLucene().checkSearch(
            "/search?prefix=991949281&text=spam_sample_type:neuro_hard&get=*"
            + "&sort=hid",
            new JsonChecker(
                "{\"hitsCount\":2,\"hitsArray\":["
                + "{\"spam_sample_data\":\"<any value>\","
                + "\"spam_sample_type\":\"neuro_hard\","
                + "\"spam_sample_revision\":\"10\","
                + "\"spam_sample_labels\": \"neuro_dist_0.05\nhid_1.1\","
                + "\"spam_sample_stid\":\"" + STID
                + "\",\"url\":\"spam_samples_" + UID + "_neuro_hard_"
                + MID + "_1.1"
                + "\"},{\"spam_sample_data\":\"<any value>\","
                + "\"spam_sample_type\":\"neuro_hard\","
                + "\"spam_sample_revision\":\"10\","
                + "\"spam_sample_labels\": \"neuro_dist_0.15\nhid_1.2\","
                + "\"spam_sample_stid\":\"" + STID
                + "\",\"url\":\"spam_samples_" + UID + "_neuro_hard_"
                + MID + "_1.2"
                + "\"}]}"));
    }

    @Test
    public void testPersonalFilters() throws Exception {
        receiveMail(1, 1);
        receiveMail(2, 1);
        moveMail(1, 1, true, 2);
        moveMailToTab(1, true, 2);
        waitAndCheckPfilters("\"spam\"", 2L, "1", null);
        moveMail(2, 1, true, 3);
        moveMailToTab(2, true, 3);
        waitAndCheckPfilters("\"spam\"", 3L, "2", null);
        receiveMail(3, 4);
        moveMail(3, 4, true, 4);
        moveMailToTab(3, true, 4);
        waitAndCheckPfilters("\"spam\"", 4L, "3", null);
        moveMail(3, 4, false, 5);
        waitAndCheckPfilters(null, 5L, "3", "1");
        receiveMail(4, 6);
        moveMail(4, 6, false, 7);
        waitAndCheckPfilters("\"ham\"", 7L, "3", "2");
    }

    @Test
    public void testPersonalFiltersNoFrom() throws Exception {
        filterSearchMock("storefs_reply_to.json", MID);
        sendNotify('{'
                + MID_PARAM + MID + ','
                + FID_PARAM + "1}",
            STORE, UID, 1);

        // move msg to spam
        filterSearchMock("storefs_reply_to_spam.json", MID);
        sendNotify('{'
            + MID_PARAM + MID + ','
            + FID_PARAM + "2,"
            + SRC_FID_PARAM + "1}", MOVE, UID, 2);
        // Check response
        waitAndCheckPfilters("\"spam\"", 2L, "1", null);
    }

    @Test
    public void testPersonalFiltersNoSender() throws Exception {
        filterSearchMock("storefs_no_from.json", MID);
        sendNotify('{'
                + MID_PARAM + MID + ','
                + FID_PARAM + "1}",
            STORE, UID, 1);

        // move msg from spam
        filterSearchMock("storefs_no_from_spam.json", MID);
        sendNotify('{'
            + MID_PARAM + MID + ','
            + FID_PARAM + "1,"
            + SRC_FID_PARAM + "2}", MOVE, UID, 2);

        // Check response
        waitAndCheckPfilters("\"ham\"", 2L, null, "1");
    }

    @Test
    public void testPersonalFiltersInboxToSpamToTrashToInboxToInbox()
        throws Exception
    {
        receiveMail(1, 1);
        moveMail(1, 1, INBOX_FID, SPAM_FID, 2);
        waitAndCheckPfilters("\"spam\"", 2L, "1", null);
        moveMail(1, 1, SPAM_FID, TRASH_FID, 3);
        waitAndCheckPfilters("\"spam\"", 2L, "1", null);
        moveMail(1, 1, TRASH_FID, INBOX_FID, 4);
        waitAndCheckPfilters(null, 4L, "1", "1");
        moveMail(1, 1, TRASH_FID, INBOX_FID, 5);
        waitAndCheckPfilters("\"ham\"", 5L, "1", "2");
    }

    @Test
    public void testPersonalFiltersInboxToTrashToSpam() throws Exception {
        receiveMail(1, 1);
        moveMail(1, 1, INBOX_FID, TRASH_FID, 2);
        waitAndCheckPfilters(null, null, null, null);
        moveMail(1, 1, TRASH_FID, SPAM_FID, 3);
        waitAndCheckPfilters("\"spam\"", 3L, "1", null);
    }

    @Test
    public void testPersonalFiltersInboxToSpamToHiddenTrash() throws Exception {
        receiveMail(1, 1);
        moveMail(1, 1, INBOX_FID, SPAM_FID, 2);
        waitAndCheckPfilters("\"spam\"", 2L, "1", null);
        moveMail(1, 1, SPAM_FID, HIDDEN_TRASH_FID, 3);
        waitAndCheckPfilters("\"spam\"", 2L, "1", null);
    }

    @Test
    public void testPersonalFiltersInboxToTrashToHiddenTrash() throws Exception {
        receiveMail(1, 1);
        moveMail(1, 1, INBOX_FID, TRASH_FID, 2);
        waitAndCheckPfilters(null, null, null, null);
        moveMail(1, 1, TRASH_FID, HIDDEN_TRASH_FID, 3);
        waitAndCheckPfilters(null, null, null, null);
    }

    @Test
    public void testImapMoveCases() throws Exception {
        String uri = "/notify?mdb=pg&pgshard=2685&operation-id=2586703247&operation-date=1653659896.294987"
            + "&uid=1130000023012675&change-type=move&changed-size=1&batch-size=1&salo-worker=pg2685:1"
            + "&transfer-timestamp=1653659896426&zoo-queue-id=24472357&deadline=1653659917629";
        String body = "{\"select_date\":\"1653659896.402\",\"uid\":\"1130000023012675\",\"operation_id\":\"2586703247\""
            + ",\"lcn\":\"73697\",\"db_user\":\"imap\",\"fresh_count\":\"0\",\"operation_date\":\"1653659896.294987\","
            + "\"arguments\":{\"fid\":3,\"tab\":null},\"change_type\":\"move\",\"useful_new_messages\":\"11\","
            + "\"pgshard\":\"2685\",\"changed\":[{\"fid\":3,\"src_fid\":1,\"deleted\":false,\"lids\":[],\"tab\":null,"
            + "\"mid\":179299560164710519,\"recent\":true,\"tid\":null,\"seen\":false,\"src_tab\":\"relevant\"}]}";
        sendNotifyMove(uri, body);
        testTrustedComplaintSignals(0, 0, 0, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + "1130000023012675", 0);
    }

    @Test
    public void testEmptySessionKey() throws Exception {
        String uri = "/notify-tteot?&changed-size=1&batch-size=1&uid=25152729&zoo-queue-id=22686042&pgshard=2609"
            +"&change-type=move&salo-worker=pg2609:12&transfer-timestamp=1653297875182&operation-id=2008112839&mdb=pg"
            + "&operation-date=1653297875.122026&deadline=1653297907107&service=iex_backlog&zoo-queue-id=92504"
            + "&deadline=1653661337085";
        String body = "{\"uid\":\"25152729\",\"select_date\":\"1653297875.167\",\"pgshard\":\"2609\",\"lcn\":\"45777\","
            + "\"fresh_count\":\"0\",\"operation_date\":\"1653297875.122026\",\"useful_new_messages\":\"26\","
            + "\"operation_id\":\"2008112839\",\"change_type\":\"move\",\"arguments\":{\"fid\":3,\"tab\":null},"
            + "\"db_user\":\"mops\",\"changed\":[{\"fid\":3,\"src_fid\":1,\"deleted\":false,\"tab\":null,\"lids\":[],"
            + "\"mid\":179299560164704592,\"recent\":true,\"tid\":null,\"seen\":false,\"src_tab\":\"news\"}]}";
        sendNotifyMove(uri, body);
        testTrustedComplaintSignals(0, 0, 0, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + "25152729", 0);
    }
}

package ru.yandex.iex.proxy;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import ru.yandex.client.so.shingler.config.ShinglerType;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.iex.proxy.complaints.Flags;
import ru.yandex.iex.proxy.complaints.Route;

public class ComplaintsSologgerTest extends MoveHandlerTestBase {
    private static final String LOG_EXT = ".log";
    private static final String COMPLLOG_FILENAME = "\ncomplaints.compl-log.file = ";
    private static final String UID_2 = "1130000042447770";
    private static final String UID_3 = "1130000053372955";
    private static final String STID_2 = "320.mail:0.E3445099:3218300422106153641462127296572";
    private static final String STID_3_1 = "320.mail:0.E5516212:3439362109167553710460831851522";
    private static final String STID_3_2 = "320.mail:0.E6092282:3507973277115565885362770636644";
    private static final String QUEUEID_2 = "FgayB37uom-mAd45ln9";
    private static final String QUEUEID_3_1 = "2AkHfn1fs4Y1-A3fCbiim";
    private static final String QUEUEID_3_2 = "r4kmn42eISw1-4seeBd9i";
    private static final String SO_OUT1_EML = "complaints/so_out1.eml";
    private static final String SO_OUT3_1_EML = "complaints/so_out3_1.eml";
    private static final String SO_OUT3_2_EML = "complaints/so_out3_2.eml";
    private static final String STOREFS_SO_OUT1_JSON = "complaints/storefs_so_out.json";
    protected static final String SO_OUT1_DELIVERY_LOG = "complaints/so_out1_delivery_log.json";

    protected File complLog;

    @Override
    protected String config(boolean sologIndex, boolean sologger) throws Exception {
        complLog = File.createTempFile(testName.getMethodName(), LOG_EXT);
        return super.config(sologIndex, sologger) + COMPLLOG_FILENAME + complLog.getAbsolutePath()
            + "\ncomplaints.compl-log.format = %{message}\n";
    }

    @Override
    protected IexProxyCluster initIexProxyCluster() throws Exception {
        return new IexProxyCluster(this, null, config(false, true), true, true);
    }

    @Override
    protected void mockFoldersInfo() throws URISyntaxException {
        super.mockFoldersInfo();
        mockUIdFoldersInfo(UID_2, "complaints/folders_so_out.json");
    }

    @Test
    public void testOneComplaintOnHamSologger() throws Exception {
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
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID),
            generateStorefsMock(0, (int) MSG_DATE, 2), //    fileToString(STOREFS_SPAM_JSON),
            MID);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSologger(QUEUEID, SAMPLE_DELIVERY_LOG, UID, Route.IN, 3);
        sendNotifyMove(UID, MID, 1, 2);
        waitAndCheckAbusesFromLucene(UID, Map.of(
            "type", HAM,
            "first_timestamp", 0,
            "last_timestamp", MSG_DATE + DAY,
            "msg_date", MSG_DATE,
            "source", "USER",
            "cnt", "1"
        ), HAM, STID);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID, 1);
        waitShinglersRequests(shinglersQueries, Route.IN, 1, false);
        checkComplLogFlags(complLog.toPath(), Set.of(Flags._F5, Flags._FA, Flags._FM, Flags._SN));
    }

    @Test
    public void testOneMovedToSpamOnSoOutSologger() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=e.a.chekusov@tmn3.etagi.com",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    1130000039493321L,
                    "e.a.chekusov@tmn3.etagi.com",
                    "1130000056695537"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=a.s.sokolova@vladimir.etagi.com",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    Long.parseUnsignedLong(UID_2),
                    "a.s.sokolova@vladimir.etagi.com",
                    "1130000059650293"))));
        cluster.onlineDB().add("/online?uid=1130000039493321", new StaticHttpResource(new OnlineHandler(true)));
        cluster.storageCluster().put(STID_2, new File(getClass().getResource(SO_OUT1_EML).toURI()));
        settingsApiMock(UID_2);
        String mid = Long.toString(173107110677061433L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_2),
            fileToString(STOREFS_SO_OUT1_JSON),
            mid);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSologger(QUEUEID_2, SO_OUT1_DELIVERY_LOG, UID_2, Route.OUT, 8);
        sendNotifyMove(UID_2, mid, 2, 1);
        waitAndCheckAbusesFromLucene(UID_2, Map.of(
            "type", SPAM,
            "first_timestamp", 0,
            "last_timestamp", TIMESTAMP,
            "msg_date", 1594716490,
            "source", "USER",
            "cnt", "1"
        ), SPAM, STID_2);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_2, 1);
        waitShinglersRequests(shinglersQueries, Route.OUT, 1, false);
        checkComplLogFlags(complLog.toPath(), Set.of(Flags._YW, Flags._FA, Flags._SN));
    }

    @Test
    public void testTwoMovedToSpamOnSoOutSologger() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=sedagurenc@unionistanbul.com",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    Long.parseUnsignedLong(UID_3),
                    "sedagurenc@unionistanbul.com",
                    "1130000070482777"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=irfanerkmen@unionistanbul.com",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    1130000046648011L,
                    "irfanerkmen@unionistanbul.com",
                    "1130000063850512"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&uid=" + UID_3,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    Long.parseUnsignedLong(UID_3),
                    "sedagurenc@unionistanbul.com",
                    "1130000070482777"))));
        cluster.onlineDB().add("/online?uid=" + UID_3, new StaticHttpResource(new OnlineHandler(true)));
        cluster.onlineDB().add("/online?uid=1130000046648011", new StaticHttpResource(new OnlineHandler(true)));
        cluster.storageCluster().put(STID_3_1, new File(getClass().getResource(SO_OUT3_1_EML).toURI()));
        cluster.storageCluster().put(STID_3_2, new File(getClass().getResource(SO_OUT3_2_EML).toURI()));
        mockUIdFoldersInfo(UID_3, "complaints/folders_so_out4.json");
        settingsApiMock(UID_3);
        String mid1 = Long.toString(177892185281138008L);
        String mid2 = Long.toString(177892185281138009L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_3),
            fileToString("complaints/storefs_so_out4.json"),
            mid1, mid2);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_3),
            fileToString("complaints/storefs_so_out4.json"),
            mid2, mid1);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSologger(
                List.of(QUEUEID_3_1, QUEUEID_3_2),
                "complaints/so_out3_delivery_log_batch.txt",
                UID_3,
                Route.OUT,
                15);
        sendNotifyMove(
            "/notify?mdb=pg&pgshard=3014&operation-id=305902126&operation-date=1641908470.288358"
                + "&uid=1130000053372955&change-type=move&changed-size=2&batch-size=1&salo-worker=pg3014:5"
                + "&transfer-timestamp=1641908470411&zoo-queue-id=20130789&deadline=1641908480523",
            "{\"uid\":\"" + UID_3 + "\",\"select_date\":\"1641908470.357\",\"pgshard\":\"3014\",\"lcn\":\"9032\","
                + "\"fresh_count\":\"0\",\"operation_date\":\"1641908470.288358\",\"operation_id\":\"305902126\","
                + "\"arguments\":{\"fid\":2,\"tab\":null},\"change_type\":\"move\",\"useful_new_messages\":\"0\","
                + "\"changed\":[{\"fid\":2,\"src_fid\":1,\"deleted\":false,\"tab\":null,\"mid\":" + mid1
                + ",\"recent\":true,\"tid\":null,\"lids\":[],\"seen\":true,\"src_tab\":\"relevant\"},"
                + "{\"fid\":2,\"src_fid\":1,\"deleted\":false,\"tab\":null,\"mid\":" + mid2 + ",\"recent\":true,"
                + "\"tid\":null,\"lids\":[],\"seen\":true,\"src_tab\":\"relevant\"}],\"db_user\":\"mops\","
                + "\"session_key\":\"LIZA-12345678-1234567891011\"}");
        waitAndCheckAbusesFromLucene(UID_3, Map.of(
            "type", SPAM,
            "first_timestamp", 0,
            "last_timestamp", 1641908470,
            "msg_date", List.of(1640599803, 1640599494),
            "source", "USER",
            "cnt", "1"
        ), SPAM, List.of(STID_3_1, STID_3_2));
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_3, 1);
        waitShinglersRequests(shinglersQueries, Route.OUT, 1, false);
        //checkComplLogFlags(complLog.toPath(), Set.of(Flags._YW, Flags._FA, Flags._SN));
    }
}

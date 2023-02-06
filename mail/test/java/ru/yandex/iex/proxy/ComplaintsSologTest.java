package ru.yandex.iex.proxy;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.client.so.shingler.config.ShinglerType;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.iex.proxy.complaints.Flags;
import ru.yandex.iex.proxy.complaints.Route;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.util.timesource.TimeSource;

public class ComplaintsSologTest extends MoveHandlerTestBase {
    private static final String UID_2 = "1130000042447770";
    private static final String UID_4 = "1042136939";
    private static final String UID_5 = "33207940";
    private static final String EMAIL_5 = "nedbali@yandex.ru";
    private static final String UID_6 = "562419305";
    private static final String UID_7 = "86672229";
    private static final String UID_8 = "420776349";
    private static final String UID_9 = "1130000026733235";
    private static final String MID = "166914661189419402";
    private static final String STID_2 = "320.mail:0.E3445099:3218300422106153641462127296572";
    //private static final String STID_2 = "320.mail:588355978.E764924:193765804864810134263421668836";
    //private static final String STID_3 = "320.mail:588355978.E764924:193765804864810134263421668837";
    //private static final String STID_3 = "1.632123143.7594801846142779115218810981";
    private static final String STID_4 = "320.mail:1042136939.E3322011:3997371081703037336742033380";
    private static final String STID_5 = "320.mail:33207940.E3093071:2656325697171083558263162333842";
    private static final String STID_6 = "320.mail:0.E2773788:2411015636178071567837689954873";
    private static final String STID_7 = "320.mail:86672229.E3676158:405766607175033724352659040476";
    private static final String STID_8 = "320.mail:0.E1567919:197377556185058535327269643108";
    private static final String STID_9 = "320.mail:1130000026733235.E4598984:83359929774392102663239454672";
    private static final String STID_10 = "320.mail:1130000026733235.E5335444:1968384306206578370830553728278";
    private static final String STID_11 = "320.mail:1130000026733235.E5362099:2051496433189374827544671794532";
    private static final String STID_12 = "320.mail:0.E4273017:11539379099799172149023766483";
    private static final String STID_13 = "320.mail:1130000026733235.E3931004:3536635919173296077212962007725";
    private static final String STID_14 = "320.mail:1130000026733235.E4105701:25399605517516509935056162721";
    private static final String QUEUEID_2 = "FgayB37uom-mAd45ln9";
    private static final String QUEUEID_4 = "KNjWjXcjz1-OuOSG3tO";
    private static final String QUEUEID_5 = "YPYiZO8vUQ-4WtiaCUG";
    private static final String QUEUEID_6 = "ZmSkxR3LsG-ri9G8SaW";
    private static final String QUEUEID_7 = "muQzhLyJtw-eAEesp3k";
    private static final String QUEUEID_8_1 = "GhhO9Sjrpj-P9JSgqM2";
    private static final String QUEUEID_8_2 = "DEBH5XAmj6-PAICRKOu";
    private static final String QUEUEID_9 = "6fr6iDjgVr-Bpo8Yopv";
    private static final String QUEUEID_10 = "bbifzBcCLg-Mw5id0bE";
    private static final String QUEUEID_11 = "ev7APguu5l-ipM0El0K";
    private static final String QUEUEID_12 = "GoS022RhBe-mEM0XVYT";
    private static final String QUEUEID_13 = "iZONKVc3Y2-MiNuIX4D";
    private static final String QUEUEID_14 = "pF6kIF5X5p-0XqiIMBN";
    private static final String SO_OUT1_EML = "complaints/so_out1.eml";
    private static final String SO_OUT2_EML = "complaints/so_out2.eml";
    private static final String SO_OUT3_EML = "complaints/so_out3.eml";
    private static final String SO_IN1_EML = "complaints/so_in1.eml";
    private static final String SO_IN2_EML = "complaints/so_in2.eml";
    private static final String SO_IN7_EML = "complaints/so_in7.eml";
    private static final String SO_IN9_EML = "complaints/so_in9.eml";
    private static final String SO_IN10_EML = "complaints/so_in10.eml";
    private static final String SO_IN11_EML = "complaints/so_in11.eml";
    private static final String SO_IN12_EML = "complaints/so_in12.eml";
    private static final String SO_IN13_EML = "complaints/so_in13.eml";
    private static final String SO_IN14_EML = "complaints/so_in14.eml";
    private static final String STOREFS_SO_OUT_JSON = "complaints/storefs_so_out.json";
    private static final String STOREFS_SO_OUT_2_JSON = "complaints/storefs_so_out2.json";
    private static final String STOREFS_SO_OUT_3_JSON = "complaints/storefs_so_out3.json";
    private static final String STOREFS_SO_IN_JSON = "complaints/storefs_so_in1.json";
    private static final String STOREFS_SO_IN_2_JSON = "complaints/storefs_so_in2.json";
    private static final String STOREFS_SO_IN_7_JSON = "complaints/storefs_so_in7.json";
    private static final String STOREFS_SO_IN_9_JSON = "complaints/storefs_so_in9.json";
    private static final String LOG_EXT = ".log";
    private static final String COMPLLOG_FILENAME = "\ncomplaints.compl-log.file = ";

    protected File complLog;

    @Override
    protected String config(boolean sologIndex, boolean sologger) throws Exception {
        complLog = File.createTempFile(testName.getMethodName(), LOG_EXT);
        return super.config(sologIndex, sologger) + COMPLLOG_FILENAME + complLog.getAbsolutePath()
            + "\ncomplaints.compl-log.format = %{message}\n";
    }

    @Override
    protected void mockFoldersInfo() throws URISyntaxException {
        super.mockFoldersInfo();
        mockUIdFoldersInfo(UID_2, "complaints/folders_so_out.json");
        mockUIdFoldersInfo(UID_6, "complaints/folders_so_out2.json");
        mockUIdFoldersInfo(UID_8, "complaints/folders_so_out3.json");
        mockUIdFoldersInfo(UID_4, "complaints/folders_so_in1.json");
        mockUIdFoldersInfo(UID_5, "complaints/folders_so_in2.json");
        mockUIdFoldersInfo(UID_7, "complaints/folders_so_in7.json");
        mockUIdFoldersInfo(UID_9, "complaints/folders_so_in9.json");
    }

    @Override
    protected IexProxyCluster initIexProxyCluster() throws Exception {
        return new IexProxyCluster(this, null, config(true, false), true, true);
    }

    @Test
    public void testOneComplaintOnHamSolog() throws Exception {
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
        final Map<ShinglerType, String> shinglersQueries = mockShinglersFromSolog(UID, QUEUEID, Route.IN, 1);
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
        checkComplLogFlags(
            complLog.toPath(),
            Set.of(Flags._F5, Flags._DP, Flags._DL, Flags._SP, Flags._HD, Flags._PF, Flags._AU, Flags._DR, Flags._SN));
    }

    @Test
    public void testOneMovedToSpamOnSoOutSolog() throws Exception {
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
                    1130000042447770L,
                    "a.s.sokolova@vladimir.etagi.com",
                    "1130000059650293"))));
        cluster.onlineDB().add("/online?uid=1130000039493321", new StaticHttpResource(new OnlineHandler(true)));
        cluster.storageCluster().put(STID_2, new File(getClass().getResource(SO_OUT1_EML).toURI()));
        settingsApiMock(UID_2);
        String mid = Long.toString(173107110677061433L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_2),
            fileToString(STOREFS_SO_OUT_JSON),
            mid);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSolog(UID_2, QUEUEID_2, Route.OUT, 2);
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
    }

    @Test
    public void testOneMovedToSpamWithoutRulesSolog() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=noreply@steampowered.com",
            HttpStatus.SC_NOT_FOUND);
        cluster.onlineDB().add("/online?uid=" + UID_4, new StaticHttpResource(new OnlineHandler(true)));
        cluster.storageCluster().put(STID_4, new File(getClass().getResource(SO_IN1_EML).toURI()));
        String mid = Long.toString(173107110677053668L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_4),
            fileToString(STOREFS_SO_IN_JSON),
            mid);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSolog(UID_4, QUEUEID_4, Route.IN, 4);
        ///notify-tteot?&changed-size=2&uid=1042136939&zoo-queue-id=13263346&pgshard=2780&change-type=move&salo
        // -worker=pg2780:3&transfer-timestamp=1596486389675&operation-id=566244493&mdb=pg&operation-date=1596486389
        // .620364&deadline=1596486410788&service=iex_backlog&zoo-queue-id=56992&deadline=1596553857096
        //{"select_date":"1596486389.664","uid":"1042136939","pgshard":"2780","lcn":"748","fresh_count":"1",
        // "operation_date":"1596486389.620364","arguments":{"fid":2},"operation_id":"566244493","change_type":"move",
        // "useful_new_messages":"0","changed":[{"fid":2,"src_fid":1,"deleted":false,"tab":"relevant",
        // "mid":173107110677053668,"recent":true,"lids":[],"seen":true,tid":null,"src_tab":"relevant"},{"fid":2,
        // "src_fid":1,"deleted":false,"tab":"relevant","mid":173107110677053669,"recent":true,"lids":[],"seen":true,
        // "tid":null,"src_tab":"relevant","db_user":"mops","session_key":"LIZA-12345678-1234567891011"}]}
        sendNotifyMove(UID_4, mid, 2, 1);
        waitAndCheckAbusesFromLucene(UID_4, Map.of(
            "type", SPAM,
            "first_timestamp", 0,
            "last_timestamp", TIMESTAMP,
            "msg_date", 1596486296,
            "source", "USER",
            "cnt", "1"
        ), SPAM, STID_4);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_4, 1);
        waitShinglersRequests(shinglersQueries, Route.IN, 0, false);
    }

    @Test
    public void testOneMovedFromSpamWithOldRulesSolog() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=admin@notify.vk.com",
            HttpStatus.SC_NOT_FOUND);
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp&login="
                + EMAIL_5,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(UID_5), EMAIL_5, "83407608"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp&uid="
                + UID_5,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(UID_5), EMAIL_5, "83407608"))));
        cluster.onlineDB().add("/online?uid=" + UID_5, new StaticHttpResource(new OnlineHandler(true)));
        cluster.storageCluster().put(STID_5, new File(getClass().getResource(SO_IN2_EML).toURI()));
        settingsApiMock(UID_5);
        String mid = Long.toString(172544160723644340L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_5),
            fileToString(STOREFS_SO_IN_2_JSON),
            mid);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSolog(UID_5, QUEUEID_5, Route.IN, 5);
        sendNotifyMove(UID_5, mid, 1, 2);
        waitAndCheckAbusesFromLucene(UID_5, Map.of(
            "type", HAM,
            "first_timestamp", 0,
            "last_timestamp", TIMESTAMP,
            "msg_date", 1591189472,
            "source", "USER",
            "cnt", "1"
        ), HAM, STID_5);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_5, 1);
        waitShinglersRequests(shinglersQueries, Route.IN, 1, false);
    }

    @Test
    public void testOneMovedToSpamMultiRecipientsSolog() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(52255086L, "salon585@yandex.ru", "142436579"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585-1@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(562362226L, "salon585-1@yandex.ru", "1063344720"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585-2@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(562411284L, "salon585-2@yandex.ru", "1063384725"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585-3@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    Long.parseLong(UID_6),
                    "salon585-3@yandex.ru",
                    "1063391470"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp&uid="
                + UID_6,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("{\"users\":[{\"id\":\"" + UID_6 + "\",\"uid\":{\"value\":\"" + UID_6 + "\",\"lite\":"
                    + "false,\"hosted\":false},\"login\":\"salon585-3\",\"have_password\":true,\"have_hint\":true,"
                    + "\"karma\":{\"value\":0},\"karma_status\":{\"value\":6085}}]}")));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585-5@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(562444223L, "salon585-5@yandex.ru", "1063410693"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585-6@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(562449469L, "salon585-6@yandex.ru", "1063415098"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=salon585-10@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    649796412L,
                    "salon585-10@yandex.ru",
                    "1113323376"))));
        cluster.onlineDB().add("/online?uid=" + UID_6, new StaticHttpResource(new OnlineHandler(true)));
        cluster.storageCluster().put(STID_6, new File(getClass().getResource(SO_OUT2_EML).toURI()));
        settingsApiMock(UID_6);
        String mid = Long.toString(173107110677057406L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_6),
            fileToString(STOREFS_SO_OUT_2_JSON),
            mid);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSolog(UID_6, QUEUEID_6, Route.OUT, 6);
        ///notify?mdb=pg&pgshard=2757&operation-id=377191746&operation-date=1596617634.816125&uid=562419305
        // &change-type=move&changed-size=1&salo-worker=pg2757:1&transfer-timestamp=1596617634966&zoo-queue-id=12481274
        // &deadline=1596617645086
        //{"uid":"562419305","select_date":"1596617634.949","pgshard":"2757","lcn":"22785","fresh_count":"0",
        // "operation_date":"1596617634.816125","arguments":{"fid":2},"operation_id":"377191746","change_type":"move",
        // "useful_new_messages":"0","changed":[{"fid":2,"src_fid":1,"deleted":false,"tab":"relevant",
        // "mid":173107110677057406,"recent":true,"tid":null,"lids":[],"seen":false,"src_tab":"relevant"}]}
        sendNotifyMove(UID_6, mid, 2, 1);
        waitAndCheckAbusesFromLucene(UID_6, Map.of(
            "type", SPAM,
            "first_timestamp", 0,
            "last_timestamp", TIMESTAMP,
            "msg_date", 1596617625,
            "source", "USER",
            "cnt", "1"
        ), SPAM, STID_6);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_6, 1);
        waitShinglersRequests(shinglersQueries, Route.OUT, 1, false);
    }

    @Test
    public void testOneMovedToTrashSolog() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=mamberger.zhanna@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(
                    86672229L,
                    "mamberger.zhanna@yandex.ru",
                    "238614521"))));
        cluster.onlineDB().add("/online?uid=" + UID_7, new StaticHttpResource(new OnlineHandler(true)));
        cluster.msearch().add(
            "/api/async/enlarge/your?uid=" + UID_7,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
        cluster.storageCluster().put(STID_7, new File(getClass().getResource(SO_IN7_EML).toURI()));
        settingsApiMock(UID_7);
        String mid = Long.toString(173670060630498099L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_7),
            fileToString(STOREFS_SO_IN_7_JSON),
            mid);
        final Map<ShinglerType, String> shinglersQueries =
            mockShinglersFromSolog(UID_7, QUEUEID_7, Route.IN, 7);
        sendNotify(
            "{\"fid\":3,\"src_fid\":1,\"deleted\":false,\"tab\":\"news\",\"mid\":" + mid + ","
                + "\"recent\":true,\"tid\":null,\"lids\":[],\"seen\":false,\"src_tab\":\"news\"}",
            MOVE,
            UID_7,
            1599842233);
        //sendNotifyMove(UID_7, mid, 3, 1, false);
        waitAndCheckAbusesFromLucene(UID_7, Map.of(
            "first_timestamp", 0,
            "last_timestamp", 1599661975,
            "msg_date", 1599661975,
            "source", "USER",
            "cnt", "1"
        ), HAM, STID_7);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_7, 1);
        waitShinglersRequests(shinglersQueries, Route.IN, 1, false);
    }

    @Test
    public void testManyMovedToSpamSolog() throws Exception {
        long recipientUid = Long.parseLong(UID_9);
        List<String> mids = List.of("176766285374330338", "176766285374330339", "176766285374330340",
            "176766285374330341", "176766285374330342", "176766285374330343");
        List<Long> msgDates = List.of(1628693091L, 1628695378L, 1628698311L, 1628686833L, 1628688165L, 1628689695L);
        List<String> stids = List.of(STID_11, STID_10, STID_9, STID_14, STID_13, STID_12);
        List<String> msgs = List.of(SO_IN11_EML, SO_IN10_EML, SO_IN9_EML, SO_IN14_EML, SO_IN13_EML, SO_IN12_EML);
        Map<Long, String> emails = Map.ofEntries(
            Map.entry(recipientUid, "r.v.nesytyh@tmn2.etagi.com"),
            Map.entry(1130000025922582L, "s.n.puincev@tmn2.etagi.com"),
            Map.entry(1130000025991343L, "a.v.fomina@tmn2.etagi.com"),
            Map.entry(1130000026024591L, "a.v.romanova@tmn2.etagi.com"),
            Map.entry(1130000026043587L, "s.i.galimova@tmn2.etagi.com"),
            Map.entry(1130000026107747L, "v.n.mosunova@tmn2.etagi.com"),
            Map.entry(1130000026142532L, "r.n.nekrasov@tmn2.etagi.com"),
            Map.entry(1130000026152688L, "yu.v.avotinsh@tmn2.etagi.com"),
            Map.entry(1130000026171390L, "g.v.shapka@tmn2.etagi.com"),
            Map.entry(1130000026208777L, "i.r.karimov@tmn2.etagi.com"),
            Map.entry(1130000026290820L, "n.a.timchenko@tmn2.etagi.com"),
            Map.entry(1130000026418981L, "s.n.gryzlova@tmn2.etagi.com"),
            Map.entry(1130000026431863L, "a.g.sarkisyan@tmn2.etagi.com"),
            Map.entry(1130000026450418L, "e.g.gurova@tmn2.etagi.com"),
            Map.entry(1130000026530772L, "b.a.zhuldasova@tmn2.etagi.com"),
            Map.entry(1130000026545481L, "g.v.soloboeva@tmn2.etagi.com"),
            Map.entry(1130000026717813L, "e.a.dzyuba@tmn2.etagi.com"),
            Map.entry(1130000026718578L, "e.s.sergeeva@tmn2.etagi.com"),
            Map.entry(1130000026733272L, "v.e.volodin@tmn2.etagi.com"),
            Map.entry(1130000026828060L, "m.v.aleksandrova@tmn2.etagi.com"),
            Map.entry(1130000026858261L, "g.k.saltykova@tmn2.etagi.com"),
            Map.entry(1130000026887560L, "d.d.raimgulov@tmn2.etagi.com"),
            Map.entry(1130000026943561L, "m.a.kuprin@tmn2.etagi.com"),
            Map.entry(1130000027110092L, "a.a.malyar@tmn2.etagi.com"),
            Map.entry(1130000027138015L, "a.n.efanova@tmn2.etagi.com"),
            Map.entry(1130000027138237L, "g.yu.petrova@tmn2.etagi.com"),
            Map.entry(1130000027165973L, "p.v.petrov@tmn2.etagi.com"),
            Map.entry(1130000027182778L, "t.v.moroz@tmn2.etagi.com"),
            Map.entry(1130000027202635L, "z.i.salikov@tmn2.etagi.com"),
            Map.entry(1130000027205125L, "l.v.emelyanova@tmn2.etagi.com"),
            Map.entry(1130000027312601L, "i.a.aleksandrova@tmn2.etagi.com")
        );
        Map<Long, String> suids = Map.ofEntries(
            Map.entry(recipientUid, "1130000043935810"),
            Map.entry(1130000025922582L, "1130000043125516"),
            Map.entry(1130000025991343L, "1130000043194275"),
            Map.entry(1130000026024591L, "1130000043227524"),
            Map.entry(1130000026043587L, "1130000043246519"),
            Map.entry(1130000026107747L, "1130000043310677"),
            Map.entry(1130000026142532L, "1130000043345462"),
            Map.entry(1130000026152688L, "1130000043355617"),
            Map.entry(1130000026171390L, "1130000043374319"),
            Map.entry(1130000026208777L, "1130000043411706"),
            Map.entry(1130000026290820L, "1130000043493749"),
            Map.entry(1130000026418981L, "1130000043621906"),
            Map.entry(1130000026431863L, "1130000043634788"),
            Map.entry(1130000026450418L, "1130000043653343"),
            Map.entry(1130000026530772L, "1130000043733682"),
            Map.entry(1130000026545481L, "1130000043748387"),
            Map.entry(1130000026717813L, "1130000043920388"),
            Map.entry(1130000026718578L, "1130000043921153"),
            Map.entry(1130000026733272L, "1130000043935847"),
            Map.entry(1130000026828060L, "1130000044030631"),
            Map.entry(1130000026858261L, "1130000044060822"),
            Map.entry(1130000026887560L, "1130000044090121"),
            Map.entry(1130000026943561L, "1130000044146122"),
            Map.entry(1130000027110092L, "1130000044312644"),
            Map.entry(1130000027138015L, "1130000044340565"),
            Map.entry(1130000027138237L, "1130000044340787"),
            Map.entry(1130000027165973L, "1130000044368522"),
            Map.entry(1130000027182778L, "1130000044385326"),
            Map.entry(1130000027202635L, "1130000044405182"),
            Map.entry(1130000027205125L, "1130000044407672"),
            Map.entry(1130000027312601L, "1130000044515143")
        );
        for (Long uid : emails.keySet()) {
            cluster.blackboxDirect().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                    + "&login=" + emails.get(uid),
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity(IexProxyCluster.blackboxResponse(uid, emails.get(uid), suids.get(uid)))));
        }
        cluster.onlineDB().add("/online?uid=" + UID_9, new StaticHttpResource(new OnlineHandler(true)));
        cluster.msearch().add(
            "/api/async/enlarge/your?uid=" + UID_9,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
        for (int i = 0; i < 6; i++) {
            cluster.storageCluster().put(stids.get(i), new File(getClass().getResource(msgs.get(i)).toURI()));
        }
        settingsApiMock(UID_9);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_9),
            fileToString(STOREFS_SO_IN_9_JSON),
            mids);
        final List<Map<ShinglerType, String>> shinglersQueries = List.of(
            mockShinglersFromSolog(UID_9, QUEUEID_9, Route.IN, 9),
            mockShinglersFromSolog(UID_9, QUEUEID_10, Route.IN, 10),
            mockShinglersFromSolog(UID_9, QUEUEID_11, Route.IN, 11),
            mockShinglersFromSolog(UID_9, QUEUEID_12, Route.IN, 12),
            mockShinglersFromSolog(UID_9, QUEUEID_13, Route.IN, 13),
            mockShinglersFromSolog(UID_9, QUEUEID_14, Route.IN, 14));
        sendNotify(
            "{\"fid\":2,\"src_fid\":1,\"deleted\":false,\"tab\":null,\"mid\":176766285374330338,\"recent\":true,"
            + "\"seen\":false,\"tid\":null,\"lids\":[],\"src_tab\":\"relevant\"},{\"fid\":2,\"src_fid\":1,"
            + "\"deleted\":false,\"tab\":null,\"mid\":176766285374330339,\"recent\":true,\"seen\":false,\"tid\":null,"
            + "\"lids\":[],\"src_tab\":\"news\"},{\"fid\":2,\"src_fid\":1,\"deleted\":false,\"tab\":null,"
            + "\"mid\":176766285374330340,\"recent\":true,\"seen\":false,\"tid\":null,\"lids\":[],\"src_tab\":\"news\"}"
            + ",{\"fid\":2,\"src_fid\":1,\"deleted\":false,\"tab\":null,\"mid\":176766285374330341,\"recent\":true,"
            + "\"seen\":false,\"tid\":null,\"lids\":[],\"src_tab\":\"relevant\"},{\"fid\":2,\"src_fid\":1,"
            + "\"deleted\":false,\"tab\":null,\"mid\":176766285374330342,\"recent\":true,\"seen\":false,\"tid\":null,"
            + "\"lids\":[],\"src_tab\":\"relevant\"},{\"fid\":2,\"src_fid\":1,\"deleted\":false,\"tab\":null,"
            + "\"mid\":176766285374330343,\"recent\":true,\"seen\":false,\"tid\":null,\"lids\":[],"
            + "\"src_tab\":\"news\"}",
            MOVE,
            UID_9,
            1628698713L);
        waitAndCheckAbusesFromLucene(UID_9, Map.of(
            "type", SPAM,
            "first_timestamp", 0,
            "last_timestamp", 1628698713L,
            "msg_date", msgDates,
            "source", "USER",
            "cnt", "1"
        ), SPAM, stids);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 0);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID_9, 1);
        for (int i = 9; i < 15; i++) {
            waitShinglersRequests(shinglersQueries.get(i - 9), Route.IN, i, false);
        }
    }

    @Test
    public void testSearchIndexForMultipleSmtpIdsSolog() throws Exception {
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=nad-art78@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(54494491L, "nad-art78@yandex.ru", "148947131"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp"
                + "&login=djuliaesckowa@yandex.ru",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(
                    IexProxyCluster.blackboxResponse(420776349L, "djuliaesckowa@yandex.ru", "950722568"))));
        cluster.blackboxDirect().add(
            "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=subscription.suid.2&sid=smtp&uid="
                + UID_8,
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("{\"users\":[{\"id\":\"" + UID_8 + "\",\"uid\":{\"value\":\"" + UID_8 + "\",\"lite\":"
                    + "false,\"hosted\":false},\"login\":\"djuliaesckowa\",\"have_password\":true,\"have_hint\":true,"
                    + "\"karma\":{\"value\":0},\"karma_status\":{\"value\":6085}}]}")));
        cluster.onlineDB().add("/online?uid=" + UID_8, new StaticHttpResource(new OnlineHandler(true)));
        cluster.msearch().add(
            "/api/async/enlarge/your?uid=" + UID_8,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
        cluster.storageCluster().put(STID_8, new File(getClass().getResource(SO_OUT3_EML).toURI()));
        settingsApiMock(UID_8);
        String mid = Long.toString(175077435514033537L);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID_8),
            fileToString(STOREFS_SO_OUT_3_JSON),
            mid);
        long now = Math.round(TimeSource.INSTANCE.currentTimeMillis() / 1000.0);
        String sologData = sologIndexMock(UID_8, QUEUEID_8_1, 8);
        Map<String, String> fields = Map.of(
            SMTP_ID, QUEUEID_8_1,
            "data", JsonChecker.StringJsonWrapper.PREFIX + sologData);
        sendNotify("{"
            + FID_PARAM +  "2," + SRC_FID_PARAM + "1,"  // complaint on spam
            + "\"deleted\":false,\"tab\":\"news\"," + MID_PARAM + mid
            + ",\"recent\":true,\"tid\":null,\"lids\":[],\"seen\":false,\"src_tab\":\"news\"}",
            MOVE,
            UID_8,
            now - 1);
        waitAndCheckSologInfoFromLucene(UID_8, List.of(QUEUEID_8_1, QUEUEID_8_2), fields, true);
        waitAndCheckSologInfoFromLucene(UID_8, List.of(QUEUEID_8_2, QUEUEID_8_1), fields, true);
        waitAndCheckSologInfoFromLucene(UID_8, List.of(QUEUEID_8_1), fields, true);
        waitAndCheckSologInfoFromLucene(UID_8, List.of(QUEUEID_8_2), fields, false);
        waitAndCheckUserDayAbusesInfoFromLucene(UID_8, STID_8, SPAM, true);
    }

    @SuppressWarnings("unused")
    private void checkComplLog(final String expected) throws Exception {
        List<String> lines = Files.readAllLines(complLog.toPath(), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, lines.get(0));
    }
}

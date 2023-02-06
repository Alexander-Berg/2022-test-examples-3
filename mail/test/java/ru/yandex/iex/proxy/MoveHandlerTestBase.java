package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.client.so.shingler.GeneralShinglerClient;
import ru.yandex.client.so.shingler.TestShinglerServer;
import ru.yandex.client.so.shingler.config.ShinglerType;
import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.iex.proxy.complaints.Flags;
import ru.yandex.iex.proxy.complaints.Route;
import ru.yandex.iex.proxy.complaints.UserActionHandler;
import ru.yandex.iex.proxy.move.UpdateDataExecutor;
import ru.yandex.json.parser.JsonException;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class MoveHandlerTestBase extends TestBase {
    protected static final String UID = "588355978";
    protected static final String SUID = "1081738993";
    protected static final String EMAIL = "marija-smolina28test@yandex.ru";
    protected static final String STID = "320.mail:588355978.E764924:193765804864810134263421668835";
    protected static final long MID0 = 166914661189419402L;
    protected static final String MID = Long.toString(MID0);
    //protected static final String QUEUEID = "Rx8Vs7vSL3-PvveWwPu";
    protected static final String QUEUEID = "Ot58hzvaU5-btJaftlg";
    protected static final String USER = "USER";
    protected static final String SPAM = "spam";
    protected static final String HAM = "ham";
    protected static final long MSG_DATE = 1537533478;
    protected static final long DAY = 86400;
    protected static final long SIX_HOURS = 21600;
    protected static final long TIMESTAMP = MSG_DATE + DAY;
    protected static final long TIMEOUT = 2000;
    protected static final int INTERVAL = 100;
    protected static final String MID_PARAM = "\"mid\": ";
    protected static final String FID_PARAM = "\"fid\": ";
    protected static final String SRC_FID_PARAM = "\"src_fid\": ";
    protected static final String FACT_MID = "fact_mid";
    protected static final String SENDERS_SPAM_BALANCE = "senders_spam_balance";
    protected static final String SMTP_ID = "smtp_id";

    protected static final String HTTP_LOCALHOST = "http://localhost:";
    protected static final String AXIS_URI = "/v1/facts/store_batch?client_id=extractors";
    protected static final String FACTS_URI = "/facts?mdb=pg&uid=" + UID + "&extract&cokedump";
    protected static final String ENTITY_RETURNED_MSG = "Entity returned:\n";
    //protected static final String FILTER_SEARCH_URI = "/filter_search?" + IexProxy.FILTER_SEARCH_PARAMS + "&uid=";
    protected static final String FOLDERS_URI = "/folders?caller=msearch&mdb=pg&uid=";
    protected static final String SETTINGS_URI = "/get?&uid=";
    protected static final String EMPTY_SEARCH_RESULT = "{\"hitsCount\":0,\"hitsArray\":[]}";
    protected static final String SOLOGGER_URI = "/search?&queueid=";

    protected static final String PATH = "move-handler/";
    protected static final String FACTS_JSON = "facts.json";
    protected static final String FACTS_FEW_JSON = "facts_few_mids.json";
    protected static final String FACTS_EMPTY_JSON = "facts_empty.json";
    protected static final String FACTS_EMPTY_FEW_JSON = "facts_empty_few_mids.json";
    protected static final String STOREFS_JSON = "storefs.json";
    protected static final String STOREFS_SPAM_JSON = "storefs_spam.json";
    protected static final String FOLDERS_JSON = "folders.json";
    protected static final String EVENT_JSON = "event.json";
    protected static final String SEARCH_JSON = "search.json";
    protected static final String SEARCH_FEW_JSON = "search_few_mids.json";
    protected static final String SEARCH_SPAM_JSON = "search_spam.json";
    protected static final String SEARCH_SPAM_FEW_JSON = "search_spam_few_mids.json";
    protected static final String MAIL_REPORT_URI = "mail_report_uri";
    protected static final String SAMPLE_EML = PATH + "eml1.eml";
    protected static final String SAMPLE_DELIVERY_LOG = PATH + "eml1_delivery_log.json";
    protected static final String EML_WITH_X_YDX_SPAM_4 = "action/change_confirm_url.eml";
    protected static final String SOLOG_DATA_JSON = "complaints/solog_data1.json";
    protected static final String SETTINGS_JSON = "complaints/settings_data.json";
    protected static final String ACTIVITY_SHINGLER_PUT = "complaints/activity_shingler_put1.json";
    protected static final String COMPL_SHINGLER_PUT = "complaints/compl_shingler_put1.json";
    protected static final String MASS_IN_SHINGLER_PUT = "complaints/mass_in_shingler_put1.txt";
    protected static final String MASS_OUT_SHINGLER_PUT = "complaints/mass_out_shingler_put1.txt";
    protected static final String FREEMAIL_SHINGLER_PUT = "complaints/freemail_shingler_put1.json";
    protected static final String SENDER_SHINGLER_PUT = "complaints/sender_shingler_put1.json";
    protected static final String URL_SHINGLER_PUT = "complaints/url_shingler_put1.json";

    protected static final String MOVE = "move";
    protected static final String STORE = "store";

    protected static final String ENVELOPES = "envelopes";

    protected static final int INBOX_FID = 1;
    protected static final int SPAM_FID = 2;
    protected static final int TRASH_FID = 3;
    protected static final int HIDDEN_TRASH_FID = 18;

    protected IexProxyCluster cluster;

    protected String config(boolean sologIndex, boolean sologger) throws Exception {
        return "complaints.solog = " + (sologIndex ? "on" : "off") + "\ncomplaints.sologger = "
            + (sologger ? "on" : "off") + '\n';
    }

    protected IexProxyCluster initIexProxyCluster() throws Exception {
        return new IexProxyCluster(this, null, config(true, true), true, true);
    }

    protected void mockBlackBox() throws UnsupportedEncodingException {
        cluster.blackbox().add(
            "/blackbox*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(UID), EMAIL, SUID))));
        cluster.blackboxDirect().add(
            "/blackbox*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(IexProxyCluster.blackboxResponse(Long.parseLong(UID), EMAIL, SUID))));
    }

    protected void mockOnlineHandler() {
        final String onlineUri = "/online?uid=" + UID;
        cluster.onlineDB().add(
            onlineUri,
            new StaticHttpResource(new OnlineHandler(true)));
    }

    protected void mockEnlargeResponse() throws UnsupportedEncodingException {
        final String msearchUri = "/api/async/enlarge/your?uid=" + UID;
        cluster.msearch().add(
            msearchUri,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));
    }

    protected void mockStatusResponse() throws UnsupportedEncodingException {
        cluster.producer().add(
            "/_status*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("[{\"localhost\":-1}]")));
    }

    protected void mockAxisResponse() {
        cluster.axis().add(AXIS_URI, HttpStatus.SC_OK);
    }

    protected void mockFoldersInfo() throws URISyntaxException {
        mockUIdFoldersInfo(UID, PATH + FOLDERS_JSON);
    }

    protected void mockUIdFoldersInfo(final String uid, final String filePath) throws URISyntaxException {
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(filePath).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.folders().add(FOLDERS_URI + uid, new StaticHttpResource(HttpStatus.SC_OK, entity));
    }

    protected void mockCalendarInfo() throws URISyntaxException {
        FileEntity entity = new FileEntity(
            new File(getClass().getResource(PATH + EVENT_JSON).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.calendar().add(
            "/api/mail/importEventByIcsUrl?*",
            new StaticHttpResource(HttpStatus.SC_OK, entity));
    }

    protected void mockAttachSid() throws UnsupportedEncodingException {
        cluster.attachSid().add(
            "/attach_sid",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("{\"result\":[{\"sids\":[\"sid_mock\"]}]}")));
    }

    protected void mockStoreMsg() throws URISyntaxException {
        cluster.storageCluster().put(STID,
            new File(getClass().getResource(SAMPLE_EML).toURI()));
    }

    protected void mockProducerAsyncClient() throws IOException {
        cluster.producerAsyncClient().add(
            "/notify*",
            new ProxyHandler(cluster.iexproxy().port()));
        cluster.producerAsyncClient().register(
            new Pattern<>("/add", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/modify", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/delete", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.producerAsyncClient().register(
            new Pattern<>("/update", false),
            new ExpectingHeaderHttpItem(
                new ProxyHandler(cluster.testLucene().indexerPort()),
                YandexHeaders.SERVICE,
                "change_log"),
            RequestHandlerMapper.POST);
    }

    protected void mockOthers() {
    }

    @Before
    public void initializeCluster() throws Exception {
        org.junit.Assume.assumeTrue(MailStorageCluster.iexUrl() != null);
        cluster = initIexProxyCluster();
        cluster.iexproxy().start();

        // blackbox response mock
        mockBlackBox();

        // online handler
        mockOnlineHandler();

        // enlarge response mock
        mockEnlargeResponse();

        // status response mock
        mockStatusResponse();

        // axis response mock
        mockAxisResponse();

        // folders mock
        mockFoldersInfo();

        // calendar mock
        mockCalendarInfo();

        // attachSid mock
        mockAttachSid();

        // mock some other web-handles
        settingsApiMock(UID);
        cluster.complaints().add("/fbl-out*", HttpStatus.SC_OK);

        // put EML in storage
        // EMLs do not belong to mails. For now, it works.
        mockStoreMsg();

        // producerAsyncClient add requests handlers
        mockProducerAsyncClient();

        mockOthers();
    }

    @After
    public void destroyCluster() throws Exception {
        cluster.close();
    }

    protected void storeMsgs(
        final String facts,
        final String search,
        boolean spam,
        final String... mids) throws Exception
    {
        StringBuilder changed = new StringBuilder();
        for (String mid : mids) {
            if (changed.length() != 0) {
                changed.append(',');
            }
            changed.append('{').append(MID_PARAM).append(mid)
                .append(',').append(FID_PARAM).append(spam ? "2" : "1").append("}");
        }
        sendNotify(changed.toString(), STORE);
        waitAndCheckFacts(PATH + facts, mids);
        waitAndCheckSearch(PATH + search);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, spam ? 0 : 1);
        cluster.waitProducerRequests(cluster.folders(), FOLDERS_URI + UID, 0);
    }

    protected void sendNotify(final String changed, final String changeType)
        throws IOException, HttpException
    {
        sendNotify(changed, changeType, UID, TIMESTAMP);
    }

    protected void sendNotify(final String changed, final String changeType, final String uid, final long time)
        throws IOException, HttpException
    {
        String uri = "/notify?mdb=pg&service=" + IexProxyCluster.QUEUE_NAME + "&operation-date=" + time
            + "&zoo-queue-id=7799292&change-type=" + changeType + "&uid=" + uid;
        String body = '{'
            + "\"uid\": " + uid + ','
            + "\"change_type\": \"" + changeType + "\","
            + "\"operation_date\": " + time + ','
            + "\"operation_id\": 10,"
            + "\"mdb\": \"pg\","
            + "\"changed\": [" + changed + "],"
            + "\"db_user\":\"mops\","
            + "\"session_key\":\"LIZA-12345678-1234567891011\"}";
        HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + uri);
        post.setEntity(new StringEntity(body));
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString = CharsetUtils.toString(response.getEntity());
                logger.info(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }

    protected void checkFacts(
        final String fileName,
        final String... mids) throws Exception
    {
        QueryConstructor uri = new QueryConstructor(
            HTTP_LOCALHOST + cluster.iexproxy().port() + FACTS_URI);
        for (String mid: mids) {
            uri.append("mid", mid);
        }
        HttpGet getFacts = new HttpGet(uri.toString());
        String facts;
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                facts = CharsetUtils.toString(response.getEntity());
            }
        }
        cluster.compareJson(fileName, facts, false);
    }

    protected void waitAndCheckFacts(
        final String fileName,
        final String... mids)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TIMEOUT) {
            try {
                checkFacts(fileName, mids);
            } catch (AssertionError error) {
                Thread.sleep(TIMEOUT >> 4);
                continue;
            }
            return;
        }
        checkFacts(fileName, mids);
    }

    protected void waitAndCheckPfilters(
        final String lastType,
        final Long lastTimestamp,
        String spams,
        String hams)
        throws Exception
    {
        QueryConstructor searchQuery = new QueryConstructor("/search?");
        searchQuery.append("prefix", UID);
        searchQuery.append("service", IexProxyCluster.QUEUE_NAME);
        searchQuery.append("sort", "url");
        searchQuery.append("text",  "url:pfilters4_*");
        searchQuery.append("get", "url,pfilters_last_type," +
            "pfilters_last_timestamp,pfilters_spams,pfilters_hams");
        String search = searchQuery.toString();

        JsonChecker checker;
        if (lastTimestamp == null) {
            Thread.sleep(TIMEOUT >> 4);
            checker = new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}");
        } else {
            // cause null -> "pfilters_ham_count": null
            // and   "1" -> "pfilters_ham_count": "1"
            if (spams != null) {
                spams = '"' + spams + '"';
            }
            if (hams != null) {
                hams = '"' + hams + '"';
            }
            checker = new JsonChecker("{" +
                "\"hitsCount\":1," +
                "\"hitsArray\":[{" +
                    "\"url\":\"pfilters4_588355978_maria_smolina@yahoo.com/yahoo.com\"," +
                    "\"pfilters_last_type\":" + lastType + "," +
                    "\"pfilters_last_timestamp\":\"" + lastTimestamp + "\"," +
                    "\"pfilters_spams\": " + spams + "," +
                    "\"pfilters_hams\": " + hams + " }]}");
        }

        long start = System.currentTimeMillis();
        while (true) {
            String output = cluster.testLucene().getSearchOutput(search);
            String compareResult = checker.check(output);
            if (compareResult == null) {
                break;
            }
            if (System.currentTimeMillis() - start > TIMEOUT) {
                Assert.fail(compareResult);
            }
            Thread.sleep(TIMEOUT >> 4);
        }
    }

    protected void checkSearch(final String fileName)
        throws Exception
    {
        // Search facts
        QueryConstructor searchQuery = new QueryConstructor("/search?");
        searchQuery.append("prefix", UID);
        searchQuery.append("service", IexProxyCluster.QUEUE_NAME);
        searchQuery.append("sort", FACT_MID);
        searchQuery.append("asc", "true");
        searchQuery.append("text", FACT_MID + ":*");
        searchQuery.append("get", "url,fact_uid,fact_stid,fact_name,fact_mid,"
            + "fact_received_date,fact_message_type,fact_is_coke_solution,"
            + "fact_from,fact_event_id,fact_event_recurrence_id,fact_domain,"
            + "fact_data");
        searchQuery.append("skip-nulls", "true");
        String search = searchQuery.toString();
        String searchOutput = cluster.testLucene().getSearchOutput(search);
        cluster.compareJson(fileName, searchOutput, false);
    }

    protected void waitAndCheckSearch(final String fileName)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TIMEOUT) {
            try {
                checkSearch(fileName);
            } catch (AssertionError error) {
                Thread.sleep(TIMEOUT >> 4);
                continue;
            }
            return;
        }
        checkSearch(fileName);
    }

    protected static String fidToFolderName(final int fid) throws Exception {
        switch (fid) {
            case INBOX_FID:
                return "Inbox";
            case SPAM_FID:
                return "Spam";
            case TRASH_FID:
                return "Trash";
            case HIDDEN_TRASH_FID:
                return "hidden_trash_0d670d1f-1c93-4967-b57c-383bff2b3db";
            default:
                throw new Exception("Unknown fid: " + fid);
        }
    }

    protected String generateStorefsMock(
        final int id,
        final int date,
        final int dstFid)
        throws Exception
    {
        String mid = Long.toString(MID0 + id);
        String stid = "320.mail:588355978.E764924:1937658048648101342" + (63421668835L + id);
        String result =
            new String(
                getClass()
                    .getResourceAsStream(PATH + "/storefs.json.template")
                    .readAllBytes(),
                StandardCharsets.UTF_8)
                .replaceAll("\\$\\{mid\\}", mid)
                .replaceAll("\\$\\{stid\\}", stid)
                .replaceAll("\\$\\{date\\}", Integer.toString(date))
                .replaceAll("\\$\\{folder_name\\}", fidToFolderName(dstFid))
                .replaceAll("\\$\\{folder_code\\}", Integer.toString(dstFid));
        return result;
    }

    protected void receiveMail(final int id, final int date) throws Exception {
        String mid = Long.toString(MID0 + id);

        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID),
            generateStorefsMock(id, date, INBOX_FID),
            mid);

        sendNotify('{'
            + MID_PARAM + mid + ','
            + FID_PARAM + "1}", STORE, UID, date);
    }

    protected void moveMail(
        final int id,
        final int receiveDate,
        final boolean toSpam,
        final int date)
        throws Exception
    {
        moveMail(
            id,
            receiveDate,
            toSpam ? INBOX_FID : SPAM_FID,
            toSpam ? SPAM_FID : INBOX_FID,
            date);
    }

    protected void moveMail(
        final int id,
        final int receiveDate,
        final int srcFid,
        final int dstFid,
        final int date)
        throws Exception
    {
        String mid = Long.toString(MID0 + id);
        IexProxyTestMocks.filterSearchMock(
            cluster,
            Long.parseUnsignedLong(UID),
            generateStorefsMock(id, receiveDate, dstFid),
            mid);
        sendNotify(
            '{' + MID_PARAM + mid + ','
                    + SRC_FID_PARAM + srcFid + ','
                    + FID_PARAM + dstFid + '}',
            MOVE,
            UID,
            date);
    }

    protected void sendNotifyMove(
        final String uid,
        final String mid,
        final int fid,
        final int srcFid,
        final boolean seen)
        throws IOException, HttpException
    {
        String uri = "/notify-tteot?&changed-size=1&uid=" + uid + "&zoo-queue-id=14210701&pgshard=2915"
            + "&change-type=move&salo-worker=pg2915:10&transfer-timestamp=1594717066546&operation-id=235565102"
            + "&mdb=pg&operation-date=" + TIMESTAMP + ".282866&deadline=1594717087693&service="
            + IexProxyCluster.QUEUE_NAME + "&zoo-queue-id=52685&deadline=1594769402575";
        String body = "{\"uid\":\"" + uid + "\",\"select_date\":\"" + TIMESTAMP + ".305\",\"pgshard\":\"2915\","
            + "\"lcn\":\"21821\",\"fresh_count\":\"0\",\"operation_date\":\"" + TIMESTAMP + ".282866\","
            + "\"operation_id\":\"235565102\",\"arguments\":{\"fid\":" + fid + "},\"change_type\":\"move\","
            + "\"useful_new_messages\":\"4\",\"changed\":[{\"fid\":" + fid + ",\"src_fid\":" + srcFid + ",\"deleted\":"
            + "false,\"tab\":\"relevant\",\"mid\":" + mid + ",\"recent\":true,\"lids\":[],\"tid\":null,\"seen\":" + seen
            + ",\"src_tab\":\"relevant\"}],\"db_user\":\"mops\",\"session_key\":\"LIZA-12345678-1234567891011\"}";
        sendNotifyMove(uri, body);
    }

    protected void sendNotifyMove(final String uid, final String mid, final int fid, final int srcFid)
        throws IOException, HttpException
    {
        sendNotifyMove(uid, mid, fid, srcFid, true);
    }

    protected void sendNotifyMove(final String uri, final String body)
        throws IOException, HttpException
    {
        HttpPost post = new HttpPost(HTTP_LOCALHOST + cluster.iexproxy().port() + uri);
        post.setEntity(new StringEntity(body));
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString = CharsetUtils.toString(response.getEntity());
                logger.info(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }

    protected void moveMailToTab(int id, boolean inSpam, int date)
        throws IOException, HttpException
    {
        sendNotify('{'
                + MID_PARAM + (MID0 + id) + ','
                + FID_PARAM + (inSpam ? '2' : '1') + ","
                + SRC_FID_PARAM + (inSpam ? '2' : '1') + "}",
            "move-to-tab",
            UID,
            date);
    }

    protected void filterSearchMock(final String file, final String... mids)
        throws URISyntaxException, BadRequestException
    {
        IexProxyTestMocks.filterSearchMock(cluster, PATH + file, Long.parseUnsignedLong(UID), mids);
    }

    protected void filterSearchMock(
        final List<String> files,
        final String... mids) throws BadRequestException, URISyntaxException, IOException, JsonException
    {
        IexProxyTestMocks.filterSearchMock(
            cluster,
            files.stream().map(x -> PATH + x).collect(Collectors.toList()),
            Long.parseUnsignedLong(UID),
            mids);
    }

    protected void settingsApiMock(final String uid) throws URISyntaxException, NumberFormatException {
        FileEntity entitySettings = new FileEntity(
            new File(getClass().getResource(SETTINGS_JSON).toURI()),
            ContentType.APPLICATION_JSON);
        if (BlackboxUserinfo.corp(Long.parseUnsignedLong(uid))) {
            cluster.corpSettingsApi().add(
               SETTINGS_URI + uid + "&settings_list=show_folders_tabs",
               new StaticHttpResource(HttpStatus.SC_OK, entitySettings));
        } else {
            cluster.settingsApi().add(
                SETTINGS_URI + uid + "&settings_list=show_folders_tabs",
                new StaticHttpResource(HttpStatus.SC_OK, entitySettings));
        }
    }

    protected Map<ShinglerType, String> mockShinglersFromSolog(
        final String uid,
        final String queueid,
        final Route route,
        final int n)
        throws Exception
    {
        sologIndexMock(uid, queueid, n);
        return mockShinglers(route, n);
    }

    protected Map<ShinglerType, String> mockShinglersFromSologger(
        final String queueid,
        final String filePath,
        final String uid,
        final Route route,
        final int n)
        throws Exception
    {
        sologgerMock(queueid, uid, route, filePath);
        return mockShinglers(route, n);
    }

    protected Map<ShinglerType, String> mockShinglersFromSologger(
        final List<String> queueIds,
        final String filePath,
        final String uid,
        final Route route,
        final int n)
        throws Exception
    {
        sologgerMock(queueIds, uid, route, filePath);
        return mockShinglers(route, n);
    }

    protected Map<ShinglerType, String> mockShinglers(final Route route, final int n)
        throws Exception
    {
        final String c = Integer.toString(n);
        final Map<ShinglerType, String> shinglersQueries = new HashMap<>();
        String activityShinglerPut = fileToString(ACTIVITY_SHINGLER_PUT.replace("1", c));
        if (activityShinglerPut != null && !activityShinglerPut.isEmpty()) {
            activityShinglerPut = activityShinglerPut.replaceAll("\\s+", "");
            cluster.shingler(ShinglerType.ACTIVITY).add(
                GeneralShinglerClient.URI,
                activityShinglerPut,
                HttpStatus.SC_OK);
            shinglersQueries.put(ShinglerType.ACTIVITY, activityShinglerPut);
        }
        String complShinglerPut = fileToString(COMPL_SHINGLER_PUT.replace("1", c));
        if (complShinglerPut != null && !complShinglerPut.isEmpty()) {
            complShinglerPut = complShinglerPut.replaceAll("\\s+", "");
            cluster.shingler(ShinglerType.COMPL).add(GeneralShinglerClient.URI, complShinglerPut, HttpStatus.SC_OK);
            shinglersQueries.put(ShinglerType.COMPL, complShinglerPut);
        }
        if (route == Route.IN) {
            String massInShinglerPut = fileToString(MASS_IN_SHINGLER_PUT.replace("1", c));
            if (massInShinglerPut != null && !massInShinglerPut.isEmpty()) {
                massInShinglerPut = massInShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.MASS_IN).add(massInShinglerPut, HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.MASS_IN, massInShinglerPut);
            }
        } else {
            String massOutShinglerPut = fileToString(MASS_OUT_SHINGLER_PUT.replace("1", c));
            if (massOutShinglerPut != null && !massOutShinglerPut.isEmpty()) {
                massOutShinglerPut = massOutShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.MASS_OUT).add(massOutShinglerPut, HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.MASS_OUT, massOutShinglerPut);
            }
            String freemailShinglerPut = fileToString(FREEMAIL_SHINGLER_PUT.replace("1", c));
            if (freemailShinglerPut != null && !freemailShinglerPut.isEmpty()) {
                freemailShinglerPut = freemailShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.FREEMAIL).add(
                    GeneralShinglerClient.URI,
                    freemailShinglerPut,
                    HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.FREEMAIL, freemailShinglerPut);
            }
        }
        String senderShinglerPut = fileToString(SENDER_SHINGLER_PUT.replace("1", c));
        if (senderShinglerPut != null && !senderShinglerPut.isEmpty()) {
            senderShinglerPut = senderShinglerPut.replaceAll("\\s+", "");
            cluster.shingler(ShinglerType.SENDER).add(GeneralShinglerClient.URI, senderShinglerPut, HttpStatus.SC_OK);
            shinglersQueries.put(ShinglerType.SENDER, senderShinglerPut);
        }
        String urlShinglerPut = fileToString(URL_SHINGLER_PUT.replace("1", c));
        if (urlShinglerPut != null && !urlShinglerPut.isEmpty()) {
            urlShinglerPut = urlShinglerPut.replaceAll("\\s+", "");
            cluster.shingler(ShinglerType.URL).add(GeneralShinglerClient.URI, urlShinglerPut, HttpStatus.SC_OK);
            shinglersQueries.put(ShinglerType.URL, urlShinglerPut);
        }
        return shinglersQueries;
    }

    @SuppressWarnings("unused")
    protected Map<ShinglerType, String> shinglersMock2(
        final Set<ShinglerType> shinglers,
        final Route route,
        final int n)
        throws Exception
    {
        final char c = Character.forDigit(n, 10);
        final Map<ShinglerType, String> shinglersQueries = new HashMap<>();

        if (shinglers.contains(ShinglerType.ACTIVITY)) {
            String activityShinglerPut = fileToString(ACTIVITY_SHINGLER_PUT.replace('1', c));
            if (activityShinglerPut != null && !activityShinglerPut.isEmpty()) {
                activityShinglerPut = activityShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.ACTIVITY).add(
                    GeneralShinglerClient.URI,
                    activityShinglerPut,
                    HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.ACTIVITY, activityShinglerPut);
            }
        }
        if (shinglers.contains(ShinglerType.COMPL)) {
            String complShinglerPut = fileToString(COMPL_SHINGLER_PUT.replace('1', c));
            if (complShinglerPut != null && !complShinglerPut.isEmpty()) {
                complShinglerPut = complShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.COMPL).add(GeneralShinglerClient.URI, complShinglerPut, HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.COMPL, complShinglerPut);
            }
        }
        if (route == Route.IN) {
            if (shinglers.contains(ShinglerType.MASS_IN)) {
                String massInShinglerPut = fileToString(MASS_IN_SHINGLER_PUT.replace('1', c));
                if (massInShinglerPut != null && !massInShinglerPut.isEmpty()) {
                    massInShinglerPut = massInShinglerPut.replaceAll("\\s+", "");
                    cluster.shingler(ShinglerType.MASS_IN).add(massInShinglerPut, HttpStatus.SC_OK);
                    shinglersQueries.put(ShinglerType.MASS_IN, massInShinglerPut);
                }
            }
        } else {
            if (shinglers.contains(ShinglerType.MASS_OUT)) {
                String massOutShinglerPut = fileToString(MASS_OUT_SHINGLER_PUT.replace('1', c));
                if (massOutShinglerPut != null && !massOutShinglerPut.isEmpty()) {
                    massOutShinglerPut = massOutShinglerPut.replaceAll("\\s+", "");
                    cluster.shingler(ShinglerType.MASS_OUT).add(massOutShinglerPut, HttpStatus.SC_OK);
                    shinglersQueries.put(ShinglerType.MASS_OUT, massOutShinglerPut);
                }
            }
        }
        if (shinglers.contains(ShinglerType.FREEMAIL)) {
            String freemailShinglerPut = fileToString(FREEMAIL_SHINGLER_PUT.replace('1', c));
            if (freemailShinglerPut != null && !freemailShinglerPut.isEmpty()) {
                freemailShinglerPut = freemailShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.FREEMAIL).add(
                    GeneralShinglerClient.URI,
                    freemailShinglerPut,
                    HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.FREEMAIL, freemailShinglerPut);
            }
        }
        if (shinglers.contains(ShinglerType.SENDER)) {
            String senderShinglerPut = fileToString(SENDER_SHINGLER_PUT.replace('1', c));
            if (senderShinglerPut != null && !senderShinglerPut.isEmpty()) {
                senderShinglerPut = senderShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.SENDER)
                    .add(GeneralShinglerClient.URI, senderShinglerPut, HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.SENDER, senderShinglerPut);
            }
        }
        if (shinglers.contains(ShinglerType.URL)) {
            String urlShinglerPut = fileToString(URL_SHINGLER_PUT.replace('1', c));
            if (urlShinglerPut != null && !urlShinglerPut.isEmpty()) {
                urlShinglerPut = urlShinglerPut.replaceAll("\\s+", "");
                cluster.shingler(ShinglerType.URL).add(GeneralShinglerClient.URI, urlShinglerPut, HttpStatus.SC_OK);
                shinglersQueries.put(ShinglerType.URL, urlShinglerPut);
            }
        }
        return shinglersQueries;
    }

    protected String sologIndexMock(final String uid, final String queueid, final int n) throws Exception {
        final String c = Integer.toString(n);
        final long prefix = Long.parseLong(uid);
        final String sologData = fileToString(SOLOG_DATA_JSON.replace("1", n < 1 ? "1" : c)).replace("\"", "\\\"");
        cluster.testLucene().add(
            new LongPrefix(prefix),
            "\"url\":\"solog_" + uid + "_" + queueid + "\",\"solog_smtp_id\":\"" + queueid + "\","
                + "\"solog_data\":\"" + sologData + "\",\"solog_route\":null,\"solog_so_res\":null");
        return sologData;
    }

    protected void sologgerMock(final String queueid, final String uid, final Route route, final String filePath)
        throws URISyntaxException
    {
        cluster.sologger().add(
            SOLOGGER_URI + queueid + "&rcpt_uid=" + uid + "&route=" + route.name().toLowerCase(Locale.ROOT),
            new File(getClass().getResource(filePath).toURI()));
    }

    protected void sologgerMock(final List<String> queueIds, final String uid, final Route route, final String filePath)
        throws URISyntaxException
    {
        cluster.sologger().add(
            SOLOGGER_URI + String.join(",", queueIds) + "&rcpt_uid=" + uid + "&route="
                + route.name().toLowerCase(Locale.ROOT),
            new File(getClass().getResource(filePath).toURI()));
    }

    @SuppressWarnings("unused")
    protected void abusesIndexMock(final String uid, final String queueid, final String stid, final String abuseType)
        throws Exception
    {
        final long prefix = Long.parseLong(uid);
        cluster.testLucene().add(
            new LongPrefix(prefix),
            "\"url\":\"" + UpdateDataExecutor.abusesUrl(uid, stid, abuseType) + "\",\"abuses_uid\":" + uid + ",\""
                + "abuses_smtp_id\":\"" + queueid + "\",\"abuses_type\":\"" + abuseType + "\"");
    }

    @SuppressWarnings("StringSplitter")
    protected static void checkComplLogFlags(final Path complLogPath, final Set<Flags> expectedFlags) throws Exception {
        List<String> lines = Files.readAllLines(complLogPath, StandardCharsets.UTF_8);
        List<String> fields = Arrays.asList(lines.get(0).split("\t"));
        Assert.assertEquals(16, fields.size());
        Set<Flags> flags = new HashSet<>();
        for (final String flagStr : fields.get(5).split("_")) {
            if (flagStr.isEmpty()) {
                continue;
            }
            Flags flag = Flags.valueOf("_" + flagStr);
            flags.add(flag);
            if (!expectedFlags.contains(flag)) {
                throw new AssertionError("Unexpected flag '_" + flagStr + "' in compl-log!");
            }
        }
        for (final Flags f : expectedFlags) {
            if (!flags.contains(f)) {
                throw new AssertionError("Absent flag '" + f.name() + "' in compl-log!");
            }
        }
    }

    protected String fileToString(final String fileName) throws Exception {
        URL url = getClass().getResource(fileName);
        if (url == null) {
            return null;
        }
        Path path = Paths.get(url.toURI());
        return Files.readString(path);
    }

    public static void waitShinglerRequests(
        final TestShinglerServer server,
        final String uri,
        final String body,
        final int count)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (server.accessCount(uri, body) != count) {
            Thread.sleep(INTERVAL);
            if (System.currentTimeMillis() - start > 2 * TIMEOUT) {
                throw new TimeoutException(server.getName() + ": expecting " + count + " requests to " + uri
                    + " but got " + server.accessCount(uri, body));
            }
        }
    }

    public void waitShinglersRequests(
        final Map<ShinglerType, String> shinglersQueries,
        Route route,
        int count,
        boolean isFreemail)
        throws Exception
    {
        logger.info("waitShinglersRequests: shinglers="
            + shinglersQueries.keySet().stream().map(ShinglerType::name).collect(Collectors.toList()));
        Set<ShinglerType> shinglers =
            new HashSet<>(Set.of(ShinglerType.ACTIVITY, ShinglerType.COMPL, ShinglerType.SENDER, ShinglerType.URL));
        if (route == Route.OUT && isFreemail) {
            shinglers.add(ShinglerType.FREEMAIL);
        }
        for (final ShinglerType shinglerType : shinglers) {
            if (shinglersQueries.containsKey(shinglerType)) {
                waitShinglerRequests(
                    cluster.shingler(shinglerType),
                    GeneralShinglerClient.URI,
                    shinglersQueries.get(shinglerType),
                    count);
            }
        }
        if (route == Route.IN && shinglersQueries.containsKey(ShinglerType.MASS_IN)) {
            waitShinglerRequests(
                cluster.shingler(ShinglerType.MASS_IN),
                shinglersQueries.get(ShinglerType.MASS_IN),
                null,
                count);
        }
        if (route == Route.OUT && shinglersQueries.containsKey(ShinglerType.MASS_OUT)) {
            waitShinglerRequests(
                cluster.shingler(ShinglerType.MASS_OUT),
                shinglersQueries.get(ShinglerType.MASS_OUT),
                null,
                count);
        }
    }

    protected void waitAndCheckAbusesFromLucene(
        final String uid,
        final Map<String, ?> counters,
        final String spamType,
        final List<String> stids)
        throws Exception
    {
        QueryConstructor searchQuery = new QueryConstructor("/search?");
        searchQuery.append("prefix", uid);
        searchQuery.append("service", IexProxyCluster.QUEUE_NAME);
        searchQuery.append("sort", "url");
        searchQuery.append("text",  "url:abuses_*");
        searchQuery.append("get", "url,"
            + counters.keySet().stream().map(x -> "abuses_" + x).collect(Collectors.joining(",")));
        String search = searchQuery.toString();

        List<JsonChecker> checkers = new ArrayList<>();
        if (!counters.containsKey("type")) {
            Thread.sleep(TIMEOUT >> 4);
            checkers.add(new JsonChecker(EMPTY_SEARCH_RESULT));
        } else {
            int[] indexes = new int[stids.size()];
            List<String> stids2 = new ArrayList<>();
            for (int i = 0; i < stids.size(); i++) {
                indexes[i] = 0;
                stids2.add(stids.get(i));
            }
            int i = 0;
            String beginning = "{\"hitsCount\":" + stids.size() + ",\"hitsArray\":[";
            String hitsArray = fillHitsArray(uid, counters, spamType, stids2);
            checkers.add(new JsonChecker(beginning + hitsArray + "]}"));
            while (i < stids2.size()) {     // going through all the other permutations of stids
                if (indexes[i] < i) {
                    swap(stids2, i % 2 == 0 ?  0: indexes[i], i);
                    hitsArray = fillHitsArray(uid, counters, spamType, stids2);
                    checkers.add(new JsonChecker(beginning + hitsArray + "]}"));
                    indexes[i]++;
                    i = 0;
                } else {
                    indexes[i] = 0;
                    i++;
                }
            }
        }
        long start = System.currentTimeMillis();
        while (true) {
            String output = cluster.testLucene().getSearchOutput(search);
            String compareResult = null;
            boolean found = checkers.size() < 1;
            for (JsonChecker checker : checkers) {
                compareResult = checker.check(output);
                if (compareResult == null) {    // if only any permutation succeeds - all checking is successful
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
            if (System.currentTimeMillis() - start > TIMEOUT) {
                Assert.fail(compareResult);
            }
            Thread.sleep(TIMEOUT >> 4);
        }
    }

    private String fillHitsArray(
        final String uid,
        final Map<String, ?> counters,
        final String spamType,
        final List<String> stids)
    {
        int j;
        String val;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"url\":\"").append(UpdateDataExecutor.abusesUrl(uid, stids.get(i), spamType));
            sb.append("\",");
            j = 0;
            for (Map.Entry<String, ?> entry : counters.entrySet()) {
                if (j++ > 0) {
                    sb.append(',');
                }
                sb.append("\"abuses_").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof List) {
                    val = ((List) entry.getValue()).get(i).toString();
                } else {
                    val = entry.getValue().toString();
                }
                sb.append(val.isEmpty() ? null : ("\"" + val + "\""));
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private void swap(final List<String> array, final int i, final int j) {
        String tmp = array.get(i);
        array.set(i, array.get(j));
        array.set(j, tmp);
    }

    protected void waitAndCheckAbusesFromLucene(
        final String uid,
        Map<String, ?> counters,
        final String spamType,
        final String stid)
        throws Exception
    {
        waitAndCheckAbusesFromLucene(uid, counters, spamType, List.of(stid));
    }

    protected void waitAndCheckSologInfoFromLucene(
        final String uid,
        final List<String> queueIds,
        final Map<String, ?> counters,
        final boolean found)
        throws Exception
    {
        String queueId = counters.containsKey(SMTP_ID) ? counters.get(SMTP_ID).toString() : queueIds.get(0);
        HashMap<String, Object> fields = new HashMap<>(counters);
        fields.put("route", null);
        fields.put("so_res", null);
        waitAndCheckInfoFromLucene(
            uid,
            uid + "_" + queueId,
            UserActionHandler.IndexDataType.SOLOG,
            queueIds,
            fields,
            found);
    }

    protected void waitAndCheckUserDayAbusesInfoFromLucene(
        final String uid,
        final String stid,
        final String abuseType,
        final boolean found)
        throws Exception
    {
        waitAndCheckInfoFromLucene(
            uid,
            uid + "_" + stid + "_" + abuseType,
            UserActionHandler.IndexDataType.USER_DAY_ABUSES,
            null,
            Map.of("cnt", 1),
            found);
    }

    protected void waitAndCheckInfoFromLucene(
        final String uid,
        final String url,
        final UserActionHandler.IndexDataType dataType,
        final List<String> queueIds,
        final Map<String, ?> counters,
        final boolean found)
        throws Exception
    {
        String search =
            UserActionHandler.getIndexRequest(uid, IexProxyCluster.QUEUE_NAME, queueIds, dataType, null, false);
        JsonChecker checker;
        if (found) {
            checker = new JsonChecker("{" +
                "\"hitsCount\":1," +
                "\"hitsArray\":[{" +
                "\"url\":\"" + dataType.keyPrefix() + url + "\","
                + counters.entrySet().stream().map(
                    x -> "\"" + dataType.keyPrefix() + x.getKey() + "\":"
                        + (x.getValue() == null || x.getValue().toString().isEmpty()
                            ? "null" : ("\"" + x.getValue() + "\"")))
                .collect(Collectors.joining(","))
                + " }]}");
        } else {
            Thread.sleep(TIMEOUT >> 4);
            checker = new JsonChecker(EMPTY_SEARCH_RESULT);
        }
        long start = System.currentTimeMillis();
        while (true) {
            String output = cluster.testLucene().getSearchOutput(search);
            String compareResult = checker.check(output);
            if (compareResult == null) {
                break;
            }
            if (System.currentTimeMillis() - start > TIMEOUT) {
                Assert.fail(compareResult);
            }
            Thread.sleep(TIMEOUT >> 4);
        }
    }

    protected void testTrustedComplaintSignals(
        final int expectedRequests,
        final int expectedFreshRequests,
        final int expectedHam,
        final int expectedSpam)
        throws IOException
    {
        String stats = HttpAssert.stats(cluster.iexproxy().host());
        HttpAssert.assertStat(
            "complaints-spam-trusted-requests_ammm",
            Integer.toString(expectedRequests),
            stats,
            IexProxyCluster.MAX_UNISTAT_SIGNALS);
        HttpAssert.assertStat(
            "complaints-spam-trusted-fresh-requests_ammm",
            Integer.toString(expectedFreshRequests),
            stats,
            IexProxyCluster.MAX_UNISTAT_SIGNALS);
        HttpAssert.assertStat(
            "complaints-ham-requests_ammm",
            Integer.toString(expectedHam),
            stats,
            IexProxyCluster.MAX_UNISTAT_SIGNALS);
        HttpAssert.assertStat(
            "complaints-spam-requests_ammm",
            Integer.toString(expectedSpam),
            stats,
            IexProxyCluster.MAX_UNISTAT_SIGNALS);
    }
}

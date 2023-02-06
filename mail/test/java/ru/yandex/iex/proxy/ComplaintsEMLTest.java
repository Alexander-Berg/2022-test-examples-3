package ru.yandex.iex.proxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.collection.Pattern;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.iex.proxy.complaints.MailMessageContext;
import ru.yandex.iex.proxy.move.UpdateDataExecutor;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.document.mail.FirstlineMailMetaInfo;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;

public class ComplaintsEMLTest extends EMLTestBase {
    private static final String RESORCE_SUBFOLDER = "complaints";
    private static final String CRM_FILE1 = "crm1.eml";
    private static final String EXCHANGE_FILE1 = "exchange1.eml";
    private static final String EXCHANGE_FILE2 = "exchange2.eml";
    private static final String FBL_FILE1 = "fbl1.eml";
    private static final String SETTINGS_JSON = "settings_data.json";
    private static final String SETTINGS_URI = "/get?&uid=";
    private static final String COMPLAINT_URI = "/fbl-out*";
    private static final String BLACKBOX_URI = "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
        + "&dbfields=subscription.suid.2&sid=smtp&login=";
    private static final String CRM = "crm";
    private static final String EXCHANGE = "exchange";
    private static final String FBL = "fbl";
    private static final String PERS_FILTERS = "pfilters";
    private static final String ABUSES = "abuses";
    private static final String SIGNAL_CRM = "complaints-crm-spam_ammm";
    private static final String SIGNAL_EXCHANGE_HAM = "complaints-exchange-ham_ammm";
    private static final String SIGNAL_EXCHANGE_SPAM = "complaints-exchange-spam_ammm";
    private static final String SIGNAL_FBL = "complaints-fbl-spam_ammm";
    private static final String SIGNAL_SO_MAILLIST = "complaints-so-maillist-spam_ammm";
    private static final String SIGNAL_UNKNOWN = "complaints-unknown_ammm";
    private static final long TIMEOUT = 2000L;
    private static final byte[] SKIP_MARK = "\n</message>\n".getBytes(StandardCharsets.UTF_8);
    private final Map<String, String> maillist = Map.of(
        "exchange-spam-reports@yandex-team.ru", "1120000000038804",
        "exchange-ham-reports@yandex-team.ru",  "1120000000039071",
        "crm-spam-report@yandex-team.ru",       "1120000000216280",
        "so@yandex-team.ru",                    "1120000000001457",
        "fbl-arf@yandex.ru",                    "133765553"
    );
    private final Map<String, String> emails = Map.of(
        "abovsky@yandex-team.ru",             "1120000000013893",
        "copyright-complaint@yandex-team.ru", "1120000000034331",
        "avonsokolova@yandex.ru",             "85047460"
    );
    private final Map<String, String> sourceDomains = Map.of(
        CRM_FILE1, "yandex.net",
        EXCHANGE_FILE1, "constantcontact.com",
        EXCHANGE_FILE2, "outlook.com",
        FBL_FILE1, "yandex.net"
    );
    private final Map<String, String> queueIds = Map.of(
        CRM_FILE1, "ohdTpLcjAk-8l4WApIm",
        EXCHANGE_FILE1, "BrmmHKaOxn-UMO8ARuu",
        EXCHANGE_FILE2, "gajub0A3Bs-bhj03uJ7",
        FBL_FILE1, "o0ElzD9TEd-77janX7d"
    );
    private final Map<String, String> complainaintUids = Map.of(
        CRM_FILE1, "1120000000034331",
        EXCHANGE_FILE1, "1120000000013893",
        EXCHANGE_FILE2, "1120000000013893",
        FBL_FILE1, "85047460"
    );

    @Override
    public String factname() {
        return "eml";
    }

    @Override
    public String configExtra() {
        return "entities_rcpt_email.sales3@phucnguyenmedia$com = eml\n"
            + "postprocess_rcpt_email.sales3@phucnguyenmedia$com = "
            + "complaint:http://localhost:" + IexProxyCluster.IPORT
                + "/complaint\n";
                //can't test /bounce now
                //check only "cokedump"
//                + "postprocess.message-type-8 = "
//                + " ticket:http://localhost:" + IexProxyCluster.IPORT
//                + "/bounce\n";
    }

    private String emlToString(final File file, final boolean skipXml)
        throws IOException
    {
        if (!skipXml) {
            return fileToString(file);
        }
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file)))
        {
            in.mark(1);
            int b = in.read();
            in.reset();
            if (b == (int) '<') {
                IOStreamUtils.skipTo(in, SKIP_MARK);
            }
            return new String(IOStreamUtils.consume(in).toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Override
    protected void prepareCluster(
        final IexProxyCluster cluster,
        final File eml,
        final FirstlineMailMetaInfo meta)
        throws Exception
    {
        super.prepareCluster(cluster, eml, meta);
        cluster.complaints().add(COMPLAINT_URI, HttpStatus.SC_OK);
        cluster.freemail().add("/api/v1*", HttpStatus.SC_OK);
    }

    @Override
    public Set<String> checkEntities() {
        return new HashSet<>(Arrays.asList("_eml", "complaint"));
    }

    @Override
    protected long getUid(final String to) {
        final String uid = maillist.get(to);
        if (uid == null) {
            return UID_VALUE;
        }
        return Long.parseLong(uid);
    }

    public static String getLogin(final String email) {
        String login = email;
        Matcher m = MailMessageContext.RE_YANDEX_EMAIL.matcher(login);
        if (m.find()) {
            login = m.group(1);
        }
        return login;
    }

    @Test
    public void testCrmComplaint1() throws Exception {
        testComplaint(CRM, CRM_FILE1);
    }

    @Test
    public void testExchangeComplaint1() throws Exception {
        testComplaint(EXCHANGE, EXCHANGE_FILE1);
    }

    @Test
    public void testExchangeComplaint2() throws Exception {
        testComplaint(EXCHANGE, EXCHANGE_FILE2);
    }

    @Test
    public void testFblComplaint1() throws Exception {
        testComplaint(FBL, FBL_FILE1);
    }

    private void testComplaint(final String complaintsKind, final String fileName) throws Exception
    {
        final URL factDirUrl = this.getClass().getResource(RESORCE_SUBFOLDER);
        Assert.assertNotNull(factDirUrl);

        try (IexProxyCluster cluster = new IexProxyCluster(this,true, true))
        {
            cluster.iexproxy().start();
            final String filePath = RESORCE_SUBFOLDER + "/" + fileName;
            logger.info("ComplaintsEMLTest.testComplaints testing file: " + filePath);
            final URL fileUrl = getClass().getResource(filePath);
            Assert.assertNotNull(fileUrl);
            testComplaint(cluster, complaintsKind, new File(fileUrl.toURI()));
        }
    }

    private void testComplaint(final IexProxyCluster cluster, final String complaintsKind, final File eml)
        throws Exception
    {
        final FirstlineMailMetaInfo meta = prepareStorage(cluster, eml, RESORCE_SUBFOLDER, false);
        logger.info("testComplaint: to='" + meta.get(MailMetaInfo.TO) + "', normalizedTo='"
            + meta.get(MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED).trim() + "'");
        long maillistUid = getUid(meta.get(MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED).trim());
        String uid = complaintsKind.equals(FBL) ? Long.toString(maillistUid) : complainaintUids.get(eml.getName());
        long lUid = Long.parseLong(uid);
        final String queueid = queueIds.get(eml.getName());
        logger.info("testComplaint: uid=" + uid + ", queueid=" + queueid + ", emails"
            + String.join(",", emails.values()));
        prepareCluster(cluster, eml, meta);
        cluster.testLucene().add(
            new LongPrefix(lUid),
            "\"url\":\"user_url_" + uid + "\",\"uid\":" + uid + ",\"stid\":\"" + FAKE_STID
                + "\",\"smtp_id\":\"" + queueid + "\",\"mid\":\"" + FAKE_MID + "\",\"all_smtp_ids\":\"" + queueid
                + "\"");
        settingsApiMock(cluster, uid);
        for (Map.Entry<String, String> email : emails.entrySet()) {
            long uid2 = Long.parseLong(email.getValue());
            if (BlackboxUserinfo.corp(uid2)) {
                cluster.corpBlackbox().add(
                    BLACKBOX_URI + email.getKey(),
                    new StaticHttpResource(
                        HttpStatus.SC_OK,
                        new StringEntity(IexProxyCluster.blackboxResponse(uid2, email.getKey()))));
            } else {
                cluster.blackboxDirect().add(
                    BLACKBOX_URI + email.getKey(),
                    new StaticHttpResource(
                        HttpStatus.SC_OK,
                        new StringEntity(IexProxyCluster.blackboxResponse(uid2, email.getKey()))));
            }
            // online handler
            cluster.onlineDB().add("/online?uid=" + email.getValue(), new StaticHttpResource(new OnlineHandler(true)));
        }
        // producerAsyncClient add requests handlers
        cluster.producerAsyncClient().add("/complaint*", new ProxyHandler(cluster.iexproxy().port()));
        cluster.producerAsyncClient().register(
            new Pattern<>("/update", false),
            new ExpectingHeaderHttpItem(
                new ProxyHandler(cluster.testLucene().indexerPort()),
                YandexHeaders.SERVICE,
                (complaintsKind.equals(FBL) ? "" : "corp_") + "change_log"),
            RequestHandlerMapper.POST);

        final String complaintResponse = sendComplaint(cluster, eml, meta, maillistUid);
        logger.info("ComplaintsEMLTest.testComplaint: complaint's handler response = " + complaintResponse);
        final JsonMap reponseMap = TypesafeValueContentHandler.parse(complaintResponse).asMap();
        final String recipientEmail = reponseMap == null ? null : reponseMap.get("recipient_email").asStringOrNull();
        final String senderEmail = reponseMap == null ? null : reponseMap.get("sender_email").asStringOrNull();
        //final String senderHost = reponseMap == null ? null : reponseMap.get("sender_host").asStringOrNull();
        final String sourceDomain = reponseMap == null ? null : reponseMap.get("source_domain").asStringOrNull();
        final String expectedSourceDomain = sourceDomains.get(eml.getName());
        Assert.assertEquals(expectedSourceDomain.isEmpty() ? null : expectedSourceDomain, sourceDomain);
        final Long receivedDate = reponseMap == null ? null : reponseMap.get("received_date").asLong();
        final Long operationDate = Long.parseLong(meta.get(MailMetaInfo.RECEIVED_DATE));
        if (!complaintsKind.equals(FBL) && recipientEmail != null) {
            uid = emails.getOrDefault(recipientEmail, "0");
        }
        final String spamType = reponseMap == null ? null : reponseMap.get("spam_type").asString().toLowerCase();
        logger.info("ComplaintsEMLTest.testComplaint: recipientEmail=" + recipientEmail + ", senderEmail="
            + senderEmail + ", spamType=" + spamType + ", uid=" + uid + ", sourceDomain=" + sourceDomain);
        cluster.waitProducerRequests(cluster.complaints(), COMPLAINT_URI, 1);
        String stats = HttpAssert.stats(cluster.iexproxy().port());
        if (complaintsKind.equals(FBL)) {
            cluster.waitProducerRequests(cluster.corpBlackbox(),BLACKBOX_URI + recipientEmail, 0);
            cluster.waitProducerRequests(cluster.blackboxDirect(),BLACKBOX_URI + recipientEmail, 0);
            waitAndCheckCountersFromLucene(cluster, uid, Map.of(
                "last_timestamp", operationDate
            ), PERS_FILTERS, senderEmail, sourceDomain);
        } else {
            if (BlackboxUserinfo.corp(Long.parseLong(uid))) {
                cluster.waitProducerRequests(cluster.corpBlackbox(),BLACKBOX_URI + recipientEmail, 1);
            } else {
                cluster.waitProducerRequests(cluster.blackboxDirect(),BLACKBOX_URI + recipientEmail, 1);
            }
            waitAndCheckCountersFromLucene(cluster, uid, Map.of(
                "last_type", spamType,
                "last_timestamp", operationDate,
                "spams", spamType.equals("spam") ? "1" : "",
                "hams", spamType.equals("spam") ? "" : "1"
            ), PERS_FILTERS, senderEmail, sourceDomain);
        }
        testComplaintsSignals(stats, complaintsKind, spamType);
        waitAndCheckCountersFromLucene(cluster, uid, Map.of(
            "type", spamType,
            "first_timestamp", 0,
            "last_timestamp", operationDate,
            "msg_date", receivedDate,
            "source", complaintsKind.toUpperCase(),
            "cnt", "1"
        //), ABUSES, meta.get(MailMetaInfo.STID), spamType);
        ), ABUSES, FAKE_STID, spamType);
        HttpAssert.assertStat(SIGNAL_UNKNOWN, "0", stats, IexProxyCluster.MAX_UNISTAT_SIGNALS);
    }

    private String sendComplaint(
        final IexProxyCluster cluster,
        final File eml,
        final FirstlineMailMetaInfo meta,
        final long uid)
        throws IOException, HttpException
    {
        String response = null;
        // /complaint?&subject=Mail.ru+abuse+report+(Feedback+Loop)&email=noreply@corp.mail.ru
        // &user_email=fbl-arf@yandex.ru&received_date=1590087486&uid=1120000000017116&suid=1120000000132972&mdb=pg
        // &pgshard=113&mid=172544160774859755&stid=320.mail:1120000000008559.E1038814:21133367906983260942514247073
        // &firstline=This+is+an+email+abuse+report+for+an+email+message+received+from&folder_name=Yandex%7Cso-fbl
        // &folder_type=user&types=8,29,46,55,102
        QueryConstructor query =
            new QueryConstructor(new StringBuilder(HTTP_LOCALHOST + cluster.iexproxy().port() + "/complaint?"));
        query.append(MailMetaInfo.SUBJECT, meta.get(MailMetaInfo.SUBJECT));
        query.append("email", meta.get(MailMetaInfo.FROM));
        query.append("user_email", meta.get(MailMetaInfo.TO));
        query.append(MailMetaInfo.RECEIVED_DATE, meta.get(MailMetaInfo.RECEIVED_DATE));
        query.append(MailMetaInfo.UID, uid);
        query.append(MailMetaInfo.MDB, "pg");
        query.append(MailMetaInfo.MID, meta.get(MailMetaInfo.MID));
        query.append(MailMetaInfo.STID, meta.get(MailMetaInfo.STID));
        query.append(MailMetaInfo.FIRSTLINE, meta.get(MailMetaInfo.FIRSTLINE));
        query.append("folder_name", "Inbox");
        query.append("folder_type", "user");
        Set<String> types = meta.messageTypes().stream().map(String::valueOf).collect(Collectors.toSet());
        query.append("types", String.join(",", types));
        Map<String, Object> bodyMap = new HashMap<>(Map.of(
            "eml", new HashMap<>(Map.of("message_body", emlToString(eml, true)))
        ));
        String body = JsonType.NORMAL.toString(bodyMap);
        logger.info("ComplaintsEMLTest.sendComplaint BODY: " + body);
        HttpPost post = new HttpPost(query.toString());
        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse httpResponse = client.execute(post)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                httpResponse.getEntity().writeTo(out);
                response = out.toString();
                //System.err.println("ComplaintsEMLTest.sendComplaint: Entity returned: " + response);
            }
        }
        return response;
    }

    private void testComplaintsSignals(
        final String stats,
        final String complaintKind,
        final String spamType)
    {
        final String complaintType = complaintKind.toLowerCase().replace('_', '-');
        final String signalName = "complaints-" + complaintType + '-' + spamType + "_ammm";
        final ArrayList<String> signals =
            new ArrayList<>(
                Arrays.asList(SIGNAL_CRM, SIGNAL_EXCHANGE_HAM, SIGNAL_EXCHANGE_SPAM, SIGNAL_FBL, SIGNAL_SO_MAILLIST));
        for (final String signal: signals) {
            if (signal.equals(signalName)) {
                HttpAssert.assertStat(signal, "1", stats, IexProxyCluster.MAX_UNISTAT_SIGNALS);
            } else {
                HttpAssert.assertStat(signal, "0", stats, IexProxyCluster.MAX_UNISTAT_SIGNALS);
            }
        }
    }

    private void waitAndCheckCountersFromLucene(
        final IexProxyCluster cluster,
        final String uid,
        Map<String, ?> counters,
        final String countersType,
        final String key1,
        final String key2)
        throws Exception
    {
        final String urlPrefix = countersType.equals(PERS_FILTERS) ? "pfilters4" : "abuses";
        final String keyType = countersType.equals(PERS_FILTERS) ? "last_type" : "type";
        QueryConstructor searchQuery = new QueryConstructor("/search?");
        searchQuery.append("prefix", uid);
        searchQuery.append("service", "iex");
        searchQuery.append("sort", "url");
        searchQuery.append("text",  "url:" + urlPrefix + "_*");
        searchQuery.append("get", "url,"
            + counters.keySet().stream().map(x -> countersType + "_" + x).collect(Collectors.joining(",")));
        String search = searchQuery.toString();

        JsonChecker checker;
        if (!counters.containsKey(keyType)) {
            Thread.sleep(TIMEOUT >> 4);
            checker = new JsonChecker("{\"hitsCount\":0,\"hitsArray\":[]}");
        } else {
            final String url;
            if (countersType.equals(PERS_FILTERS)) {
                url = urlPrefix + "_" + uid + '_' + key1 + '/' + key2;
            } else {
                url = UpdateDataExecutor.abusesUrl(uid, key1, key2);
            }
            checker = new JsonChecker("{" +
                "\"hitsCount\":1," +
                "\"hitsArray\":[{" +
                    "\"url\":\"" + url + "\","
                    + counters.entrySet().stream().map(
                        x -> "\"" + countersType + "_" + x.getKey() + "\":"
                            + (x.getValue().toString().isEmpty() ? null : ("\"" + x.getValue() + "\"")))
                        .collect(Collectors.joining(","))
                    + " }]}");
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

    private void settingsApiMock(final IexProxyCluster cluster, final String uid) throws URISyntaxException {
        FileEntity entitySettings = new FileEntity(
            new File(getClass().getResource(RESORCE_SUBFOLDER + "/" + SETTINGS_JSON).toURI()),
            ContentType.APPLICATION_JSON);
        if (uid.startsWith("112000")) {
            cluster.corpSettingsApi().add(
                SETTINGS_URI + uid + "&settings_list=show_folders_tabs",
                new StaticHttpResource(HttpStatus.SC_OK, entitySettings));
        } else {
            cluster.settingsApi().add(
                SETTINGS_URI + uid + "&settings_list=show_folders_tabs",
                new StaticHttpResource(HttpStatus.SC_OK, entitySettings));
        }
    }
}

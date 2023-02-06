package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.dbfields.PgFields;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.mail.search.MailSearchDefaults;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.document.mail.FirstlineMailMetaInfo;
import ru.yandex.search.document.mail.JsonFirstlineMailMetaHandler;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public abstract class CalendarTestBase extends TestBase {
    protected static final String SEARCH = "/search?";
    protected static final String HTTP_LOCALHOST = "http://localhost:";
    protected static final String AXIS_URI =
        "/v1/facts/store_batch?client_id=extractors";
    protected static final String PREFIX = "prefix";
    protected static final String SERVICE = "service";
    protected static final String IEX = "iex";
    protected static final String UID = "uid";
    protected static final String TEXT = "text";
    protected static final String GET = "get";
    protected static final String FACT_EVENT_ID = "fact_event_id";
    protected static final String IMPORT_EVENT_JSON =
        "/import_event_by_ics_url.json";
    protected static final String MSG_EML = "/msg.eml";
    protected static final String ENVELOPES_JSON = "/envelopes.json";
    protected static final String FACTS_JSON = "/facts.json";
    protected static final String SEARCH_JSON = "/search.json";

    private static final String CHANGE_LOG = MailSearchDefaults.BP_CHANGE_LOG;
    private static final String NOTIFY = "/notify?";
    private static final String MDB = "mdb";
    private static final String PG = "pg";
    private static final String UID_FIELD = "\"uid\": ";
    private static final String STID = "stid";
    private static final String FAKE_STID =
        "1.632123143.7594801846142779115218810981";
    private static final long TIMEOUT = 10000;
    private static final String OPERATION_DATE_FIELD =
        "\"operation_date\": 123456780";
    private static final String ENTITY_RETURNED_MSG = "Entity returned:\n";

    protected IexProxyCluster cluster;

    protected void sendNotifyIexUpdate(final String... mids) throws Exception {
        QueryConstructor updateCache = new QueryConstructor(NOTIFY);
        updateCache.append(SERVICE, CHANGE_LOG);
        updateCache.append(PREFIX, uid());
        updateCache.append(MDB, PG);
        updateCache.append("update_cache", "event-ticket");
        updateCache.append(PgFields.CHANGE_TYPE, "iex-update");
        String body = '{'
            + UID_FIELD + uid() + ','
            + "\"change_type\": \"iex-update\","
            + OPERATION_DATE_FIELD + ','
            + "\"mdb\": \"pg\","
            + "\"changed\":["
            + Arrays.stream(mids)
                .map(mid -> "{\"mid\": " + mid + '}')
                .reduce((mid1, mid2) -> mid1 + ',' + mid2)
                .orElse("")
            + "]}";

        HttpPost post = new HttpPost(HTTP_LOCALHOST
            + cluster.iexproxy().port()
            + updateCache);
        post.setEntity(new StringEntity(body));
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }

    // CSOFF: MultipleStringLiterals
    protected void sendCalendarLog(
        final String eventId,
        final String recurrenceId)
        throws Exception
    {
        QueryConstructor query = new QueryConstructor("/calendar-log-handle?");
        query.append(
            "topic",
            "rt3.man--calendar-yt--calendar-yt-events-log-json");
        query.append("partition", "0");
        query.append("message-write-time", "1544682655867");
        String recurrenceIdStr;
        if (recurrenceId == null) {
            recurrenceIdStr = "\"recurrence_id\": null,";
        } else {
            recurrenceIdStr = "\"recurrence_id\": \""
                + recurrenceId + '\"' + ',';
        }
        String body =
            '{'
                + "\"host\":\"calcorp-work1o\","
                + "\"event_info\":{"
                    + "\"actor\": \"" + uid() + '\"' + ','
                    + "\"change_type\": \"UPDATE\","
                    + "\"event_id\": 1234567,"
                    + "\"external_id\": \"" + eventId + '\"' + ','
                    + "\"main_event_id\": 1234567,"
                    + recurrenceIdStr
                    + "\"type\": \"EVENT_CHANGE\","
                    + "\"users\": {"
                    + "\"added\": ["
                        + '{'
                        + "\"availability\": \"BUSY\","
                        + "\"decision\": \"YES\","
                        + "\"email\": \"arcadiy@yandex.ru\","
                        + UID_FIELD + uid()
                        + '}'
                        + ']'
                    + '}'
                + '}'
            + '}'
            + '{'
                + "\"host\":\"calcorp-work1o\","
                + "\"event_info\":{"
                    + "\"actor\": \"" + uid() + '\"' + ','
                    + "\"change_type\": \"UPDATE\","
                    + "\"event_id\": 1234567,"
                    + "\"external_id\": \"" + eventId + '\"' + ','
                    + "\"main_event_id\": 1234567,"
                    + recurrenceIdStr
                    + "\"type\": \"EVENT_CHANGE\","
                    + "\"users\": {"
                    + "\"updated\": ["
                        + '{'
                        + "\"availability\": \"BUSY\","
                        + "\"decision\": \"YES\","
                        + "\"email\": \"arcadiy@yandex.ru\","
                        + UID_FIELD + uid()
                        + "},"
                        + '{'
                        + "\"availability\": \"MAYBE\","
                        + "\"decision\": \"UNDECIDED\","
                        + "\"email\": \"arcadiy2@yandex.ru\","
                        + UID_FIELD + (uid() + 1)
                        + '}'
                        + ']'
                    + '}'
                + '}'
            + '}';
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST
            + cluster.iexproxy().port()
            + query);
        post.setEntity(new StringEntity(body));
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println(ENTITY_RETURNED_MSG + entityString);
            }
        }
    }
    // CSON: MultipleStringLiterals

    protected void checkFacts(
        final String fileName,
        final String... mids) throws Exception
    {
        QueryConstructor uri = new QueryConstructor(
            HTTP_LOCALHOST + cluster.iexproxy().port() + factsUri());
        for (String mid: mids) {
            uri.append("mid", mid);
        }
        HttpGet getFacts = new HttpGet(uri.toString());
        String facts;
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            try (CloseableHttpResponse response = client.execute(getFacts)) {
                facts = CharsetUtils.toString(response.getEntity());
                System.out.println("/facts returned:\n" + facts);
            }
        }
        cluster.compareJson(fileName, facts, false);
    }

    protected void waitAndCheckFacts(
        final String fileName,
        final String... mids) throws Exception
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TIMEOUT) {
            try {
                checkFacts(fileName, mids);
            } catch (AssertionError error) {
                continue;
            }
            return;
        }
        checkFacts(fileName, mids);
    }

    private void checkSearch(final String fileName, final String eventId)
        throws Exception
    {
        QueryConstructor searchQuery = new QueryConstructor(SEARCH);
        searchQuery.append(PREFIX, uid());
        searchQuery.append(SERVICE, IEX);
        searchQuery.append("sort", "fact_mid");
        searchQuery.append("asc", "true");
        searchQuery.append(TEXT, FACT_EVENT_ID + ':' + eventId);
        searchQuery.append(GET, "url,fact_uid,fact_stid,fact_name,fact_mid,"
            + "fact_received_date,fact_message_type,fact_is_coke_solution,"
            + "fact_from,fact_event_id,fact_event_recurrence_id,fact_domain,"
            + "fact_data");
        String search = searchQuery.toString();
        String searchOutput = cluster.testLucene().getSearchOutput(search);
        cluster.compareJson(fileName, searchOutput, false);
    }

    protected void waitAndCheckSearch(
        final String fileName,
        final String eventId)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TIMEOUT) {
            try {
                checkSearch(fileName, eventId);
            } catch (AssertionError error) {
                continue;
            }
            return;
        }
        checkSearch(fileName, eventId);
    }

    protected void importEventResponseMock(final String fileName)
        throws Exception
    {
        URL eventEntityURL = getClass().getResource(fileName);
        FileEntity eventEntity = new FileEntity(
            new File(eventEntityURL.toURI()),
            ContentType.APPLICATION_JSON);
        StaticServer calendar;
        if (BlackboxUserinfo.corp(uid())) {
            calendar = cluster.calendarTools();
        } else {
            calendar = cluster.calendar();
        }
        calendar.add(
            calendarUrl(),
            new StaticHttpResource(HttpStatus.SC_OK, eventEntity));
    }

    protected void filterSearchResponseMock(
        final String fileNameEml,
        final String fileNameEnvelopes,
        final String... mids) throws Exception
    {
        URL fsInvURL = getClass().getResource(fileNameEnvelopes);
        final JsonMap fsInvRoot =
            TypesafeValueContentHandler.parse(
                fileToString(new File(fsInvURL.toURI()))).asMap();
        IexProxyTestMocks.filterSearchMock(
            cluster,
            uid(),
            JsonType.NORMAL.toString(fsInvRoot),
            mids);

        final JsonList envelopes = fsInvRoot.get("envelopes").asList();
        final JsonMap envelope = envelopes.get(0).asMap();
        envelope.put(STID, new JsonString(FAKE_STID));
        FirstlineMailMetaInfo meta = new FirstlineMailMetaInfo();
        new JsonFirstlineMailMetaHandler(meta).handle(envelope);
    }

    // CSOFF: MultipleStringLiterals
    // CSOFF: ParameterNumber
    protected void addFactToLucene(
        final String mid,
        final String eventId,
        final String recurrenceId,
        final String receivedDate)
        throws IOException
    {
        String doc =
            "\"url\": \"facts_" + uid() + '_' + mid + "_event-ticket\","
            + "\"fact_uid\": " + uid() + ','
            + "\"fact_stid\": \"320.mail:" + uid()
                + ".E1012402:4131466600177705023448835500537\","
            + "\"fact_name\": \"event-ticket\","
            + "\"fact_mid\": \"" + mid + '\"' + ','
            + "\"fact_received_date\": \"" + receivedDate + '\"' + ','
            + "\"fact_message_type\": \"56 trust_6 42 invite\","
            + "\"fact_is_coke_solution\": \"false\","
            + "\"fact_from\": \"info@" + domain() + '\"' + ','
            + "\"fact_event_id\": \"" + eventId + '\"' + ','
            + "\"fact_event_recurrence_id\": \"" + recurrenceId + '\"' + ','
            + "\"fact_domain\": \"" + domain() + '\"' + ','
            + "\"fact_data\": \"{"
                + "\\\"externalEventId\\\":\\\"" + eventId + "\\\","
                + "\\\"recurrenceEventId\\\":\\\"" + recurrenceId + "\\\","
                + "\\\"isCancelled\\\":" + false + ','
                + "\\\"start_date_ts\\\":1479204000000,"
                + "\\\"special_parts\\\":[\\\"1.2\\\"],"
                + "\\\"widget_subtype\\\":\\\"calendar\\\","
                + "\\\"origin\\\":\\\"ics\\\","
                + "\\\"domain\\\":\\\"" + domain() + "\\\","
                + "\\\"end_date_ts\\\":1479214800000,"
                + "\\\"location\\\":\\\"\\\","
                + "\\\"title\\\":\\\"Test event\\\""
            + "}\"";
        cluster.testLucene().add(new LongPrefix(uid()), doc);
    }
    // CSON: MultipleStringLiterals
    // CSON: ParameterNumber

    protected void attachSidMock() throws UnsupportedEncodingException {
        String entity = "{\"result\":[{\"uid\":\"" + uid() + "\",\"sids\":["
            + "\"ak7sh492jf5yd63jd383h\"]}]}";
        StaticServer attachSid;
        if (BlackboxUserinfo.corp(uid())) {
            attachSid = cluster.corpAttachSid();
        } else {
            attachSid = cluster.attachSid();
        }
        attachSid.add(
            "/attach_sid",
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(entity)));
    }

    private String fileToString(final File file) throws IOException {
        return Files.readString(file.toPath());
    }

    private String factsUri() {
        return "/facts?mdb=pg&uid=" + uid() + "&cokedump&extract";
    }

    protected abstract long uid();

    protected abstract String domain();

    protected String calendarUrl() {
        return "/api/mail/importEventByIcsUrl?*";
    }
}


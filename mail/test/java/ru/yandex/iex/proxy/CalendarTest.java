package ru.yandex.iex.proxy;

import java.io.File;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.request.RequestHandlerMapper;

public class CalendarTest extends CalendarTestBase {
    private static final String RESOURCES_PATH = "calendar";
    private static final long UID_VALUE = 588355978L;
    private static final String RECEIVED_DATE = "1519818303";

    @Before
    public void initializeCluster() throws Exception {
        cluster = new IexProxyCluster(this, true);
        cluster.iexproxy().start();

        // blackbox response mock
        String to = "marija.smolina28test@yandex.ru";
        cluster.blackbox().add(
            "/blackbox*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity(
                    IexProxyCluster.blackboxResponse(UID_VALUE, to))));

        // online handler
        final String onlineUri = "/online?uid=" + UID_VALUE;
        cluster.onlineDB().add(
            onlineUri,
            new StaticHttpResource(new OnlineHandler(true)));

        // enlarge response mock
        final String msearchUri = "/api/async/enlarge/your?uid=" + UID_VALUE;
        cluster.msearch().add(
            msearchUri,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity("")));

        // status response mock
        cluster.producer().add(
            "/_status*",
            new StaticHttpResource(
                HttpStatus.SC_OK,
                new StringEntity("[{\"localhost\":-1}]")));

        // axis response mock
        cluster.axis().add(AXIS_URI, HttpStatus.SC_OK);

        // producerAsyncClient add requests handlers
        cluster.producerAsyncClient().add(
            "/notify*",
            new ProxyHandler(cluster.iexproxy().port()));
        cluster.producerAsyncClient().add(
            "/calendar-log-handle*",
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
            new Pattern<>("/update", false),
            new ProxyHandler(cluster.testLucene().indexerPort()),
            RequestHandlerMapper.POST);
        cluster.cokemulatorIexlib().add(
            "/process?*",
            new StaticHttpItem(
                HttpStatus.SC_OK,
                "{\"contentline\":{\"weight\":1,\"text\":\"\"}}"));
        attachSidMock();
    }

    @Test
    public void testSimpleEvent() throws Exception {
        String path = RESOURCES_PATH + "/test-simple-event";
        String mid = "165507286305865987";
        String mid2 = "165507286305865988";
        String eventId = "5hN99zjryandex.ru";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid2);

        addFactToLucene(mid, eventId, "", RECEIVED_DATE);
        sendNotifyIexUpdate(mid2);

        cluster.waitProducerRequests(
            cluster.calendar(),
            calendarUrl(),
            1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckFacts(path + FACTS_JSON, mid, mid2);
    }

    @Test
    public void testSeriesEvent() throws Exception {
        String path = RESOURCES_PATH + "/test-series-event";
        String mid = "165507286305865989";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        sendNotifyIexUpdate(mid);

        cluster.waitProducerRequests(
            cluster.calendar(),
            calendarUrl(),
            1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Test
    public void testOneOfSeriesEvent() throws Exception {
        String path = RESOURCES_PATH + "/test-one-of-series-event";
        String mid = "165507286305865978";
        String eventId = "7731116s31phbu3el8rfisuvn3@google.com";
        String recId = "2018-05-25T21:00:00.000Z";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        addFactToLucene("165507286305865976", eventId, recId, "1519818300");
        addFactToLucene("165507286305865977", eventId, "", RECEIVED_DATE);
        sendNotifyIexUpdate(mid);

        cluster.waitProducerRequests(
            cluster.calendar(),
            calendarUrl(),
            1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckSearch(path + SEARCH_JSON, eventId);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Test
    public void testLogbrokerNotifySeriesEvent() throws Exception {
        String path = RESOURCES_PATH + "/test-logbroker-notify-series-event";
        String mid = "165507286305865990";
        String eventId = "MruHrE2Uyandex.ru";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        addFactToLucene(mid, eventId, "", "1519818200");
        sendCalendarLog(eventId, null);

        cluster.waitProducerRequests(
            cluster.calendar(),
            calendarUrl(),
            1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Test
    public void testLogbrokerNotifyOneOfSeriesEvent() throws Exception {
        String path = RESOURCES_PATH
            + "/test-logbroker-notify-one-of-series-event";
        String mid = "165507286305865991";
        String eventId = "YXj4iFWMyandex.ru";
        String recId = "2018-05-06T13:00:00.000Z";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        addFactToLucene(mid, eventId, recId, "1519818310");
        addFactToLucene("165507286305865992", eventId, "", "1519818313");
        sendCalendarLog(eventId, recId);

        cluster.waitProducerRequests(
            cluster.calendar(),
            calendarUrl(),
            1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckSearch(path + SEARCH_JSON, eventId);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Test
    public void testNoIcsAttachEvent() throws Exception {
        String path = RESOURCES_PATH + "/test-no-ics-attach";
        String mid = "170010885933253424";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        FileEntity tikaite = new FileEntity(
            new File(getClass().getResource(path + "/tikaite.json").toURI()),
            ContentType.APPLICATION_JSON);
        cluster.tikaite().add(
            "/tikaite?*",
            new StaticHttpItem(HttpStatus.SC_OK, tikaite));

        sendNotifyIexUpdate(mid);

        cluster.waitProducerRequests(
            cluster.calendar(),
            calendarUrl(),
            1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Override
    protected long uid() {
        return UID_VALUE;
    }

    @Override
    protected String domain() {
        return "calendar.yandex.ru";
    }
}

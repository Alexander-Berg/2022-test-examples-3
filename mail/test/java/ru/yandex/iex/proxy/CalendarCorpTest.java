package ru.yandex.iex.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.request.RequestHandlerMapper;

public class CalendarCorpTest extends CalendarTestBase {
    private static final String RESOURCES_PATH = "calendar-corp";
    private static final long UID_VALUE = 1120000000070488L;

    @Before
    public void initializeCluster() throws Exception {
        org.junit.Assume.assumeTrue(MailStorageCluster.iexUrl() != null);
        //String path = RESOURCES_PATH + "/init";
        cluster = new IexProxyCluster(this, true);
        cluster.iexproxy().start();

        // blackbox response mock
        String to = "m-smolina@yandex-team.ru";
        cluster.corpBlackbox().add(
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
    public void testSeriesEvent() throws Exception {
        String path = RESOURCES_PATH + "/test-series-event";
        String mid = "165507286305871998";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        sendNotifyIexUpdate(mid);

        cluster.waitProducerRequests(cluster.calendarTools(), calendarUrl(), 1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Test
    public void testLogbrokerNotifyOneOfSeriesEvent() throws Exception {
        String path = RESOURCES_PATH
            + "/test-logbroker-notify-one-of-series-event";
        String mid = "165507286305871999";
        String eventId = "XUvQESfoyandex.ru";
        String recId = "2018-05-13T13:30:00.000Z";

        importEventResponseMock(path + IMPORT_EVENT_JSON);
        filterSearchResponseMock(path + MSG_EML, path + ENVELOPES_JSON, mid);

        addFactToLucene(mid, eventId, recId, "1519818310");
        addFactToLucene("165507286305872000", eventId, "", "1519818313");

        sendCalendarLog(eventId, recId);

        cluster.waitProducerRequests(cluster.calendarTools(), calendarUrl(), 1);
        cluster.waitProducerRequests(cluster.axis(), AXIS_URI, 1);
        waitAndCheckSearch(path + SEARCH_JSON, eventId);
        waitAndCheckFacts(path + FACTS_JSON, mid);
    }

    @Override
    protected long uid() {
        return UID_VALUE;
    }

    @Override
    protected String domain() {
        return "calendar.yandex-team.ru";
    }

    @Override
    protected String calendarUrl() {
        return "/api/mail/getEventInfoByIcsUrl?*";
    }
}

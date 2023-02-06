package ru.yandex.market.antifraud.yql.step;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.ForwardChainExpectation;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.stat.step.StepEventClient;
import ru.yandex.market.stat.step.model.StepEvent;
import ru.yandex.market.stat.step.model.StepEventsRequestResult;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Slf4j
public class StepEventClientTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private ForwardChainExpectation stepEventsRequestExpectation;
    private ForwardChainExpectation createEventExpectation;
    private StepEventClient eventReader;

    @Before
    public void initClientAndResponser() {
        MockServerClient client = new MockServerClient("localhost", mockServerRule.getPort());
        stepEventsRequestExpectation = client
            .when(
                request()
                    .withMethod("GET")
                    .withPath("/api/v1/events")
                    .withQueryStringParameters(
                        new Parameter("format", "json"),
                        new Parameter("name", "cluster_table_publish"),
                        new Parameter("params__log", "market-new-clicks-log"),
                        new Parameter("params__cluster", "hahn"),
                        new Parameter("params__scale", "1d"),
                        new Parameter("sort", "-time_created"),
                        new Parameter("skip", "0"),
                        new Parameter("limit", "10")
                    ),
                Times.exactly(1)
            );
        createEventExpectation = client
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/api/v1/events")
                                .withBody(
        "{\"events\":[{\"name\":\"marketstat_rollbacks_publish_test\",\"params\":{\"scale\":" +
        "\"1d\",\"log\":\"market-new-shows-log\",\"day\":\"2018-01-15\",\"cluster\":" +
        "\"hahn\",\"path\":\"//home/market/production/mstat/yqlaf/rollbacks/" +
        "market-new-shows-log/1d/2017-09-05\"}}]}"
                                ),
                        Times.exactly(1)
                );
        eventReader = new StepEventClient("http://localhost:" + mockServerRule.getPort() + "/api/v1/events");
    }

    @Test
    @SneakyThrows
    public void mustParseNormalCreateEventResponse() {
        createEventExpectation.respond(response("{\"ids\":[\"event_id\"]}"));
        String date = IntDateUtil.hyphenated(20180115);
        String id = eventReader.createEvent(
                "marketstat_rollbacks_publish_test",
                "market-new-shows-log",
                    date,
                "1d",
                "hahn",
                "//home/market/production/mstat/yqlaf/rollbacks/market-new-shows-log/1d/2017-09-05");

        assertThat(id, is("event_id"));
    }

    @Test
    @SneakyThrows
    public void mustParseNormalEventsRequestResponse() {
        stepEventsRequestExpectation.respond(
                response(
                        IOUtils.toString(
                                this.getClass().getResourceAsStream("/step_events.json"),
                                "UTF-8"
                        )
                )
        );

        StepEventsRequestResult requestResult = eventReader.getEvents(
                "cluster_table_publish",
                "market-new-clicks-log",
                "hahn",
                "1d",
                "-time_created",
                0,
                10);

        assertThat(requestResult.getEvents().size(), is(10));
        for(StepEvent event: requestResult.getEvents()) {
            assertThat(event.getName(), is("cluster_table_publish"));
            assertThat(event.getStepEventParams().getScale(), anyOf(is("1d"), is("1h")));
            assertThat(event.getStepEventParams().getLog(), is("market-new-clicks-log"));

        }
    }

    @Test
    @SneakyThrows
    public void mustParseEventsRequestEmptyResponse() {
        stepEventsRequestExpectation.respond(response("{\"result\": []}"));

        StepEventsRequestResult requestResult = eventReader.getEvents(
                "cluster_table_publish",
                "market-new-clicks-log",
                "hahn",
                "1d",
                "-time_created",
                0,
                10);

        assertThat(requestResult.getEvents().size(), is(0));
    }

    @Test
    public void testEventsRequestEmptyResponse() throws IOException, URISyntaxException {
        stepEventsRequestExpectation.respond(HttpResponse.response().withStatusCode(503));

        StepEventsRequestResult requestResult = eventReader.getEvents(
                "cluster_table_publish",
                "market-new-clicks-log",
                "hahn",
                "1d",
                "-time_created",
                0,
                10);

        assertNull(requestResult);
    }
}

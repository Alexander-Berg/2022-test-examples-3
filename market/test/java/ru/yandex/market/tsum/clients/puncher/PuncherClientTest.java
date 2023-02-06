package ru.yandex.market.tsum.clients.puncher;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.tsum.clients.puncher.models.PuncherProtocol;
import ru.yandex.market.tsum.clients.puncher.models.PuncherRequest;
import ru.yandex.market.tsum.clients.puncher.models.PuncherResult;
import ru.yandex.market.tsum.clients.puncher.models.PuncherRule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;


public class PuncherClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());
    private PuncherClient client;

    @Before
    public void before() {
        client = new PuncherClient(
            "http://localhost:" + wireMockRule.port() + "/api/dynfw",
            "http://puncher.test.site",
            "testtoken"
        );
    }

    private void addJsonStub(String url, String jsonFile) {
        try {
            wireMockRule.stubFor(get(urlPathEqualTo(url))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(Resources.toString(Resources.getResource(jsonFile), Charsets.UTF_8))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource", e);
        }
    }

    @Test
    public void testFindRules() throws Exception {
        addJsonStub("/api/dynfw/rules", "clients/puncher/find_rules.json");

        PuncherResult result = client.findRules(
            "_MARKETNETS_",
            "test2.tst.vs.market.yandex.net",
            null,
            null
        );
        assertEquals(result.getStatus(), PuncherResult.Status.SUCCESS);
        PuncherRule puncherRule = result.getRules().get(0);
        assertEquals(puncherRule.getSources().get(0).getMachineName(), "_MARKETNETS_");
        assertEquals(puncherRule.getDestinations().get(0).getMachineName(), "test2.tst.vs.market.yandex.net");
    }

    @Test
    public void testRulesNotFound() throws Exception {
        addJsonStub("/api/dynfw/rules", "clients/puncher/find_rules_not_found.json");

        PuncherResult result = client.findRules(
            "_MARKETNETS_",
            "test2.tst.vs.market.yandex.net",
            null,
            null
        );
        assertEquals(result.getStatus(), PuncherResult.Status.SUCCESS);
        assertEquals(result.getCount(), 0);
        assertEquals(result.getRules().size(), 0);
    }

    @Test
    public void testFindRulesBadRequest() throws Exception {
        addJsonStub("/api/dynfw/rules", "clients/puncher/find_rules_bad_request.json");

        PuncherResult result = client.findRules(
            "_MARKETNETS_",
            "test2.tst.vs.market.yandex.net",
            null,
            null
        );
        assertEquals(result.getStatus(), PuncherResult.Status.ERROR);
        assertEquals(result.getMessage(), "Bad value for \"rules\": \"kek\".");
    }

    @Test
    public void testCreateRequest() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/dynfw/requests"))
            .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
            // CSADMIN-25149 Порты сериализуются в строку
            .withRequestBody(containing("\"ports\":[\"443\"]"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=UTF-8")
                .withBody(Resources.toString(Resources.getResource("clients/puncher/create_request.json"),
                    Charsets.UTF_8))));
        PuncherResult result = client.createRequest(
            Collections.singletonList("_C_MARKET_PUBLIC_"),
            Collections.singletonList("test2.tst.vs.market.yandex.net"),
            PuncherProtocol.TCP,
            Collections.singletonList(443),
            "test comment"
        );
        result.raiseForStatus();
        PuncherRequest request = result.getRequest();

        assertEquals(request.getAuthor().getLogin(), "pashayelkin");
        assertEquals(request.getSources().get(0).getMachineName(), "_C_MARKET_PUBLIC_");
        assertEquals(request.getDestinations().get(0).getMachineName(), "test2.tst.vs.market.yandex.net");
        assertEquals(request.getProtocol(), PuncherProtocol.TCP);
        assertEquals(request.getPorts(), Collections.singletonList("443"));
    }

    @Test
    public void testCreateRequestError() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/dynfw/requests"))
            .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json; charset=UTF-8")
                .withBody(Resources.toString(Resources.getResource("clients/puncher/create_request_error.json"),
                    Charsets.UTF_8))));
        PuncherResult result = client.createRequest(
            Collections.singletonList("_C_MARKET_PUBLIC_"),
            Collections.singletonList("test2.tst.vs.market.yandex.net"),
            PuncherProtocol.TCP,
            Collections.singletonList(443),
            "test comment"
        );

        assertEquals(PuncherResult.Status.ERROR, result.getStatus());
        assertEquals(true, result.isError());
        assertEquals(
            "No responsible users for host test2.tst.vs.market.yandex.net in Golem",
            result.getMessage()
        );
        assertEquals("ownerless_host_in_golem [test2.tst.vs.market.yandex.net]", result.getErrorMessage());

    }

    @Test
    public void testGetRequest() throws Exception {
        addJsonStub("/api/dynfw/requests", "clients/puncher/get_request.json");
        PuncherRequest request = client.getRequest("5abb98300d0795e7d385e3ac");
        assertEquals(request.getAuthor().getLogin(), "pashayelkin");
        assertEquals(request.getSources().get(0).getMachineName(), "_C_MARKET_PUBLIC_");
        assertEquals(request.getDestinations().get(0).getMachineName(), "test2.tst.vs.market.yandex.net");
        assertEquals(request.getProtocol(), PuncherProtocol.TCP);
        assertEquals(request.getPorts(), Collections.singletonList("443"));
    }

    /**
     * Несколько ответов на запрос request по id - это ошибка.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetRequestMultipleResults() throws Exception {
        addJsonStub("/api/dynfw/requests", "clients/puncher/get_request_multiple_results.json");
        PuncherRequest request = client.getRequest("5abb98300d0795e7d385e3ac");
    }

    @Test
    public void testGetRequestNotFound() throws Exception {
        addJsonStub("/api/dynfw/requests", "clients/puncher/get_request_not_found.json");
        Boolean excThrown = false;
        try {
            PuncherRequest request = client.getRequest("5abb98300d0795e7d385e3aa");
        } catch (IllegalStateException e) {
            assertEquals(e.getMessage(), "Puncher request not found: 5abb98300d0795e7d385e3aa");
            excThrown = true;
        }
        assertEquals(excThrown, true);
    }

    @Test
    public void testRequestUrl() throws Exception {
        assertEquals(
            client.getRequestUrl("testid"),
            "http://puncher.test.site/tasks?id=testid"
        );
    }

    /**
     * Панчер отвечает датами в двух форматах - с миллисекундами и без.
     */
    @Test
    public void testDateFormat() throws Exception {
        Gson gson = PuncherClient.getGson();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(2000, Calendar.JANUARY, 1, 7, 0, 0);


        // XXX: Сравнение дат не работает:
        // java.lang.AssertionError: expected: java.util.Date<Sat Jan 01 12:00:00 YEKT 2000>
        // but was: java.util.Date<Sat Jan 01 12:00:00 YEKT 2000>

        // Дата с миллисекундами
        assertEquals(
            cal.getTime().toString(),
            gson.fromJson("\"2000-01-01T12:00:00.000+05:00\"", Date.class).toString()
        );

        // Дата без миллисекунд
        assertEquals(
            cal.getTime().toString(),
            gson.fromJson("\"2000-01-01T12:00:00+05:00\"", Date.class).toString()
        );
    }
}

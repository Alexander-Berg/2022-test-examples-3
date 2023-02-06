package ru.yandex.market.sre.services.tms.tasks.startrek;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.SearchRequest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckExistingAICronTaskTest extends StartekTestPreset {

    @InjectMocks
    protected CheckExistingAICronTask checkExistingAICronTask;

    ObjectMapper mapper;

    @Before
    public void setUp() {
        mockStAPIServer.stubFor(WireMock.patch(WireMock.urlEqualTo("/v2/issues/?notifyAuthor=false&notify=false" +
                        "&fields="))
                .inScenario("Add and remove tags")
                .whenScenarioStateIs(Scenario.STARTED)
                .withRequestBody(WireMock.equalToJson(
                        "{\"tags\":{\"add\":[\"#noactionitems\"]}}", true, true))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(
                                "{\"self\":\"https://st-api.test.yandex-team.ru/v2/issues/TESTMARKETINCID-1\"," +
                                        "\"id\":\"123\", \"key\":\"TESTMARKETINCID-1\",\"summary\":\"TEST\"}"))
                .willSetStateTo("Tag added"));

        mockStAPIServer.stubFor(WireMock.patch(WireMock.urlEqualTo("/v2/issues/?notifyAuthor=false&notify=false" +
                        "&fields="))
                .inScenario("Add and remove tags")
                .whenScenarioStateIs("Tag added")
                .withRequestBody(WireMock.equalToJson(
                        "{\"tags\":{\"remove\":[\"#noactionitems\"]}}", true, true))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(
                                "{\"self\":\"https://st-api.test.yandex-team" +
                                        ".ru/v2/issues/TESTMARKETINCID-1\"," +
                                        "\"id\":\"123\", \"key\":\"TESTMARKETINCID-1\",\"summary\":\"TEST\"}"))
                .willSetStateTo(Scenario.STARTED));

        Session session = StartrekClientBuilder.newBuilder()
                .uri(mockStAPIServer.url(""))
                .maxConnections(10)
                .connectionTimeout(5, TimeUnit.SECONDS)
                .socketTimeout(5, TimeUnit.SECONDS)
                .build("<token>");

        checkExistingAICronTask.startrekSession = session;
        expectedException = ExpectedException.none();
        checkExistingAICronTask.startrekUiUrl = TEST_STARTEK_UI_LINK;
        checkExistingAICronTask.incidentQueueName = TEST_INCIDENT_QUEUE_NAME;
        mapper = createMapper();
    }

    private Issue prepareIssue(String descriptionFile, String tag) throws IOException {
        Issue issue = mock(Issue.class);
        ListF<String> tagsList = tag != null ? Cf.list(tag) : Cf.list();

        when(issue.getDescription()).thenReturn(Option.of(loadTextResource(descriptionFile)));
        when(issue.getKey()).thenReturn(StartekTestPreset.TEST_STARTREK_INC_ISSUE);
        when(issue.getTags()).thenReturn(tagsList);
        when(issue.getId()).thenReturn("");

        return issue;
    }

    @Test
    public void cronExpression() {
        assertNotNull(checkExistingAICronTask.cronExpression());
    }

    @Test
    public void prepareSearchRequest() {
        SearchRequest request = checkExistingAICronTask.prepareSearchRequest();
        assertNotNull(request);
        assertTrue("Необходимо фильтровать по названию очереди",
                request.toString().contains(checkExistingAICronTask.incidentQueueName));
    }

    /**
     * Начинаем с того, что в ActionItem пусто
     * Проверяем, что выставляется тег #noactionitems если нет АИ
     * Проверяем, что тег #noactionitems не исчезает при повторном запуске и отсутствие АИ
     * Проверяем, что если АИ появился, то тег #noactionitems удаляется
     */
    @Test
    public void processIssue_addTagForIncidentWoAI() throws IOException {
        Issue issue = prepareIssue("description/NoActionItemTag.txt", null);
        checkExistingAICronTask.processIssue(issue);
        Mockito.verify(issue, times(0)).getLinks();
        Mockito.verify(mockStartrekSession.links(), times(0))
                .create(any(), (String) any(), any());

        // проверяем, что если тег уже стоит, мы его не трогаем
        issue = prepareIssue("description/NoActionItemTag.txt", "#noactionitems");
        checkExistingAICronTask.processIssue(issue);

        // проверяем, что если АИ появились, то тег #noactionitems удаляем
        issue = prepareIssue("description/ActionItem-3.txt", "#noactionitems");
        checkExistingAICronTask.processIssue(issue);
    }

}

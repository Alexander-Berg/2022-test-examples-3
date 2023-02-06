package ru.yandex.market.tsum.pipelines.wms.jobs.warehouse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.impl.EmptyIterator;
import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.tsum.clients.arcadia.ArcanumClient;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsExternalTemplateParams;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsIssueConfig;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsIssueTemplate;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsParentIssue;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.SearchRequest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WmsCreateStartrekTicketJobTest {

    private static final String DESCRIPTION_WITH_INPUTS = "Test description test-desc\n\n\n"
        + "**Ниже перечислены параметры, которые нужно заполнить после выполнения задачи:**\n"
        + "###\n{\n  \"value1\": \"\", \n  \"value2\": \"\"\n}\n###\n"
        + "Инструкция по параметрам: https://wiki.yandex-team.ru/users/serpentine/new-wh-pipeline/";
    private static final String SUMMARY = "Test summary {{ param1 }}";
    private static final String DESCRIPTION = "Test description {{ param2 }}";
    private static final Map<String, Object> TEMPLATE_PARAMS =
        new HashMap<>(Map.of("param1", "test-sum", "param2", "test-desc"));
    private static final Map<String, Object> EXTERNAL_PARAMS =
        new HashMap<>(Map.of("param1", "another-sum", "param2", "another-desc"));
    private static final List<String> TAGS = List.of("tag1", "tag2");
    private static final String TEST_USER = "test42";
    private static final String ANOTHER_TEST_USER = "test43";
    private static final String PARENT_ISSUE = "MARKETTEST-1234";
    private static final String TRIGGERED_USER = "triggeredUser";

    private final TsumJobContext jobContext = new TestTsumJobContext(null, TRIGGERED_USER);

    @Mock
    private ArcanumClient arcanumClient;

    @Mock
    private Session sessionMock;
    @Mock
    private Issues issueMock;


    private JobInstanceBuilder jobBuilder;

    @Before
    public void setup() {
        Mockito.when(sessionMock.issues()).thenReturn(issueMock);
        Mockito.when(issueMock.create(Mockito.any(IssueCreate.class))).thenReturn(issue());
        Mockito.when(issueMock.get("123")).thenReturn(issue());
        Mockito.when(issueMock.find(any(SearchRequest.class))).thenReturn(new EmptyIterator());
        jobBuilder = JobInstanceBuilder.create(WmsCreateStartrekTicketJob.class)
            .withResource(new WmsParentIssue(PARENT_ISSUE))
            .withBeans(sessionMock, issueMock, arcanumClient);
    }


    @Test
    public void shouldCreateIssue() throws Exception {

        JobExecutor job = jobBuilder
            .withResources(issueConfig())
            .create();

        job.execute(jobContext);

        ArgumentCaptor<IssueCreate> issuesCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issueMock, times(1)).create(issuesCaptor.capture());

        IssueCreate issues = issuesCaptor.getValue();

        assertEquals(PARENT_ISSUE, issues.getLinks().get(0).getIssue().get());
        assertEquals("Test summary test-sum", issues.getValues().getO("summary").get());
        assertTrue(Arrays.equals(new String[]{ANOTHER_TEST_USER},
            (String[]) issues.getValues().getOptional("followers").get()));
        assertEquals(TEST_USER, issues.getValues().getO("assignee").get());
        assertEquals("MARKETTEST", issues.getValues().getO("queue").get());
        assertTrue(Arrays.equals(new String[]{"tag1", "tag2"}, (String[]) issues.getValues().getO("tags").get()));

        ArgumentCaptor<IssueUpdate> updateCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issueMock, times(1)).update(Mockito.any(IssueRef.class), updateCaptor.capture());
        IssueUpdate updateIssue = updateCaptor.getValue();

        assertEquals("Test description test-desc", ((ScalarUpdate) updateIssue.getValues().getO("description")
            .get()).getSet().get());
    }

    @Test
    public void shouldTemplateWithExternalParams() throws Exception {

        WmsIssueConfig wmsIssueConfig = issueConfig();
        wmsIssueConfig.getTemplateParams().clear();

        JobExecutor job = jobBuilder
            .withResources(new WmsExternalTemplateParams(EXTERNAL_PARAMS), wmsIssueConfig)
            .create();

        job.execute(jobContext);

        ArgumentCaptor<IssueCreate> issuesCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issueMock, times(1)).create(issuesCaptor.capture());

        IssueCreate issues = issuesCaptor.getValue();

        assertEquals("Test summary another-sum", issues.getValues().getO("summary").get());

        ArgumentCaptor<IssueUpdate> updateCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issueMock, times(1)).update(Mockito.any(IssueRef.class), updateCaptor.capture());
        IssueUpdate updateIssue = updateCaptor.getValue();
        assertEquals("Test description another-desc", ((ScalarUpdate) updateIssue.getValues().getO("description")
            .get()).getSet().get());
    }

    @Test
    public void jobParamShouldOverrideExternal() throws Exception {

        JobExecutor job = jobBuilder
            .withResources(new WmsExternalTemplateParams(EXTERNAL_PARAMS), issueConfig())
            .create();

        job.execute(jobContext);

        ArgumentCaptor<IssueCreate> issuesCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issueMock, times(1)).create(issuesCaptor.capture());

        IssueCreate issues = issuesCaptor.getValue();

        assertEquals("Test summary test-sum", issues.getValues().getO("summary").get());

        ArgumentCaptor<IssueUpdate> updateCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issueMock, times(1)).update(Mockito.any(IssueRef.class), updateCaptor.capture());
        IssueUpdate updateIssue = updateCaptor.getValue();
        assertEquals("Test description test-desc", ((ScalarUpdate) updateIssue.getValues().getO("description")
            .get()).getSet().get());
    }

    @Test
    public void shouldAssignTicketOnTriggeredUser() throws Exception {
        WmsIssueConfig wmsIssueConfig = issueConfig();
        wmsIssueConfig.setAssignOnTriggered(true);

        JobExecutor job = jobBuilder
            .withResources(wmsIssueConfig)
            .create();

        job.execute(jobContext);

        ArgumentCaptor<IssueCreate> issuesCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issueMock, times(1)).create(issuesCaptor.capture());

        IssueCreate issues = issuesCaptor.getValue();

        assertEquals(TRIGGERED_USER, issues.getValues().getO("assignee").get());
    }

    @Test
    public void shouldAddCustomSettingsToIssue() throws Exception {
        WmsIssueConfig wmsIssueConfig = issueConfig();
        wmsIssueConfig.getIssueParams().put("qaEngineer", "tester");

        JobExecutor job = jobBuilder
            .withResources(wmsIssueConfig)
            .create();

        job.execute(jobContext);

        ArgumentCaptor<IssueCreate> issuesCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issueMock, times(1)).create(issuesCaptor.capture());

        IssueCreate issues = issuesCaptor.getValue();

        assertEquals("tester", issues.getValues().getO("qaEngineer").get());
    }

    @Test
    public void shouldAddInputsToDescription() throws Exception {
        WmsIssueConfig wmsIssueConfig = issueConfig();
        wmsIssueConfig.setInputs(List.of("value1", "value2"));

        JobExecutor job = jobBuilder
            .withResources(wmsIssueConfig)
            .create();
        job.execute(jobContext);

        ArgumentCaptor<IssueUpdate> issuesCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issueMock, times(1)).update(any(IssueRef.class), issuesCaptor.capture());

        IssueUpdate value = issuesCaptor.getValue();
        String description = (String) ((ScalarUpdate) value.getValues().get("description")).getSet().get();
        assertEquals(DESCRIPTION_WITH_INPUTS, description);
    }

    private WmsIssueConfig issueConfig() {
        return new WmsIssueConfig("MARKETTEST", issueTemplate(), TEMPLATE_PARAMS, new HashMap<>(),
            TAGS, false, TEST_USER, List.of(ANOTHER_TEST_USER), false, Collections.emptyList());
    }

    private WmsIssueTemplate issueTemplate() {
        return new WmsIssueTemplate(SUMMARY, DESCRIPTION, null);
    }

    private Issue issue() {
        return new Issue("123", null, "MARKETTEST-12312", "", 0L, EmptyMap.INSTANCE, sessionMock);
    }

}

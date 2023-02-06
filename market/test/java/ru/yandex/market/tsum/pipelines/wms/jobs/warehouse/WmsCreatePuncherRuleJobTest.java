package ru.yandex.market.tsum.pipelines.wms.jobs.warehouse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.EmptyIterator;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.puncher.PuncherClient;
import ru.yandex.market.tsum.clients.puncher.models.PuncherObject;
import ru.yandex.market.tsum.clients.puncher.models.PuncherProtocol;
import ru.yandex.market.tsum.clients.puncher.models.PuncherRequest;
import ru.yandex.market.tsum.clients.puncher.models.PuncherResult;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsExternalTemplateParams;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsParentIssue;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsPuncherRule;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WmsCreatePuncherRuleJobTest {

    private static final String TEST_REQ_ID = "req1234";
    private static final String TEST_ISSUE = "MARKETTEST-1234";

    private final TsumJobContext jobContext = new TestTsumJobContext(null);

    private JobInstanceBuilder jobBuilder;

    @Mock
    private PuncherClient puncherClient;
    @Mock
    private Issues issuesMock;
    @Mock
    private Issue issueMock;
    @Mock
    private PuncherResult resultMock;

    @Before
    public void setup() {
        when(issuesMock.get(TEST_ISSUE)).thenReturn(issueMock);
        when(issueMock.getComments()).thenReturn(new EmptyIterator<>());
        when(resultMock.isError()).thenReturn(true);
        when(resultMock.getRequest()).thenReturn(makeApprovedPuncherRequest());

        when(puncherClient.createRequest(Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any(PuncherProtocol.class),
            Mockito.anyList(),
            Mockito.anyString()
        )).thenReturn(resultMock);

        jobBuilder = JobInstanceBuilder.create(WmsCreatePuncherRuleJob.class)
            .withResources(new WmsParentIssue(TEST_ISSUE),
                new WmsExternalTemplateParams(Map.of("whCode", "test", "comm", "comment1")))
            .withBeans(puncherClient, issuesMock);
    }

    @Test
    public void checkInputs() throws Exception {
        ArgumentCaptor<List> sourceCapture = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> destCapture = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> portsCapture = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> commentCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PuncherProtocol> protocolCapture = ArgumentCaptor.forClass(PuncherProtocol.class);

        when(puncherClient.createRequest(sourceCapture.capture(),
            destCapture.capture(),
            protocolCapture.capture(),
            portsCapture.capture(),
            commentCapture.capture()
        )).thenReturn(resultMock);

        when(resultMock.isError()).thenReturn(false);

        JobExecutor job = jobBuilder
            .withResource(makePuncherRuleWithoutWait())
            .create();

        job.execute(jobContext);

        assertEquals(Collections.singletonList("wms-test.test.yandex.net"), sourceCapture.getValue());
        assertEquals(Collections.singletonList("wms-sql-test.test.yandex.net"), destCapture.getValue());
        assertEquals(List.of(443, 80), portsCapture.getValue());
        assertEquals("Test comment1", commentCapture.getValue());
        assertEquals(PuncherProtocol.TCP, protocolCapture.getValue());
    }

    @Test(expected = JobManualFailException.class)
    public void checkJobState() throws Exception {
        when(resultMock.isError()).thenReturn(true);
        when(resultMock.getMessage()).thenReturn("");

        JobExecutor job = jobBuilder
            .withResource(makePuncherRuleWithoutWait())
            .create();

        job.execute(jobContext);
    }

    @Test
    public void checkRequestApprove() throws Exception {
        when(resultMock.isError()).thenReturn(false);

        when(puncherClient.getRequest(Mockito.eq(TEST_REQ_ID)))
            .thenReturn(makeApprovedPuncherRequest());

        JobExecutor job = jobBuilder
            .withResource(makePuncherRuleWithWait())
            .create();

        job.execute(jobContext);

        TaskState taskState = jobContext.progress().getTaskStates()
            .stream()
            .findFirst().get();
        assertEquals(TaskState.TaskStatus.SUCCESSFUL, taskState.getStatus());
        assertEquals(Module.PUNCHER, taskState.getModule());
    }

    @Test
    public void checkRequestDecline() throws Exception {
        when(resultMock.isError()).thenReturn(false);

        when(puncherClient.getRequest(Mockito.eq(TEST_REQ_ID)))
            .thenReturn(makeDeclinedPuncherRequest());

        JobExecutor job = jobBuilder
            .withResource(makePuncherRuleWithWait())
            .create();

        job.execute(jobContext);

        TaskState taskState = jobContext.progress().getTaskStates()
            .stream()
            .findFirst().get();
        assertEquals(TaskState.TaskStatus.FAILED, taskState.getStatus());
        assertEquals(Module.PUNCHER, taskState.getModule());
    }

    @Test
    public void testIdempotency() throws Exception {
        when(resultMock.isError()).thenReturn(false);

        IteratorF mockIterator = Mockito.mock(IteratorF.class);
        Comment mockComment = Mockito.mock(Comment.class);

        when(issuesMock.get(TEST_ISSUE)).thenReturn(issueMock);
        when(issueMock.getComments()).thenReturn(mockIterator);
        when(mockIterator.next()).thenReturn(mockComment);
        when(mockIterator.hasNext()).thenReturn(true).thenReturn(false);

        when(puncherClient.getRequest(eq("60f6a01ea8e9af06badcf45b"))).thenReturn(createPuncherRequest());

        when(mockComment.getText()).thenReturn(Option.of("Заявки в puncher созданы\n"
            + "https://puncher.yandex-team.ru/tasks?id=60f6a01ea8e9af06badcf45b\n"
            + "<{PUNCHER_REQUEST_IDS_FOR_TICKET_PARSER\n"
            + "60f6a01ea8e9af06badcf45b\n"
            + "}>"));

        JobExecutor job = jobBuilder
            .withResource(makePuncherRuleWithoutWait())
            .create();

        job.execute(jobContext);

        verify(issuesMock, times(1)).get(TEST_ISSUE);
        verify(issueMock, times(1)).getComments();
        verify(mockComment, times(1)).getText();

        verify(puncherClient,
            never()).createRequest(anyList(), anyList(), eq(PuncherProtocol.TCP), anyList(), anyString());
    }

    private PuncherRequest createPuncherRequest() {
        PuncherRequest request = new PuncherRequest();
        request.setSources(Collections.singletonList(new PuncherObject("wms-test.test.yandex.net")));
        request.setDestinations(Collections.singletonList(new PuncherObject("wms-sql-test.test.yandex.net")));
        request.setProtocol(PuncherProtocol.TCP);
        request.setPorts(List.of("443", "80"));
        request.setId(TEST_REQ_ID);
        return request;
    }

    private WmsPuncherRule makePuncherRuleWithoutWait() {
        return new WmsPuncherRule(List.of("wms-{{ whCode }}.test.yandex.net"),
            List.of("wms-sql-{{ whCode }}.test.yandex.net"),
            PuncherProtocol.TCP,
            List.of("443", "80"),
            "Test {{ comm }}",
            false
        );
    }

    private WmsPuncherRule makePuncherRuleWithWait() {
        return new WmsPuncherRule(List.of("wms-{{ whCode }}.test.yandex.net"),
            List.of("wms-sql-{{ whCode }}.test.yandex.net"),
            PuncherProtocol.TCP,
            List.of("443", "80"),
            "Test {{ comm }}",
            true
        );
    }

    private PuncherRequest makeApprovedPuncherRequest() {
        PuncherRequest request = new PuncherRequest();
        request.setId(TEST_REQ_ID);
        request.setStatus(PuncherRequest.Status.APPROVED);
        request.setTask(TEST_ISSUE);
        return request;
    }

    private PuncherRequest makeDeclinedPuncherRequest() {
        PuncherRequest request = new PuncherRequest();
        request.setId(TEST_REQ_ID);
        request.setStatus(PuncherRequest.Status.CLOSED_BY_ADMIN);
        request.setTask(TEST_ISSUE);
        return request;
    }

}

package ru.yandex.market.tsum.clients.sandbox;

import java.util.Collections;
import java.util.HashSet;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 01.10.2018
 */
public class SandboxTaskRunnerTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private static final String TASK_TYPE = "TASK_TYPE";
    private static final String OWNER = "OWNER";
    private static final String DESCRIPTION = "some text\nDESCRIPTION";
    private static final String ROBOT_LOGIN = "ROBOT_LOGIN";
    private static final String TOKEN = "TOKEN";
    private static final long TASK_ID = 9876543210L;

    private final SandboxTaskRunner.Listener listener = mock(SandboxTaskRunner.Listener.class);
    private SandboxTaskRunner sut;

    @Before
    public void setUp() {
        SandboxClient sandboxClient = new SandboxClient(
            wireMockRule.url("/api/v1.0"),
            "",
            TOKEN,
            ROBOT_LOGIN
        );

        sut = sandboxClient.newSandboxTaskRunner()
            .withTaskInput(createTaskInput())
            .withJobTaskTags(Collections.singletonList("tag"))
            .withListener(listener)
            .withSleeper(duration -> {
            });
    }

    @Test
    public void run_taskDoesNotExist() throws Exception {
        givenTaskDoesNotExist();
        sut.run();
        verifyRun(createdTask(), startedTask(), polledTask());
    }


    @Test
    public void run_taskExists_draft() throws Exception {
        givenTaskInStatusDraft();
        sut.run();
        verifyRun(foundExistingTask(), startedTask(), polledTask());
    }

    @Test
    public void run_taskExists_inProgress() throws Exception {
        givenTaskInProgress();
        sut.run();
        verifyRun(foundExistingTask(), didNotStartTask(), polledTask());
    }

    @Test
    public void run_taskExists_alreadyFinished() throws Exception {
        givenFinishedTask();
        sut.run();
        verifyRun(foundExistingTask(), didNotStartTask(), polledTask());
    }

    @Test
    public void cancel_taskDoesNotExist() {
        givenTaskDoesNotExist();
        assertFalse(sut.cancel());
        verifyTaskWasNotCancelled();
    }

    @Test
    public void cancel_taskExists_draft() {
        givenTaskInStatusDraft();
        assertTrue(sut.cancel());
        verifyTaskWasCancelled();
    }

    @Test
    public void cancel_taskExists_inProgress() {
        givenTaskInProgress();
        assertTrue(sut.cancel());
        verifyTaskWasCancelled();
    }

    @Test
    public void cancel_taskExists_alreadyFinished() {
        givenFinishedTask();
        assertFalse(sut.cancel());
        verifyTaskWasNotCancelled();
    }

    @Test
    public void searchesByTags() {
        SandboxClient client = mock(SandboxClient.class);
        SandboxTaskRunner runner = new SandboxTaskRunner(client, "")
            .withTaskInput(createTaskInput())
            .withJobTaskTags(Collections.singletonList("test_tag"));

        runner.findExistingTask();

        ArgumentCaptor<TaskSearchRequest> captor = ArgumentCaptor.forClass(TaskSearchRequest.class);
        verify(client, times(1)).getTask(captor.capture());

        Assert.assertEquals(Collections.singletonList("test_tag"), captor.getValue().getTags());
    }

    private void givenTaskDoesNotExist() {
        stubTaskSearchToReturnNoTasks();
        stubCreateTask();
        stubStartTask();
        stubStopTask();
        stubEndpointsForPolling();
    }

    private void givenTaskInStatusDraft() {
        stubTaskSearchToReturnTask("DRAFT");
        stubStartTask();
        stubStopTask();
        stubEndpointsForPolling();
    }

    private void givenTaskInProgress() {
        stubTaskSearchToReturnTask("EXECUTING");
        stubStopTask();
        stubEndpointsForPolling();
    }

    private void givenFinishedTask() {
        stubTaskSearchToReturnTask("SUCCESS");
        stubEndpointsForPolling();
    }

    private void stubEndpointsForPolling() {
        stubGetTask("SUCCESS");
        stubGetTaskAudit();
    }

    private void stubTaskSearchToReturnNoTasks() {
        stubTaskSearchToReturn("{\"items\":[]}");
    }

    private void stubTaskSearchToReturnTask(String taskStatus) {
        stubTaskSearchToReturn(String.format(
            "{\"items\": [%s]}",
            getTaskJson(taskStatus)
        ));
    }

    private void stubTaskSearchToReturn(String body) {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/api/v1.0/task/"))
                .willReturn(ok(body))
        );
    }

    private void stubCreateTask() {
        wireMockRule.stubFor(
            post(urlPathEqualTo("/api/v1.0/task"))
                .willReturn(ok(String.format("{\"id\": %d, \"status\": \"DRAFT\"}", TASK_ID)))
        );
    }

    private void stubStartTask() {
        stubBatchAction("start");
    }

    private void stubStopTask() {
        stubBatchAction("stop");
    }

    private void stubBatchAction(String action) {
        wireMockRule.stubFor(
            put(urlPathEqualTo("/api/v1.0/batch/tasks/" + action))
                .willReturn(ok(String.format("[{\"id\": %d, \"status\": \"SUCCESS\"}]", TASK_ID)))
        );
    }

    private void stubGetTask(String taskStatus) {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID))
                .willReturn(ok(getTaskJson(taskStatus)))
        );
    }

    private static String getTaskJson(String taskStatus) {
        return String.format("{\"id\": %d, \"status\": \"%s\"}", TASK_ID, taskStatus);
    }

    private void stubGetTaskAudit() {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID + "/audit"))
                .willReturn(ok("[]"))
        );
    }


    private void verifyRun(Runnable... verifiers) {
        for (Runnable verifier : verifiers) {
            verifier.run();
        }
        verifyNoMoreInteractions(listener);
    }

    private Runnable foundExistingTask() {
        return () -> {
            WireMock.verify(0, postRequestedFor(urlPathEqualTo("/api/v1.0/task")));
        };
    }

    private Runnable createdTask() {
        return () -> {
            WireMock.verify(
                1,
                postRequestedFor(urlPathEqualTo("/api/v1.0/task"))
                    .withHeader("Authorization", equalTo("OAuth " + TOKEN))
                    .withRequestBody(matchingJsonPath("$.type", equalTo(TASK_TYPE)))
                    .withRequestBody(matchingJsonPath("$.description", equalTo(DESCRIPTION)))
                    .withRequestBody(matchingJsonPath("$.owner", equalTo(OWNER)))
            );
        };
    }

    private static Runnable didNotStartTask() {
        return () -> WireMock.verify(
            0,
            putRequestedFor(urlPathEqualTo("/api/v1.0/batch/tasks/start"))
        );
    }

    private Runnable startedTask() {
        return () -> {
            WireMock.verify(
                1,
                putRequestedFor(urlPathEqualTo("/api/v1.0/batch/tasks/start"))
                    .withHeader("Authorization", equalTo("OAuth " + TOKEN))
                    .withRequestBody(matchingJsonPath("$.id.length()", equalTo("1")))
                    .withRequestBody(matchingJsonPath("$.id[0]", equalTo(String.valueOf(TASK_ID))))
            );
        };
    }

    private Runnable polledTask() {
        return () -> {
            Mockito.verify(listener, atLeastOnce()).onTaskProgress(any(SandboxTask.class), anyList());
            Mockito.verify(listener).onTaskFinished(any(SandboxTask.class), anyList());
        };
    }

    private static void verifyTaskWasCancelled() {
        WireMock.verify(
            1,
            putRequestedFor(urlPathEqualTo("/api/v1.0/batch/tasks/stop"))
                .withHeader("Authorization", equalTo("OAuth " + TOKEN))
                .withRequestBody(matchingJsonPath("$.id.length()", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.id[0]", equalTo(String.valueOf(TASK_ID))))
        );
    }

    private static void verifyTaskWasNotCancelled() {
        WireMock.verify(
            0,
            putRequestedFor(urlPathEqualTo("/api/v1.0/batch/tasks/stop"))
        );
    }


    private static TaskInputDto createTaskInput() {
        TaskInputDto result = new TaskInputDto(TASK_TYPE);
        result.setDescription(DESCRIPTION);
        result.setOwner(OWNER);
        result.setTags(new HashSet<>(Collections.singletonList("tag")));
        return result;
    }
}

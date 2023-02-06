package ru.yandex.market.logistic.gateway.service.flow;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.config.properties.FlowMapProperties;
import ru.yandex.market.logistic.gateway.config.properties.RetryProperties;
import ru.yandex.market.logistic.gateway.exceptions.FlowStatusTransitionException;
import ru.yandex.market.logistic.gateway.exceptions.RetryCountExceededException;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;

import static java.util.Collections.emptyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowServiceTest extends BaseTest {

    private static final long TASK_ID = 123;

    private static final String ERROR_MESSAGE = "Task error";

    private static final int REQUEST_FLOW_RETRY_DELAY = 120;

    private static final int REQUEST_FLOW_MAX_RETRY_COUNT = 5;

    private static final int RESPONSE_FLOW_RETRY_DELAY = 60;

    private static final int RESPONSE_FLOW_MAX_RETRY_COUNT = 3;

    private static final int DEFAULT_MAX_RETRY_COUNT = 0;

    @Mock
    private FlowMapProperties flowMapProperties;

    @Mock
    private ClientTaskRepository taskRepository;

    @Mock
    private TaskProcessService taskProcessService;

    @InjectMocks
    private FlowService flowService;

    @Before
    public void init() {
        RetryProperties defaultRequestFlowsRetryProperties = new RetryProperties();
        defaultRequestFlowsRetryProperties.setLimit(REQUEST_FLOW_MAX_RETRY_COUNT);
        defaultRequestFlowsRetryProperties.setIntervals(Arrays.asList(10, REQUEST_FLOW_RETRY_DELAY, 100));
        RetryProperties defaultResponseFlowsRetryProperties = new RetryProperties();
        defaultResponseFlowsRetryProperties.setLimit(RESPONSE_FLOW_MAX_RETRY_COUNT);
        defaultResponseFlowsRetryProperties.setIntervals(Arrays.asList(20, RESPONSE_FLOW_RETRY_DELAY, 200));
        when(flowMapProperties.getRetryProperties()).thenReturn(emptyMap());
        when(flowMapProperties.getDefaultRequestFlowsRetryProperties()).thenReturn(defaultRequestFlowsRetryProperties);
        when(flowMapProperties.getDefaultResponseFlowsRetryProperties())
            .thenReturn(defaultResponseFlowsRetryProperties);
    }

    @Test
    public void updateTaskStatusSuccess() {
        when(taskRepository.findTask(eq(TASK_ID))).thenReturn(getTask(TaskStatus.NEW, RequestFlow.DS_CREATE_ORDER));
        flowService.updateTaskStatus(TASK_ID, TaskStatus.IN_PROGRESS, "comment");
    }

    @Test(expected = FlowStatusTransitionException.class)
    public void updateTaskStatusFailed() {
        when(taskRepository.findTask(eq(TASK_ID))).thenReturn(getTask(TaskStatus.ERROR, RequestFlow.DS_CREATE_ORDER));
        flowService.updateTaskStatus(TASK_ID, TaskStatus.IN_PROGRESS, "comment");
    }

    @Test
    public void defaultRetryPropertiesWorksCorrectWhenMaxRetryCountNotReachedForRequestFlow() {
        when(taskRepository.findTask(TASK_ID)).thenReturn(getTaskWithRetryCount(TaskStatus.ERROR, 1,
            RequestFlow.DS_CREATE_ORDER));
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(TASK_ID, REQUEST_FLOW_RETRY_DELAY, ERROR_MESSAGE);
    }

    @Test(expected = RetryCountExceededException.class)
    public void defaultRetryPropertiesWorksCorrectWhenMaxRetryCountReachedForRequestFlow() {
        when(taskRepository.findTask(TASK_ID)).thenReturn(getTaskWithRetryCount(TaskStatus.ERROR,
            REQUEST_FLOW_MAX_RETRY_COUNT, RequestFlow.DS_CREATE_ORDER));
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
    }

    @Test
    public void defaultRetryPropertiesWorksCorrectWhenMaxRetryCountNotReachedForResponseFlow() {
        when(taskRepository.findTask(TASK_ID)).thenReturn(getTaskWithRetryCount(TaskStatus.ERROR, 1,
            RequestFlow.DS_CREATE_ORDER_SUCCESS));
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
        verify(taskProcessService).retryTask(TASK_ID, RESPONSE_FLOW_RETRY_DELAY, ERROR_MESSAGE);
    }

    @Test(expected = RetryCountExceededException.class)
    public void defaultRetryPropertiesWorksCorrectWhenMaxRetryCountReachedForResponseFlow() {
        when(taskRepository.findTask(TASK_ID)).thenReturn(getTaskWithRetryCount(TaskStatus.ERROR,
            RESPONSE_FLOW_MAX_RETRY_COUNT, RequestFlow.DS_CREATE_ORDER_SUCCESS));
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
    }

    @Test(expected = RetryCountExceededException.class)
    public void defaultRetryPropertiesWorksCorrectWhenMaxRetryCountReachedForNullFlow() {
        when(taskRepository.findTask(TASK_ID)).thenReturn(getTaskWithRetryCount(TaskStatus.ERROR,
            DEFAULT_MAX_RETRY_COUNT, null));
        flowService.retryFlow(TASK_ID, new RuntimeException(ERROR_MESSAGE));
    }

    private ClientTask getTaskWithRetryCount(TaskStatus taskStatus, int retryCount, RequestFlow requestFlow) {
        ClientTask task = getTask(taskStatus, requestFlow);
        task.setCountRetry(retryCount);
        return task;
    }

    private ClientTask getTask(TaskStatus status, RequestFlow requestFlow) {
        ClientTask task = new ClientTask();

        task.setId(TASK_ID);
        task.setStatus(status);
        task.setFlow(requestFlow);

        return task;
    }
}

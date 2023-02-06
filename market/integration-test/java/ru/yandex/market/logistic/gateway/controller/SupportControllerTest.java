package ru.yandex.market.logistic.gateway.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.SupportControllerException;
import ru.yandex.market.logistic.gateway.model.dto.support.RetryTasksFromUIRequest;
import ru.yandex.market.logistic.gateway.model.dto.support.RetryTasksRequest;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.model.entity.TaskHistory;
import ru.yandex.market.logistic.gateway.service.support.TaskSupportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.DS_CANCEL_ORDER_ERROR;
import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.DS_CANCEL_ORDER_SUCCESS;
import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.DS_CREATE_ORDER;
import static ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow.DS_UPDATE_ORDER;
import static ru.yandex.market.logistic.gateway.model.TaskStatus.ERROR;
import static ru.yandex.market.logistic.gateway.model.TaskStatus.NEW;

public class SupportControllerTest extends AbstractIntegrationTest {

    private static final List<Long> taskIdsList = ImmutableList.of(1L, 2L, 3L);

    public static final String RETRY_REASON_TEST = "Retry reason test";

    @MockBean
    private TaskSupportService taskSupportService;

    @Test
    public void updateTasksStatusInvalidRequest() throws Exception {
        mockMvc.perform(post("/support/tasks/retry")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/support_retry_tasks_request_invalid.json")))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void updateTasksStatusValidRequest() throws Exception {
        mockMvc.perform(post("/support/tasks/retry")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/support_retry_tasks_request.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<RetryTasksRequest> retryTasksRequestCaptor = ArgumentCaptor.forClass(RetryTasksRequest.class);
        verify(taskSupportService).cloneTasks(retryTasksRequestCaptor.capture());
        softAssert.assertThat(retryTasksRequestCaptor.getValue().getTaskIds())
            .as("task ids")
            .isEqualTo(taskIdsList);
        softAssert.assertThat(retryTasksRequestCaptor.getValue().getRetryReason())
            .as("retry reason")
            .isEqualTo(RETRY_REASON_TEST);
    }

    @Test
    public void updateTasksStatusFromUIValidRequest() throws Exception {
        mockMvc.perform(post("/support/tasks/retry-from-ui")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/support_retry_tasks_from_ui_request.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<RetryTasksFromUIRequest> argumentCaptor = ArgumentCaptor.forClass(RetryTasksFromUIRequest.class);
        verify(taskSupportService).cloneTasks(argumentCaptor.capture());

        softAssert.assertThat(argumentCaptor.getValue().getIds())
            .as("TaskIds should be a list with one task with id 1")
            .isEqualTo(ImmutableList.of(1L));
    }

    @Test
    public void updateMultipleTasksFromUIValidRequest() throws Exception {
        mockMvc.perform((post("/support/tasks/retry-multiple-from-ui")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                extractFileContent("fixtures/request/support_retry_multiple_tasks_from_ui_request.json"))))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<RetryTasksFromUIRequest> argumentCaptor = ArgumentCaptor.forClass(RetryTasksFromUIRequest.class);
        verify(taskSupportService).cloneTasks(argumentCaptor.capture());
        softAssert.assertThat(argumentCaptor.getValue().getIds())
            .as("task ids")
            .isEqualTo(taskIdsList);
    }

    @Test
    public void cancelTaskFromUi() throws Exception {
        mockMvc.perform(post("/support/tasks/cancel-from-ui")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/support_cancel_tasks_from_ui_request.json")))
            .andExpect(status().is2xxSuccessful());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);

        verify(taskSupportService).cancelTasks(captor.capture());
        assertThat(captor.getValue()).containsExactly(123L);
    }

    @Test
    public void cancelMultipleTasksFromUi() throws Exception {
        mockMvc.perform(post("/support/tasks/cancel-multiple-from-ui")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                extractFileContent("fixtures/request/support_cancel_multiple_tasks_from_ui_request.json")))
            .andExpect(status().is2xxSuccessful());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);

        verify(taskSupportService).cancelTasks(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 2L, 3L, 5L);
    }

    @Test
    public void updateTaskMessageValidRequest() throws Exception {
        when(taskSupportService.updateClientTaskMessage(anyLong(), anyString()))
            .thenReturn(getClientTask());

        mockMvc.perform((put("/support/tasks/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/support_update_task_request.json"))))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Long> longArgumentCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskSupportService).updateClientTaskMessage(longArgumentCaptor.capture(), stringArgumentCaptor.capture());
        softAssert.assertThat(longArgumentCaptor.getValue())
            .as("Task with id 1 is being updated")
            .isEqualTo(1);

        softAssert.assertThat(stringArgumentCaptor.getValue())
            .as("New task message is correct")
            .isEqualTo("{\n  \"field\": \"value\"\n}");
    }

    @Test
    public void updateTaskMessageInvalidRequest() throws Exception {
        mockMvc.perform(put("/support/tasks/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/support_update_task_invalid_request.json")))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void getClientTasksValidRequestEmpty() throws Exception {
        when(taskSupportService.findClientTasks(any(Pageable.class), anyMapOf(String.class, String.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/support/tasks"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_empty_result.json"), true));
    }

    @Test
    public void getClientTasksValidRequestNoPagination() throws Exception {
        when(taskSupportService.findClientTasks(any(Pageable.class), anyMapOf(String.class, String.class)))
            .thenReturn(new PageImpl<>(getClientTasks()));

        mockMvc.perform(get("/support/tasks"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_client_tasks.json"), true));
    }

    /**
     * Проверяем, что yql ссылка - пустая у callback запросов.
     */
    @Test
    public void getCallbackClientTaskRequest() throws Exception {
        when(taskSupportService.findTaskById(1L))
            .thenReturn(createClientTaskWithFlow(DS_CANCEL_ORDER_ERROR));
        when(taskSupportService.findTaskById(2L))
            .thenReturn(createClientTaskWithFlow(DS_CANCEL_ORDER_SUCCESS));

        mockMvc.perform(get("/support/tasks/1"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_error_callback_client_task.json"), true));
        mockMvc.perform(get("/support/tasks/2"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_success_callback_client_task.json"), true));
    }

    @Test
    public void getClientTaskValidRequest() throws Exception {
        when(taskSupportService.findTaskById(1L))
            .thenReturn(getClientTask());

        mockMvc.perform(get("/support/tasks/1"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_client_task.json"), true));
    }

    @Test
    public void getClientTaskValidRequestNoMatches() throws Exception {
        when(taskSupportService.findClientTasks(anyObject(),
            eq(ImmutableMap.of("flow", "ds-cancel-order"))))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/support/tasks?requestFlow=ds-cancel-order"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_empty_result.json"), true));
    }

    @Test
    public void getClientTaskValidRequestByRequestFlow() throws Exception {
        when(taskSupportService.findClientTasks(anyObject(),
            eq(ImmutableMap.of("flow", "ds-create-order"))))
            .thenReturn(new PageImpl<>(Collections.singletonList(getClientTask())));

        mockMvc.perform(get("/support/tasks?requestFlow=ds-create-order"))

            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(
                getFileContent("fixtures/response/support_get_client_tasks_filter.json"), true));
    }

    @Test
    public void getClientTaskValidRequestByEntityIdSuccessful() throws Exception {
        when(taskSupportService.findClientTasksByEntityId(eq("123ABC456"), anyObject()))
            .thenReturn(new PageImpl<>(Collections.singletonList(getClientTask())));

        mockMvc.perform(get("/support/tasks?entityId=123ABC456"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(
                getFileContent("fixtures/response/support_get_client_tasks_entity_filter.json"), true));

        verify(taskSupportService).findClientTasksByEntityId(eq("123ABC456"), anyObject());
    }

    @Test
    public void getClientTaskValidRequestByEntityIdWithOtherFiltersFailed() throws Exception {
        assertThatThrownBy(() -> mockMvc.perform(get("/support/tasks?entityId=123ABC456&requestFlow=ds-create-order")))
            .hasCause(new SupportControllerException(
                "Если используется фильтрация по entityId, то не допускается одновременная с этим " +
                    "фильтрация по другим полям."));
    }

    @Test
    public void getAllTasksHistoryEmptyResult() throws Exception {
        when(taskSupportService.getTasksHistory(isNull(Long.class), anyObject()))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/support/tasks/history"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(
                getFileContent("fixtures/response/support_get_empty_result.json"), true)
            );

        verify(taskSupportService).getTasksHistory(isNull(Long.class), anyObject());
    }


    @Test
    public void getAllTasksHistory() throws Exception {
        when(taskSupportService.getTasksHistory(isNull(Long.class), anyObject()))
            .thenReturn(new PageImpl<>(Arrays.asList(getTaskHistory(100L), getTaskHistory(101L))));

        mockMvc.perform(get("/support/tasks/history"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_all_tasks_history.json"), true));

        verify(taskSupportService).getTasksHistory(isNull(Long.class), anyObject());
    }

    @Test
    public void getTaskHistoryByTaskId() throws Exception {
        when(taskSupportService.getTasksHistory(eq(1L), anyObject()))
            .thenReturn(new PageImpl<>(Collections.singletonList(getTaskHistory(102L))));

        mockMvc.perform(get("/support/tasks/history?taskId=1"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/support_get_tasks_history_by_id.json"), true));

        verify(taskSupportService).getTasksHistory(eq(1L), anyObject());
    }

    private List<ClientTask> getClientTasks() {
        return Arrays.asList(
            new ClientTask()
                .setId(1L)
                .setConsumer(TaskResultConsumer.LOM)
                .setParentId(null)
                .setRootId(1L)
                .setFlow(DS_CREATE_ORDER)
                .setMessage("{}")
                .setStatus(NEW)
                .setCountRetry(3)
                .setDelaySeconds(60)
                .setCreated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-01-01 00:00:00")))
                .setUpdated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-01-02 00:00:00")))
                .setRequestId("request1")
                .setLastSubReqNumber(12345),
            new ClientTask()
                .setId(2L)
                .setConsumer(TaskResultConsumer.LOM)
                .setParentId(1L)
                .setRootId(1L)
                .setFlow(DS_UPDATE_ORDER)
                .setMessage("{}")
                .setStatus(NEW)
                .setCountRetry(5)
                .setDelaySeconds(55)
                .setCreated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-01-03 00:00:00")))
                .setUpdated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-01-04 00:00:00")))
                .setRequestId("request2")
                .setLastSubReqNumber(67890)
        );
    }

    private ClientTask getClientTask() {
        return createClientTaskWithFlow(DS_CREATE_ORDER);
    }

    private TaskHistory getTaskHistory(Long id) {
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setId(id);
        taskHistory.setTaskId(1L);
        taskHistory.setCreated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-02-07 00:00:00")));
        taskHistory.setFromStatus(NEW);
        taskHistory.setToStatus(ERROR);
        taskHistory.setComment("some-test-comment");
        return taskHistory;
    }

    private ClientTask createClientTaskWithFlow(RequestFlow flow) {
        return new ClientTask()
            .setId(1L)
            .setConsumer(TaskResultConsumer.LOM)
            .setParentId(null)
            .setRootId(null)
            .setFlow(flow)
            .setMessage("{ }")
            .setStatus(NEW)
            .setCountRetry(5)
            .setDelaySeconds(60)
            .setCreated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-02-05 00:00:00")))
            .setUpdated(DateUtil.asLocalDateTime(DateUtil.convertToDate("2118-02-06 00:00:00")))
            .setRequestId("test request")
            .setLastSubReqNumber(123);
    }
}

package ru.yandex.market.logistic.gateway.service.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.model.dto.support.RetryTasksFromUIRequest;
import ru.yandex.market.logistic.gateway.model.dto.support.RetryTasksRequest;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.repository.QueryDslBuilder;
import ru.yandex.market.logistic.gateway.service.flow.TaskProcessService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskSupportServiceTest extends BaseTest {

    private static final List<Long> taskIdsList = ImmutableList.of(1L, 2L, 3L);

    @Mock
    private TaskProcessService taskProcessService;

    @Mock
    private ClientTaskRepository clientTaskRepository;

    @Mock
    private QueryDslBuilder queryDslBuilder;

    @InjectMocks
    private TaskSupportService taskSupportService;

    @Test
    public void retryTasksEmpty() {
        taskSupportService.cloneTasks(getRetryTasksRequestEmpty());
        verify(taskProcessService, never()).cloneTaskSafe(anyLong());
    }

    @Test
    public void retryTasksMultiple() {
        taskSupportService.cloneTasks(getRetryTasksRequestMultiple());

        ArgumentCaptor<Long> taskIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(taskProcessService, times(3))
            .cloneTask(taskIdCaptor.capture());

        List<Long> taskIdsCaptured = taskIdCaptor.getAllValues();
        assertions.assertThat(taskIdsCaptured).as("Captured task ids list matches").isEqualTo(taskIdsList);
    }

    @Test
    public void retryTasksMultipleFromUi() {
        taskSupportService.cloneTasks(new RetryTasksFromUIRequest(taskIdsList));

        ArgumentCaptor<Long> taskIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(taskProcessService, times(3))
            .cloneTaskSafe(taskIdCaptor.capture());

        List<Long> taskIdsCaptured = taskIdCaptor.getAllValues();
        assertions.assertThat(taskIdsCaptured).as("Captured task ids list matches").isEqualTo(taskIdsList);
    }

    @Test
    public void testFindClientTasksByEntityId() {
        taskSupportService.findClientTasksByEntityId("123456", PageRequest.of(0, 10));
        verify(clientTaskRepository).findByEntityId("123456", PageRequest.of(0, 10));
    }

    @Test
    public void findClientTasksReturnsPageFromClientTaskRepository() {
        Pageable mockPageable = mock(Pageable.class);
        Map<String, String> mockFilterParams = Collections.unmodifiableMap(new HashMap<>());
        Predicate mockPredicate = mock(Predicate.class);
        Page<ClientTask> mockPage = mock(Page.class);

        when(queryDslBuilder.buildPredicate(ClientTask.class, mockFilterParams)).thenReturn(mockPredicate);
        when(clientTaskRepository.findAll(mockPredicate, mockPageable)).thenReturn(mockPage);

        assertEquals(mockPage, taskSupportService.findClientTasks(mockPageable, mockFilterParams));
    }

    @Test
    public void findClientTasksReturnsEmptyPageOnConversionFailedException() {
        Pageable mockPageable = mock(Pageable.class);
        Map<String, String> mockFilterParams = Collections.unmodifiableMap(new HashMap<>());

        when(queryDslBuilder.buildPredicate(ClientTask.class, mockFilterParams)).thenThrow(ConversionFailedException.class);

        assertEquals(Page.empty(), taskSupportService.findClientTasks(mockPageable, mockFilterParams));
    }

    private RetryTasksRequest getRetryTasksRequestEmpty() {
        return new RetryTasksRequest(Collections.emptyList(), "test reason");
    }

    private RetryTasksRequest getRetryTasksRequestMultiple() {
        return new RetryTasksRequest(taskIdsList, "test reason");
    }
}

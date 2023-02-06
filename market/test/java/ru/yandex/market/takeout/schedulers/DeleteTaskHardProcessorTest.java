package ru.yandex.market.takeout.schedulers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.takeout.config.DeleteService;
import ru.yandex.market.takeout.service.DeleteTask;
import ru.yandex.market.takeout.service.DeletedTasksProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeleteTaskHardProcessorTest {

    @Mock
    public DeleteService deleteService;
    @Mock
    public DeletedTasksProvider tasksProvider;
    @InjectMocks
    public DeleteTaskHardProcessor processor;

    @Test
    public void shouldNotDeleteHardPersonalData() {
        processor.deleteHardPersonalData();
        verify(deleteService, never()).deleteHard(any(DeleteTask.class), any(RequestContext.class));
        verify(tasksProvider, never())
                .setTaskHardDeletedStatus(any(DeleteTask.class), anyBoolean(), any(RequestContext.class));
    }
}

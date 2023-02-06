package ru.yandex.market.crm.campaign.yql;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.yql.client.YqlClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteYqlTaskTest {

    @Mock
    private YqlClient yqlClient;

    @Test
    public void RetryingAfterErrorTest() throws Exception {
        var task = new ExecuteYqlTask<Void, ExecuteYqlTaskData>(yqlClient) {
            @Nonnull
            @Override
            protected Optional<String> generateYql(Void context, ExecuteYqlTaskData data) {
                return Optional.empty();
            }
        };

        var data = new ExecuteYqlTaskData();
        data.setOperationId("123123");
        var control = Mockito.mock(Control.class);

        for (int i = 0; i < 4; i++) {
            var result = task.run(null, data, control);
            assertEquals(TaskStatus.WAITING, result.getNextStatus());
            assertEquals(data.getRetryCount(), i + 1);
        }

        var result = task.run(null, data, control);
        assertEquals(TaskStatus.FAILING, result.getNextStatus());
        assertEquals(data.getRetryCount(), 5);
    }

    @Before
    public void setUp() {
        when(yqlClient.getOperationStatus(any()))
                .thenThrow(new RuntimeException("error"));
    }
}

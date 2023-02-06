package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.palet.EnrichRegisterWithPalletsProducer;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static org.mockito.ArgumentMatchers.argThat;

public class TransportationTaskLauncherServiceTest extends AbstractContextualTest {

    @Autowired
    private TransportationTaskLauncherService transportationTaskLauncherService;

    @Autowired
    private EnrichRegisterWithPalletsProducer enrichRegisterWithPalletsProducer;

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @DatabaseSetup(value = "/repository/transportation_task/transportation_tasks_for_launch.xml", type = INSERT)
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_tasks_launched.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void startEnrichmentTasks() {
        transportationTaskLauncherService.launch();

        ArgumentMatcher<List<Long>> arg1 = list -> list.size() == 10;
        ArgumentMatcher<List<Long>> arg2 = list -> list.size() == 1;
        Mockito.verify(enrichRegisterWithPalletsProducer).enqueue(argThat(arg1));
        Mockito.verify(enrichRegisterWithPalletsProducer).enqueue(argThat(arg2));
    }
}

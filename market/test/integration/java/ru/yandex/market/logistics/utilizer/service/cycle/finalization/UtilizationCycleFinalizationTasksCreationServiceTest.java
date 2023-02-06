package ru.yandex.market.logistics.utilizer.service.cycle.finalization;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailPayload;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

import static org.mockito.Mockito.times;

public class UtilizationCycleFinalizationTasksCreationServiceTest extends AbstractContextualTest {

    @Autowired
    private UtilizationCycleFinalizationTasksCreationService utilizationCycleFinalizationTasksCreationService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/finalization-tasks/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/finalization-tasks/1/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void createCycleFinalizationTasks() {
        utilizationCycleFinalizationTasksCreationService.createCycleFinalizationTasks();

        ArgumentCaptor<EnqueueParams<SendUtilizationCycleFinalizationEmailPayload>> payloadCaptor =
                ArgumentCaptor.forClass(EnqueueParams.class);
        Mockito.verify(sendUtilizationCycleFinalizationEmailProducer, times(2))
                .enqueue(payloadCaptor.capture());
        List<Long> cycleIdsInPayload = payloadCaptor.getAllValues().stream()
                .map(EnqueueParams::getPayload)
                .map(SendUtilizationCycleFinalizationEmailPayload::getUtilizationCycleId)
                .collect(Collectors.toList());
        softly.assertThat(cycleIdsInPayload).containsExactlyInAnyOrder(1L, 3L);
    }
}

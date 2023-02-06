package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation_task.encrich.EnrichTransportationTaskProducer;

public class TransportationTaskMainFlowServiceTest extends AbstractContextualTest {

    @Autowired
    private TransportationTaskMainFlowService transportationTaskMainFlowService;

    @Autowired
    private EnrichTransportationTaskProducer enrichTransportationTaskProducer;

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_enrichment_start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void startEnrichmentTasks() {
        transportationTaskMainFlowService.startEnrichmentTasks();
        Mockito.verify(enrichTransportationTaskProducer).enqueue(1L);
        Mockito.verify(enrichTransportationTaskProducer).enqueue(5L);
    }

}

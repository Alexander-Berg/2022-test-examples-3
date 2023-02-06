package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTaskStatus;

public class TransportationTaskStatusServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationTaskStatusService transportationTaskStatusService;

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatus() {
        transportationTaskStatusService.setStatus(List.of(1L, 2L), TransportationTaskStatus.COMPLETED);
    }
}

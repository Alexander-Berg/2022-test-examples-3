package ru.yandex.market.delivery.transport_manager.service.transportation_task;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class TransportationTaskDeletionServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationTaskDeletionService transportationTaskDeletionService;

    @DatabaseSetup(value = {
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/transportation_task_transportations.xml",
        "/repository/transportation_task/validation_errors.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void delete() {
        transportationTaskDeletionService.delete(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L));
    }
}

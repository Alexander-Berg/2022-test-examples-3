package ru.yandex.market.delivery.transport_manager.service.transportation_task;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTask;

class TransportationTaskDeniedRegisterServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationTaskDeniedRegisterService transportationTaskDeniedRegisterService;

    @DatabaseSetup("/repository/transportation_task/transportation_tasks_with_deny_register.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/transportation_tasks_with_deny_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void getDeniedRegisterId() {
        softly.assertThat(
                transportationTaskDeniedRegisterService.getOrCreateDeniedRegisterId(
                    new TransportationTask()
                        .setId(1L)
                        .setDeniedRegisterId(1L)
                )
            )
            .isEqualTo(1L);
    }

    @DatabaseSetup("/repository/transportation_task/transportation_tasks_insertable_register.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/transportation_tasks_with_deny_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createDeniedRegisterId() {
        softly.assertThat(
                transportationTaskDeniedRegisterService.getOrCreateDeniedRegisterId(
                    new TransportationTask().setId(1L)
                )
            )
            .isEqualTo(1L);
    }
}

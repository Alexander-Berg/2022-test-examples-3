package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class TransportationTaskQuantityValidationServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationTaskQuantityValidationService service;

    @DisplayName("Завершаем валидацию для всех задач, у которых по всем запросам в аксапту пришли ответы")
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/axapta_requests_completed_check.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void completeValidationSucceeded() {
        service.completeValidationSucceeded();
    }

    @DisplayName("Переводим задачу в статус STOCK_AVAILABILITY_CHECK_FAILED, " +
            "если доля товаров в красном реестре больше 95%")
    @DatabaseSetup("/repository/transportation_task/transportation_task_red_registers.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/transportation_task_red_register_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void completeValidationSucceededRedRegister() {
        service.completeValidationSucceeded();
    }

    @DisplayName("Не переводим задачу в статус STOCK_AVAILABILITY_CHECK_FAILED," +
            " если доля товаров в красном реестре больше 0% и меньше 95%")
    @DatabaseSetup("/repository/transportation_task/transportation_task_no_cancel_red_registers.xml")
    @ExpectedDatabase(
            value = "/repository/transportation_task/transportation_task_no_cancel_red_registers_result.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void completeValidationHalfRedRegister() {
        service.completeValidationSucceeded();
    }

    @DisplayName(
        "Ошибка валидации для всех задач, у которых по части запросов в аксапту не пришли ответы спустя 30 мин"
    )
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/axapta_requests_failed_check.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void completeValidationExpired() {
        clock.setFixed(
            ZonedDateTime.of(2021, 5, 6, 12, 31, 0, 0, ZoneOffset.systemDefault()).toInstant(),
            ZoneOffset.systemDefault()
        );
        service.completeValidationExpired();
    }

    @DisplayName(
        "Не переводим задачу в ошибку, так как все запросы в аксапту ещё новые, и нельзя сказать, что "
            + "ответ по ним не придёт никогда"
    )
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/transportation_tasks_width_axapta_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void completeValidationNotExpiredYet() {
        clock.setFixed(
            ZonedDateTime.of(2021, 5, 6, 12, 28, 0, 0, ZoneOffset.systemDefault()).toInstant(),
            ZoneOffset.systemDefault()
        );
        service.completeValidationExpired();
    }

    @DisplayName("Ошибка обработки одной задачи не должна ломать метод")
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests_wrong.xml")
    @Test
    void completeValidationSkipFailed() {
        softly.assertThat(service.completeValidationSucceeded())
            .hasSize(1)
            .allMatch(e -> e instanceof IllegalStateException)
            .allMatch(e -> e.getMessage().contains("Duplicate key RegisterUnitStockKey"));
    }
}

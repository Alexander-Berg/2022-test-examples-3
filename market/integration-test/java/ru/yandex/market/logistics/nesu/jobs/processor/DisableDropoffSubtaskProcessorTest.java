package ru.yandex.market.logistics.nesu.jobs.processor;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.nesu.enums.DropoffDisablingSubtaskStatus;
import ru.yandex.market.logistics.nesu.enums.DropoffDisablingSubtaskType;
import ru.yandex.market.logistics.nesu.exception.http.BadRequestException;
import ru.yandex.market.logistics.nesu.logging.enums.LoggingTag;
import ru.yandex.market.logistics.nesu.model.dto.DropoffDisablingSubtaskExecutionResult;
import ru.yandex.market.logistics.nesu.service.dropoff.AbstractDisablingSubtaskTest;
import ru.yandex.market.logistics.nesu.service.dropoff.executor.SendMessageExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DisplayName("Отключение дропоффа")
@ParametersAreNonnullByDefault
class DisableDropoffSubtaskProcessorTest extends AbstractDisablingSubtaskTest {

    @MockBean
    private SendMessageExecutor sendMessageExecutor;

    @Test
    @DisplayName("Успешное выполнение подзадачи, задача переводится в SUCCESS")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_one_task_scheduled.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/all_success_dropoff_disable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAllTaskSuccess() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(1L, 1L));
    }

    @Test
    @DisplayName("Успешное выполнение подзадачи, следующая задача выставлена на выполнение")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/new_subtask_in_dbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successPutNextTaskOnExecution() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(2L, 2L));
    }

    @Test
    @DisplayName("Успешное выполнение подзадачи, следующая задача находится не в статусе NEW")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/disabling_dropoff_no_new_subtask.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNoNewSubtaskInQueue() {
        when(sendMessageExecutor.matching(any(DropoffDisablingSubtaskType.class))).thenReturn(true);
        when(sendMessageExecutor.execute(anyLong()))
            .thenReturn(new DropoffDisablingSubtaskExecutionResult(DropoffDisablingSubtaskStatus.SCHEDULED));
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(2L, 5L));
    }

    @Test
    @DisplayName("Ошибка выполнения подзадачи, пишем в лог")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
    void errorSubtaskWriteToBacklog() {
        when(sendMessageExecutor.matching(any(DropoffDisablingSubtaskType.class))).thenReturn(true);
        when(sendMessageExecutor.execute(anyLong()))
            .thenReturn(new DropoffDisablingSubtaskExecutionResult(DropoffDisablingSubtaskStatus.ERROR, "test"));
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(2L, 5L));

        verifyBacklog(LoggingTag.DISABLING_DROPOFF_SUBTASK_EXCEPTION, "test", 2L, 5L);
    }

    @Test
    @DisplayName("Не существует экзекутора для процессинга подзадачи")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
    void notExistSubtaskExecutor() {
        when(sendMessageExecutor.matching(any(DropoffDisablingSubtaskType.class))).thenReturn(false);
        softly
            .assertThatThrownBy(() -> disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(2L, 5L)))
            .hasMessage("No executor for process subtask with type SEND_MESSAGES");
    }

    @Test
    @DisplayName("Ошибка, задача находится в терминальном статусе")
    @DatabaseSetup("/repository/dropoff/disabling/before/error_disabling_dropoff_request.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/error_disabling_dropoff_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failureInTerminalStatus() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(4L, 1L));
        verifyBacklog(
            LoggingTag.DISABLING_DROPOFF_SUBTASK_EXCEPTION,
            "DropoffDisablingRequest is in terminal status",
            4L,
            1L
        );
    }

    @Test
    @DisplayName("Ошибка не найден DisablingRequest")
    @DatabaseSetup("/repository/dropoff/disabling/before/error_request_in_terminal_status.xml")
    void invalidRequestId() {
        String exceptionText = "Failed to find [DROPOFF_DISABLING_REQUEST] with ids [100]";
        softly.assertThatThrownBy(
            () -> disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100L, 1L)),
            exceptionText
        );
    }

    @Test
    @DisplayName("Ошибка не найден DisablingSubtask")
    @DatabaseSetup("/repository/dropoff/disabling/before/error_request_in_terminal_status.xml")
    void invalidSutaskId() {
        String exceptionText = "Failed to find [DROPOFF_DISABLING_SUBTASK] with ids [100]";
        softly.assertThatThrownBy(
            () -> disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(5L, 100L)),
            exceptionText
        );
    }

    @Test
    @DisplayName("Ошибка final failure, переводим запрос в error, пишем в лог")
    @DatabaseSetup("/repository/dropoff/disabling/before/error_request_in_terminal_status.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/error_request_in_terminal_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void finalFailureToError() {
        String exceptionText = "Test";
        disableDropoffSubtaskProcessor.processFinalFailure(
            getDisableDropoffPayload(5L, 1L),
            new BadRequestException(exceptionText)
        );

        verifyBacklog(LoggingTag.DISABLING_DROPOFF_EXCEPTION, exceptionText, 5L, 1L);
        verifyBacklog(LoggingTag.DISABLING_DROPOFF_SUBTASK_EXCEPTION, exceptionText, 5L, 1L);
    }

    @Test
    @DisplayName("Ошибка final failure, пишем в лог")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/disabling_dropoff_request_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void finalFailure() {
        String exceptionText = "Test";
        disableDropoffSubtaskProcessor.processFinalFailure(
            getDisableDropoffPayload(1L, 1L),
            new BadRequestException(exceptionText)
        );

        verifyBacklog(LoggingTag.DISABLING_DROPOFF_EXCEPTION, exceptionText, 1L, 1L);
    }

    @Test
    @DisplayName("Успешная обработка подзадачи отключения конфигурации доступности у лог. точки")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_availability_subtask.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/disabling_availability_subtask.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processDisableLogisticPointAvailabilitySubtask() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(1L, 2L));
    }
}

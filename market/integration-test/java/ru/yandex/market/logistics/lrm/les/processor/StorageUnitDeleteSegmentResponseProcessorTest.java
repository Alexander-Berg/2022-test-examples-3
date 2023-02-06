package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.dto.ResultDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorType;
import ru.yandex.market.logistics.les.tpl.StorageUnitDeleteSegmentResponseEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static com.github.springtestdbunit.annotation.DatabaseOperation.UPDATE;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@ParametersAreNonnullByDefault
@DisplayName("Обработка ответа из леса об удалении на СЦ")
@DatabaseSetup("/database/les/delete-in-sc/before/minimal.xml")
class StorageUnitDeleteSegmentResponseProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private AsyncLesEventProcessor processor;

    private static final String REQUEST_ID = "request-id-test";

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2022-05-06T07:08:09.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Не найден бизнес-процесс")
    @ExpectedDatabase(value = "/database/les/delete-in-sc/before/minimal.xml", assertionMode = NON_STRICT)
    void businessProcessNotFound() {
        softly.assertThatCode(() -> processor.execute(getDbQueuePayload(
                new StorageUnitDeleteSegmentResponseEvent("non-existing-req-id", new ResultDto(List.of()))
            )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "No business process with requestId non-existing-req-id and type DELETE_SEGMENT_IN_SC"
            );
    }

    @Test
    @DisplayName("Сегмент успешно удалён в СЦ, нет следующего за ним СЦ")
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/business_process_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/no_next_segment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successDeleteInScNoNextSc() {
        execute();
        checkLogsForSuccess();
    }

    @Test
    @DisplayName("Сегмент успешно удалён в СЦ и есть следующий за ним СЦ")
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/with_next_sc.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/business_process_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/next_segment_cancelled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successDeleteInScHasNextSc() {
        execute();
        checkLogsForSuccess();
    }

    @Test
    @DisplayName("Ошибка при удалении в СЦ")
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/with_next_sc.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/business_process_fail.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void errorDeletingInSc() {
        execute(errors());
        checkLogsForError();
    }

    @Test
    @DisplayName("Ошибка при удалении в СЦ: статусы на СЦ уже существуют")
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/with_next_sc_statuses_already_exists.xml", type = REFRESH)
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/business_process_fail_statuses_not_changed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void errorDeletingInScStatusesExists() {
        execute(errors());
        checkLogsForError();
    }

    @Test
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/cancel_return.xml", type = REFRESH)
    @DisplayName("Отмена возврата, один сегмент СЦ")
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/business_process_success_cancel_return.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/cancel_return_update_status_task_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/no_next_segment_cancel_return.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successDeleteInScCancelReturn() {
        execute();
        checkLogsForSuccess();
    }

    @Test
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/cancel_return.xml", type = REFRESH)
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/cancel_return_with_next_sc.xml", type = REFRESH)
    @DisplayName("Отмена возврата, несколько сегментов СЦ")
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/business_process_success_cancel_return.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/database/les/delete-in-sc/after/next_segment_cancel_return.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void successDeleteInScWithNextSegmentCancelReturn() {
        execute();
        checkLogsForSuccess();
    }

    @Test
    @DisplayName("Дубль успеха")
    @DatabaseSetup(value = "/database/les/delete-in-sc/before/success.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = NON_STRICT
    )
    void doubleSuccess() {
        execute();
    }

    @Nonnull
    private List<StorageUnitResponseErrorDto> errors() {
        return List.of(
            new StorageUnitResponseErrorDto(
                "1 error",
                StorageUnitResponseErrorType.INVALID_PARTNER,
                List.of(new StorageUnitResponseErrorDto.CargoUnit("id1", "segmentUuid1")),
                null
            ),
            new StorageUnitResponseErrorDto(
                "2 error",
                StorageUnitResponseErrorType.UNKNOWN_ERROR,
                List.of(
                    new StorageUnitResponseErrorDto.CargoUnit("id2", "segmentUuid2"),
                    new StorageUnitResponseErrorDto.CargoUnit("id3", "segmentUuid3")
                ),
                null
            )
        );
    }

    private void execute() {
        execute(List.of());
    }

    private void execute(List<StorageUnitResponseErrorDto> errors) {
        processor.execute(getDbQueuePayload(new StorageUnitDeleteSegmentResponseEvent(
            REQUEST_ID,
            new ResultDto(errors)
        )));
    }

    private void checkLogsForSuccess() {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=INFO\t\
                    format=plain\t\
                    payload=Deleting segment in sc 2 successfully completed\t\
                    request_id=request-id-test\
                    """
            );
    }

    private void checkLogsForError() {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=ERROR\t\
                    format=plain\t\
                    payload=Got error response at deleting segment in sc 2\t\
                    request_id=request-id-test\
                    """
            );
    }
}

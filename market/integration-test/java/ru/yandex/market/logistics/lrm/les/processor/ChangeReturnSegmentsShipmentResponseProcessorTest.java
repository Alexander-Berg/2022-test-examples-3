package ru.yandex.market.logistics.lrm.les.processor;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.dto.ResultDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorType;
import ru.yandex.market.logistics.les.tpl.StorageUnitUpdateSegmentResponseEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@DisplayName("Обработка ответа из леса об изменении отгрузки на СЦ")
@DatabaseSetup("/database/les/change-shipment/before/prepare.xml")
class ChangeReturnSegmentsShipmentResponseProcessorTest extends AbstractIntegrationTest {

    private static final String REQUEST_ID = "request-id-test";

    @Autowired
    private AsyncLesEventProcessor processor;

    @Test
    @DisplayName("Не найден бизнес-процесс")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void businessProcessNotFound() {
        softly.assertThatCode(() -> execute("non-existing-req-id", List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "No business process with requestId non-existing-req-id and type CHANGE_RETURN_SEGMENTS_SHIPMENT"
            );
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/les/change-shipment/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        execute();
    }

    @Test
    @DisplayName("Дубль успеха")
    @DatabaseSetup(
        value = "/database/les/change-shipment/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void doubleSuccess() {
        execute();
    }

    @Test
    @DisplayName("Ошибка в СЦ")
    @ExpectedDatabase(
        value = "/database/les/change-shipment/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorsReceived() {
        execute(errors());

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=ERROR\t\
                    format=plain\t\
                    payload=Got error response at updating segment in SC\t\
                    request_id=request-id-test\
                    """
            );
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
        execute(REQUEST_ID, List.of());
    }

    private void execute(List<StorageUnitResponseErrorDto> errors) {
        execute(REQUEST_ID, errors);
    }

    private void execute(String requestId, List<StorageUnitResponseErrorDto> errors) {
        processor.execute(getDbQueuePayload(new StorageUnitUpdateSegmentResponseEvent(
            requestId,
            new ResultDto(errors)
        )));
    }
}

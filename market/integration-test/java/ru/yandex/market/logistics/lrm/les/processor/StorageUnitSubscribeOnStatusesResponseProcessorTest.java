package ru.yandex.market.logistics.lrm.les.processor;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.dto.ResultDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorType;
import ru.yandex.market.logistics.les.tpl.StorageUnitSubscribeOnStatusesResponseEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;
import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@DisplayName("Обработка ответа о подписки грузомест на статусы в СЦ")
@DatabaseSetup("/database/tasks/return-segment/subscribe-on-statuses-in-sc/response/before/prepare.xml")
class StorageUnitSubscribeOnStatusesResponseProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private AsyncLesEventProcessor processor;

    @Test
    @DisplayName("Процесс существует, в ответе есть ошибка - статус FAIL")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/subscribe-on-statuses-in-sc/response/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processExistResponseWithErrorCreateNewTask() {
        processor.execute(getDbQueuePayload(
            new StorageUnitSubscribeOnStatusesResponseEvent(
                TEST_REQUEST_ID + "/1",
                new ResultDto(List.of(new StorageUnitResponseErrorDto(
                    "Error",
                    StorageUnitResponseErrorType.UNKNOWN_ERROR,
                    List.of(new StorageUnitResponseErrorDto.CargoUnit("1", "uid")),
                    null
                )))
            )
        ));
    }

    @Test
    @DisplayName("Процесс существует, в ответе нет ошибок - статус SUCCESS")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/subscribe-on-statuses-in-sc/response/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void processExistResponseWithoutError() {
        processor.execute(getDbQueuePayload(successEvent(TEST_REQUEST_ID + "/1")));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Процесс не найден")
    @MethodSource
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/subscribe-on-statuses-in-sc/response/before/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalid(@SuppressWarnings("unused") String name, String requestId, String message) {
        softly.assertThatCode(() -> processor.execute(getDbQueuePayload(successEvent(requestId))))
            .hasMessage(message)
            .isInstanceOf(RuntimeException.class);
    }

    @Nonnull
    private static Stream<Arguments> invalid() {
        return Stream.of(
            Arguments.of(
                "По идентификатору реквеста нет процесса",
                "reqId100",
                "No business process with requestId reqId100 and type SUBSCRIBE_SEGMENT_ON_STATUSES_IN_SC"
            ),
            Arguments.of(
                "По типу нет процесса",
                "test-request-id-3",
                "No business process with requestId test-request-id-3 and type SUBSCRIBE_SEGMENT_ON_STATUSES_IN_SC"
            ),
            Arguments.of(
                "Невалидный статус процесса",
                "test-request-id-2",
                "Got a different result SUCCESS on business process 2 in status FAIL"
            )
        );
    }

    @Nonnull
    private StorageUnitSubscribeOnStatusesResponseEvent successEvent(String requestId) {
        return new StorageUnitSubscribeOnStatusesResponseEvent(requestId, new ResultDto(List.of()));
    }
}

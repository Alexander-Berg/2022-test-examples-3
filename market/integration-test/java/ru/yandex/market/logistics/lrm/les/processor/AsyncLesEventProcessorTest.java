package ru.yandex.market.logistics.lrm.les.processor;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.OrderDamagedEvent;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.les.LesEventFactory;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

@DisplayName("Асинхронная обработка события из LES")
public class AsyncLesEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private AsyncLesEventProcessor processor;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Невалидный пэйлоад")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidPayload(@SuppressWarnings("unused") String name, EventPayload payload) {
        softly.assertThatCode(() -> processor.execute(LesEventFactory.getDbQueuePayload(payload)))
            .hasMessage("Invalid payload");
    }

    @Nonnull
    private static Stream<Arguments> invalidPayload() {
        return Stream.of(
            Arguments.of("Нет пэйлоада", null),
            Arguments.of("Для пэйлоада нет процессора", new OrderDamagedEvent())
        );
    }
}

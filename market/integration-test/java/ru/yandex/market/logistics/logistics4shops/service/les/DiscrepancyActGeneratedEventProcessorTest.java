package ru.yandex.market.logistics.logistics4shops.service.les;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.DropshipScDiscrepancyActGeneratedEvent;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.service.les.processor.DiscrepancyActGeneratedEventProcessor;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Обработка события генерации акта о расхождениях")
@ParametersAreNonnullByDefault
class DiscrepancyActGeneratedEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private DiscrepancyActGeneratedEventProcessor processor;

    @Test
    @DisplayName("Успешная обработка события")
    @ExpectedDatabase(
        value = "/service/les/discrepancyactgenerated/after/queue_task_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processingEventSuccess() {
        processor.process(
            new DropshipScDiscrepancyActGeneratedEvent(
                "transportation-id",
                "bucket",
                "filename",
                true
            ),
            "1"
        );
    }

    @DisplayName("Обработка невалидного ивента")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ExpectedDatabase(
        value = "/service/les/discrepancyactgenerated/after/queue_task_not_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void validationError(
        @SuppressWarnings("unused") String displayName,
        DropshipScDiscrepancyActGeneratedEvent invalidEvent
    ) {
        processor.process(invalidEvent, "1");
    }

    @Nonnull
    private static Stream<Arguments> validationError() {
        return Stream.of(
            Arguments.of(
                "no transportation",
                new DropshipScDiscrepancyActGeneratedEvent(null, "bucket", "filename", true)
            )
        );
    }
}

package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.DiscrepancyActGeneratedPayload;
import ru.yandex.market.logistics.logistics4shops.queue.payload.DiscrepancyActGeneratedPayload.DiscrepancyActGeneratedPayloadBuilder;

@DisplayName("Сохранение информацию об акте о расхождениях")
@ParametersAreNonnullByDefault
class SaveDiscrepancyActProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private SaveDiscrepancyActProcessor processor;

    @Test
    @DisplayName("Обработка существующего акта о расхождениях")
    @DatabaseSetup("/queue/processor/savediscrepancyact/before/prepare.xml")
    @ExpectedDatabase(
        value = "/queue/processor/savediscrepancyact/after/save_discrepancy_act.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void discrepancyActProcessing() {
        processor.execute(defaultPayloadBuilder().build());
    }

    @Test
    @DisplayName("Нет расхождений, есть путь до акта о расхождениях")
    @DatabaseSetup("/queue/processor/savediscrepancyact/before/prepare.xml")
    @ExpectedDatabase(
        value = "/queue/processor/savediscrepancyact/after/no_discrepancy_act.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noDiscrepancyActProcessing() {
        processor.execute(defaultPayloadBuilder().discrepancyExists(false).build());
    }

    @Test
    @DisplayName("Нет расхождений, нет пути до акта о расхождениях")
    @DatabaseSetup("/queue/processor/savediscrepancyact/before/prepare.xml")
    @ExpectedDatabase(
        value = "/queue/processor/savediscrepancyact/after/no_discrepancy_act.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noDiscrepancyActNoFilePathProcessing() {
        processor.execute(defaultPayloadBuilder().discrepancyExists(false).filename(null).bucket(null).build());
    }

    @Test
    @DisplayName("Перемещения не существует")
    @DatabaseSetup("/queue/processor/savediscrepancyact/before/prepare.xml")
    @ExpectedDatabase(
        value = "/queue/processor/savediscrepancyact/before/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noTransportation() {
        softly.assertThatThrownBy(
            () -> processor.execute(defaultPayloadBuilder().transportationId("no-such-transportation-id").build())
        )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [OUTBOUND] with transportationId no-such-transportation-id");
    }

    @Test
    @DisplayName("Существует более одного перемещения")
    @DatabaseSetup("/queue/processor/savediscrepancyact/before/prepare.xml")
    @DatabaseSetup(
        value = "/queue/processor/savediscrepancyact/before/additional_outbound.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/savediscrepancyact/after/multiple_transportations.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleTransportations() {
        softly.assertThatThrownBy(
            () -> processor.execute(defaultPayloadBuilder().build())
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("More than 1 transportation found for transportationId 123");
    }

    @DisplayName("Обработка невалидного payload-а")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void invalidPayload(
        @SuppressWarnings("unused") String displayName,
        DiscrepancyActGeneratedPayload invalidPayload,
        String interpolatedMessage,
        String propertyPath
    ) {
        softly.assertThatThrownBy(() -> processor.execute(invalidPayload))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("interpolatedMessage='%s'".formatted(interpolatedMessage))
            .hasMessageContaining("propertyPath=%s".formatted(propertyPath));
    }

    @Nonnull
    private static Stream<Arguments> invalidPayload() {
        return Stream.of(
            Arguments.of(
                "no transportationId",
                defaultPayloadBuilder().transportationId(null).build(),
                "must not be blank",
                "transportationId"
            ),
            Arguments.of(
                "blank transportationId",
                defaultPayloadBuilder().transportationId("").build(),
                "must not be blank",
                "transportationId"
            ),
            Arguments.of(
                "no bucket but discrepancyExists",
                defaultPayloadBuilder().bucket(null).build(),
                "bucket and filename must not be blank",
                ""
            ),
            Arguments.of(
                "blank bucket but discrepancyExists",
                defaultPayloadBuilder().bucket("").build(),
                "bucket and filename must not be blank",
                ""
            ),
            Arguments.of(
                "no filename but discrepancyExists",
                defaultPayloadBuilder().filename(null).build(),
                "bucket and filename must not be blank",
                ""
            ),
            Arguments.of(
                "blank filename but discrepancyExists",
                defaultPayloadBuilder().filename("").build(),
                "bucket and filename must not be blank",
                ""
            )
        );
    }

    @Nonnull
    private static DiscrepancyActGeneratedPayloadBuilder<?, ?> defaultPayloadBuilder() {
        return DiscrepancyActGeneratedPayload.builder()
            .transportationId("TM123")
            .bucket("bucket")
            .filename("filename")
            .discrepancyExists(true);
    }
}

package ru.yandex.market.logistics.management.queue.processor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.queue.model.LogisticSegmentValidationPayload;

public class LogisticSegmentValidationProcessorTest extends AbstractContextualAspectValidationTest {
    @Autowired
    private LogisticSegmentValidationProcessor logisticSegmentValidationProcessor;

    @Test
    @DatabaseSetup("/data/queue/processor/logistics_segment_movement_without_services.xml")
    @ExpectedDatabase(value = "/data/queue/processor/after/logistics_segment_movement_with_services_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void validateMovementSegment_failed() {
        logisticSegmentValidationProcessor.processPayload(getPayloadWIthSegment(3));
    }

    @Test
    @DatabaseSetup("/data/queue/processor/logistics_segment_movement_with_services.xml")
    @ExpectedDatabase(value = "/data/queue/processor/after/logistics_segment_movement_with_services.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void validateMovementSegment_valid() {
        logisticSegmentValidationProcessor.processPayload(getPayloadWIthSegment(3));
    }

    @Test
    @DatabaseSetup("/data/queue/processor/logistics_segment_without_services.xml")
    @ExpectedDatabase(value = "/data/queue/processor/after/logistics_segment_no_rules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void validateMovementSegment_no_rules() {
        logisticSegmentValidationProcessor.processPayload(getPayloadWIthSegment(3));
    }

    private static LogisticSegmentValidationPayload getPayloadWIthSegment(long segmentId) {
        return new LogisticSegmentValidationPayload("requestId", segmentId);
    }
}

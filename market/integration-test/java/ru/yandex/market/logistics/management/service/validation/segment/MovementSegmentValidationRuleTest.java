package ru.yandex.market.logistics.management.service.validation.segment;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentRepository;
import ru.yandex.market.logistics.management.service.validation.ValidationResult;

@Transactional
public class MovementSegmentValidationRuleTest extends AbstractContextualAspectValidationTest {
    private static final long SEGMENT_ID = 1001;

    @Autowired
    private LogisticSegmentRepository repository;
    @Autowired
    private MovementSegmentHasMovementServiceRule movementSegmentHasMovementServiceRule;

    @DatabaseSetup("/data/service/validation/movement/movement_segment.xml")
    @Test
    void testMovementSegmentHasMovementService_failed() {
        ValidationResult validationResult =
            movementSegmentHasMovementServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @DatabaseSetup({"/data/service/validation/movement/movement_segment.xml",
        "/data/service/validation/movement/movement_service.xml"})
    @Test
    void testMovementSegmentHasMovementService_ok() {
        ValidationResult validationResult =
            movementSegmentHasMovementServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }
    @DatabaseSetup({"/data/service/validation/movement/movement_segment.xml",
        "/data/service/validation/movement/movement_segment_with_tm_service.xml"})
    @Test
    void testMovementSegmentHasMovementService_tmService_notApplicable() {
        boolean canApplyRuleToSegment =
            movementSegmentHasMovementServiceRule.canApplyRuleToSegment(repository.getOne(SEGMENT_ID));
        softly.assertThat(canApplyRuleToSegment).isFalse();
    }
}

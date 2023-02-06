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
public class GoPlatformSegmentValidationRuleTest extends AbstractContextualAspectValidationTest {
    private static final long SEGMENT_ID = 1001;

    @Autowired
    private LogisticSegmentRepository repository;
    @Autowired
    private GoPlatformSegmentHasLocationIdRule goPlatformSegmentHasLocationIdRule;
    @Autowired
    private GoPlatformSegmentHasHandingServiceRule goPlatformSegmentHasHandingServiceRule;
    @Autowired
    private GoPlatformSegmentHasNoOutgoingSegmentsRule goPlatformSegmentHasNoOutgoingSegmentsRule;

    @Test
    @DatabaseSetup("/data/service/validation/go_platform/go_platform_segment.xml")
    void testGoPlatformHasLocationIdRule_ok() {
        ValidationResult validationResult = goPlatformSegmentHasLocationIdRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/go_platform/go_platform_segment_no_location_id.xml")
    void testGoPlatformHasLocationIdRule_fail() {
        ValidationResult validationResult = goPlatformSegmentHasLocationIdRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup("/data/service/validation/go_platform/go_platform_segment.xml")
    void testGoPlatformHasHandingServiceRule_fail() {
        ValidationResult validationResult =
            goPlatformSegmentHasHandingServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup({"/data/service/validation/go_platform/go_platform_segment.xml",
        "/data/service/validation/go_platform/handing_service.xml"})
    void testGoPlatformHasHandingServiceRule_ok() {
        ValidationResult validationResult =
            goPlatformSegmentHasHandingServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/go_platform/go_platform_segment.xml")
    void testGoPlatformHasNoOutgoingSegmentsRule_ok() {
        ValidationResult validationResult =
            goPlatformSegmentHasNoOutgoingSegmentsRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/go_platform/with_outgoing_segment.xml")
    void testGoPlatformHasNoOutgoingSegmentsRule_fail() {
        ValidationResult validationResult =
            goPlatformSegmentHasNoOutgoingSegmentsRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
        softly.assertThat(validationResult.getMessage())
            .isEqualTo("Go_platform must not have next segments. Next segments: [200]");
    }
}

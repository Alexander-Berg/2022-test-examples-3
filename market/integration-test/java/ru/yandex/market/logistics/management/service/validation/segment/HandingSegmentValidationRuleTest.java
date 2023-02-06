package ru.yandex.market.logistics.management.service.validation.segment;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.combinator.LogisticSegment;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentRepository;
import ru.yandex.market.logistics.management.service.validation.ValidationResult;

@Transactional
public class HandingSegmentValidationRuleTest extends AbstractContextualAspectValidationTest {
    private static final long SEGMENT_ID = 1001;

    @Autowired
    private LogisticSegmentRepository repository;
    @Autowired
    private HandingSegmentHasHandingServiceRule handingSegmentHasHandingServiceRule;
    @Autowired
    private HandingSegmentHasLocationIdRule handingSegmentHasLocationIdRule;
    @Autowired
    private HandingSegmentHasUniqueLocationIdAndPartnerRule handingSegmentHasUniqueLocationIdAndPartnerRule;

    @Test
    @DatabaseSetup("/data/service/validation/handing/handing_segment.xml")
    void testHandingSegmentHasHandingService_fail() {
        LogisticSegment segment = repository.getOne(SEGMENT_ID);
        boolean canApply = handingSegmentHasHandingServiceRule.canApplyRuleToSegment(segment);
        ValidationResult validationResult = handingSegmentHasHandingServiceRule.validate(segment);
        softly.assertThat(canApply).isTrue();
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup({"/data/service/validation/handing/handing_segment.xml",
        "/data/service/validation/handing/handing_service.xml"})
    void testHandingSegmentHasHandingService_ok() {
        LogisticSegment segment = repository.getOne(SEGMENT_ID);
        boolean canApply = handingSegmentHasHandingServiceRule.canApplyRuleToSegment(segment);
        ValidationResult validationResult = handingSegmentHasHandingServiceRule.validate(segment);
        softly.assertThat(canApply).isTrue();
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }
    @Test
    @DatabaseSetup({"/data/service/validation/handing/handing_segment_no_log_point.xml",
        "/data/service/validation/handing/handing_service.xml"})
    void testHandingSegmentHasHandingServiceWithoutLogisticPoint_ok() {
        LogisticSegment segment = repository.getOne(SEGMENT_ID);
        boolean canApply = handingSegmentHasHandingServiceRule.canApplyRuleToSegment(segment);
        ValidationResult validationResult = handingSegmentHasHandingServiceRule.validate(segment);
        softly.assertThat(canApply).isTrue();
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/handing/handing_segment_for_dbs.xml")
    void testHandingSegmentHasHandingService_notApplicableForDBS() {
        boolean canApply = handingSegmentHasHandingServiceRule.canApplyRuleToSegment(repository.getOne(SEGMENT_ID));
        softly.assertThat(canApply).isFalse();
    }

    @Test
    @DatabaseSetup("/data/service/validation/handing/handing_segment.xml")
    void testHandingSegmentHasLocationId_ok() {
        ValidationResult validationResult = handingSegmentHasLocationIdRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/handing/handing_segment_for_dbs.xml")
    void testHandingSegmentHasLocationId_fail() {
        ValidationResult validationResult = handingSegmentHasLocationIdRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup("/data/service/validation/handing/handing_segment.xml")
    void testHandingSegmentHasUniqueLocationIdAndPartner_ok() {
        ValidationResult validationResult =
            handingSegmentHasUniqueLocationIdAndPartnerRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup({"/data/service/validation/handing/handing_segment.xml",
        "/data/service/validation/handing/handing_segment_duplicating_location_and_partner.xml"})
    void testHandingSegmentHasUniqueLocationIdAndPartner_fail() {
        ValidationResult validationResult =
            handingSegmentHasUniqueLocationIdAndPartnerRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
        softly.assertThat(validationResult.getMessage())
            .isEqualTo("Handing segment must have unique pair locationId and partnerId. Other segments: [1002]");
    }

    @Test
    @DatabaseSetup("/data/service/validation/handing/handing_segment_with_inactive_logistics_point.xml")
    void testHandingSegmentWithInactiveLogisticsPoint_warn() {
        ValidationResult validationResult =
            handingSegmentHasHandingServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.WARN);
        softly.assertThat(validationResult.getMessage())
            .isEqualTo("Logistics point 505 for this segment is not active");
    }
}

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
public class PickupSegmentValidationRuleTest extends AbstractContextualAspectValidationTest {
    private static final long SEGMENT_ID = 1001;

    @Autowired
    private LogisticSegmentRepository repository;
    @Autowired
    private PickupSegmentHasLogisticsPointIdRule pickupSegmentHasLogisticsPointIdRule;
    @Autowired
    private PickupSegmentHasHandingServiceRule pickupSegmentHasHandingServiceRule;
    @Autowired
    private PickupSegmentHasPaymentTypeRule pickupSegmentHasPaymentTypeRule;

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment.xml")
    void testPickupSegmentHasLogisticsPoint_ok() {
        ValidationResult validationResult =
            pickupSegmentHasLogisticsPointIdRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment_no_log_point.xml")
    void testPickupSegmentHasLogisticsPoint_fail() {
        ValidationResult validationResult =
            pickupSegmentHasLogisticsPointIdRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment.xml")
    void testPickupSegmentHasHandingService_fail() {
        ValidationResult validationResult = pickupSegmentHasHandingServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup({"/data/service/validation/pickup/pickup_segment.xml",
        "/data/service/validation/pickup/handing_service.xml"})
    void testPickupSegmentHasHandingService_ok() {
        ValidationResult validationResult = pickupSegmentHasHandingServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment.xml")
    void testPickupPointHasPaymentTypeRule_ok() {
        ValidationResult validationResult = pickupSegmentHasPaymentTypeRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment_no_payment_type.xml")
    void testPickupPointHasPaymentTypeRule_fail() {
        ValidationResult validationResult = pickupSegmentHasPaymentTypeRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
        softly.assertThat(validationResult.getMessage())
            .isEqualTo("Logistics point 505 must have at least one payment type allowed");
    }

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment_no_payment_type_dsbs.xml")
    void testPickupPointHasPaymentTypeRule_dsbs_no_payment_ok() {
        boolean canApply = pickupSegmentHasPaymentTypeRule.canApplyRuleToSegment(repository.getOne(SEGMENT_ID));
        softly.assertThat(canApply).isFalse();
    }

    @Test
    @DatabaseSetup("/data/service/validation/pickup/pickup_segment_inactive_logistics_point.xml")
    void testPickupSegmentHasHandingService_inactiveLogisticsPoint_warn() {
        ValidationResult validationResult = pickupSegmentHasHandingServiceRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.WARN);
        softly.assertThat(validationResult.getMessage())
            .isEqualTo("Logistics point 505 for this segment is not active");
    }

}

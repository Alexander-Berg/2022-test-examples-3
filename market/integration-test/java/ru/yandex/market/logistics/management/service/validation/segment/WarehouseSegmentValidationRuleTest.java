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
public class WarehouseSegmentValidationRuleTest extends AbstractContextualAspectValidationTest {
    private static final long SEGMENT_ID = 1001;

    @Autowired
    private LogisticSegmentRepository repository;
    @Autowired
    private WarehouseSegmentHasProcessingOrSortServiceRule warehouseRule;

    @Test
    @DatabaseSetup("/data/service/validation/warehouse/warehouse_segment.xml")
    void testWarehouseHasServices_failed() {
        ValidationResult validationResult = warehouseRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.FAILED);
    }

    @Test
    @DatabaseSetup({"/data/service/validation/warehouse/warehouse_segment.xml",
        "/data/service/validation/warehouse/sort_service.xml"})
    void testWarehouseHasServices_sortService_ok() {
        ValidationResult validationResult = warehouseRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup({"/data/service/validation/warehouse/warehouse_segment.xml",
        "/data/service/validation/warehouse/processing_service.xml"})
    void testWarehouseHasServices_processingService_ok() {
        ValidationResult validationResult = warehouseRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.OK);
    }

    @Test
    @DatabaseSetup("/data/service/validation/warehouse/warehouse_segment_inactive_logistic_point.xml")
    void testWarehouseHasServices_inactiveLogisticsPoint_warn() {
        ValidationResult validationResult = warehouseRule.validate(repository.getOne(SEGMENT_ID));
        softly.assertThat(validationResult.getStatus()).isEqualTo(ValidationStatus.WARN);
        softly.assertThat(validationResult.getMessage())
            .isEqualTo("Logistics point 505 for this segment is not active");
    }

}

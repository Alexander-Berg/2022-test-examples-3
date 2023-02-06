package ru.yandex.market.logistics.management.service.partner.warehouse;

import java.time.Duration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.exception.BadRequestException;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;

import static ru.yandex.market.logistics.management.service.partner.warehouse.WarehouseHandlingDurationService.INCOMPATIBLE_PARTNER_TYPE;
import static ru.yandex.market.logistics.management.service.partner.warehouse.WarehouseHandlingDurationService.MISSING_PARTNER;
import static ru.yandex.market.logistics.management.service.partner.warehouse.WarehouseHandlingDurationService.MISSING_POINT_OR_SEGMENT;
import static ru.yandex.market.logistics.management.service.partner.warehouse.WarehouseHandlingDurationService.MISSING_SERVICE;
import static ru.yandex.market.logistics.management.service.partner.warehouse.WarehouseHandlingDurationService.MULTIPLE_POINTS_OR_SEGMENTS;

public class WarehouseHandlingDurationServiceTest extends AbstractContextualTest {

    @Autowired
    private WarehouseHandlingDurationService warehouseHandlingDurationService;

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDurationSuccessful() {
        Duration duration = warehouseHandlingDurationService.getDuration(1L);
        softly.assertThat(duration)
            .isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    void testGetDurationPartnerDoesNotExist() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.getDuration(999L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(String.format(MISSING_PARTNER, 999L));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDurationPartnerWithIncompatibleType() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.getDuration(2L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(String.format(INCOMPATIBLE_PARTNER_TYPE, PartnerType.OWN_DELIVERY.name()));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDurationMissingLogisticPoint() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.getDuration(3L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(String.format(MISSING_POINT_OR_SEGMENT, 3L));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDurationMultipleSegments() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.getDuration(4L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(String.format(MULTIPLE_POINTS_OR_SEGMENTS, 4L));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDurationMissingSegment() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.getDuration(5L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(String.format(MISSING_POINT_OR_SEGMENT, 5L));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDurationMissingSortService() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.getDuration(6L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(String.format(MISSING_SERVICE, 400L, ServiceCodeName.SORT.name()));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_update_successful.xml")
    @ExpectedDatabase(
        value = "/data/service/partner/after/warehouse_duration_update_successful.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateDurationSuccessful() {
        warehouseHandlingDurationService.updateDuration(1L, Duration.ofMinutes(10));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_update_missing_service.xml")
    void testUpdateDurationMissingService() {
        softly.assertThatThrownBy(() -> warehouseHandlingDurationService.updateDuration(1L, Duration.ofMinutes(10)))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(String.format(MISSING_SERVICE, 100L, ServiceCodeName.PROCESSING.name()));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_update_missing_handling_time.xml")
    @ExpectedDatabase(
        value = "/data/service/partner/after/warehouse_duration_update_missing_handling_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateDurationMissingHandlingTime() {
        warehouseHandlingDurationService.updateDuration(1L, Duration.ofMinutes(10));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_update_for_supplier.xml")
    @ExpectedDatabase(
        value = "/data/service/partner/after/warehouse_duration_update_for_supplier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateSupplierDuration() {
        warehouseHandlingDurationService.updateDuration(1L, Duration.ofMinutes(10));
    }

}

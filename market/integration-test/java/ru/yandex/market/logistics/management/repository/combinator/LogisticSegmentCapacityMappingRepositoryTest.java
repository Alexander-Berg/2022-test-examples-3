package ru.yandex.market.logistics.management.repository.combinator;

import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@CleanDatabase
@DatabaseSetup("/data/controller/admin/logisticsSegmentsMapping/prepare_data.xml")
class LogisticSegmentCapacityMappingRepositoryTest extends AbstractContextualTest {

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_mapped_service_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mappingCreationOnServiceInsert() {
        assertDoesNotThrow(() -> createService(30L, 10L));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_mapped_service_unmapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mappingRemovalOnNullCapacityUpdate() {
        assertDoesNotThrow(() -> updateServiceCapacity(20, null));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_mapped_service_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mappingRemovalOnMappedServiceRemovalCapacityUpdate() {
        assertDoesNotThrow(() -> deleteService(20));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_unmapped_service_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removalOfUnmappedServiceDoesNotThrow() {
        assertDoesNotThrow(() -> deleteService(10));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_unmapped_service_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertOfUnmappedServiceDoesNotThrow() {
        assertDoesNotThrow(() -> createService(30L, null));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_unmapped_service_mapping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mappingCreationOnNonNullCapacityUpdate() {
        assertDoesNotThrow(() -> updateServiceCapacity(10L, 20L));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_mapped_service_capacity_id_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mappingUpdateOnServiceCapacityUpdate() {
        assertDoesNotThrow(() -> updateServiceCapacity(20L, 20L));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsSegmentsMapping/after/after_duplicate_mapped_service_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void duplicateMappingHandledCorrectly() {
        jdbcTemplate.update("INSERT INTO logistic_segments_services_capacity_mapping " +
            "(service_id, capacity_id) VALUES (30, 30)");

        assertDoesNotThrow(() -> createService(30L, 30L));
    }

    private void deleteService(long serviceId) {
        jdbcTemplate.update("DELETE FROM logistic_segments_services WHERE id=?", serviceId);
    }

    private void createService(long serviceId, @Nullable Long capacityId) {
        jdbcTemplate.update("INSERT INTO logistic_segments_services " +
            "(id, segment_id, capacity_id, code, duration, price, delivery_type, status, frozen)" +
            "VALUES (?, '10002', ?, 3, 32, 113, 'courier', 'active', false)", serviceId, capacityId);
    }

    private void updateServiceCapacity(long serviceId, @Nullable Long capacityId) {
        jdbcTemplate.update("UPDATE logistic_segments_services SET capacity_id=? WHERE id=?", capacityId, serviceId);
    }
}

package ru.yandex.market.logistics.cs.versioning;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.entity.ServiceCapacityMapping;
import ru.yandex.market.logistics.cs.repository.ServiceCapacityMappingRepository;

@DisplayName("Версионирование связок сервис - капасити")
@DatabaseSetup("/repository/versioning/before/base_versioning.xml")
class ServiceCapacityMappingVersioningTest extends AbstractIntegrationTest {

    public static final long CAPACITY_ID = 1L;
    public static final long EXISTING_SERVICE_ID = 10L;
    public static final long DELETED_BEFORE_SERVICE_ID = 1000L;
    public static final long NEW_SERVICE_ID = 20L;

    @Autowired
    private ServiceCapacityMappingRepository repository;

    @Test
    @DisplayName("Добавление новой связки")
    @ExpectedDatabase(
        value = "/repository/versioning/after/add_new_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newMappingVersionIs1() {
        softly.assertThat(repository.save(mapping(NEW_SERVICE_ID))).isNotNull();
    }

    @Test
    @DisplayName("Добавление ранее удаленной связки")
    @ExpectedDatabase(
        value = "/repository/versioning/after/add_old_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deletedMappingHasBeenAdded() {
        softly.assertThat(repository.save(mapping(DELETED_BEFORE_SERVICE_ID))).isNotNull();
    }

    @Test
    @DisplayName("Удаление связки")
    @ExpectedDatabase(
        value = "/repository/versioning/after/update_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteMapping() {
        repository.delete(mapping(EXISTING_SERVICE_ID));
        softly.assertThat(repository.findById(EXISTING_SERVICE_ID)).isEmpty();
    }

    @Test
    @DisplayName("Изменение связки")
    @ExpectedDatabase(
        value = "/repository/versioning/after/update_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateMapping() {
        ServiceCapacityMapping serviceCapacityMapping = repository.findById(EXISTING_SERVICE_ID)
            .orElseThrow(() -> new RuntimeException("mapping doesn't found"));

        serviceCapacityMapping = serviceCapacityMapping.toBuilder()
            .capacityId(CAPACITY_ID + 1)
            .build();

        softly.assertThat(repository.save(serviceCapacityMapping)).isNotNull();
    }

    private ServiceCapacityMapping mapping(Long serviceId) {
        return ServiceCapacityMapping.builder()
            .id(serviceId)
            .serviceId(serviceId)
            .capacityId(CAPACITY_ID)
            .build();
    }
}

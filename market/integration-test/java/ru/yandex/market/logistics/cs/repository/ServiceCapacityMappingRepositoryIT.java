package ru.yandex.market.logistics.cs.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("Репозиторий для работы с маппингами сервисов на капасити")
class ServiceCapacityMappingRepositoryIT extends AbstractIntegrationTest {
    @Autowired
    private ServiceCapacityMappingRepository repository;

    @Test
    @DatabaseSetup("/repository/service_capacity_mapping/before/before_adding_multiple_rows.xml")
    @ExpectedDatabase(
        value = "/repository/service_capacity_mapping/after/after_adding_multiple_rows.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Импорт работает при добавлении сразу нескольких маппингов")
    void mergeAddedMappings() {
        assertDoesNotThrow(() -> repository.mergeAddedMappings());
    }
}

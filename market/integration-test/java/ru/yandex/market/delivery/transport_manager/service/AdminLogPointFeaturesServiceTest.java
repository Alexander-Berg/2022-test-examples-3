package ru.yandex.market.delivery.transport_manager.service;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.NewLogPointFeatureDto;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminLogPointFeatureType;
import ru.yandex.market.delivery.transport_manager.exception.AdminValidationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminLogPointFeaturesServiceTest extends AbstractContextualTest {
    @Autowired
    private AdminLogPointFeaturesService service;

    @Test
    @ExpectedDatabase(
        value = "/repository/logpoint_features/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldCreateLogPointFeature() {
        var dto = new NewLogPointFeatureDto()
            .setLogisticsPointId(10001L)
            .setFeatureType(AdminLogPointFeatureType.DROPOFF_WITH_ENABLED_RETURN);
        service.create(dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/logpoint_features/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldFailWhenCreateTheSame() {
        var dto = new NewLogPointFeatureDto()
            .setLogisticsPointId(10001L)
            .setFeatureType(AdminLogPointFeatureType.DROPOFF_WITH_ENABLED_RETURN);
        service.create(dto);
        assertThrows(AdminValidationException.class, () -> service.create(dto));
    }

    @Test
    @DatabaseSetup("/repository/logpoint_features/few_records.xml")
    @ExpectedDatabase(
            value = "/repository/logpoint_features/after_delete.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldDeleteByIds() {
        service.deleteByIds(Set.of(2L, 3L, 4L, 6L));
    }
}

package ru.yandex.market.logistics.management.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.SettingsApi;
import ru.yandex.market.logistics.management.domain.entity.SettingsMethod;
import ru.yandex.market.logistics.management.domain.entity.type.MethodType;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@DatabaseSetup(
    value = "/data/repository/settings/partner_with_settings_and_methods.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class SettingsMethodRepositoryTest extends AbstractContextualTest {

    @Autowired
    private SettingsMethodRepository settingsMethodRepository;

    @Test
    void testFindAll() {
        List<SettingsMethod> all = settingsMethodRepository.findAll();
        softly.assertThat(all)
            .as("Methods exist")
            .isNotNull()
            .as("There are seven methods")
            .hasSize(7);
    }

    @Test
    void testFindAllSettingsMethodsByPartnerId() {
        Collection<SettingsMethod> methods = settingsMethodRepository.findAllBySettingsApiPartnerId(1L);

        softly.assertThat(methods)
            .as("Methods actually exist")
            .isNotEmpty()
            .as("There are four methods for this partner")
            .hasSize(4);
    }

    @Test
    void testFindAllByMethodIn() {
        List<SettingsMethod> methods = settingsMethodRepository
            .findAllByActiveIsTrueAndCronExpressionIsNotNullAndMethodIsIn(
                List.of(
                    MethodType.GET_REFERENCE_PICKUP_POINTS,
                    MethodType.GET_REFERENCE_WAREHOUSES)
            );

        softly.assertThat(methods)
            .as("There are two such methods")
            .hasSize(2)
            .as("Partner 1 has both methods")
            .extracting(SettingsMethod::getSettingsApi)
            .extracting(SettingsApi::getId)
            .containsExactly(1L, 1L);
    }

    @Test
    void findBySettingsApiPartnerIdAndMethod() {
        Optional<SettingsMethod> actualMethod = settingsMethodRepository.findBySettingsApiPartnerIdAndMethod(
            1L,
            MethodType.GET_REFERENCE_PICKUP_POINTS
        );
        softly.assertThat(actualMethod).map(SettingsMethod::getId).contains(5L);
    }
}

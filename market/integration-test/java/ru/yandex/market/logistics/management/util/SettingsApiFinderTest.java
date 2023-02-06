package ru.yandex.market.logistics.management.util;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.SettingsApi;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.management.repository.SettingsApiRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DatabaseSetup(
    value = "/data/util/before/settings_api_finder_setup.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class SettingsApiFinderTest extends AbstractContextualTest {

    @Autowired
    private SettingsApiRepository settingsApiRepository;

    @Autowired
    private SettingsApiFinder settingsApiFinder;

    @Test
    void findSettingsApi_returnsEmptyWhenSettingsApiNotFound() {
        assertThat(settingsApiFinder.findSettingsApi(1234L, null))
            .isEmpty();
    }

    @Test
    void findSettingsApi_returnsSettingsApiForGivenType() {
        Optional<SettingsApi> expectedSettings = settingsApiRepository.findById(201L);

        assertThat(expectedSettings).as("Expected settings to be in DB!").isNotEmpty();
        assertThat(settingsApiFinder.findSettingsApi(101L, ApiType.DELIVERY))
            .isEqualTo(expectedSettings);
    }

    @Test
    void findSettingsApi_returnsEmptyWhenGivenTypeIsNotInDb() {
        assertThat(settingsApiFinder.findSettingsApi(103L, ApiType.DELIVERY))
            .isEmpty();
    }

    @Test
    void findSettingsApi_returnsSettingsApiWithInferredType() {
        Optional<SettingsApi> expectedSettings = settingsApiRepository.findById(201L);

        assertThat(expectedSettings).as("Expected settings to be in DB!").isNotEmpty();
        assertThat(settingsApiFinder.findSettingsApi(101L, null))
            .isEqualTo(expectedSettings);
    }

    @Test
    void findSettingsApi_returnsEmptyWhenCannotInferApiTypeAndNullTypeDoesNotExist() {
        assertThat(settingsApiFinder.findSettingsApi(103L, null))
            .isEmpty();
    }

    @Test
    void findSettingsApiWithMethod_returnsSettingsApiForMethod() {
        Optional<SettingsApi> expectedSettings = settingsApiRepository.findById(201L);

        assertThat(expectedSettings).as("Expected settings to be in DB!").isNotEmpty();
        assertThat(settingsApiFinder.findSettingsApiWithMethod(101L, "createOrder", ApiType.DELIVERY))
            .isEqualTo(expectedSettings);
    }
}

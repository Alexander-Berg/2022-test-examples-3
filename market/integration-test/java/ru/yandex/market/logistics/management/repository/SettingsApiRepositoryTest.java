package ru.yandex.market.logistics.management.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.SettingsApi;
import ru.yandex.market.logistics.management.domain.entity.type.FormatType;
import ru.yandex.market.logistics.management.domain.entity.type.MethodType;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@SuppressWarnings({"unchecked", "checkstyle:MagicNumber"})
class SettingsApiRepositoryTest extends AbstractContextualTest {

    @Autowired
    private SettingsApiRepository settingsApiRepository;

    @Test
    @DatabaseSetup(
        value = "/data/repository/settings/partner_with_settings_api.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testFindByPartnerId() {
        SettingsApi byPartnerId = settingsApiRepository.findAllByPartnerId(1L).stream().findFirst().orElse(null);

        softly.assertThat(byPartnerId)
            .as("Settings actually exist")
            .isNotNull()
            .as("Settings fields are correct")
            .extracting(
                SettingsApi::getApiType,
                SettingsApi::getToken,
                SettingsApi::getFormat,
                SettingsApi::getVersion
            )
            .containsExactly(ApiType.FULFILLMENT, "token2", FormatType.JSON, "2.0");
    }

    @Test
    @DatabaseSetup(
        value = "/data/repository/settings/partner_with_settings_and_methods.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testFindSettingsMethodByPartnerIdAndMethodType() {
        SettingsApi method = settingsApiRepository
            .findAllByPartnerIdAndMethodType(1L, MethodType.CREATE_ORDER)
            .stream()
            .findFirst()
            .orElse(null);

        softly.assertThat(method)
            .as("Settings exists")
            .isNotNull()
            .as("Settings fields are correct")
            .extracting(
                SettingsApi::getToken,
                SettingsApi::getFormat,
                SettingsApi::getVersion
            )
            .containsExactly("token1", FormatType.JSON, "1.0");
    }

    @Test
    @DatabaseSetup(
        value = "/data/repository/settings/partner_with_settings_and_methods.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void testFindSettingsWithMethodsByPartnerId() {
        SettingsApi settings = settingsApiRepository.findWithMethodsByPartnerId(1L)
            .orElse(null);

        softly.assertThat(settings)
            .as("Settings are not null")
            .isNotNull()
            .as("Settings have methods")
            .extracting(SettingsApi::getMethods)
            .isNotNull();
    }
}

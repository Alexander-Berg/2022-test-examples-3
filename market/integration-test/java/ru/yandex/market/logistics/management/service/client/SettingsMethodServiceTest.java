package ru.yandex.market.logistics.management.service.client;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodCreateDto;

@DatabaseSetup(
    value = {
        "/data/repository/settings/partner_with_settings_and_methods.xml",
        "/data/service/client/before/settings_method_sync_addon.xml"
    },
    connection = "dbUnitQualifiedDatabaseConnection"
)
class SettingsMethodServiceTest extends AbstractContextualTest {

    @Autowired
    private SettingsMethodService settingsMethodService;

    @Test
    @ExpectedDatabase(
        value = "/data/service/client/after/method_7_added_to_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void updateMethod_addsActiveMethodToSyncTable() {
        SettingsMethodCreateDto settingsMethodCreateDto = SettingsMethodCreateDto.newBuilder()
            .method("getReferencePickupPoints")
            .url("testurl7")
            .active(true)
            .build();

        settingsMethodService.updateMethod(7L, settingsMethodCreateDto);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/client/before/settings_method_sync_addon.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void updateMethod_doesNotAddActiveMethodToSyncTableWhenItIsAlreadyPresent() {
        SettingsMethodCreateDto settingsMethodCreateDto = SettingsMethodCreateDto.newBuilder()
            .method("getReferencePickupPoints")
            .url("testurl5")
            .cronExpression("0 0 0/4 ? * * *")
            .build();

        settingsMethodService.updateMethod(5L, settingsMethodCreateDto);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/client/after/method_5_removed_from_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void updateMethod_removesInactiveMethodFromSyncTable() {
        SettingsMethodCreateDto settingsMethodCreateDto = SettingsMethodCreateDto.newBuilder()
            .method("getReferencePickupPoints")
            .url("testurl5")
            .active(false)
            .build();

        settingsMethodService.updateMethod(5L, settingsMethodCreateDto);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/client/before/settings_method_sync_addon.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void updateMethod_doesNotFailWhenMethodToRemoveIsNotInSyncTable() {
        SettingsMethodCreateDto settingsMethodCreateDto = SettingsMethodCreateDto.newBuilder()
            .method("getReferencePickupPoints")
            .url("testurl7")
            .cronExpression("0 0 0 ? * * *")
            .build();

        settingsMethodService.updateMethod(7L, settingsMethodCreateDto);
    }
}

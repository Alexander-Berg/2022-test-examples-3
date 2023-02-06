package ru.yandex.market.abo.util.db.toggle;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 10/11/2020.
 */
class DbToggleServiceTest extends EmptyTest {
    private static final CoreConfig CONFIG = CoreConfig.PREMOD_INBOX_LIMIT;

    @Autowired
    private ConfigurationService aboConfigurationService;
    private DbToggleService dbToggleServiceTesting;
    private List<DbToggleService> toggles;

    @BeforeEach
    void setUp() {
        dbToggleServiceTesting = new DbToggleService(aboConfigurationService, "testing");
        DbToggleService dbToggleServiceProd = new DbToggleService(aboConfigurationService, "production");
        toggles = List.of(dbToggleServiceTesting, dbToggleServiceProd);
    }

    @Test
    void toggleDisabled() {
        aboConfigurationService.mergeValue(CONFIG.getIdAsString(), 0L);

        toggles.forEach(toggleService -> {
            assertTrue(toggleService.configDisabled(CONFIG));
            assertFalse(toggleService.configEnabled(CONFIG));
        });
    }

    @Test
    void configAbsent() {
        aboConfigurationService.deleteValue(CONFIG.getIdAsString());
        toggles.forEach(toggleService -> {
            assertTrue(toggleService.configDisabled(CONFIG));
            assertFalse(toggleService.configEnabled(CONFIG));
        });
    }

    @Test
    void configEnabled() {
        aboConfigurationService.mergeValue(CONFIG.getIdAsString(), 1L);

        toggles.forEach(toggleService -> {
            assertFalse(toggleService.configDisabled(CONFIG));
            assertTrue(toggleService.configEnabled(CONFIG));
        });
    }

    @AfterEach
    void tearDown() {
        assertFalse(dbToggleServiceTesting.configDisabledInProduction(CONFIG));
        assertTrue(dbToggleServiceTesting.configEnabledInProduction(CONFIG));

        aboConfigurationService.deleteValue(CONFIG.getIdAsString());
    }
}
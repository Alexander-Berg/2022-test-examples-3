package ru.yandex.market.health.ui.features.logshatter_config;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.health.ui.TestConfig;
import ru.yandex.market.health.ui.features.common.view_model.VersionedConfigIdViewModel;
import ru.yandex.market.health.ui.features.logshatter_config.view_model.LogshatterConfigVersionViewModel;
import ru.yandex.market.health.ui.features.logshatter_config.view_model.LogshatterConfigViewModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тут проверяем только что хоть одна ручка работает. Методы общего для Кликфита и Логшаттера HealthConfigController'а
 * тестируются тестами ClickphiteConfigController'а.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LogshatterConfigControllerGetConfigTest extends LogshatterConfigControllerBaseTest {
    @Test
    public void foundWithHistoryAndActiveVersion() throws Exception {
        dao.createConfig(config("id1", Instant.ofEpochSecond(1)));
        dao.activateVersion(dao.createValidVersion(version("id1"), null), "user42");
        dao.createValidVersion(version("id1"), null);

        LogshatterConfigViewModel config = deserializeConfig(
            mockMvc.perform(get("/api/logshatter/config?id=id1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
        );

        assertEquals("id1", config.getId());
        assertEquals(0, config.getCurrentVersion().getId().getVersionNumber());
        assertEquals("user42", config.getActivatedBy());

        assertThat(config.getAllVersions())
            .extracting(LogshatterConfigVersionViewModel::getId)
            .extracting(VersionedConfigIdViewModel::getConfigId, VersionedConfigIdViewModel::getVersionNumber)
            .containsExactly(tuple("id1", 0L), tuple("id1", 1L));
    }
}

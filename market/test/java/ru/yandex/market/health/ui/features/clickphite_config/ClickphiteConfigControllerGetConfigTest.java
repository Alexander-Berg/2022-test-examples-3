package ru.yandex.market.health.ui.features.clickphite_config;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceSplitOrFieldEntity;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.ui.TestConfig;
import ru.yandex.market.health.ui.features.clickphite_config.view_model.ClickphiteConfigGroupVersionViewModel;
import ru.yandex.market.health.ui.features.clickphite_config.view_model.ClickphiteConfigGroupViewModel;
import ru.yandex.market.health.ui.features.common.view_model.VersionedConfigIdViewModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.health.ui.TestUtils.loadFromClasspath;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClickphiteConfigControllerGetConfigTest extends ClickphiteConfigControllerBaseTest {

    private static final String CONFIG_ID = "id1";

    @Test
    public void notFound() throws Exception {
        mockMvc.perform(get("/api/clickphite/config?id=id1"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void foundWithoutHistory() throws Exception {
        dao.createConfig(config("id1", Instant.ofEpochSecond(1)));

        ClickphiteConfigGroupViewModel config = deserializeConfig(
            mockMvc.perform(get("/api/clickphite/config?id=id1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
        );

        assertEquals("id1", config.getId());
        assertTrue(config.getAllVersions().isEmpty());
    }

    @Test
    public void foundWithHistoryAndActiveVersion() throws Exception {
        dao.createConfig(config("id1", Instant.ofEpochSecond(1)));
        dao.activateVersion(dao.createValidVersion(version("id1"), null), "user42");
        dao.createValidVersion(version("id1"), null);

        ClickphiteConfigGroupViewModel config = deserializeConfig(
            mockMvc.perform(get("/api/clickphite/config?id=id1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
        );

        assertEquals("id1", config.getId());
        assertEquals(0, config.getCurrentVersion().getId().getVersionNumber());
        assertEquals("user42", config.getActivatedBy());

        assertThat(config.getAllVersions())
            .extracting(ClickphiteConfigGroupVersionViewModel::getId)
            .extracting(VersionedConfigIdViewModel::getConfigId, VersionedConfigIdViewModel::getVersionNumber)
            .containsExactly(tuple("id1", 0L), tuple("id1", 1L));
    }

    @Test
    public void graphiteSolomonSplitWhitelistSettingsSerialization() throws Exception {
        splitWhitelistSettingsSerialization(new ClickphiteConfigGroupVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id(CONFIG_ID, null),
            null,
            null,
            "owner1",
            new ClickphiteConfigEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                new GraphiteMetricsAndSolomonSensorsEntity(
                    Collections.singletonList(new SplitEntity(
                        null,
                        null,
                        createSplitWhitelistSettings()
                    )),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                null
            ),
            null
        ), "graphite_solomon_configs_with_split_whitelist_settings.json");
    }

    @Test
    public void statfaceSolomonSplitWhitelistSettingsSerialization() throws Exception {
        splitWhitelistSettingsSerialization(new ClickphiteConfigGroupVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id(CONFIG_ID, null),
            null,
            null,
            "owner1",
            null,
            Collections.singletonList(new ClickphiteConfigEntity(
                new TableEntity(null, "table1"),
                null,
                Collections.singletonList(MetricPeriod.ONE_MIN),
                null,
                null,
                null,
                null,
                new StatfaceReportEntity(
                    "title1",
                    "report1",
                    Collections.singletonList(new StatfaceSplitOrFieldEntity(
                        "some_name",
                        null,
                        null,
                        null,
                        null,
                        null,
                        createSplitWhitelistSettings()
                    )),
                    null,
                    null
                )
            ))
        ), "statface_configs_with_split_whitelist_settings.json");
    }

    private void splitWhitelistSettingsSerialization(ClickphiteConfigGroupVersionEntity entity,
                                                     String expectedResultResourcePath) throws Exception {
        dao.createConfig(config(CONFIG_ID));
        final VersionedConfigEntity.VersionEntity.Id version = dao.createValidVersion(entity, null);
        dao.activateVersion(version, "user42");
        mockMvc.perform(get("/api/clickphite/config?id=" + CONFIG_ID))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andExpect(content().json(loadFromClasspath(expectedResultResourcePath)));
    }

    private SplitWhitelistSettingsEntity createSplitWhitelistSettings() {
        return new SplitWhitelistSettingsEntity(
            Arrays.asList("sample-whitelist-entry-1", "sample-whitelist-entry-2"),
            new SplitWhitelistAutoUpdateEntity(
                7,
                500
            ),
            false
        );
    }

}

package ru.yandex.market.health.ui.features.clickphite_config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.ui.TestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClickphiteConfigControllerActivateConfigVersionTest extends ClickphiteConfigControllerBaseTest {
    @Test
    public void noConfigId() throws Exception {
        mockMvc.perform(post("/api/clickphite/config/version/activate?versionNumber=0"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void noVersionNumber() throws Exception {
        mockMvc.perform(post("/api/clickphite/config/version/activate?configId=id1"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void configNotFound() throws Exception {
        mockMvc.perform(post("/api/clickphite/config/version/activate?configId=id1&versionNumber=0"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void configVersionNotFound() throws Exception {
        dao.createConfig(config("id1"));

        mockMvc.perform(post("/api/clickphite/config/version/activate?configId=id1&versionNumber=0"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void created() throws Exception {
        dao.createConfig(config("id1"));
        dao.createValidVersion(version("id1"), null);

        mockMvc.perform(post("/api/clickphite/config/version/activate?configId=id1&versionNumber=0"))
            .andExpect(status().isOk());

        assertThat(dao.getConfig("id1").getCurrentVersion())
            .extracting(VersionedConfigEntity.VersionEntity::getId)
            .isEqualToComparingFieldByField(new VersionedConfigEntity.VersionEntity.Id("id1", 0L));
    }
}

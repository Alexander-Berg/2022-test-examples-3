package ru.yandex.market.health.ui.features.clickphite_config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.health.ui.TestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClickphiteConfigControllerDeactivateConfigTest extends ClickphiteConfigControllerBaseTest {
    @Test
    public void noConfigId() throws Exception {
        mockMvc.perform(post("/api/clickphite/config/deactivate"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void configNotFound() throws Exception {
        mockMvc.perform(post("/api/clickphite/config/deactivate?configId=id1"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void configDoesNotHaveCurrentVersion() throws Exception {
        dao.createConfig(config("id1"));

        mockMvc.perform(post("/api/clickphite/config/deactivate?configId=id1"))
            .andExpect(status().isOk());
    }

    @Test
    public void deactivated() throws Exception {
        dao.createConfig(config("id1"));
        dao.activateVersion(dao.createValidVersion(version("id1"), null), null);

        mockMvc.perform(post("/api/clickphite/config/deactivate?configId=id1"))
            .andExpect(status().isOk());

        assertThat(dao.getConfig("id1").getCurrentVersion()).isNull();
    }
}

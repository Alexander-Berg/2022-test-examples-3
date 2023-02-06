package ru.yandex.market.health.ui.features.clickphite_config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
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
public class ClickphiteConfigControllerCreateConfigTest extends ClickphiteConfigControllerBaseTest {
    @Test
    public void noBody() throws Exception {
        mockMvc.perform(post("/api/clickphite/config/create"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void validation_alreadyExists() throws Exception {
        dao.createConfig(config("id1"));

        mockMvc.perform(
            post("/api/clickphite/config/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeConfig(configViewModel("id1")))
        )
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void created() throws Exception {
        mockMvc.perform(
            post("/api/clickphite/config/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeConfig(configViewModel("id1")))
        )
            .andExpect(status().isOk());

        assertThat(dao.getOptionalConfig("id1")).isPresent();
    }
}

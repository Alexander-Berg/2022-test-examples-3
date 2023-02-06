package ru.yandex.market.health.ui.features.clickphite_config;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.health.ui.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClickphiteConfigControllerGetConfigsTest extends ClickphiteConfigControllerBaseTest {
    @Test
    public void empty() throws Exception {
        mockMvc.perform(get("/api/clickphite/config"))
            .andExpect(status().isOk())
            .andExpect(content().json("{content: [], pageProps: {totalPages: 0, currentPage: 1}}"));
    }

    @Test
    public void instantSerialization() throws Exception {
        dao.createConfig(config("id1"));

        Instant createdTime = dao.getConfig("id1").getCreatedTime();

        mockMvc.perform(get("/api/clickphite/config"))
            .andExpect(status().isOk())
            .andExpect(content().json("{content: [{\"id\": \"id1\", \"createdTime\": "
                + (double) createdTime.toEpochMilli() / 1000 + "}], pageProps: {totalPages: 1, currentPage: 1}}"));
    }
}

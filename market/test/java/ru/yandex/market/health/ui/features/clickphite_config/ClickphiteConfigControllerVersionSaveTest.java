package ru.yandex.market.health.ui.features.clickphite_config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.health.ui.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = ClickphiteConfigControllerVersionSaveTest.USER_NAME)
public class ClickphiteConfigControllerVersionSaveTest extends ClickphiteConfigControllerBaseTest {

    public static final String USER_NAME = "user42";
    private static final String PATH = "/api/clickphite/config/version/save";
    private static final String CONFIG_ID = "id1";

    @Test
    void tryToSaveAlreadyPublishedConfig() throws Exception {
        dao.createConfig(config(CONFIG_ID));
        // метод version создаёт версию в статусе VersionStatus.PUBLIC - это важно для теста
        dao.createDraftVersion(version(CONFIG_ID), USER_NAME);

        mockMvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeConfigVersion(configVersionViewModel(CONFIG_ID)))
        )
            .andDo(print())
            .andExpect(status().isConflict());
    }

}

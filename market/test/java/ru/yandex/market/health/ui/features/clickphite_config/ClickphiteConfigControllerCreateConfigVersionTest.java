package ru.yandex.market.health.ui.features.clickphite_config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.ui.TestConfig;
import ru.yandex.market.health.ui.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "user42")
public class ClickphiteConfigControllerCreateConfigVersionTest extends ClickphiteConfigControllerBaseTest {

    private static final String PATH = "/api/clickphite/config/version/create";
    private static final String CONFIG_ID = "id1";

    @Test
    public void noBody() throws Exception {
        mockMvc.perform(post(PATH))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void validation_configNotFound() throws Exception {
        mockMvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeConfigVersion(configVersionViewModel(CONFIG_ID)))
        )
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void created() throws Exception {
        createConfig();

        mockMvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeConfigVersion(configVersionViewModel(CONFIG_ID)))
        )
            .andExpect(status().isOk());
    }

    @Test
    public void createWithoutApplyInClickHouseQueriesField() throws Exception {
        testApplyInClickHouseQueriesField(false, "no_apply_in_click_house_queries_field.json");
    }

    @Test
    public void createWithFalseApplyInClickHouseQueriesField() throws Exception {
        testApplyInClickHouseQueriesField(false, "false_apply_in_click_house_queries_field.json");
    }

    @Test
    public void createWithTrueApplyInClickHouseQueriesField() throws Exception {
        testApplyInClickHouseQueriesField(true, "true_apply_in_click_house_queries_field.json");
    }

    private void testApplyInClickHouseQueriesField(boolean expectedValue, String resourceName) throws Exception {
        createConfig();

        mockMvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.loadFromClasspath(resourceName))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk());

        final VersionedConfigEntity.VersionEntity.Id versionId = new VersionedConfigEntity.VersionEntity.Id(CONFIG_ID,
            0L);
        Assertions.assertEquals(expectedValue, dao.getConfigVersion(versionId)
            .getConfigs()
            .get(0)
            .getGraphiteSolomon()
            .getSplits()
            .get(0)
            .getWhitelistSettings()
            .getApplyInClickHouseQueries());
    }

    private void createConfig() {
        dao.createConfig(config(CONFIG_ID));
    }
}

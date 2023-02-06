package ru.yandex.market.wms.datacreator.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TtsNotificationsControllerTest extends DataCreatorIntegrationTest {

    @Test
    @DatabaseSetup(value = "/controller/tts-notifications/before.xml", connection = "wmwhse1Connection")
    @ExpectedDatabase(value = "/controller/tts-notifications/after.xml", connection = "wmwhse1Connection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void deleteNotificationsByUserNameTest() throws Exception {
        mockMvc.perform(delete("/tts/notifications?userName=ad12"))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteNotificationsWithoutUserNameParameterTest() throws Exception {
        mockMvc.perform(delete("/tts/notifications"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$")
                        .value("Required request parameter 'userName' for method parameter type " +
                                "String is not present"));
    }

    @Test
    public void deleteNotificationsWithBlankUserNameParameterTest() throws Exception {
        mockMvc.perform(delete("/tts/notifications?userName="))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$")
                        .value("deleteNotificationsByUser.userName: must not be blank"));
    }
}

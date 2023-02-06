package ru.yandex.market.pers.notify.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.notify.model.SubscriptionSettings;
import ru.yandex.market.pers.notify.model.Uid;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.test.TestUtil.stringFromFile;

public class SettingsControllerInvoker {
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();


    void createSubscriptionsIllegal(Long uid, String email, String jsonFileName) throws Exception {
        createSubscriptions(uid, email, jsonFileName, status().isForbidden());
    }

    void createSubscriptions(Long uid, String email, String jsonFileName) throws Exception {
        createSubscriptions(uid, email, jsonFileName, status().isOk());
    }

    private void createSubscriptions(Long uid, String email, String jsonFileName,
                                     ResultMatcher resultMatcher) throws Exception {
        createSubscriptions(uid, email, resultMatcher, stringFromFile(jsonFileName));
    }

    private void createSubscriptions(Long uid, String email,
                                     ResultMatcher resultMatcher, String content) throws Exception {
        mockMvc.perform(post("/settings/email/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .param("uid", String.valueOf(uid))
            .param("email", email)
            .content(content))
            .andDo(print())
            .andExpect(resultMatcher);
    }

    void checkSubscriptions(Long uid, String email, String settingsFileName) throws Exception {
        checkSubscriptionsByContent(uid, email, stringFromFile(settingsFileName));
    }

    private void checkSubscriptionsByContent(Long uid, String email, String jsonContent) throws Exception {
        mockMvc.perform(get("/settings/email/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .param("email", email)
            .param("uid", String.valueOf(uid)))
            .andDo(print())
            .andExpect(content().json(jsonContent));
    }

    void createSubscriptionsBySettings(Uid uid, String email, SubscriptionSettings settings) throws Exception {
        createSubscriptions(uid.getValue(), email, status().isOk(), toJson(settings));
    }

    void checkSubscriptionsBySettings(Uid uid, String email, SubscriptionSettings settings) throws Exception {
        checkSubscriptionsByContent(uid.getValue(), email, toJson(settings));
    }

    void createActiveIllegal(String email, long uid) throws Exception {
        mockMvc.perform(post("/settings/UID/" + uid + "/emails/active")
            .contentType(MediaType.APPLICATION_JSON)
            .param("email", email))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    void createActive(Long uid, String email) throws Exception {
        mockMvc.perform(post("/settings/UID/" + uid + "/emails/active")
            .contentType(MediaType.APPLICATION_JSON)
            .param("email", email))
            .andDo(print())
            .andExpect(status().isOk());
    }

    String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
}

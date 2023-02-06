package ru.yandex.market.checkout.pushapi.helpers;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class PushApiSettingsHelper extends MockMvcAware {

    public PushApiSettingsHelper(MockMvc mockMvc, PushApiTestSerializationService testSerializationService) {
        super(mockMvc, testSerializationService);
    }

    public ResultActions getSettingsForActions(long shopId) throws Exception {
        var result = mockMvc.perform(get("/shops/{shopId}/settings", shopId))
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public Settings getSettings(long shopId) throws Exception {
        MvcResult result = getSettingsForActions(shopId).andReturn();

        return testSerializationService.deserialize(result.getResponse().getContentAsString(),
                Settings.class);
    }

    public void postSettings(long shopId, Settings settings) throws Exception {
        mockMvc.perform(post("/shops/{shopId}/settings", shopId)
                        .content(testSerializationService.serialize(settings))
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().isOk());
    }
}

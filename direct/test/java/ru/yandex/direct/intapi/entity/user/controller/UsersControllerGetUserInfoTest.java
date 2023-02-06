package ru.yandex.direct.intapi.entity.user.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.rbac.RbacRepType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.feature.FeatureName.MODERATION_OFFER_ENABLED_FOR_DNA;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UsersControllerGetUserInfoTest {

    @Autowired
    private UsersController controller;
    @Autowired
    private Steps steps;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getUserInfo_whenFeatureEnabledAndOnlyOneRepresentative() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);

        String jsonAnswer = doRequest(clientInfo.getUid());
        assertThat(jsonAnswer).isEqualTo("{\"is_offer_accepted\":true,\"success\":true}");
    }

    @Test
    public void getUserInfo_whenFeatureEnabledButSeveralRepresentatives() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);

        String jsonAnswer = doRequest(clientInfo.getUid());
        assertThat(jsonAnswer).isEqualTo("{\"is_offer_accepted\":false,\"success\":true}");
    }

    @Test
    public void getUserInfo_whenFeatureDisabledAndOnlyOneRepresentative() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, false);

        String jsonAnswer = doRequest(clientInfo.getUid());
        assertThat(jsonAnswer).isEqualTo("{\"is_offer_accepted\":false,\"success\":true}");
    }

    @Test
    public void getUserInfo_whenUserNotExist() throws Exception {
        String jsonAnswer = doRequest(Integer.MAX_VALUE - 1L);
        assertThat(jsonAnswer).isEqualTo("{\"code\":1,\"text\":\"User not found\",\"description\":null," +
                "\"success\":false}");
    }

    private String doRequest(Long userId) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/users/info")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("user_id", userId.toString());
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());
        return perform.andReturn().getResponse().getContentAsString();
    }

}

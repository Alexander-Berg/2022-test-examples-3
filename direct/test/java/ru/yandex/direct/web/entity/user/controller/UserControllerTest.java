package ru.yandex.direct.web.entity.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ClientPrimaryManagerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

@DirectWebTest
@RunWith(SpringRunner.class)
public class UserControllerTest {

    private static final String METHOD_PATH = "/user/set_offer_accepted";

    @Autowired
    private UserController userController;
    @Autowired
    private MockMvcCreator mockMvcCreator;
    @Autowired
    private TestAuthHelper testAuthHelper;
    @Autowired
    private DirectWebAuthenticationSource directWebAuthenticationSource;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(userController).build();
    }

    @Test
    public void setOfferAccepted_success() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientAndUser();
        UserInfo secondUserInfo = steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        Long subjectUserUid = secondUserInfo.getUid();

        JsonNode answer = sendRequest(subjectUserUid, subjectUserUid);

        //noinspection ConstantConditions
        Boolean actualOfferAccepted = userService.getUser(subjectUserUid).getIsOfferAccepted();
        boolean success = answer.get("success").asBoolean();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualOfferAccepted).as("isOfferAccepted").isTrue();
            soft.assertThat(success).as("success").isTrue();
        });
    }

    @Test
    public void setOfferAccepted_whenOperatorIsNotRepresentative_failure() throws Exception {
        ClientPrimaryManagerInfo primaryManagerInfo = steps.idmGroupSteps().createIdmPrimaryManager();
        ClientInfo subjectClientInfo = primaryManagerInfo.getSubjectClientInfo();
        UserInfo secondUserInfo = steps.userSteps().createUser(subjectClientInfo, RbacRepType.MAIN);
        Long subjectUserUid = secondUserInfo.getUid();
        Long operatorUid = primaryManagerInfo.getManagerUid();

        JsonNode answer = sendRequest(subjectUserUid, operatorUid);

        //noinspection ConstantConditions
        Boolean actualOfferAccepted = userService.getUser(subjectUserUid).getIsOfferAccepted();
        boolean success = answer.get("success").asBoolean();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualOfferAccepted).as("isOfferAccepted").isFalse();
            soft.assertThat(success).as("success").isFalse();
        });
    }

    private JsonNode sendRequest(Long subjectUserUid, Long operatorUid) throws Exception {
        testAuthHelper.setSubjectUser(subjectUserUid);
        testAuthHelper.setOperator(operatorUid);
        testAuthHelper.setSecurityContext();
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(METHOD_PATH)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);
        ResultActions perform = mockMvc.perform(requestBuilder);
        String answer = perform.andReturn().getResponse().getContentAsString();
        return JsonUtils.fromJson(answer);
    }

}

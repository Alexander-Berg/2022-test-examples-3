package ru.yandex.direct.web.entity.adgeneration.controller;


import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.ManualTestingWithTvm;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static ru.yandex.direct.core.entity.adgeneration.model.GenerationDefectIds.BANGEN_PROXY_API_ERROR;

@DirectWebTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ManualTestingWithTvm.class)
public class AdGenerationControllerManualTest {

    private static final String URL = "https://yandex.ru/";
    private static final String USER_TEXT = "Поисковая система";

    @Autowired
    private AdGenerationController adGenerationController;

    @Autowired
    private MockMvcCreator mockMvcCreator;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;
    private ClientInfo clientInfo;

    @Before
    public void Before() {
        mockMvc = mockMvcCreator.setup(adGenerationController).build();
        clientInfo = steps.clientSteps().createDefaultClient();
        testAuthHelper.setOperator(clientInfo.getUid());
        testAuthHelper.setSubjectUser(clientInfo.getUid());
        testAuthHelper.setSecurityContext();
    }

    @Ignore("Только для ручного запуска")
    @Test
    public void generateTextSuggestions_success() throws Exception {
        String requestURL =
                "/ad_generation/ad_generation/generate_text_suggestions?url=" + URL + "&ulogin=" + clientInfo.getLogin();
        JsonNode response =
                getResponse(requestURL);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(response.get("success").asBoolean())
                .as("generate text suggestions success")
                .isTrue();
        checkForErrors(softAssertions, response);
        softAssertions.assertAll();
    }

    @Ignore("Только для ручного запуска")
    @Test
    public void generateText_success() throws Exception {
        JsonNode response =
                getResponse("/ad_generation/ad_generation/generate_text?campaignId=0&url=" + URL + "&ulogin=" + clientInfo.getLogin());

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(response.get("success").asBoolean())
                .as("generate text success")
                .isTrue();
        checkForErrors(softAssertions, response);
        softAssertions.assertAll();
    }

    @Ignore("Только для ручного запуска")
    @Test
    public void generateTextSuggestionsBySubject_success() throws Exception {
        JsonNode response =
                getResponse("/ad_generation/ad_generation/generate_text_suggestions_by_subject?campaignId=0&url=" + URL + "&ulogin=" + clientInfo.getLogin() + "&adSubject=" + USER_TEXT);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(response.get("success").asBoolean())
                .as("generate text suggestions by subject success")
                .isTrue();
        checkForErrors(softAssertions, response);
        softAssertions.assertAll();
    }

    private JsonNode getResponse(String methodPath) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(methodPath)
                .contentType(APPLICATION_JSON_UTF8_VALUE)
                .accept(APPLICATION_JSON_UTF8_VALUE);
        var performedRequest = mockMvc.perform(requestBuilder);
        return JsonUtils.fromJson(performedRequest
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    private void checkForErrors(SoftAssertions softAssertions, JsonNode response) {
        List<String> warningsArray = JsonUtils.getObjectMapper()
                .convertValue(response.get("additionalInfo").get("warnings"), new TypeReference<List<String>>() {
                });
        softAssertions.assertThat(warningsArray)
                .as("no BANGEN_PROXY_API_ERROR")
                .noneMatch(warning -> warning.contains(BANGEN_PROXY_API_ERROR.name()));
    }
}

package ru.yandex.direct.web.entity.communication.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.ManualTestingWithTvm;
import ru.yandex.direct.web.configuration.TestingDirectWebAppConfiguration;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;

@DirectWebTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ManualTestingWithTvm.class, TestingDirectWebAppConfiguration.class})

public class CommunicationControllerManualTest {

    @Autowired
    private CommunicationController communicationController;
    @Autowired
    private MockMvcCreator mockMvcCreator;
    @Autowired
    private TestAuthHelper testAuthHelper;
    @Autowired
    private Steps steps;

    private MockMvc mockMvc;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(communicationController).build();
    }

    @Ignore("Для ручного запуска. Читает сообщение и YT-таблицы" +
            "Сообщение получается в результате обработки Цезарем события, отправляемого sendEvent_success")
    @Test
    public void getMessages_success() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        JsonNode answer = getRequest("/communication/messages?uid=" + clientInfo.getUid(), clientInfo.getUid());

        boolean success = answer.get("success").asBoolean();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(success).as("success").isTrue();
        });
    }

    @Ignore("Для ручного запуска. Делает реальную запись события в топик логброкера")
    @Test
    public void sendEvent_success() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        JsonNode answer = sendEvent("/communication/event", clientInfo.getUid(),
                "{\"event\": {"
                        + "\"EventId\":12345678, \"ParentEventIds\": [1111,22222],"
                        + "\"Uid\":[" + clientInfo.getUid() + "], \"EventType\":\"SEND_MESSAGE\","
                        + "\"SendMessageData\":{\"MessageType\":\"ALERT\"}}}");

        boolean success = answer.get("success").asBoolean();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(success).as("success").isTrue();
        });
    }

    private JsonNode getRequest(String methodPath, Long operatorUid) throws Exception {
        testAuthHelper.setOperator(operatorUid);
        testAuthHelper.setSecurityContext();
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(methodPath)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE);
        ResultActions perform = mockMvc.perform(requestBuilder);
        String answer = perform.andReturn().getResponse().getContentAsString();

        return JsonUtils.fromJson(answer);
    }

    private JsonNode sendEvent(String methodPath, Long operatorUid, String requestData) throws Exception {
        testAuthHelper.setOperator(operatorUid);
        testAuthHelper.setSecurityContext();
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(methodPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestData)
                .accept(MediaType.APPLICATION_JSON);
        ResultActions perform = mockMvc.perform(requestBuilder);
        String answer = perform.andReturn().getResponse().getContentAsString();

        return JsonUtils.fromJson(answer);
    }
}

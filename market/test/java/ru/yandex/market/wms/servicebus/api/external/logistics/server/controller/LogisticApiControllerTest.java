package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.scenario.builder.FunctionalTestScenarioBuilder;

public class LogisticApiControllerTest extends IntegrationTest {
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;

    @Test
    public void shouldFailOnIncorrectToken() throws Exception {
        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
                .sendRequestToWrap("/api/logistic/createOrder", HttpMethod.POST,
                        "api/logistics/server/auth/invalid-token/request.xml")
                .andExpectWrapAnswerToMeetRequirements((response, assertions) -> {
                    List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();

                    assertions.assertThat(errorCodes)
                            .as("Assert that error codes contain 1 error")
                            .hasSize(1);

                    ErrorPair errorCode = errorCodes.get(0);

                    assertions.assertThat(errorCode.getCode())
                            .as("Assert that it is unknown error")
                            .isEqualTo(ErrorCode.INVALID_AUTHORIZATION_TOKEN);
                })
                .build(mockMvc, xmlMapper, null)
                .start();
    }

    @Test
    public void shouldAcceptValidToken() throws Exception {
        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
                .sendRequestToWrap("/api/logistic/createOrder", HttpMethod.POST,
                        "api/logistics/server/auth/valid-token/request.xml")
                .andExpectWrapAnswerToMeetRequirements((response, assertions) -> {
                    List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();
                    if (!errorCodes.isEmpty()) {
                        ErrorPair errorCode = errorCodes.get(0);
                        assertions.assertThat(errorCode.getCode())
                                .as("Assert that it is not INVALID_AUTHORIZATION_TOKEN error")
                                .isNotEqualTo(ErrorCode.INVALID_AUTHORIZATION_TOKEN);
                    }
                })
                .build(mockMvc, xmlMapper, "")
                .start();
    }
}

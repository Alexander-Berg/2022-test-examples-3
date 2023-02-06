package ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete;

import org.assertj.core.api.SoftAssertions;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.fulfillment.wrap.core.scenario.FunctionalTestScenario;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetInboundsStatusRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundsStatusResponse;

import java.util.List;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

public class GetInboundStatusNegativeScenarioWrongWaybillType extends FunctionalTestScenario<RequestWrapper<GetInboundsStatusRequest>, ResponseWrapper<GetInboundsStatusResponse>> {

    private final static String REQUEST_PATH = "functional/get_inbound_status/positive/request.xml";
    private final static String RESPONSE_PATH = "functional/get_inbound_status/negative/wrong_waybill_response.json";

    private final String requestUrl;

    public GetInboundStatusNegativeScenarioWrongWaybillType(RestTemplate restTemplate, String requestUrl) {
        super(restTemplate);
        this.requestUrl = requestUrl;
    }

    @Override
    public void configureMocks() {
        mockServer.expect(once(), requestTo(requestUrl + "/waybill/745723"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(extractFileContent(RESPONSE_PATH)));
    }

    @Override
    public String getRequestContent() {
        return extractFileContent(REQUEST_PATH);
    }

    @Override
    public void doAssertions(SoftAssertions assertions,
                             RequestWrapper<GetInboundsStatusRequest> request,
                             ResponseWrapper<GetInboundsStatusResponse> response) {
        assertions.assertThat(request.getHash())
            .as("Asserting hash value")
            .isEqualTo(response.getHash());

        List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();
        assertions.assertThat(errorCodes)
            .as("Asserting that there are errors in error codes")
            .isNotNull();

        assertions.assertThat(errorCodes.get(0).getCode())
            .as("Asserting error message")
            .isEqualTo(ErrorCode.UNKNOWN_ERROR);

        assertions.assertThat(errorCodes.get(0).getMessage())
            .as("Asserting error message")
            .isEqualTo("Expected type id from Marschroute is 12, but actual is 2");
    }
}

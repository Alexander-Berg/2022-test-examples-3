package ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete;

import org.assertj.core.api.SoftAssertions;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.fulfillment.wrap.core.scenario.FunctionalTestScenario;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetInboundsStatusRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundsStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType.CREATED;

public class GetInboundStatusPositiveScenario extends FunctionalTestScenario<RequestWrapper<GetInboundsStatusRequest>, ResponseWrapper<GetInboundsStatusResponse>> {

    private final static String REQUEST_PATH = "functional/get_inbound_status/positive/request.xml";
    private final static String RESPONSE_PATH = "functional/get_inbound_status/positive/response.json";

    private final String requestUrl;

    public GetInboundStatusPositiveScenario(RestTemplate restTemplate, String requestUrl) {
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

        assertions.assertThat(response.getRequestState().getErrorCodes())
            .as("Asserting that there are no errors in error codes")
            .isNull();

        InboundStatus inboundStatus = response.getResponse().getInboundsStatus().get(0);

        assertions.assertThat(request.getRequest().getInboundsId().get(0))
            .as("Asserting inbound ids are equals")
            .isEqualTo(inboundStatus.getInboundId());

        assertions.assertThat(inboundStatus.getStatusCode())
            .as("Asserting inbound state in response")
            .isEqualTo(CREATED);

        assertions.assertThat(inboundStatus.getDate())
            .as("Asserting inbound state date")
            .isEqualTo(new DateTime("2017-10-04T20:09:45"));
    }
}

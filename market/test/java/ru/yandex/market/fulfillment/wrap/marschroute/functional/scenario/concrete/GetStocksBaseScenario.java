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
import ru.yandex.market.logistic.api.model.fulfillment.request.GetStocksRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

public abstract class GetStocksBaseScenario extends FunctionalTestScenario<RequestWrapper<GetStocksRequest>, ResponseWrapper<GetStocksResponse>> {

    private final static String REQUEST_PATH = "functional/get_stocks/request.xml";
    private final String requestUrl;
    private final String responseFilePath;


    public GetStocksBaseScenario(String responseFilePath, RestTemplate restTemplate,
                                 String requestUrl) {
        super(restTemplate);
        this.requestUrl = requestUrl;
        this.responseFilePath = responseFilePath;
    }

    @Override
    public void configureMocks() {
        mockServer.expect(once(), requestTo(requestUrl + "/products?limit=20&offset=0&sort%5Bprod_id%5D=asc"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(extractFileContent(responseFilePath)));
    }


    @Override
    public void doAssertions(SoftAssertions assertions, RequestWrapper<GetStocksRequest> request, ResponseWrapper<GetStocksResponse> response) {
        assertions.assertThat(response.getHash())
            .as("Asserting response has value")
            .isEqualTo(request.getHash());
    }

    @Override
    public String getRequestContent() {
        return extractFileContent(REQUEST_PATH);
    }

}

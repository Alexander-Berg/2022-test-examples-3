package ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete;

import org.assertj.core.api.SoftAssertions;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetStocksRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;

public class GetStocksNegativeScenario extends GetStocksBaseScenario {

    private final static String RESPONSE_PATH = "functional/get_stocks/wrong_item_id/response.json";


    public GetStocksNegativeScenario(RestTemplate restTemplate,
                                     String requestUrl) {
        super(RESPONSE_PATH, restTemplate, requestUrl);
    }


    @Override
    public void doAssertions(SoftAssertions assertions,
                             RequestWrapper<GetStocksRequest> request,
                             ResponseWrapper<GetStocksResponse> response) {
        super.doAssertions(assertions, request, response);
        assertions
                .assertThat(response.getRequestState().hasErrors())
                .as("Response must be failed due to errors")
                .isTrue();
        assertions
                .assertThat(response.getRequestState().getErrorCodes())
                .as("Error pairs must contains at least one error")
                .hasSize(1);

        ErrorPair errorPair = response.getRequestState().getErrorCodes().get(0);
        assertions
                .assertThat(errorPair.getCode())
                .as("Error code ")
                .isEqualTo(ErrorCode.UNKNOWN_ERROR);

        assertions
                .assertThat(errorPair.getMessage())
                .as("Error message ")
                .contains("does not match item identifier pattern");
    }
}

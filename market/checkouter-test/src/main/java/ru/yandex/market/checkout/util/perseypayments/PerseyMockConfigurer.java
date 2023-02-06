package ru.yandex.market.checkout.util.perseypayments;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;

@TestComponent
public class PerseyMockConfigurer {

    public static final String RIDE_SUBS_URL = "/internal/v1/charity/ride_subs";
    public static final String RIDE_DONATION_URL = "/internal/v1/charity/ride_donation";
    public static final String PAY = "PerseyPaymentsPay";
    public static final String ESTIMATE = "PerseyPaymentsEstimate";
    public static final String REFUND = "PerseyPaymentsRefund";
    public static final String PAY_ENDPOINT = "pay";
    public static final String ESTIMATE_ENDPOINT = "estimate";
    public static final String REFUND_ENDPOINT = "refund";
    private static final UrlPathPattern ESTIMATE_URL_PATTERN =
            urlPathEqualTo(RIDE_DONATION_URL + "/" + ESTIMATE_ENDPOINT);
    private static final UrlPathPattern REFUND_URL_PATTERN =
            urlPathEqualTo(RIDE_DONATION_URL + "/" + REFUND_ENDPOINT);

    @Autowired
    private WireMockServer perseyPaymentsMock;

    private static ResponseDefinitionBuilder ok() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_OK);
    }

    public void mockWholePerseyPayments() {
        mockEstimateSuccess();
        mockPay();
        mockRefund();
    }

    public void mockEstimateSuccess() {
        MappingBuilder builder = get(ESTIMATE_URL_PATTERN)
                .withName(ESTIMATE)
                .willReturn(ok().withBody("{\"is_subscribed\": true, \"amount_info\": {\"amount\": \"5.0000\", "
                        + "\"currency_code\": \"RUB\", \"currency_sign\": \"₽\"}}"));
        perseyPaymentsMock.stubFor(builder);
    }

    public void mockEstimateZeroDonation() {
        MappingBuilder builder = get(ESTIMATE_URL_PATTERN)
                .withName(ESTIMATE)
                .willReturn(ok().withBody("{\"is_subscribed\": true}"));
        perseyPaymentsMock.stubFor(builder);
    }

    public void mockEstimateNotSubscribedNonZeroDonation() {
        MappingBuilder builder = get(ESTIMATE_URL_PATTERN)
                .withName(ESTIMATE)
                .willReturn(ok().withBody("{\"is_subscribed\": false, \"nonzero_donation_if_subscribes\" : true}"));
        perseyPaymentsMock.stubFor(builder);
    }

    public void mockEstimateFailedRequest() {
        MappingBuilder builder = get(ESTIMATE_URL_PATTERN)
                .withName(ESTIMATE)
                .willReturn(aResponse().withStatus(SC_INTERNAL_SERVER_ERROR));
        perseyPaymentsMock.stubFor(builder);
    }

    public void mockPay() {
        MappingBuilder builder = post(RIDE_SUBS_URL + "/" + PAY_ENDPOINT)
                .withName(PAY)
                .willReturn(ok().withBody("{}"));
        perseyPaymentsMock.stubFor(builder);
    }

    public void mockRefund() {
        MappingBuilder builder = put(REFUND_URL_PATTERN)
                .withName(REFUND)
                .willReturn(ok().withBody("{}"));
        perseyPaymentsMock.stubFor(builder);
    }
}

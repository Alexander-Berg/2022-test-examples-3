package ru.yandex.travel.train.partners.im;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.testing.misc.TestResources;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceCheckoutRequest;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceCheckoutResponse;
import ru.yandex.travel.train.partners.im.model.insurance.InsurancePricingRequest;
import ru.yandex.travel.train.partners.im.model.insurance.MainServiceReferenceInternal;
import ru.yandex.travel.train.partners.im.model.insurance.RailwayInsuranceTravelCheckoutRequest;
import ru.yandex.travel.train.partners.im.model.insurance.RailwayTravelPricingRequest;
import ru.yandex.travel.train.partners.im.model.insurance.RailwayTravelProductPricingInfo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class TestInsuranceRequests {
    private static final Logger logger = LoggerFactory.getLogger(TestInsuranceRequests.class);
    private ImClient client;

    @Rule
    public WireMockRule wireMockRule
            = new WireMockRule(WireMockConfiguration.wireMockConfig().bindAddress("localhost").dynamicPort());

    @Before
    public void setUp() {
        var testClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("expediaAhcPool")
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
                .build());
        var clientWrapper = new AsyncHttpClientWrapper(
                testClient, logger, "testDestination", new MockTracer(),
                Arrays.stream(DefaultImClient.Method.values()).map(Enum::name).collect(Collectors.toSet())
        );
        client = new DefaultImClient(clientWrapper, "pos", Duration.ofSeconds(10),
                String.format("http://localhost:%d/", wireMockRule.port()),"ya", "***");
    }

    @Test
    public void testInsurancePricingRequest(){
        stubFor(post(urlPathMatching(".*")).willReturn(aResponse()
                .withBody(TestResources.readResource("InsurancePricingResponse.json"))));

        var request = new InsurancePricingRequest(new RailwayTravelPricingRequest(
                new MainServiceReferenceInternal(333)));
        var response = client.insurancePricing(request);

        verify(postRequestedFor(urlPathMatching(".*/Insurance/V1/Travel/Pricing"))
                .withRequestBody(equalToJson(TestResources.readResource("InsurancePricingRequest.json"),
                        false, false)));

        var expectedPricingInfo = new RailwayTravelProductPricingInfo();

        expectedPricingInfo.setAmount(BigDecimal.valueOf(4500.0));
        expectedPricingInfo.setOrderCustomerId(5632);
        expectedPricingInfo.setProductPackage("AccidentWithFloatPremium");
        expectedPricingInfo.setProvider("P3");
        expectedPricingInfo.setCompany("Renessans");
        expectedPricingInfo.setCompensation(BigDecimal.valueOf(170.0));

        assertThat(response.getPricingResult().getProductPricingInfoList().get(0)).isEqualTo(expectedPricingInfo);
    }

    @Test
    public void testInsuranceCheckoutRequest(){
        stubFor(post(urlPathMatching(".*")).willReturn(aResponse()
                .withBody(TestResources.readResource("InsuranceCheckoutResponse.json"))));

        var request = new InsuranceCheckoutRequest(
                new MainServiceReferenceInternal(789),
                new RailwayInsuranceTravelCheckoutRequest("AccidentWithFloatPremium"),
                "P3", "Renessans");
        request.getMainServiceReference().setOrderCustomerId(563);
        var response = client.insuranceCheckout(request);

        verify(postRequestedFor(urlPathMatching(".*/Insurance/V1/Travel/Checkout"))
                .withRequestBody(equalToJson(TestResources.readResource("InsuranceCheckoutRequest.json"),
                        false, false)));

        var expectedResponse = new InsuranceCheckoutResponse();
        expectedResponse.setOrderId(333);
        expectedResponse.setOrderItemId(790);
        expectedResponse.setOrderCustomerId(563);
        expectedResponse.setAmount(BigDecimal.valueOf(150.0));

        assertThat(response).isEqualTo(expectedResponse);
    }
}

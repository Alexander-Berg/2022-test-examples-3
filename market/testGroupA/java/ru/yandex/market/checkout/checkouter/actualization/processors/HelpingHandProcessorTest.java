package ru.yandex.market.checkout.checkouter.actualization.processors;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.HelpingHandStatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.persey.model.EstimateOrderDonationResponse;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.perseypayments.PerseyMockConfigurer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class HelpingHandProcessorTest extends AbstractWebTestBase {

    @Autowired
    private PerseyMockConfigurer perseyMockConfigurer;
    @Autowired
    private WireMockServer perseyPaymentsMock;
    @Autowired
    private ObjectMapper perseyObjectMapper;

    @BeforeEach
    public void setup() {
        perseyPaymentsMock.resetRequests();
    }

    @Test
    public void testHelpingHandEstimateShouldBeDisabledByDefault() {
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(0));
    }

    @Test
    public void testHelpingHandEstimateEnabled_successfulNonZeroDonation() throws Exception {
        perseyMockConfigurer.mockEstimateSuccess();
        checkouterProperties.setEnableHelpingHandEstimate(true);
        var parameters = defaultBlueOrderParameters();
        parameters.getBuyer().setUid(1L);
        parameters.setPlatform(Platform.IOS);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getHelpingHandStatus(), is(HelpingHandStatus.ENABLED));
        assertThat(multiOrder.getHelpingHandDonationAmount(), is(5));
        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(2));
        var serveEvent = events.get(0);
        var actualRequest = serveEvent.getRequest();
        assertThat(actualRequest.getHeader("X-Yandex-UID"), is("1"));
        assertThat(actualRequest.getHeader("X-Application"), is(Platform.IOS.toString()));
        assertThat(actualRequest.queryParameter("ride_cost").firstValue(), is("350"));
        assertThat(actualRequest.queryParameter("currency_code").firstValue(), is("RUB"));
        assertThat(actualRequest.queryParameter("payment_tech_type").firstValue(), is("card"));
        EstimateOrderDonationResponse actualBody = perseyObjectMapper.readValue(
                serveEvent.getResponse().getBodyAsString(),
                EstimateOrderDonationResponse.class);
        assertThat(actualBody.getSubscribed(), is(true));
        assertThat(actualBody.getAmountInfo().getAmount(), is(new BigDecimal("5.00")));
        assertThat(actualBody.getAmountInfo().getCurrencyCode(), is("RUB"));
    }

    @Test
    public void testHelpingHandEstimate_successfulZeroDonation() throws Exception {
        perseyMockConfigurer.mockEstimateZeroDonation();
        checkouterProperties.setEnableHelpingHandEstimate(true);
        var parameters = defaultBlueOrderParameters();
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getHelpingHandStatus(), is(HelpingHandStatus.ENABLED));
        assertThat(multiOrder.getHelpingHandDonationAmount(), is(0));
        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(2));
        var serveEvent = events.get(0);
        EstimateOrderDonationResponse actualBody = perseyObjectMapper.readValue(
                serveEvent.getResponse().getBodyAsString(),
                EstimateOrderDonationResponse.class);
        assertThat(actualBody.getSubscribed(), is(true));
        assertThat(actualBody.getAmountInfo(), is(nullValue()));
    }

    @Test
    public void testHelpingHandEstimate_notSubscribedNonZeroDonation() throws Exception {
        perseyMockConfigurer.mockEstimateNotSubscribedNonZeroDonation();
        checkouterProperties.setEnableHelpingHandEstimate(true);
        var parameters = defaultBlueOrderParameters();
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getHelpingHandStatus(), is(HelpingHandStatus.NON_ZERO_DONATION_IF_SUBSCRIBES));
        assertThat(multiOrder.getHelpingHandDonationAmount(), is(0));
        List<ServeEvent> events = perseyPaymentsMock.getServeEvents().getRequests();
        assertThat(events, hasSize(2));
        var serveEvent = events.get(0);
        EstimateOrderDonationResponse actualBody = perseyObjectMapper.readValue(
                serveEvent.getResponse().getBodyAsString(),
                EstimateOrderDonationResponse.class);
        assertThat(actualBody.getSubscribed(), is(false));
        assertThat(actualBody.getNonzeroDonationIfSubscribes(), is(true));
    }

    @Test
    public void testHelpingHandEstimate_perseyRespondsWithError() throws Exception {
        perseyMockConfigurer.mockEstimateFailedRequest();
        checkouterProperties.setEnableHelpingHandEstimate(true);
        var parameters = defaultBlueOrderParameters();
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getHelpingHandStatus(), is(HelpingHandStatus.ROUNDING_ERROR));
        assertThat(multiOrder.getHelpingHandDonationAmount(), is(0));
    }
}

package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.MarketReportSearchService.REPORT_EXPERIMENTS_PARAM;
import static ru.yandex.market.checkout.checkouter.report.Experiments.ENABLE_BNPL_EXP;
import static ru.yandex.market.checkout.checkouter.report.Experiments.ENABLE_INSTALLMENTS_EXP;
import static ru.yandex.market.checkout.checkouter.report.Experiments.ENABLE_INSTALLMENT_FILTERS_EXP;
import static ru.yandex.market.checkout.checkouter.report.Experiments.SHOW_CREDITS_EXP;

public class DisableCreditForB2bTest extends AbstractWebTestBase {

    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    @BeforeEach
    void init() {
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        reportMock.resetRequests();
        checkouterProperties.setEnableShowCredits(true);
        checkouterProperties.setEnableInstallments(true);
    }

    @Test
    public void shouldDisableAllCreditsExperimentsWhenBusinessCart() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B, true);
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        turnOnCredits(parameters);

        orderCreateHelper.cart(parameters);

        assertCreditParamsForPlace("0", MarketReportPlace.OFFER_INFO);
        assertCreditParamsForPlace("0", MarketReportPlace.ACTUAL_DELIVERY);
        assertNoRequestForCreditOptions();
    }

    @Test
    public void shouldNotDisableAllCreditsExperimentsWhenPersonCart() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B, true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        turnOnCredits(parameters);

        orderCreateHelper.cart(parameters);

        assertCreditParamsForPlace("1", MarketReportPlace.OFFER_INFO);
        assertCreditParamsForPlace("1", null, MarketReportPlace.ACTUAL_DELIVERY);
    }

    @Test
    public void shouldNotDisableAllCreditsExperimentsWhenBusinessCartAndNoFeature() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B, false);
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        turnOnCredits(parameters);

        orderCreateHelper.cart(parameters);

        assertCreditParamsForPlace("1", MarketReportPlace.OFFER_INFO);
        assertCreditParamsForPlace("1", null, MarketReportPlace.ACTUAL_DELIVERY);
    }

    @Test
    public void shouldDisableAllCreditsExperimentsWhenBusinessCheckout() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B, true);
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        turnOnCredits(parameters);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(multiCart, parameters);

        assertCreditParamsForPlace("0", MarketReportPlace.OFFER_INFO);
        assertCreditParamsForPlace("0", MarketReportPlace.ACTUAL_DELIVERY);
        assertNoRequestForCreditOptions();
    }

    @Test
    public void shouldNotDisableAllCreditsExperimentsWhenPersonCheckout() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B, true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        turnOnCredits(parameters);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(multiCart, parameters);

        assertCreditParamsForPlace("1", MarketReportPlace.OFFER_INFO);
        assertCreditParamsForPlace("1", null, MarketReportPlace.ACTUAL_DELIVERY);
    }

    @Test
    public void shouldNotDisableAllCreditsExperimentsWhenBusinessCheckoutAndNoFeature() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B, false);
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        turnOnCredits(parameters);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(multiCart, parameters);

        assertCreditParamsForPlace("1", MarketReportPlace.OFFER_INFO);
        assertCreditParamsForPlace("1", null, MarketReportPlace.ACTUAL_DELIVERY);
    }

    private void turnOnCredits(Parameters parameters) {
        parameters.setExperiments(Experiments.empty()
                .with(Map.of(
                        ENABLE_INSTALLMENTS_EXP, "1",
                        ENABLE_INSTALLMENT_FILTERS_EXP, "1",
                        ENABLE_BNPL_EXP, "1",
                        SHOW_CREDITS_EXP, "1"
                )));
        parameters.setShowInstallments(true);
        parameters.setShowCredits(true);
    }

    private void assertCreditParamsForPlace(String expectedValue,
                                            MarketReportPlace place) {
        assertCreditParamsForPlace(expectedValue, expectedValue, place);
    }

    private void assertCreditParamsForPlace(String expectedRearrValue,
                                            String expectedCgiValue,
                                            MarketReportPlace place) {
        List<ServeEvent> offerInfoEvents = reportMock.getServeEvents().getServeEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .queryParameter("place")
                        .containsValue(place.getId()))
                .collect(Collectors.toList());

        LoggedRequest offerInfoRequest = offerInfoEvents.get(0).getRequest();
        assertTrue(offerInfoRequest.queryParameter(REPORT_EXPERIMENTS_PARAM).values().size() > 0);

        String offerInfoRearrs = offerInfoRequest.queryParameter(REPORT_EXPERIMENTS_PARAM).firstValue();
        assertTrue(offerInfoRearrs.contains(ENABLE_INSTALLMENTS_EXP + "=" + expectedRearrValue));
        assertTrue(offerInfoRearrs.contains(ENABLE_INSTALLMENT_FILTERS_EXP + "=" + expectedRearrValue));
        assertTrue(offerInfoRearrs.contains(ENABLE_BNPL_EXP + "=" + expectedRearrValue));
        assertTrue(offerInfoRearrs.contains(SHOW_CREDITS_EXP + "=" + expectedRearrValue));

        if (expectedCgiValue == null) {
            assertFalse(offerInfoRequest.queryParameter("show-installments").isPresent());
            assertFalse(offerInfoRequest.queryParameter("show-credits").isPresent());
        } else {
            assertEquals(expectedCgiValue, offerInfoRequest.queryParameter("show-installments").firstValue());
            assertEquals(expectedCgiValue, offerInfoRequest.queryParameter("show-credits").firstValue());
        }
    }

    private void assertNoRequestForCreditOptions() {
        List<ServeEvent> creditInfoEvents = reportMock.getServeEvents().getServeEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .queryParameter("place")
                        .containsValue(MarketReportPlace.CREDIT_INFO.getId()))
                .collect(Collectors.toList());
        assertEquals(0, creditInfoEvents.size());
    }
}

package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.BnplFactory;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_CREDIT_BROKER;

public class CreditActualizeV2Test extends AbstractWebTestBase {

    @Test
    void shouldFillCreditInfo() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        params.setShowCredits(true);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getCreditInformation(), notNullValue());
        assertThat(cart.getCreditInformation().getCreditMonthlyPayment(), comparesEqualTo(BigDecimal.valueOf(605)));
        assertThat(cart.getCreditInformation().getPriceForCreditAllowed(), comparesEqualTo(BigDecimal.valueOf(3500)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldFillCreditInfoOptionsIfShowCreditBrokerIsTrue(boolean showCreditBroker) {
        checkouterFeatureWriter.writeValue(ENABLE_CREDIT_BROKER, true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setShowCreditBroker(showCreditBroker);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(parameters);

        if (showCreditBroker) {
            assertThat(cart.getCreditInformation(), notNullValue());
            validateCreditInformationOptions(cart.getCreditInformation());
        } else {
            assertThat(cart.getCreditInformation(), notNullValue());
            assertThat(cart.getCreditInformation().getOptions(), nullValue());
        }
    }

    private FoundOfferBuilder makeCreditInfo(FoundOfferBuilder offer) {
        return offer.bnpl(true, BnplFactory.installments(1, BnplFactory.payment(100)));
    }

    private void validateCreditInformationOptions(CreditInformation creditInformation) {
        assertThat(creditInformation.getOptions(), hasSize(4));
        assertThat(creditInformation.getOptions(), containsInAnyOrder(
                allOf(
                        hasProperty("term", is("3")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("3614"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("6")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("1917"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("12")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("1075"))
                        ))
                ),
                allOf(
                        hasProperty("term", is("24")),
                        hasProperty("monthlyPayment", allOf(
                                hasProperty("currency", is(Currency.RUR)),
                                hasProperty("value", is("667"))
                        ))
                )
        ));
    }
}

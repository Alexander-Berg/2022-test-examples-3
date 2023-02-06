package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.BnplFactory;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class InstallmentsActualizeV2Test extends AbstractWebTestBase {

    @BeforeEach
    void configure() {
        checkouterProperties.setEnableInstallments(true);
    }

    @Test
    void shouldFillInstallments() {
        var params = BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .itemBuilder(OrderItemProvider.orderItemWithSortingCenter().price(3500))
                .build());
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        params.setShowInstallments(true);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getInstallmentsInfo(), notNullValue());
        assertThat(cart.getInstallmentsInfo().getOptions(), hasSize(1));
        assertThat(cart.getInstallmentsInfo().getOptions(), hasItem(allOf(
                hasProperty("term", is("1")),
                hasProperty("monthlyPayment", allOf(
                  hasProperty("currency", is(Currency.RUR)),
                  hasProperty("value", is("3500"))
                ))
        )));
    }

    private FoundOfferBuilder makeCreditInfo(FoundOfferBuilder offer) {
        return offer.bnpl(true, BnplFactory.installments(1, BnplFactory.payment(100)));
    }
}

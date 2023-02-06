package ru.yandex.market.checkout.checkouter.report;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

public class CartFoForPriceDropReportParametersTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer reportMock;

    @BeforeEach
    public void configure() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_REPORT_DISCOUNT_VALUE, true);
    }

    @Test
    public void shouldPassCartFoToReportOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.cart(parameters);

        assertReportCalledWithCartFo(parameters.getBuiltMultiCart());
    }

    @Test
    public void shouldPassCartFoToReportOnClientCart() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.initializeMock(parameters);

        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        client.cart(parameters.getBuiltMultiCart(), cartParameters);

        assertReportCalledWithCartFo(parameters.getBuiltMultiCart());
    }

    @Test
    public void shouldPassCartFoToReportOnCheckout() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(order, parameters);

        assertReportCalledWithCartFo(order);
    }

    @Test
    public void shouldPassCartFoToReportOnClientCheckout() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        orderCreateHelper.initializeMock(parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0), true);
        reportMock.resetRequests();

        CheckoutParameters checkoutParameters = CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        client.checkout(order, checkoutParameters);

        assertReportCalledWithCartFo(order);
    }

    private void assertReportCalledWithCartFo(MultiCart multiCart) {
        List<LoggedRequest> requestList = getRequestsWithCartFo(multiCart);
        assertThat(requestList, hasSize(1));
        assertThat(
                requestList.stream()
                        .map(r -> r.getQueryParams().get("place").firstValue())
                        .distinct()
                        .collect(Collectors.toList()),
                contains("offerinfo")
        );
    }

    private List<LoggedRequest> getRequestsWithCartFo(MultiCart cart) {
        String queryParam = cart.getCarts()
                .stream()
                .flatMap(c -> c.getItems().stream())
                .map(OfferItem::getFeedOfferId)
                .map(offer -> format("%d-%s", offer.getFeedId(), offer.getId()))
                .collect(joining(","));
        return reportMock.findAll(
                getRequestedFor(anyUrl())
                        .withQueryParam(
                                "cart-fo",
                                equalTo(queryParam)
                        )
        );
    }
}

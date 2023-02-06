package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class CreateOrderMissingTest extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CreateOrderMissingTest.class);

    @Autowired
    private WireMockServer reportMock;

    @Test
    public void shouldNotUseMissingItemInActualDeliveryRequest() {
        OrderItem firstItem = OrderItemProvider.getOrderItem();
        OrderItem secondItem = OrderItemProvider.getAnotherOrderItem();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem, secondItem);
        parameters.getReportParameters().overrideItemInfo(firstItem.getFeedOfferId()).setHideOffer(true);
        parameters.setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getCarts().get(0).getItem(firstItem.getFeedOfferId()).getChanges(),
                CoreMatchers.hasItem(ItemChange.MISSING));

        List<LoggedRequest> actualDeliveryRequests = reportMock.getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.getQueryParams().get("place").values().get(0).equals("actual_delivery"))
                .collect(Collectors.toList());

        assertThat(actualDeliveryRequests, hasSize(1));

        for (LoggedRequest request : actualDeliveryRequests) {
            QueryParameter offersList = request.getQueryParams().get("offers-list");
            LOG.info("offersList: {}", offersList);

            assertThat(offersList.values(), hasSize(1));
            String[] offers = offersList.values().get(0).split(",");
            assertThat(offers.length, CoreMatchers.equalTo(1));
        }
    }

    @Test
    public void shouldNotUseMissingItemInActualDeliveryRequestOnCheckout() throws Exception {
        OrderItem firstItem = OrderItemProvider.getOrderItem();
        OrderItem secondItem = OrderItemProvider.getAnotherOrderItem();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem, secondItem);
        parameters.getReportParameters().overrideItemInfo(firstItem.getFeedOfferId()).setHideOffer(true);
        parameters.setCheckCartErrors(false);

        Delivery delivery = Iterables.getOnlyElement(
                orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters).getCarts()
        ).getDelivery();
        reportMock.resetRequests();
        parameters.getOrder().setDelivery(delivery);
        MultiCart order = orderCreateHelper.cart(parameters);

        assertThat(order.getCarts().get(0).getItem(firstItem.getFeedOfferId()).getChanges(),
                CoreMatchers.hasItem(ItemChange.MISSING));

        List<LoggedRequest> actualDeliveryRequests = reportMock.getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.getQueryParams().get("place").values().get(0).equals("actual_delivery"))
                .collect(Collectors.toList());

        assertThat(actualDeliveryRequests, hasSize(2));

        for (LoggedRequest request : actualDeliveryRequests) {
            QueryParameter offersList = request.getQueryParams().get("offers-list");
            LOG.info("offersList: {}", offersList);

            assertThat(offersList.values(), hasSize(1));
            String[] offers = offersList.values().get(0).split(",");
            assertThat(offers.length, CoreMatchers.equalTo(1));
        }
    }
}

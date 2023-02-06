package ru.yandex.market.checkout.checkouter.checkout;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

public class CreateResellerOrderTest extends AbstractWebTestBase {

    private static final String SELLER_INN_1 = "7710140679";
    private static final String SELLER_INN_2 = "7710140680";
    private static final String OFFER_NAME_1 = "OfferName1";
    private static final String OFFER_NAME_2 = "OfferName2";

    @Autowired
    private ReceiptService receiptService;

    private Order order;

    @BeforeEach
    void init() {
        OrderItem item1 = OrderItemProvider.defaultOrderItem();
        OrderItem item2 = OrderItemProvider.defaultOrderItem();
        item1.setSellerInn(SELLER_INN_1);
        item2.setSellerInn(SELLER_INN_2);
        item1.setOfferName(OFFER_NAME_1);
        item2.setOfferName(OFFER_NAME_2);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().setItems(List.of(item1, item2));
        checkouterProperties.setResellers(Set.of(item1.getSupplierId()));
        order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());
    }

    @Test
    void shouldSaveSellerInn() {
        assertThat(order.getItems(), Matchers.hasItems(
                hasProperty("sellerInn", is(SELLER_INN_1)),
                hasProperty("sellerInn", is(SELLER_INN_2))
        ));
    }

    @Test
    void fillCorrectInnInReceiptLines() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Order dbOrder = orderService.getOrder(order.getId());
        List<Receipt> incomeReceipt = receiptService.findByOrder(dbOrder.getId(), ReceiptType.INCOME);
        assertThat(incomeReceipt.size(), is(1));

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        Streams.stream(body.get("orders").getAsJsonArray().iterator())
                .map(JsonObject.class::cast)
                .forEach(orderLine -> {
                    if (orderLine.get("fiscal_title").toString().contains(OFFER_NAME_1)) {
                        assertThat(orderLine.get("fiscal_inn").getAsString(), is(SELLER_INN_1));
                    }
                    if (orderLine.get("fiscal_title").toString().contains(OFFER_NAME_2)) {
                        assertThat(orderLine.get("fiscal_inn").getAsString(), is(SELLER_INN_2));
                    }
                    if (orderLine.get("order_id").toString().contains("-delivery")) {
                        assertThat(orderLine.get("fiscal_inn").getAsString(),
                                is(PaymentTestHelper.DEFAULT_SUPPLIER_INN));
                    }
                });
    }

}

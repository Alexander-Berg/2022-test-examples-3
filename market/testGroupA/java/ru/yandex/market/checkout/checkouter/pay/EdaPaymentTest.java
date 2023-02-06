package ru.yandex.market.checkout.checkouter.pay;


import java.math.BigDecimal;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.ServiceOrderIdProvider;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.eda.EdaOrderChangePriceRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.UPDATE_BASKET_LINE_STUB;

public class EdaPaymentTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private ServiceOrderIdProvider serviceOrderIdProvider;
    @Autowired
    private OrderPayHelper orderPayHelper;

    private Order edaOrder;

    @BeforeEach
    public void init() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setIsEda(true);
        edaOrder = orderCreateHelper.createOrder(parameters);
    }

    @Test
    void fillIdenticalBalanceOrderId() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);
        String balanceOrderId = serviceOrderIdProvider.identicalOrderItemServiceOrderId(edaPaidOrder.getId());
        assertTrue(edaPaidOrder.getItems().stream()
                .allMatch(item -> item.getBalanceOrderId().equals(balanceOrderId)));
    }

    @Test
    void resizeBasketByAmountOnPriceReduced() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);

        BigDecimal reducedPrice = edaPaidOrder.getBuyerItemsTotal().min(BigDecimal.ONE);
        client.eda().changeEdaOrderPrice(
                new RequestClientInfo(ClientRole.SHOP_USER, 0L, WhiteParametersProvider.WHITE_SHOP_ID),
                edaPaidOrder.getId(),
                new EdaOrderChangePriceRequest(reducedPrice));

        List<LoggedRequest> updateBasketRequests = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(UPDATE_BASKET_LINE_STUB))
                .map(ServeEvent::getRequest)
                .collect(toList());
        assertThat(updateBasketRequests, hasSize(1));
        LoggedRequest request = updateBasketRequests.iterator().next();
        assertThat(request.getBodyAsString(), is("{\"amount\":\"" + reducedPrice + "\",\"qty\":1}"));
        String purchaseToken = edaPaidOrder.getPayment().getBasketKey().getPurchaseToken();
        String balanceOrderId = serviceOrderIdProvider.identicalOrderItemServiceOrderId(edaPaidOrder.getId());
        assertThat(request.getUrl(), matchesPattern(".*/payments/" + purchaseToken + "/orders/" + balanceOrderId +
                "/resize"));
    }

    @Test
    void netSpasibo() {
        assertThat(edaOrder.getValidFeatures(), not(hasItem(MarketplaceFeature.SPASIBO_PAY)));

        orderPayHelper.pay(edaOrder.getId());

        List<LoggedRequest> requests = getTrustMockConfigurer().servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .map(ServeEvent::getRequest)
                .collect(toList());
        assertThat(requests, hasSize(1));
    }

}

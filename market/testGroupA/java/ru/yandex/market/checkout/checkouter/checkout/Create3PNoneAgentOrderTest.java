package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.FISCAL_AGENT_TYPE_ENABLED;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.WHITE_SHOP_ID;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

public class Create3PNoneAgentOrderTest extends AbstractPaymentTestBase {
    @Autowired
    private ReceiptService receiptService;

    @Test
    void fillCorrectAgentTypeForNoneAgent3PShop() {
        checkouterFeatureWriter.writeValue(CollectionFeatureType.NONE_AGENT_3P_SHOPS, Set.of(WHITE_SHOP_ID));
        checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());

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
        assertThat(body.get("fiscal_agent_type").getAsString(), is("none_agent"));
    }
}

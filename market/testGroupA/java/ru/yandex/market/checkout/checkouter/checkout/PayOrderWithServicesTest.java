package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.pay.builders.AbstractPaymentBuilder.DELIVERY_TITLE;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

/**
 * @author zagidullinri
 * @date 22.10.2021
 */
public class PayOrderWithServicesTest extends AbstractWebTestBase {

    @Autowired
    private ReceiptService receiptService;

    @Test
    void fillCorrectLinesInReceipt() {
        checkouterProperties.setEnableServicesPrepay(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM, Set.of(OptionalOrderPart.ITEM_SERVICES));
        OrderItem orderItem = order.getItems().iterator().next();
        ItemService itemService = orderItem.getServices().iterator().next();
        List<Receipt> incomeReceipt = receiptService.findByOrder(order.getId(), ReceiptType.INCOME);
        assertThat(incomeReceipt.size(), is(1));
        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        JsonArray basketLines = body.get("orders").getAsJsonArray();
        assertThat(basketLines.size(), equalTo(3));
        StreamSupport.stream(basketLines.spliterator(), false)
                .map(JsonObject.class::cast)
                .forEach(orderFromEvent -> {
                    if (orderFromEvent.get("fiscal_title").getAsString().equals(orderItem.getOfferName())) {
                        assertThat(orderFromEvent.get("fiscal_inn").getAsString(),
                                is(PaymentTestHelper.DEFAULT_SUPPLIER_INN));
                    } else if (orderFromEvent.get("fiscal_title").getAsString().equals(itemService.getTitle())) {
                        assertThat(orderFromEvent.get("fiscal_inn").getAsString(),
                                is(checkouterProperties.getDefaultServiceProviderInn()));
                    } else if (orderFromEvent.get("fiscal_title").getAsString().equals(DELIVERY_TITLE)) {
                        assertThat(orderFromEvent.get("fiscal_inn").getAsString(),
                                is(PaymentTestHelper.DEFAULT_MARKET_INN));
                    } else {
                        throw new IllegalStateException("Found unexpected basket line: \n" + orderFromEvent.toString());
                    }
                });
    }
}

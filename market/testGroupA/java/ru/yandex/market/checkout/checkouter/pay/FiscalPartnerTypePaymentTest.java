package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.FISCAL_AGENT_TYPE_ENABLED;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

public class FiscalPartnerTypePaymentTest extends AbstractPaymentTestBase {

    @Test
    public void testFiscalPartnerTypeIsPassedInCreateBasket() throws Exception {
        orderServiceTestHelper.createUnpaidBlueOrder(order -> {
            order.getItems().forEach(item -> item.setSupplierType(SupplierType.THIRD_PARTY));
        });

        checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of(order().getBuyer().getUid())));

        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @Test
    public void testDifferentFiscalPartnerTypesArePassedInCreateBasket() throws Exception {
        orderServiceTestHelper.createUnpaidBlueOrder(order -> {
            Iterator<OrderItem> itemIterator = order.getItems().iterator();
            OrderItem firstItem = itemIterator.next();
            OrderItem secondItem = itemIterator.next();
            firstItem.setSupplierType(SupplierType.FIRST_PARTY);
            secondItem.setSupplierType(SupplierType.THIRD_PARTY);

            order.getDelivery().setPrice(BigDecimal.ZERO);
            order.getDelivery().setBuyerPrice(BigDecimal.ZERO);
        });

        checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of(order().getBuyer().getUid())));

        paymentTestHelper.initAndHoldPayment();

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        assertThat(body.get("fiscal_agent_type").getAsString(), is("none_agent"));
        List<String> jsonFiscalAgentTypes = Streams.stream(body.get("orders").getAsJsonArray().iterator())
                .map(order -> order.getAsJsonObject().get("fiscal_agent_type").getAsString())
                .collect(Collectors.toList());
        assertThat(jsonFiscalAgentTypes, containsInAnyOrder("agent", "none_agent"));
    }
}

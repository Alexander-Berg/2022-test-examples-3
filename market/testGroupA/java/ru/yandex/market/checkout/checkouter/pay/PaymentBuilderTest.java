package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.cashier.model.PassParams;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketRequest;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.builders.PaymentBuilder;
import ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder;
import ru.yandex.market.checkout.checkouter.pay.builders.SupplierPaymentBuilder;
import ru.yandex.market.checkout.common.util.UrlBuilder;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Unit тесты для {@link PaymentBuilder}.
 *
 * @author avetokhin 05/06/17.
 */
public class PaymentBuilderTest {

    private static final long UID = 1234;
    private static final Currency CURRENCY = Currency.USD;
    private static final BigDecimal AMOUNT = new BigDecimal(8473434);
    private static final String BALANCE_ORDER_ID_1 = "orderId_1";
    private static final String BALANCE_ORDER_ID_2 = "orderId_2";
    private static final String BALANCE_ORDER_ID_3 = "orderId_3";

    private static final BigDecimal PRICE_1 = new BigDecimal(43233);
    private static final BigDecimal PRICE_2 = new BigDecimal(54344);
    private static final BigDecimal PRICE_3 = new BigDecimal(67834);
    private static final int COUNT = 10;

    private static final String OFFER_1 = "offer1";
    private static final String OFFER_2 = "offer2";

    private static final VatType VAT_1 = VatType.VAT_18_118;
    private static final VatType VAT_2 = VatType.VAT_10_110;
    private static final VatType VAT_3 = VatType.VAT_18;

    @Test
    public void testSpasiboParamsGeneration() {
        Order order = createOrder();
        PrepayPaymentBuilder paymentBuilder = new PrepayPaymentBuilder();
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));

        paymentBuilder.ensureSpasiboParams(Collections.singleton(order));
        paymentBuilder.fillSpasiboOrderMap(Collections.singleton(order));
        BigDecimal maxAmount = order.getItems().stream()
                .map(orderItem -> orderItem.getBuyerPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getCount()))
                        .subtract(BigDecimal.ONE))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal minAmount = BigDecimal.valueOf(order.getItems().size());

        CreateBasketRequest basketRequest = paymentBuilder.build();
        assertThat(basketRequest.getDeveloperPayload(),
                equalTo("{\"max_spasibo_amount\":" + maxAmount + ",\"min_spasibo_amount\":" + minAmount + "," +
                        "\"call_preview_payment\":\"card_info\"}"));
        assertThat(basketRequest.getPassParams(), equalTo(new PassParams()));
        assertThat(paymentBuilder.getSpasiboOrderMap().entrySet(), hasSize(2));
    }

    @Test
    public void testSpasiboParamsGenerationForMultiOrder() {
        Order order = createOrder();
        Order order2 = createOrder();
        PrepayPaymentBuilder paymentBuilder = new PrepayPaymentBuilder();
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));

        paymentBuilder.ensureSpasiboParams(ImmutableList.of(order, order2));
        paymentBuilder.fillSpasiboOrderMap(ImmutableList.of(order, order2));
        List<OrderItem> items = new ArrayList<>(order.getItems());
        items.addAll(order2.getItems());

        BigDecimal maxAmount = items.stream()
                .map(orderItem -> orderItem.getBuyerPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getCount()))
                        .subtract(BigDecimal.ONE))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal minAmount = BigDecimal.valueOf(items.size());

        CreateBasketRequest basketRequest = paymentBuilder.build();
        assertThat(basketRequest.getDeveloperPayload(),
                equalTo("{\"max_spasibo_amount\":" + maxAmount + ",\"min_spasibo_amount\":" + minAmount + "," +
                        "\"call_preview_payment\":" +
                        "\"card_info\"}"));
        assertThat(basketRequest.getPassParams(), equalTo(new PassParams()));
        assertThat(paymentBuilder.getSpasiboOrderMap().entrySet(), hasSize(2));
        assertThat(paymentBuilder.getSpasiboOrderMap().keySet(), containsInAnyOrder(BALANCE_ORDER_ID_1,
                BALANCE_ORDER_ID_2));
    }

    @Test
    public void testOriginPaymentInSupplierPayment() {
        final long originId = 23L;

        Order order = createOrder();
        Payment origin = new Payment();
        origin.setId(originId);

        SupplierPaymentBuilder paymentBuilder
                = new SupplierPaymentBuilder(Collections.singletonList(order), origin.getId());
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setServiceUrl(UrlBuilder.fromString("http://market.yandex.net:39001"));


        CreateBasketRequest basketRequest = paymentBuilder.build();
        String originPaymentIdJson = "{\"origin_payment_id\":" + originId + ",\"call_preview_payment\":" +
                "\"card_info\"}";
        assertThat(basketRequest.getDeveloperPayload(), equalTo(originPaymentIdJson));
        assertThat(basketRequest.getPassParams(), equalTo(new PassParams()));
    }

    public static Order createOrder() {
        final Order order = new Order();
        order.setUid(UID);
        order.setNoAuth(false);
        order.setBuyerCurrency(CURRENCY);
        order.setBuyerTotal(AMOUNT);
        order.setBalanceOrderId(BALANCE_ORDER_ID_1);

        order.setBuyer(BuyerProvider.getBuyer());

        final OrderItem item1 = new OrderItem();
        item1.setOfferId("offer1");
        item1.setBalanceOrderId(BALANCE_ORDER_ID_1);
        item1.setBuyerPrice(PRICE_1);
        item1.setCount(COUNT);
        item1.setOfferName(OFFER_1);
        item1.setVat(VAT_1);
        item1.setShopSku("SKU_1");

        final OrderItem item2 = new OrderItem();
        item2.setOfferId("offer2");
        item2.setBalanceOrderId(BALANCE_ORDER_ID_2);
        item2.setBuyerPrice(PRICE_2);
        item2.setCount(COUNT);
        item2.setOfferName(OFFER_2);
        item2.setVat(VAT_2);
        item2.setShopSku("SKU_2");

        order.setItems(Arrays.asList(item1, item2));

        final Delivery delivery = new Delivery();
        delivery.setVat(VAT_3);
        delivery.setBalanceOrderId(BALANCE_ORDER_ID_3);
        delivery.setBuyerPrice(PRICE_3);
        delivery.setPrice(PRICE_3);

        order.setDelivery(delivery);

        return order;
    }
}

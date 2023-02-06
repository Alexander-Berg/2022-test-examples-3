package ru.yandex.market.checkout.util.sberbank;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.sberbank.model.AdditionalJsonParams;
import ru.yandex.market.checkout.checkouter.sberbank.model.CartItem;
import ru.yandex.market.checkout.checkouter.sberbank.model.CartItems;
import ru.yandex.market.checkout.checkouter.sberbank.model.Installments;
import ru.yandex.market.checkout.checkouter.sberbank.model.OrderBundle;
import ru.yandex.market.checkout.checkouter.sberbank.model.ProductType;
import ru.yandex.market.checkout.checkouter.sberbank.model.Quantity;

import static java.net.URLDecoder.decode;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.helpers.utils.PaymentParameters.DEFAULT_RETURN_PATH;
import static ru.yandex.market.checkout.util.sberbank.SberMockConfigurer.REFUND_DO;
import static ru.yandex.market.checkout.util.sberbank.SberMockConfigurer.REGISTER_DO;

public final class SberMockChecker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SberMockChecker() {
    }

    public static void checkRefundCreditCall(Iterator<ServeEvent> eventsIter, BigDecimal amount) {
        ServeEvent event = eventsIter.next();
        assertEquals(REFUND_DO, event.getStubMapping().getName());

        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getQueryParams();
        assertThat(queryParams.entrySet(), hasSize(4));
        assertThat(queryParams.get("userName").size(), equalTo(1));
        assertThat(queryParams.get("password").size(), equalTo(1));

        assertThat(queryParams.get("orderId").size(), equalTo(1));
        assertThat(queryParams.getFirst("orderId"), equalTo("f7a92c89-79e7-7ac2-a173-637a04b35dad")); // см. json-моки

        assertThat(queryParams.get("amount").size(), equalTo(1));
        assertThat(queryParams.getFirst("amount"), equalTo(
                String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue())));
    }

    public static void checkCreateCreditCall(Iterator<ServeEvent> eventsIter, Long paymentId, String returnUrl,
                                             List<Order> orders)
            throws IOException {
        ServeEvent event = eventsIter.next();
        assertEquals(REGISTER_DO, event.getStubMapping().getName());

        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getQueryParams();
        assertThat(queryParams.get("userName").size(), equalTo(1));
        assertThat(queryParams.get("password").size(), equalTo(1));

        Order firstOrder = orders.get(0);

        Map<String, String> parameters = new HashMap<>();
        for (String v : split(event.getRequest().getBodyAsString(), "&")) {
            parameters.put(
                    substringBefore(v, "="),
                    decode(substringAfter(v, "="), StandardCharsets.UTF_8.name()));
        }

        assertEquals(8, parameters.size()); // see SberbankRestAPI.buildRegisterDoBody
        assertEquals(paymentId.toString(), parameters.get("orderNumber"));
        assertEquals(ordersTotalAmount(orders), parameters.get("amount"));
        assertEquals(String.valueOf(firstOrder.getBuyerCurrency().getJdkCurrency().getNumericCode()), parameters.get(
                "currency"));
        assertEquals(returnUrl, parameters.get("returnUrl"));
        assertEquals(returnUrl, parameters.get("failUrl"));
        assertEquals(buildExpectedJsonParams(firstOrder),
                OBJECT_MAPPER.readValue(parameters.get("jsonParams"), AdditionalJsonParams.class));
        assertEquals(buildExpectedOrderBundle(orders, firstOrder.getPaymentMethod()),
                OBJECT_MAPPER.readValue(parameters.get("orderBundle"), OrderBundle.class));
    }

    public static void checkCreateCreditCall(Iterator<ServeEvent> eventsIter, Long paymentId, List<Order> orders)
            throws IOException {
        checkCreateCreditCall(eventsIter, paymentId, DEFAULT_RETURN_PATH, orders);
    }


    private static String ordersTotalAmount(List<Order> orders) {
        return String.valueOf(orders.stream().map(Order::getBuyerTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(new BigDecimal(100)).longValue());
    }

    private static AdditionalJsonParams buildExpectedJsonParams(Order order) {
        return new AdditionalJsonParams(order.getBuyer().getEmail(), order.getBuyer().getNormalizedPhone());
    }

    private static OrderBundle buildExpectedOrderBundle(List<Order> orders, PaymentMethod method) {
        return new OrderBundle(
                new Installments(mapPaymentMethod(method)),
                new CartItems(buildExpectedCartItems(orders))
        );
    }

    private static List<CartItem> buildExpectedCartItems(List<Order> orders) {
        List<CartItem> items = new ArrayList<>();
        int position = 0;
        for (Order order : orders) {
            for (OrderItem orderItem : order.getItems().stream()
                    .sorted(Comparator.comparing(OrderItem::getId)).collect(Collectors.toList())) {
                items.add(new CartItem(
                        position,
                        orderItem.getOfferName(),
                        new Quantity((long) orderItem.getCount(), "шт."),
                        orderItem.getBuyerPrice().multiply(new BigDecimal(100)).longValue(),
                        order.getBuyerCurrency().getJdkCurrency().getNumericCode(),
                        order.getId() + "-item-" + orderItem.getId()
                ));

                position++;
            }

            if (order.getDelivery() != null && !order.getDelivery().isFree()) {
                items.add(new CartItem(
                        position,
                        "Доставка",
                        new Quantity(1L, "шт."),
                        order.getDelivery().getBuyerPrice().multiply(new BigDecimal(100)).longValue(),
                        order.getBuyerCurrency().getJdkCurrency().getNumericCode(),
                        order.getId() + "-delivery"
                ));
                position++;
            }
        }

        return items;
    }

    private static ProductType mapPaymentMethod(PaymentMethod method) {
        if (method == PaymentMethod.CREDIT) {
            return ProductType.CREDIT;
        }
        if (method == PaymentMethod.INSTALLMENT) {
            return ProductType.INSTALLMENT;
        }
        throw new IllegalArgumentException("incorrect paymentMethod: " + method);
    }

}

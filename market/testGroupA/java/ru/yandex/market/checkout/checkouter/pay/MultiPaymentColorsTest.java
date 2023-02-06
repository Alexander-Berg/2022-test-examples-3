package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * Тест оплаты мультизаказа с разными цветами заказов
 *
 * @author gelvy
 * Created on: 15.10.2020
 * @see <a href="https://st.yandex-team.ru/MARKETCHECKOUT-16216"/>
 */
public class MultiPaymentColorsTest extends AbstractPaymentTestBase {

    private static final Long EXPECTED_BALANCE_SERVICE_ID = 610L;
    @Autowired
    PaymentService paymentService;
    @Autowired
    RefundService refundService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private TestSerializationService serializationService;

    @BeforeEach
    public void prepareOrders() {
        trustMockConfigurer.resetRequests();
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата синего и белого заказов одним платежом.")
    @Test
    public void payOrdersWithSameTrustServiceToken() throws Exception {
        List<Order> orders = new ArrayList<>();
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(order -> order.setRgb(Color.WHITE)));
        BigDecimal total = orders.stream()
                .map(BasicOrder::getBuyerTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        String returnPath = (new PaymentParameters()).getReturnPath();
        ordersPay(asIds(orders), returnPath)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.balanceServiceId").value(numberEqualsTo(EXPECTED_BALANCE_SERVICE_ID)))
                .andExpect(jsonPath("$.paymentForm").isNotEmpty())
                .andExpect(jsonPath("$.paymentForm.purchase_token").isNotEmpty())
                .andExpect(jsonPath("$.paymentForm._TARGET").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").value(formatAmount(total)));

        Collection<Order> payedOrders = orderService.getOrders(asIds(orders)).values();

        assertEquals(2, payedOrders.size());
        assertEquals(1, payedOrders.stream().map(o -> o.getPayment().getBasketId()).distinct().count());
        paymentHelper.payForOrders(orders);
        paymentTestHelper.tryClearMultipayment(orders, Collections.emptyList());
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Разметка платежа для разноцветного заказа.")
    @Test
    public void markupPaymentForMulticolorOrder() throws Exception {
        Order blueOrder = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        Order whiteOrder = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        Payment payment = orderPayHelper.payForOrdersWithoutNotification(Arrays.asList(blueOrder, whiteOrder));

        mockMvc.perform(post("/payments/{purchaseToken}/markup",
                payment.getBasketKey().getPurchaseToken())
                .content(serializationService.serializeCheckouterObject(
                        createDefaultPartitions(BigDecimal.valueOf(250))))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    private List<Long> asIds(Collection<Order> orders) {
        return orders.stream().map(Order::getId).collect(Collectors.toList());
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(0, BigDecimal.ROUND_HALF_EVEN).toString();
    }

    private PaymentPartitions createDefaultPartitions(BigDecimal total) {
        PaymentPartitions partitions = new PaymentPartitions();
        IncomingPaymentPartition spasiboPart = new IncomingPaymentPartition(
                PaymentAgent.SBER_SPASIBO,
                total.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN)
        );
        partitions.setPartitions(singletonList(spasiboPart));
        return partitions;
    }
}

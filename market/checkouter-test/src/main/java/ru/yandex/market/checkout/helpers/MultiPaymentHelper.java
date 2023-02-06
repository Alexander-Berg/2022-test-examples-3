package ru.yandex.market.checkout.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class MultiPaymentHelper extends MockMvcAware {

    public MultiPaymentHelper(WebApplicationContext webApplicationContext,
                              TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public static CheckBasketParams checkBasketForClearTask(List<Order> orders) {
        Collection<CheckBasketParams.BasketLineState> basketLines = new ArrayList<>();
        Collection<CheckBasketParams.BasketRefund> basketRefunds = new ArrayList<>();

        for (Order order : orders) {
            if (order.getBalanceOrderId() != null) {
                basketLines.add(new CheckBasketParams.BasketLineState(
                        order.getBalanceOrderId(), 1, order.getTotal()));
            } else {
                order.getItems().forEach(i -> basketLines.add(
                        new CheckBasketParams.BasketLineState(
                                i.getBalanceOrderId(), i.getCount(), i.getBuyerPrice())
                        )
                );
                if (!order.getDelivery().isFree()) {
                    basketLines.add(
                            new CheckBasketParams.BasketLineState(
                                    order.getDelivery().getBalanceOrderId(), 1, order.getDelivery().getPrice())
                    );
                }
            }
        }

        CheckBasketParams config = new CheckBasketParams();
        config.setLines(basketLines);
        config.setRefunds(basketRefunds);

        return config;
    }

    public static CheckBasketParams checkBasketSplitOrderItemsForClearTask(List<Order> orders) {
        Collection<CheckBasketParams.BasketLineState> basketLines = new ArrayList<>();
        Collection<CheckBasketParams.BasketRefund> basketRefunds = new ArrayList<>();

        for (Order order : orders) {
            if (order.getBalanceOrderId() != null) {
                basketLines.add(new CheckBasketParams.BasketLineState(
                        order.getBalanceOrderId(), 1, order.getTotal()));
            } else {
                for (OrderItem i : order.getItems()) {
                    for (int j = 1; j <= i.getCount(); j++) {
                        CheckBasketParams.BasketLineState line = new CheckBasketParams.BasketLineState(
                                i.getBalanceOrderId() + "-" + j, 1, i.getBuyerPrice());
                        basketLines.add(line);
                    }
                }
                if (!order.getDelivery().isFree()) {
                    basketLines.add(
                            new CheckBasketParams.BasketLineState(
                                    order.getDelivery().getBalanceOrderId(), 1, order.getDelivery().getPrice())
                    );
                }
            }
        }

        CheckBasketParams config = new CheckBasketParams();
        config.setLines(basketLines);
        config.setRefunds(basketRefunds);

        return config;
    }

    public Payment ordersPay(long uid, List<Long> orderIds, String returnPath) throws Exception {
        MockHttpServletRequestBuilder builder = post("/orders/payment/")
                .contentType(MediaType.APPLICATION_JSON)
                .content((new JSONArray(orderIds)).toString())
                .param("uid", String.valueOf(uid));
        if (returnPath != null) {
            builder.param("returnPath", returnPath);
        }

        return performApiRequest(builder, Payment.class);
    }

    public void notifyMultiPayment(Payment multiPayment) throws Exception {
        mockMvc.perform(post("/payments/{paymentId}/notify?status={status}&trust_payment_id={basketId}&mode={mode}",
                multiPayment.getId(), BasketStatus.success, multiPayment.getBasketId(), NotificationMode.result))
                .andDo(log())
                .andExpect(status().isOk());
    }

}

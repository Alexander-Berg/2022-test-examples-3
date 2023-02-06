package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CheckBasketTest extends AbstractPaymentTestBase {

    @Test
    public void testCheckBasket() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initAndHoldPayment();

        mockMvc.perform(get("/payments/{paymentId}/basket", order().getPaymentId()))
                .andExpect(status().isOk())
                .andDo(log());
    }

    @Disabled("Включить когда будет переезд на кэшир")
    @Test
    public void testCheckBasketViaCashier() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initAndHoldPayment();

        mockMvc.perform(get("/payments/{paymentId}/basket", order().getPaymentId()))
                .andExpect(status().isOk())
                .andDo(log());
    }
}

package ru.yandex.market.checkout.checkouter.controller;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentSupportControllerTest extends AbstractPaymentTestBase {

    @Autowired
    private PaymentService paymentService;

    private static final String PROCESS_HELD_PAYMENTS_URL = "/process-held-payments";

    @Test
    void testManualProcessHeldPayments() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initAndHoldPayment();
        Long paymentId = order().getPayment().getId();

        mockMvc.perform(post(PROCESS_HELD_PAYMENTS_URL)
                .content(String.valueOf(Set.of(paymentId)))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        Payment payment = paymentService.getPayment(paymentId, ClientInfo.SYSTEM);
        assertTrue(payment.isCleared());
    }

}

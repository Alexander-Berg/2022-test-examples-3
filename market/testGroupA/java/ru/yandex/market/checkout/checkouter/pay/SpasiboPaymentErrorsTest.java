package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.balance.model.notifications.PaymentNotification.checkPaymentNotification;

public class SpasiboPaymentErrorsTest extends AbstractPaymentTestBase {

    @Autowired
    protected WireMockServer trustMock;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TestSerializationService serializationService;

    @Test
    public void markupWithoutPartitions() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initPayment();
        trustMockConfigurer.resetRequests();
        mockMvc.perform(post("/payments/{purchaseToken}/markup",
                order().getPayment().getBasketKey().getPurchaseToken())
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(".code").value("INVALID_FORMAT"));
    }

    @Test
    public void markupForNonExistentPayment() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initPayment();
        trustMockConfigurer.resetRequests();
        mockMvc.perform(post("/payments/{purchaseToken}/markup",
                "some-non-existing-purchase-token")
                .content(serializationService.serializeCheckouterObject(createDefaultPartitions()))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(".code").value("payment_not_found"));
    }

    @Test
    public void markupForCancelledPayment() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initPayment();
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildFailCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildFailCheckBasket(), null);
        paymentService.notifyPayment(checkPaymentNotification(order().getPaymentId(), false));
        trustMockConfigurer.resetRequests();
        mockMvc.perform(post("/payments/{purchaseToken}/markup",
                order().getPayment().getBasketKey().getPurchaseToken())
                .content(serializationService.serializeCheckouterObject(createDefaultPartitions()))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(".code").value("INVALID_PAYMENT_STATUS"));
    }

    private PaymentPartitions createDefaultPartitions() {
        BigDecimal total = order.get().getBuyerItemsTotal();
        PaymentPartitions partitions = new PaymentPartitions();
        IncomingPaymentPartition spasiboPart = new IncomingPaymentPartition(
                PaymentAgent.SBER_SPASIBO,
                total.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN)
        );
        partitions.setPartitions(singletonList(spasiboPart));
        return partitions;
    }
}

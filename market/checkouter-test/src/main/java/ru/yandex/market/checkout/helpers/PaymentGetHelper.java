package ru.yandex.market.checkout.helpers;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ARCHIVED;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;

@WebTestHelper
public class PaymentGetHelper extends MockMvcAware {
    public PaymentGetHelper(WebApplicationContext webApplicationContext, TestSerializationService
            testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public PagedPayments getOrderPayments(long orderId,
                                          ClientInfo clientInfo) throws Exception {
        return performApiRequest(get("/orders/{orderId}/payments", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId())), PagedPayments.class);
    }

    public ResultActions getOrderPaymentsForActions(long orderId,
                                                    ClientInfo clientInfo) throws Exception {
        return getOrderPaymentsForActions(orderId, clientInfo, false);
    }

    public ResultActions getOrderPaymentsForActions(long orderId,
                                                    ClientInfo clientInfo, boolean archived) throws Exception {
        return performApiRequest(get("/orders/{orderId}/payments", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId()))
                .param(ARCHIVED, String.valueOf(archived)));
    }


    public Payment getOrderPayment(long orderId,
                                          ClientInfo clientInfo) throws Exception {
        return performApiRequest(get("/orders/{orderId}/payment", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId())), Payment.class);
    }

    public ResultActions getOrderPaymentForActions(long orderId,
                                                   ClientInfo clientInfo) throws Exception {
        return getOrderPaymentForActions(orderId, clientInfo, false);
    }

    public ResultActions getOrderPaymentForActions(long orderId, ClientInfo clientInfo, boolean archived)
            throws Exception {
        return performApiRequest(get("/orders/{orderId}/payment", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId()))
                .param(ARCHIVED, String.valueOf(archived)));
    }

    public ResultActions getPaymentForActions(long paymentId, ClientInfo clientInfo, boolean archived)
            throws Exception {
        return performApiRequest(get("/payments/{paymentId}", paymentId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId()))
                .param(ARCHIVED, String.valueOf(archived)));
    }
}

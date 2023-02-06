package steps.shopSteps;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.mbi.api.client.entity.payment.PaymentCheckStatus;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

public class PaymentStatusSteps {
    private static final boolean YAM_CONTRACT_STATUS_COMPLETED = true;
    private static final PaymentCheckStatus PAYMENT_CHECK_STATUS = PaymentCheckStatus.SUCCESS;

    private PaymentStatusSteps() {
    }

    static JSONObject getPaymentStatusJson() throws JSONException {
        JSONObject paymentStatusJson = new JSONObject();

        paymentStatusJson.put("yamContractStatusCompleted", YAM_CONTRACT_STATUS_COMPLETED);
        paymentStatusJson.put("paymentCheckStatus", PAYMENT_CHECK_STATUS);

        return paymentStatusJson;
    }

    public static Shop.PaymentStatus getPaymentStatus() {
        return new Shop.PaymentStatus(PAYMENT_CHECK_STATUS, YAM_CONTRACT_STATUS_COMPLETED);
    }
}

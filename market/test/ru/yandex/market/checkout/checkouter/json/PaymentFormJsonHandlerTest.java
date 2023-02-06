package ru.yandex.market.checkout.checkouter.json;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentForm;

public class PaymentFormJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{\n" +
                "    \"id\": 108148,\n" +
                "    \"orderId\": 441081,\n" +
                "    \"basketId\": \"57c3d4a2795be2558c54c08d\",\n" +
                "    \"fake\": false,\n" +
                "    \"status\": \"INIT\",\n" +
                "    \"uid\": 4001792531,\n" +
                "    \"currency\": \"RUR\",\n" +
                "    \"totalAmount\": 360,\n" +
                "    \"creationDate\": \"29-08-2016 09:22:24\",\n" +
                "    \"updateDate\": \"29-08-2016 09:22:24\",\n" +
                "    \"statusUpdateDate\": \"29-08-2016 09:22:24\",\n" +
                "    \"statusExpiryDate\": \"29-08-2016 09:42:24\",\n" +
                "    \"paymentForm\": {\n" +
                "        \"purchase_token\": \"599fba1593d8f7f246b4571cc7e28568\",\n" +
                "        \"_TARGET\": \"https://tmongo1f.fin.yandex.ru/web/payment\"\n" +
                "    }\n" +
                "}";
        Payment payment = read(Payment.class, json);
        PaymentForm paymentForm = payment.getPaymentForm();
        Assertions.assertNotNull(paymentForm);
        Assertions.assertEquals("599fba1593d8f7f246b4571cc7e28568", paymentForm.getParameters().get("purchase_token"));
        Assertions.assertEquals("https://tmongo1f.fin.yandex.ru/web/payment", paymentForm.getParameters().get(
                "_TARGET"));
    }

    @Test
    public void serialize() throws Exception {
        PaymentForm paymentForm = new PaymentForm(ImmutableMap.of(
                "purchase_token", "599fba1593d8f7f246b4571cc7e28568",
                "_TARGET", "https://tmongo1f.fin.yandex.ru/web/payment"
        ));

        String json = write(paymentForm);

        checkJson(json, "$.purchase_token", "599fba1593d8f7f246b4571cc7e28568");
        checkJson(json, "$._TARGET", "https://tmongo1f.fin.yandex.ru/web/payment");
    }
}

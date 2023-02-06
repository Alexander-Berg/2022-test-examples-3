package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.legacy.PaymentSubMethod;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class PaymentOptionJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String JSON = "{ " +
            "\"paymentMethod\": \"YANDEX\", " +
            "\"paymentSubMethods\" : [\"YA_MONEY\"], " +
            "\"hiddenReason\": \"MUID\" " +
            "}";

    @Test
    public void deserialize() throws Exception {
        PaymentOption paymentOption = read(PaymentOption.class, JSON);

        Assertions.assertEquals(PaymentMethod.YANDEX, paymentOption.getPaymentMethod());
        assertThat(paymentOption.getPaymentSubMethods(), hasItem(PaymentSubMethod.YA_MONEY));
        Assertions.assertEquals(PaymentOptionHiddenReason.MUID, paymentOption.getHiddenReason());
    }

    @Test
    public void serialize() throws Exception {
        PaymentOption paymentOption = new PaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.MUID);
        paymentOption.setPaymentSubMethods(Collections.singletonList(PaymentSubMethod.YA_MONEY));

        String json = write(paymentOption);

        checkJson(json, "$." + Names.PaymentOption.PAYMENT_TYPE, PaymentType.PREPAID.name());
        checkJson(json, "$." + Names.PaymentOption.PAYMENT_METHOD, PaymentMethod.YANDEX.name());
        checkJsonMatcher(json, "$." + Names.PaymentOption.PAYMENT_SUBMETHODS,
                hasItem(PaymentSubMethod.YA_MONEY.name()));
        checkJson(json, "$." + Names.PaymentOption.HIDDEN_REASON, PaymentOptionHiddenReason.MUID.name());
    }
}

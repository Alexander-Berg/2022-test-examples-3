package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.util.PaymentUtil;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderPaymentDetailsActualizerTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator
            .dataRandom(OrderPaymentDetailsActualizerTest.class).build();

    private final OrderPaymentDetailsActualizer orderPaymentDetailsActualizer;

    @Test
    public void testValid() {
        OrderActualization orderActualization = RANDOM.nextObject(OrderActualization.class, "errors", "warnings");
        orderActualization.getOrderPayment().setTrustPaymethodId(PaymentUtil.APPLE_PAY_PAYMETHOD);
        OrderActualization actualize = orderPaymentDetailsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getErrors()).isEmpty();
        Assertions.assertThat(actualize.getWarnings()).isEmpty();
    }

    @Test
    public void testAppleTokenMissed() {
        OrderActualization orderActualization = RANDOM.nextObject(OrderActualization.class, "errors", "warnings");
        orderActualization.getOrderPayment()
                .setTrustPaymethodId(PaymentUtil.APPLE_PAY_PAYMETHOD)
                .setAppleToken(null);
        OrderActualization actualize = orderPaymentDetailsActualizer.actualize(orderActualization);
        OrderActualization expected = new OrderActualization()
                .setAppliedPromos(null);
        expected.getErrors().add(new ActualizationError()
                .setCode(ActualizationError.Code.PAYMENT_ISSUES)
                .setMessage("Apple token is missed"));
        Assertions.assertThat(actualize).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expected);
    }

}

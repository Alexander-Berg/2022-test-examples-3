package ru.yandex.market.checkout.checkouter.antifraud;

import java.util.Arrays;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.Payment;

import static org.hamcrest.MatcherAssert.assertThat;

public class AntifraudLogFormatterTest {

    public static Stream<Arguments> parameterizedTestData() {
        Buyer buyer = new Buyer();
        buyer.setUid(6666L);
        buyer.setYandexUid("YandexUidYandexUid");

        Payment payment = new Payment();
        payment.setBasketId("BasketIdBasketId");
        payment.setId(123123L);

        long orderId = 555L;

        String detectorName = "fraudDetectedOlolol!";

        String expectedMessage = "\tfraudDetectedOlolol!\t555";
        return Arrays.asList(new Object[][]{
                {
                        buyer,
                        payment,
                        orderId,
                        detectorName,
                        expectedMessage
                },
                {
                        null,
                        null,
                        444L,
                        "someDetecter",
                        "\tsomeDetecter\t444\t\t"
                },
        }).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void format(Buyer buyer, Payment payment, long orderId, String detectorName, String expectedMessage)
            throws Exception {
        Order o = new Order();
        o.setBuyer(buyer);
        o.setPayment(payment);
        o.setId(orderId);
        AntifraudLogFormatter logFormatter = new AntifraudLogFormatter(ImmutableOrder.from(o), detectorName);
        String logMessage = logFormatter.format();
        assertThat(logMessage, CoreMatchers.containsString(expectedMessage));
    }
}

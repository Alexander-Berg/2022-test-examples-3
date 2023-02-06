package ru.yandex.market.core.order.payment;

import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Тесты для {@link ServiceOrderId}.
 *
 * @author vbudnev
 */
class ServiceOrderIdTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("", matcher(null, null)),
                Arguments.of("randomStuff", matcher(null, null)),
                Arguments.of("123567", matcher(null, null)),
                Arguments.of("123-567", matcher(123L, 567L)),
                Arguments.of("123-random-words-567-more-words", matcher(123L, 567L)),
                Arguments.of("123-iTeM-567", matcher(123L, null)),
                Arguments.of("123-item_underscored-567", matcher(123L, null)),
                // Боевые примеры
                Arguments.of("123-item-567", matcher(123L, 567L)),
                Arguments.of("4616789-cash-396816-refund-5606774-1", matcher(4616789L, 5606774L)),
                Arguments.of("4810142-item-subsidy-5994583", matcher(4810142L, 5994583L)),
                // Пример crossborder
                Arguments.of("6198705-order_prepay", matcher(6198705L, null)),
                // Примеры возвратов
                Arguments.of("14705848-item-5773060-ret-10107-2", matcher(14705848L, 5773060L, 10107L)),
                Arguments.of("39422725-item-76259621-ret-727107-2", matcher(39422725L, 76259621L, 727107L)),
                //Пример парсинга идентификатора команды на выплату
                Arguments.of("payment-order-123", matcher(null, null, null, 123L))
        );
    }

    private static Matcher<ServiceOrderId> matcher(Long orderId, Long orderItemId) {
        return matcher(orderId, orderItemId, null);
    }

    private static Matcher<ServiceOrderId> matcher(Long orderId, Long orderItemId, Long returnId) {
        return MbiMatchers.<ServiceOrderId>newAllOfBuilder()
                .add(ServiceOrderId::getOrderId, orderId, "orderId")
                .add(ServiceOrderId::getItemId, orderItemId, "itemId")
                .add(ServiceOrderId::getReturnId, returnId, "returnId")
                .build();
    }

    private static Matcher<ServiceOrderId> matcher(Long orderId, Long orderItemId, Long returnId, Long paymentOrderId) {
        return MbiMatchers.<ServiceOrderId>newAllOfBuilder()
                .add(ServiceOrderId::getOrderId, orderId, "orderId")
                .add(ServiceOrderId::getItemId, orderItemId, "itemId")
                .add(ServiceOrderId::getReturnId, returnId, "returnId")
                .add(ServiceOrderId::getPaymentOrderId, paymentOrderId, "paymentOrderId")
                .build();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource(value = "args")
    void test(String serviceOrderId, Matcher<ServiceOrderId> matcher) {
        assertThat(
                ServiceOrderId.from(serviceOrderId),
                matcher
        );
    }

}

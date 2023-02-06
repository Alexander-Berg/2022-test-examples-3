package ru.yandex.market.logistics.nesu.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.api.converter.OrderPaymentMethodConverter;
import ru.yandex.market.logistics.nesu.dto.order.OrderCost;

import static ru.yandex.market.logistics.nesu.dto.enums.PaymentMethod.CARD;
import static ru.yandex.market.logistics.nesu.dto.enums.PaymentMethod.CASH;
import static ru.yandex.market.logistics.nesu.dto.enums.PaymentMethod.PREPAID;

public class ConvertPaymentMethodTest extends AbstractTest {
    private final OrderPaymentMethodConverter converter = new OrderPaymentMethodConverter(new EnumConverter());

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "prepaid",
        "cash",
        "card",
    })
    void convertPaymentMethod(
        String displayName,
        OrderCost orderCost,
        Boolean cardAllowed,
        Boolean cashAllowed,
        PaymentMethod expectedPaymentMethod
    ) {
        softly.assertThat(converter.convertPaymentMethod(orderCost, cardAllowed, cashAllowed))
            .isEqualTo(expectedPaymentMethod);
    }

    @Nonnull
    private static Stream<Arguments> prepaid() {
        return Stream.of(
            Arguments.of(
                "PREPAID, cardAllowed is null, cashAllowed is null -> PREPAID",
                new OrderCost().setPaymentMethod(PREPAID),
                null,
                null,
                PaymentMethod.PREPAID
            ),
            Arguments.of(
                "PREPAID (fullyPrepaid is true), cardAllowed is null, cashAllowed is null -> PREPAID",
                new OrderCost().setPaymentMethod(PREPAID).setFullyPrepaid(true),
                null,
                null,
                PaymentMethod.PREPAID
            ),
            Arguments.of(
                "PREPAID, cardAllowed is true, cashAllowed is false -> CARD",
                new OrderCost().setPaymentMethod(PREPAID).setFullyPrepaid(false),
                true,
                false,
                PaymentMethod.CARD
            ),
            Arguments.of(
                "PREPAID, cardAllowed is false, cashAllowed is true -> CASH",
                new OrderCost().setPaymentMethod(PREPAID).setFullyPrepaid(false),
                false,
                true,
                PaymentMethod.CASH
            ),
            Arguments.of(
                "PREPAID, cardAllowed is true, cashAllowed is true -> CASH",
                new OrderCost().setPaymentMethod(PREPAID).setFullyPrepaid(false),
                true,
                true,
                PaymentMethod.CASH
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> cash() {
        return Stream.of(
            Arguments.of(
                "CASH, cardAllowed is null, cashAllowed is null -> CASH",
                new OrderCost().setPaymentMethod(CASH),
                null,
                null,
                PaymentMethod.CASH
            ),
            Arguments.of(
                "CASH (fullyPrepaid is true), cardAllowed is null, cashAllowed is null -> PREPAID",
                new OrderCost().setPaymentMethod(CASH).setFullyPrepaid(true),
                null,
                null,
                PaymentMethod.PREPAID
            ),
            Arguments.of(
                "CASH, cardAllowed is true, cashAllowed is false -> CARD",
                new OrderCost().setPaymentMethod(CASH),
                true,
                false,
                PaymentMethod.CARD
            ),
            Arguments.of(
                "CASH, cardAllowed is false, cashAllowed is true -> CASH",
                new OrderCost().setPaymentMethod(CASH),
                false,
                true,
                PaymentMethod.CASH
            ),
            Arguments.of(
                "CASH, cardAllowed is true, cashAllowed is true -> CASH",
                new OrderCost().setPaymentMethod(CASH),
                true,
                true,
                PaymentMethod.CASH
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> card() {
        return Stream.of(
            Arguments.of(
                "CARD, cardAllowed is null, cashAllowed is null -> CARD",
                new OrderCost().setPaymentMethod(CARD),
                null,
                null,
                PaymentMethod.CARD
            ),
            Arguments.of(
                "CARD (fullyPrepaid is true), cardAllowed is null, cashAllowed is null -> PREPAID",
                new OrderCost().setPaymentMethod(CARD).setFullyPrepaid(true),
                null,
                null,
                PaymentMethod.PREPAID
            ),
            Arguments.of(
                "CARD, cardAllowed is true, cashAllowed is false -> CARD",
                new OrderCost().setPaymentMethod(CARD),
                true,
                false,
                PaymentMethod.CARD
            ),
            Arguments.of(
                "CARD, cardAllowed is false, cashAllowed is true -> CASH",
                new OrderCost().setPaymentMethod(CARD),
                false,
                true,
                PaymentMethod.CASH
            ),
            Arguments.of(
                "CARD, cardAllowed is true, cashAllowed is true -> CARD",
                new OrderCost().setPaymentMethod(CARD),
                true,
                true,
                PaymentMethod.CARD
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void convertPaymentMethodInvalid(
        String displayName,
        OrderCost orderCost,
        Boolean cardAllowed,
        Boolean cashAllowed
    ) {
        softly.assertThatThrownBy(() -> converter.convertPaymentMethod(orderCost, cardAllowed, cashAllowed))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Order is not prepaid, and cash and card are both not available");
    }

    @Nonnull
    private static Stream<Arguments> convertPaymentMethodInvalid() {
        return Stream.of(
            Arguments.of(
                "CARD, cardAllowed is false, cashAllowed is false",
                new OrderCost().setPaymentMethod(CARD),
                false,
                false
            ),
            Arguments.of(
                "CASH, cardAllowed is false, cashAllowed is false",
                new OrderCost().setPaymentMethod(CASH),
                false,
                false
            ),
            Arguments.of(
                "PREPAID (fullyPrepaid is false), cardAllowed is false, cashAllowed is false",
                new OrderCost().setPaymentMethod(PREPAID).setFullyPrepaid(false),
                false,
                false
            )
        );
    }
}

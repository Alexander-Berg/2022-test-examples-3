package ru.yandex.market.pers.notify.ems.consumer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты проверяет отправку писем типа {@link NotificationSubtype#ORDER_CANCELLED} с использованием рассылятора.
 *
 * В данном наборе находятся тесты, проверяющие какие-то дополнительные по отношению к основному набору тестов
 * {@link CancelledOrderSenderTestMain} условия.
 *
 * @author semin-serg
 */

public class CancelledOrderSenderTestExtra extends CancelledOrderSenderTestsCommon {

    @Test
    public void testSecretKeyFromCheckoutData() throws Exception {
        //OrderSubstatus мог бы быть любым, просто для примера выбран SHOP_FAILED
        test(OrderSubstatus.SHOP_FAILED, "SHOP_FAILED", SkSource.EVENT_SOURCE_DATA);
    }

    @Test
    public void testLackOfSecretKeyProcessing() throws Exception {
        when(aboService.generateSkFeedback(anyLong())).thenReturn(null);
        test(
                //OrderSubstatus мог бы быть любым, просто для примера выбран SHOP_FAILED
                OrderSubstatus.SHOP_FAILED,
                null,
                null,
                SkSource.ABO,
                NotificationEventStatus.REJECTED_WITH_ERRORS
        );
    }

}

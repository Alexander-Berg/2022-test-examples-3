package ru.yandex.travel.orders.services.finances.billing;

import java.time.LocalDate;
import java.time.Month;
import java.util.function.Consumer;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.finances.BankOrder;
import ru.yandex.travel.orders.entities.finances.BankOrderDetail;
import ru.yandex.travel.orders.entities.finances.BankOrderPayment;

public final class RandomObjects {
    private static final EasyRandomParameters COMMON_PARAMETERS = new EasyRandomParameters()
            .dateRange(LocalDate.of(2019, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 1));

    private static final EasyRandom BANK_ORDER_DETAILS_GENERATOR = new EasyRandom(COMMON_PARAMETERS
            .randomize(ProtoCurrencyUnit.class, () -> ProtoCurrencyUnit.RUB)
            .excludeField(field -> field.getName().equals("bankOrderPayment"))
    );

    private static final EasyRandom BANK_ORDER_PAYMENT_GENERATOR = new EasyRandom(COMMON_PARAMETERS
            .randomize(BankOrderDetail.class, () -> BANK_ORDER_DETAILS_GENERATOR.nextObject(BankOrderDetail.class))
            .excludeField(field -> field.getName().equals("orders"))
            .collectionSizeRange(1, 4)
    );

    private static final EasyRandom BANK_ORDER_GENERATOR = new EasyRandom(COMMON_PARAMETERS
            .randomizationDepth(1)
            .randomize(BankOrderPayment.class, () -> BANK_ORDER_PAYMENT_GENERATOR.nextObject(BankOrderPayment.class))
    );

    public static BankOrder createBankOrder(Consumer<BankOrder> modifier) {
        final BankOrder bankOrder = BANK_ORDER_GENERATOR.nextObject(BankOrder.class);
        modifier.accept(bankOrder);
        return bankOrder;
    }

    RandomObjects() {
        /* static class */
    }
}

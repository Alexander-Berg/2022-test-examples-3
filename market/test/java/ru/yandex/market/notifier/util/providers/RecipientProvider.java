package ru.yandex.market.notifier.util.providers;

import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;

public abstract class RecipientProvider {
    public static Recipient getDefaultRecipient() {
        return new Recipient(
                new RecipientPerson("Leo", null, "Tolstoy"),
                "+71234567891", null, "leo@ya.ru");
    }
}

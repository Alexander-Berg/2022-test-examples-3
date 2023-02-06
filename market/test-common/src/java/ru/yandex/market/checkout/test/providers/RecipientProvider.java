package ru.yandex.market.checkout.test.providers;

import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;

public abstract class RecipientProvider {
    public static Recipient getDefaultRecipient() {
        return new Recipient(
                new RecipientPerson("Leo", null, "Tolstoy"), "cd368e42341ff6ca7bbd12a05998e705",
                "+71234567891", "c0dec0dedec0dec0dec0dec0dedec0de",
                "leo@ya.ru", "909701f1b46bdd8a4cafe2ec68255373");
    }
}

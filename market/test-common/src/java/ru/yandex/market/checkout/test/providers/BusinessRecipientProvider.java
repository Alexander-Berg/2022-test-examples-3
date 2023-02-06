package ru.yandex.market.checkout.test.providers;

import ru.yandex.market.checkout.checkouter.delivery.BusinessRecipient;

public class BusinessRecipientProvider {

    private BusinessRecipientProvider() {
    }

    public static BusinessRecipient getDefaultBusinessRecipient() {
        var recipient = new BusinessRecipient();
        recipient.setName("ООО Рога и Копыта (c)");
        recipient.setInn("123_test_inn_321");
        recipient.setKpp("123_test_kpp_321");
        return recipient;
    }
}

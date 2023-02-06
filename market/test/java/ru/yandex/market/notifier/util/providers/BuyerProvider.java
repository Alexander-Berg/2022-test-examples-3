package ru.yandex.market.notifier.util.providers;

import ru.yandex.market.checkout.checkouter.order.Buyer;

public abstract class BuyerProvider {

    public static final long UID = 359953025L;
    public static final String YANDEX_UID = "test-yandex-uid";
    public static final String PHONE = "+71234567891";

    public static Buyer getBuyer() {
        return getDefaultBuyer(UID);
    }

    public static Buyer getDefaultBuyer(long uid) {
        Buyer buyer = new Buyer(uid);
        buyer.setRegionId(213L);
        buyer.setIpRegionId(213L);
        buyer.setUserAgent("Mozilla/5.0 ...");
        buyer.setFirstName("Leo");
        buyer.setLastName("Tolstoy");
        buyer.setEmail("a@b.com");
        buyer.setPhone(PHONE);
        buyer.setNormalizedPhone("71234567891");
        buyer.setYandexUid(YANDEX_UID);
        return buyer;
    }
}

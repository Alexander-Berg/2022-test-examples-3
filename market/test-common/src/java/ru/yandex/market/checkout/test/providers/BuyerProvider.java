package ru.yandex.market.checkout.test.providers;

import ru.yandex.market.checkout.checkouter.order.Buyer;

public abstract class BuyerProvider {

    public static final long UID = 359953025L;
    public static final long SBER_ID = (1L << 61) - 1L;
    public static final long MUID = 1L << 60;
    public static final long ASSESSOR_UID = 123456789L;
    public static final long UID_WITH_PHONE = 11111111L;
    public static final String YANDEX_UID = "test-yandex-uid";
    @Deprecated
    public static final String PHONE = "+71234567891";
    public static final String PERSONAL_PHONE_ID = "c0dec0dedec0dec0dec0dec0dedec0de";
    public static final String PERSONAL_EMAIL_ID = "9e92bc743c624f958b8876c7841a653b";
    public static final String PERSONAL_FULL_NAME_ID = "a1c595eb35404207aecfa080f90a8986";

    public static Buyer getBuyer() {
        return getDefaultBuyer(UID);
    }

    public static Buyer getSberIdBuyer() {
        return getDefaultBuyer(SBER_ID);
    }

    public static Buyer getBuyerAssessor() {
        Buyer buyer = getDefaultBuyer(ASSESSOR_UID);
        buyer.setAssessor(true);
        return buyer;
    }

    public static Buyer getBuyerWithPhone(String phone) {
        Buyer buyer = getDefaultBuyer(UID_WITH_PHONE);
        buyer.setPhone(phone);
        buyer.setNormalizedPhone(null);
        return buyer;
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
        buyer.setPersonalPhoneId(PERSONAL_PHONE_ID);
        buyer.setPersonalEmailId(PERSONAL_EMAIL_ID);
        buyer.setPersonalFullNameId(PERSONAL_FULL_NAME_ID);
        buyer.setYandexUid(YANDEX_UID);
        return buyer;
    }
}

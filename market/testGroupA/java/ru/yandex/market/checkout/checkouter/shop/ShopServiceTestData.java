package ru.yandex.market.checkout.checkouter.shop;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;

import static ru.yandex.market.checkout.checkouter.shop.PrepayType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.shop.PrepayType.YANDEX_MARKET_AG;

/**
 * Created by poluektov on 02.06.17.
 */
final class ShopServiceTestData {

    static final ShopMetaData NON_MARKET_NULL_INN_AND_PHONE = newTestMetaData(YANDEX_MARKET_AG, null, null);
    private static final String CORRECT_INN = "123456789012";
    static final ShopMetaData NULL_PHONE = newTestMetaData(YANDEX_MARKET, CORRECT_INN, null);
    private static final String CORRECT_PHONE = "+7 (495) 1234567";
    static final ShopMetaData CORRECT = newTestMetaData(YANDEX_MARKET, CORRECT_INN, CORRECT_PHONE);
    static final ShopMetaData LONG_INN = newTestMetaData(YANDEX_MARKET, "12345678901234", CORRECT_PHONE);
    static final ShopMetaData NULL_INN = newTestMetaData(YANDEX_MARKET, null, CORRECT_PHONE);
    static final ShopMetaData WRONG_FORMAT_INN = newTestMetaData(YANDEX_MARKET, "someshit1234", CORRECT_PHONE);

    private ShopServiceTestData() {
    }

    private static ShopMetaData newTestMetaData(PrepayType prepayType, String inn, String phoneNumber) {
        return ShopMetaDataBuilder.createTestDefault()
                .withPrepayType(prepayType)
                .withInn(inn)
                .withPhone(phoneNumber)
                .build();
    }

}

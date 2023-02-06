package ru.yandex.market.checkout.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static ru.yandex.market.checkout.common.DataNormalizeUtil.adjustPhone;
import static ru.yandex.market.checkout.common.DataNormalizeUtil.normalizePhone;


public class DataNormalizeUtilNormalizePhoneTest {

    @ParameterizedTest
    @CsvSource(value = {"+7 916 339 34 55,79163393455", "8-916-339-34-55,89163393455", "adsfkhaskdhfs,''"})
    public void normalizePhoneTest(String phoneOriginal, String phoneNormalized) {
        Assertions.assertEquals(phoneNormalized, normalizePhone(phoneOriginal));
    }

    @ParameterizedTest
    @CsvSource(value = {"+7 925 486-86-80,+79254868680", "+7 916 339 34 55,+79163393455", "8-916-339-34-55," +
            "89163393455", "adsfkhaskdhfs,''"})
    public void adjustPhoneTest(String phoneOriginal, String phoneNormalized) {
        Assertions.assertEquals(phoneNormalized, adjustPhone(phoneOriginal));
    }


}

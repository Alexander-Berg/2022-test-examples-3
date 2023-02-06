package ru.yandex.direct.utils;

import java.math.BigInteger;

import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.HashingUtils.getMd5HalfHashUtf8;
import static ru.yandex.direct.utils.HashingUtils.getMd5HalfMix;
import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsBigInteger;
import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsHexString;

public class HashingUtilsMd5Test {

    @Test
    public void getMd5HashAsHexString_DoesNotCutOneLeftZero() {
        byte[] bytes = new byte[]{97};
        String md5HashWithoutZero = "cc175b9c0f1b6a831c399e269772661";
        String expectedMd5Hash = "0" + md5HashWithoutZero;

        BigInteger md5 = getMd5HashAsBigInteger(bytes);
        checkState(md5.toString(16).equals(md5HashWithoutZero),
                "для проведения теста m5 должен быть равен " + expectedMd5Hash);

        String md5HexStr = getMd5HashAsHexString(bytes);
        assertThat(md5HexStr, is(expectedMd5Hash));
    }

    @Test
    public void getMd5HashAsHexString_DoesNotCutTwoLeftZeroes() {
        byte[] bytes = new byte[]{126, 76};
        String md5HashWithoutZeroes = "e3b779e50d324fedada7be2ec564bd";
        String expectedMd5Hash = "00" + md5HashWithoutZeroes;

        BigInteger md5 = getMd5HashAsBigInteger(bytes);
        checkState(md5.toString(16).equals(md5HashWithoutZeroes),
                "для проведения теста m5 должен быть равен " + expectedMd5Hash);

        String md5HexStr = getMd5HashAsHexString(bytes);
        assertThat(md5HexStr, is(expectedMd5Hash));
    }

    @Test
    public void getMd5HalfHashUtf8_SimpleTest() {
        String source = "mobile_multiplier:11986445:612998406";
        BigInteger md5HalfHashUtf8 = getMd5HalfHashUtf8(source);

        assertThat(md5HalfHashUtf8, is(new BigInteger("9681007499388343634")));
    }

    @Test
    public void getMd5HalfMix_SimpleTest() {
        byte[] source = {120, 95, -69, -121, -20, -128, -104, -126, -104, -25, 33, 88, 125, -92, -43, -115};
        BigInteger halfMix = getMd5HalfMix(source);

        assertThat(halfMix, is(new BigInteger("10458568965251177183")));
    }
}

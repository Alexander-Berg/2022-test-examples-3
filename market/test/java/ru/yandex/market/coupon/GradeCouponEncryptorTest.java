package ru.yandex.market.coupon;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crypto.AesEncoder;

/**
 * @author dinyat
 * 26/06/2017
 */
public class GradeCouponEncryptorTest {

    private final String key = "ac909698d078afbb";

    @Test
    public void testDecryptCouponCodeWhenCouponFormatMatches() {
        String encryptCouponCode = AesEncoder.encrypt(key, "market_coupon=GRADE_COUPON");

        String couponCode = GradeCouponEncryptor.decryptCouponCode(key, encryptCouponCode);

        Assert.assertEquals("GRADE_COUPON", couponCode);
    }

    @Test
    public void testGetCouponCodeWhenCouponFormatNotMatches() {
        String encryptCouponCode = AesEncoder.encrypt(key, "market_couponGRADE_COUPON");

        String couponCode = GradeCouponEncryptor.decryptCouponCode(key, encryptCouponCode);

        Assert.assertNull(couponCode);
    }

}

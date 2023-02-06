package ru.yandex.market.crypto;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author dinyat
 * 26/06/2017
 */
public class AesEncoderTest {

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        String key = "816acdc372103d2b";
        String text = "Hello World";

        String encryptedValue = AesEncoder.encrypt(key, text);

        Assert.assertNotNull(encryptedValue);

        String result = AesEncoder.decrypt(key, encryptedValue);

        Assert.assertEquals(text, result);
    }

}

package ru.yandex.chemodan.app.docviewer.crypt;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class AesCryptTest {

    @Test
    public void cryptDecrypt() {
        String text = "English / русский ; 012345";
        Assert.equals(text, AesCrypt.decrypt(AesCrypt.encrypt(text)));
    }

    @Test
    public void nonTrivialCrypt() {
        Assert.notEquals("abc123", AesCrypt.encrypt("abc123"));
    }

}

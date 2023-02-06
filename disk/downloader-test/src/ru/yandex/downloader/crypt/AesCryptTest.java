package ru.yandex.downloader.crypt;

import org.junit.Test;
import ru.yandex.downloader.TestData;
import ru.yandex.misc.test.Assert;

import java.security.NoSuchAlgorithmException;

/**
 * @author akirakozov
 */
public class AesCryptTest {

    @Test
    public void encrypt() throws NoSuchAlgorithmException {
        AesCrypt crypt = new AesCrypt(ZaberunSecrets.STID_ENCRYPT_SECRET);
        String res = crypt.encryptInBase64(TestData.IMAGE_PNG_STID.toSerializedString());
        Assert.equals("5M-iSLYEfDMZ--tasDOlGcAmWPGfpRVeXkio8wcq0_fcgPsHoM-iaY_Kc8w5xlDh-dOlJzGh5tgpdPU2jVDFAA==", res);
    }
}
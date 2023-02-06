package ru.yandex.chemodan.app.djfs.core;

import org.junit.Test;

import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

public class LegacyMpfsAesTest {
    @Test
    public void testEncryption() {
        LegacyMpfsAes aes = new LegacyMpfsAes("f8dbcb3d33954d62");
        String encryptedData = aes.encrypt("100000.yadisk:128280859.10138334424013437596");

        Assert.equals(
                encryptedData,
                "2fmRXJOMTWELBUB94nzuabz17c04W7A0YWTRjfp0678b_Ym6sLCoUGfmdi-zMb3rdfqpiZuA8oA4ixSocw0Vmg=="
        );
    }

    @Test
    public void testDecryption() {
        LegacyMpfsAes aes = new LegacyMpfsAes("f8dbcb3d33954d623c42373e0f09a349");
        String hash = UrlUtils.urlDecode(
                "UaHDAYXbE6zowdzul7ulM%2Bi9FzF5iIjAix5D5Afntc5%2Bw5tVv%2BG2BX3yl8SL0CLzq%2FJ6bpmRyOJonT3VoXnDag%3D%3D");
        final String decrypted = aes.decrypt(hash);
        Assert.equals(decrypted, "10696992:db24e1c4ce034fb5a5bac016e8a6b021");
    }
}

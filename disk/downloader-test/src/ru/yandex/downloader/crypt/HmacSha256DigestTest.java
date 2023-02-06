package ru.yandex.downloader.crypt;

import org.junit.Test;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class HmacSha256DigestTest {

    @Test
    public void getDigest() throws Exception {
        String message= "Hello, Zaberun!";
        String secret = "ntcvlfj";

        HmacSha256Digest digester = new HmacSha256Digest(secret);
        String result = digester.getDigestInHex(message);
        Assert.equals("2c9312d9e006c1102f233ab9f20724450a21ec9d207b4e150323c12c77eff77f", result);
    }
}
package ru.yandex.downloader.crypt;

import org.apache.commons.codec.binary.Hex;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.lang.CharsetUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author akirakozov
 */
public class HmacSha256Digest {
    private final Mac hmacSha256;

    public HmacSha256Digest(String secret) {
        this.hmacSha256 = createHmacSha256(secret);
    }

    public String getDigestInHex(String message) {
        return Hex.encodeHexString(hmacSha256.doFinal(message.getBytes(CharsetUtils.UTF8_CHARSET)));
    }

    private static Mac createHmacSha256(String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(CharsetUtils.UTF8_CHARSET), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return sha256_HMAC;
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }
}

package ru.yandex.downloader.crypt;

import com.ning.http.util.Base64;
import org.apache.commons.lang.StringUtils;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.lang.CharsetUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author akirakozov
 */
public class AesCrypt {
    private static int DEFAULT_BLOCK_SIZE = 32;
    private static String PADDING_SYMBOL = "{";

    private final Cipher cipher;

    public AesCrypt(String secret) {
        this.cipher = createCipher(secret);
    }

    public String encryptInBase64(String message) {
        try {
            // Base64 with some characters fix: CHEMODAN-8207
            return Base64.encode(cipher.doFinal(addPadding(message).getBytes(CharsetUtils.UTF8_CHARSET)))
                    .replace('+', '-')
                    .replace('/', '_');
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }

    private String addPadding(String message) {
        int numberOfPadChars = DEFAULT_BLOCK_SIZE - message.length() % DEFAULT_BLOCK_SIZE;
        return message + StringUtils.repeat(PADDING_SYMBOL, numberOfPadChars);
    }

    private static Cipher createCipher(String secret) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            byte[] secretBytes = secret.getBytes(CharsetUtils.UTF8_CHARSET);
            SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher;
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }

}

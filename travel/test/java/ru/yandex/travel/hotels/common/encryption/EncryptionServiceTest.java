package ru.yandex.travel.hotels.common.encryption;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EncryptionServiceTest {
    @Test
    public void testEncryptDecrypt() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        EncryptionService service = new EncryptionService("some-encryption-key");
        String input = "some input text";
        byte[] encrypted = service.encrypt(input.getBytes("UTF-8"));
        byte[] decrypted = service.decrypt(encrypted);
        String output = new String(decrypted, "UTF-8");
        assertThat(output).isEqualTo(input);
    }
}

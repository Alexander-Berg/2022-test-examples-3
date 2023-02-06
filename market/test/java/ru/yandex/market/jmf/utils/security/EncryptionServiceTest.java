package ru.yandex.market.jmf.utils.security;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.common.security.impl.EncryptionServiceImpl;

public class EncryptionServiceTest {

    @Test
    public void simple() {
        var encryptionService = new EncryptionServiceImpl();
        String data = "One Day Two Weeks Free Months Four Years";
        Supplier<String> secretSupplier = () -> "fkjHJAHKFhkjnnfndfhskjfsjkdfhdskfa";
        String encrypted = encryptionService.encrypt(data, secretSupplier);
        String decrypted = encryptionService.decrypt(encrypted, secretSupplier);
        Assertions.assertNotEquals(data, encrypted);
        Assertions.assertNotEquals(encrypted, decrypted);
        Assertions.assertEquals(data, decrypted);
    }
}

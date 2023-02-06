package ru.yandex.market.mcrm.utils.security;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncryptionServiceTest {

    @Test
    public void simple() {
        var encryptionService = new EncryptionService();
        String data = "One Day Two Weeks Free Months Four Years";
        Supplier<String> secretSupplier = () -> "fkjHJAHKFhkjnnfndfhskjfsjkdfhdskfa";
        String encrypted = encryptionService.encrypt(data, secretSupplier);
        String decrypted = encryptionService.decrypt(encrypted, secretSupplier);
        Assertions.assertNotEquals(data, encrypted);
        Assertions.assertNotEquals(encrypted, decrypted);
        Assertions.assertEquals(data, decrypted);
    }
}

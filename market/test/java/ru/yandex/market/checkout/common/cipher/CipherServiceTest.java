package ru.yandex.market.checkout.common.cipher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.cipher.CipherException;
import ru.yandex.market.checkout.cipher.CipherService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class CipherServiceTest {

    private static final String EXAMPLE_FEE_SHOW = "8-qH2tqoDtKE6aHsEk-igaBtOPE4mzE_FNMCN4UY6cJ0gY0xbnPQEhGVS1" +
            "-4eEnCLJC0xqH3x77lmGA6N9tjFqM-r7e7jPVoFqmWrhOBz7m9vULGKQ6G7Q,,";
    private static final String DECIPHERED_FEE = "0.0200";
    private static final String DECIPHERED_FEE_SUM = "3.97";
    private static final String CIPHER_KEY = "ayFVMGPqmKf4pZ0rnsGMGQ==\n";
    private static final CipherService SERVICE = new CipherService("Blowfish", CIPHER_KEY,
            "CBC/NoPadding", "arcadia+");

    @Test
    public void decipherShowInfo() {
        byte[] res = SERVICE.decipher(EXAMPLE_FEE_SHOW);
        String resStr = new String(res);
        assertThat(resStr, containsString(DECIPHERED_FEE));
        assertThat(resStr, containsString(DECIPHERED_FEE_SUM));
    }

    @Test
    public void decipherWrongShowInfo() {
        Assertions.assertThrows(CipherException.class, () -> {
            SERVICE.decipher("yoyoyo");
        });
    }
}

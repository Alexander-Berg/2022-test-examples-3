package ru.yandex.market.checkout.checkouter.service.binding;

import java.security.SecureRandom;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class BindKeyGeneratorImplTest {

    private static final int EXPECTED_RAND_LENGTH = 32;
    private BindKeyGeneratorImpl bindKeyGenerator;

    @BeforeEach
    public void setUp() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(0xDEADBEAFL);
        secureRandom.setSeed(new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xAF});

        bindKeyGenerator = new BindKeyGeneratorImpl(secureRandom);
    }

    @Test
    public void shouldGenerateKey() {
        long orderId = 123L;
        String bindKey = bindKeyGenerator.generateBindKey(orderId);
        assertThat(bindKey, CoreMatchers.startsWith("123."));
        Assertions.assertEquals("123.".length() + EXPECTED_RAND_LENGTH, bindKey.length());
    }
}

package ru.yandex.market.loyalty.core.service.generator;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by maratik.
 */
public class RandomCodeGeneratorTest {
    private static final int GENERATED_LENGTH = 100;

    @Test
    public void testRandomCodeGenerator() {
        RandomCodeGenerator codeGenerator = new RandomCodeGenerator(
                "1239A",
                GENERATED_LENGTH,
                new SecureRandom()
        );

        String generated = codeGenerator.generate().orElseThrow(() -> new AssertionError("Code is not generated"));
        assertEquals(GENERATED_LENGTH, generated.length());
        assertFalse(Pattern.compile("[^1239A]").matcher(generated).find());
    }
}

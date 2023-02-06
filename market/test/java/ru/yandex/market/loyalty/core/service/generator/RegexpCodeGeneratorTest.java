package ru.yandex.market.loyalty.core.service.generator;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by maratik.
 */
public class RegexpCodeGeneratorTest {
    private RegexpCodeGenerator codeGenerator;

    @Before
    public void init() {
        codeGenerator = new RegexpCodeGenerator(
                new CallbackCodeGenerator(() -> Optional.of("1234567890"))
        );
        codeGenerator.setPattern(Pattern.compile("(.{5})(.{5})"));
        codeGenerator.setReplacement("$1-$2");
    }

    @Test
    public void testRegexpCodeGenerator() throws GeneratorException {
        Optional<String> generated = codeGenerator.generate();
        assertTrue(generated.isPresent());
        assertEquals("12345-67890", generated.get());
    }
}

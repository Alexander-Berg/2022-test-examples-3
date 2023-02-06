package ru.yandex.market.abo.gen.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.model.GeneratorProfile;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         04.06.15
 */
public class ProblemSecondaryCheckGeneratorTest extends EmptyTest {
    @Autowired
    ProblemSecondaryCheckGenerator problemSecondaryCheckHypothesisGenerator;

    @Test
    public void testGenerateNew() throws Exception {
        problemSecondaryCheckHypothesisGenerator.configure(new GeneratorProfile(91, 100, "sdsd"));
        problemSecondaryCheckHypothesisGenerator.generate().forEach(System.out::println);
    }
}

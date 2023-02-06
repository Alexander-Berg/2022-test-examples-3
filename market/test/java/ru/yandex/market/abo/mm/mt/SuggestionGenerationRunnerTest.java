package ru.yandex.market.abo.mm.mt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 07.11.2008
 */
public class SuggestionGenerationRunnerTest extends EmptyTest {

    @Autowired
    private SuggestionGenerationRunner suggestionGenerationRunner;

    @Test
    public void testRun() {
        suggestionGenerationRunner.startSuggestionGeneration();
    }
}

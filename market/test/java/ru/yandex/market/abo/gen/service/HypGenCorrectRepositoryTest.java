package ru.yandex.market.abo.gen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.model.HypGenCorrect;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 30.04.18
 */
public class HypGenCorrectRepositoryTest extends EmptyTest {

    @Autowired
    HypGenCorrectRepository hypGenCorrectRepository;

    @Test
    public void testRepo() {
        HypGenCorrect hypGenCorrect = initHypGenCorrect();
        hypGenCorrectRepository.save(hypGenCorrect);
        HypGenCorrect dbHypGenCorrect = hypGenCorrectRepository.findByIdOrNull(hypGenCorrect.getGenId());
        assertEquals(hypGenCorrect, dbHypGenCorrect);
    }

    @Test
    public void updateKillPercentTest() {
        HypGenCorrect hypGenCorrect = initHypGenCorrect();
        hypGenCorrectRepository.save(hypGenCorrect);
        double killPercent = 13.02;
        hypGenCorrectRepository.updateKillPercent(hypGenCorrect.getGenId(), killPercent);
        assertEquals(killPercent, hypGenCorrectRepository.findByIdOrNull(hypGenCorrect.getGenId()).getKillPercent());
    }

    @Test
    public void updateCoefficientTest() {
        HypGenCorrect hypGenCorrect = initHypGenCorrect();
        hypGenCorrectRepository.save(hypGenCorrect);
        double coefficient = 7.08;
        hypGenCorrectRepository.updateCoefficient(hypGenCorrect.getGenId(), coefficient);
        assertEquals(coefficient, hypGenCorrectRepository.findByIdOrNull(hypGenCorrect.getGenId()).getCoefficient());
    }

    private HypGenCorrect initHypGenCorrect() {
        return new HypGenCorrect(12, true, 5.702, 9.04);
    }
}

package ru.yandex.market.abo.clch;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.clch.model.ShopSameness;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 19.11.17.
 */
public class CheckerAnalyzerDbTest extends ClchTest {
    @Autowired
    private CheckerAnalyzer checkerAnalyzer;

    @Test
    public void calculateSameness() throws Exception {
        assertNotNull(checkerAnalyzer.calculateShopSameness(RND.nextLong()));
    }

    @Test
    public void savePairSameness() throws Exception {
        checkerAnalyzer.savePairSameness(RND.nextLong(), initRandomSameness());
    }

    @Test
    public void saveFavourite() throws Exception {
        checkerAnalyzer.saveFavourite(RND.nextLong(), initRandomSameness());
    }

    @Test
    public void saveSessionSameness() throws Exception {
        checkerAnalyzer.saveSessionSameness(RND.nextLong(), initRandomSameness());
    }

    private static Set<ShopSameness> initRandomSameness() {
        return IntStream.range(0, 10)
                .mapToObj(i -> new ShopSameness(i, i + 1))
                .peek(s -> s.setSameness(RND.nextDouble())).collect(Collectors.toSet());
    }
}
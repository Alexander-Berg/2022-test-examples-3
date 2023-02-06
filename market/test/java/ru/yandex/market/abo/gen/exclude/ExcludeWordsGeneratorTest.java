package ru.yandex.market.abo.gen.exclude;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         11/22/13
 */
@Transactional("pgTransactionManager")
public class ExcludeWordsGeneratorTest extends EmptyTest {

    @Autowired
    private ExcludeWordsGenerator excludeWordsGenerator;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    private String[] words = new String[]{"сеалекс", "машина", "вино", "ошейник", "электрошок", "водка"};

    @Test
    public void testGenerate() {
        Arrays.stream(words).forEach(word -> pgJdbcTemplate.update("INSERT INTO exclude_offer(word) VALUES(?)", word));
        excludeWordsGenerator.configure(new GeneratorProfile(65, 15, ""));
        List<Hypothesis> hyps = excludeWordsGenerator.generate();
        assertEquals(words.length, hyps.size());
    }

    @Test
    public void testGenerateForWordOnlyUniqueShops() {
        for (String word : words) {
            excludeWordsGenerator.configure(new GeneratorProfile(65, 15, ""));

            Set<Long> shopIds = new HashSet<>();
            final List<Hypothesis> hips = excludeWordsGenerator.generateForWord(0, word);

            for (Hypothesis h : hips) {
                long shopID = h.getShopId();
                assertFalse(shopIds.contains(shopID));
                shopIds.add(shopID);
            }
        }
    }
}

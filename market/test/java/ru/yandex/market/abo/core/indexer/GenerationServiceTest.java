package ru.yandex.market.abo.core.indexer;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author imelnikov
 */
public class GenerationServiceTest extends EmptyTest {

    @Autowired
    private GenerationService generationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void store() {
        Generation gen = createGeneration(1, true);

        generationService.storeGeneration(gen);
        Generation loaded1 = generationService.loadGeneration(1);
        assertNotNull(loaded1);

        long originalId = loadOriginalId(loaded1.getId());
        assertEquals(loaded1.getOriginalId(), originalId);
    }

    @Test
    public void lastGeneration() {
        long originalId = 1;

        generationService.storeGeneration(createGeneration(originalId, true));
        generationService.storeGeneration(createGeneration(originalId + 1, true));
        generationService.storeGeneration(createGeneration(originalId + 2, false));

        assertEquals(generationService.loadLastReleaseGeneration().getOriginalId(), originalId + 1);
        assertEquals(generationService.loadPrevReleaseGeneration().getOriginalId(), originalId);
    }

    private long loadOriginalId(long aboGenId) {
        return jdbcTemplate.queryForObject("SELECT orig_id FROM idx_generation WHERE id = ?", Long.class, aboGenId);
    }

    public static Generation createGeneration(long originalId, boolean released) {
        Generation generation = new Generation();
        generation.setOriginalId(originalId);
        generation.setName("name");
        generation.setHost("host");
        generation.setReleased(released);
        generation.setStartDate(new Date());
        generation.setReleaseDate(released ? new Date() : null);
        return generation;
    }
}

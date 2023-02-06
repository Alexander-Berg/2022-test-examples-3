package ru.yandex.market.deliverycalculator.storage.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

class GenerationRepositoryTest extends FunctionalTest {

    @Autowired
    private GenerationRepository tested;

    /**
     * Тест для {@link GenerationRepository#findByIdGreaterThanEqualOrderById(Long, Pageable)}.
     */
    @DbUnitDataSet(before = "database/searchGenerations.before.csv")
    @Test
    void testFindByIdGreaterThanEqualOrderById() {
        List<Generation> generations = tested.findByIdGreaterThanEqualOrderById(2L, PageRequest.of(0, 3));

        assertNotNull(generations);
        assertEquals(3, generations.size());
        assertEquals(Long.valueOf(2L), generations.get(0).getId());
        assertFalse(CollectionUtils.isEmpty(generations.get(0).getDaasPickupGenerations()));
        assertEquals(Long.valueOf(3L), generations.get(1).getId());
        assertFalse(CollectionUtils.isEmpty(generations.get(1).getMardoCourierGenerations()));
        assertEquals(Long.valueOf(4L), generations.get(2).getId());
        assertFalse(CollectionUtils.isEmpty(generations.get(2).getMardoWhitePickupGenerations()));

        generations = tested.findByIdGreaterThanEqualOrderById(2L, PageRequest.of(1, 3));

        assertNotNull(generations);
        assertEquals(1, generations.size());
        assertEquals(Long.valueOf(5L), generations.get(0).getId());

        generations = tested.findByIdGreaterThanEqualOrderById(2L, PageRequest.of(2, 3));
        assertNotNull(generations);
        assertTrue(CollectionUtils.isEmpty(generations));
    }

    /**
     * Тест для {@link GenerationRepository#findMinExternalGenerationId()}.
     * Случай: БД содержит некоторое количесвто поколений
     */
    @DbUnitDataSet(before = "database/searchGenerations.before.csv")
    @Test
    void testFindMinExternalGenerationId_databaseNotEmpty() {
        Optional<Long> actual = tested.findMinExternalGenerationId();

        assertTrue(actual.isPresent());
        assertEquals(Long.valueOf(1), actual.get());
    }

    /**
     * Тест для {@link GenerationRepository#findMinExternalGenerationId()}.
     * Случай: БД пустая.
     */
    @Test
    @DbUnitDataSet
    void testFindMinExternalGenerationId_databaseEmpty() {
        assertFalse(tested.findMinExternalGenerationId().isPresent());
    }

    /**
     * Тест для {@link GenerationRepository#findMaxExternalGenerationIdEarlierThan(Instant)}.
     * Случай: БД содержит данные.
     */
    @DbUnitDataSet(before = "database/searchGenerations.before.csv")
    @Test
    void testFindMaxExternalGenerationIdEarlierThan_databaseNotEmpty() {
        Optional<Long> actual = tested.findMaxExternalGenerationIdEarlierThan(
                LocalDateTime.of(2019, 8, 1, 14, 40, 3)
                        .toInstant(OffsetDateTime.now(ZoneId.systemDefault()).getOffset()));

        assertTrue(actual.isPresent());
        assertEquals(Long.valueOf(3), actual.get());
    }
}

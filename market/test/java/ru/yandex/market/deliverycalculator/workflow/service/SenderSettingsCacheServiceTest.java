package ru.yandex.market.deliverycalculator.workflow.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link SenderSettingsCacheService}.
 */
class SenderSettingsCacheServiceTest extends FunctionalTest {

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private SenderSettingsCacheService tested;

    /**
     * Проверяет корректную работу кэша настроек сендера.
     */
    @Test
    @DbUnitDataSet(before = "database/putSenderSettingsToCache.before.csv")
    void testCacheBehaviour() {
        /*
        Подгружаем в кэш следующие данные:
        настройки сендера с идентификатором 1 с внешними поколениеями 1,2,3
        настройки сендера с идентификатором 2 с внешними поколениями 2,4
        настройки сендера с идентификатором 3 с внешними поколением 1
         */
        tested.loadToCache(generationRepository.findById(1L).orElseThrow());
        tested.loadToCache(generationRepository.findById(2L).orElseThrow());
        tested.loadToCache(generationRepository.findById(3L).orElseThrow());
        tested.loadToCache(generationRepository.findById(4L).orElseThrow());

        /*
        Проверяем, что данные нормально положились в кэш
         */
        assertTrue(tested.getCacheValue(1L, 2L).isPresent());
        assertTrue(tested.getCacheValue(1L, 3L).isPresent());
        assertTrue(tested.getCacheValue(1L, 4L).isPresent());
        assertTrue(tested.getCacheValue(1L, 1L).isPresent());
        assertFalse(tested.getCacheValue(1L, 0L).isPresent());

        assertTrue(tested.getCacheValue(2L, 2L).isPresent());
        assertTrue(tested.getCacheValue(2L, 3L).isPresent());
        assertTrue(tested.getCacheValue(2L, 4L).isPresent());
        assertTrue(tested.getCacheValue(2L, 5L).isPresent());
        assertFalse(tested.getCacheValue(2L, 1L).isPresent());

        assertTrue(tested.getCacheValue(3L, 1L).isPresent());
        assertTrue(tested.getCacheValue(3L, 2L).isPresent());

        /*
        Удаляем поколения из кэша.
        Удаляем все поколени.
        Ожидаемый результат:
        настройки сендера с идентификатором 1 с внешними поколениеями 2,3
        настройки сендера с идентификатором 2 с внешними поколениями 2,4
        настройки сендера с идентификатором 3 с внешними поколением 1
         */
        tested.outdateGenerationsBefore(2L);

        /*
        Проверяем, что все корректно удалилось.
         */
        assertFalse(tested.getCacheValue(1L, 1L).isPresent());
        assertTrue(tested.getCacheValue(1L, 2L).isPresent());
        assertTrue(tested.getCacheValue(1L, 3L).isPresent());
        assertTrue(tested.getCacheValue(1L, 4L).isPresent());

        assertFalse(tested.getCacheValue(2L, 1L).isPresent());
        assertTrue(tested.getCacheValue(2L, 2L).isPresent());
        assertTrue(tested.getCacheValue(2L, 3L).isPresent());
        assertTrue(tested.getCacheValue(2L, 4L).isPresent());

        assertTrue(tested.getCacheValue(3L, 1L).isPresent());
    }

}

package ru.yandex.market.core.indexer.db.generation;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.indexer.model.GenerationInfo;

/**
 * Тесты для {@link DbGenerationService}.
 *
 * @author Vadim Lyalin
 */
class DbGenerationServiceTest extends FunctionalTest {
    @Autowired
    private DbGenerationService generationService;

    @Test
    @DbUnitDataSet(before = "DbGenerationServiceTest.getFeedLog.before.csv")
    void testGetLastFullGeneration() {
        GenerationInfo expectedGeneration = new GenerationInfo(1, "1-1",
                Date.valueOf(LocalDate.of(2017, Month.JANUARY, 1)),
                Date.valueOf(LocalDate.of(2017, Month.FEBRUARY, 2)),
                Date.valueOf(LocalDate.of(2017, Month.MARCH, 3)));
        expectedGeneration.setScVersion("1.0");
        GenerationInfo generation = generationService.getLastFullGeneration();
        ReflectionAssert.assertReflectionEquals(expectedGeneration, generation);
    }

    @Test
    @DbUnitDataSet(after = "DbGenerationServiceTest.saveGeneration.after.csv")
    void testSaveGeneration() {
        GenerationInfo generation = new GenerationInfo(1, "1-1",
                Date.valueOf(LocalDate.of(2017, Month.JANUARY, 11)),
                Date.valueOf(LocalDate.of(2017, Month.FEBRUARY, 12)),
                Date.valueOf(LocalDate.of(2017, Month.MARCH, 13)));
        generation.setScVersion("1.0");

        var service = Mockito.spy(generationService);
        Mockito.doNothing().when(service).cleanGeneration(ArgumentMatchers.anyLong());
        service.saveGeneration(generation);
    }
}

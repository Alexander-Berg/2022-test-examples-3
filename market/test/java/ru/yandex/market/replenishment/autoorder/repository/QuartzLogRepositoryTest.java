package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.QuartzLogRepository;
public class QuartzLogRepositoryTest extends FunctionalTest {

    @Autowired
    private QuartzLogRepository quartzLogRepository;

    @Test
    @DbUnitDataSet(before = "QuartzLogRepositoryTest.before.csv")
    public void testFind() {
        final List<String> result = quartzLogRepository.getFailedImportStages(
                LocalDateTime.of(2021, 10, 14, 14, 30, 0));
        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(
                result.containsAll(Set.of("demandsProcessorExecutor", "recommendationCountryInfo1pExecutor")));
    }

}

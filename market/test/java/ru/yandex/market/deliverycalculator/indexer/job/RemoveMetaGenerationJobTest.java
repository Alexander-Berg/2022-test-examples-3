package ru.yandex.market.deliverycalculator.indexer.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;

class RemoveMetaGenerationJobTest extends FunctionalTest {

    @Autowired
    private RemoveMetaGenerationJob removeMetaGenerationJob;

    @Test
    @DbUnitDataSet(before = "cleanUpMetaGenerationsTest.before.csv", after = "cleanUpMetaGenerationsTest.after.csv")
    void cleanUpMetaGenerationsTest() {
        removeMetaGenerationJob.doJob(null);
    }
}

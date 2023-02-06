package ru.yandex.cs.placement.tms.mbo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

public class LoadMboCategoriesExecutorTest extends AbstractCsPlacementTmsFunctionalTest {

    private final LoadMboCategoriesExecutor loadMboCategoriesExecutor;

    @Autowired
    public LoadMboCategoriesExecutorTest(LoadMboCategoriesExecutor loadMboCategoriesExecutor) {
        this.loadMboCategoriesExecutor = loadMboCategoriesExecutor;
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/mbo/LoadMboCategoriesExecutor/testLoad/before.csv",
            after = "/ru/yandex/cs/placement/tms/mbo/LoadMboCategoriesExecutor/testLoad/after.csv"
    )
    void testLoad() {
        loadMboCategoriesExecutor.doJob(null);
    }
}

package ru.yandex.market.vendors.analytics.tms.jobs.testing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * @author ogonek.
 */
@DbUnitDataSet(before = "UpdateTestingPartnerCategories.before.csv")
public class UpdateTestingPartnerCategoriesExecutorTest extends FunctionalTest {

    @Autowired
    private UpdateTestingPartnerCategoriesExecutor updateTestingPartnerCategoriesExecutor;

    @Test
    @DbUnitDataSet(after = "UpdateTestingPartnerCategories.after.csv")
    void updateCategoriesTest() {
        updateTestingPartnerCategoriesExecutor.doJob(null);
    }
}

package ru.yandex.autotests.market.billing.backend.getClicksExecutor;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 10/31/14
 */

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.modelmapper.PropertyMap;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.stat.PlogClick;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.ClicksGenerationSteps;
import ru.yandex.autotests.market.stat.beans.hive.clicks.Click;
import ru.yandex.autotests.market.stat.beans.tables.MstGetterTable;
import ru.yandex.qatools.hazelcast.LockRule;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

abstract public class AbstractClicksCaptureTest {

    @ClassRule
    public static LockRule lockRule = new LockRule("Lock by campaign id 243245");

    @Parameterized.Parameter
    public static MstGetterTable mstGetterTable;

    @Parameterized.Parameter(1)
    public static PropertyMap<PlogClick, Click> transMapping;

    @Parameterized.Parameter(2)
    public static PropertyMap<Click, Click> cisMapping;

    private BillingDaoSteps billingDaoSteps = BillingDaoSteps.getInstance();
    private ClicksGenerationSteps generationSteps = ClicksGenerationSteps.getInstance();

    @Test
    public void complianceTest() throws IOException {
        List<Click> clicksBefore = generationSteps.generateLbClicks(mstGetterTable, 10, 0, 243245L, "10", cisMapping);
        Collection<Long> transIds = generationSteps.waitForLbClicksCapture(mstGetterTable, clicksBefore);
        List<Click> clicksAfter = billingDaoSteps.getClicksByTransIds(mstGetterTable, transIds, transMapping);
        assertThat(clicksAfter, containsInAnyOrder(clicksBefore.toArray()));
    }
}

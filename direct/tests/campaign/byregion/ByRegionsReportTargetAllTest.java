package ru.yandex.autotests.direct.tests.campaign.byregion;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.direct.utils.model.PerlBoolean.ONE;

@Aqua.Test
@Title("Тест статистики по регионам всего xls")
@Features(TestFeatures.BY_REGIONS_REPORT)
public class ByRegionsReportTargetAllTest extends ByRegionsReportTestBase {
    @Test
    @Title("Тест статистики по регионам всего xls")
    public void byRegionsReportTargetAllTest() {
        checkStatByRegionReportXls();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest().withTargetAll(ONE);
    }
}

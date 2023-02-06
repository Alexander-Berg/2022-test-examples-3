package ru.yandex.autotests.direct.tests.campaign.bysite;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Title("Тест статистики по площадкам дефолтный xls")
@Features(TestFeatures.BY_SITE_REPORT)
public class BySiteReportDefaultXlsTest extends BySiteReportXlsTest {

    @Test
    @Title("Тест статистики по площадкам дефолтный xls")
    public void bySiteReportDefaultXlsTest() {
        checkStatBySiteReportXls();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest();
    }
}

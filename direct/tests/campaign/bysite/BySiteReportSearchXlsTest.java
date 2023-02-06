package ru.yandex.autotests.direct.tests.campaign.bysite;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.TargetTypeEnum;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Title("Тест статистики по площадкам на поиске xls")
@Features(TestFeatures.BY_SITE_REPORT)
public class BySiteReportSearchXlsTest extends BySiteReportXlsTest {

    @Test
    @Title("Тест статистики по площадкам на поиске xls")
    public void bySiteReportSearchXlsTest() {
        checkStatBySiteReportXls();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest().withTargetType(TargetTypeEnum.SEARCH);
    }
}

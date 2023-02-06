package ru.yandex.autotests.direct.tests.campaign.common;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Title("Тест Общей статистики дефолтный xls")
@Features(TestFeatures.COMMON_REPORT)
public class CommonReportXlsDefaultTest extends CommonReportXlsTestBase {

    @Test
    @Title("Тест общей статистики дефолтный xls")
    public void commonReportXlsDefaultTest() {
        commonReportXlsTest();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest();
    }
}

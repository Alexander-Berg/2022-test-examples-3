package ru.yandex.autotests.direct.tests.campaign.common;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.direct.utils.model.PerlBoolean.ONE;

@Aqua.Test
@Title("Тест Общей статистики с НДС xls")
@Features(TestFeatures.COMMON_REPORT)
public class CommonReportXlsWithNdsTest extends CommonReportXlsTestBase {

    @Test
    @Title("Тест общей статистики с НДС xls")
    public void commonReportXlsWithNdsTest() {
        commonReportXlsTest();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest().withNds(ONE);
    }
}

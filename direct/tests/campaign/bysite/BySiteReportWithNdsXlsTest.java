package ru.yandex.autotests.direct.tests.campaign.bysite;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.direct.utils.model.PerlBoolean.ONE;

@Aqua.Test
@Title("Тест статистики по площадкам с НДС xls")
@Features(TestFeatures.BY_SITE_REPORT)
public class BySiteReportWithNdsXlsTest extends BySiteReportXlsTest {

    @Test
    @Title("Тест статистики по площадкам с НДС xls")
    public void bySiteReportWithNdsXlsTest() {
        checkStatBySiteReportXls();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest().withNds(ONE).withUsePageId(ONE);
    }
}

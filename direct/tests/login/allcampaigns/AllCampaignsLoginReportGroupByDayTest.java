package ru.yandex.autotests.direct.tests.login.allcampaigns;


import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatGroupEnum;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.direct.utils.model.PerlBoolean.ONE;

@Aqua.Test
@Title("Тест статистики по всем кампаниям на логин по дням")
@Features(TestFeatures.ALL_CAMPAIGNS_REPORT)
public class AllCampaignsLoginReportGroupByDayTest extends AllCampaignsLoginReportTestBase {

    @Test
    @Title("Тест статистики по всем кампаниям на логин по дням")
    public void allCampaignsLoginReportGroupByDayXlsTest() {
        allCampaignsLoginReportXlsTest();
    }

    @Override
    protected ShowCampStatRequest getRequest() {
        return new ShowCampStatRequest().withGroup(StatGroupEnum.DAY).withTargetAll(ONE);
    }
}

package ru.yandex.autotests.direct.cmd.groups;

import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public abstract class CopyGroupStatusModerateBaseTest {
    protected static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannerRule;

    @Description("копируем группу")
    public void copyGroupTest() {
        copyGroup();
        checkStatus();
    }

    protected void checkStatus() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannerRule.getCampaignId().toString());
        Long copiedBannerId = showCamp.getGroups().get(1).getBid();
        Banner actualBanner = cmdRule.cmdSteps().groupsSteps().getBanner(CLIENT, bannerRule.getCampaignId(), copiedBannerId);
        BannersRecord actualBannerBd = TestEnvironment.newDbSteps().bannersSteps().getBanner(copiedBannerId);

        assumeThat("статус модерации баннера в бд соответсвует ожиданиям",
                actualBannerBd.getStatusmoderate(),
                beanDiffer(BannersStatusmoderate.New));

        assertThat(
                "статус модерации баннера соответствует ожиданиям",
                actualBanner.getStatusModerate(),
                beanDiffer(BannersPerformanceStatusmoderate.New.toString())
        );
    }

    protected abstract void copyGroup();
}

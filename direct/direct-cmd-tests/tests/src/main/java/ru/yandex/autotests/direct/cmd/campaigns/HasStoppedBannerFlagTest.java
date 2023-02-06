package ru.yandex.autotests.direct.cmd.campaigns;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("hasStopped не установлен, если нет остановленных баннеров")
@Stories(TestFeatures.Banners.BANNER_FLAGS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.SHOW_CAMP)
public class HasStoppedBannerFlagTest {

    public static final String ULOGIN = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule()
            .withUlogin(ULOGIN);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannersRule);

    @Test
    @Ignore("https://st.yandex-team.ru/DIRECT-52682") //когда доделают фронт выключить
    @ru.yandex.qatools.allure.annotations.TestCaseId("9372")
    public void test() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ULOGIN, bannersRule.getCampaignId().toString());

        assumeThat("В кампании 1на группа", response.getGroups(), hasSize(1));

        String hasStoppedFlag = response.getGroups().get(0).getHasStoppedBanner();

        assumeThat("hasStopped не установлен, если нет остановленных баннеров", hasStoppedFlag, equalTo(null));

        Long bannerId = response.getGroups().get(0).getBid();

        cmdRule.darkSideSteps().getBannersFakeSteps().makeBannersStopped(bannerId);

        response = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(ULOGIN, bannersRule.getCampaignId().toString());

        assumeThat("В кампании 1на группа", response.getGroups(), hasSize(1));

        hasStoppedFlag = response.getGroups().get(0).getHasStoppedBanner();

        assertThat("hasStopped не установлен, если нет остановленных баннеров", hasStoppedFlag, equalTo("Yes"));

    }
}

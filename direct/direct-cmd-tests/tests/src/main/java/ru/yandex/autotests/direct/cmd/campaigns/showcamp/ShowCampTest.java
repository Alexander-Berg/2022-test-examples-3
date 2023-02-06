package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
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
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Просмотр ставок баннера в текстовых кампаниях")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class ShowCampTest {
    private final static String CLIENT = Logins.DEFAULT_CLIENT;
    public BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Проверяем настройки кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10827")
    public void showCamp() {
        ShowCampResponse actualShowCamp =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, bannersRule.getCampaignId().toString());
        assumeThat("id кампании соответсвует ожиданиям",
                actualShowCamp.getCampaignID(),
                equalTo(bannersRule.getCampaignId().toString())
        );
        assumeThat("у кампании есть группы", actualShowCamp.getGroups(), notNullValue());
        assertThat("число групп соответсвует ожиданиям", actualShowCamp.getGroups(), hasSize(1));
    }


}

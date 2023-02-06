package ru.yandex.autotests.direct.cmd.campaigns;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.collection.IsMapContaining.hasKey;
import static ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampRequest.createDefaultRequestForOneGroup;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Показ одной группы баннеров в кампании")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class ShowCampShowAdgroupsTest {
    private final static String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Открываем кампанию с выбранной одной группой баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9375")
    public void showCamp() {
        ShowCampRequest showCampRequest =
                createDefaultRequestForOneGroup(CLIENT, bannersRule.getGroupId(), bannersRule.getCampaignId());
        assertThat("Показываемая группа соответсвует ожиданиям",
                bannersRule.getDirectCmdSteps().campaignSteps().getShowCamp(showCampRequest).getShowGroups(),
                hasKey(bannersRule.getGroupId()));
    }

}

package ru.yandex.autotests.direct.cmd.adjustment;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.GroupMultiplierStats;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка отображения диапазона корректировок баннера при наличии кор-вок в кампании (контроллер showCamp)")
@Stories(TestFeatures.Campaigns.SHOW_ADJUSTMENTS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Features(TestFeatures.CAMPAIGNS)
public class GroupPercentRangeExistCampAdjustmentTest extends PercentRangeTestBase {
    private final static String CLIENT = "at-direct-adjustment-ret";

    private final static String CAMPAIGN_MOBILE_PCT = "196";
    private final static String CAMPAIGN_DEMOGRAPHY_PCT = "197";
    private final static String CAMPAIGN_RETARGETING_PCT = "198";

    private final static String GROUP_MOBILE_PCT = "300";
    private final static String GROUP_DEMOGRAPHY_PCT = "301";
    private final static String GROUP_RETARGETING_PCT = "302";

    private final static String CAMPAIGN_LOWER_BOUND = "196";
    private final static String CAMPAIGN_UPPER_BOUND = "765";

    private final static String EXPECTED_LOWER_BOUND = "300";
    private final static String EXPECTED_UPPER_BOUND = "2727";

    @Override
    protected String getLogin() {
        return CLIENT;
    }

    @Test
    @Description("Проверяем отображение диапазона корректировок ставок группы вместо корректировок кампании" +
            " (корректировки кампании: 96%, 97%, 98%; корректировки группы: 200%, 201%, 202%)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("8950")
    public void checkShowAdjustmentBoundExistCampAdjAfterSaveTextAdGroups() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest().
                withCid(campaignId).
                withHierarhicalMultipliers(getHierarchicalMultipliers(CAMPAIGN_MOBILE_PCT,
                        CAMPAIGN_DEMOGRAPHY_PCT,
                        CAMPAIGN_RETARGETING_PCT)).
                withUlogin(CLIENT);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        assumeThat("диапазон отображается", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, campaignId).getGroups().stream()
                .filter(t -> t.getBid().equals(bannersRule.getBannerId()))
                .findFirst().get().getGroupMultiplierStats(), beanDiffer(new GroupMultiplierStats()
                .withAdjustmentsLowerBound(CAMPAIGN_LOWER_BOUND)
                .withAdjustmentsUpperBound(CAMPAIGN_UPPER_BOUND)));

        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(
                CLIENT, Long.valueOf(campaignId), getGroup(GROUP_MOBILE_PCT, GROUP_DEMOGRAPHY_PCT, GROUP_RETARGETING_PCT));

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        ShowCampResponse showCampResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);

        assertThat("Верхняя планка корректировок соответствует ожидаемой",
                showCampResponse.getGroups().stream()
                        .filter(t -> t.getBid().equals(bannersRule.getBannerId()))
                        .findFirst().get().getGroupMultiplierStats(),
                beanDiffer(getExpectedGroupMultiplierStats(EXPECTED_LOWER_BOUND, EXPECTED_UPPER_BOUND)));

    }
}

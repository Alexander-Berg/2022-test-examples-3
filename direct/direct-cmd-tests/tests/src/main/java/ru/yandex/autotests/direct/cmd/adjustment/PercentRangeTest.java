package ru.yandex.autotests.direct.cmd.adjustment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
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

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка отображения диапазона корректировок (контроллер showCamp)")
@Stories(TestFeatures.Campaigns.SHOW_ADJUSTMENTS)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class PercentRangeTest extends PercentRangeTestBase {
    private final static String CLIENT = "at-direct-adjustment-ret14";

    @Parameterized.Parameter(value = 0)
    public String mobileMultiplierPct;
    @Parameterized.Parameter(value = 1)
    public String demographyMultiplierPct;
    @Parameterized.Parameter(value = 2)
    public String retargetingMultiplierPct;

    @Parameterized.Parameter(value = 3)
    public String expectedLowerBound;
    @Parameterized.Parameter(value = 4)
    public String expectedUpperBound;

    @Parameterized.Parameters(name = "Данные: mobileMultiplierPct = {0}, demographyMultiplierPct = {1}," +
            " retargetingMultiplierPct = {2}. Ожидаемая нижняя граница = {3}, верхняя граница = {4}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"50", "400", "400", "50", "1600"},
                {"1300", "400", "400", "400", "20800"},
                {"100", "100", "100", null, null},
                {"50", "100", "100", null, "50"},
                {"50", null, null, null, "50"},
                {null, "150", null, null, "150"},
                {null, null, "150", null, "150"}
        });
    }

    @Override
    protected String getLogin() {
        return CLIENT;
    }

    @Test
    @Description("Проверяем отображение диапазона ставок при наличии корректировок: мобильные, демография, ретагретинг " +
            " в кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("8951")
    public void checkShowAdjustmentBoundAfterSaveCamp() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest().
                withCid(campaignId).
                withHierarhicalMultipliers(getHierarchicalMultipliers(
                        mobileMultiplierPct, demographyMultiplierPct, retargetingMultiplierPct));

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        ShowCampResponse showCampResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);

        assertThat("диапазон корректировок соответствует ожидаемому",
                showCampResponse.getGroups().stream()
                        .filter(t -> t.getBid().equals(bannersRule.getBannerId()))
                        .findFirst().get().getGroupMultiplierStats(),
                beanDiffer(getExpectedGroupMultiplierStats(expectedLowerBound, expectedUpperBound)));

    }

    @Test
    @Description("Проверяем отображение диапазона корректировок ставок " +
            "при сохранении контроллером saveTextAdGroups")
    @ru.yandex.qatools.allure.annotations.TestCaseId("8952")
    public void checkShowAdjustmentBoundAfterSaveTextAdGroups() {
        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(
                CLIENT, Long.valueOf(campaignId),
                getGroup(mobileMultiplierPct, demographyMultiplierPct, retargetingMultiplierPct));

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        ShowCampResponse showCampResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);

        assertThat("диапазон корректировок соответствует ожидаемому",
                showCampResponse.getGroups().stream()
                        .filter(t -> t.getBid().equals(bannersRule.getBannerId()))
                        .findFirst().get().getGroupMultiplierStats(),
                beanDiffer(getExpectedGroupMultiplierStats(expectedLowerBound, expectedUpperBound)));

    }
}

package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.conditions.common.AfterLastConditionTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("ТГО/РМП Проверка статусов после удаления последней фразы через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag("TESTIRT-8612")
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(ObjectTag.PHRASE)
@RunWith(Parameterized.class)
public class AfterLastPhraseRemoveTest extends AfterLastConditionTestBase {

    @Parameterized.Parameters(name = "Проверка статусов после удаления последней фразы у {0} кампании")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    public AfterLastPhraseRemoveTest(CampaignTypeEnum campaignType) {
        super(BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType));
    }

    @Override
    protected void deleteCondition() {
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps()
                .deletePhrases(bannersRule.getCampaignId(), bannersRule.getGroupId(), getClient(), getPhraseIds());
        assumeThat("все фразы удалились", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(getClient(), campaignId).getGroups().get(0).getPhrases(), hasSize(0));
    }

    @Override
    protected void suspendCondition() {
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps()
                .suspendPhrases(bannersRule.getCampaignId(), bannersRule.getGroupId(), getClient(), getPhraseIds());
        assumeThat("все фразы были остановлены", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(getClient(), campaignId).getGroups().get(0).getPhrases()
                .stream().map(Phrase::getSuspended).collect(Collectors.toList()), not(hasItem("0")));
    }

    private String[] getPhraseIds() {
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(getClient(), campaignId)
                .getGroups().get(0).getPhrases().stream()
                .map(t -> String.valueOf(t.getId())).toArray(String[]::new);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10874")
    public void deleteConditionTest() {
        super.deleteConditionTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10875")
    public void suspendConditionTest() {
        super.suspendConditionTest();
    }
}

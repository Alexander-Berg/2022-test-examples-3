package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.conditions.common.AfterLastConditionTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка статусов после остановки последней фразы группы с ГО через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@RunWith(Parameterized.class)
@Tag("TESTIRT-8612")
public class AfterLastPhraseRemoveImageBannerTest extends AfterLastConditionTestBase {

    public AfterLastPhraseRemoveImageBannerTest(CampaignTypeEnum campaignTypeEnum) {
        super(new ImageBannerRule(campaignTypeEnum).withImageUploader(new NewImagesUploadHelper()));
    }

    @Parameterized.Parameters(name = "Проверка статусов после удаления последней фразы у {0} кампании с ГО")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
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
                .stream().map(Phrase::getSuspended).collect(Collectors.toList()), not(CoreMatchers.hasItem("0")));
    }

    private String[] getPhraseIds() {
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(getClient(), campaignId)
                .getGroups().get(0).getPhrases().stream()
                .map(t -> String.valueOf(t.getId())).toArray(String[]::new);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10872")
    public void deleteConditionTest() {
        super.deleteConditionTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10873")
    public void suspendConditionTest() {
        super.suspendConditionTest();
    }
}

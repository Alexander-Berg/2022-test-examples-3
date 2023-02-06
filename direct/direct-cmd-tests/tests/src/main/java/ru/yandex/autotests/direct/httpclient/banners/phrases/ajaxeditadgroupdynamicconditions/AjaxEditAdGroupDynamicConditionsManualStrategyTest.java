package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxeditadgroupdynamicconditions;


import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions.DynamicConditionsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.directapi.enums.CampaignType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by f1nal
 * on 19.08.15.
 * TESTIRT-6778
 */

@Aqua.Test
@Issue("TESTIRT-6778")
@Description("Проверка изменения ставки через ajaxUpdateShowConditions в кампании с ручной стратегией")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.PHRASES)
@Tag(TrunkTag.YES)
public class AjaxEditAdGroupDynamicConditionsManualStrategyTest extends AjaxEditAdGroupDynamicConditionsBase {

    @Test
    @Description("Позитивная провека установки одной ставки ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10253")
    public void updateSingleConditionPriceTest() {
        phraseBean = new AjaxUpdateShowConditionsBean();
        phraseBean.setPrice("123.46");
        phrasesMap.put(String.valueOf(expectedPhrasesId.get(0)), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(groupId), groupPhrases);
        sendRequestAjaxEditAdGroupDynamicConditions();
        checkPhrasePrice(phrasesMap);
    }

    @Test
    @Description("Позитивная провека установки нескольких ставкок ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10254")
    public void updateMultiConditionPriceTest() {
        double newPrice = 123.46;
        for (Integer i = 1; i <= 3; i++) {
            phraseBean = new AjaxUpdateShowConditionsBean();
            phraseBean.setPrice(String.valueOf((newPrice * i)));
            phrasesMap.put(String.valueOf(expectedPhrasesId.get(i - 1)), phraseBean);
        }
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(groupId), groupPhrases);
        sendRequestAjaxEditAdGroupDynamicConditions();
        checkPhrasePrice(phrasesMap);
    }

    @Override
    protected Long createCamp() {
        return cmdRule.apiSteps().campaignSteps().addDefaultCampaign(CampaignType.DYNAMIC);
    }

    protected void assertPhrasePrice(DynamicConditionsCmdBean condition, AjaxUpdateShowConditionsBean checkPhraseBean) {
        assertThat("Ставка на фразе соответствует ожиданиям", condition.getPrice(), equalTo(checkPhraseBean.getPrice()));
    }
}

package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxeditadgroupdynamicconditions;


import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions.DynamicConditionsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.api5.campaigns.AddRequestMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.StrategyMaximumClicksAddMap;
import ru.yandex.autotests.directapi.model.api5.general.ExpectedResult;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by f1nal
 * on 19.08.15.
 * TESTIRT-6778
 */

@Aqua.Test
@Issue("TESTIRT-6778")
@Description("Проверка изменения ставки через ajaxUpdateShowConditions в кампании с авто стратегией")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.PHRASES)
@Tag(TrunkTag.YES)
public class AjaxEditAdGroupDynamicConditionsAutoStrategyTest extends AjaxEditAdGroupDynamicConditionsBase {

    @Override
    @Before
    public void before() {
        setUp();
    }

    @Override
    protected Long createCamp() {
        Long weeklySpendLimit = MoneyCurrency.get(Currency.RUB).getMaxPrice().multiply(10).bidLong().longValue();
        DynamicTextCampaignStrategyAddMap strategyMap = new DynamicTextCampaignStrategyAddMap()
                .withSearch(new DynamicTextCampaignSearchStrategyAddMap()
                        .withBiddingStrategyType(DynamicTextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withWbMaximumClicks(new StrategyMaximumClicksAddMap()
                                        .withWeeklySpendLimit(weeklySpendLimit)
                        ))
                .withNetwork(new DynamicTextCampaignNetworkStrategyAddMap().defaultServingOff());

        AddResponse response = cmdRule.apiSteps().campaignSteps().shouldGetResultOnAdd(
                new AddRequestMap().withCampaigns(new CampaignAddItemMap()
                        .defaultCampaignAddItem()
                        .withDynamicTextCampaign(new DynamicTextCampaignAddItemMap()
                                .withBiddingStrategy(strategyMap))),
                ExpectedResult.success());

        assumeThat("получили результаты добавления кампании", response.getAddResults(), hasSize(1));
        return response.getAddResults().get(0).getId();
    }



    @Test
    @Description("Позитивная провека установки одной ставки ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10251")
    public void updateSingleConditionPriceTest() {
        phraseBean = new AjaxUpdateShowConditionsBean();
        phraseBean.setAutobudgetPriority("5");
        phrasesMap.put(String.valueOf(expectedPhrasesId.get(0)), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(groupId), groupPhrases);
        sendRequestAjaxEditAdGroupDynamicConditions();
        checkPhrasePrice(phrasesMap);
    }

    @Test
    @Description("Позитивная провека установки нескольких ставкок ")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10252")
    public void updateMultiConditionPriceTest() {
        Integer newPrice = 1;
        for (Integer i = 0; i <= 2; i++) {
            phraseBean = new AjaxUpdateShowConditionsBean();
            Integer newPriority = (int) Math.ceil(newPrice + i * 1.8);
            phraseBean.setAutobudgetPriority(String.valueOf(newPriority));
            phrasesMap.put(String.valueOf(expectedPhrasesId.get(i)), phraseBean);
        }
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(groupId), groupPhrases);
        sendRequestAjaxEditAdGroupDynamicConditions();
        checkPhrasePrice(phrasesMap);
    }

    protected void assertPhrasePrice(DynamicConditionsCmdBean condition, AjaxUpdateShowConditionsBean checkPhraseBean) {
        assertThat("Ставка на фразе соответствует ожиданиям", condition.getAutobudgetPriority(), equalTo(checkPhraseBean.getAutobudgetPriority()));
    }
}

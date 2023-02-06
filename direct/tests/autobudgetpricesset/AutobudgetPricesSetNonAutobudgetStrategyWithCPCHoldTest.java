package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;

import com.yandex.direct.api.v5.campaigns.TextCampaignSettingsEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSettingMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignStrategyAddMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Authors: buhter
 * Date: 03.09.15
 * https://st.yandex-team.ru/TESTIRT-6971
 */
@Aqua.Test(title = "AutobudgetPrices.set - не автобюджетная стратегия c включенным CPC hold")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetNonAutobudgetStrategyWithCPCHoldTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    private AutobudgetPricesSetRequest request;

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        Long campaignId = api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                .defaultCampaignAddItem()
                .withTextCampaign(new TextCampaignAddItemMap()
                        .defaultTextCampaign()
                        .withBiddingStrategy(new TextCampaignStrategyAddMap()
                                .withSearch(new TextCampaignSearchStrategyAddMap().defaultHighestPosition())
                                .withNetwork(new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault()))
                        .withSettings(new TextCampaignSettingMap()
                                .withOption(TextCampaignSettingsEnum.MAINTAIN_NETWORK_CPC)
                                .withValue(YesNoEnum.YES))));
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        Long bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        Long keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(pid);

        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BigInteger phraseIDFake = api.userSteps.phrasesFakeSteps().getBannerPhraseParams(keywordId).getPhraseID();
        float price = 11;
        float contextPrice = 12;
        request = new AutobudgetPricesSetRequest(
                pid, phraseIDFake,
                price, contextPrice,
                1, 1);
    }

    @Test
    public void noErrorCallAutobudgetPricesSetForManualStrategyWithCPCHoldEnabled() {
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);
    }
}

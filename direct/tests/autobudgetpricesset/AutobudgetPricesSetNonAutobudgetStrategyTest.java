package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;

import com.yandex.direct.api.v5.campaigns.TextCampaignSettingsEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
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
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Authors: omaz, xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3980
 */
@Aqua.Test(title = "AutobudgetPrices.set - не автобюджетная стратегия")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetNonAutobudgetStrategyTest {
    private static final String ERROR_MESSAGE = "autobudget disabled";

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Test
    public void expectErrorCallAutobudgetPricesSetForManualStrategy() {
        Long campaignId = api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                .defaultCampaignAddItem()
                .withTextCampaign(new TextCampaignAddItemMap()
                        .defaultTextCampaign()
                        .withBiddingStrategy(new TextCampaignStrategyAddMap()
                                .withSearch(new TextCampaignSearchStrategyAddMap().defaultHighestPosition())
                                .withNetwork(new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault()))
                        .withSettings(new TextCampaignSettingMap()
                                .withOption(TextCampaignSettingsEnum.MAINTAIN_NETWORK_CPC)
                                .withValue(YesNoEnum.NO))));
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        Long keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(pid);

        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BigInteger phraseIDFake = api.userSteps.phrasesFakeSteps().getBannerPhraseParams(keywordId).getPhraseID();

        float price = 11;
        float contextPrice = 12;
        AutobudgetPricesSetRequest request = new AutobudgetPricesSetRequest(
                pid, phraseIDFake,
                price, contextPrice,
                1, 1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(request, 1, ERROR_MESSAGE);


        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("Удалось установить неверное значение для поля Price",
                keywordGetItem.getBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
        assertThat("Удалось установить неверное значение для поля ContextPrice",
                keywordGetItem.getContextBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
    }

    @Test
    public void expectErrorCallAutobudgetPricesSetForMaxCoverageStrategy() {
        Long campaignId = api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                .defaultCampaignAddItem()
                .withTextCampaign(new TextCampaignAddItemMap()
                        .defaultTextCampaign()
                        .withBiddingStrategy(new TextCampaignStrategyAddMap()
                                .withSearch(new TextCampaignSearchStrategyAddMap().defaultServingOff())
                                .withNetwork(new TextCampaignNetworkStrategyAddMap().defaultMaximumCoverage()))
                        .withSettings(new TextCampaignSettingMap()
                                .withOption(TextCampaignSettingsEnum.MAINTAIN_NETWORK_CPC)
                                .withValue(YesNoEnum.NO))));
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        Long keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(pid);

        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BigInteger phraseIDFake = api.userSteps.phrasesFakeSteps().getBannerPhraseParams(keywordId).getPhraseID();
        float price = 11;
        float contextPrice = 12;
        AutobudgetPricesSetRequest request = new AutobudgetPricesSetRequest(
                pid, phraseIDFake,
                price, contextPrice,
                1, 1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(request, 1, ERROR_MESSAGE);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("Удалось установить неверное значение для поля Price",
                keywordGetItem.getBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
        assertThat("Удалось установить неверное значение для поля ContextPrice",
                keywordGetItem.getContextBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
    }
}

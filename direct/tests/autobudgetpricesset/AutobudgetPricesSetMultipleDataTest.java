package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;

import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
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
@Aqua.Test(title = "AutobudgetPrices.set - две фразы")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetMultipleDataTest {
    private Long keywordId;
    private Long keywordIdElse;
    private Money price;
    private Money contextPriceElse;
    private AutobudgetPricesSetRequest requestData;
    private AutobudgetPricesSetRequest requestDataElse;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void init() {
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault()
        );
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(pid);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BannerPhraseFakeInfo[] bannerPhraseFakeInfos = api.userSteps.phrasesFakeSteps().getBannerPhrasesParams(pid);
        BigInteger phraseIDFake = bannerPhraseFakeInfos[0].getPhraseID();
        price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount().add(
                MoneyCurrency.get(Currency.RUB).getStepPrice()
        );
        requestData = new AutobudgetPricesSetRequest(
                pid, phraseIDFake,
                price.floatValue(), contextPrice.floatValue(),
                1, 1);

        Long campaignIdElse = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultServingOff(),
                new TextCampaignNetworkStrategyAddMap().defaultWbMaximumClicks(Currency.RUB)
        );

        Long pidElse = api.userSteps.adGroupsSteps().addDefaultGroup(campaignIdElse);
        api.userSteps.adsSteps().addDefaultTextAd(pidElse);
        keywordIdElse = api.userSteps.keywordsSteps().addDefaultKeyword(pidElse);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pidElse);
        bannerPhraseFakeInfos = api.userSteps.phrasesFakeSteps().getBannerPhrasesParams(pidElse);
        BigInteger phraseIDFakeElse = bannerPhraseFakeInfos[0].getPhraseID();


        Money priceElse = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount().add(
                MoneyCurrency.get(Currency.RUB).getStepPrice().multiply(2.0)
        );
        contextPriceElse = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount().add(
                MoneyCurrency.get(Currency.RUB).getStepPrice().multiply(3.0)
        );
        requestDataElse = new AutobudgetPricesSetRequest(
                pidElse, phraseIDFakeElse,
                priceElse.floatValue(), contextPriceElse.floatValue(),
                1, 1);
    }

    @Test
    public void autobudgetPricesSetMassTest() {
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(requestData, requestDataElse);
        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        KeywordGetItem keywordGetItemElse = api.userSteps.keywordsSteps().keywordsGetById(keywordIdElse).get(0);
        assertThat("Не удалось установить верное значение поля Price для кампании с автобюджетом на поиске",
                keywordGetItem.getBid(),
                equalTo(price.bidLong().longValue()));
        assertThat("Не удалось установить верное значение поля ContextPrice для кампании с автобюджетом на тематике",
                keywordGetItemElse.getContextBid(),
                equalTo(contextPriceElse.bidLong().longValue()));
    }

    @Test
    public void autobudgetPricesSetMassWithInvalidIDsTest() {
        requestData.setGroupExportID(1L);
        AutobudgetPricesSetResponse expectedError = new AutobudgetPricesSetResponse(
                1,
                "no such adgroup",
                requestData.getGroupExportID(),
                requestData.getPhraseID()
        );

        requestDataElse.setPhraseID(BigInteger.ONE.negate());
        AutobudgetPricesSetResponse expectedErrorElse = new AutobudgetPricesSetResponse(
                1,
                "bad format",
                requestDataElse.getGroupExportID(),
                requestDataElse.getPhraseID()
        );

        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedErrors(
                new AutobudgetPricesSetRequest[]{requestData, requestDataElse},
                expectedError, expectedErrorElse
        );
    }

}

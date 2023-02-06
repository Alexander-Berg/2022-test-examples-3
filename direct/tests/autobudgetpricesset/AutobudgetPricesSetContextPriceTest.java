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
@Aqua.Test(title = "AutobudgetPrices.set - установка ContextPrice")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetContextPriceTest {
    private Long keywordId;
    private AutobudgetPricesSetRequest request;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void init() {
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultServingOff(),
                new TextCampaignNetworkStrategyAddMap().defaultWbMaximumClicks(Currency.RUB)
        );
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        keywordId = api.userSteps.keywordsSteps().addDefaultKeyword(pid);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BannerPhraseFakeInfo[] bannerPhraseFakeInfos = api.userSteps.phrasesFakeSteps().getBannerPhrasesParams(pid);
        BigInteger phraseIDFake = bannerPhraseFakeInfos[0].getPhraseID();

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(phraseIDFake);
        request.setContextType(1);
    }


    @Test
    public void autobudgetPricesSetContextPriceTest() {
        Money price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount()
                .add(MoneyCurrency.get(Currency.RUB).getStepPrice().bigDecimalValue());
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        request.setCurrency(1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("Не удалось установить верное значение поля ContextPrice",
                keywordGetItem.getContextBid(), equalTo(contextPrice.bidLong().longValue()));
    }


    @Test
    public void autobudgetPricesSetCurrencyContextNegativePriceTest() {
        Money price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount().multiply(-1f);
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(
                request, 1, "bad format");

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("Не удалось установить неверное значение для поля ContextPrice",
                keywordGetItem.getContextBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
    }

}

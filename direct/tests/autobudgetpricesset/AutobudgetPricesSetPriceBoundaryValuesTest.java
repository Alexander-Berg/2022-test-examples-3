package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.money.MoneyFormat;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.apiclient.errors.AxisErrorDetails;
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
 * Authors: omaz, xy6er, buhter
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3980
 * https://st.yandex-team.ru/TESTIRT-9938
 */
@Aqua.Test(title = "AutobudgetPrices.set - граничные значения Price")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetPriceBoundaryValuesTest {
    private static final String LOCALE = "en";

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private Long keywordId;
    private AutobudgetPricesSetRequest request;

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

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(phraseIDFake);
        request.setContextType(1);
        request.setCurrency(1);
    }

    @Test
    public void callAutobudgetPricesSetWithWithZeroPrice() {
        Money price = Money.valueOf(BigDecimal.ZERO, Currency.RUB);
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("установилось минимально допустимое значение для поля Price",
                keywordGetItem.getBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
    }

    @Test
    public void callAutobudgetPricesSetWithLessThanMinPrice() {
        //Пробуем выставить значение меньше минимального
        Money price = MoneyCurrency.get(Currency.RUB).getMinPrice()
                .subtract(MoneyCurrency.get(Currency.RUB).getStepPrice());
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("установилось минимально допустимое значение для поля Price",
                keywordGetItem.getBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
    }

    @Test
    public void callAutobudgetPricesSetWithMinPrice() {
        Money price = MoneyCurrency.get(Currency.RUB).getMinPrice();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        //при конвертации ставок между валютами используем округление вверх
        assertThat("Не удалось установить верное значение поля Price",
                keywordGetItem.getBid(),
                equalTo(price.bidLong().longValue()));
    }

    @Test
    public void callAutobudgetPricesSetWithMaxPrice() {
        Money price = MoneyCurrency.get(Currency.RUB).getMaxPrice();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("Не удалось установить верное значение поля Price",
                keywordGetItem.getBid(),
                equalTo(price.bidLong().longValue()));
    }

    @Test
    public void expectErrorCallAutobudgetPricesSetWithOverMaxPrice() {
        Money price = MoneyCurrency.get(Currency.RUB).getMaxPrice().add(MoneyCurrency.get(Currency.RUB).getStepPrice());
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        String errorText = "bad price: " +
                TextResourceFormatter.resource(AxisErrorDetails.THE_BID_CANNOT_BE_MORE)
                        .args(MoneyCurrency.get(Currency.RUB).getMaxPrice()
                                        .stringValue(MoneyFormat.TWO_DIGITS_POINT_SEPARATED),
                                MoneyCurrency.get(Currency.RUB).getAPIAbbreviation(LOCALE))
                        .locale(LOCALE).toString();
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(request, 7, errorText);

        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(keywordId).get(0);
        assertThat("Удалось установить неверное значение для поля Price",
                keywordGetItem.getBid(),
                equalTo(MoneyCurrency.get(Currency.RUB).getMinPrice().bidLong().longValue()));
    }

}

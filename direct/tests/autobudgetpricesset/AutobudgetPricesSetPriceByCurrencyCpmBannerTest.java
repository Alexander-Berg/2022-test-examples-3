package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesAdgroupType;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Authors: sco76
 * Date: 15.05.18
 * https://st.yandex-team.ru/DIRECT-79892
 */
@Aqua.Test(title = "AutobudgetPrices.set — установка Price для валютного клиента с CPM-группами")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
@Issue("https://st.yandex-team.ru/DIRECT-79892")
public class AutobudgetPricesSetPriceByCurrencyCpmBannerTest {
    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);
    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shard;
    private Long keywordId;
    private AutobudgetPricesSetRequest request;

    @BeforeClass
    public static void beforeClass() {
        shard = api.userSteps.clientFakeSteps().getUserShard(Logins.LOGIN_RUB);
    }

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
        api.userSteps.campaignFakeSteps().setType(campaignId, CampaignsType.CPM_BANNER);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).adGroupsSteps()
                .setType(pid, PhrasesAdgroupType.cpm_banner);

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(phraseIDFake);
        request.setContextType(1);
    }

    @Test
    public void autobudgetPricesSetCurrencyPriceLargeAmountTest() {
        Money price = MoneyCurrency.get(Currency.RUB).getMaxCpmPrice();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getMaxCpmPrice();
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        request.setCurrency(1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        BigDecimal price1 =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).bidsSteps().getBidById(keywordId).getPrice();
        BigDecimal price2 =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).bidsSteps().getBidById(keywordId)
                        .getPriceContext();

        ImmutablePair<BigDecimal, BigDecimal> prices = ImmutablePair.of(price1, price2);
        ImmutablePair<BigDecimal, BigDecimal> expectedPrices =
                ImmutablePair.of(price.bigDecimalValue(), contextPrice.bigDecimalValue());

        assertThat("Не удалось установить верное значение полей",
                prices,
                comparesEqualTo(expectedPrices));
    }

    @Test
    public void autobudgetPricesSetCurrencyPriceBelowMinAmountTest() {
        Money price = MoneyCurrency.get(Currency.RUB).getMinCpmPrice();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getMinCpmPrice();
        request.setPrice(price.subtract(2).floatValue());
        request.setContextPrice(contextPrice.subtract(2).floatValue());
        request.setCurrency(1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        BigDecimal price1 =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).bidsSteps().getBidById(keywordId).getPrice();
        BigDecimal price2 =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).bidsSteps().getBidById(keywordId)
                        .getPriceContext();

        ImmutablePair<BigDecimal, BigDecimal> prices = ImmutablePair.of(price1, price2);
        ImmutablePair<BigDecimal, BigDecimal> expectedPrices =
                ImmutablePair.of(price.bigDecimalValue(), contextPrice.bigDecimalValue());

        assertThat("Не удалось установить верное значение полей",
                prices,
                comparesEqualTo(expectedPrices));
    }

}

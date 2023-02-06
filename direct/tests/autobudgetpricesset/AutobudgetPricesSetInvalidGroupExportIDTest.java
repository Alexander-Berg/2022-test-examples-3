package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

/**
 * Author: xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3980
 */
@Aqua.Test(title = "AutobudgetPrices.set - некорректный GroupExportID")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
@RunWith(Parameterized.class)
public class AutobudgetPricesSetInvalidGroupExportIDTest {
    private static AutobudgetPricesSetRequest request;

    @Parameterized.Parameter
    public Long groupExportID;
    @Parameterized.Parameter(1)
    public String errorMessage;
    @Parameterized.Parameter(2)
    public String retargetingErrorMessage;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameters(name = "BannerID {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {0L, "bad format", ""},
                {-10L, "bad format", ""},
                {1L, "no such adgroup", " (retargetings)"},
                {null, "bad format", ""},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void initClass() {
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault()
        );
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        Long bannerID = api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.keywordsSteps().addDefaultKeyword(pid);
        api.userSteps.phrasesFakeSteps().setBannerFakePhraseIds(pid);
        BannerPhraseFakeInfo[] bannerPhraseFakeInfos = api.userSteps.phrasesFakeSteps().getBannerPhrasesParams(pid);
        BigInteger phraseIDFake = bannerPhraseFakeInfos[0].getPhraseID();

        Money price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount()
                .add(MoneyCurrency.get(Currency.RUB).getStepPrice());

        request = new AutobudgetPricesSetRequest();
        request.setPhraseID(phraseIDFake);
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        request.setCurrency(1);
    }


    @Test
    public void expectErrorCallAutobudgetPricesSetWithInvalidGroupExportID() {
        request.setGroupExportID(groupExportID);
        request.setContextType(1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(request, 1, errorMessage);
    }

    @Test
    public void expectErrorCallAutobudgetPricesSetForRetargetingsWithInvalidGroupExportID() {
        request.setGroupExportID(groupExportID);
        request.setContextType(2);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps()
                .setWithExpectedError(request, 1, errorMessage + retargetingErrorMessage);
    }

}

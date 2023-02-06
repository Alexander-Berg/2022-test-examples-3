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
import ru.yandex.autotests.directapi.common.api45.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
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
@Aqua.Test(title = "AutobudgetPrices.set - некорректный phraseID")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
@RunWith(Parameterized.class)
public class AutobudgetPricesSetInvalidPhraseIDTest {
    private static AutobudgetPricesSetRequest request;
    private static final String ERROR_MESSAGE = "bad format";

    @Parameterized.Parameter
    public BigInteger phraseIDFake;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameters(name = "PhraseID {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {BigInteger.ZERO},
                {BigInteger.TEN.negate()},
                {null},
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
        api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.keywordsSteps().addDefaultKeyword(pid);

        Money price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        Money contextPrice = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount()
                .add(MoneyCurrency.get(Currency.RUB).getStepPrice());

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPrice(price.floatValue());
        request.setContextPrice(contextPrice.floatValue());
        request.setCurrency(1);
    }


    @Test
    public void expectErrorCallAutobudgetPricesSetWithInvalidPhraseID() {
        request.setPhraseID(phraseIDFake);
        request.setContextType(1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(request, 1, ERROR_MESSAGE);
    }

    @Test
    public void expectErrorCallAutobudgetPricesSetForRetargetingsWithInvalidPhraseID() {
        request.setPhraseID(phraseIDFake);
        request.setContextType(2);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().setWithExpectedError(request, 1, ERROR_MESSAGE);
    }

}

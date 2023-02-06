package ru.yandex.autotests.directintapi.tests.campaignssumsforbs;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.CampaignsSumsForBsResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.campaigns.Status;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.directintapi.utils.ListUtils.selectFirstNotNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 25.07.14.
 * https://jira.yandex-team.ru/browse/TESTIRT-2378
 * https://st.yandex-team.ru/TESTIRT-2887
 */
@Aqua.Test(title = "CampaignsSumsForBS - позитивные тесты")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CAMPAIGNS_SUMS_FOR_BS)
@RunWith(Parameterized.class)
public class CampaignsSumsForBsTest {
    static DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public Long cid;

    @Parameterized.Parameter(1)
    public Float sum;

    @Parameterized.Parameter(2)
    public String statusActivating;

    @Parameterized.Parameter(3)
    public String name;

    @Parameterized.Parameters(name = "{3}")
    public static Collection<Object[]> data() {
        Long cid = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
        Float sum = RandomUtils.getRandomFloat(0f, 1000000f);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(
                new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cid)
                        .withTimestamp()
                        .withConsumeQty(sum)
                        .withProductCurrency(Currency.RUB.value())
        );

        Long cid_rub = api.as(Logins.LOGIN_MAIN).userSteps.campaignSteps().addDefaultTextCampaign();
        Float sum_rub = RandomUtils.getRandomFloat(0f, 1000000f);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(
                new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(cid_rub)
                        .withTimestamp()
                        .withConsumeQty(sum_rub)
                        .withProductCurrency(Currency.RUB.value())
        );
        api.userSteps.makeCampaignActiveV5(Logins.LOGIN_MAIN, cid_rub);

        Long cid_connected_to_wallet = api.as(Logins.LOGIN_WALLET_CAMPAIGNS_SUMS_FOR_BS)
                .userSteps.campaignSteps().addDefaultTextCampaign();

        Object[][] data = new Object[][]{
                {cid, sum, Status.PENDING, "Неактивная кампания"},
                {cid_rub, sum_rub, Status.YES, "Активная кампания"},
                {cid_connected_to_wallet, 0.f, Status.PENDING, "Кампания, привязанная к кошельку"}
        };
        return Arrays.asList(data);
    }

    @Test
    public void campaignsSumsForBsTest() {
        List<CampaignsSumsForBsResponse> response = darkSideSteps.getCampaignsSumsForBsSteps().get(cid);
        CampaignsSumsForBsResponse actual =
                selectFirstNotNull(response, having(on(CampaignsSumsForBsResponse.class).getCid(), equalTo(cid)));
        CampaignsSumsForBsResponse expected =
                new CampaignsSumsForBsResponse(cid, sum, statusActivating, CampaignsType.TEXT.value(),
                        Currency.RUB.getIsoCode());
        assertThat("Неправильная сумма у кампании", actual, beanEquals(expected));
    }
}

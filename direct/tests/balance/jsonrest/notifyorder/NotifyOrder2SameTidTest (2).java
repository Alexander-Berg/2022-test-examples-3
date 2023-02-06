package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


/**
 * Created by chicos on 23.06.2015.
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@Issue("https://st.yandex-team.ru/DIRECT-42952")
public class NotifyOrder2SameTidTest {
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2SameTidTest.class);

    private final static String TID = Long.toString(DateTime.now().getMillis() * 10);
    private Money qty = Money.valueOf(100.0f, Currency.RUB);

    private long campaignId;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Before
    @Step("Подготовим данные для теста")
    public void prepareCampaign() {
        log.info("Создадим кампанию");
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        log.info("Вызываем метод NotifyOrder2");
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTid(TID)
                .withConsumeQty(0f)
                .withProductCurrency(Currency.RUB.value())
        );

        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assumeThat("баланс кампании", campaignGetItem.getFunds().getCampaignFunds().getSum(), equalTo(0L));
    }

    @Test
    public void notifyOrder2WithSameTidTest() {
        log.info("Вызываем метод NotifyOrder2 с тем же Tid'ом");
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps().notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                .withServiceOrderId(campaignId)
                .withTid(TID)
                .withConsumeQty(qty.floatValue())
                .withProductCurrency(Currency.RUB.value())
        );
        CampaignGetItem campaignGetItem =
                api.userSteps.campaignSteps().campaignsGet(campaignId, CampaignFieldEnum.FUNDS);
        assertThat("баланс кампании изменился", campaignGetItem.getFunds().getCampaignFunds().getSum(),
                equalTo(Money.valueOf(qty.floatValue()).bidLong().longValue()));
    }

}

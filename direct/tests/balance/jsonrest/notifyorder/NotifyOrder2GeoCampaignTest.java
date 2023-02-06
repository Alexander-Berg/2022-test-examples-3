package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder.NotifyOrder2WrongServiceIDTest.ERROR_TEXT_FORMAT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Author: xy6er
 * https://st.yandex-team.ru/TESTIRT-9086
 */
@Issue("https://st.yandex-team.ru/DIRECT-53288")
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
public class NotifyOrder2GeoCampaignTest {

    private long campaignId;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_RUB);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    @Step("Создаем гео кампанию")
    public void createGeoCampaign() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setType(campaignId, CampaignsType.GEO);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_RUB);
    }


    @Test
    @Title("NotifyOrder2 для гео-кампании")
    @Description("Вызываем NotifyOrder2 для гео-кампании. Проверяем, что сумма у кампании изменилась")
    public void notifyOrder2ForGeoCampaignTest() {
        float qty = 100.0f;
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(new NotifyOrder2JSONRequest()
                        .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                        .withServiceOrderId(campaignId)
                        .withTimestamp()
                        .withConsumeQty(qty)
                        .withProductCurrency(Currency.RUB.value())
                );
        CampaignsRecord campaigns = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании изменился", campaigns.getSum(), equalTo(BigDecimal.valueOf(qty).setScale(6)));
    }

    @Test
    @Title("NotifyOrder2 для гео-кампании с неправильным ServiceID")
    @Description("Вызываем NotifyOrder2 для гео-кампании с неправильным ServiceID. " +
            "Проверяем, что получили ошибку в ответе метода и что сумма у кампании не изменилась")
    public void notifyOrder2WithWrongServiceIDForGeoCampaignTest() {
        api.userSteps.getDarkSideSteps().getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.BAYAN_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withConsumeQty(100f)
                                .withProductCurrency(Currency.RUB.value()),
                        400, 1014,
                        String.format(ERROR_TEXT_FORMAT, campaignId, CampaignsType.GEO.value(),
                                NotifyOrder2JSONRequest.BAYAN_SERVICE_ID)
                );
        CampaignsRecord campaigns = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(campaignId);
        assertThat("Баланс кампании не изменился", campaigns.getSum(), equalTo(BigDecimal.ZERO.setScale(6)));
    }
}

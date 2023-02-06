package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BsResyncQueueRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.common.Value;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


/**
 * Created by chicos on 02.07.2015.
 * <p>
 * https://st.yandex-team.ru/TESTIRT-6103
 */

@Aqua.Test(title = "NotifyOrder2 - провека синхронизации с БК")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_ORDER2)
@Issue("https://st.yandex-team.ru/DIRECT-43046")
@RunWith(Parameterized.class)
public class NotifyOrder2MinorChangesToResyncQueueTest {
    protected static LogSteps log = LogSteps.getLogger(NotifyOrder2MinorChangesToResyncQueueTest.class);
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    public static final String LOGIN = Logins.LOGIN_RUB;
    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    public long campaignId;

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public NotifyOrder2JSONRequest request;

    @Parameterized.Parameter(value = 2)
    public String expectedBsSynced;

    @Parameterized.Parameter(value = 3)
    public Matcher resyncedQueueMatcher;

    @Parameterized.Parameters(name = "test = {0}")
    public static Collection dataSet() {
        Object[][] data = new Object[][]{
                {"NotifyOrder2 без изменений",
                        new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withTimestamp()
                                .withConsumeQty(0f)
                                .withProductCurrency("RUB"),
                        Value.YES,
                        iterableWithSize(0)},
                {"NotifyOrder2 со значимыми изменениями - сумма на кампании",
                        new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withTimestamp()
                                .withConsumeQty(100f)
                                .withProductCurrency("RUB"),
                        Value.NO,
                        iterableWithSize(0)}
        };
        return Arrays.asList(data);
    }

    @Test
    public void statusBsSyncedTest() {
        log.info("Создадим кампанию");
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(LOGIN);
        log.info("Установим фейково статус синхронизации с БК");
        api.userSteps.campaignFakeSteps().setBSSynced(campaignId, true);
        CampaignFakeInfo campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assumeThat("кампания фейково синхронизирована с БК", campaignFakeInfo.getStatusBsSynced(), equalTo(Value.YES));

        log.info("Вызываем метод NotifyOrder2 для кампании " + campaignId);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderNoErrors(request.withServiceOrderId(campaignId));

        campaignFakeInfo = api.userSteps.campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("статус синхронизации с БК", campaignFakeInfo.getStatusBsSynced(), equalTo(expectedBsSynced));

        List<BsResyncQueueRecord> resyncQueue
                = api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN).bsResyncQueueSteps().getBsResyncQueueRecordsByCid(campaignId);
        assertThat("очередь ленивой синхронизации с БК", resyncQueue, resyncedQueueMatcher);
    }
}

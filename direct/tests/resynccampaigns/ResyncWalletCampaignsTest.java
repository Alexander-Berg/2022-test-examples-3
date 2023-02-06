package ru.yandex.autotests.directintapi.tests.resynccampaigns;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Author bzzzz
 * Date 2016-11-16
 * https://st.yandex-team.ru/TESTIRT-10206
 */
@Aqua.Test
@Description("ResyncCampaigns - Ручка для переотправки кампаний по инициативе БК (Поддержка кошельков)")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.RESYNC_CAMPAIGNS)
public class ResyncWalletCampaignsTest {
    private static final int MIN_PRIORITY = 100;

    protected LogSteps log = LogSteps.getLogger(this.getClass());

    private static String login = Logins.LOGIN_WALLET;
    private Long walletCid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(login);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trashman = new Trashman(api);

    @Before
    public void before() {
        long cid = api.userSteps.campaignSteps().addDefaultTextCampaign(login);
        walletCid = Long.valueOf(api.userSteps.campaignFakeSteps().fakeGetCampaignParams(cid).getWalletCid());
        api.userSteps.getDBSteps().getTransportDBSteps().deleteCampaignFromBsResyncQueueByCid(walletCid);
    }

    @Test
    public void resyncWalletCampaignsTest() {
        assumeThat("кошелек не в очереди",
                api.userSteps.getDBSteps().getTransportDBSteps().getResyncQueueList(walletCid), nullValue());
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(walletCid);
        api.userSteps.getDBSteps().getTransportDBSteps()
                .checkCampaignsPriorityInResyncQueue(walletCid, greaterThan(MIN_PRIORITY));
    }

    @After
    public void after() {
        api.userSteps.getDBSteps().getTransportDBSteps().deleteCampaignFromBsResyncQueueByCid(walletCid);
    }
}

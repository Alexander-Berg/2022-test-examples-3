package ru.yandex.autotests.directintapi.tests.resynccampaigns;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.greaterThan;

/**
 * Author xy6er
 * Date 11.12.14
 * https://st.yandex-team.ru/TESTIRT-3728
 */
@Aqua.Test(title = "ResyncCampaigns - Ручка для переотправки кампаний по инициативе БК")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.RESYNC_CAMPAIGNS)
public class ResyncCampaignsTest {
    private static final int MIN_PRIORITY = 100;

    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private Long cid;
    private Long bid;
    private Long pid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void before() {
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.keywordsSteps().addDefaultKeyword(pid);
    }


    @Test
    public void resyncCampaignsCidTest() {
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(cid);
        api.userSteps.getDBSteps().getTransportDBSteps()
                .checkCampaignsPriorityInResyncQueue(cid, greaterThan(MIN_PRIORITY));
    }

    @Test
    public void resyncCampaignsBidTest() {
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(cid);
        api.userSteps.getDBSteps().getTransportDBSteps()
                .checkBannersPriorityInResyncQueue(cid, pid, bid, greaterThan(MIN_PRIORITY));
    }

    @Test
    public void resyncCampaignsPidTest() {
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(cid);
        api.userSteps.getDBSteps().getTransportDBSteps()
                .checkPhrasesPriorityInResyncQueue(cid, pid, greaterThan(MIN_PRIORITY));
    }

    @Test
    public void resyncCampaignsWithInvalidAndDeletedAndValidCidsTest() {
        log.info("Вызываем ResyncCampaigns с невалидным и корректным cid");
        log.info("Ожидаем, что ручка не упадет и добавить в очередь кампанию с корректным cid");
        Long invalidCid = -1L;
        Long deletedCid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignSteps().campaignsDelete(deletedCid);
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(invalidCid, deletedCid, cid);
        api.userSteps.getDBSteps().getTransportDBSteps()
                .checkCampaignsPriorityInResyncQueue(cid, greaterThan(MIN_PRIORITY));
    }

}

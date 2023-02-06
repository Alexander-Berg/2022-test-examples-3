package ru.yandex.autotests.directintapi.tests.resynccampaigns;

import java.util.List;

import javax.util.streamex.StreamEx;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BsResyncQueueRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

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

    private static DirectJooqDbSteps jooqDbSteps;
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private Long cid;
    private Long bid;
    private Long pid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void prepareJooqDbSteps() {
        jooqDbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN_MAIN);
    }

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
        checkCampaignsPriorityInResyncQueue(cid);
    }

    @Test
    public void resyncCampaignsBidTest() {
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(cid);
        BsResyncQueueRecord result = jooqDbSteps.bsResyncQueueSteps().getBsResyncQueueRecord(cid, pid, bid);
        assertThat("Баннер не встал в очередь на повторную отправку", result, notNullValue());
        assertThat("Приоритет не соответствует матчеру: ", result.getPriority(), greaterThan(MIN_PRIORITY));
    }

    @Test
    public void resyncCampaignsPidTest() {
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(cid);
        BsResyncQueueRecord result = jooqDbSteps.bsResyncQueueSteps().getBsResyncQueueRecord(cid, pid);
        assertThat("Группа не встала в очередь на повторную отправку", result, notNullValue());
        assertThat("Приоритет не соответствует матчеру: ", result.getPriority(), greaterThan(MIN_PRIORITY));
    }

    @Test
    public void resyncCampaignsWithInvalidAndDeletedAndValidCidsTest() {
        log.info("Вызываем ResyncCampaigns с невалидным и корректным cid");
        log.info("Ожидаем, что ручка не упадет и добавить в очередь кампанию с корректным cid");
        Long invalidCid = -1L;
        Long deletedCid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignSteps().campaignsDelete(deletedCid);
        api.userSteps.getDarkSideSteps().getResyncCampaignsSteps().add(invalidCid, deletedCid, cid);
        checkCampaignsPriorityInResyncQueue(cid);
    }

    private void checkCampaignsPriorityInResyncQueue(Long cid) {
        List<BsResyncQueueRecord> result = jooqDbSteps.bsResyncQueueSteps().getBsResyncQueueRecordsByCid(cid);
        assertThat("Кампания не встала в очередь на повторную отправку", result.isEmpty(), equalTo(false));
        assertThat("Приоритет не соответствует матчеру: ",
                StreamEx.of(result).map(BsResyncQueueRecord::getPriority).toList(),
                everyItem(greaterThan(MIN_PRIORITY)));
    }
}

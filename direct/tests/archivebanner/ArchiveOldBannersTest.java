package ru.yandex.autotests.directintapi.tests.archivebanner;

import java.util.Arrays;
import java.util.Collection;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusarch;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.common.Value;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by chicos on 21.04.2015.
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.ARCHIVE_OLD_BANNER)
@Issue("https://st.yandex-team.ru/DIRECT-40317")
@Description("Проверка архивации остановленного баннера, если в нем не было изменений в течение более чем 30 дней")
@RunWith(Parameterized.class)
public class ArchiveOldBannersTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    private static final String login = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(login);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public DateTime lastChange;

    @Parameterized.Parameter(2)
    public BannersStatusarch expectedArchiveStatus;

    @Parameterized.Parameters(name = "test = {0}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                {"LastChange меньше 30 дней", DateTime.now().minusDays(29), BannersStatusarch.No},
                {"LastChange больше 30 дней", DateTime.now().minusDays(30).minusMinutes(1), BannersStatusarch.Yes},
        });
    }

    private Long bannerID;
    private static Long campaignId;

    @Before
    @Step("Подготовим данные для теста")
    public void prepareCampaign() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long groupID = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        bannerID = api.userSteps.adsSteps().addDefaultTextAd(groupID);

        log.info("Промодерируем баннер и остановим");
        api.userSteps.campaignFakeSteps().makeCampaignModerated(campaignId);
        api.userSteps.bannersFakeSteps().makeBannersModerated(bannerID);
        api.userSteps.groupFakeSteps().setStatusModerate(groupID, Value.YES);
        api.userSteps.groupFakeSteps().setGroupFakeStatusBsSynced(groupID, Value.YES);
        api.userSteps.adsSteps().adsSuspend(bannerID);

        log.info("Фейково обновим время последней модификации кампании");
        api.userSteps.campaignFakeSteps().setLastChange(campaignId, lastChange.toString());
        api.userSteps.bannersFakeSteps().setLastChange(bannerID, lastChange.toString());
        api.userSteps.groupFakeSteps().setLastChange(groupID, lastChange.toString());
    }

    @Test
    public void autoArchiveOldBannersTest() {
        int userShard = api.userSteps.clientFakeSteps().getUserShard(login);
        log.info("Вызываем скрипт ppcArchiveOldBanners.pl - архивация баннеров не обновлявшихся более 30 дней");
        darkSideSteps.getRunScriptSteps().runPpcArchiveOldBanners(userShard, campaignId);
        BannersRecord bannersRecord =
                api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).bannersSteps().getBanner(bannerID);

        assertThat("статус архивации баннера", bannersRecord.getStatusarch(), equalTo(expectedArchiveStatus));
    }
}

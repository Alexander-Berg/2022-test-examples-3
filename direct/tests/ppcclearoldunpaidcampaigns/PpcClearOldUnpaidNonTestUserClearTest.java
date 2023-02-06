package ru.yandex.autotests.directintapi.tests.ppcclearoldunpaidcampaigns;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.UsersHidden;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampOptionsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.GetRequestMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.emptyIterable;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 11/08/16
 * https://st.yandex-team.ru/TESTIRT-9407
 * Из-за https://st.yandex-team.ru/DIRECT-48848 тест работает плохо.
 * После удаления всех кампаний, при попытке создания новой в After - падаем. Кампания создается, но ОС не включается
 * При след. запуске будет ошибка при получении ОС. Пока @Ignore.
 */
@Aqua.Test
@Features(FeatureNames.PPC_CLEAR_OLD_UNPAID)
@Issue("https://st.yandex-team.ru/DIRECT-53668")
public class PpcClearOldUnpaidNonTestUserClearTest {
    public static final String LOGIN = Logins.LOGIN_CLEARCAMP3;
    public static final int DAYS_BEFORE_DELETE = 365;
    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @Rule
    public Trashman trashman = new Trashman(api);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    public static int shard;

    public List<Long> cids;
    public CampaignsRecord wallet;

    @BeforeClass
    public static void init() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).usersSteps()
                .setUsersHidden(Long.valueOf(User.get(LOGIN).getPassportUID()), UsersHidden.No);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void prepare() {
        api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignSteps().addDefaultMobileAppCampaign();
        cids = api.userSteps.campaignSteps().getCampaigns(new GetRequestMap().withFieldNames(CampaignFieldEnum.ID)
                .withSelectionCriteria(new CampaignsSelectionCriteriaMap()))
                .stream().map(CampaignGetItem::getId).collect(Collectors.toList());
        Long walletСid = (long) api.userSteps.financeSteps().getAccountID(LOGIN);
        wallet = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().getCampaignById(walletСid);
        List<CampaignsRecord> campaignsRecords = new ArrayList<>();
        List<CampOptionsRecord> campOptionsRecords = new ArrayList<>();
        for (Long cid : cids) {
            campaignsRecords.add(api.userSteps.getDirectJooqDbSteps().useShard(shard)
                    .campaignsSteps().getCampaignById(cid));
            campOptionsRecords.add(api.userSteps.getDirectJooqDbSteps().useShard(shard)
                    .campaignsSteps().getCampOptionsById(cid));
        }
        for (CampaignsRecord campaignsRecord : campaignsRecords) {
            api.userSteps.getDirectJooqDbSteps().useShard(shard)
                    .campaignsSteps().updateCampaigns(
                    campaignsRecord
                            .setOrderid(0L)
                            .setSum(BigDecimal.ZERO)
                            .setSumToPay(BigDecimal.ZERO)
                            .setSumSpent(BigDecimal.ZERO)
                            .setSumLast(BigDecimal.ZERO)
                            .setShows(0L)
            );
        }
        for (CampOptionsRecord campOptionsRecord : campOptionsRecords) {
            api.userSteps.getDirectJooqDbSteps().useShard(shard)
                    .campaignsSteps().updateCampOptions(
                    campOptionsRecord
                            .setCreateTime(Timestamp.from(Instant.now().minus(DAYS_BEFORE_DELETE, DAYS)))
            );
        }
    }

    @Test
    public void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcClearOldUnpaidCampaigns(shard, LOGIN);
        List<Long> cidsAfterClear = api.userSteps.campaignSteps()
                .getCampaigns(new GetRequestMap().withFieldNames(CampaignFieldEnum.ID)
                        .withSelectionCriteria(new CampaignsSelectionCriteriaMap()))
                .stream().map(CampaignGetItem::getId).collect(Collectors.toList());
        assertThat("после запуска скрипта очистки для нетестового клиента удалили все кампании",
                cidsAfterClear, emptyIterable());
    }

    @After
    public void after() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).usersSteps()
                .setUsersHidden(Long.valueOf(User.get(LOGIN).getPassportUID()), UsersHidden.Yes);
        api.userSteps.campaignSteps().addDefaultTextCampaign();
    }
}

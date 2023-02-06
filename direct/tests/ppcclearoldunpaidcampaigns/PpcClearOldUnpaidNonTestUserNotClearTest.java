package ru.yandex.autotests.directintapi.tests.ppcclearoldunpaidcampaigns;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.AfterClass;
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
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 11/08/16
 * https://st.yandex-team.ru/TESTIRT-9407
 * В BeforeClass создаем несколько разных кампаний. Модифицируем их таким образом, чтобы скрипт их чистил.
 * В Before создаем новую кампанию, модифицируем таким образом, чтобы скрипт ее чистил.
 * В каждом тесте меняем один из параметров таким образом, чтобы она мешала чистке.
 * После теста Trashman ее удаляет. Использовать одну кампанию для всех тетов не годится, так как в случае бага,
 * она удалится и дальнейшие тесты попадают, что усложнит разбор тестов.
 */
@Aqua.Test
@Features(FeatureNames.PPC_CLEAR_OLD_UNPAID)
@Issue("https://st.yandex-team.ru/DIRECT-53668")
public class PpcClearOldUnpaidNonTestUserNotClearTest {
    public static final String LOGIN = Logins.LOGIN_CLEARCAMP4;
    public static final int DAYS_BEFORE_DELETE = 365;
    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @Rule
    public Trashman trashman = new Trashman(api);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    public static int shard;

    public static List<Long> cids;
    public static CampaignsRecord wallet;
    public CampOptionsRecord campOptions;
    public CampaignsRecord campaign;

    @BeforeClass
    public static void init() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).usersSteps()
                .setUsersHidden(Long.valueOf(User.get(LOGIN).getPassportUID()), UsersHidden.No);
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

    @Before
    @Step("Подготовка тестовых данных")
    public void prepare() {
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        campaign = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().getCampaignById(cid);
        campOptions = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().getCampOptionsById(cid);
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().updateCampaigns(
                wallet
                        .setSum(BigDecimal.ZERO)
                        .setSumSpent(BigDecimal.ZERO)
                        .setSumLast(BigDecimal.ZERO)
        );
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().updateCampaigns(
                campaign
                        .setOrderid(0L)
                        .setSum(BigDecimal.ZERO)
                        .setSumToPay(BigDecimal.ZERO)
                        .setSumSpent(BigDecimal.ZERO)
                        .setSumLast(BigDecimal.ZERO)
                        .setShows(0L)
        );
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().updateCampOptions(
                campOptions
                        .setCreateTime(Timestamp.from(Instant.now().minus(DAYS_BEFORE_DELETE, DAYS)))
        );
        cids = api.userSteps.campaignSteps().getCampaigns(new GetRequestMap().withFieldNames(CampaignFieldEnum.ID)
                .withSelectionCriteria(new CampaignsSelectionCriteriaMap()))
                .stream().map(CampaignGetItem::getId).collect(Collectors.toList());
    }

    @Test
    public void testCampaignOrderId() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps().updateCampaigns(campaign.setOrderid(1L));
        test();
    }

    @Test
    public void testCampaignSum() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(campaign.setSum(BigDecimal.ONE));
        test();
    }

    @Test
    public void testCampaignSumToPay() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(campaign.setSumToPay(BigDecimal.ONE));
        test();
    }

    @Test
    public void testCampaignSumSpent() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(campaign.setSumSpent(BigDecimal.ONE));
        test();
    }

    @Test
    public void testCampaignSumLast() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(campaign.setSumLast(BigDecimal.ONE));
        test();
    }

    @Test
    public void testCampaignShows() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(campaign.setShows(1L));
        test();
    }

    @Test
    public void testCampOptionsCreateTime() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .campaignsSteps().updateCampOptions(
                campOptions
                        .setCreateTime(Timestamp.from(Instant.now().minus(DAYS_BEFORE_DELETE - 1, DAYS)))
        );
        test();
    }

    @Test
    public void testWalletSum() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(wallet.setSum(BigDecimal.ONE));
        test();
    }

    @Test
    public void testWalletSumSpent() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(wallet.setSumSpent(BigDecimal.ONE));
        test();
    }

    @Test
    public void testWalletSumLast() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps()
                .updateCampaigns(wallet.setSumLast(BigDecimal.ONE));
        test();
    }

    private void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcClearOldUnpaidCampaigns(shard, LOGIN);
        List<Long> cidsAfterClear = api.userSteps.campaignSteps()
                .getCampaigns(new GetRequestMap().withFieldNames(CampaignFieldEnum.ID)
                        .withSelectionCriteria(new CampaignsSelectionCriteriaMap()))
                .stream().map(CampaignGetItem::getId).collect(Collectors.toList());
        assertThat("список кампаний после запуска скрипта очистки для нетестового клиента не изменился",
                cidsAfterClear, beanDiffer(cids));
    }


    @AfterClass
    public static void afterClass() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).usersSteps()
                .setUsersHidden(Long.valueOf(User.get(LOGIN).getPassportUID()), UsersHidden.Yes);

    }
}

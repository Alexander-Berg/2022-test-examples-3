package ru.yandex.autotests.directintapi.tests.ppcclearoldunpaidcampaigns;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.UsersHidden;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampOptionsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.GetRequestMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 11/08/16
 * https://st.yandex-team.ru/TESTIRT-9407
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_CLEAR_OLD_UNPAID)
@Issue("https://st.yandex-team.ru/DIRECT-53668")
public class PpcClearOldUnpaidTestUserWithoutWalletTest {
    public static final String LOGIN = Logins.LOGIN_CLEARCAMP2;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @Rule
    public Trashman trashman = new Trashman(api);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    public static int shard;

    public List<Long> cids;

    @BeforeClass
    public static void init() {
        shard = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void prepare() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).usersSteps()
                .setUsersHidden(Long.valueOf(User.get(LOGIN).getPassportUID()), UsersHidden.Yes);
        api.userSteps.campaignSteps().addDefaultDynamicTextCampaign();
        api.userSteps.campaignSteps().addDefaultTextCampaign();
        api.userSteps.campaignSteps().addDefaultMobileAppCampaign();
        cids = api.userSteps.campaignSteps().getCampaigns(new GetRequestMap().withFieldNames(CampaignFieldEnum.ID)
                .withSelectionCriteria(new CampaignsSelectionCriteriaMap()))
                .stream().map(CampaignGetItem::getId).collect(Collectors.toList());
        Object wallet = api.userSteps.financeSteps().getAccount(LOGIN);
        assumeThat("у пользователя отключен ОС", wallet, nullValue());
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
                            .setCreateTime(Timestamp.from(Instant.now().minus(365, DAYS)))
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
        assertThat("список кампаний после очистки тестового клиента не изменился", cidsAfterClear, beanDiffer(cids));
    }
}

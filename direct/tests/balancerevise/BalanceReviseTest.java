package ru.yandex.autotests.directintapi.tests.balancerevise;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.utils.date.DBTimeConverter;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BALANCE_REVISE)
@Title("Проверка получения информации о кампания с деньгами, но не в БК (orderId=0)")
@Issue("https://st.yandex-team.ru/DIRECT-51536")
public class BalanceReviseTest {

    public static final Long CAMPAIGN_SUM = 50L;
    public static final Integer MINUS_MINUTES = 61;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private Long campaignId;

    @Before
    public void before() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.LOGIN_MAIN);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
        CampaignsRecord campRecord = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(campaignId);
        campRecord.setSum(new BigDecimal(CAMPAIGN_SUM));
        campRecord.setLastchange(DBTimeConverter.jodaToSql(DateTime.now().minusMinutes(MINUS_MINUTES)));
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().updateCampaigns(campRecord);
    }

    @Test
    @Title("Получение данных кампании с деньгами, но не в БК")
    public void getPaidCidsNotInBSTest() {
        List<Long> actualResponse = api.userSteps.getDarkSideSteps()
                .getBalanceReviseSteps().getPaidCidsNotInBS();
        assertThat("кампания есть в списке", actualResponse, hasItem(campaignId));
    }

}

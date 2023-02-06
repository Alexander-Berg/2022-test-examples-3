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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BALANCE_REVISE)
@Title("Проверка отсутствия кампании в сетвисе balanceRevise")
@Issue("https://st.yandex-team.ru/DIRECT-51536")
public class BalanceReviseValidationTest {

    public static final Long CAMPAIGN_SUM = 50L;
    public static final Integer MINUS_MINUTES = 61;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private Long campaignId;
    private CampaignsRecord campaign;

    @Before
    public void before() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.LOGIN_MAIN);
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
        campaign = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(campaignId);
        campaign.setSum(new BigDecimal(CAMPAIGN_SUM));
        campaign.setLastchange(DBTimeConverter.jodaToSql(DateTime.now().minusMinutes(MINUS_MINUTES)));
    }

    @Test
    @Title("Проверка отсутствия кампании с orderId != 0")
    public void getPaidCidsNotInBSOrderNotZeroValidationTest() {
        campaign.setOrderid(1L);
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().updateCampaigns(campaign);
        check();
    }


    @Test
    @Title("Проверка отсутствия кампании с lastChange = now()")
    public void getPaidCidsNotInBSLastChangeNowValidationTest() {
        campaign.setLastchange(DBTimeConverter.jodaToSql(DateTime.now()));
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().updateCampaigns(campaign);
        check();
    }


    @Test
    @Title("Проверка отсутствия кампании с sum = 0")
    public void getPaidCidsNotInBSSumZeroValidationTest() {
        campaign.setSum(new BigDecimal(0L));
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().updateCampaigns(campaign);
        check();
    }


    @Test
    @Title("Проверка отсутствия кампании с sum-sumSpent = 0")
    public void getPaidCidsNotInBSSumSpentValidationTest() {
        campaign.setSumSpent(new BigDecimal(CAMPAIGN_SUM));
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().updateCampaigns(campaign);
        check();
    }

    private void check() {
        List<Long> actualResponse = api.userSteps.getDarkSideSteps()
                .getBalanceReviseSteps().getPaidCidsNotInBS();
        assertThat("кампании нет в списке", actualResponse,
                not(hasItem(campaignId)));
    }

}

package ru.yandex.autotests.directintapi.tests.soap.setbannerautobudgetshowstatus;

import java.util.Arrays;
import java.util.Collection;

import com.yandex.direct.api.v5.campaigns.DailyBudgetModeEnum;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.GroupFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.soap.SetBannerAutobudgetShowStatusRequestData;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.SoapClientSteps;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DailyBudgetMap;
import ru.yandex.autotests.directapi.model.common.Value;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: omaz
 * Date: 11.09.13
 * https://st.yandex-team.ru/DIRECT-31039
 */

@Aqua.Test(title = "setBannerAutobudgetShowStatus - должна игнорировтаься для кампаний без автобюджета")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.SOAP_SET_BANNER_AUTOBUDGET_SHOW_STATUS)
@RunWith(Parameterized.class)
public class SetBannerAutobudgetShowStatusNegativeTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    public String currentMethodName = SoapClientSteps.SET_AUTOBUDGET_SHOW_STATUS;
    private Long pid;
    @Parameterized.Parameter()
    public Long campaignId;
    @Parameterized.Parameter(1)
    public String testcase;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_WALLET);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {

        Object[][] data = new Object[][]{
                {
                        api.userSteps.campaignSteps().addCampaign(new CampaignAddItemMap()
                                .defaultCampaignAddItem()
                                .withDailyBudget(new DailyBudgetMap()
                                        .withAmount(Money.valueOf(300).bidLong().longValue())
                                        .withMode(DailyBudgetModeEnum.STANDARD))
                                .withDefaultTextCampaign()),
                        "кампания с нерастянутым дневным бюджетом"
                },
                {
                        api.userSteps.campaignSteps().addDefaultTextCampaign(),
                        "кампания без автобюджета"
                },
        };
        return Arrays.asList(data);
    }

    @Before
    public void init() {
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
    }

    @Test
    @Title("DIRECT-31039")
    public void setBannerAutobudgetShowStatusTest() {
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().checkSetBannerAutobudgetShowStatusNoErrors(
                currentMethodName, new SetBannerAutobudgetShowStatusRequestData().withBanner(pid, false)
        );
        log.info("Проверяем, что значение autobudgetShowStatus не поменялось");
        GroupFakeInfo groupFakeInfo = api.userSteps.groupFakeSteps().getGroupParams(pid);
        assertThat("Не удалось изменить значение autobudgetShowStatus", groupFakeInfo.getStatusAutobudgetShow(),
                equalTo(Value.YES));
    }

}

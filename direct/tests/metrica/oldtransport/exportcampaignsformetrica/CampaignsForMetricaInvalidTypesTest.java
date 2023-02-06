package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.exportcampaignsformetrica;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.yaml.CampaignsForMetricaResponse;
import ru.yandex.autotests.directapi.darkside.model.CampaignsType;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: omaz
 * Date: 27.09.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1044
 */
@Aqua.Test(title = "CampaignsForMetrica для кампаний неподходящих типов")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_CAMPAIGNS_FOR_METRICA)
@Issues({@Issue("https://st.yandex-team.ru/TESTIRT-1044"), @Issue("https://st.yandex-team.ru/DIRECT-78212")})
@RunWith(value = Parameterized.class)
public class CampaignsForMetricaInvalidTypesTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);
    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();
    @Parameterized.Parameter
    public CampaignsType campaignType;
    @Parameterized.Parameter(1)
    public boolean acceptable;
    private DarkSideSteps darkSideSteps = new DarkSideSteps();
    private int uid = Integer.parseInt(api.userSteps.clientFakeSteps()
            .getClientData(Logins.LOGIN_MAIN).getPassportID());
    private Long campaignId;

    @Parameterized.Parameters(name = "тип кампании: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {CampaignsType.MCB, false},
                {CampaignsType.GEO, false},
                {CampaignsType.WALLET, false},
                {CampaignsType.MCBANNER, true},
                {CampaignsType.PERFORMANCE, true},
                {CampaignsType.CPM_DEALS, true},
                {CampaignsType.CPM_BANNER, true},
                {CampaignsType.MOBILE_CONTENT, false}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        api.userSteps.adsSteps().addDefaultTextAd(pid);
    }

    @Test
    public void campaignsForMetricaInvalidTypesTest() {
        api.userSteps.campaignFakeSteps().setType(campaignId, campaignType);
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().exportCampaignsForMetricaNoErrors(uid, true);
        CampaignsForMetricaResponse.CampaignData campaignData =
                selectFirst(response.getCampaignList(),
                        having(on(CampaignsForMetricaResponse.CampaignData.class).getCid(), equalTo(campaignId)));

        assertThat(("Ответ соответствует ожиданиям для кампании " + campaignId),
                campaignData,
                acceptable ? notNullValue() : nullValue()
        );
    }
}

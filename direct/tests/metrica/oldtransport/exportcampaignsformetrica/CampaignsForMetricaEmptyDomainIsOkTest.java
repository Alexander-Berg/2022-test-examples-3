package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.exportcampaignsformetrica;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.yaml.CampaignsForMetricaResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.yaml.CampaignsForMetricaResponse.CampaignData;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.api5.ads.AdAddItemMap;
import ru.yandex.autotests.directapi.model.api5.ads.TextAdAddMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by chicos on 22.04.2015.
 */
@Aqua.Test(title = "CampaignsForMetrica для кампаний с баннером со ссылками и без ссылок")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_CAMPAIGNS_FOR_METRICA)
@Issue("https://st.yandex-team.ru/DIRECT-37744")
public class CampaignsForMetricaEmptyDomainIsOkTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Rule
    public Trashman trasher = new Trashman(api);

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static int uid;
    private static Long campaignHasNoDomainBanner;
    private static Long campaignHasDomainBanner;

    @BeforeClass
    public static void prepareCampaigns() {
        uid = Integer.valueOf(api.userSteps.clientFakeSteps().getClientData(Logins.LOGIN_MAIN).getPassportID());

        //баннер без href и domain
        campaignHasNoDomainBanner = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pidHasNoDomainBanner = api.userSteps.adGroupsSteps().addDefaultGroup(campaignHasNoDomainBanner);
        Long vCardId = api.userSteps.vCardsSteps().addDefaultVCard(campaignHasNoDomainBanner);
        api.userSteps.adsSteps().addAd(new AdAddItemMap()
                .withAdGroupId(pidHasNoDomainBanner)
                .withTextAd(new TextAdAddMap()
                        .defaultTextAd()
                        .withHref(null)
                        .withVCardId(vCardId)));

        //баннер c href и domain
        campaignHasDomainBanner = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pidHasDomainBanner = api.userSteps.adGroupsSteps().addDefaultGroup(campaignHasDomainBanner);
        api.userSteps.adsSteps().addDefaultTextAd(pidHasDomainBanner);
    }

    @Test
    public void emptyBannerDomainIsOkTest() {
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().exportCampaignsForMetricaNoErrors(uid, true);
        List<Long> campaignIds = extract(response.getCampaignList(), on(CampaignData.class).getCid());

        assertThat("наличие кампаний с баннером содержащим домен и с банером без домена", campaignIds,
                allOf(
                        hasItem(campaignHasNoDomainBanner),
                        hasItem(campaignHasDomainBanner))
        );
    }


    @Test
    public void emptyBannerDomainNotOkTest() {
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().exportCampaignsForMetricaNoErrors(uid, false);
        List<Long> campaignIds = extract(response.getCampaignList(), on(CampaignData.class).getCid());

        assertThat("наличие кампании с баннером содержащим домен и отсутствие кампании с банером без домена",
                campaignIds,
                allOf(
                        hasItem(campaignHasDomainBanner),
                        not(hasItem(campaignHasNoDomainBanner)))
        );
    }

    @Test
    public void emptyBannerDomainDefaultTest() {
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().exportCampaignsForMetricaNoErrors(uid);
        List<Long> campaignIds = extract(response.getCampaignList(), on(CampaignData.class).getCid());

        assertThat("наличие кампании с баннером содержащим домен и отсутствие кампании с банером без домена",
                campaignIds,
                allOf(
                        hasItem(campaignHasDomainBanner),
                        not(hasItem(campaignHasNoDomainBanner)))
        );
    }
}

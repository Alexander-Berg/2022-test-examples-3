package ru.yandex.direct.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.old.OldAbstractBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignForNotifyUrlMonitoring;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestWalletCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMcBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;

@CoreTest
@RunWith(Parameterized.class)
public class UrlMonitoringCampaignRepositoryTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private ClientOptionsRepository clientOptionsRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TestWalletCampaignRepository testWalletCampaignRepository;

    @Parameterized.Parameter
    public CampaignType campaignType;

    // Номер для уникальности domain name между типами
    @Parameterized.Parameter(1)
    public int typeNumber;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT, 1},
                {CampaignType.DYNAMIC, 2},
                {CampaignType.MCBANNER, 3},
        });
    }

    @Test
    public void campaignsWithZeroBalanceShouldNotBeIncluded() {
        final Campaign campaign =
                activeCampaignByCampaignType(campaignType)
                        .withStatusMetricaControl(true)
                        .withFinishTime(today())
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain1" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "1niamodeuqinuym.www")
                        .withHref("https://www.myuniquedomain1" + typeNumber + ".com"),
                campaignInfo
        );

        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(),
                        singleton(Pair.of("https", domain)));

        assertThat(campaignsForNotifyUrlMonitoring, equalTo(emptyMap()));
    }

    @Test
    public void campaignsWithZeroBalanceOnWalletShouldNotBeIncluded() {
        final CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeWalletCampaign(null, null)
                                .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));

        final Campaign campaign =
                activeCampaignByCampaignType(campaignType)
                        .withStatusMetricaControl(true)
                        .withFinishTime(today())
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletCampaign.getCampaignId()));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, walletCampaign.getClientInfo());
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain2" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "2niamodeuqinuym.www")
                        .withHref("http://www.myuniquedomain2" + typeNumber + ".com"),
                campaignInfo
        );

        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(),
                        singleton(Pair.of("http", domain)));

        assertThat(campaignsForNotifyUrlMonitoring, equalTo(emptyMap()));
    }

    @Test
    public void campaignsWithZeroBalanceOnWalletAndAutoOverdraftSwitchedOnShouldBeIncluded() {
        final CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeWalletCampaign(null, null)
                                .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));

        final Campaign campaign =
                activeCampaignByCampaignType(campaignType)
                        .withStatusMetricaControl(true)
                        .withFinishTime(today())
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletCampaign.getCampaignId()));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, walletCampaign.getClientInfo());
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain3" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "3niamodeuqinuym.www")
                        .withHref("https://www.myuniquedomain3" + typeNumber + ".com"),
                campaignInfo
        );

        // enable auto overdraft
        BigDecimal limit = BigDecimal.ONE;
        ClientId clientId = walletCampaign.getClientInfo().getClientId();
        Integer shard = walletCampaign.getShard();
        steps.clientSteps().setOverdraftOptions(shard, clientId, limit, BigDecimal.ZERO, null);
        clientOptionsRepository.updateAutoOverdraftLimit(shard, clientId, limit);

        // fire
        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("https", domain)));
        Set<Long> campaignIds = campaignsForNotifyUrlMonitoring.get(bannerInfo.getUid()).get(domain).stream()
                .map(CampaignForNotifyUrlMonitoring::getId)
                .collect(Collectors.toSet());

        assertThat(campaignIds, equalTo(singleton(bannerInfo.getCampaignId())));
    }

    @Test
    public void campaignsWithZeroBalanceOnWalletAndAutoOverdraftOffShouldNotBeIncluded() {
        final CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeWalletCampaign(null, null)
                                .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));

        final Campaign campaign =
                activeCampaignByCampaignType(campaignType)
                        .withStatusMetricaControl(true)
                        .withFinishTime(today())
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletCampaign.getCampaignId()));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, walletCampaign.getClientInfo());
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain4" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "4niamodeuqinuym.www")
                        .withHref("http://www.myuniquedomain4" + typeNumber + ".com"),
                campaignInfo
        );

        // enable auto overdraft
        BigDecimal limit = BigDecimal.ONE;
        ClientId clientId = walletCampaign.getClientInfo().getClientId();
        Integer shard = walletCampaign.getShard();
        steps.clientSteps().setOverdraftOptions(shard, clientId, limit, BigDecimal.ZERO, null);

        // fire
        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("http", domain)));

        assertThat(campaignsForNotifyUrlMonitoring, equalTo(emptyMap()));
    }

    @Test
    public void campaignsWithPositiveBalanceOnWalletShouldBeIncluded() {
        final CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeWalletCampaign(null, null));

        testWalletCampaignRepository.addWalletWithTotalSum(walletCampaign.getShard(), walletCampaign.getCampaignId(),
                walletCampaign.getCampaign().getBalanceInfo().getSum());

        final Campaign campaign =
                activeCampaignByCampaignType(campaignType)
                        .withStatusMetricaControl(true)
                        .withFinishTime(today())
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletCampaign.getCampaignId()));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, walletCampaign.getClientInfo());

        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain5" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "5niamodeuqinuym.www")
                        .withHref("https://www.myuniquedomain5" + typeNumber + ".com"),
                campaignInfo
        );
        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("https", domain)));
        Set<Long> campaignIds = campaignsForNotifyUrlMonitoring.get(bannerInfo.getUid()).get(domain).stream()
                .map(CampaignForNotifyUrlMonitoring::getId)
                .collect(Collectors.toSet());
        // todo: use beanDiffer
        assertThat(campaignIds, equalTo(singleton(bannerInfo.getCampaignId())));
    }

    @Test
    public void getCampaignsForNotifyUrlMonitoringRespondsWithNonEmptyResultForActiveBannerDomain() {
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain6" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "6niamodeuqinuym.www")
                        .withHref("http://www.myuniquedomain6" + typeNumber + ".com"),
                steps.campaignSteps().createCampaign(
                        activeCampaignByCampaignType(campaignType)
                                .withStatusMetricaControl(true)
                )
        );
        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("http", domain)));
        Set<Long> campaignIds = campaignsForNotifyUrlMonitoring.get(bannerInfo.getUid()).get(domain).stream()
                .map(CampaignForNotifyUrlMonitoring::getId)
                .collect(Collectors.toSet());
        // todo: use beanDiffer
        assertThat(campaignIds, equalTo(singleton(bannerInfo.getCampaignId())));
    }

    @Test
    public void statusMetricaControlNegative() {
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain6" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "6niamodeuqinuym.www")
                        .withHref("https://www.myuniquedomain6" + typeNumber + ".com"),
                steps.campaignSteps().createCampaign(
                        activeCampaignByCampaignType(campaignType)
                                .withStatusMetricaControl(false)
                )
        );
        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("https", domain)));
        assertThat(campaignsForNotifyUrlMonitoring, equalTo(emptyMap()));
    }

    @Test
    public void caseSensitivityNegative() {
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("WWW.myuniquedomain6" + typeNumber + ".coM")
                        .withReverseDomain("Moc." + typeNumber + "6niamodeuqinuym.WWW")
                        .withHref("https://WWW.myuniquedomain6" + typeNumber + ".coM"),
                steps.campaignSteps().createCampaign(
                        activeCampaignByCampaignType(campaignType)
                                .withStatusMetricaControl(true)
                )
        );
        String domain = "www.myuniquedomain6.com";
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("https", domain)));
        assertThat(campaignsForNotifyUrlMonitoring, equalTo(emptyMap()));
    }

    @Test
    public void protocolNegative() {
        var bannerInfo = steps.bannerSteps().createBanner(
                createActiveMcBannerByCampaignType()
                        .withDomain("www.myuniquedomain7" + typeNumber + ".com")
                        .withReverseDomain("moc." + typeNumber + "7niamodeuqinuym.www")
                        .withHref("https://www.myuniquedomain7" + typeNumber + ".com"),
                steps.campaignSteps().createCampaign(
                        activeCampaignByCampaignType(campaignType)
                                .withStatusMetricaControl(true)
                )
        );
        String domain = bannerInfo.getBanner().getDomain();
        Map<Long, Map<String, List<CampaignForNotifyUrlMonitoring>>> campaignsForNotifyUrlMonitoring =
                campaignRepository.getCampaignsForNotifyUrlMonitoring(bannerInfo.getShard(), singleton(Pair.of("http", domain)));
        assertThat(campaignsForNotifyUrlMonitoring, equalTo(emptyMap()));
    }

    private LocalDate today() {
        return LocalDate.now();
    }

    private OldAbstractBanner createActiveMcBannerByCampaignType() {
        switch (campaignType) {
            case MCBANNER:
                return activeMcBanner();
            case DYNAMIC:
                return activeDynamicBanner();
            default:
                return activeTextBanner();
        }
    }
}

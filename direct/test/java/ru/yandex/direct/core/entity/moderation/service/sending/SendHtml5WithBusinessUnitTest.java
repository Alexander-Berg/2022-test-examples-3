package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerRequestData;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.repository.AdGroupMappings.geoToDb;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestProducts.defaultProduct;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringRunner.class)
public class SendHtml5WithBusinessUnitTest {
    public static final List<Long> DEFAULT_GEO = List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT,
            -FAR_EASTERN_DISTRICT);
    public static final List<Long> DEFAULT_GEO_EXPANDED = List.of(NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT,
            URAL_DISTRICT, SOUTH_DISTRICT, NORTH_CAUCASIAN_DISTRICT);
    public static final Integer DEFAULT_GEO_TYPE = REGION_TYPE_DISTRICT;

    final static long BISUNESS_UNIT_ID = 62839173L;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private CpmHtml5BannerSender cpmHtml5BannerSender;

    private int shard;
    private CpmBannerInfo cpmBannerInfo;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private ClientId clientId;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClientAndUser();


        var product = defaultProduct()
                .withId((long) nextInt())
                .withType(ProductType.CPM_BANNER)
                .withUnitName("SendHtml5WithBusinessUnitTest")
                .withCurrencyCode(clientInfo.getClient().getWorkCurrency())
                .withBusinessUnit(BISUNESS_UNIT_ID);

        steps.productSteps().addProductsIfNotExists(Set.of(product));

        var pricePackage = approvedPricePackage()
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(DEFAULT_GEO)
                        .withGeoType(DEFAULT_GEO_TYPE)
                        .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withProductId(product.getId())
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(clientInfo)));

        steps.pricePackageSteps().createPricePackage(pricePackage);

        CpmPriceCampaign campaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage);
        steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, campaign);

        long campaignId = campaign.getId();

        CampaignInfo campaignInfo = new CampaignInfo(clientInfo,
                TestCampaigns.activeCpmPriceCampaign(clientInfo.getClientId(), clientInfo.getUid()));

        campaignInfo.getCampaign().setId(campaignId);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);

        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        long adGroupId = adGroupInfo.getAdGroupId();

        creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(clientInfo, steps.creativeSteps().getNextCreativeId());

        cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignId, adGroupId, creativeInfo.getCreativeId())
                        .withStatusModerate(OldBannerStatusModerate.READY),
                adGroupInfo);

        shard = cpmBannerInfo.getShard();
        clientId = cpmBannerInfo.getClientId();
        bannerRepository.moderation.updateStatusModerate(shard, singletonList(cpmBannerInfo.getBannerId()),
                BannerStatusModerate.READY);
    }

    @Test
    public void makeHtml5ModerationRequests_RequestDataIsCorrect() {
        var banner = cpmBannerInfo.getBanner();

        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        Html5BannerRequestData actual = requests.get(0).getData();

        Creative creative = creativeInfo.getCreative();
        Html5BannerRequestData expected = new Html5BannerRequestData();

        expected.setDomain(banner.getDomain());
        expected.setLinks(List.of(
                new BannerLink()
                        .setHref(banner.getHref())
                        .setParametrizedHref(banner.getHref())
        ));
        var expectedGeo = cpmBannerInfo.getAdGroupInfo().getAdGroup().getGeo();
        expected.setGeo(geoToDb(expectedGeo));
        expected.setCreativeId(creative.getId());
        expected.setPreviewUrl(creative.getPreviewUrl());
        expected.setArchiveUrl(creative.getArchiveUrl());
        expected.setLivePreviewUrl(creative.getLivePreviewUrl());
        expected.setSimplePicture(creative.getIsGenerated());
        expected.setAspectRatio(new AspectRatio(creative.getWidth(), creative.getHeight()));
        expected.setBusinessUnit(BISUNESS_UNIT_ID);
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected).useCompareStrategy(
                // валюту не проверяем, она отправляется по историческим причинам, модерация на неё не смотрит
                allFieldsExcept(newPath("currency"))
        ));
    }

    private List<Html5BannerModerationRequest> makeCpmHtml5ModerationRequests(int shard, List<Long> bids) {
        Consumer<List<Html5BannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<Html5BannerModerationRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        cpmHtml5BannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());

        return requestsCaptor.getValue();
    }

}

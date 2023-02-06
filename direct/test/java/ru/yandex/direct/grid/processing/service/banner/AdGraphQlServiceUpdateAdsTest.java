package ru.yandex.direct.grid.processing.service.banner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerTurboAppType;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerTurboApp;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboAppsRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboGalleriesRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.sitelink.turbolanding.model.SitelinkTurboLanding;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.ImageCreativeBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdPriceCurrency;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.GdTurboGalleryParams;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.constants.GdButtonAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxLengthWordTemplateMarker;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_TITLE_WORD;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultCpcVideoAd;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultCreativeAd;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getSimpleTextAd;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateAdsTest {
    private static final String PREVIEW_MUTATION = "updateAds";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String UPDATE_ADS_MUTATION = "updateAds";
    private static final String TURBO_GALLERY_HREF = "https://yandex.ru/turbo?text";
    private static final String HREF = "https://yandex.ru";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OldBannerTurboGalleriesRepository turboGalleriesRepository;

    @Autowired
    private OldBannerTurboAppsRepository bannerTurboAppsRepository;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private BannerService bannerService;

    private int shard;
    private long bannerId;
    private AdGroupInfo defaultAdGroup;
    private User operator;
    private TextBannerInfo defaultBanner;
    private Long gdUpdateTurbolandingId;
    private ClientId clientId;
    private Long chiefUid;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        defaultBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        bannerId = defaultBanner.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        SitelinkTurboLanding defaultSitelinkTurboLanding =
                steps.turboLandingSteps().createDefaultSitelinkTurboLanding(clientInfo.getClientId());
        gdUpdateTurbolandingId = defaultSitelinkTurboLanding.getId();
        chiefUid = rbacService.getChiefByClientId(clientId);
    }

    @Test
    public void updateBanners() {

        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withTurbolandingId(gdUpdateTurbolandingId));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);
    }

    @Test
    public void updateAd_afterModeration() {
        var flags = BannerFlags.fromSource("medicine");
        steps.bannerSteps().setFlags(shard, bannerId, flags);
        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withTurbolandingId(gdUpdateTurbolandingId));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);
        var result = bannerService.getBannersByIds(List.of(bannerId)).get(0);
        assertThat(result.getFlags())
                .is(matchedBy(beanDiffer(flags)));
    }

    @Test
    public void updateBanner_addTurboGalleryHref() {
        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withTurboGalleryParams(new GdTurboGalleryParams().withTurboGalleryHref(TURBO_GALLERY_HREF)));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        String addedTurboGalleryHref = turboGalleriesRepository.getTurboGalleriesByBannerIds(shard,
                singletonList(bannerId)).get(bannerId);
        assertThat(addedTurboGalleryHref).isEqualTo(TURBO_GALLERY_HREF);
    }

    @Test
    public void updateBanner_notUpdatedTurboGalleryHref() {
        OldBanner bannerWithTurboGalleryHref = steps.bannerSteps()
                .createBanner(activeTextBanner().withTurboGalleryHref(TURBO_GALLERY_HREF), defaultAdGroup)
                .getBanner();
        Long bannerId = bannerWithTurboGalleryHref.getId();
        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withTurboGalleryParams(null));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        String addedTurboGalleryHref = turboGalleriesRepository.getTurboGalleriesByBannerIds(shard,
                singletonList(bannerId)).get(bannerId);
        assertThat(addedTurboGalleryHref)
                .describedAs("Ссылка на турбо-галерею не удалена")
                .isEqualTo(TURBO_GALLERY_HREF);
    }


    @Test
    public void updateBannerPrice() {
        GdAdPrice adPrice = new GdAdPrice().withPrice("123.00").withCurrency(GdAdPriceCurrency.RUB);
        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withAdPrice(adPrice));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(defaultBanner.getShard(), bannerId);
        assertThat(actualPrice).isNotNull();
        assertThat(actualPrice.getCurrency()).isEqualTo(OldBannerPricesCurrency.valueOf(adPrice.getCurrency().name()));
        assertThat(actualPrice.getPrice()).isEqualTo(new BigDecimal(adPrice.getPrice()));
    }


    @Test
    public void updateBannerPrice_bannerWithTurboApp() {
        Long turboAppInfoId = steps.turboAppSteps().addDefaultTurboAppInfo(shard, clientId.asLong());
        OldBannerTurboApp bannerTurboApp = new OldBannerTurboApp()
                .withBannerId(bannerId)
                .withBannerTurboAppType(BannerTurboAppType.FEATURE)
                .withTurboAppInfoId(turboAppInfoId);
        steps.turboAppSteps().addBannerTurboApp(shard, bannerTurboApp);
        GdAdPrice adPrice = new GdAdPrice().withPrice("123.00").withCurrency(GdAdPriceCurrency.RUB);
        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withAdPrice(adPrice));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        OldBannerTurboApp actualTurboApp = bannerTurboAppsRepository.getTurboAppByBannerIds(shard, List.of(bannerId)).get(0);
        assertThat(actualTurboApp.getBannerTurboAppType()).isEqualTo(BannerTurboAppType.OFFER);
    }

    @Test
    public void updateCpcVideoBanners() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        creativeId = steps.creativeSteps().addDefaultCpcVideoCreative(defaultAdGroup.getClientInfo(), creativeId)
                .getCreativeId();
        CpcVideoBannerInfo defaultCpCVideoBanner =
                steps.bannerSteps().createDefaultCpcVideoBanner(defaultAdGroup, creativeId);

        Long bannerId = defaultCpCVideoBanner.getBannerId();
        GdUpdateAds gdUpdateAds = createUpdateRequest(getDefaultCpcVideoAd()
                .withId(bannerId)
                .withCreativeId(creativeId)
                .withTurbolandingId(gdUpdateTurbolandingId));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);
    }

    @Test
    public void updateImageCreativeBanners() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        creativeId = steps.creativeSteps().addDefaultCanvasCreative(defaultAdGroup.getClientInfo(), creativeId)
                .getCreativeId();
        ImageCreativeBannerInfo defaultImageCreativeBanner =
                steps.bannerSteps().createDefaultImageCreativeBanner(defaultAdGroup, creativeId);


        GdUpdateAds gdUpdateAds = createUpdateRequest(getDefaultCreativeAd()
                .withDomain(null)
                .withCreativeId(creativeId)
                .withId(defaultImageCreativeBanner.getBannerId())
                .withTurbolandingId(gdUpdateTurbolandingId));

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(defaultImageCreativeBanner.getBannerId(), data);
    }

    @Test
    public void updateBanner_TextBannerWithOrganization_OrganizationUpdated() {
        Long permalinkId = nextLong();

        GdUpdateAds gdUpdateAds = createUpdateRequest(getSimpleTextAd()
                .withId(bannerId)
                .withPermalinkId(permalinkId));

        String query = createQuery(gdUpdateAds);

        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(chiefUid));

        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        Organization organization = organizationRepository.getOrganizationsByBannerIds(shard, singletonList(bannerId))
                .get(bannerId);

        Assert.assertThat(organization.getPermalinkId(), is(permalinkId));
        Assert.assertThat(organization.getClientId(), is(clientId));
    }

    @Test
    public void updateBanners_OneInvalidOneValid() {
        TextBannerInfo secondBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        GdUpdateAds gdUpdateAds = new GdUpdateAds()
                .withSaveDraft(true)
                .withAdUpdateItems(asList(
                        getSimpleTextAd()
                                .withTitle(RandomStringUtils.randomAlphabetic(MAX_LENGTH_TITLE_WORD + 1))
                                .withId(defaultBanner.getBannerId()),
                        getSimpleTextAd().withId(secondBanner.getBannerId())));


        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        Path expectedPath = path(field(GdUpdateAds.AD_UPDATE_ITEMS), index(0), field(GdUpdateAd.TITLE));

        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(
                        asList(null, new GdUpdateAdPayloadItem().withId(secondBanner.getBannerId())))
                .withValidationResult(toGdValidationResult(expectedPath,
                        maxLengthWordTemplateMarker(MAX_LENGTH_TITLE_WORD))
                        .withWarnings(null));

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(UPDATE_ADS_MUTATION), GdUpdateAdsPayload.class);

        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        assertThat(gdUpdateAdsPayload)
                .is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void updateBanners_aggregatorDomain() {
        GdUpdateAd gdUpdateAd = getSimpleTextAd()
                .withId(bannerId)
                .withHref(VK_TEST_PUBLIC_HREF);

        GdUpdateAds gdUpdateAds = createUpdateRequest(gdUpdateAd);

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void updateBanners_notAggregatorDomain() {
        GdUpdateAd gdUpdateAd = getSimpleTextAd()
                .withId(bannerId);

        GdUpdateAds gdUpdateAds = createUpdateRequest(gdUpdateAd);

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void updateBanners_withoutHref() {
        GdUpdateAd gdUpdateAd = getSimpleTextAd()
                .withId(bannerId)
                .withTurbolandingId(gdUpdateTurbolandingId);

        GdUpdateAds gdUpdateAds = createUpdateRequest(gdUpdateAd);

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void updateBanners_logo() {
        String imageHash = steps.bannerSteps().createLogoImageFormat(
                defaultAdGroup.getClientInfo()).getImageHash();
        GdUpdateAd gdUpdateAd = getSimpleTextAd()
                .withId(bannerId)
                .withLogoImageHash(imageHash);

        GdUpdateAds gdUpdateAds = createUpdateRequest(gdUpdateAd);

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        TextBanner actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(defaultAdGroup.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(imageHash);
    }

    @Test
    public void updateBanners_button() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        GdUpdateAd gdUpdateAd = getSimpleTextAd()
                .withId(bannerId)
                .withButton(new GdBannerButton().withAction(GdButtonAction.DOWNLOAD).withHref(HREF));

        GdUpdateAds gdUpdateAds = createUpdateRequest(gdUpdateAd);

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        TextBanner actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(defaultAdGroup.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner.getButtonCaption()).isEqualTo("Скачать");
        assertThat(actualBanner.getButtonHref()).isEqualTo(HREF);
    }

    @Test
    public void updateBanners_name() {
        String name = "Система поиска в Сети";
        GdUpdateAd gdUpdateAd = getSimpleTextAd()
                .withId(bannerId)
                .withName(name);

        GdUpdateAds gdUpdateAds = createUpdateRequest(gdUpdateAd);

        String query = createQuery(gdUpdateAds);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerId, data);

        TextBanner actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(defaultAdGroup.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getName()).isEqualTo(name);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private Map<String, Object> processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(UPDATE_ADS_MUTATION);
        return data;
    }

    private void validateUpdateSuccessful(Long bannerId, Map<String, Object> data) {
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(
                        singletonList(new GdUpdateAdPayloadItem().withId(bannerId)));

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(UPDATE_ADS_MUTATION), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload)
                .is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));
    }

    private GdUpdateAds createUpdateRequest(GdUpdateAd... gdUpdateAd) {
        return new GdUpdateAds()
                .withSaveDraft(true)
                .withAdUpdateItems(asList(gdUpdateAd));
    }

    private String createQuery(GdUpdateAds gdUpdateAds) {
        return String.format(QUERY_TEMPLATE, PREVIEW_MUTATION, graphQlSerialize(gdUpdateAds));
    }
}

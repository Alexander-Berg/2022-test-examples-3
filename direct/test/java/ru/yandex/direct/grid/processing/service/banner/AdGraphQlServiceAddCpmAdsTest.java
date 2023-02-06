package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithBigKingImage;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusBannerLogoModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingParams;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdBannerAdditionalHref;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddCpmAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddCpmAds;
import ru.yandex.direct.grid.processing.model.cliententity.GdPixel;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.misc.lang.StringUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.INVALID_HREF;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.RESTRICTED_CHARS_IN_FIELD;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.UNSUPPORTED_BANNER_TYPE;
import static ru.yandex.direct.core.entity.banner.type.button.BannerWithButtonConstants.CAPTION_MAX_LENGTH;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_VIDEO_FRONTPAGE_TITLE;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoPinCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoproductCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.feature.FeatureName.ASSET_BIG_KING_IMAGE;
import static ru.yandex.direct.feature.FeatureName.ASSET_BUTTON_CUSTOM_TEXT;
import static ru.yandex.direct.feature.FeatureName.CPM_GEOPRODUCT_ENABLED;
import static ru.yandex.direct.feature.FeatureName.CPM_INDOOR_GROUPS_EDIT_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_BANNER;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_GEOPRODUCT;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_GEO_PIN;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_INDOOR;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_OUTDOOR;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_PRICE;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_PRICE_FRONTPAGE_VIDEO;
import static ru.yandex.direct.grid.processing.model.cliententity.GdPixelKind.AUDIENCE;
import static ru.yandex.direct.grid.processing.model.constants.GdButtonAction.CUSTOM_TEXT;
import static ru.yandex.direct.grid.processing.model.constants.GdButtonAction.DOWNLOAD;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.DefectIds.NO_RIGHTS;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class AdGraphQlServiceAddCpmAdsTest {

    private static final String MUTATION_NAME = "addCpmAds";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String TURBOLANDING_HREF_PARAMS = "turbolandingHrefParams";
    private static final String HREF = "https://yandex.ru";
    private static final String TNS_ID = "someTnsID";
    private static final String TITLE = "Title";
    private static final String EXTENTION_TITLE = "Extention title";
    private static final String BODY = "Body";
    private static final List<GdPixel> GD_PIXELS
            = singletonList(new GdPixel().withKind(AUDIENCE).withUrl(yaAudiencePixelUrl()));
    private static final List<String> PIXELS = singletonList(yaAudiencePixelUrl());
    private static final String BUTTON_CUSTOM_TEXT = "Купить зайчиков";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    private User operator;
    private int shard;
    private ClientInfo clientInfo;
    private Long chiefUid;

    private Long creativeCanvasId;
    private Long creativeOutdoorId;
    private Long creativeIndoorId;
    private Long creativeCpmGeoproductId;
    private Long creativeCpmGeoPinId;
    private Long creativeCpmPriceCreativeId;
    private Long creativeCpmPriceVideoFrontpageCreativeId;

    private Long turboLandingId;
    private final Long permalinkId = nextLong();

    private Long cpmAdGroupId;
    private Long outdoorAdGroupId;
    private Long indoorAdGroupId;
    private Long geoProductAdGroupId;
    private Long cpmGeoPinAdGroupId;
    private Long frontpageAdGroupId;
    private Long frontpageVideoAdGroupId;
    private String logoImageHash;
    private String bigKingImageHash;

    @Before
    public void before() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        chiefUid = rbacService.getChiefByClientId(clientId);
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo cpmBannerAdGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        cpmAdGroupId = cpmBannerAdGroup.getAdGroupId();
        geoProductAdGroupId = steps.adGroupSteps().createActiveCpmGeoproductAdGroup(clientInfo).getAdGroupId();
        outdoorAdGroupId = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(clientInfo).getAdGroupId();
        indoorAdGroupId = steps.adGroupSteps().createActiveCpmIndoorAdGroup(clientInfo).getAdGroupId();
        cpmGeoPinAdGroupId = steps.adGroupSteps().createActiveCpmGeoPinAdGroup(clientInfo).getAdGroupId();

        CpmPriceCampaign cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        frontpageAdGroupId = steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo)
                .getId();

        PricePackage pricePackage = approvedPricePackage()
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO, AdGroupType.CPM_YNDX_FRONTPAGE));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        CpmPriceCampaign cpmPriceCampaignFrontpagePackage = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        frontpageVideoAdGroupId = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(cpmPriceCampaignFrontpagePackage, clientInfo)
                .getId();


        creativeCanvasId = steps.creativeSteps().createCreative(defaultCanvas(null, null), clientInfo).getCreativeId();
        creativeOutdoorId = steps.creativeSteps().createCreative(
                defaultCpmOutdoorVideoAddition(null, null), clientInfo).getCreativeId();
        creativeIndoorId = steps.creativeSteps().createCreative(
                defaultCpmIndoorVideoAddition(null, null), clientInfo).getCreativeId();
        creativeCpmGeoproductId = steps.creativeSteps().createCreative(
                defaultCpmGeoproductCanvas(null, null), clientInfo).getCreativeId();
        creativeCpmGeoPinId = steps.creativeSteps().createCreative(
                defaultCpmGeoPinCanvas(null, null), clientInfo).getCreativeId();
        creativeCpmPriceCreativeId = steps.creativeSteps()
                .addDefaultHtml5CreativeForPriceSales(clientInfo, cpmPriceCampaign).getCreativeId();
        creativeCpmPriceVideoFrontpageCreativeId = steps.creativeSteps()
                .addCpmVideoFrontpageCreative(clientInfo).getCreativeId();

        OldBannerTurboLanding bannerTurboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        turboLandingId = bannerTurboLanding.getId();
        logoImageHash = steps.bannerSteps().createLogoImageFormat(cpmBannerAdGroup.getClientInfo()).getImageHash();
        bigKingImageHash = steps.bannerSteps().createBannerImageFormat(cpmBannerAdGroup.getClientInfo()).getImageHash();
        steps.featureSteps().addClientFeature(cpmBannerAdGroup.getClientId(), ASSET_BUTTON_CUSTOM_TEXT, true);
        steps.featureSteps().addClientFeature(cpmBannerAdGroup.getClientId(), ASSET_BIG_KING_IMAGE, true);
    }

    @Test
    public void addBanners_CpmBanner_whenSaveDraft() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner();
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_CpmBanner_whenForceModerate() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER);
        GdAddAdsPayload payload = processQuery(addCpmAd, false);
        validateAddSuccessful(payload);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.READY);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanner_CpmBanner_BannerWithTns() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withTnsId(TNS_ID);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withTnsId(TNS_ID);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanner_CpmBanner_BannerWithTns_ValidationCheck() {
        String path = "adAddItems[0].tnsId";

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withTnsId("tns_id");
        GdAddAdsPayload invalidTnsFormatPlayload = processQuery(addCpmAd);
        checkAssertionError(invalidTnsFormatPlayload, path, RESTRICTED_CHARS_IN_FIELD);

        GdAddCpmAd emptyPixelsaddCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withPixels(emptyList())
                .withTnsId(TNS_ID);
        processQueryAndValidate(emptyPixelsaddCpmAd);

        GdAddCpmAd wrongLengthAddCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withTnsId("a".repeat(200));
        GdAddAdsPayload wrongLengthPlayload = processQuery(wrongLengthAddCpmAd);
        var actualDefect = wrongLengthPlayload.getValidationResult().getErrors().get(0);
        Assert.assertThat(actualDefect, beanDiffer(
                        new GdDefect()
                                .withCode(LENGTH_CANNOT_BE_MORE_THAN_MAX.getCode())
                                .withPath(path)
                ).useCompareStrategy(DefaultCompareStrategies.allFieldsExcept(BeanFieldPath.newPath("params")))
        );

        GdAddCpmAd noErrorAddCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withPixels(emptyList());
        processQueryAndValidate(noErrorAddCpmAd);
    }

    @Test
    public void addBanner_CpmBanner_BannerWithButton() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withButton(new GdBannerButton().withAction(DOWNLOAD).withHref(HREF));
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonCaption("Скачать")
                .withButtonHref(HREF);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanner_CpmBanner_BannerWithButton_CustomText() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withButton(new GdBannerButton()
                        .withAction(CUSTOM_TEXT)
                        .withCustomText(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF));
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withButtonAction(ButtonAction.CUSTOM_TEXT)
                .withButtonCaption(BUTTON_CUSTOM_TEXT)
                .withButtonHref(HREF);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanner_CpmBanner_BannerWithButton_CustomText_ValidationError() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withButton(new GdBannerButton()
                        .withAction(CUSTOM_TEXT)
                        .withCustomText("+++++++++++++++++++++")
                        .withHref("abc"));
        GdAddAdsPayload payload = processQuery(addCpmAd, true);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adAddItems[0].buttonCaption").withCode(RESTRICTED_CHARS_IN_FIELD.getCode()),
                new GdDefect().withPath("adAddItems[0].buttonCaption")
                        .withCode(LENGTH_CANNOT_BE_MORE_THAN_MAX.getCode())
                        .withParams(Map.of("maxLength", CAPTION_MAX_LENGTH)),
                new GdDefect().withPath("adAddItems[0].buttonHref").withCode(INVALID_HREF.getCode())
        ));
    }

    @Test
    public void addBanner_CpmBanner_TooBigTitle_ValidationError() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_PRICE_FRONTPAGE_VIDEO)
                .withTitle(StringUtils.repeat("f", 35));

        GdAddAdsPayload payload = processQuery(addCpmAd, true);

        Assert.assertThat(payload.getValidationResult().getErrors(), Matchers.hasItem(
                new GdDefect().withPath("adAddItems[0].title")
                        .withCode(BannerDefectIds.String.TEXT_LENGTH_WITHOUT_TEMPLATE_MARKER_CANNOT_BE_MORE_THAN_MAX.getCode())
                        .withParams(Map.of("maxLength", MAX_LENGTH_VIDEO_FRONTPAGE_TITLE))
        ));
    }

    @Test
    public void addBanner_CpmBanner_BannerWithButton_CommonAction_ValidationError() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withButton(new GdBannerButton()
                        .withAction(DOWNLOAD)
                        .withHref("abc"));
        GdAddAdsPayload payload = processQuery(addCpmAd, true);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adAddItems[0].buttonHref").withCode(INVALID_HREF.getCode())
        ));
    }


    @Test
    public void addBanner_CpmBanner_WithDisableFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER);
        GdAddAdsPayload payload = processQuery(addCpmAd, true);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adAddItems[0].adGroupId").withCode(NO_RIGHTS.getCode())
        ));
    }

    @Test
    public void addBanners_CpmBanner_aggregatorDomain() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withHref(VK_TEST_PUBLIC_HREF);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withHref(VK_TEST_PUBLIC_HREF);

        checkBanner(bannerId, expectedBanner);
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void addBanners_CpmBanner_notAggregatorDomain() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner();
        checkBanner(bannerId, expectedBanner);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void addBanners_CpmBanner_withoutHref() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withHref(null);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withHref(null);
        checkBanner(bannerId, expectedBanner);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void addBanners_CpmBanner_withLogo() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withLogoImageHash(logoImageHash);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withLogoImageHash(logoImageHash)
                .withLogoStatusModerate(StatusBannerLogoModerate.NEW);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_CpmBanner_withLogoForceModerate() {
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withLogoImageHash(logoImageHash);
        GdAddAdsPayload payload = processQuery(addCpmAd, false);
        validateAddSuccessful(payload);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withLogoImageHash(logoImageHash)
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.READY)
                .withLogoStatusModerate(StatusBannerLogoModerate.READY);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_CpmBanner_withBigKing() {
        //Добавление царь баннера
        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_BANNER)
                .withBigKingImageHash(bigKingImageHash);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        BannerWithBigKingImage actualBanner = (BannerWithBigKingImage) bannerTypedRepository
                .getTyped(shard, List.of(bannerId)).get(0);
        assertThat(actualBanner.getBigKingImageHash()).isEqualTo(bigKingImageHash);
    }

    @Test
    public void addBanners_Outdoor_FeatureIsOn_Success() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA, true);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_OUTDOOR);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmOutdoorBanner expectedBanner = new OldCpmOutdoorBanner()
                .withBannerType(OldBannerType.CPM_OUTDOOR)
                .withCreativeId(creativeOutdoorId)
                .withHref(HREF)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_Outdoor_FeatureIsOff_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA, false);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_OUTDOOR);
        GdAddAdsPayload payload = processQuery(addCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adAddItems[0].adType").withCode(UNSUPPORTED_BANNER_TYPE.getCode())));
    }

    @Test
    public void addBanners_Indoor_FeatureIsOn_Success() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_INDOOR_GROUPS_EDIT_FOR_DNA, true);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_INDOOR);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmIndoorBanner expectedBanner = new OldCpmIndoorBanner()
                .withBannerType(OldBannerType.CPM_INDOOR)
                .withCreativeId(creativeIndoorId)
                .withHref(HREF)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_Indoor_FeatureIsOff_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_INDOOR_GROUPS_EDIT_FOR_DNA, false);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_INDOOR);
        GdAddAdsPayload payload = processQuery(addCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adAddItems[0].adType").withCode(UNSUPPORTED_BANNER_TYPE.getCode())));
    }

    @Test
    public void addBanners_CpmGeoproduct_FeatureIsOn_Success() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_GEOPRODUCT_ENABLED, true);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_GEOPRODUCT);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withBannerType(OldBannerType.CPM_BANNER)
                .withCreativeId(creativeCpmGeoproductId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW)
                .withTurboLandingId(turboLandingId)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams(TURBOLANDING_HREF_PARAMS))
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.NEW);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_CpmGeoproduct_FeatureIsOff_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_GEOPRODUCT_ENABLED, false);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_GEOPRODUCT);
        GdAddAdsPayload payload = processQuery(addCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adAddItems[0].adType").withCode(UNSUPPORTED_BANNER_TYPE.getCode())));
    }

    @Test
    public void addBanners_CpmGeoPin() {
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(chiefUid));

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_GEO_PIN);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();

        OldCpmGeoPinBanner expectedBanner = new OldCpmGeoPinBanner()
                .withBannerType(OldBannerType.CPM_GEO_PIN)
                .withCreativeId(creativeCpmGeoPinId)
                .withPermalinkId(permalinkId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_CpmPrice() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_PRICE)
                .withAdditionalHrefs(List.of(
                        new GdBannerAdditionalHref().withHref("http://google.com"),
                        new GdBannerAdditionalHref().withHref("http://bing.com")
                ));
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();
        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withId(bannerId)
                .withAdGroupId(frontpageAdGroupId)
                .withCreativeId(creativeCpmPriceCreativeId)
                .withBannerType(OldBannerType.CPM_BANNER)
                .withHref(HREF)
                .withAdditionalHrefs(List.of(
                        new OldBannerAdditionalHref().withHref("http://google.com"),
                        new OldBannerAdditionalHref().withHref("http://bing.com")
                ));
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_CpmPriceWithNullAdditionalHrefs() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);

        GdAddCpmAd addCpmAd = defaultAddCpmAd(CPM_PRICE)
                .withAdditionalHrefs(null);
        GdAddAdsPayload payload = processQueryAndValidate(addCpmAd);

        Long bannerId = payload.getAddedAds().get(0).getId();
        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withId(bannerId)
                .withAdGroupId(frontpageAdGroupId)
                .withCreativeId(creativeCpmPriceCreativeId)
                .withBannerType(OldBannerType.CPM_BANNER)
                .withHref(HREF)
                .withAdditionalHrefs(emptyList());
        checkBanner(bannerId, expectedBanner);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdAddCpmAd defaultAddCpmAd(GdAdType type) {
        GdAddCpmAd gdAddCpmAd = new GdAddCpmAd()
                .withAdType(type);

        if (type == CPM_BANNER) {
            gdAddCpmAd
                    .withAdGroupId(cpmAdGroupId)
                    .withTitle(TITLE)
                    .withTitleExtension(EXTENTION_TITLE)
                    .withBody(BODY)
                    .withHref(HREF)
                    .withCreativeId(creativeCanvasId)
                    .withTurbolandingId(turboLandingId)
                    .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS)
                    .withPixels(GD_PIXELS);
        } else if (type == CPM_GEOPRODUCT) {
            gdAddCpmAd
                    .withAdGroupId(geoProductAdGroupId)
                    .withCreativeId(creativeCpmGeoproductId)
                    .withTurbolandingId(turboLandingId)
                    .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS)
                    .withPixels(GD_PIXELS);
        } else if (type == CPM_GEO_PIN) {
            gdAddCpmAd
                    .withAdGroupId(cpmGeoPinAdGroupId)
                    .withHref(HREF)
                    .withCreativeId(creativeCpmGeoPinId)
                    .withPermalinkId(permalinkId);
        } else if (type == CPM_INDOOR) {
            gdAddCpmAd
                    .withAdGroupId(indoorAdGroupId)
                    .withHref(HREF)
                    .withCreativeId(creativeIndoorId);
        } else if (type == CPM_OUTDOOR) {
            gdAddCpmAd
                    .withAdGroupId(outdoorAdGroupId)
                    .withHref(HREF)
                    .withCreativeId(creativeOutdoorId);
        } else if (type == CPM_PRICE) {
            gdAddCpmAd
                    .withAdGroupId(frontpageAdGroupId)
                    .withCreativeId(creativeCpmPriceCreativeId)
                    .withHref(HREF)
                    .withTurbolandingHrefParams("");
        } else if (type == CPM_PRICE_FRONTPAGE_VIDEO) {
            gdAddCpmAd
                    .withTitle(TITLE)
                    .withAdGroupId(frontpageVideoAdGroupId)
                    .withCreativeId(creativeCpmPriceVideoFrontpageCreativeId)
                    .withHref(HREF)
                    .withTurbolandingHrefParams("")
                    .withLogoImageHash(logoImageHash);
        }

        return gdAddCpmAd;
    }

    private GdAddAdsPayload processQuery(GdAddCpmAd gdAddCpmAd) {
        return processQuery(gdAddCpmAd, true);
    }

    private GdAddAdsPayload processQueryAndValidate(GdAddCpmAd gdAddCpmAd) {
        GdAddAdsPayload payload = processQuery(gdAddCpmAd, true);
        validateAddSuccessful(payload);
        return payload;
    }

    private GdAddAdsPayload processQuery(GdAddCpmAd gdAddCpmAd, boolean saveDraft) {
        GdAddCpmAds gdAddCpmAds = new GdAddCpmAds()
                .withSaveDraft(saveDraft)
                .withAdAddItems(singletonList(gdAddCpmAd));
        String query = createQuery(gdAddCpmAds);
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdAddCpmAds gdAddCpmAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdAddCpmAds));
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);
    }

    private void validateAddSuccessful(GdAddAdsPayload actualGdAddAdsPayload) {
        assertThat(actualGdAddAdsPayload.getValidationResult()).isNull();
    }

    private OldCpmBanner defaultExpectedCpmBanner() {
        return new OldCpmBanner()
                .withBannerType(OldBannerType.CPM_BANNER)
                .withTitle(TITLE)
                .withTitleExtension(EXTENTION_TITLE)
                .withBody(BODY)
                .withCreativeId(creativeCanvasId)
                .withHref(HREF)
                .withTurboLandingId(turboLandingId)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams(TURBOLANDING_HREF_PARAMS))
                .withPixels(PIXELS)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);
    }

    private void checkBanner(Long bannerId, OldBanner expectedBanner) {
        OldBanner actualBanner = bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private void checkAssertionError(GdAddAdsPayload payload, String path, DefectId defectId) {
        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath(path).withCode(defectId.getCode())));
    }
}

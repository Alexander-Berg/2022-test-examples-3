package ru.yandex.direct.grid.processing.service.banner;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.apache.commons.beanutils.BeanUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerButton;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusBannerLogoModerate;
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
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdBannerAdditionalHref;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateCpmAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateCpmAds;
import ru.yandex.direct.grid.processing.model.cliententity.GdPixel;
import ru.yandex.direct.grid.processing.model.constants.GdButtonAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.misc.lang.StringUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerButtonStatusModerate.READY;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerButtonStatusModerate.YES;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.INVALID_HREF;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.RESTRICTED_CHARS_IN_FIELD;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.UNSUPPORTED_BANNER_TYPE;
import static ru.yandex.direct.core.entity.banner.type.button.BannerWithButtonConstants.CAPTION_MAX_LENGTH;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_VIDEO_FRONTPAGE_TITLE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoproductCanvas;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.feature.FeatureName.ASSET_BUTTON_CUSTOM_TEXT;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_BANNER;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_GEOPRODUCT;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_PRICE;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_PRICE_FRONTPAGE_VIDEO;
import static ru.yandex.direct.grid.processing.model.cliententity.GdPixelKind.AUDIENCE;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class AdGraphQlServiceUpdateCpmAdsTest {

    private static final String MUTATION_NAME = "updateCpmAds";
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
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String TURBOLANDING_HREF_PARAMS = "turbolandingHrefParams";
    private static final String HREF = "https://yandex.ru";
    private static final String HREF_1 = "https://ya.ru";
    private static final String TITLE = "Title";
    private static final String EXTENTION_TITLE = "Extention title";
    private static final String BODY = "Body";
    private static final List<GdPixel> GD_PIXELS
            = singletonList(new GdPixel().withKind(AUDIENCE).withUrl(yaAudiencePixelUrl()));
    private static final List<String> PIXELS = singletonList(yaAudiencePixelUrl());
    private static final String BUTTON_CUSTOM_TEXT = "Купить зайчиков";
    private static final String BUTTON_CUSTOM_TEXT_1 = "Купить лисичек";

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
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    @Autowired
    private FeatureSteps featureSteps;

    private User operator;
    private int shard;
    private ClientInfo clientInfo;
    private Long oldCreativeCanvasId;
    private Long oldTurboLandingId;
    private Long newCreativeCanvasId;
    private Long oldCreativeCpmGeoproductId;
    private Long newCreativeCpmGeoproductId;
    private CpmVideoAdGroup frontpageVideoFrontpageAdGroup;
    private Long creativeCpmPriceVideoFrontpageCreativeId;
    Long cpmPriceCampaignFrontpagePackageId;
    private Long newTurboLandingId;
    private AdGroupInfo adGroupInfo;
    private Long adGroupId;
    private Long campaignId;
    private String logoImageHashOld;
    private String logoImageHashNew;

    @Before
    public void before() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = adGroupInfo.getCampaignId();

        PricePackage pricePackage = approvedPricePackage()
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO, AdGroupType.CPM_YNDX_FRONTPAGE));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        CpmPriceCampaign cpmPriceCampaignFrontpagePackage = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        cpmPriceCampaignFrontpagePackageId = cpmPriceCampaignFrontpagePackage.getId();
        frontpageVideoFrontpageAdGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(cpmPriceCampaignFrontpagePackage, clientInfo);

        creativeCpmPriceVideoFrontpageCreativeId = steps.creativeSteps()
                .addCpmVideoFrontpageCreative(clientInfo).getCreativeId();
        oldCreativeCanvasId = steps.creativeSteps().createCreative(
                defaultCanvas(null, null), clientInfo).getCreativeId();
        newCreativeCanvasId = steps.creativeSteps().createCreative(
                defaultCanvas(null, null), clientInfo).getCreativeId();

        oldCreativeCpmGeoproductId = steps.creativeSteps().createCreative(
                defaultCpmGeoproductCanvas(null, null), clientInfo).getCreativeId();
        newCreativeCpmGeoproductId = steps.creativeSteps().createCreative(
                defaultCpmGeoproductCanvas(null, null), clientInfo).getCreativeId();

        OldBannerTurboLanding bannerTurboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        oldTurboLandingId = bannerTurboLanding.getId();
        OldBannerTurboLanding newBannerTurboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        newTurboLandingId = newBannerTurboLanding.getId();
        logoImageHashOld = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();
        logoImageHashNew = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();

        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), ASSET_BUTTON_CUSTOM_TEXT, true);
    }

    @Test
    public void updateBanners_cpm() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId);
        processQueryAndValidate(updateCpmAd);
        OldCpmBanner expectedBanner = defaultExpectedCpmBanner();
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateAd_afterModeration() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        var flags = BannerFlags.fromSource("medicine");
        steps.bannerSteps().setFlags(clientInfo.getShard(), bannerId, flags);

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId);
        processQueryAndValidate(updateCpmAd);
        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withFlags(flags);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateBanners_cpm_logo() {
        testLogoImage(null, null, null, null);
        testLogoImage(null, null, logoImageHashNew, StatusBannerLogoModerate.READY);
        testLogoImage(logoImageHashOld, StatusBannerLogoModerate.YES, null, null);
        testLogoImage(logoImageHashOld, StatusBannerLogoModerate.YES, logoImageHashOld, StatusBannerLogoModerate.YES);
        testLogoImage(logoImageHashOld, StatusBannerLogoModerate.YES, logoImageHashNew, StatusBannerLogoModerate.READY);
    }

    // Проверяем установку поля logoImageHash и сброс статуса модерации
    // После перехода на junit5 сделать параметризованным
    private void testLogoImage(String oldLogoHash, StatusBannerLogoModerate oldLogoStatusModerate, String newLogoHash,
                               StatusBannerLogoModerate newLogoStatusModerate) {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withLogoImageHash(oldLogoHash)
                .withLogoStatusModerate(oldLogoStatusModerate);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAd(CPM_BANNER)
                .withId(bannerId)
                .withTitle(banner.getTitle())
                .withTitleExtension(banner.getTitleExtension())
                .withBody(banner.getBody())
                .withCreativeId(banner.getCreativeId())
                .withHref(banner.getHref())
                .withLogoImageHash(newLogoHash);

        GdUpdateCpmAds gdUpdateCpmAds = new GdUpdateCpmAds()
                .withSaveDraft(false)
                .withAdUpdateItems(singletonList(updateCpmAd));

        processQueryAndGetResult(createQuery(gdUpdateCpmAds));

        //Поля логотипа проверяем явно, при их удалении стратегия onlyExpectedFields их не проверит
        OldCpmBanner actualBanner = (OldCpmBanner) getBanner(bannerId);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(newLogoHash);
        assertThat(actualBanner.getLogoStatusModerate()).isEqualTo(newLogoStatusModerate);
    }

    @Test
    public void updateBanners_Button() throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        LocaleContextHolder.setLocale(I18NBundle.RU);

        //null -> null
        testButton(null, null, null);

        //download -> null
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF)
                        .withStatusModerate(YES),
                null,
                null);

        //null -> download
        testButton(null,
                new GdBannerButton()
                        .withAction(GdButtonAction.DOWNLOAD)
                        .withHref(HREF),
                new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF)
                        .withStatusModerate(READY));

        //download -> download
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF)
                        .withStatusModerate(YES),
                new GdBannerButton()
                        .withAction(GdButtonAction.DOWNLOAD)
                        .withHref(HREF),
                new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF)
                        .withStatusModerate(YES));

        //download -> download с заменой урла
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF)
                        .withStatusModerate(YES),
                new GdBannerButton()
                        .withAction(GdButtonAction.DOWNLOAD)
                        .withHref(HREF_1),
                new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF_1)
                        .withStatusModerate(READY));

        //download -> custom_text
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.DOWNLOAD)
                        .withCaption("Скачать")
                        .withHref(HREF)
                        .withStatusModerate(YES),
                new GdBannerButton()
                        .withAction(GdButtonAction.CUSTOM_TEXT)
                        .withCustomText(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1),
                new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1)
                        .withStatusModerate(READY));

        //custom_text -> custom_text
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1)
                        .withStatusModerate(YES),
                new GdBannerButton()
                        .withAction(GdButtonAction.CUSTOM_TEXT)
                        .withCustomText(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1),
                new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1)
                        .withStatusModerate(YES));

        //custom_text -> custom_text с заменой текста
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1)
                        .withStatusModerate(YES),
                ((GdBannerButton) BeanUtils.cloneBean(new GdBannerButton()
                        .withAction(GdButtonAction.CUSTOM_TEXT)
                        .withCustomText(BUTTON_CUSTOM_TEXT_1)
                        .withHref(HREF_1))),
                new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT_1)
                        .withHref(HREF_1)
                        .withStatusModerate(READY));

        //custom_text -> custom_text с заменой текста
        testButton(new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF_1)
                        .withStatusModerate(YES),
                new GdBannerButton()
                        .withAction(GdButtonAction.CUSTOM_TEXT)
                        .withCustomText(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF),
                new OldBannerButton()
                        .withAction(ButtonAction.CUSTOM_TEXT)
                        .withCaption(BUTTON_CUSTOM_TEXT)
                        .withHref(HREF)
                        .withStatusModerate(READY));
    }

    // После перехода на junit5 сделать параметризованным
    private void testButton(OldBannerButton oldButton, GdBannerButton newButton, OldBannerButton expectedButton) {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES);

        if (oldButton != null) {
            banner.withButtonAction(oldButton.getAction())
                    .withButtonCaption(oldButton.getCaption())
                    .withButtonHref(oldButton.getHref())
                    .withButtonStatusModerate(oldButton.getStatusModerate());
        }
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAd(CPM_BANNER)
                .withId(bannerId)
                .withTitle(banner.getTitle())
                .withTitleExtension(banner.getTitleExtension())
                .withBody(banner.getBody())
                .withCreativeId(banner.getCreativeId())
                .withHref(banner.getHref())
                .withLogoImageHash(banner.getLogoImageHash())
                .withButton(newButton);

        GdUpdateCpmAds gdUpdateCpmAds = new GdUpdateCpmAds()
                .withSaveDraft(false)
                .withAdUpdateItems(singletonList(updateCpmAd));

        processQueryAndGetResult(createQuery(gdUpdateCpmAds));

        //Поля кнопки проверяем явно, при их удалении стратегия onlyExpectedFields их не проверит
        OldCpmBanner actualBanner = (OldCpmBanner) getBanner(bannerId);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ifNotNull(expectedButton, OldBannerButton::getAction));
        assertThat(actualBanner.getButtonCaption()).isEqualTo(ifNotNull(expectedButton, OldBannerButton::getCaption));
        assertThat(actualBanner.getButtonHref()).isEqualTo(ifNotNull(expectedButton, OldBannerButton::getHref));
        assertThat(actualBanner.getButtonStatusModerate()).isEqualTo(ifNotNull(expectedButton,
                OldBannerButton::getStatusModerate));
    }

    @Test
    public void updateBanners_Button_CustomText_ValidationError() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES);

        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withButton(new GdBannerButton()
                        .withAction(GdButtonAction.CUSTOM_TEXT)
                        .withCustomText("+++++++++++++++++++++")
                        .withHref("abc"));

        GdUpdateAdsPayload payload = processQuery(updateCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adUpdateItems[0].buttonCaption").withCode(RESTRICTED_CHARS_IN_FIELD.getCode()),
                new GdDefect().withPath("adUpdateItems[0].buttonCaption")
                        .withCode(LENGTH_CANNOT_BE_MORE_THAN_MAX.getCode())
                        .withParams(Map.of("maxLength", CAPTION_MAX_LENGTH)),
                new GdDefect().withPath("adUpdateItems[0].buttonHref").withCode(INVALID_HREF.getCode())
        ));
    }



    @Test
    public void updateBanner_CpmBanner_TooBigTitle_ValidationError() {
        OldCpmBanner banner = activeCpmVideoBanner(cpmPriceCampaignFrontpagePackageId, frontpageVideoFrontpageAdGroup.getId(), creativeCpmPriceVideoFrontpageCreativeId)
                .withTitle(StringUtils.repeat("f", 35))
                .withStatusModerate(OldBannerStatusModerate.YES);

        Long bannerId = bannerRepository.addBanners(clientInfo.getShard(), List.of(banner)).get(0);

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAd(CPM_PRICE_FRONTPAGE_VIDEO)
                .withId(bannerId)
                .withTitle(StringUtils.repeat("f", 35));

        GdUpdateAdsPayload payload = processQuery(updateCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), Matchers.hasItem(
                new GdDefect().withPath("adUpdateItems[0].title")
                        .withCode(BannerDefectIds.String.TEXT_LENGTH_WITHOUT_TEMPLATE_MARKER_CANNOT_BE_MORE_THAN_MAX.getCode())
                        .withParams(Map.of("maxLength", MAX_LENGTH_VIDEO_FRONTPAGE_TITLE))
        ));
    }

    @Test
    public void updateBanners_Button_CommonAction_ValidationError() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES);

        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withButton(new GdBannerButton()
                        .withAction(GdButtonAction.DOWNLOAD)
                        .withHref("abc"));

        GdUpdateAdsPayload payload = processQuery(updateCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adUpdateItems[0].buttonHref").withCode(INVALID_HREF.getCode())
        ));
    }


    @Test
    public void updateBanners_WithDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES);

        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withButton(new GdBannerButton()
                        .withAction(GdButtonAction.DOWNLOAD)
                        .withHref("abc"));

        GdUpdateAdsPayload payload = processQuery(updateCpmAd);

        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adUpdateItems[0].id").withCode(CAMPAIGN_NO_WRITE_RIGHTS.getCode())
        ));
    }

    @Test
    public void updateBanners_cpmWithTns() {
        String oldTns = "oldTnsId";
        String newTnsId = "newTnsId";

        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withTnsId(oldTns);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withTnsId(newTnsId);
        processQueryAndValidate(updateCpmAd);
        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withTnsId(newTnsId);
        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateBanner_BannerWithTns_ValidationCheck() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withTnsId("tnsId");
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withTnsId("tnsId***");
        GdUpdateAdsPayload payload = processQuery(updateCpmAd);
        String path = "adUpdateItems[0].tnsId";
        checkAssertionError(payload, path, RESTRICTED_CHARS_IN_FIELD);

        updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withPixels(emptyList())
                .withTnsId("tnsId");
        GdUpdateAdsPayload notEmptyTnsWithEmptyPixels = processQuery(updateCpmAd);
        validateUpdateSuccessful(notEmptyTnsWithEmptyPixels);

        updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withPixels(emptyList())
                .withTnsId(null);
        GdUpdateAdsPayload noError = processQuery(updateCpmAd);
        validateUpdateSuccessful(noError);
    }

    @Test
    public void updateBanners_CpmGeoproduct_FeatureIsOn_Success() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);

        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCpmGeoproductId)
                .withTurboLandingId(oldTurboLandingId)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams(TURBOLANDING_HREF_PARAMS))
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_GEOPRODUCT)
                .withId(bannerId);
        processQueryAndValidate(updateCpmAd);
        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withCreativeId(newCreativeCpmGeoproductId)
                .withTitle(null)
                .withTitleExtension(null)
                .withBody(null)
                .withHref(null);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateBanners_CpmGeoproduct_FeatureIsOff_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, false);

        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCpmGeoproductId)
                .withTurboLandingId(oldTurboLandingId)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams(TURBOLANDING_HREF_PARAMS))
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(bannerId, newCreativeCpmGeoproductId, newTurboLandingId,
                CPM_GEOPRODUCT);

        Assert.assertThat(gdUpdateAdsPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath("adUpdateItems[0].adType").withCode(UNSUPPORTED_BANNER_TYPE.getCode())
        ));
    }

    @Test
    public void updateBanners_aggregatorDomain() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withHref(HREF)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withHref(VK_TEST_PUBLIC_HREF);
        processQueryAndValidate(updateCpmAd);
        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withHref(VK_TEST_PUBLIC_HREF);

        checkBanner(bannerId, expectedBanner);
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void updateBanners_notAggregatorDomain() {
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, oldCreativeCanvasId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withHref(HREF)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();


        GdUpdateCpmAd updateCpmAd = defaultUpdateCpmAdWithTurbolanding(CPM_BANNER)
                .withId(bannerId)
                .withHref(HREF);
        processQueryAndValidate(updateCpmAd);
        OldCpmBanner expectedBanner = defaultExpectedCpmBanner()
                .withHref(HREF);

        checkBanner(bannerId, expectedBanner);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void updateBanners_CpmPrice() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CreativeInfo creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(), creative.getCreativeId())
                .withAdditionalHrefs(List.of(new OldBannerAdditionalHref().withHref("http://google.com")));
        steps.bannerSteps().createActiveCpmBannerRaw(shard, banner, adGroup);

        GdUpdateCpmAd gdUpdateCpmAd = new GdUpdateCpmAd()
                .withId(banner.getId())
                .withCreativeId(creative.getCreativeId())
                .withAdType(CPM_PRICE)
                .withHref(HREF)
                .withTurbolandingHrefParams("")
                .withAdditionalHrefs(List.of(
                        new GdBannerAdditionalHref().withHref("http://bing.com"),
                        new GdBannerAdditionalHref().withHref("http://yahoo.com")
                ));

        GdUpdateAdsPayload gdUpdateAdsPayload = processQuery(gdUpdateCpmAd);
        validateUpdateSuccessful(gdUpdateAdsPayload);

        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withId(banner.getId())
                .withAdGroupId(adGroup.getId())
                .withCreativeId(creative.getCreativeId())
                .withBannerType(OldBannerType.CPM_BANNER)
                .withHref(HREF)
                .withAdditionalHrefs(List.of(
                        new OldBannerAdditionalHref().withHref("http://bing.com"),
                        new OldBannerAdditionalHref().withHref("http://yahoo.com")
                ));
        checkBanner(banner.getId(), expectedBanner);
    }

    @Test
    public void updateBanners_CpmPriceWithNullAdditionalHrefs() {
        featureSteps.addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CreativeInfo creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(), creative.getCreativeId())
                .withAdditionalHrefs(List.of(new OldBannerAdditionalHref().withHref("http://google.com")));
        steps.bannerSteps().createActiveCpmBannerRaw(shard, banner, adGroup);

        GdUpdateCpmAd gdUpdateCpmAd = new GdUpdateCpmAd()
                .withId(banner.getId())
                .withCreativeId(creative.getCreativeId())
                .withAdType(CPM_PRICE)
                .withHref(HREF)
                .withTurbolandingHrefParams("")
                .withAdditionalHrefs(null);

        GdUpdateAdsPayload gdUpdateAdsPayload = processQuery(gdUpdateCpmAd);
        validateUpdateSuccessful(gdUpdateAdsPayload);

        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withId(banner.getId())
                .withAdGroupId(adGroup.getId())
                .withCreativeId(creative.getCreativeId())
                .withBannerType(OldBannerType.CPM_BANNER)
                .withHref(HREF)
                .withAdditionalHrefs(emptyList());
        checkBanner(banner.getId(), expectedBanner);
    }

    private GdUpdateCpmAd defaultUpdateCpmAd(GdAdType type) {
        GdUpdateCpmAd updateCpmAd = new GdUpdateCpmAd()
                .withAdType(type);

        if (type == CPM_BANNER) {
            updateCpmAd
                    .withTitle(TITLE)
                    .withTitleExtension(EXTENTION_TITLE)
                    .withBody(BODY)
                    .withHref(HREF)
                    .withCreativeId(newCreativeCanvasId)
                    .withPixels(GD_PIXELS);
        } else if (type == CPM_GEOPRODUCT) {
            updateCpmAd
                    .withTitle(TITLE)
                    .withCreativeId(newCreativeCpmGeoproductId)
                    .withPixels(GD_PIXELS);
        } else if (type == CPM_PRICE_FRONTPAGE_VIDEO) {
            updateCpmAd
                    .withTitle(TITLE)
                    .withCreativeId(creativeCpmPriceVideoFrontpageCreativeId)
                    .withHref(HREF)
                    .withTurbolandingHrefParams("")
                    .withLogoImageHash(logoImageHashOld);
        }

        return updateCpmAd;
    }

    private GdUpdateCpmAd defaultUpdateCpmAdWithTurbolanding(GdAdType type) {
        return defaultUpdateCpmAd(type)
                .withTurbolandingId(newTurboLandingId)
                .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS);
    }

    private OldCpmBanner defaultExpectedCpmBanner() {
        return new OldCpmBanner()
                .withBannerType(OldBannerType.CPM_BANNER)
                .withTitle(TITLE)
                .withTitleExtension(EXTENTION_TITLE)
                .withBody(BODY)
                .withCreativeId(newCreativeCanvasId)
                .withHref(HREF)
                .withTurboLandingId(newTurboLandingId)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams(TURBOLANDING_HREF_PARAMS))
                .withPixels(PIXELS)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW)
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.NEW);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdUpdateAdsPayload updateBanner(Long bannerId, Long creativeId, Long turboLandingId, GdAdType type) {
        return updateBanner(bannerId, creativeId, turboLandingId, type, type == CPM_GEOPRODUCT ? null : HREF);
    }

    private GdUpdateAdsPayload updateBanner(Long bannerId,
                                            Long creativeId,
                                            Long turboLandingId,
                                            GdAdType type,
                                            String href) {
        GdUpdateCpmAd gdUpdateCpmAd = new GdUpdateCpmAd()
                .withAdType(type)
                .withId(bannerId)
                .withCreativeId(creativeId)
                .withHref(href);

        if (type == CPM_BANNER || type == CPM_GEOPRODUCT) {
            gdUpdateCpmAd
                    .withTurbolandingId(turboLandingId)
                    .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS)
                    .withPixels(singletonList(new GdPixel().withKind(AUDIENCE).withUrl(yaAudiencePixelUrl())));
        }

        return processQuery(gdUpdateCpmAd);
    }

    private String createQuery(GdUpdateCpmAds adds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(adds));
    }

    private GdUpdateAdsPayload processQueryAndValidate(GdUpdateCpmAd gdUpdateCpmAd) {
        GdUpdateAdsPayload payload = processQuery(gdUpdateCpmAd);
        validateUpdateSuccessful(payload);
        return payload;
    }

    private GdUpdateAdsPayload processQuery(GdUpdateCpmAd gdUpdateCpmAd) {
        GdUpdateCpmAds gdUpdateCpmAds = new GdUpdateCpmAds()
                .withSaveDraft(true)
                .withAdUpdateItems(singletonList(gdUpdateCpmAd));

        return processQueryAndGetResult(createQuery(gdUpdateCpmAds));
    }

    private GdUpdateAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);
    }

    private void validateUpdateSuccessful(GdUpdateAdsPayload actualGdUpdateAdsPayload) {
        assertThat(actualGdUpdateAdsPayload.getValidationResult()).isNull();
    }

    private void checkBanner(Long bannerId, OldBanner expectedBanner) {
        OldBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private void checkAssertionError(GdUpdateAdsPayload gdAddAdsPayload, String path, DefectId defectId) {
        Assert.assertThat(gdAddAdsPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withPath(path).withCode(defectId.getCode())));
    }

    private OldBanner getBanner(Long bannerId) {
        return bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
    }
}

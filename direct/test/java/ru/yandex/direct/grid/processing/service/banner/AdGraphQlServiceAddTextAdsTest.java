package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.constants.GdButtonAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.i18n.I18NBundle;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddTextAdsTest {
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
            + "    addedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String ADD_ADS_MUTATION = "addAds";
    private static final String HREF = "https://yandex.ru";
    private static final String BODY = "body";
    private static final String TITLE = "title";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    private int shard;
    private User operator;
    private Long turboLandingId;
    private ClientInfo clientInfo;
    private Long creativeId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        ClientId clientId = clientInfo.getClientId();
        OldBannerTurboLanding bannerTurboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        turboLandingId = bannerTurboLanding.getId();
        creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
    }

    @Test
    public void addBanners_aggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdAddAd gdUpdateAd = getGdTextAd(adGroupInfo)
                .withHref(VK_TEST_PUBLIC_HREF);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void addBanners_notAggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdAddAd gdUpdateAd = getGdTextAd(adGroupInfo)
                .withHref(HREF);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void addBanners_withoutHref() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DESKTOP_LANDING, true);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdAddAd gdUpdateAd = getGdTextAd(adGroupInfo)
                .withTurbolandingId(turboLandingId);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void addBanners_withLogo() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        String imageHash = steps.bannerSteps().createLogoImageFormat(
                adGroupInfo.getClientInfo()).getImageHash();
        GdAddAd gdAddAd = getGdTextAd(adGroupInfo)
                .withHref(HREF)
                .withLogoImageHash(imageHash);

        GdAddAds gdAddAds = createAddRequest(gdAddAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();

        TextBanner actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(adGroupInfo.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(imageHash);
    }

    @Test
    public void addBanners_withButton() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdAddAd gdAddAd = getGdTextAd(adGroupInfo)
                .withHref(HREF)
                .withButton(new GdBannerButton().withAction(GdButtonAction.DOWNLOAD).withHref(HREF));

        GdAddAds gdAddAds = createAddRequest(gdAddAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();

        TextBanner actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(adGroupInfo.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner.getButtonCaption()).isEqualTo("Скачать");
        assertThat(actualBanner.getButtonHref()).isEqualTo(HREF);
    }

    @Test
    public void addBanners_withName() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        String name = "Система поиска в Сети";
        GdAddAd gdAddAd = getGdTextAd(adGroupInfo)
                .withHref(HREF)
                .withName(name);

        GdAddAds gdAddAds = createAddRequest(gdAddAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();

        TextBanner actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(adGroupInfo.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getName()).isEqualTo(name);
    }

    private GdAddAd getGdTextAd(AdGroupInfo adGroupInfo) {
        return new GdAddAd()
                .withAdType(GdAdType.TEXT)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCreativeId(creativeId)
                .withIsMobile(false)
                .withBody(BODY)
                .withTitle(TITLE);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(ADD_ADS_MUTATION);
        return convertValue(data.get(ADD_ADS_MUTATION), GdAddAdsPayload.class);
    }

    private GdAddAds createAddRequest(GdAddAd... gdUpdateAd) {
        return new GdAddAds()
                .withSaveDraft(true)
                .withAdAddItems(List.of(gdUpdateAd));
    }

    private String createQuery(GdAddAds gdUpdateAds) {
        return String.format(QUERY_TEMPLATE, ADD_ADS_MUTATION, graphQlSerialize(gdUpdateAds));
    }
}

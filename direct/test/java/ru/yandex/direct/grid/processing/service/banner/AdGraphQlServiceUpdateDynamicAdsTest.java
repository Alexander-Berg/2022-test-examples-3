package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateDynamicAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateDynamicAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adExtensionNotFound;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.fullDynamicBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.banner.AdGraphQlServiceAddDynamicBannerTest.BODY;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateDynamicAdsTest {

    private static final String MUTATION_NAME = "updateDynamicAds";
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

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private UserRepository userRepository;

    private ClientInfo clientInfo;
    private User operator;
    private int shard;
    private AdGroupInfo adGroupInfo;
    private Long bannerId;
    private NewDynamicBannerInfo bannerInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        Long oldCalloutId = steps.calloutSteps().createDefaultCallout(clientInfo).getId();

        var banner = fullDynamicBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withCalloutIds(singletonList(oldCalloutId));
        bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(
                new NewDynamicBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
        bannerId = bannerInfo.getBannerId();
    }

    @Test
    public void updateDynamicAds_whenSaveDraft() {
        Long vcardId = steps.vcardSteps().createVcard(adGroupInfo.getCampaignInfo()).getVcardId();
        Long sitelinkSetId = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSetId();
        Long calloutId = steps.calloutSteps().createDefaultCallout(clientInfo).getId();
        String imageHash = steps.bannerSteps().createRegularImageFormat(clientInfo).getImageHash();

        GdUpdateDynamicAd gdUpdateAd = new GdUpdateDynamicAd()
                .withId(bannerId)
                .withBody(BODY)
                .withVcardId(vcardId)
                .withSitelinksSetId(sitelinkSetId)
                .withCalloutIds(singletonList(calloutId))
                .withBannerImageHash(imageHash);

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(gdUpdateAd, true);
        checkIsSuccessful(gdUpdateAdsPayload);

        DynamicBanner expectedBanner = new DynamicBanner()
                .withStatusModerate(BannerStatusModerate.NEW)
                .withBody(BODY)
                .withVcardId(vcardId)
                .withSitelinksSetId(sitelinkSetId)
                .withCalloutIds(singletonList(calloutId))
                .withImageHash(imageHash);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateDynamicAds_whenForceModerate() {
        GdUpdateDynamicAd gdUpdateAd = new GdUpdateDynamicAd()
                .withId(bannerId)
                .withBody(BODY);

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(gdUpdateAd, false);
        checkIsSuccessful(gdUpdateAdsPayload);

        DynamicBanner expectedBanner = new DynamicBanner()
                .withStatusModerate(BannerStatusModerate.READY)
                .withBody(BODY);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateDynamicAd_afterModeration() {
        var flags = BannerFlags.fromSource("medicine");
        steps.bannerSteps().setFlags(clientInfo.getShard(), bannerId, flags);
        GdUpdateDynamicAd gdUpdateAd = new GdUpdateDynamicAd()
                .withId(bannerId)
                .withBody(BODY);

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(gdUpdateAd, false);
        checkIsSuccessful(gdUpdateAdsPayload);

        DynamicBanner expectedBanner = new DynamicBanner()
                .withStatusModerate(BannerStatusModerate.READY)
                .withFlags(flags)
                .withBody(BODY);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateDynamicAds_whenAnotherClientCallout() {
        Banner bannerBeforeUpdate = getBanner(bannerId);

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        Long anotherClientCalloutId = steps.calloutSteps().createDefaultCallout(anotherClientInfo).getId();

        GdUpdateDynamicAd gdUpdateAd = new GdUpdateDynamicAd()
                .withId(bannerId)
                .withBody(BODY)
                .withCalloutIds(singletonList(anotherClientCalloutId));

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(gdUpdateAd, true);

        GdDefect expectedGdDefect = toGdDefect(
                path(field(GdUpdateDynamicAds.AD_UPDATE_ITEMS), index(0), field(GdUpdateDynamicAd.CALLOUT_IDS)),
                adExtensionNotFound(anotherClientCalloutId),
                true);
        assertThat(gdUpdateAdsPayload.getValidationResult().getErrors()).containsExactly(expectedGdDefect);

        checkBanner(bannerId, bannerBeforeUpdate);
    }

    @Test
    public void updateDynamicAds_whenWrongBannerLanguageUkrainian() {
        // 10857 - Калининградская область, из реального кейса
        steps.campaignSteps().setCampaignProperty(adGroupInfo.getCampaignInfo(), Campaign.GEO, singleton(10857));
        steps.adGroupSteps().setAdGroupProperty(adGroupInfo, AdGroup.GEO, singletonList(10857L));
        steps.bannerSteps().setLanguage(bannerInfo, Language.RU_);
        Banner bannerBeforeUpdate = getBanner(bannerId);

        GdUpdateDynamicAd gdUpdateAd = new GdUpdateDynamicAd()
                .withId(bannerId)
                .withBody("українська мова");
        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(gdUpdateAd, true);

        List<GdDefect> errors = gdUpdateAdsPayload.getValidationResult().getErrors();
        checkState(errors.size() == 1, "An error were expected");
        var errorCode = errors.get(0).getCode();
        assertThat(errorCode).as("errorCode")
                .isEqualTo("BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO");
        checkBanner(bannerId, bannerBeforeUpdate);
    }

    @Test
    public void updateDynamicAds_whenWrongBannerLanguageTurkish() {
        steps.campaignSteps().setCampaignProperty(adGroupInfo.getCampaignInfo(), Campaign.GEO,
                singleton((int) Region.MOSCOW_REGION_ID));
        steps.adGroupSteps().setAdGroupProperty(adGroupInfo, AdGroup.GEO, singletonList(Region.MOSCOW_REGION_ID));
        steps.bannerSteps().setLanguage(bannerInfo, Language.RU_);
        Banner bannerBeforeUpdate = getBanner(bannerId);

        GdUpdateDynamicAd gdUpdateAd = new GdUpdateDynamicAd()
                .withId(bannerId)
                .withBody("Türkçe");
        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(gdUpdateAd, true);

        List<GdDefect> errors = gdUpdateAdsPayload.getValidationResult().getErrors();
        checkState(errors.size() == 1, "An error were expected");
        var errorCode = errors.get(0).getCode();
        assertThat(errorCode).as("errorCode")
                .isEqualTo("BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO");
        checkBanner(bannerId, bannerBeforeUpdate);
    }

    private GdUpdateAdsPayload updateBanner(GdUpdateDynamicAd gdUpdateAd, boolean saveDraft) {
        GdUpdateDynamicAds gdUpdateAds = new GdUpdateDynamicAds()
                .withSaveDraft(saveDraft)
                .withAdUpdateItems(singletonList(gdUpdateAd));

        String query = createQuery(gdUpdateAds);
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdUpdateDynamicAds gdUpdateAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdUpdateAds));
    }

    private GdUpdateAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);
    }

    private void checkIsSuccessful(GdUpdateAdsPayload actualGdUpdateAdsPayload) {
        assertThat(actualGdUpdateAdsPayload.getValidationResult()).isNull();
    }

    private void checkBanner(Long bannerId, Banner expectedBanner) {
        Banner actualBanner = getBanner(bannerId);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private Banner getBanner(Long bannerId) {
        return bannerTypedRepository.getTyped(shard, singletonList(bannerId)).get(0);
    }
}

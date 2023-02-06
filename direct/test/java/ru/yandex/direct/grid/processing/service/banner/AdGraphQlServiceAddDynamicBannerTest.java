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

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddDynamicAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddDynamicAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adExtensionNotFound;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
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
public class AdGraphQlServiceAddDynamicBannerTest {

    static final String BODY = "test body";

    private static final String MUTATION_NAME = "addDynamicAds";
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

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private UserRepository userRepository;

    private int shard;
    private ClientInfo clientInfo;
    private User operator;
    private AdGroupInfo adGroupInfo;
    private Long adGroupId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void addDynamicAds_whenSaveDraft() {
        Long vcardId = steps.vcardSteps().createVcard(adGroupInfo.getCampaignInfo()).getVcardId();
        Long sitelinkSetId = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSetId();
        Long calloutId = steps.calloutSteps().createDefaultCallout(clientInfo).getId();
        String imageHash = steps.bannerSteps().createRegularImageFormat(clientInfo).getImageHash();

        GdAddDynamicAd gdAddAd = new GdAddDynamicAd()
                .withAdGroupId(adGroupId)
                .withBody(BODY)
                .withVcardId(vcardId)
                .withSitelinksSetId(sitelinkSetId)
                .withCalloutIds(singletonList(calloutId))
                .withBannerImageHash(imageHash);

        GdAddAdsPayload gdAddAdsPayload = addBanner(gdAddAd, true);
        checkIsSuccessful(gdAddAdsPayload);
        Long bannerId = gdAddAdsPayload.getAddedAds().get(0).getId();

        OldDynamicBanner expectedBanner = new OldDynamicBanner()
                .withBannerType(OldBannerType.DYNAMIC)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withAdGroupId(adGroupId)
                .withBody(BODY)
                .withVcardId(vcardId)
                .withSitelinksSetId(sitelinkSetId)
                .withCalloutIds(singletonList(calloutId))
                .withBannerImage(new OldBannerImage().withImageHash(imageHash));

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addDynamicAds_whenForceModerate() {
        GdAddDynamicAd gdAddAd = new GdAddDynamicAd()
                .withAdGroupId(adGroupId)
                .withBody(BODY);

        GdAddAdsPayload gdAddAdsPayload = addBanner(gdAddAd, false);
        checkIsSuccessful(gdAddAdsPayload);
        Long bannerId = gdAddAdsPayload.getAddedAds().get(0).getId();

        OldDynamicBanner expectedBanner = new OldDynamicBanner()
                .withBannerType(OldBannerType.DYNAMIC)
                .withStatusModerate(OldBannerStatusModerate.READY)
                .withAdGroupId(adGroupId)
                .withBody(BODY);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addDynamicAds_whenAnotherClientCallout() {
        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        Long anotherClientCalloutId = steps.calloutSteps().createDefaultCallout(anotherClientInfo).getId();

        GdAddDynamicAd gdAddAd = new GdAddDynamicAd()
                .withAdGroupId(adGroupId)
                .withBody(BODY)
                .withCalloutIds(singletonList(anotherClientCalloutId));

        GdAddAdsPayload gdAddAdsPayload = addBanner(gdAddAd, true);

        GdDefect expectedGdDefect = toGdDefect(
                path(field(GdAddDynamicAds.AD_ADD_ITEMS), index(0), field(GdAddDynamicAd.CALLOUT_IDS)),
                adExtensionNotFound(anotherClientCalloutId),
                true);
        assertThat(gdAddAdsPayload.getValidationResult().getErrors()).containsExactly(expectedGdDefect);

        List<Banner> banners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(adGroupId));
        assertThat(banners).isEmpty();
    }

    private GdAddAdsPayload addBanner(GdAddDynamicAd gdAddAd, boolean saveDraft) {
        GdAddDynamicAds gdAddAds = new GdAddDynamicAds()
                .withSaveDraft(saveDraft)
                .withAdAddItems(singletonList(gdAddAd));

        String query = createQuery(gdAddAds);
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdAddDynamicAds gdAddAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdAddAds));
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);
    }

    private void checkIsSuccessful(GdAddAdsPayload actualGdAddAdsPayload) {
        assertThat(actualGdAddAdsPayload.getValidationResult()).isNull();
    }

    private void checkBanner(Long bannerId, OldBanner expectedBanner) {
        OldBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private OldBanner getBanner(Long bannerId) {
        return bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
    }
}

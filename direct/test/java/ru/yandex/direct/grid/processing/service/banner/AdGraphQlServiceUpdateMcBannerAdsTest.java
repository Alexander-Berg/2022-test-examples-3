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

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldMcBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateMcBannerAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateMcBannerAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMcBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultMcBannerImageFormat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateMcBannerAdsTest {

    private static final String MUTATION_NAME = "updateMcBannerAds";
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
    private static final String HREF = "https://yandex.ru";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private BannerTypedRepository bannerRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    private User operator;
    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void updateBanners_aggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash();
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);

        OldMcBanner banner = activeMcBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(bannerId, imageHash, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isEqualTo(VK_TEST_PUBLIC_HREF);
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void updateBanners_notAggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash();
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        OldMcBanner banner = activeMcBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(bannerId, imageHash, HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isEqualTo(HREF);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    public void updateBanners_whenFlagsAlreadyExist() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        String imageHash = createBannerImageHash();
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        var flags = BannerFlags.fromSource("medicine");

        OldMcBanner banner = activeMcBanner(campaignId, adGroupId)
                .withImage(image)
                .withFlags(flags);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(bannerId, imageHash, HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        var actualBanner = bannerRepository.getStrictly(shard, singletonList(bannerId), McBanner.class).get(0);
        assertThat(actualBanner.getFlags()).is(matchedBy(beanDiffer(flags)));
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdUpdateAdsPayload updateBanner(Long bannerId, String imageHash, String href) {
        GdUpdateMcBannerAd gdAddAd = new GdUpdateMcBannerAd()
                .withAdType(GdAdType.MCBANNER)
                .withId(bannerId)
                .withAdImageHash(imageHash)
                .withHref(href);

        GdUpdateMcBannerAds gdUpdateAds = new GdUpdateMcBannerAds()
                .withSaveDraft(true)
                .withAdUpdateItems(singletonList(gdAddAd));

        String query = String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdUpdateAds));
        return processQueryAndGetResult(query);
    }

    private GdUpdateAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);
    }

    private void validateAddSuccessful(GdUpdateAdsPayload actualGdUpdateAdsPayload) {
        assertThat(actualGdUpdateAdsPayload.getValidationResult()).isNull();
    }

    private BannerWithHref getBanner(Long bannerId) {
        return bannerRepository.getStrictly(shard, singletonList(bannerId), BannerWithHref.class).get(0);
    }

    private String createBannerImageHash() {
        return steps.bannerSteps()
                .createBannerImageFormat(clientInfo, defaultMcBannerImageFormat(null))
                .getImageHash();
    }
}

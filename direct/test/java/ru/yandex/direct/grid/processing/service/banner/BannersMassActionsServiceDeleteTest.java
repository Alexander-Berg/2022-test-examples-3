package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassAction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassActionPayload;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersMassActionsServiceDeleteTest extends BannersMassActionsServiceBaseTest {

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    private static final String BANNER_DELETE_MUTATION_NAME = "deleteAds";

    @Test
    public void deleteBannersTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                        .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY),
                clientInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.SENDING);
        OldBanner activeSyncedTextBanner = createTextBanner(adGroupInfo, true, false, true, OldBannerStatusModerate.YES);
        OldBanner draftTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.NEW);
        OldBanner suspendedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.SENT);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_DELETE_MUTATION_NAME,
                graphQlSerialize(
                        createRequest(activeTextBanner, activeSyncedTextBanner, draftTextBanner, suspendedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(activeTextBanner, draftTextBanner, suspendedTextBanner).map(OldBanner::getId).toList())
                .withSkippedAdIds(StreamEx.of(activeSyncedTextBanner).map(OldBanner::getId).toList())
                .withSuccessCount(3)
                .withTotalCount(4);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_DELETE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_DELETE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void suspendDifferentBannersTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo textAdGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY), clientInfo);
        AdGroupInfo smartAdGroupInfo = steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(null, feedId)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY), clientInfo);
        AdGroupInfo mcBannerAdGroupInfo = steps.adGroupSteps().createAdGroup(activeMcBannerAdGroup(null)
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY), clientInfo);

        OldBanner activeTextBanner = createTextBanner(textAdGroupInfo, true, false, false, OldBannerStatusModerate.NEW);
        OldBanner activeSmartBanner = createSmartBanner(smartAdGroupInfo, true, false, false, OldBannerStatusModerate.NEW);
        OldBanner activeMcBanner = createMcBanner(mcBannerAdGroupInfo, true, false, false, OldBannerStatusModerate.NEW);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_DELETE_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, activeSmartBanner, activeMcBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(activeTextBanner, activeSmartBanner, activeMcBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSkippedAdIds(emptyList())
                .withSuccessCount(3)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_DELETE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_DELETE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void deleteBannersByNotOwnerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);

        OldBanner suspendedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_DELETE_MUTATION_NAME,
                graphQlSerialize(createRequest(suspendedTextBanner, activeTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(emptyList())
                .withSkippedAdIds(
                        StreamEx.of(suspendedTextBanner, activeTextBanner).map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(2);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_DELETE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_DELETE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void deleteBannersWithHrefWithAggregatorDomain() {
        TestAuthHelper.setDirectAuthentication(operator);

        TextAdGroup adGroup = activeTextAdGroup()
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, clientInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.SENDING, VK_TEST_PUBLIC_HREF);

        OldBanner activeMcBanner = createMcBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.SENDING, VK_TEST_PUBLIC_HREF);

        OldBanner imageHashBanner = createImageHashBanner(adGroupInfo, VK_TEST_PUBLIC_HREF);

        Integer shard = adGroupInfo.getShard();
        Long imageCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCanvasCreative(clientInfo, imageCreativeId);
        OldBanner imageCreativeBanner = createImageCreativeBanner(adGroupInfo, imageCreativeId, VK_TEST_PUBLIC_HREF);

        Long cpmCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCanvasCreative(clientInfo, cpmCreativeId);
        OldBanner cpmBannerBanner = createCpmBanner(adGroupInfo, cpmCreativeId, VK_TEST_PUBLIC_HREF);

        Long cpmVideoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, cpmVideoCreativeId);
        OldBanner cpmVideoBanner = createCpmVideoBanner(adGroupInfo, cpmVideoCreativeId, VK_TEST_PUBLIC_HREF);

        OldBanner mobileAppBanner = createMobileAppBanner(adGroupInfo, VK_TEST_PUBLIC_HREF);

        OldBanner dynamicBanner = createDynamicBanner(adGroupInfo, VK_TEST_PUBLIC_HREF);

        List<OldBanner> banners = List.of(activeTextBanner,
                activeMcBanner,
                imageHashBanner,
                imageCreativeBanner,
                cpmBannerBanner,
                cpmVideoBanner,
                mobileAppBanner,
                dynamicBanner);

        int bannersCount = banners.size();
        List<Long> bannerIds = mapList(banners, OldBanner::getId);
        var aggregatorDomainsBeforeDelete = aggregatorDomainsRepository.getAggregatorDomains(shard, bannerIds);
        assumeThat(aggregatorDomainsBeforeDelete.keySet(), hasSize(bannersCount));
        assumeThat(aggregatorDomainsBeforeDelete.values(), Matchers.everyItem(is(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN)));

        GdAdsMassAction request = createRequest(bannerIds);
        String serialized = graphQlSerialize(request);
        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE, BANNER_DELETE_MUTATION_NAME, serialized);

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(bannerIds)
                .withSkippedAdIds(emptyList())
                .withSuccessCount(bannersCount)
                .withTotalCount(bannersCount);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap).containsOnlyKeys(BANNER_DELETE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload = GraphQlJsonUtils.convertValue(
                actualPayloadMap.get(BANNER_DELETE_MUTATION_NAME),
                GdAdsMassActionPayload.class);

        assertThat(actualPayload).is(matchedBy(beanDiffer(expectedPayload)));

        var aggregatorDomainsAfterDelete = aggregatorDomainsRepository.getAggregatorDomains(shard, bannerIds);
        assertThat(aggregatorDomainsAfterDelete.entrySet()).isEmpty();
    }
}

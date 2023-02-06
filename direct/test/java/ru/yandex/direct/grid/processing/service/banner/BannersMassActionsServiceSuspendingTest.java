package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collections;
import java.util.Map;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassActionPayload;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersMassActionsServiceSuspendingTest extends BannersMassActionsServiceBaseTest {

    private static final String BANNER_SUSPEND_MUTATION_NAME = "suspendAds";
    private static final String BANNER_RESUME_MUTATION_NAME = "resumeAds";

    @Test
    public void suspendBannersTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner draftTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.NEW);
        OldBanner suspendedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_SUSPEND_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, draftTextBanner, suspendedTextBanner,
                        archivedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(activeTextBanner, suspendedTextBanner).map(OldBanner::getId).toList())
                .withSkippedAdIds(StreamEx.of(draftTextBanner, archivedTextBanner)
                        .map(OldBanner::getId)
                        .toList())
                .withSuccessCount(2)
                .withTotalCount(4);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_SUSPEND_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_SUSPEND_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void suspendDifferentBannersTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        AdGroupInfo smartAdGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        AdGroupInfo mcBannerAdGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);

        OldBanner activeTextBanner = createTextBanner(textAdGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner activeSmartBanner = createSmartBanner(smartAdGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner activeMcBanner = createMcBanner(mcBannerAdGroupInfo, true, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_SUSPEND_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, activeSmartBanner, activeMcBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(activeTextBanner, activeSmartBanner, activeMcBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSkippedAdIds(Collections.emptyList())
                .withSuccessCount(3)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_SUSPEND_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_SUSPEND_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void suspendDraftBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        OldBanner draftBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.NEW);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_SUSPEND_MUTATION_NAME, graphQlSerialize(createRequest(draftBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(singletonList(draftBanner.getId()))
                .withSuccessCount(0)
                .withTotalCount(1);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_SUSPEND_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_SUSPEND_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void suspendBannersInArchivedCampaignTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(
                        activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withArchived(true),
                        clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_SUSPEND_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, stoppedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(activeTextBanner, stoppedTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(2);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_SUSPEND_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_SUSPEND_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void suspendBannersByNotOwner() {
        TestAuthHelper.setDirectAuthentication(operator);

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_SUSPEND_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, stoppedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(activeTextBanner, stoppedTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(2);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_SUSPEND_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_SUSPEND_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void resumeBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner draftBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.NEW);
        OldBanner activeTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_RESUME_MUTATION_NAME, graphQlSerialize(createRequest(archivedTextBanner, draftBanner,
                        activeTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(StreamEx.of(activeTextBanner).map(OldBanner::getId).toList())
                .withSkippedAdIds(StreamEx.of(archivedTextBanner, draftBanner).map(OldBanner::getId).toList())
                .withSuccessCount(1)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_RESUME_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_RESUME_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void resumeDifferentBannersTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        AdGroupInfo smartAdGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        AdGroupInfo mcBannerAdGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);

        OldBanner stoppedTextBanner = createTextBanner(textAdGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedSmartBanner = createSmartBanner(smartAdGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedMcBanner = createMcBanner(mcBannerAdGroupInfo, false, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_RESUME_MUTATION_NAME,
                graphQlSerialize(createRequest(stoppedTextBanner, stoppedSmartBanner, stoppedMcBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(stoppedTextBanner, stoppedSmartBanner, stoppedMcBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSkippedAdIds(Collections.emptyList())
                .withSuccessCount(3)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_RESUME_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_RESUME_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void resumeBannersInArchivedCampaignTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(
                        activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withArchived(true),
                        clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        OldBanner activeBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.NO);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_RESUME_MUTATION_NAME, graphQlSerialize(createRequest(activeBanner, stoppedBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(activeBanner, stoppedBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(2);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_RESUME_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_RESUME_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void resumeBannerByNotOwnerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);

        OldBanner suspendedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_RESUME_MUTATION_NAME,
                graphQlSerialize(createRequest(suspendedTextBanner, archivedTextBanner, activeTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(suspendedTextBanner, archivedTextBanner, activeTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_RESUME_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_RESUME_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

}

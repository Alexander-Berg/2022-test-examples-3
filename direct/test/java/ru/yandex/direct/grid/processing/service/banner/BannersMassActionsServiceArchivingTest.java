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
public class BannersMassActionsServiceArchivingTest extends BannersMassActionsServiceBaseTest {

    private static final String BANNER_ARCHIVE_MUTATION_NAME = "archiveAds";
    private static final String BANNER_UNARCHIVE_MUTATION_NAME = "unarchiveAds";

    @Test
    public void archiveBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_ARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, stoppedTextBanner, archivedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(activeTextBanner, stoppedTextBanner, archivedTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSkippedAdIds(Collections.emptyList())
                .withSuccessCount(3)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_ARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_ARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void archiveDifferentBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        AdGroupInfo smartAdGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        AdGroupInfo mcBannerAdGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);

        OldBanner activeTextBanner = createTextBanner(textAdGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner activeSmartBanner = createSmartBanner(smartAdGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner activeMcBanner = createMcBanner(mcBannerAdGroupInfo, true, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_ARCHIVE_MUTATION_NAME,
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
                .containsOnlyKeys(BANNER_ARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_ARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void archiveDraftBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        OldBanner textBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.NEW);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_ARCHIVE_MUTATION_NAME, graphQlSerialize(createRequest(textBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(singletonList(textBanner.getId()))
                .withSuccessCount(0)
                .withTotalCount(1);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_ARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_ARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void archiveBannersInArchivedCampaignTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(
                        activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withArchived(true),
                        clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_ARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, stoppedTextBanner, archivedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(activeTextBanner, stoppedTextBanner, archivedTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_ARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_ARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void archiveBannersByNotOwner() {
        TestAuthHelper.setDirectAuthentication(operator);

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);

        OldBanner activeTextBanner = createTextBanner(adGroupInfo, true, false, false, OldBannerStatusModerate.YES);
        OldBanner stoppedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_ARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(activeTextBanner, stoppedTextBanner, archivedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(activeTextBanner, stoppedTextBanner, archivedTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_ARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_ARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void unarchiveBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner archivedDeclinedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.NO);
        OldBanner unarchivedTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);
        OldBanner activeTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_UNARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(archivedTextBanner, archivedDeclinedTextBanner, unarchivedTextBanner,
                        activeTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(archivedTextBanner, archivedDeclinedTextBanner, unarchivedTextBanner,
                                activeTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSkippedAdIds(Collections.emptyList())
                .withSuccessCount(4)
                .withTotalCount(4);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_UNARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_UNARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void unarchiveDifferentBannerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        AdGroupInfo smartAdGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        AdGroupInfo mcBannerAdGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);

        OldBanner archivedTextBanner = createTextBanner(textAdGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner archivedSmartBanner = createSmartBanner(smartAdGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner archivedMcBanner = createMcBanner(mcBannerAdGroupInfo, false, true, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_UNARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(archivedTextBanner, archivedSmartBanner, archivedMcBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(
                        StreamEx.of(archivedTextBanner, archivedSmartBanner, archivedMcBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSkippedAdIds(Collections.emptyList())
                .withSuccessCount(3)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_UNARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_UNARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void unarchiveBannersInArchivedCampaignTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(
                        activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withArchived(true),
                        clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner archivedDeclinedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.NO);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_UNARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(archivedTextBanner, archivedDeclinedTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(archivedTextBanner, archivedDeclinedTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(2);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_UNARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_UNARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void unarchiveBannerByNotOwnerTest() {
        TestAuthHelper.setDirectAuthentication(operator);

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);
        OldBanner archivedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.YES);
        OldBanner archivedDeclinedTextBanner = createTextBanner(adGroupInfo, false, true, false, OldBannerStatusModerate.NO);
        OldBanner activeTextBanner = createTextBanner(adGroupInfo, false, false, false, OldBannerStatusModerate.YES);

        String query = String.format(BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                BANNER_UNARCHIVE_MUTATION_NAME,
                graphQlSerialize(createRequest(archivedTextBanner, archivedDeclinedTextBanner, activeTextBanner)));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdAdsMassActionPayload expectedPayload = new GdAdsMassActionPayload()
                .withProcessedAdIds(Collections.emptyList())
                .withSkippedAdIds(
                        StreamEx.of(archivedTextBanner, archivedDeclinedTextBanner, activeTextBanner)
                                .map(OldBanner::getId)
                                .toList())
                .withSuccessCount(0)
                .withTotalCount(3);

        Map<String, Object> actualPayloadMap = result.getData();
        assertThat(actualPayloadMap)
                .containsOnlyKeys(BANNER_UNARCHIVE_MUTATION_NAME);

        GdAdsMassActionPayload actualPayload =
                GraphQlJsonUtils.convertValue(actualPayloadMap.get(BANNER_UNARCHIVE_MUTATION_NAME),
                        GdAdsMassActionPayload.class);

        assertThat(actualPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

}

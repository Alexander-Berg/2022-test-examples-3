package ru.yandex.direct.core.entity.feed.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.configuration.GrutCoreTest;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.uac.GrutSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedIsUsedInCampaignBrief;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedStatusWrong;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedUsedInAdGroup;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteFeedValidationServiceTest {

    @Autowired
    private DeleteFeedValidationService deleteFeedValidationService;

    @Autowired
    private Steps steps;

    @Autowired
    private GrutSteps grutSteps;

    private ClientInfo clientInfo;
    private int shard;
    private ClientId clientId;
    private Long clientUid;
    private Long operatorUid;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        operatorUid = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).getUid();
        grutSteps.createClient(clientInfo);
    }

    @Test
    public void validate_newStatus_failure() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.NEW);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedInPerformanceAdGroup = steps.feedSteps().createFeed(feedInfo);
        steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInPerformanceAdGroup.getFeedId());
        List<Long> feedIds = List.of(feedInPerformanceAdGroup.getFeedId());

        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, operatorUid, feedIds);
        assertThat(validationResult)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0)),
                        feedStatusWrong(UpdateStatus.NEW)
                ))));
    }

    @Test
    public void validate_errorStatusAndUsed_failure() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.ERROR);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedInDynamicAdGroup = steps.feedSteps().createFeed(feedInfo);
        steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInDynamicAdGroup);
        List<Long> feedIds = List.of(feedInDynamicAdGroup.getFeedId());

        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, operatorUid, feedIds);
        assertThat(validationResult)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0)),
                        feedUsedInAdGroup()
                ))));
    }

    @Test
    public void validate_updatingStatus_failure() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.UPDATING);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedInPerformanceAndDynamicAdGroup = steps.feedSteps().createFeed(feedInfo);
        steps.adGroupSteps().createActivePerformanceAdGroup(feedInPerformanceAndDynamicAdGroup.getFeedId());
        steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInPerformanceAndDynamicAdGroup);
        List<Long> feedIds = List.of(feedInPerformanceAndDynamicAdGroup.getFeedId());

        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, operatorUid, feedIds);
        assertThat(validationResult)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0)),
                        feedStatusWrong(UpdateStatus.UPDATING)
                ))));
    }

    @Test
    public void validate_outdatedStatus_failure() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.OUTDATED);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedInPerformanceAndDynamicAdGroup = steps.feedSteps().createFeed(feedInfo);
        steps.adGroupSteps().createActivePerformanceAdGroup(feedInPerformanceAndDynamicAdGroup.getFeedId());
        steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInPerformanceAndDynamicAdGroup);
        List<Long> feedIds = List.of(feedInPerformanceAndDynamicAdGroup.getFeedId());

        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, operatorUid, feedIds);
        assertThat(validationResult)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0)),
                        feedStatusWrong(UpdateStatus.OUTDATED)
                ))));
    }

    @Test
    public void validate_doneStatusAndUnused_success() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.DONE);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedWithoutAdGroup = steps.feedSteps().createFeed(feedInfo);
        List<Long> feedIds = List.of(feedWithoutAdGroup.getFeedId());

        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, operatorUid, feedIds);
        assertThat(validationResult).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_noWritableOperator_failure() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.DONE);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedWithoutAdGroup = steps.feedSteps().createFeed(feedInfo);
        List<Long> feedIds = List.of(feedWithoutAdGroup.getFeedId());

        ClientInfo badOperator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);
        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, badOperator.getUid(), feedIds);

        assertThat(validationResult)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(),
                        noRights()
                ))));
    }

    @Test
    public void validate_feedIsUsedInCampaignBrief_failure() {
        Feed feed = defaultFeed(null).withUpdateStatus(UpdateStatus.DONE);
        FeedInfo feedInfo = new FeedInfo().withFeed(feed).withClientInfo(clientInfo);
        FeedInfo feedWithoutAdGroup = steps.feedSteps().createFeed(feedInfo);
        var feedId = feedWithoutAdGroup.getFeedId();

        grutSteps.createAndGetTextCampaign(clientInfo, true, feedId, null, null).getUacCampaign();

        ValidationResult<List<Long>, Defect> validationResult =
                deleteFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feedId));
        assertThat(validationResult)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0)),
                        feedIsUsedInCampaignBrief()
                ))));
    }
}

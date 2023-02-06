package ru.yandex.direct.api.v5.entity.feeds.delegate;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.feeds.DeleteRequest;
import com.yandex.direct.api.v5.feeds.DeleteResponse;
import com.yandex.direct.api.v5.feeds.FeedsSelectionCriteria;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import org.assertj.core.api.SoftAssertions;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.feeds.validation.FeedsValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.GrutApi5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedCategory;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedSupplementaryDataRepository;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.dbschema.ppc.Tables.PERF_FEED_HISTORY;
import static ru.yandex.direct.dbschema.ppc.Tables.PERF_FEED_VENDORS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GrutApi5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class DeleteFeedsDelegateTest {

    private static final Long NON_EXISTENT_FEED_ID = 2342L;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private FeedService feedService;

    @Autowired
    private FeedSupplementaryDataRepository feedSupplementaryDataRepository;

    private GenericApiService genericApiService;
    private DeleteFeedsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientId);
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        delegate = new DeleteFeedsDelegate(
                auth,
                resultConverter,
                feedService,
                mock(PpcPropertiesSupport.class),
                mock(FeatureService.class)
        );
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(
                apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class)
        );
    }

    @Test
    public void delete_doneStatusAndInPerformanceAdGroups_failure() {
        FeedInfo feedInfo = createFeed(UpdateStatus.DONE, clientInfo);
        var feedId = feedInfo.getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);

        DeleteResponse response = sendDeleteRequest(feedId);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        checkFeedIsNotDeleted(clientInfo.getClientId(), feedInfo.getFeed(), errors);
    }

    @Test
    public void delete_errorStatusAndInDynamicAdGroup_failure() {
        FeedInfo feedInfo = createFeed(UpdateStatus.ERROR, clientInfo);
        steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo);
        var feedId = feedInfo.getFeedId();

        DeleteResponse response = sendDeleteRequest(feedId);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        checkFeedIsNotDeleted(clientInfo.getClientId(), feedInfo.getFeed(), errors);
    }

    @Test
    public void delete_newStatusAndInDynamicAdGroup_failure() {
        FeedInfo feedInfo = createFeed(UpdateStatus.NEW, clientInfo);
        steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo);
        var feedId = feedInfo.getFeedId();

        DeleteResponse response = sendDeleteRequest(feedId);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        checkFeedIsNotDeleted(clientInfo.getClientId(), feedInfo.getFeed(), errors);
    }

    @Test
    public void delete_doneStatusWithoutAnyAdGroup_success() {
        var feedCategory = new FeedCategory()
                .withCategoryId(BigInteger.ONE)
                .withParentCategoryId(BigInteger.ONE)
                .withIsDeleted(false)
                .withName("feed_category")
                .withOfferCount(1L);
        FeedInfo feedInfo = createFeed(UpdateStatus.DONE, clientInfo);
        var feedId = feedInfo.getFeedId();
        int shard = clientInfo.getShard();
        steps.feedSteps().createFeedCategory(shard, feedCategory.withFeedId(feedId));

        addFeedVendor(shard, feedId);
        addFeedHistory(shard, feedId);

        DeleteResponse response = sendDeleteRequest(feedId);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        List<ActionResult> deleteResults = response.getDeleteResults();
        var expectedActionResult = new ActionResult().withId(feedId);
        List<FeedCategory> feedCategories = feedSupplementaryDataRepository.getFeedCategories(shard, List.of(feedId));
        boolean feedVendorExist = isFeedVendorExist(shard, feedId);
        boolean feedHistoryExist = isFeedHistoryExist(shard, feedId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors)
                    .as("Response does not contain errors")
                    .isEmpty();

            soft.assertThat(deleteResults)
                    .as("Only one feed should be removed")
                    .hasSize(1);

            soft.assertThat(deleteResults.get(0))
                    .as("Response is the same as expected")
                    .is(matchedBy(beanDiffer(expectedActionResult)));

            soft.assertThat(feedCategories)
                    .as("Feed categories should be removed")
                    .isEmpty();

            soft.assertThat(feedVendorExist)
                    .as("Feed vendor should be removed")
                    .isFalse();

            soft.assertThat(feedHistoryExist)
                    .as("Feed history should be removed")
                    .isFalse();
        });
    }

    @Test(expected = ApiValidationException.class)
    public void delete_moreThanMaxCountIds_failure() {
        List<Long> ids = LongStream.range(0, FeedsValidationService.MAX_IDS_COUNT + 1)
                .boxed()
                .collect(Collectors.toList());
        var selectionCriteria = new FeedsSelectionCriteria().withIds(ids);
        var externalRequest = new DeleteRequest()
                .withSelectionCriteria(selectionCriteria);
        genericApiService.doAction(delegate, externalRequest);
    }

    @Test
    public void delete_nonExistentFeed_failure() {
        DeleteResponse response = sendDeleteRequest(NON_EXISTENT_FEED_ID);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void delete_whenAskedSomebodyElse_failure() {
        ClientInfo otherClientInfo = steps.clientSteps().createDefaultClient();
        FeedInfo feedInfo = createFeed(UpdateStatus.NEW, otherClientInfo);
        Long feedId = feedInfo.getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(feedId);
        DeleteResponse response = sendDeleteRequest(feedId);
        List<ExceptionNotification> errors = response.getDeleteResults().get(0).getErrors();
        checkFeedIsNotDeleted(otherClientInfo.getClientId(), feedInfo.getFeed(), errors);
    }

    private FeedInfo createFeed(UpdateStatus status, ClientInfo clientInfo) {
        Feed feed = defaultFeed(null).withUpdateStatus(status);
        return steps.feedSteps().createFeed(new FeedInfo().withFeed(feed).withClientInfo(clientInfo));
    }

    private DeleteResponse sendDeleteRequest(Long feedId) {
        var selectionCriteria = new FeedsSelectionCriteria().withIds(feedId);
        var externalRequest = new DeleteRequest()
                .withSelectionCriteria(selectionCriteria);
        return genericApiService.doAction(delegate, externalRequest);
    }

    private void checkFeedIsNotDeleted(ClientId clientId, Feed expectedFeed, List<ExceptionNotification> errors) {
        Feed actualFeed = feedService.getFeeds(clientId, List.of(expectedFeed.getId())).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors).as("Response must contain errors").isNotEmpty();
            soft.assertThat(actualFeed).as("Some else feed shouldn't have change")
                    .is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
        });
    }

    private void addFeedVendor(int shard, Long feedId) {
        dslContextProvider.ppc(shard).insertInto(PERF_FEED_VENDORS)
                .set(PERF_FEED_VENDORS.FEED_ID, feedId)
                .set(PERF_FEED_VENDORS.NAME, "name")
                .execute();
    }

    private void addFeedHistory(int shard, Long feedId) {
        dslContextProvider.ppc(shard).insertInto(PERF_FEED_HISTORY)
                .set(PERF_FEED_HISTORY.FEED_ID, feedId)
                .execute();
    }

    private boolean isFeedVendorExist(int shard, Long feedId) {
        return dslContextProvider.ppc(shard)
                .select(DSL.val(1))
                .from(PERF_FEED_VENDORS)
                .where(PERF_FEED_VENDORS.FEED_ID.eq(feedId))
                .limit(1)
                .fetchAny() != null;
    }

    private boolean isFeedHistoryExist(int shard, Long feedId) {
        return dslContextProvider.ppc(shard)
                .select(DSL.val(1))
                .from(PERF_FEED_HISTORY)
                .where(PERF_FEED_HISTORY.FEED_ID.eq(feedId))
                .limit(1)
                .fetchAny() != null;
    }
}

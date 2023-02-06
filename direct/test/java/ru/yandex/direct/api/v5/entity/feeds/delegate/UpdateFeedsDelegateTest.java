package ru.yandex.direct.api.v5.entity.feeds.delegate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.feeds.FeedUpdateItem;
import com.yandex.direct.api.v5.feeds.FileFeedUpdate;
import com.yandex.direct.api.v5.feeds.ObjectFactory;
import com.yandex.direct.api.v5.feeds.UpdateRequest;
import com.yandex.direct.api.v5.feeds.UpdateResponse;
import com.yandex.direct.api.v5.feeds.UrlFeedUpdate;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.data.TestFeeds;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFileFeed;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsBase64YaStringWithoutPadding;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class UpdateFeedsDelegateTest {
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final Long NON_EXISTENT_FEED_ID = 2342L;

    @Autowired
    private Steps steps;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private FeedService feedService;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private GenericApiService genericApiService;
    private UpdateFeedsDelegate delegate;

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
        delegate = new UpdateFeedsDelegate(
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
    public void update_name_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        String newName = "newName";
        Long feedId = feedInfo.getFeedId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        var banner = activePerformanceBanner(
                adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(),
                creativeInfo.getCreativeId()
        ).withStatusShow(true);
        var bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withName(newName));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        Feed expectedFeed = feedInfo.getFeed().withName(newName);
        Feed actualFeed = feedService.getFeeds(clientInfo.getClientId(), List.of(feedId)).get(0);
        var bannersByGroupIds =
                bannerTypedRepository.getBannersByGroupIds(clientInfo.getShard(), List.of(adGroupInfo.getAdGroupId()));
        var banners = StreamEx.of(bannersByGroupIds)
                .map(b -> (BannerWithSystemFields) b)
                .toMap(Banner::getId, Function.identity());
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors)
                    .as("Response does not contain errors")
                    .isEmpty();

            soft.assertThat(actualFeed)
                    .as("Feed is the same as expected")
                    .is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));

            soft.assertThat(banners.get(bannerId).getStatusBsSynced())
                    .as("StatusBsSynced is same as expected")
                    .isEqualTo(StatusBsSynced.NO);

        });
    }

    @Test
    public void update_url_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        assumeThat(feedInfo.getFeed().getSource(), is(Source.URL));
        String newUrl = "http://newurl.ru/feed.csv";
        String newLogin = "newLogin";
        String newPassword = "newPassword";
        UrlFeedUpdate urlFeed = new UrlFeedUpdate()
                .withUrl(newUrl)
                .withLogin(OBJECT_FACTORY.createUrlFeedUpdateLogin(newLogin))
                .withPassword(OBJECT_FACTORY.createUrlFeedUpdatePassword(newPassword));
        Long feedId = feedInfo.getFeedId();
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withUrlFeed(urlFeed));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        Feed expectedFeed = feedInfo.getFeed()
                .withUpdateStatus(UpdateStatus.NEW)
                .withUrl(newUrl)
                .withLogin(newLogin)
                .withPlainPassword(newPassword);
        Feed actualFeed = feedService.getFeeds(clientInfo.getClientId(), List.of(feedId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors)
                    .as("Response does not contain errors")
                    .isEmpty();

            soft.assertThat(actualFeed)
                    .as("Feed is the same as expected")
                    .is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void update_fileWithNewFile_success() {
        String newFileName = "new_file_name.xml";
        byte[] fileData = "<feed />".getBytes(StandardCharsets.UTF_8);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();

        FileFeedUpdate fileFeed = new FileFeedUpdate()
                .withFilename(newFileName)
                .withData(fileData);
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withFileFeed(fileFeed));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        Feed expectedFeed = defaultFileFeed(clientInfo.getClientId())
                .withFilename(newFileName)
                .withCachedFileHash(getMd5HashAsBase64YaStringWithoutPadding(fileData))
                .withUpdateStatus(UpdateStatus.NEW);
        Feed actualFeed = feedService.getFeeds(clientInfo.getClientId(), List.of(feedId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors)
                    .as("Response does not contain errors")
                    .isEmpty();

            soft.assertThat(actualFeed)
                    .as("Feed is the same as expected")
                    .is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void update_fileWithTheSameFile_success() {
        String newFileName = "new_file_name.xml";
        FeedInfo feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();

        FileFeedUpdate fileFeed = new FileFeedUpdate()
                .withFilename(newFileName)
                .withData(TestFeeds.FILE_DATA);
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withFileFeed(fileFeed));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        Feed expectedFeed = defaultFileFeed(clientInfo.getClientId())
                .withFilename(newFileName)
                .withCachedFileHash(TestFeeds.FILE_HASH)
                .withUpdateStatus(UpdateStatus.DONE);
        Feed actualFeed = feedService.getFeeds(clientInfo.getClientId(), List.of(feedId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors)
                    .as("Response does not contain errors")
                    .isEmpty();

            soft.assertThat(actualFeed)
                    .as("Feed is the same as expected")
                    .is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void update_nonExistentFeed_failure() {
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(NON_EXISTENT_FEED_ID).withName("name"));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void update_urlAsFile_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        assumeThat(feedInfo.getFeed().getSource(), is(Source.URL));
        var fileFeed = new FileFeedUpdate()
                .withFilename("feed.csv");
        Long feedId = feedInfo.getFeedId();
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withFileFeed(fileFeed));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        assertThat(errors).isNotEmpty();
    }

    @Test(expected = ApiValidationException.class)
    public void add_moreThanMaxPerRequest_failure() {
        List<FeedUpdateItem> addItems = LongStream.range(0, FeedsValidationService.MAX_ELEMENTS_PER_REQUEST + 1)
                .mapToObj(it -> new FeedUpdateItem().withId(it).withName("name"))
                .collect(Collectors.toList());
        var externalRequest = new UpdateRequest().withFeeds(addItems);
        genericApiService.doAction(delegate, externalRequest);
    }

    @Test
    public void update_resetPasswordWithoutLogin_failure() {
        Feed feed = TestFeeds.defaultFeed(clientInfo.getClientId());
        FeedInfo feedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(feed);
        FeedInfo addedFeedInfo = steps.feedSteps().createFeed(feedInfo);
        UrlFeedUpdate updatedItem = new UrlFeedUpdate()
                .withLogin(OBJECT_FACTORY.createUrlFeedUpdateLogin(null));
        Long feedId = addedFeedInfo.getFeedId();
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withUrlFeed(updatedItem));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void update_passwordWithoutLogin_failure() {
        Feed feed = TestFeeds.defaultFeed(clientInfo.getClientId())
                .withSource(Source.URL)
                .withLogin(null)
                .withPlainPassword(null);
        FeedInfo feedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(feed);
        FeedInfo addedFeedInfo = steps.feedSteps().createFeed(feedInfo);

        UrlFeedUpdate updatedItem = new UrlFeedUpdate()
                .withLogin(OBJECT_FACTORY.createUrlFeedUpdateLogin("password"));
        Long feedId = addedFeedInfo.getFeedId();
        var externalRequest = new UpdateRequest()
                .withFeeds(new FeedUpdateItem().withId(feedId).withUrlFeed(updatedItem));
        UpdateResponse response = genericApiService.doAction(delegate, externalRequest);
        List<ExceptionNotification> errors = response.getUpdateResults().get(0).getErrors();
        assertThat(errors).isNotEmpty();
    }
}

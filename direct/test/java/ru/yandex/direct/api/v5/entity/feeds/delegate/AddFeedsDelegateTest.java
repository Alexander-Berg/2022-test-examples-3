package ru.yandex.direct.api.v5.entity.feeds.delegate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.feeds.AddRequest;
import com.yandex.direct.api.v5.feeds.AddResponse;
import com.yandex.direct.api.v5.feeds.BusinessTypeEnum;
import com.yandex.direct.api.v5.feeds.FeedAddItem;
import com.yandex.direct.api.v5.feeds.FileFeedAdd;
import com.yandex.direct.api.v5.feeds.SourceTypeEnum;
import com.yandex.direct.api.v5.feeds.UrlFeedAdd;
import com.yandex.direct.api.v5.general.YesNoEnum;
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
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class AddFeedsDelegateTest {
    private static final String FEED_NAME = "name";
    private static final String FEED_URL = "http://ya.ru/feed.csv";

    @Autowired
    private Steps steps;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private FeedService feedService;

    private GenericApiService genericApiService;
    private AddFeedsDelegate delegate;

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
        delegate = new AddFeedsDelegate(
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
    public void add_file_success() {
        String filename = "filename.csv";
        var feed = new FileFeedAdd()
                .withData("<xml></xml>".getBytes())
                .withFilename(filename);
        var feedAddItem = new FeedAddItem()
                .withName(FEED_NAME)
                .withSourceType(SourceTypeEnum.FILE)
                .withBusinessType(BusinessTypeEnum.HOTELS)
                .withFileFeed(feed);
        var externalRequest = new AddRequest().withFeeds(feedAddItem);
        AddResponse response = genericApiService.doAction(delegate, externalRequest);
        Long feedId = response.getAddResults().get(0).getId();
        var expectedFeed = new Feed()
                .withId(feedId)
                .withName("name")
                .withBusinessType(BusinessType.HOTELS)
                .withUpdateStatus(UpdateStatus.NEW)
                .withSource(Source.FILE)
                .withRefreshInterval(0L)
                .withFilename(filename);
        Feed actualFeed = feedService.getFeeds(clientInfo.getClientId(), List.of(feedId)).get(0);
        assertThat(actualFeed).is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void add_url_success() {
        var feedAddItem = buildUrlFeedAddItem();
        var externalRequest = new AddRequest().withFeeds(feedAddItem);
        AddResponse response = genericApiService.doAction(delegate, externalRequest);
        Long feedId = response.getAddResults().get(0).getId();
        var expectedFeed = new Feed()
                .withId(feedId)
                .withName("name")
                .withUrl("http://ya.ru/feed.csv")
                .withBusinessType(BusinessType.AUTO)
                .withUpdateStatus(UpdateStatus.NEW)
                .withSource(Source.URL)
                .withRefreshInterval(TimeUnit.DAYS.toSeconds(1L));
        Feed actualFeed = feedService.getFeeds(clientInfo.getClientId(), List.of(feedId)).get(0);
        assertThat(actualFeed).is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
    }

    @Test(expected = ApiValidationException.class)
    public void add_moreThanMaxPerRequest_failure() {
        List<FeedAddItem> addItems = LongStream.range(0, FeedsValidationService.MAX_ELEMENTS_PER_REQUEST + 1)
                .mapToObj(it -> buildUrlFeedAddItem())
                .collect(Collectors.toList());
        var externalRequest = new AddRequest().withFeeds(addItems);
        genericApiService.doAction(delegate, externalRequest);
    }

    private FeedAddItem buildUrlFeedAddItem() {
        var feed = new UrlFeedAdd()
                .withRemoveUtmTags(YesNoEnum.YES)
                .withUrl(FEED_URL);
        return new FeedAddItem()
                .withName(FEED_NAME)
                .withSourceType(SourceTypeEnum.URL)
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withUrlFeed(feed);
    }
}

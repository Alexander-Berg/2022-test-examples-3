package ru.yandex.direct.api.v5.entity.feeds.delegate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.feeds.BusinessTypeEnum;
import com.yandex.direct.api.v5.feeds.FeedFieldEnum;
import com.yandex.direct.api.v5.feeds.FeedGetItem;
import com.yandex.direct.api.v5.feeds.FeedStatusEnum;
import com.yandex.direct.api.v5.feeds.FeedsSelectionCriteria;
import com.yandex.direct.api.v5.feeds.FileFeedFieldEnum;
import com.yandex.direct.api.v5.feeds.FileFeedGet;
import com.yandex.direct.api.v5.feeds.GetRequest;
import com.yandex.direct.api.v5.feeds.GetResponse;
import com.yandex.direct.api.v5.feeds.ObjectFactory;
import com.yandex.direct.api.v5.feeds.SourceTypeEnum;
import com.yandex.direct.api.v5.feeds.UrlFeedFieldEnum;
import com.yandex.direct.api.v5.feeds.UrlFeedGet;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.feeds.converter.GetResponseConverterService;
import ru.yandex.direct.api.v5.entity.feeds.validation.FeedsValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.api.v5.common.GeneralUtil.yesNoFromBool;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.createFakeFeedUrl;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.unFakeUrlIfNeeded;
import static ru.yandex.direct.core.entity.feed.model.BusinessType.REALTY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetFeedsDelegateTest {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final Long NON_EXISTENT_FEED_ID = 2342L;
    private static final Long MARKET_FEED_ID = nextLong();
    private static final Long MARKET_SHOP_ID = nextLong();
    private static final Long MARKET_BUSINESS_ID = nextLong();
    private static final String FEED_FILENAME = "feed.xml";
    private static final String FEED_NAME = "feed name";
    private static final String URL = "https://mds.yandex.ru/feed";
    private static final long OFFERS_COUNT = 20L;
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";

    @Autowired
    private Steps steps;

    @Autowired
    private GetResponseConverterService getResponseConverterService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    private GenericApiService genericApiService;
    private GetFeedsDelegate delegate;

    private ClientInfo clientInfo;
    private JAXBElement<ArrayOfLong> jaxbCampaignIds;

    private CampaignInfo campaignInfo;
    private ClientId clientId;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientId);
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        delegate = new GetFeedsDelegate(auth,
                getResponseConverterService,
                shardHelper,
                feedRepository,
                campaignRepository);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        ArrayOfLong campaignIds = new ArrayOfLong().withItems(List.of(campaignInfo.getCampaignId()));
        jaxbCampaignIds = OBJECT_FACTORY.createFeedGetItemCampaignIds(campaignIds);
    }

    @Test
    public void get_file_withBaseAndFileFields_success() {
        var fileFeed = buildFileFeed();
        var fileFeedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(fileFeed);
        var fileFeedId = steps.feedSteps().createFeed(fileFeedInfo).getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, fileFeedId).getAdGroupId();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(fileFeedId);
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withFileFeedFieldNames(FileFeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        FeedGetItem actualItem = response.getFeeds().get(0);
        var expectedItem = new FeedGetItem()
                .withId(fileFeedId)
                .withName(FEED_NAME)
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withStatus(FeedStatusEnum.DONE)
                .withCampaignIds(jaxbCampaignIds)
                .withSourceType(SourceTypeEnum.FILE)
                .withNumberOfItems(OBJECT_FACTORY.createFeedGetItemNumberOfItems(OFFERS_COUNT))
                .withFileFeed(OBJECT_FACTORY.createFeedGetItemFileFeed(new FileFeedGet().withFilename(FEED_FILENAME)));
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void get_url_withBaseAndUrlFields_success() {
        var fileFeed = buildUrlFeed();
        var fileFeedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(fileFeed);
        var fileFeedId = steps.feedSteps().createFeed(fileFeedInfo).getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, fileFeedId).getAdGroupId();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(fileFeedId);
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withUrlFeedFieldNames(UrlFeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        FeedGetItem actualItem = response.getFeeds().get(0);
        var expectedUrlFeedItem = new UrlFeedGet()
                .withUrl(URL)
                .withLogin(OBJECT_FACTORY.createUrlFeedGetLogin(LOGIN))
                .withRemoveUtmTags(yesNoFromBool(true));
        var expectedItem = new FeedGetItem()
                .withId(fileFeedId)
                .withName(FEED_NAME)
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withStatus(FeedStatusEnum.DONE)
                .withCampaignIds(jaxbCampaignIds)
                .withSourceType(SourceTypeEnum.URL)
                .withNumberOfItems(OBJECT_FACTORY.createFeedGetItemNumberOfItems(OFFERS_COUNT))
                .withUrlFeed(OBJECT_FACTORY.createFeedGetItemUrlFeed(expectedUrlFeedItem));
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getUrlFields_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaignInfo);
        steps.feedSteps().setFeedProperty(adGroupInfo.getFeedInfo(), Feed.OFFERS_COUNT, OFFERS_COUNT);
        Long feedId = adGroupInfo.getFeedId();
        Feed feed = adGroupInfo.getFeedInfo().getFeed();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(feedId);
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withUrlFeedFieldNames(UrlFeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);

        var expectedUrlFeedItem = new UrlFeedGet()
                .withUrl(feed.getUrl())
                .withLogin(OBJECT_FACTORY.createUrlFeedGetLogin(feed.getLogin()))
                .withRemoveUtmTags(yesNoFromBool(feed.getIsRemoveUtm()));
        var expectedItem = new FeedGetItem()
                .withId(feedId)
                .withName(feed.getName())
                .withBusinessType(BusinessTypeEnum.RETAIL)
                .withStatus(FeedStatusEnum.DONE)
                .withFilterSchema("PerformanceDefault")
                .withCampaignIds(jaxbCampaignIds)
                .withSourceType(SourceTypeEnum.URL)
                .withNumberOfItems(OBJECT_FACTORY.createFeedGetItemNumberOfItems(OFFERS_COUNT))
                .withUrlFeed(OBJECT_FACTORY.createFeedGetItemUrlFeed(expectedUrlFeedItem));

        GetResponse response = genericApiService.doAction(delegate, externalRequest);

        FeedGetItem actualItem = response.getFeeds().get(0);
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getEmptyFilterSchema_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaignInfo);
        steps.feedSteps().setFeedProperty(adGroupInfo.getFeedInfo(), Feed.BUSINESS_TYPE, REALTY);

        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withUrlFeedFieldNames(UrlFeedFieldEnum.values())
                .withSelectionCriteria(new FeedsSelectionCriteria()
                        .withIds(adGroupInfo.getFeedId()));
        GetResponse response = genericApiService.doAction(delegate, externalRequest);

        String filterSchema = response.getFeeds().get(0).getFilterSchema();
        assertThat(filterSchema).isEqualTo("EmptySchema");
    }

    @Test
    public void get_url_withFileFields_empty() {
        var fileFeed = buildUrlFeed();
        var fileFeedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(fileFeed);
        var fileFeedId = steps.feedSteps().createFeed(fileFeedInfo).getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, fileFeedId).getAdGroupId();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(fileFeedId);
        var externalRequest = new GetRequest()
                .withFileFeedFieldNames(FileFeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        FeedGetItem actualItem = response.getFeeds().get(0);
        assertThat(actualItem).is(matchedBy(beanDiffer(new FeedGetItem()).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getUrlFields_withFakeUrl() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaignInfo);
        steps.feedSteps().setFeedProperty(adGroupInfo.getFeedInfo(), Feed.OFFERS_COUNT, OFFERS_COUNT);
        steps.feedSteps().setFeedProperty(adGroupInfo.getFeedInfo(), Feed.URL, createFakeFeedUrl(MARKET_BUSINESS_ID,
                MARKET_SHOP_ID, MARKET_FEED_ID, adGroupInfo.getFeedInfo().getFeed().getUrl(), false, false));
        Long feedId = adGroupInfo.getFeedId();
        Feed feed = adGroupInfo.getFeedInfo().getFeed();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(feedId);
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withUrlFeedFieldNames(UrlFeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);

        var expectedUrlFeedItem = new UrlFeedGet()
                .withUrl(unFakeUrlIfNeeded(feed.getUrl()))
                .withLogin(OBJECT_FACTORY.createUrlFeedGetLogin(feed.getLogin()))
                .withRemoveUtmTags(yesNoFromBool(feed.getIsRemoveUtm()));
        var expectedItem = new FeedGetItem()
                .withId(feedId)
                .withName(feed.getName())
                .withBusinessType(BusinessTypeEnum.RETAIL)
                .withStatus(FeedStatusEnum.DONE)
                .withFilterSchema("PerformanceDefault")
                .withCampaignIds(jaxbCampaignIds)
                .withSourceType(SourceTypeEnum.URL)
                .withNumberOfItems(OBJECT_FACTORY.createFeedGetItemNumberOfItems(OFFERS_COUNT))
                .withUrlFeed(OBJECT_FACTORY.createFeedGetItemUrlFeed(expectedUrlFeedItem));

        GetResponse response = genericApiService.doAction(delegate, externalRequest);

        FeedGetItem actualItem = response.getFeeds().get(0);
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }


    @Test
    public void get_nonExistentFeed_failure() {
        var selectionCriteria = new FeedsSelectionCriteria().withIds(NON_EXISTENT_FEED_ID);
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        List<FeedGetItem> feeds = response.getFeeds();
        assertThat(feeds).isEmpty();
    }

    @Test
    public void get_onlyIdField_success() {
        var urlFeed = buildUrlFeed();
        var urlFeedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(urlFeed);
        var urlFeedId = steps.feedSteps().createFeed(urlFeedInfo).getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, urlFeedId).getAdGroupId();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(urlFeedId);
        var externalRequest = new GetRequest()
                .withSelectionCriteria(selectionCriteria)
                .withFieldNames(FeedFieldEnum.ID);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        FeedGetItem actualItem = response.getFeeds().get(0);
        FeedGetItem expectedItem = new FeedGetItem().withId(urlFeedId);
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getAll_success() {
        var urlFeed = buildUrlFeed();
        var urlFeedInfo = new FeedInfo().withClientInfo(clientInfo).withFeed(urlFeed);
        var urlFeedId = steps.feedSteps().createFeed(urlFeedInfo).getFeedId();
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, urlFeedId).getAdGroupId();
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.ID);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        FeedGetItem actualItem = response.getFeeds().get(0);
        FeedGetItem expectedItem = new FeedGetItem().withId(urlFeedId);
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test(expected = ApiValidationException.class)
    public void get_moreThanMaxCountIds_failure() {
        List<Long> ids = LongStream.range(0, FeedsValidationService.MAX_IDS_COUNT + 1)
                .boxed()
                .collect(Collectors.toList());
        var selectionCriteria = new FeedsSelectionCriteria().withIds(ids);
        var externalRequest = new GetRequest()
                .withSelectionCriteria(selectionCriteria)
                .withFieldNames(FeedFieldEnum.ID);
        genericApiService.doAction(delegate, externalRequest);
    }

    @Test
    public void get_whenAskedSomebodyElseFeed_failure() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        var selectionCriteria = new FeedsSelectionCriteria().withIds(adGroupInfo.getFeedId());
        var externalRequest = new GetRequest()
                .withFieldNames(FeedFieldEnum.values())
                .withSelectionCriteria(selectionCriteria);
        GetResponse response = genericApiService.doAction(delegate, externalRequest);
        List<FeedGetItem> feeds = response.getFeeds();
        assertThat(feeds).isEmpty();
    }

    private Feed buildFileFeed() {
        return new Feed()
                .withClientId(clientId.asLong())
                .withName(FEED_NAME)
                .withBusinessType(BusinessType.AUTO)
                .withFilename(FEED_FILENAME)
                .withUpdateStatus(UpdateStatus.DONE)
                .withOffersCount(OFFERS_COUNT)
                .withRefreshInterval(10L)
                .withSource(Source.FILE);
    }

    private Feed buildUrlFeed() {
        return new Feed()
                .withClientId(clientId.asLong())
                .withName(FEED_NAME)
                .withBusinessType(BusinessType.AUTO)
                .withUrl(URL)
                .withLogin(LOGIN)
                .withPlainPassword(PASSWORD)
                .withIsRemoveUtm(true)
                .withUpdateStatus(UpdateStatus.DONE)
                .withOffersCount(OFFERS_COUNT)
                .withRefreshInterval(10L)
                .withSource(Source.URL);
    }
}

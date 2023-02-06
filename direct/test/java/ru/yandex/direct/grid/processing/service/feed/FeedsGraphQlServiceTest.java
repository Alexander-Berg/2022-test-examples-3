package ru.yandex.direct.grid.processing.service.feed;

import java.math.BigInteger;
import java.net.IDN;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedCategory;
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItem;
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItemParseResults;
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItemParseResultsDefect;
import ru.yandex.direct.core.entity.feed.model.MasterSystem;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestFeeds;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.gemini.GeminiClient;
import ru.yandex.direct.grid.model.feed.GdFeedAccess;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedsGraphQlServiceTest {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String FEED_REQUEST = "{\n"
            + "  client(searchBy:{\n"
            + "    id: %s\n"
            + "  }) {\n"
            + "    feeds(url: %s){\n"
            + "      totalCount,\n"
            + "      features{\n"
            + "        canBeDeletedCount,\n"
            + "        canBeEditedCount\n"
            + "      }\n"
            + "      rowset{\n"
            + "        id,\n"
            + "        name,\n"
            + "        feedType,\n"
            + "        businessType,\n"
            + "        updateStatus,\n"
            + "        errors{\n"
            + "         code\n"
            + "         messageEn\n"
            + "         messageRu\n"
            + "        }\n"
            + "        warnings{\n"
            + "         code\n"
            + "         messageEn\n"
            + "         messageRu\n"
            + "        }\n"
            + "        url,\n"
            + "        login,\n"
            + "        hasPassword,\n"
            + "        fileName,\n"
            + "        lastChange,\n"
            + "        source,\n"
            + "        offersCount,\n"
            + "        fetchErrorsCount,\n"
            + "        isReadOnly,\n"
            + "        isRemoveUtm,\n"
            + "        campaigns{\n"
            + "         id\n"
            + "         name\n"
            + "        }\n"
            + "        access{\n"
            + "         canEdit\n"
            + "         canDelete\n"
            + "        }\n"
            + "        categories{\n"
            + "         categoryId\n"
            + "         parentCategoryId\n"
            + "         name\n"
            + "         offerCount\n"
            + "         isDeleted\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String DEFAULT_FEED_REQUEST = "{\n"
            + "  client(searchBy:{\n"
            + "    id: %s\n"
            + "  }) {\n"
            + "    feeds(url: %s){\n"
            + "      defaultFeedId,\n"
            + "      totalCount\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String SITE_URL_1 = "https://some-site.com";
    private static final String SITE_URL_RU = "https://яндекс.рф";
    private static final String SITE_DOMAIN_1 = "some-site.com";
    private static final String SITE_DOMAIN_2 = "another-site.com";
    private static final String SITE_DOMAIN_RU = "яндекс.рф";
    private static final String WWW_SITE_DOMAIN = "www.some-site.com";

    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private GeminiClient geminiClient;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    private ClientInfo clientInfo;
    private GridGraphQLContext qlContext;
    private ClientId clientId;

    private static Map<String, Object> getExpected(Feed feed, FeedCategory feedCategory,
                                                   @Nullable FeedHistoryItem historyItem,
                                                   Campaign campaign, GdFeedAccess gdFeedAccess) {
        var campaigns = campaign == null ? list() : list(map(
                "id", campaign.getId(),
                "name", campaign.getName()));
        var errors = Optional.ofNullable(historyItem)
                .map(FeedHistoryItem::getParseResults)
                .map(FeedHistoryItemParseResults::getErrors)
                .map(list -> mapList(list, error -> map(
                        "code", error.getCode(),
                        "messageEn", error.getMessageEn(),
                        "messageRu", error.getMessageRu()
                )))
                .orElse(null);
        var warnings = Optional.ofNullable(historyItem)
                .map(FeedHistoryItem::getParseResults)
                .map(FeedHistoryItemParseResults::getWarnings)
                .map(list -> mapList(list, error -> map(
                        "code", error.getCode(),
                        "messageEn", error.getMessageEn(),
                        "messageRu", error.getMessageRu()
                )))
                .orElse(null);
        var categories = feedCategory == null ? list() : list(map(
                "categoryId", feedCategory.getCategoryId().toString(),
                "parentCategoryId", feedCategory.getParentCategoryId().toString(),
                "name", feedCategory.getName(),
                "offerCount", feedCategory.getOfferCount(),
                "isDeleted", feedCategory.getIsDeleted()));
        var access = map(
                "canEdit", gdFeedAccess.getCanEdit(),
                "canDelete", gdFeedAccess.getCanDelete()
        );
        var features = map(
                "canBeEditedCount", gdFeedAccess.getCanEdit() ? 1 : 0,
                "canBeDeletedCount", gdFeedAccess.getCanDelete() ? 1 : 0
        );
        return map(
                "client", map(
                        "feeds", map(
                                "totalCount", 1,
                                "features", features,
                                "rowset", list(
                                        map(
                                                "id", feed.getId(),
                                                "name", feed.getName(),
                                                "feedType", getEnumName(feed.getFeedType()),
                                                "businessType", getEnumName(feed.getBusinessType()),
                                                "updateStatus", getEnumName(feed.getUpdateStatus()),
                                                "errors", errors,
                                                "warnings", warnings,
                                                "url", feed.getUrl(),
                                                "login", feed.getLogin(),
                                                "hasPassword", isNotBlank(feed.getPlainPassword()),
                                                "fileName", feed.getFilename(),
                                                "lastChange", formatDateTime(feed.getLastChange()),
                                                "source", getEnumName(feed.getSource()),
                                                "offersCount", feed.getOffersCount(),
                                                "fetchErrorsCount", feed.getFetchErrorsCount(),
                                                "isReadOnly", feed.getMasterSystem() != MasterSystem.DIRECT,
                                                "isRemoveUtm", feed.getIsRemoveUtm(),
                                                "campaigns", campaigns,
                                                "categories", categories,
                                                "access", access
                                        )
                                )
                        )
                )
        );
    }

    private static String formatDateTime(TemporalAccessor temporal) {
        return StringUtils.removeEnd(DATETIME_FORMATTER.format(temporal), ":00");
    }

    private static <T extends Enum<T>> String getEnumName(@Nullable T t) {
        return t != null ? t.name() : null;
    }

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.clientOptionsSteps().addEmptyClientOptions(clientInfo);
        clientId = clientInfo.getClientId();

        User user = userService.getUser(clientInfo.getUid());
        qlContext = buildContext(user)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(qlContext);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getFeeds_success() {
        Feed feed = steps.feedSteps().createDefaultFeed(clientInfo).getFeed();
        Long campaignId = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feed.getId()).getCampaignId();
        Campaign campaign = campaignService.getCampaigns(clientId, singleton(campaignId)).get(0);
        FeedHistoryItem feedHistoryItem = new FeedHistoryItem()
                .withFeedId(feed.getId())
                .withParseResults(new FeedHistoryItemParseResults()
                        .withWarnings(List.of(new FeedHistoryItemParseResultsDefect()
                                .withCode(1204L)
                                .withMessageEn("XML Parser warning: Category must not contain itself as a parent."))));
        steps.feedSteps().createFeedHistoryItem(clientInfo.getShard(), feedHistoryItem);
        FeedCategory feedCategory = new FeedCategory()
                .withFeedId(feed.getId())
                .withCategoryId(BigInteger.ONE)
                .withParentCategoryId(BigInteger.ONE)
                .withIsDeleted(false)
                .withName("feed_category")
                .withOfferCount(1L);
        steps.feedSteps().createFeedCategory(clientInfo.getShard(), feedCategory);

        Map<String, Object> data = sendRequest(clientId);

        Feed actualFeed = feedRepository.get(clientInfo.getShard(), List.of(feed.getId())).get(0);
        var access = new GdFeedAccess()
                .withCanDelete(false)
                .withCanEdit(true);
        Map<String, Object> expected = getExpected(actualFeed, feedCategory, feedHistoryItem, campaign, access);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getFeeds_canBeDeletedSuccess() {
        Feed feed = steps.feedSteps().createDefaultFeed(clientInfo).getFeed();
        FeedCategory feedCategory = new FeedCategory()
                .withFeedId(feed.getId())
                .withCategoryId(BigInteger.ONE)
                .withParentCategoryId(BigInteger.ONE)
                .withIsDeleted(false)
                .withName("feed_category")
                .withOfferCount(1L);
        steps.feedSteps().createFeedCategory(clientInfo.getShard(), feedCategory);

        Map<String, Object> data = sendRequest(clientId);

        Feed actualFeed = feedRepository.get(clientInfo.getShard(), List.of(feed.getId())).get(0);
        var access = new GdFeedAccess()
                .withCanDelete(true)
                .withCanEdit(true);
        Map<String, Object> expected = getExpected(actualFeed, feedCategory, null, null, access);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getFeeds_feedWithErrors_success() {
        Feed feed = steps.feedSteps().createFeed(clientInfo, UpdateStatus.ERROR).getFeed();
        Long campaignId = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feed.getId()).getCampaignId();
        Campaign campaign = campaignService.getCampaigns(clientId, singleton(campaignId)).get(0);
        FeedHistoryItem feedHistoryItem = new FeedHistoryItem()
                .withFeedId(feed.getId())
                .withParseResults(new FeedHistoryItemParseResults()
                        .withErrors(List.of(new FeedHistoryItemParseResultsDefect()
                                .withCode(1201L)
                                .withMessageEn("Err1201: can't download url"))));
        steps.feedSteps().createFeedHistoryItem(clientInfo.getShard(), feedHistoryItem);
        FeedCategory feedCategory = new FeedCategory()
                .withFeedId(feed.getId())
                .withCategoryId(BigInteger.ONE)
                .withParentCategoryId(BigInteger.ONE)
                .withIsDeleted(false)
                .withName("feed_category")
                .withOfferCount(1L);
        steps.feedSteps().createFeedCategory(clientInfo.getShard(), feedCategory);

        Map<String, Object> data = sendRequest(clientId);

        Feed actualFeed = feedRepository.get(clientInfo.getShard(), List.of(feed.getId())).get(0);
        var access = new GdFeedAccess()
                .withCanDelete(false)
                .withCanEdit(true);
        Map<String, Object> expected = getExpected(actualFeed, feedCategory, feedHistoryItem, campaign, access);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getFeeds_syncedUrlFeed_success() {
        steps.feedSteps().createDefaultSyncedFeed(clientInfo);

        Map<String, Object> data = sendRequest(clientId);

        String returnedUrl = getDataValue(data, "client/feeds/rowset/0/url");
        assertThat(returnedUrl).isEqualTo(TestFeeds.DEFAULT_FEED_URL);
    }

    @Test
    public void getFeeds_syncedSiteFeed_success() {
        steps.feedSteps().createDefaultSyncedSiteFeed(clientInfo);

        Map<String, Object> data = sendRequest(clientId);

        String returnedUrl = getDataValue(data, "client/feeds/rowset/0/url");
        assertThat(returnedUrl).isEqualTo(TestFeeds.DEFAULT_FEED_SITE);
    }

    @Test
    public void getFeeds_defaultFeedId_simple() {
        Feed defaultFeed = TestFeeds.defaultFeed();

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_urlFeed() {
        Feed defaultFeed = TestFeeds.defaultFeed();

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                TestFeeds.defaultFeed(),
                TestFeeds.defaultFileFeed(),
                defaultFeed
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_fileFeed() {
        Feed defaultFeed = TestFeeds.defaultFileFeed();

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                TestFeeds.defaultFileFeed(),
                TestFeeds.defaultFeed(),
                defaultFeed
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_onlyDoneOrUpdating() {
        Feed defaultFeed = TestFeeds.defaultFileFeed();

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                TestFeeds.defaultFeed().withUpdateStatus(UpdateStatus.DONE),
                defaultFeed.withUpdateStatus(UpdateStatus.UPDATING),
                TestFeeds.defaultFeed().withUpdateStatus(UpdateStatus.ERROR),
                TestFeeds.defaultFileFeed().withUpdateStatus(UpdateStatus.OUTDATED),
                TestFeeds.defaultFeed().withUpdateStatus(UpdateStatus.NEW)
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_siteFeedsLast() {
        Feed defaultFeed = TestFeeds.defaultFileFeed();

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                TestFeeds.defaultFeed(),
                defaultFeed,
                TestFeeds.defaultSiteFeed()
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_siteFeed() {
        Feed defaultFeed = TestFeeds.defaultSiteFeed();

        createFeedsWithIncreasingLastChange(List.of(
                defaultFeed,
                TestFeeds.defaultFileFeed().withUpdateStatus(UpdateStatus.NEW),
                TestFeeds.defaultFeed().withUpdateStatus(UpdateStatus.ERROR)
        ));

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isNull();
    }

    @Test
    public void getFeeds_defaultFeedId_siteFeedNotDefault() {
        Feed defaultFeed = TestFeeds.defaultFeed();

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed,
                TestFeeds.defaultSiteFeed()
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_manualFeed() {
        Feed defaultFeed = TestFeeds.defaultFileFeed().withMasterSystem(MasterSystem.MANUAL);

        createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        ));

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, null);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isNull();
    }

    @Test
    public void getFeeds_defaultFeedId_sameTargetDomain() {
        String url = SITE_URL_1;
        Feed defaultFeed = TestFeeds.defaultFeed().withTargetDomain(SITE_DOMAIN_1);

        doReturn(Map.of(url, url)).when(geminiClient).getMainMirrors(List.of(url));

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, url);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_anotherTargetDomain() {
        String url = SITE_URL_1;
        Feed defaultFeed = TestFeeds.defaultFeed().withTargetDomain(SITE_DOMAIN_2);

        doReturn(Map.of(url, url)).when(geminiClient).getMainMirrors(List.of(url));

        Map<Feed, FeedInfo> feeds = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        ));

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, url);

        Integer totalCount = getDataValue(data, "client/feeds/totalCount");
        assertThat(totalCount).isEqualTo(feeds.size());

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isNull();
    }

    @Test
    public void getFeeds_defaultFeedId_punycodeTargetDomain() {
        String url = SITE_URL_RU;
        String punycodeDomain = IDN.toASCII(SITE_DOMAIN_RU);
        Feed defaultFeed = TestFeeds.defaultFeed().withTargetDomain(punycodeDomain);

        doReturn(Map.of(url, url)).when(geminiClient).getMainMirrors(List.of(url));

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, url);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    @Test
    public void getFeeds_defaultFeedId_wwwTargetDomain() {
        String url = SITE_URL_1;
        Feed defaultFeed = TestFeeds.defaultFeed().withTargetDomain(WWW_SITE_DOMAIN);

        doReturn(Map.of(url, url)).when(geminiClient).getMainMirrors(List.of(url));

        Map<Feed, FeedInfo> feeds = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        ));

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, url);

        Integer totalCount = getDataValue(data, "client/feeds/totalCount");
        assertThat(totalCount).isEqualTo(feeds.size());

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isNull();
    }

    @Test
    public void getFeeds_defaultFeedId_failedGemini() {
        String url = SITE_URL_1;
        Feed defaultFeed = TestFeeds.defaultFeed().withTargetDomain(SITE_DOMAIN_1);

        doReturn(Map.of()).when(geminiClient).getMainMirrors(List.of(url));

        FeedInfo defaultFeedInfo = createFeedsWithIncreasingLastChange(List.of(
                defaultFeed
        )).get(defaultFeed);

        Map<String, Object> data = sendRequest(clientId, DEFAULT_FEED_REQUEST, url);

        Long defaultFeedId = getDataValue(data, "client/feeds/defaultFeedId");
        assertThat(defaultFeedId).isEqualTo(defaultFeedInfo.getFeedId());
    }

    private Map<Feed, FeedInfo> createFeedsWithIncreasingLastChange(Collection<Feed> feeds) {
        return StreamEx.of(feeds)
                .zipWith(StreamEx.iterate(LocalDateTime.now(), date -> date.plusHours(1)), Feed::withLastChange)
                .mapToEntry(Function.identity(), Function.identity())
                .mapValues(feed -> new FeedInfo().withClientInfo(clientInfo).withFeed(feed))
                .mapValues(steps.feedSteps()::createFeed)
                .toMap();
    }

    private Map<String, Object> sendRequest(ClientId clientId) {
        return sendRequest(clientId, FEED_REQUEST, null);
    }

    private Map<String, Object> sendRequest(ClientId clientId, String request, String url) {
        if (url != null) {
            url = "\"" + url + "\"";
        }
        String query = String.format(request, clientId, url);
        ExecutionResult result = processor.processQuery(null, query, null, qlContext);
        checkErrors(result.getErrors());
        return result.getData();
    }
}

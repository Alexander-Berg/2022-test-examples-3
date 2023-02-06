package ru.yandex.direct.core.entity.feed.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.uac.model.EcomDomain;
import ru.yandex.direct.core.entity.uac.service.EcomDomainsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.gemini.GeminiClient;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.misc.random.Random2;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.createFakeFeedUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteContainsDuplicatedUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteForNotAllowedUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteForNotDomainOnlyUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteWithSameDomainAlreadyExists;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedInvalidFilename;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedInvalidHref;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedLoginIsNotSet;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedPasswordIsNotSet;
import static ru.yandex.direct.core.entity.feed.validation.constraints.FeedConstraints.MBI_MAX_LOGIN_LENGTH;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripDomainTail;
import static ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripProtocol;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxElementsExceeded;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddFeedValidationServiceTest {

    @Autowired
    private FeedRepository feedRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ClientLimitsService clientLimitsService;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private Steps steps;

    @Autowired
    private RbacService rbacService;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private EcomDomainsService ecomDomainsService;

    private AddFeedValidationService addFeedValidationService;
    private int shard;
    private ClientId clientId;
    private Long clientUid;
    private Long operatorUid;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        operatorUid = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).getUid();

        when(clientLimitsService.getClientLimits(eq(clientId)).getFeedCountLimitOrDefault())
                .thenReturn(10L);

        ppcPropertiesSupport.set(PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE,
                PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE.getType().serialize(emptySet()));

        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(false);

        addFeedValidationService = new AddFeedValidationService(feedRepository, clientLimitsService, rbacService,
                ppcPropertiesSupport, geminiClient, ecomDomainsService);
    }

    @Test
    public void validate_success() {
        Feed feed = defaultFeed(clientId);

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_invalidUrl_failure() {
        Feed feed = defaultFeed(clientId).withUrl("invalid.url");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedInvalidHref()
                ))));
    }

    @Test
    public void validate_ipUrl_success() {
        Feed feed = defaultFeed(clientId).withUrl("http://82.202.168.218/feed.xml");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_invalidIpUrl_failure() {
        // Вышли за пределы [0, 255]
        Feed feed = defaultFeed(clientId).withUrl("http://256.0.0.1/feed.xml");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedInvalidHref()
                ))));
    }

    @Test
    public void validate_invalidFileName_failure() {
        Feed feed = defaultFeed(clientId).withFilename("invalid.jpg");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.FILENAME)),
                        feedInvalidFilename()
                ))));
    }

    @Test
    public void validate_loginWithoutPassword_failure() {
        Feed feed = defaultFeed(clientId).withPlainPassword(null).withLogin("login");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.PLAIN_PASSWORD)),
                        feedPasswordIsNotSet()
                ))));
    }

    @Test
    public void validate_passwordWithoutLogin_failure() {
        Feed feed = defaultFeed(clientId).withLogin(null).withPlainPassword("password");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.LOGIN)),
                        feedLoginIsNotSet()
                ))));
    }

    @Test
    public void validate_loginWithPassword_success() {
        Feed feed = defaultFeed(clientId)
                .withLogin(Random2.R.nextStringSimpleUtf8(MBI_MAX_LOGIN_LENGTH))
                .withPlainPassword("password");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_tooLongLoginWithPassword_failure() {
        Feed feed = defaultFeed(clientId)
                .withLogin(Random2.R.nextStringSimpleUtf8(MBI_MAX_LOGIN_LENGTH + 1))
                .withPlainPassword("password");

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.LOGIN)),
                        maxStringLength(MBI_MAX_LOGIN_LENGTH)
                ))));
    }

    @Test
    public void validate_moreThanClientLimit_failure() {
        long maxFeedCountForClient = 3L;
        List<Feed> feeds = LongStreamEx.range(0, maxFeedCountForClient)
                .mapToObj(i -> defaultFeed(clientId))
                .toList();
        feedRepository.add(shard, feeds);
        Feed feed = defaultFeed(clientId);
        when(clientLimitsService.getClientLimits(eq(clientId)).getFeedCountLimitOrDefault())
                .thenReturn(maxFeedCountForClient);

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(),
                        maxElementsExceeded((int) maxFeedCountForClient)
                ))));
    }

    @Test
    public void validate_noWritableOperator_failure() {
        Feed feed = defaultFeed(clientId);

        ClientInfo badOperator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);
        var vr = addFeedValidationService.validate(shard, clientId, clientUid, badOperator.getUid(), List.of(feed),
                false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(),
                        noRights()
                ))));
    }

    @Test
    public void validate_sourceSite_success() {
        Feed feed = defaultFeed(clientId)
                .withSource(Source.SITE)
                .withUrl("https://yandex.ru");
        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of(feed.getUrl(), feed.getUrl()));

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), true);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_sourceSiteIsDomainOnlyFinishedWithSlash_success() {
        Feed feed = defaultFeed(clientId)
                .withSource(Source.SITE)
                .withUrl("https://yandex.ru/");
        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of(feed.getUrl(), feed.getUrl()));

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), true);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_sourceSiteIsNotDomainOnly_failure() {
        Feed feed = defaultFeed(clientId)
                .withSource(Source.SITE)
                .withUrl("https://yandex.ru/a");
        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of(feed.getUrl(), feed.getUrl()));

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), true);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotDomainOnlyUrl("https://yandex.ru/a", "https://yandex.ru")
                ))));
    }

    @Test
    public void validate_sourceSiteWithWrongBusinessType_failure() {
        Feed feed = defaultFeed(clientId)
                .withSource(Source.SITE)
                .withBusinessType(BusinessType.FLIGHTS);

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), true);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.SOURCE)),
                        invalidValue()
                ))));
    }

    @Test
    public void validate_sourceSiteWithoutMbi_failure() {
        Feed feed = defaultFeed(clientId)
                .withSource(Source.SITE);

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), false);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.SOURCE)),
                        invalidValue()
                ))));
    }

    @Test
    public void validate_withFakeUrl_failure() {
        String fakeFeedUrl = createFakeFeedUrl(1L, 2L, 3L, "http://ya.ru", false, false);
        Feed feed = defaultFeed(clientId).withUrl(fakeFeedUrl);

        var vr = addFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(feed), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        invalidValue()
                ))));
    }

    @Test
    public void validate_manySiteFeeds_success() {
        Feed feed1 = createSiteFeed("https://ya.ru");
        Feed feed2 = createSiteFeed("https://abc.com");
        Feed feed3 = createSiteFeed("http://qwerty.ru");

        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://abc.com", "abc.com",
                        "https://ya.ru", "ya.ru",
                        "http://qwerty.ru", "www.qwerty.ru"
                ));

        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feed1, feed2, feed3), true);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_manySiteFeedsWithDuplicatedDomains_failure() {
        Feed feed1 = createSiteFeed("https://ya.ru");
        Feed feed2 = createSiteFeed("https://abc.com");
        Feed feed3 = createSiteFeed("http://ya.ru");

        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://abc.com", "abc.com",
                        "https://ya.ru", "ya.ru",
                        "http://qwerty.ru", "www.qwerty.ru"
                ));

        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feed1, feed2, feed3), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteContainsDuplicatedUrl("https://ya.ru")
                ))));
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(2), field(Feed.URL)),
                        feedBySiteContainsDuplicatedUrl("http://ya.ru")
                ))));
    }

    @Test
    public void validate_withExistingFeedWithSameUrl_failure() {
        Feed existingFeed = createSiteFeed("https://ya.ru");
        Long feedId = feedRepository.add(shard, List.of(existingFeed)).get(0);

        Feed feedToAdd1 = createSiteFeed("https://abc.com");
        Feed feedToAdd2 = createSiteFeed("http://qwerty.ru");
        Feed feedToAdd3 = createSiteFeed("https://ya.ru");
        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://abc.com", "abc.com",
                        "https://ya.ru", "ya.ru",
                        "http://qwerty.ru", "www.qwerty.ru"
                ));

        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feedToAdd1, feedToAdd2, feedToAdd3), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(2), field(Feed.URL)),
                        feedBySiteWithSameDomainAlreadyExists(feedId)
                ))));
    }

    @Test
    public void validate_withNotAllowedSiteDomain_failure() {
        Feed feedToAdd1 = createSiteFeed("https://abc.com");
        Feed feedToAdd2 = createSiteFeed("http://qwertyu.ru");
        Feed feedToAdd3 = createSiteFeed("https://ya.ru");

        ppcPropertiesSupport.set(PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE,
                PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE.getType().serialize(Set.of("www.qwertyu.ru")));

        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://abc.com", "abc.com",
                        "https://ya.ru", "ya.ru",
                        "http://qwertyu.ru", "www.qwertyu.ru"
                ));

        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feedToAdd1, feedToAdd2, feedToAdd3), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(1), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("http://qwertyu.ru")
                ))));
    }

    @Test
    public void validate_withEcomDomains_success() {
        Feed feedToAdd = createSiteFeed("https://abc.com");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(10L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(100_000L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(Map.of("https://abc.com",
                        new EcomDomain().withDomain("https://abc.com").withOffersCount(1000L)));

        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feedToAdd), true);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_withEcomDomainsNotEcom_failure() {
        Feed feedToAdd = createSiteFeed("https://abc.com");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(10L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(100_000L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(emptyMap());
        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feedToAdd), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://abc.com")
                ))));
    }

    @Test
    public void validate_withEcomDomainsNotEnougthOffers_failure() {
        Feed feedToAdd = createSiteFeed("https://abc.com");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(1_001L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(100_000L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(Map.of("https://abc.com",
                        new EcomDomain().withDomain("https://abc.com").withOffersCount(1000L)));
        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feedToAdd), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://abc.com")
                ))));
    }

    @Test
    public void validate_withEcomDomainsTooManyOffers_failure() {
        Feed feedToAdd = createSiteFeed("https://abc.com");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(10L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(999L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(Map.of("https://abc.com",
                        new EcomDomain().withDomain("https://abc.com").withOffersCount(1000L)));
        var vr = addFeedValidationService.validate(
                shard, clientId, clientUid, operatorUid, List.of(feedToAdd), true);

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://abc.com")
                ))));
    }

    private Feed createSiteFeed(String url) {
        return defaultFeed(clientId)
                .withUrl(url)
                .withName("feed name")
                .withTargetDomain(stripDomainTail(stripProtocol(url)))
                .withSource(Source.SITE);
    }

}

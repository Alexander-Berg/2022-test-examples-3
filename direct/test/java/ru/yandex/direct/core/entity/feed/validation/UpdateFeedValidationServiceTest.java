package ru.yandex.direct.core.entity.feed.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.uac.model.EcomDomain;
import ru.yandex.direct.core.entity.uac.service.EcomDomainsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.gemini.GeminiClient;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.misc.random.Random2;

import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.SetUtils.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.feed.FeedUtilsKt.createFakeFeedUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteContainsDuplicatedUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteForNotAllowedUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteForNotDomainOnlyUrl;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedBySiteWithSameDomainAlreadyExists;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedInvalidFilename;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedInvalidHref;
import static ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedNameCannotBeEmpty;
import static ru.yandex.direct.core.entity.feed.validation.constraints.FeedConstraints.MBI_MAX_LOGIN_LENGTH;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripDomainTail;
import static ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripProtocol;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateFeedValidationServiceTest {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private Steps steps;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private EcomDomainsService ecomDomainsService;

    private UpdateFeedValidationService updateFeedValidationService;

    private ClientInfo clientInfo;
    private int shard;
    private ClientId clientId;
    private Long clientUid;
    private Long operatorUid;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        ppcPropertiesSupport.set(PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE,
                PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE.getType().serialize(emptySet()));

        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(false);

        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        operatorUid = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).getUid();
        updateFeedValidationService = new UpdateFeedValidationService(feedRepository, rbacService,
                ppcPropertiesSupport, geminiClient, ecomDomainsService);
    }

    @Test
    public void validate_urlFeedValues_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        String newName = "newName";
        String newUrl = "http://newurl.com/newFeed.csv";
        mc.process(newName, Feed.NAME);
        mc.process(newUrl, Feed.URL);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_fileFeedValues_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        String newFilename = "newFilename.csv";
        mc.process(newFilename, Feed.FILENAME);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_invalidUrlFeedValues_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        String newName = "";
        String newUrl = "invalid.url";
        mc.process(newName, Feed.NAME);
        mc.process(newUrl, Feed.URL);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        SoftAssertions.assertSoftly(
                soft -> {
                    soft.assertThat(vr)
                            .as("Невалидная ссылка фида")
                            .is(matchedBy(hasDefectDefinitionWith(validationError(
                                    path(index(0), field(Feed.URL)),
                                    feedInvalidHref()
                            ))));
                    soft.assertThat(vr)
                            .as("Невалидное имя фида")
                            .is(matchedBy(hasDefectDefinitionWith(validationError(
                                    path(index(0), field(Feed.NAME)),
                                    feedNameCannotBeEmpty()
                            ))));
                }
        );
    }

    @Test
    public void validate_ipUrl_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        String newUrl = "http://82.202.168.218/feed.xml";
        mc.process(newUrl, Feed.URL);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_invalidIpUrl_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        // Вышли за пределы [0, 255]
        String newUrl = "http://256.202.168.218/feed.xml";
        mc.process(newUrl, Feed.URL);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedInvalidHref()
                ))));
    }

    @Test
    public void validate_fakeUrl_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        String fakeFeedUrl = createFakeFeedUrl(1L, 2L, 3L, "http://ya.ru", false, false);
        mc.process(fakeFeedUrl, Feed.URL);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        invalidValue()
                ))));
    }

    @Test
    public void validate_invalidFileFeedValues_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        String newFilename = "filename.jpg";
        mc.process(newFilename, Feed.FILENAME);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.FILENAME)),
                        feedInvalidFilename()
                ))));
    }

    @Test
    public void validate_loginWithoutPasswordButExists_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        mc.process("login", Feed.LOGIN);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_passwordWithoutLoginButExists_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        mc.process("password", Feed.PLAIN_PASSWORD);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_loginWithPassword_success() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class)
                .process(Random2.R.nextStringSimpleUtf8(MBI_MAX_LOGIN_LENGTH), Feed.LOGIN)
                .process("password", Feed.PLAIN_PASSWORD);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_tooLongLoginWithPassword_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class)
                .process(Random2.R.nextStringSimpleUtf8(MBI_MAX_LOGIN_LENGTH + 1), Feed.LOGIN)
                .process("password", Feed.PLAIN_PASSWORD);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.LOGIN)),
                        maxStringLength(MBI_MAX_LOGIN_LENGTH)
                ))));
    }

    @Test
    public void validate_noWritableOperator_failure() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFileFeed(clientInfo);
        Long feedId = feedInfo.getFeedId();
        ModelChanges<Feed> mc = new ModelChanges<>(feedId, Feed.class);
        mc.process("newName", Feed.NAME);
        ClientInfo badOperator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);
        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, badOperator.getUid(), List.of(mc));
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(),
                        noRights()
                ))));
    }

    @Test
    public void validate_manySiteFeedsUrlsUpdate_success() {
        FeedInfo feed1 = createSiteFeed("https://ya.ru");
        FeedInfo feed2 = createSiteFeed("https://abc.com");
        FeedInfo feed3 = createSiteFeed("http://qwerty.ru");

        ModelChanges<Feed> mc1 = createModelChanges(feed1, "https://new-ya.ru");
        ModelChanges<Feed> mc2 = createModelChanges(feed2, "http://qwerty.ru");
        ModelChanges<Feed> mc3 = createModelChanges(feed3, "http://abc.com");

        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://new-ya.ru", "new-ya.ru",
                        "http://qwerty.ru", "qwerty.ru",
                        "http://abc.com", "abc.com"
                ));

        var vr =
                updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc1, mc2, mc3));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_manySiteFeedsWithDuplicatedDomains_failure() {
        FeedInfo feed1 = createSiteFeed("https://ya.ru");
        FeedInfo feed2 = createSiteFeed("https://abc.com");
        FeedInfo feed3 = createSiteFeed("http://qwerty.ru");

        ModelChanges<Feed> mc1 = createModelChanges(feed1, "https://new-ya.ru");
        ModelChanges<Feed> mc2 = createModelChanges(feed2, "https://ya.ru");
        ModelChanges<Feed> mc3 = createModelChanges(feed3, "http://ya.ru");
        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://new-ya.ru", "new-ya.ru",
                        "https://ya.ru", "ya.ru",
                        "http://ya.ru", "ya.ru"
                ));

        var vr =
                updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc1, mc2, mc3));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(1), field(Feed.URL)),
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
        FeedInfo existingFeed = createSiteFeed("https://ya.ru");

        FeedInfo feed1 = createSiteFeed("https://asd.ru");
        FeedInfo feed2 = createSiteFeed("https://abc.com");
        FeedInfo feed3 = createSiteFeed("http://qwerty.ru");

        ModelChanges<Feed> mc1 = createModelChanges(feed1, "https://new-ya.ru");
        ModelChanges<Feed> mc2 = createModelChanges(feed2, "http://qwerty.ru");
        ModelChanges<Feed> mc3 = createModelChanges(feed3, "http://ya.ru");

        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://new-ya.ru", "new-ya.ru",
                        "http://ya.ru", "ya.ru",
                        "http://qwerty.ru", "qwerty.ru"
                ));
        var vr =
                updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc1, mc2, mc3));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(2), field(Feed.URL)),
                        feedBySiteWithSameDomainAlreadyExists(existingFeed.getFeedId())
                ))));
    }

    @Test
    public void validate_withNotAllowedSiteDomain_failure() {
        FeedInfo feed1 = createSiteFeed("https://asd.ru");
        FeedInfo feed2 = createSiteFeed("https://qwerty.ru");

        ModelChanges<Feed> mc1 = createModelChanges(feed1, "https://new-asd.ru");
        ModelChanges<Feed> mc2 = createModelChanges(feed2, "https://new-qwerty.ru");
        when(geminiClient.getMainMirrors(anyList()))
                .thenReturn(Map.of("https://new-asd.ru", "new-asd.ru",
                        "https://new-qwerty.ru", "www.new-qwerty.ru"
                ));

        ppcPropertiesSupport.set(PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE,
                PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_FEED_FROM_SITE.getType()
                        .serialize(Set.of("www.new-qwerty.ru")));

        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);

        var vr =
                updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc1, mc2));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(1), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://new-qwerty.ru")
                ))));
    }

    @Test
    public void validate_updateSourceSite_success() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru");

        var vr =
                updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_sourceSiteIsDomainOnlyFinishedWithSlash_success() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru/");

        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_sourceSiteIsNotDomainOnly_failure() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru/a");

        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotDomainOnlyUrl("https://yandex.ru/a", "https://yandex.ru")
                ))));
    }

    @Test
    public void validate_withEcomDomains_success() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(10L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(100_000L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(Map.of("https://yandex.ru",
                        new EcomDomain().withDomain("https://yandex.ru").withOffersCount(1000L)));

        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_withEcomDomainsNotEcom_failure() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(10L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(100_000L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(emptyMap());

        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://yandex.ru")
                ))));
    }

    @Test
    public void validate_withEcomDomainsNotEnougthOffers_failure() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(1001L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(100_000L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(Map.of("https://yandex.ru",
                        new EcomDomain().withDomain("https://yandex.ru").withOffersCount(1000L)));

        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://yandex.ru")
                ))));
    }

    @Test
    public void validate_withEcomDomainsTooManyOffers_failure() {
        FeedInfo feed = createSiteFeed("https://yandex.com");
        ModelChanges<Feed> mc = createModelChanges(feed, "https://yandex.ru");
        when(ecomDomainsService.isDomainSuitableEcomCheckNeeded()).thenReturn(true);
        when(ecomDomainsService.getMinOffersForEcomAllowed()).thenReturn(10L);
        when(ecomDomainsService.getMaxOffersForEcomAllowed()).thenReturn(999L);
        when(ecomDomainsService.getEcomDomainsByUrls(anyCollection()))
                .thenReturn(Map.of("https://yandex.ru",
                        new EcomDomain().withDomain("https://yandex.ru").withOffersCount(1000L)));

        var vr = updateFeedValidationService.validate(shard, clientId, clientUid, operatorUid, List.of(mc));

        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feed.URL)),
                        feedBySiteForNotAllowedUrl("https://yandex.ru")
                ))));
    }

    private FeedInfo createSiteFeed(String url) {
        Feed feed = defaultFeed(clientId)
                .withUrl(url)
                .withName("feed name")
                .withTargetDomain(stripDomainTail(stripProtocol(url)))
                .withSource(Source.SITE);

        return steps.feedSteps().createFeed(new FeedInfo().withClientInfo(clientInfo).withFeed(feed));
    }

    private ModelChanges<Feed> createModelChanges(FeedInfo feed, String newUrl) {
        ModelChanges<Feed> mc = new ModelChanges<>(feed.getFeedId(), Feed.class);
        return mc.process(newUrl, Feed.URL);
    }

}

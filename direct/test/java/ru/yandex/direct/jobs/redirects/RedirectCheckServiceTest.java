package ru.yandex.direct.jobs.redirects;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAggregatorDomain;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.domain.service.AggregatorDomainsService;
import ru.yandex.direct.core.entity.redirectcheckqueue.repository.RedirectCheckQueueRepository;
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.RedirectCheckQueueRecord;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.jobs.redirects.RedirectCheckService.MIN_CHUNK_SIZE;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximately;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;


@JobsTest
@ExtendWith(SpringExtension.class)
class RedirectCheckServiceTest {
    private static final int SHARD = 1;

    private static final String HREF1 = "http://domain-before-redirect-1.ru/some/path/";
    private static final String HREF2 = "http://domain-before-redirect-2.ru/some/path/";
    private static final String HREF3 = "http://domain-before-redirect-3.ru/some/path/";
    private static final String HREF4 = "http://domain-before-redirect-4.ru/some/path/";

    private static final String FAILED_HREF = "http://404.ru/some/path/";
    private static final String INVALID_HREF = "http://invalid-domain .com/";

    private static final String REDIRECT_URL1 = "http://domain-after-redirect-1.ru/another/path/";
    private static final String REDIRECT_URL2 = "http://domain-after-redirect-2.ru/another/path/";
    private static final String REDIRECT_URL3 = "http://domain-after-redirect-3.ru/another/path/";
    private static final String REDIRECT_URL4 = "https://vk.com/test?w=wall-1234";

    private static final String REDIRECT_DOMAIN1 = "domain-after-redirect-1.ru";
    private static final String REDIRECT_DOMAIN2 = "domain-after-redirect-2.ru";
    private static final String REDIRECT_DOMAIN3 = "domain-after-redirect-3.ru";
    private static final String REDIRECT_DOMAIN4 = "vk.com";

    private static final String AGGREGATOR_DOMAIN = "test.vk.com";
    private static final String OLD_DOMAIN = "old.domain.ru";
    private static final String FAILED_HREF_DOMAIN = "404.ru";


    private static final String WARGAMING_URL = "https://ad.doubleclick.net/ddm/trackclk/N88301";
    private static final String WARGAMING_REDIRECT = "https://join.worldoftanks.ru/some";
    private static final String WARGAMING_REDIRECT_DOMAIN = "join.worldoftanks.ru";

    @Autowired
    private Steps steps;
    @Autowired
    private RedirectCheckQueueRepository redirectCheckQueueRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private BannerDomainRepository bannerDomainRepository;
    @Autowired
    private AggregatorDomainsService aggregatorDomainsService;
    @Autowired
    private AggregatorDomainsRepository aggregatorDomainsRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private TestBannerRepository testBannerRepository;
    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    private BannerUrlCheckService bannerUrlCheckService;
    private RedirectCacheService redirectCacheService;
    private RedirectCheckService redirectCheckService;

    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;


    @BeforeEach
    void init() {
        bannerUrlCheckService = mock(BannerUrlCheckService.class);
        redirectCacheService = mock(RedirectCacheService.class);
        redirectCheckService = new RedirectCheckService(bannersUrlHelper, redirectCheckQueueRepository,
                bannerUrlCheckService, bannerDomainRepository, domainRepository, bannerTypedRepository,
                aggregatorDomainsService, redirectCacheService);

        mockSuccessfulGetRedirects(HREF1, REDIRECT_URL1, REDIRECT_DOMAIN1);
        mockSuccessfulGetRedirects(HREF2, REDIRECT_URL2, REDIRECT_DOMAIN2);
        mockSuccessfulGetRedirects(HREF4, REDIRECT_URL4, REDIRECT_DOMAIN4);
        mockSuccessfulGetRedirects(WARGAMING_URL, WARGAMING_REDIRECT, WARGAMING_REDIRECT_DOMAIN);
        mockGetRedirects(FAILED_HREF, RedirectCheckResult.createFailResult());

        clientInfo1 = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD));
        clientInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD));
    }

    @Test
    void processQueue_whenSuccessfulGetRedirect() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(HREF1, clientInfo1);
        processQueue();

        checkBanner(bannerId, HREF1, REDIRECT_DOMAIN1);

        List<RedirectCheckQueueRecord> records = testBannerRepository
                .getRedirectQueueRecords(SHARD, singletonList(bannerId));
        assertThat(records, hasSize(0));
    }

    @Test
    void processQueue_whenFailedGetRedirect() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(FAILED_HREF, clientInfo1);
        processQueue();

        checkBanner(bannerId, FAILED_HREF, FAILED_HREF_DOMAIN);

        RedirectCheckQueueRecord record = testBannerRepository
                .getRedirectQueueRecords(SHARD, singletonList(bannerId)).get(0);

        assertThat(record.getTries(), is(1L));
        assertThat(record.getLogtime(), approximately(LocalDateTime.now().plusHours(2)));
    }

    @Test
    void processQueue_whenTwoThreads() {
        List<Long> bannerIds1 = IntStreamEx.range(MIN_CHUNK_SIZE)
                .mapToObj(i -> createBannerWithHrefAndPushIntoQueue(HREF1, clientInfo1))
                .toList();
        List<Long> bannerIds2 = singletonList(createBannerWithHrefAndPushIntoQueue(HREF2, clientInfo2));
        List<Long> bannerIds = StreamEx.of(bannerIds1, bannerIds2).toFlatList(identity());

        processQueue();

        List<BannerWithHref> actualBanners =
                bannerTypedRepository.getSafely(SHARD, bannerIds, BannerWithHref.class);

        Map<Long, String> actualDomainByBannerId =
                listToMap(actualBanners, BannerWithHref::getId, BannerWithHref::getDomain);

        Map<Long, String> expectedDomainByBannerId = StreamEx.of(bannerIds1)
                .mapToEntry(id -> REDIRECT_DOMAIN1)
                .append(bannerIds2.get(0), REDIRECT_DOMAIN2)
                .toMap();
        assertThat(expectedDomainByBannerId, is(actualDomainByBannerId));
    }

    @Test
    void processQueue_whenCachedHref() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(HREF3, clientInfo1);

        RedirectCacheRecord recordFromCache = new RedirectCacheRecord()
                .withHref(HREF3)
                .withRedirectUrl(REDIRECT_URL3)
                .withRedirectDomain(REDIRECT_DOMAIN3);
        when(redirectCacheService.getFromCache(eq(HREF3))).thenReturn(recordFromCache);

        processQueue();

        Mockito.verify(bannerUrlCheckService, never()).getRedirect(eq(HREF3));

        BannerWithHref actualBanner =
                bannerTypedRepository.getSafely(SHARD, singletonList(bannerId), BannerWithHref.class).get(0);

        assertThat(REDIRECT_DOMAIN3, is(actualBanner.getDomain()));
    }

    @Test
    void processQueue_saveToCache_whenSuccessfulGetRedirect() {
        createBannerWithHrefAndPushIntoQueue(HREF1, clientInfo1);

        processQueue();

        Mockito.verify(redirectCacheService).saveToCache(argThat(allOf(
                hasProperty("href", equalTo(HREF1)),
                hasProperty("redirectUrl", equalTo(REDIRECT_URL1)),
                hasProperty("redirectDomain", equalTo(REDIRECT_DOMAIN1)))));
    }

    @Test
    void processQueue_dontSaveToCache_whenFailedGetRedirect() {
        createBannerWithHrefAndPushIntoQueue(FAILED_HREF, clientInfo1);

        processQueue();

        Mockito.verify(redirectCacheService, never()).saveToCache(argThat(
                hasProperty("href", equalTo(FAILED_HREF))));
    }

    @Test
    void processQueue_whenInvalidHref() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(INVALID_HREF, clientInfo1);

        processQueue();

        Mockito.verify(bannerUrlCheckService, never()).getRedirect(eq(INVALID_HREF));

        BannerWithHref actualBanner =
                bannerTypedRepository.getSafely(SHARD, singletonList(bannerId), BannerWithHref.class).get(0);

        assertThat(OLD_DOMAIN, is(actualBanner.getDomain()));
    }

    @Test
    void processQueue_whenAggregatorDomain() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(HREF4, clientInfo1);

        processQueue();

        TextBanner actualBanner =
                bannerTypedRepository.getSafely(SHARD, singletonList(bannerId), TextBanner.class).get(0);
        TextBanner expectedBanner = new TextBanner()
                .withHref(HREF4)
                .withDomain(REDIRECT_DOMAIN4)
                .withAggregatorDomain(AGGREGATOR_DOMAIN)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(BannerStatusModerate.READY);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    void processQueue_whenAggregatorDomain_andFailedGetRedirect() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(FAILED_HREF, clientInfo1);
        aggregatorDomainsRepository.updateAggregatorDomains(SHARD, Map.of(bannerId, AGGREGATOR_DOMAIN));

        processQueue();

        BannerWithAggregatorDomain actualBanner =
                bannerTypedRepository.getSafely(SHARD, singletonList(bannerId), BannerWithAggregatorDomain.class)
                        .get(0);

        assertThat(actualBanner.getAggregatorDomain(), nullValue());
    }

    @Test
    void processQueue_whenAggregatorDomain_andBannerIsDraft() {
        // банер-черновик
        Long bannerId = createBannerAndPushIntoQueue(
                fullTextBanner().withHref(HREF4).withDomain(OLD_DOMAIN)
                        .withStatusBsSynced(StatusBsSynced.NO)
                        .withStatusModerate(BannerStatusModerate.NEW)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                clientInfo1);

        processQueue();

        TextBanner actualBanner =
                bannerTypedRepository.getSafely(SHARD, singletonList(bannerId), TextBanner.class).get(0);
        TextBanner expectedBanner = new TextBanner()
                .withHref(HREF4)
                .withDomain(REDIRECT_DOMAIN4)
                .withAggregatorDomain(AGGREGATOR_DOMAIN)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NO);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }


    @Test
    void processQueue_whenDoubleRedirect() {
        Long bannerId = createBannerWithHrefAndPushIntoQueue(WARGAMING_URL, clientInfo1);
        processQueue();

        checkBanner(bannerId, WARGAMING_URL, WARGAMING_REDIRECT_DOMAIN);

        List<RedirectCheckQueueRecord> records = testBannerRepository
                .getRedirectQueueRecords(SHARD, singletonList(bannerId));
        assertThat(records, hasSize(0));
        Mockito.verify(bannerUrlCheckService, Mockito.only()).getRedirect(eq(WARGAMING_URL), eq(true));
    }


    private void processQueue() {
        redirectCheckService.processQueue(SHARD, true);
    }

    private Long createBannerWithHrefAndPushIntoQueue(String href, ClientInfo clientInfo) {
        return createBannerAndPushIntoQueue(fullTextBanner().withHref(href).withDomain(OLD_DOMAIN), clientInfo);
    }

    private Long createBannerAndPushIntoQueue(TextBanner banner, ClientInfo clientInfo) {
        Long bannerId = steps.textBannerSteps()
                .createBanner(new NewTextBannerInfo()
                        .withBanner(banner)
                        .withClientInfo(clientInfo)).getBannerId();
        redirectCheckQueueRepository.pushBannersIntoQueue(clientInfo.getShard(), singletonList(bannerId));
        return bannerId;
    }

    private void checkBanner(Long bannerId, String expectedHref, String expectedDomain) {


        TextBanner actualBanner =
                bannerTypedRepository.getSafely(SHARD, singletonList(bannerId), TextBanner.class).get(0);

        Long expectedDomainId = domainRepository.getDomains(SHARD, singletonList(expectedDomain)).get(0).getId();
        TextBanner expectedBanner = new TextBanner()
                .withHref(expectedHref)
                .withDomain(expectedDomain)
                .withDomainId(expectedDomainId)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(BannerStatusModerate.READY);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    private void mockSuccessfulGetRedirects(String href, String redirectUrl, String redirectDomain) {
        RedirectCheckResult result = RedirectCheckResult.createSuccessResult(redirectUrl, redirectDomain);
        mockGetRedirects(href, result);
    }

    private void mockGetRedirects(String href, RedirectCheckResult result) {
        when(bannerUrlCheckService.getRedirect(eq(href), anyBoolean())).thenReturn(result);
    }
}

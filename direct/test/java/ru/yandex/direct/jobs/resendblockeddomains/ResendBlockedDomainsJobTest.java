package ru.yandex.direct.jobs.resendblockeddomains;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.filter.BannerFilterFactory;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.resendblockeddomains.model.BadDomainsTitle;
import ru.yandex.direct.jobs.resendblockeddomains.model.BadDomainsTitleStatus;
import ru.yandex.direct.jobs.resendblockeddomains.repository.BadDomainsTitlesRepository;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на джобу {@link ru.yandex.direct.jobs.resendblockeddomains.ResendBlockedDomainsJob}
 */
class ResendBlockedDomainsJobTest {

    private static final int FIRST_SHARD = 1;
    private static final int SECOND_SHARD = 2;
    private static final List<Integer> SHARDS = List.of(FIRST_SHARD, SECOND_SHARD);
    private static final Long DOMAIN_ID = 1L;
    private static final Long CAMPAIGN_ID = 1L;
    private static final Long BANNER_ID1 = 1L;
    private static final Long BANNER_ID2 = 2L;
    private static final String DOMAIN = "a.com";


    private ResendBlockedDomainsJob resendBlockedDomainsJob;
    private BadDomainsTitlesRepository badDomainsTitlesRepository;
    private BannerTypedRepository bannerTypedRepository;
    private BsResyncService bsResyncService;
    private ShardHelper shardHelper;


    @BeforeEach
    void initMocks() {
        badDomainsTitlesRepository = mock(BadDomainsTitlesRepository.class);
        bannerTypedRepository = mock(BannerTypedRepository.class);
        bsResyncService = mock(BsResyncService.class);
        shardHelper = mock(ShardHelper.class);

        resendBlockedDomainsJob = new ResendBlockedDomainsJob(badDomainsTitlesRepository, bannerTypedRepository,
                bsResyncService, shardHelper);
    }

    /**
     * Проверяем, что когда нет доменов на обработку,
     * то методы на переотправку в БК и смену статуса и удаления не вызываются.
     */
    @Test
    void execute_noUnprocessedDomains_noResync() {
        when(badDomainsTitlesRepository.getNotProcessedBadDomainsTitles(anyInt())).thenReturn(Collections.emptyList());

        resendBlockedDomainsJob.execute();

        verify(bsResyncService, never()).addObjectsToResync(anyCollection());
        verify(badDomainsTitlesRepository, never()).markDisablingDomainAsProcessed(anyLong());
        verify(badDomainsTitlesRepository, never()).deleteInStatusForEnabling(anyLong());
    }

    /**
     * Проверяем. что когда у домена в статусе for_disabling нет баннеров,
     * то никакие баннеры не переотправляются в БК и вызывается метод для смены статуса домена.
     */
    @Test
    void execute_forDisablingDomainAndNoBanners_noResync() {
        when(badDomainsTitlesRepository.getNotProcessedBadDomainsTitles(anyInt()))
                .thenReturn(List.of(makeBadDomainTitle(BadDomainsTitleStatus.FOR_DISABLING)));
        when(shardHelper.dbShards()).thenReturn(SHARDS);
        when(bannerTypedRepository.getSafely(eq(FIRST_SHARD), eq(BannerFilterFactory.bannerDomainFilter(List.of(DOMAIN,
                "www." + DOMAIN))), eq(BannerWithHref.class)))
                .thenReturn(Collections.emptyList());
        when(bannerTypedRepository.getSafely(eq(SECOND_SHARD), eq(BannerFilterFactory.bannerDomainFilter(List.of(DOMAIN,
                "www." + DOMAIN))), eq(BannerWithHref.class)))
                .thenReturn(Collections.emptyList());

        resendBlockedDomainsJob.execute();

        verify(bsResyncService, never()).addObjectsToResync(anyCollection());
        verify(badDomainsTitlesRepository).markDisablingDomainAsProcessed(eq(1L));
        verify(badDomainsTitlesRepository, never()).deleteInStatusForEnabling(anyLong());
    }

    /**
     * Проверяем. что когда у домена в статусе for_disabling нет баннеров,
     * то никакие баннеры не переотправляются в БК и вызывается метод для удаления домена.
     */
    @Test
    void execute_forEnablingDomainAndNoBanners_noResync() {
        when(badDomainsTitlesRepository.getNotProcessedBadDomainsTitles(anyInt()))
                .thenReturn(List.of(makeBadDomainTitle(BadDomainsTitleStatus.FOR_ENABLING)));
        when(shardHelper.dbShards()).thenReturn(SHARDS);
        when(bannerTypedRepository.getSafely(eq(FIRST_SHARD), eq(BannerFilterFactory.bannerDomainFilter(List.of(DOMAIN,
                "www." + DOMAIN))), eq(BannerWithHref.class)))
                .thenReturn(Collections.emptyList());
        when(bannerTypedRepository.getSafely(eq(SECOND_SHARD), eq(BannerFilterFactory.bannerDomainFilter(List.of(DOMAIN,
                "www." + DOMAIN))), eq(BannerWithHref.class)))
                .thenReturn(Collections.emptyList());

        resendBlockedDomainsJob.execute();

        verify(bsResyncService, never()).addObjectsToResync(anyCollection());
        verify(badDomainsTitlesRepository, never()).markDisablingDomainAsProcessed(anyLong());
        verify(badDomainsTitlesRepository).deleteInStatusForEnabling(eq(BANNER_ID1));
    }

    /**
     * Проверяем, что когда есть баннеры у доменов из bad_domain_titles, то они передаются для переотправки в БК.
     */
    @Test
    void execute_fetchedBanners_bannersSentToResync() {
        when(badDomainsTitlesRepository.getNotProcessedBadDomainsTitles(anyInt()))
                .thenReturn(List.of(makeBadDomainTitle(BadDomainsTitleStatus.FOR_DISABLING)));
        when(shardHelper.dbShards()).thenReturn(SHARDS);

        when(bannerTypedRepository.getSafely(eq(FIRST_SHARD), eq(BannerFilterFactory.bannerDomainFilter(List.of(DOMAIN,
                "www." + DOMAIN))), eq(BannerWithHref.class)))
                .thenReturn(List.of(new TextBanner()
                        .withId(BANNER_ID1)
                        .withCampaignId(CAMPAIGN_ID)
                        .withDomain(DOMAIN)
                        .withHref("href1")));
        when(bannerTypedRepository.getSafely(eq(SECOND_SHARD), eq(BannerFilterFactory.bannerDomainFilter(List.of(DOMAIN,
                "www." + DOMAIN))), eq(BannerWithHref.class)))
                .thenReturn(List.of(new TextBanner()
                        .withId(BANNER_ID2)
                        .withCampaignId(CAMPAIGN_ID)
                        .withDomain(DOMAIN)
                        .withHref("href2")));

        resendBlockedDomainsJob.execute();

        var expected = List.of(
                new BsResyncItem(BsResyncPriority.RESEND_DOMAINS_WITH_BLOCKED_TITLE, CAMPAIGN_ID, BANNER_ID1, null),
                new BsResyncItem(BsResyncPriority.RESEND_DOMAINS_WITH_BLOCKED_TITLE, CAMPAIGN_ID, BANNER_ID2, null));

        verify(bsResyncService).addObjectsToResync(eq(expected));
        verify(badDomainsTitlesRepository).markDisablingDomainAsProcessed(eq(BANNER_ID1));
        verify(badDomainsTitlesRepository, never()).deleteInStatusForEnabling(anyLong());
    }

    private BadDomainsTitle makeBadDomainTitle(BadDomainsTitleStatus status) {
        return new BadDomainsTitle(DOMAIN_ID, DOMAIN, status);
    }
}

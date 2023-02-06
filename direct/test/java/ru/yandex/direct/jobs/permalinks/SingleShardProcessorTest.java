package ru.yandex.direct.jobs.permalinks;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.yandex.direct.jobs.permalinks.DiffTableChangeType.ADDED;
import static ru.yandex.direct.jobs.permalinks.DiffTableChangeType.DELETED;

@ParametersAreNonnullByDefault
class SingleShardProcessorTest {
    private static final long bid = 123;
    private static final long permalink = 345;
    private static final long chainId = 567;

    private int shard;
    private OrganizationRepository organizationRepository;
    private BannerTypedRepository bannerTypedRepository;
    private SingleShardProcessor processor;
    private BsResyncQueueRepository bsResyncQueueRepository;

    @BeforeEach
    void setUp() {
        shard = 1;
        organizationRepository = mock(OrganizationRepository.class);
        bannerTypedRepository = mock(BannerTypedRepository.class);
        bsResyncQueueRepository = mock(BsResyncQueueRepository.class);
        processor = new SingleShardProcessor(shard, null, null, null, null,
                organizationRepository, bannerTypedRepository, bsResyncQueueRepository);

        when(bannerTypedRepository.getStrictly(eq(shard), anyCollection(), eq(BannerWithSystemFields.class)))
                .then(invocation -> {
                    Collection<Long> ids = (Collection<Long>) invocation.getArguments()[1];
                    return StreamEx.of(ids)
                            .map(id -> new TextBanner().withId(id).withCampaignId(1L).withAdGroupId(2L))
                            .toList();
                });
    }

    @Test
    void processByShard_oneAdded_deleteNotCalled() {
        var row = new DiffTableRow(bid, ADDED, permalink, 0L);
        var shardBids = List.of(bid);
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        rowsByBid.put(bid, row);

        processor.processByShard(shard, shardBids, rowsByBid);

        verify(organizationRepository, times(1)).addRecords(anyInt(), anyCollection());
        verifyNoMoreInteractions(organizationRepository);

        verify(bannerTypedRepository, times(1)).getStrictly(anyInt(), anyCollection(), any());
        verifyNoMoreInteractions(bannerTypedRepository);

        verify(bsResyncQueueRepository, times(1)).addToResync(anyInt(), anyCollection());
        verifyNoMoreInteractions(bsResyncQueueRepository);
    }

    @Test
    void processByShard_twoAdded_deleteNotCalled() {
        var row = new DiffTableRow(bid, ADDED, permalink, 0L);
        var anotherRow = new DiffTableRow(bid, ADDED, 0L, chainId);
        var shardBids = List.of(bid);
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        rowsByBid.put(bid, row);
        rowsByBid.put(bid, anotherRow);

        processor.processByShard(shard, shardBids, rowsByBid);

        verify(organizationRepository, times(1)).addRecords(anyInt(), anyCollection());
        verifyNoMoreInteractions(organizationRepository);
    }

    @Test
    void processByShard_oneDeleted_addNotCalled() {
        var row = new DiffTableRow(bid, DELETED, permalink, 0L);
        var shardBids = List.of(bid);
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        rowsByBid.put(bid, row);

        processor.processByShard(shard, shardBids, rowsByBid);

        verify(organizationRepository, times(1)).deleteRecords(anyInt(), anyCollection());
        verifyNoMoreInteractions(organizationRepository);

        verify(bannerTypedRepository, times(1)).getStrictly(anyInt(), anyCollection(), any());
        verifyNoMoreInteractions(bannerTypedRepository);

        verify(bsResyncQueueRepository, times(1)).addToResync(anyInt(), anyCollection());
        verifyNoMoreInteractions(bsResyncQueueRepository);
    }

    @Test
    void processByShard_twoDeleted_addNotCalled() {
        var row = new DiffTableRow(bid, DELETED, permalink, 0L);
        var anotherRow = new DiffTableRow(bid, DELETED, 0L, chainId);
        var shardBids = List.of(bid);
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        rowsByBid.put(bid, row);
        rowsByBid.put(bid, anotherRow);

        processor.processByShard(shard, shardBids, rowsByBid);

        verify(organizationRepository, times(1)).deleteRecords(anyInt(), anyCollection());
        verifyNoMoreInteractions(organizationRepository);

        verify(bannerTypedRepository, times(1)).getStrictly(anyInt(), anyCollection(), any());
        verifyNoMoreInteractions(bannerTypedRepository);

        verify(bsResyncQueueRepository, times(1)).addToResync(anyInt(), anyCollection());
        verifyNoMoreInteractions(bsResyncQueueRepository);
    }

    @Test
    void processByShard_oneAddedAndOneDeleted() {
        var anotherBid = bid + 1;

        var shardBids = List.of(bid, anotherBid);
        var row = new DiffTableRow(bid, ADDED, permalink, 0L);
        var anotherRow = new DiffTableRow(bid, DELETED, permalink, 0L);
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        rowsByBid.put(bid, row);
        rowsByBid.put(anotherBid, anotherRow);

        processor.processByShard(shard, shardBids, rowsByBid);

        verify(organizationRepository, times(1)).addRecords(anyInt(), anyCollection());
        verify(organizationRepository, times(1)).deleteRecords(anyInt(), anyCollection());
        verifyNoMoreInteractions(organizationRepository);

        verify(bannerTypedRepository, times(1)).getStrictly(anyInt(), anyCollection(), any());
        verifyNoMoreInteractions(bannerTypedRepository);

        verify(bsResyncQueueRepository, times(1)).addToResync(anyInt(), anyCollection());
        verifyNoMoreInteractions(bsResyncQueueRepository);
    }

    @Test
    void processByShard_shardBidsEmpty_noAddsOrDeletes() {
        var row = new DiffTableRow(bid, ADDED, permalink, 0L);
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        rowsByBid.put(bid, row);
        processor.processByShard(shard, emptyList(), rowsByBid);
        verifyZeroInteractions(organizationRepository);
        verifyZeroInteractions(bannerTypedRepository);
        verifyZeroInteractions(bsResyncQueueRepository);
    }

    @Test
    void processByShard_rowsByBidEmpty_noAddsOrDeletes() {
        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        processor.processByShard(shard, List.of(bid), rowsByBid);
        verifyZeroInteractions(organizationRepository);
        verifyZeroInteractions(bannerTypedRepository);
        verifyZeroInteractions(bsResyncQueueRepository);
    }

    @Test
    void processByShard_noShardBidsInRowsByBidEmpty_noAddsOrDeletes() {
        var shardBids = List.of(bid + 1);

        Multimap<Long, DiffTableRow> rowsByBid = HashMultimap.create();
        var row = new DiffTableRow(bid, ADDED, permalink, 0L);
        rowsByBid.put(bid, row);

        processor.processByShard(shard, shardBids, rowsByBid);

        verifyZeroInteractions(organizationRepository);
        verifyZeroInteractions(bannerTypedRepository);
        verifyZeroInteractions(bsResyncQueueRepository);
    }
}

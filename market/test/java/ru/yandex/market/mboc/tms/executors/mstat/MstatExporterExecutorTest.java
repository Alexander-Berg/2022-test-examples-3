package ru.yandex.market.mboc.tms.executors.mstat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.pgupdateseq.PgUpdateSeqRow;
import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.mstat.MstatExporterService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MstatExporterExecutorTest extends BaseDbTestClass {

    private OfferRepository offerRepository;
    private MstatExporterService exporterService;
    private OfferUpdateSequenceService offerUpdateSequenceService;
    private SolomonPushService solomonPushService;
    private MstatExporterExecutor exporterExecutor;
    private Random random;

    @Captor
    private ArgumentCaptor<List<Long>> longListCaptor;

    @Before
    public void setup() {
        exporterService = Mockito.mock(MstatExporterService.class);
        offerRepository = Mockito.mock(OfferRepository.class);
        offerUpdateSequenceService = Mockito.spy(new OfferUpdateSequenceService(
            jdbcTemplate,
            storageKeyValueService,
            solomonPushService
        ));
        solomonPushService = Mockito.mock(SolomonPushService.class);
        exporterExecutor = new MstatExporterExecutor(
            exporterService, offerUpdateSequenceService, storageKeyValueService,
            solomonPushService, offerRepository
        );
        random = new Random(System.currentTimeMillis());
    }

    @Test
    public void testItDoesntFailOnEmptyInput() {
        Mockito.doReturn(Collections.emptyList()).when(offerUpdateSequenceService).getModifiedRecordsIdBatch(anyLong(), anyInt());
        Mockito.doReturn(random.nextLong()).when(offerUpdateSequenceService).getNextModifiedSeqId(anyLong());
        exporterExecutor.execute();
    }

    @Test
    public void testItShouldCallAndRecordAsNeeded() {
        var lastSeqId = 200L;
        var batchSize = 10;
        var batchCount = 20;
        init(lastSeqId, batchSize, batchCount);

        Mockito.doReturn(
                LongStream.range(1, 200)
                    .mapToObj(seq -> new PgUpdateSeqRow<>(seq, seq + lastSeqId, Instant.now(), Instant.now()))
                    .collect(Collectors.toList()))
            .when(offerUpdateSequenceService).getModifiedRecordsIdBatch(anyLong(), anyInt());

        Mockito.doAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            return new MstatExporterService.MstatExportResult(LocalDateTime.now(), CollectionUtils.last(ids));
        }).when(exporterService).updateOfferByIds(any());

        exporterExecutor.execute();
        assertThat(storageKeyValueService.getLong("mstat_export_seq_id", -1L)).isEqualTo(399L);
    }

    @Test
    public void testMaxOKShouldBeRecorded() {
        var lastSeqId = 0;
        var batchSize = 10;
        var batchCount = 20;
        init(lastSeqId, batchSize, batchCount);

        var batchList = LongStream.range(lastSeqId + 1, 200)
            .mapToObj(seq -> new PgUpdateSeqRow<>(seq, seq, Instant.now(), Instant.now()))
            .collect(Collectors.toList());

        when(offerUpdateSequenceService.getModifiedRecordsIdBatch(anyLong(), anyInt()))
            .thenReturn(batchList);

        var throwExceptionOnBatchNum = 4;
        var expectedLastSeqId = lastSeqId + batchSize * (throwExceptionOnBatchNum - 1);
        long throwOnOfferId = lastSeqId + batchSize * throwExceptionOnBatchNum;

        Mockito.doAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            if (ids.contains(throwOnOfferId)) {
                throw new IllegalArgumentException("Surprise!");
            }
            return new MstatExporterService.MstatExportResult(LocalDateTime.now(), CollectionUtils.last(ids));
        }).when(exporterService).updateOfferByIds(any());

        assertThatThrownBy(() -> exporterExecutor.execute()).hasMessageContaining("Surprise");
        assertThat(storageKeyValueService.getLong("mstat_export_seq_id", -1L)).isEqualTo(expectedLastSeqId);
    }

    @Test
    public void shouldThrowExceptionWhenHaveNorModifiedSeqId() {
        assertThatThrownBy(() -> exporterExecutor.execute()).hasMessageContaining("Couldn't find next modifiedSeqId");
    }

    @Test
    public void testBatchesRespectServiceOfferParts() {
        var lastSeqId = 200L;
        var batchSize = 55;
        var batchCount = 20;
        init(lastSeqId, batchSize, batchCount);

        Mockito.doReturn(
                LongStream.range(1, 6)
                    .mapToObj(seq -> new PgUpdateSeqRow<>(seq, seq + lastSeqId, Instant.now(), Instant.now()))
                    .collect(Collectors.toList()))
            .when(offerUpdateSequenceService).getModifiedRecordsIdBatch(anyLong(), anyInt());

        Mockito.doReturn(
                LongStream.range(1, 4) // 2 last is omitted
                    .mapToObj(i -> Pair.of(i, (int) i * 50))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))
            .when(offerRepository).countServicePartsForOfferIds(anyCollection());

        Mockito.doAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            return new MstatExporterService.MstatExportResult(LocalDateTime.now(), CollectionUtils.last(ids));
        }).when(exporterService).updateOfferByIds(any());

        exporterExecutor.execute();
        assertThat(storageKeyValueService.getLong("mstat_export_seq_id", -1L)).isEqualTo(205L);

        verify(exporterService, new Times(3))
            .updateOfferByIds(longListCaptor.capture());

        assertThat(longListCaptor.getAllValues()).containsExactlyInAnyOrder(
            List.of(1L, 2L),
            List.of(3L),
            List.of(4L, 5L) // 2 last counts as 1 service offer
        );

        Mockito.reset(offerRepository);
    }

    private void init(long lastSeqId, int batchSize, int batchCount) {
        storageKeyValueService.putValue("mstat_export_seq_id", lastSeqId);
        storageKeyValueService.putValue("mstat_export_batch_size", batchSize);
        storageKeyValueService.putValue("mstat_export_batch_count", batchCount);
        Mockito.doReturn(random.nextLong()).when(offerUpdateSequenceService).getNextModifiedSeqId(anyLong());
    }
}

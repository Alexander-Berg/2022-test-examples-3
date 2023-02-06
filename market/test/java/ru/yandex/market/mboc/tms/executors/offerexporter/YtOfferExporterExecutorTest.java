package ru.yandex.market.mboc.tms.executors.offerexporter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.pgupdateseq.PgUpdateSeqRow;
import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.mstat.MstatExporterService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


public class YtOfferExporterExecutorTest extends BaseDbTestClass {
    private MstatExporterService exporterService;
    private OfferUpdateSequenceService offerUpdateSequenceService;
    private SolomonPushService solomonPushService;
    private YtOfferExporterExecutor exporterExecutor;
    private Random random;

    @Before
    public void setup() {
        exporterService = Mockito.mock(MstatExporterService.class);
        offerUpdateSequenceService = Mockito.spy(new OfferUpdateSequenceService(
            jdbcTemplate,
            storageKeyValueService,
            solomonPushService
        ));
        solomonPushService = Mockito.mock(SolomonPushService.class);
        exporterExecutor = new YtOfferExporterExecutor(
            exporterService, offerUpdateSequenceService, storageKeyValueService,
            solomonPushService
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
        assertThat(storageKeyValueService.getLong(YtOfferExporterExecutor.SEQ_ID, -1L)).isEqualTo(399L);
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
        assertThat(storageKeyValueService.getLong(YtOfferExporterExecutor.SEQ_ID, -1L))
            .isEqualTo(expectedLastSeqId);
    }

    @Test
    public void shouldThrowExceptionWhenHaveNorModifiedSeqId() {
        assertThatThrownBy(() -> exporterExecutor.execute()).hasMessageContaining("Couldn't find next modifiedSeqId");
    }

    private void init(long lastSeqId, int batchSize, int batchCount) {
        storageKeyValueService.putValue(YtOfferExporterExecutor.SEQ_ID, lastSeqId);
        storageKeyValueService.putValue(YtOfferExporterExecutor.BATCH_SIZE, batchSize);
        storageKeyValueService.putValue(YtOfferExporterExecutor.BATCH_COUNT, batchCount);
        Mockito.doReturn(random.nextLong()).when(offerUpdateSequenceService).getNextModifiedSeqId(anyLong());
    }
}

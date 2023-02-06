package ru.yandex.market.delivery.tracker.service.logger;

import java.time.Instant;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.tracker.domain.dto.PartnerCreatedBatchesDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushOrderStatusLoggerTest {

    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private PushOrderStatusLogger pushOrderStatusLogger;

    @Test
    void log() {
        String startTsStr = "2021-10-11 12:13:14.000";
        String endTsStr = "2021-10-11 12:13:15.000";
        when(tskvLogger.formatDate(any())).thenReturn(startTsStr, endTsStr);
        var description = new PartnerCreatedBatchesDescription(10, 5, 41, 25);

        pushOrderStatusLogger.log(Instant.now(), Instant.now(), description);

        verify(tskvLogger).log(
            eq(
                ImmutableMap.<String, String>builder()
                    .put("startTs", startTsStr)
                    .put("endTs", endTsStr)
                    .put("partnerId", String.valueOf(description.getPartnerId()))
                    .put("batchSize", String.valueOf(description.getBatchSize()))
                    .put("batchesCreated", String.valueOf(description.getBatchesCreated()))
                    .put("tracksBatched", String.valueOf(description.getTracksBatched()))
                    .build()
            )
        );
    }
}

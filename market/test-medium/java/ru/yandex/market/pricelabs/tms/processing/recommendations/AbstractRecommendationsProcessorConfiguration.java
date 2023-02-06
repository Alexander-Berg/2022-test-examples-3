package ru.yandex.market.pricelabs.tms.processing.recommendations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.recommendation.SyncInfo;
import ru.yandex.market.pricelabs.processing.recommendations.repository.SyncInfoRepository;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;

public class AbstractRecommendationsProcessorConfiguration extends AbstractTmsSpringConfiguration {

    @Autowired
    protected SyncInfoRepository syncInfoRepository;

    protected void insertSyncInfo(String tableName, Instant syncedAt, String timeStr) {
        syncInfoRepository.upsert(new SyncInfo(tableName, syncedAt, getSyncId(timeStr)));
    }

    protected void assertSyncInfo(String tableName, Instant syncedAt, String timeStr) {
        Optional<SyncInfo> syncInfo = syncInfoRepository.findByTableName(tableName);
        Assertions.assertTrue(syncInfo.isPresent());
        Assertions.assertEquals(
                new SyncInfo(tableName, syncedAt, getSyncId(timeStr)),
                syncInfo.get()
        );
    }
    protected String getSyncId(String timeStr) {
        return String.valueOf(
                LocalDateTime.parse(timeStr)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );
    }
}

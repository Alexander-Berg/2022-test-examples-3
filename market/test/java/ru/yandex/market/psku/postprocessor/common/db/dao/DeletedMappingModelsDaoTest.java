package ru.yandex.market.psku.postprocessor.common.db.dao;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MonitoringLevel;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MonitoringType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MonitoringState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MonitoringStateHistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.MONITORING_STATE_HISTORY;

public class DeletedMappingModelsDaoTest extends BaseDBTest {

    @Autowired
    DeletedMappingModelsDao deletedMappingModelsDao;

    @Test
    public void getLongProcessingModelsCount() {
        Timestamp freshTs = Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES));
        Timestamp oldTs = Timestamp.from(Instant.now().minus(7, ChronoUnit.HOURS));
        deletedMappingModelsDao.insert(
            new DeletedMappingModels(1L, CleanupStatus.NO_MAPPINGS, freshTs, freshTs, 0L),
            new DeletedMappingModels(2L, CleanupStatus.SUCCESS, oldTs, oldTs, 0L),
            new DeletedMappingModels(3L, CleanupStatus.READY_FOR_PROCESSING, freshTs, freshTs, 0L),
            new DeletedMappingModels(4L, CleanupStatus.READY_FOR_MBOC, oldTs, oldTs, 0L)
        );

        int count = deletedMappingModelsDao.getLongProcessingModelsCount(Duration.ofHours(6));

        assertThat(count).isEqualTo(1);
    }
}

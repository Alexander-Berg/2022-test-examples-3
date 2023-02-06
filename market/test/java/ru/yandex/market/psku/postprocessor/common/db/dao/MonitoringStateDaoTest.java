package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MonitoringLevel;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MonitoringType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MonitoringState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MonitoringStateHistory;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.MONITORING_STATE_HISTORY;

public class MonitoringStateDaoTest extends BaseDBTest {

    @Autowired
    MonitoringStateDao monitoringStateDao;

    @Test
    public void rotateAllShouldMoveAllRecordToHistory() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        monitoringStateDao.insert(
            new MonitoringState(
                null, MonitoringType.FAILED_TASKS, timestamp, "message_1", MonitoringLevel.CRIT
            ),
            new MonitoringState(
                null, MonitoringType.FLAPPING_TASKS, timestamp, "message_2", MonitoringLevel.WARN
            )
        );
        monitoringStateDao.refreshActualState(Collections.emptyList());
        assertThat(monitoringStateDao.findAll()).isEmpty();
        List<MonitoringStateHistory> monitoringStateHistories = dsl().selectFrom(MONITORING_STATE_HISTORY)
            .fetchInto(MonitoringStateHistory.class);
        assertThat(monitoringStateHistories).hasSize(2)
            .allSatisfy(ms -> assertThat(ms).hasNoNullFieldsOrProperties());
    }
}
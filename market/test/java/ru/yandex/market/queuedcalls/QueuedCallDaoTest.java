package ru.yandex.market.queuedcalls;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.queuedcalls.model.TestQCType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class QueuedCallDaoTest extends AbstractQueuedCallTest {

    private static final String SQL_INSERT = "insert into queued_calls " +
            "(id, call_type, object_id, created_at, next_try_at, tries_count, last_try_error, processing_by, " +
            "processing_started_at, processor_last_heartbeat, processed_at, trace_id, payload, order_id)" +
            "values " +
            "(?, ?, ?, ?, ?, 0, null, null, null, null, null, ?, ?, ?)";
    private static final String SQL_TRUNCATE = "truncate table queued_calls";

    private static final long QC_ID = 1;
    private static final long OBJ_ID = 1;
    private static final long ORDER_ID = 1;
    private static final QueuedCallType QUEUED_CALL_TYPE = TestQCType.FIRST;

    @BeforeEach
    void initDb() {
        transactionTemplate.execute(ts -> {
            jdbcTemplate.update(
                    SQL_INSERT,
                    QC_ID,
                    QUEUED_CALL_TYPE.getId(),
                    OBJ_ID,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(10),
                    null, null,
                    ORDER_ID
            );
            return null;
        });

    }

    @AfterEach
    void tearDownDb() {
        transactionTemplate.execute(ts -> {
            jdbcTemplate.execute(SQL_TRUNCATE);
            return null;
        });
    }

    @Test
    void notCompletedCallsForObjectEmptyTypesTest() {
        assertThat(qcDao.notCompletedCallsForObject(List.of(), OBJ_ID), hasSize(0));
    }

    @Test
    void notCompletedCallsForObjectEmptyObjectIdsTest() {
        assertThat(qcDao.notCompletedCallsForObject(List.of(TestQCType.FIRST), List.of()), hasSize(0));
    }

    @Test
    void notCompletedCallsForObjectEmptyTypesObjectIdsTest() {
        assertThat(qcDao.notCompletedCallsForObject(List.of(), List.of(OBJ_ID)), hasSize(0));
    }

    @Test
    void existsNotCompletedCallsForObjectsEmptyTypesTest() {
        assertThat(qcDao.existsNotCompletedCallsForObjects(List.of(), List.of(OBJ_ID)), hasSize(0));
    }

    @Test
    void existsNotCompletedCallsForObjectsEmptyObjectIdsTest() {
        assertThat(qcDao.existsNotCompletedCallsForObjects(List.of(TestQCType.FIRST), List.of()), hasSize(0));
    }

    @Test
    void allCallsForObjectEmptyTypesTest() {
        assertThat(qcDao.allCallsForObject(List.of(), OBJ_ID), hasSize(0));
    }

    @Test
    void allCallsForObjectTest() {
        assertThat(qcDao.allCallsForObject(List.of(TestQCType.FIRST), OBJ_ID), hasSize(1));
    }

    @Test
    void existsNotCompletedCallsForObjectsTest() {
        assertThat(qcDao.existsNotCompletedCallsForObjects(List.of(TestQCType.FIRST), List.of(OBJ_ID)), hasSize(1));
    }

    @Test
    void notCompletedCallsForObjectObjectIdsTest() {
        assertThat(qcDao.notCompletedCallsForObject(List.of(TestQCType.FIRST), List.of(OBJ_ID)), hasSize(1));
    }

    @Test
    void notCompletedCallsForObjectObjectIdTest() {
        assertThat(qcDao.notCompletedCallsForObject(List.of(TestQCType.FIRST), OBJ_ID), hasSize(1));
    }

}

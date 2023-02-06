package ru.yandex.market.sc.core.domain.archive.schrodingerbox.legacy;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import lombok.Builder;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.archive.repository.Archive;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveRepository;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntry;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntryRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.external.transfermanager.TransferManagerService;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@EmbeddedDbTest
public class MarkStepTest {
    @Autowired
    MarkStepOrderScanLog markStepOrderScanLog;

    @Autowired
    TestFactory testFactory;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ArchiveRepository archiveRepository;

    @Autowired
    OrderScanLogEntryRepository scanLogRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurationService configurationService;

    @MockBean
    TransferManagerService transferManagerService;

    private static final String INSERT_INTO_ORDER_SCAN_LOG = """
            INSERT INTO order_scan_log (
                created_at, updated_at, scanned_at,
                sorting_center_id, external_order_id,
                operation, context, result, archive_id
            ) VALUES (
                now() - ${days} * interval '1 days',
                now() - ${days} * interval '1 days',
                now() - ${days} * interval '1 days',
                ${sortingCenterId}, '${orderExternalId}',
                 'SCAN', 'SORT', 'OK', ${archiveId}
            ) RETURNING id;

        """;

    private SortingCenter sortingCenter;
    private OrderLike order;
    private User user;

    @BeforeEach
    void beforeAll() {
        sortingCenter = testFactory.storedSortingCenter();
        order = testFactory.createOrder(sortingCenter).get();
        user = testFactory.storedUser(sortingCenter, 1000);
        enableDbPurifier();
    }

    @Transactional
    @ParameterizedTest
    @ValueSource(longs = {100, 1000, 10000})
    void checkIfRecordsArchivedAfterThreshold(Long daysToPersistInDatabase) {
        DbPurifierMarkTestParams params = DbPurifierMarkTestParams.builder()
                .daysToPersistInDB(daysToPersistInDatabase)
                .scanLogAge(daysToPersistInDatabase)
                .build();

        checkIfRecordsArchivedAfterThreshold(params);
    }



    @Transactional
    @Test
    void checkIfRecordPersistForReasonableAmountOfTimeIfNullPassed() {
        DbPurifierMarkTestParams params = DbPurifierMarkTestParams.builder()
                .daysToPersistInDB(null)
                .scanLogAge(MarkStepOrderScanLog.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                .build();

        checkIfRecordsArchivedAfterThreshold(params);
    }

    @Transactional
    @Test
    void checkIfRecordPersistForReasonableAmountOfTimeIfDefaultMethodInvoked() {
        DbPurifierMarkTestParams params = DbPurifierMarkTestParams.builder()
                .executeDefaultMethod(true)
                .scanLogAge(MarkStepOrderScanLog.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                .build();

        checkIfRecordsArchivedAfterThreshold(params);
    }

    @Transactional
    @Test
    void checkOtherArchivesAreNotAffected() {
        DbPurifierMarkTestParams params = DbPurifierMarkTestParams.builder()
                .executeDefaultMethod(true)
                .scanLogAge(MarkStepOrderScanLog.REASONABLE_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE)
                .scanLogBelongToOtherArchive(true)
                .build();

        checkIfRecordsArchivedAfterThreshold(params);
    }

    @Transactional
    @ParameterizedTest
    @ValueSource(longs = {-1000, -1, 0, 1, 1, 2})
    void checkThatYouCantArchiveWholeTable(Long daysToPersistInDatabase) {
        DbPurifierMarkTestParams params = DbPurifierMarkTestParams.builder()
                .daysToPersistInDB(daysToPersistInDatabase)
                .scanLogAge(daysToPersistInDatabase)
                .expectException(true)
                .build();

        checkIfRecordsArchivedAfterThreshold(params);
    }
    void checkIfRecordsArchivedAfterThreshold(DbPurifierMarkTestParams params) {
        //test params
        Long scanLogAge = params.scanLogAge;
        Long daysToPersistInDatabase = params.daysToPersistInDB;
        boolean executeDefaultMethod = params.executeDefaultMethod;
        boolean expectException = params.expectException;
        boolean otherArchive = params.scanLogBelongToOtherArchive;
        Long otherArchiveId = otherArchive ? 987654321L : null;

        //test data preparation
        long scanLogToBeArchivedId = createScanLog(sortingCenter, order, scanLogAge + 1, otherArchiveId);
        long scanLogNotToBeArchivedId = createScanLog(sortingCenter, order, scanLogAge, null);

        assertThat(archiveRepository.findAll()).isEmpty();

        //tested method
        if (expectException) {
            assertThatThrownBy(() -> executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod));
        } else {
            executeTestedMethod(daysToPersistInDatabase, executeDefaultMethod);
        }

        //hacky hacks to refresh cache
        entityManager.flush();
        scanLogRepository.findAll();

        //result check

        if (expectException) {
            assertThat(archiveRepository.findAll()).isEmpty();
        } else {
            //archive created and is in proper status
            List<Archive> archives = archiveRepository.findAll();
            assertThat(archives.size()).isEqualTo(1);
            Archive archive = archives.get(0);
            assertThat(archive.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_FINISHED);
        }


        Optional<OrderScanLogEntry> logToBeArchived = scanLogRepository.findById(scanLogToBeArchivedId);
        assertThat(logToBeArchived).isPresent();
        var archiveAssert = assertThat(logToBeArchived.get().getArchiveId());

        if (expectException) {
            archiveAssert.isNull();
        } else {
            if (otherArchive) {
                archiveAssert.isEqualTo(otherArchiveId);
            } else {
                archiveAssert.isNotNull();
            }
        }

        Optional<OrderScanLogEntry> logNotToBeArchived = scanLogRepository.findById(scanLogNotToBeArchivedId);
        assertThat(logNotToBeArchived).isPresent();
        assertThat(logNotToBeArchived.get().getArchiveId()).isNull();

    }

    private void executeTestedMethod(Long daysToPersistInDatabase, boolean executeDefaultMethod) {
        if (executeDefaultMethod) {
            markStepOrderScanLog.mark();
        } else {
            markStepOrderScanLog.mark(daysToPersistInDatabase);
        }
    }

    @Builder
    static class DbPurifierMarkTestParams {
        Long scanLogAge;
        Long daysToPersistInDB;
        boolean executeDefaultMethod;
        boolean expectException;
        boolean scanLogBelongToOtherArchive;

    }

    private void enableDbPurifier() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }


    private long createScanLog(SortingCenter sortingCenter, OrderLike order, long daysInPast, Long archiveId) {
        Map<String, Object> data = new HashMap<>();
        data.put("days", daysInPast);
        data.put("sortingCenterId", sortingCenter.getId());
        data.put("orderExternalId", order.getExternalId());
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        String readyInsert = StrSubstitutor.replace(INSERT_INTO_ORDER_SCAN_LOG, data);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);

        long scanLogId = keyHolder.getKey().longValue();
        return scanLogId;
    }
}

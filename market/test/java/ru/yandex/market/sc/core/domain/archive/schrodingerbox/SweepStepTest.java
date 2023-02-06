package ru.yandex.market.sc.core.domain.archive.schrodingerbox;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.archive.ArchiveQueryService;
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


@EmbeddedDbTest
public class SweepStepTest {
    @Autowired
    SweepStep sweepStep;

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
        enableArchiving();
        enableArchivingDelete();
    }


    @Transactional
    @Test
    void testOnlyArchiveInProperStatusAreDeleted() {
        List<Archive> archivesToPersist = new ArrayList<>();
        List<Archive> archivesToDelete = new ArrayList<>();
        HashMap<Long, Archive> scanLogIdToArchive = new HashMap<>();
        HashMap<Long, ArchiveStatus> initialArchiveStatus = new HashMap<>();
        List<Long> scanLogIdToPersist = new ArrayList<>();
        List<Long> scanLogIdToDelete = new ArrayList<>();

        for (ArchiveStatus status : ArchiveStatus.values()) {
            Archive archive = new Archive(status);
            archive = archiveRepository.save(archive);
            long scanLogId = createScanLog(sortingCenter, order,
                    ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE, archive.getId());

            if (status == ArchiveStatus.VERIFICATION_FINISHED || status == ArchiveStatus.DELETE_FAILED) {
                archivesToDelete.add(archive);
                scanLogIdToDelete.add(scanLogId);
            } else {
                archivesToPersist.add(archive);
                scanLogIdToPersist.add(scanLogId);
            }
            scanLogIdToArchive.put(scanLogId, archive);
            initialArchiveStatus.put(archive.getId(), status);
        }

        //don't delete records without archive
        long scanLogIdWithoutArchive = createScanLog(sortingCenter, order,
                    ArchiveQueryService.MINIMUM_AMOUNT_OF_DAYS_TO_PERSIST_IN_DATABASE, null);
        scanLogIdToPersist.add(scanLogIdWithoutArchive);


        //tested method
        sweepStep.sweep();

        //hacky hacks to refresh cache
        entityManager.flush();
        scanLogRepository.findAll();

        //result check
        for (Long id : scanLogIdToDelete) {
            Optional<OrderScanLogEntry> scanLog = scanLogRepository.findById(id);
            assertThat(scanLog).isEmpty();
        }

        for (Long id : scanLogIdToPersist) {
            Optional<OrderScanLogEntry> scanLogO = scanLogRepository.findById(id);
            assertThat(scanLogO).isPresent();
            OrderScanLogEntry scanLog = scanLogO.get();
            assertThat(scanLog.getArchiveId()).isEqualTo(
                    Optional.ofNullable(scanLogIdToArchive.get(id)).map(Archive::getId).orElse(null)
            );
        }

        for (Archive archive : archivesToDelete) {
            Optional<Archive> archiveToDelete = archiveRepository.findById(archive.getId());
            assertThat(archiveToDelete).isPresent();
            assertThat(archiveToDelete.get().getArchiveStatus()).isEqualTo(ArchiveStatus.DELETED);
        }

        for (Archive archive : archivesToPersist) {
            Optional<Archive> archiveToPersist = archiveRepository.findById(archive.getId());
            assertThat(archiveToPersist).isPresent();
            assertThat(archiveToPersist.get().getArchiveStatus()).isEqualTo(initialArchiveStatus.get(archive.getId()));
        }
    }

    private void enableArchiving() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }

    private void enableArchivingDelete() {
        configurationService.mergeValue(ConfigurationProperties.DB_ARCHIVING_DELETE_ENABLED_PROPERTY, true);
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

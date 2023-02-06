package ru.yandex.market.mbo.mdm.common.masterdata.yt;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.http.HttpUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.pgaudit.PgAuditCleanerService;
import ru.yandex.market.mbo.pgaudit.PgAuditPartitionManager;
import ru.yandex.market.mbo.pgaudit.PgAuditRecord;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class ArchiveMdmAuditToYTServiceTest extends MdmBaseIntegrationTestClass {

    private static final int YT_INIT_TIMEOUT = 10;
    private static final int ITEMS_BATCH_SIZE = 10;

    private static final int SEED = 463;

    private static final int VALID_SUPPLIER_ID = 12345;

    private EnhancedRandom random;

    @Value("${market.mdm.yt.audit-archive-path.test-prefix}")
    private String auditTablePrefix;

    @Autowired
    @Qualifier("hahnYtHttpApi")
    private UnstableInit<Yt> hahnYtHttpApi;

    @Autowired
    private UnstableInit<AuditYtWriter> unstableAuditYtWriter;

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;

    @Autowired
    private PgAuditRepository pgAuditRepository;

    @Autowired
    private PgAuditCleanerService pgAuditCleanerService;

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Autowired
    private PgAuditPartitionManager pgAuditPartitionManager;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionHelper transactionHelper;

    private ArchiveMdmAuditToYTService archiveMdmAuditToYTService;

    private Yt yt;
    private YPath root;

    @Before
    public void setUp() throws Exception {
        pgAuditRepository.clearAll();

        random = TestDataUtils.defaultRandom(SEED);
        yt = hahnYtHttpApi.get(YT_INIT_TIMEOUT, TimeUnit.SECONDS);

        root = YPath.simple(auditTablePrefix).child(UUID.randomUUID().toString());
        yt.cypress().create(
            root, CypressNodeType.MAP, true, false,
            Map.of("expiration_time",
                YTree.stringNode(HttpUtils.YT_INSTANT_FORMATTER.format(Instant.now().plus(Duration.ofDays(1)))))
        );
        YPath auditPath = root.child("mdm_audit");
        AuditYtWriter auditYtWriter = unstableAuditYtWriter.get(YT_INIT_TIMEOUT, TimeUnit.SECONDS);
        archiveMdmAuditToYTService = new ArchiveMdmAuditToYTService(
            pgAuditRepository,
            pgAuditCleanerService,
            auditYtWriter,
            storageKeyValueService,
            auditPath.toString(),
            true,
            jdbcTemplate,
            pgAuditPartitionManager,
            transactionHelper
        );
        storageKeyValueService.putValue(MdmProperties.AUDIT_ARCHIVE_SPLIT_MODE, false);
    }

    @After
    public void tearDown() throws Exception {
        yt.cypress().remove(root);
    }

    private List<FromIrisItemWrapper> generateAndStoreItems() {
        List<FromIrisItemWrapper> itemsBatch = random.objects(MdmIrisPayload.Item.class, ITEMS_BATCH_SIZE)
            .map(item ->
                item.toBuilder().setItemId(item.getItemId().toBuilder().setSupplierId(VALID_SUPPLIER_ID)).build())
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());
        fromIrisItemRepository.insertBatch(itemsBatch);
        return itemsBatch;
    }

    @Test
    public void whenCollectRecodsBatchShouldReturnFirstRecordsParallel() {
        storageKeyValueService.putValue(MdmProperties.AUDIT_ARCHIVE_SPLIT_MODE, true);
        whenCollectRecodsBatchShouldReturnFirstRecords();
    }

    @Test
    public void whenCollectRecodsBatchShouldReturnFirstRecords() {
        pgAuditRepository.clearAll();
        List<PgAuditRecord> initialRecords = archiveMdmAuditToYTService.collectAuditRecordsBatch(
            Instant.now(), 0L, ITEMS_BATCH_SIZE
        );
        Assertions.assertThat(initialRecords).isEmpty();

        List<FromIrisItemWrapper> itemsBatch = generateAndStoreItems();
        List<PgAuditRecord> recordsBatch = pgAuditRepository.findAll();
        Assertions.assertThat(recordsBatch).hasSameSizeAs(itemsBatch);

        int largerBatchSize = ITEMS_BATCH_SIZE * 100;
        List<PgAuditRecord> auditRecordsFromService = archiveMdmAuditToYTService.collectAuditRecordsBatch(
            Instant.now(), 0L, largerBatchSize
        );
        Assertions.assertThat(auditRecordsFromService)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(recordsBatch);

        int lesserBatchSize = ITEMS_BATCH_SIZE / 2;
        List<PgAuditRecord> auditRecordsFromService2 = archiveMdmAuditToYTService.collectAuditRecordsBatch(
            Instant.now(), 0L, lesserBatchSize
        );
        Assertions.assertThat(auditRecordsFromService2).hasSize(lesserBatchSize);
        Assertions.assertThat(auditRecordsFromService2)
            .usingRecursiveFieldByFieldElementComparator()
            .isSubsetOf(recordsBatch);
    }

    @Test
    public void whenExecuteArchiveJobShouldMoveRecordsToYtParallel() {
        storageKeyValueService.putValue(MdmProperties.AUDIT_ARCHIVE_SPLIT_MODE, true);
        whenExecuteArchiveJobShouldMoveRecordsToYt();
    }

    @Test
    public void whenExecuteArchiveJobShouldMoveRecordsToYt() {
        pgAuditRepository.clearAll();
        List<FromIrisItemWrapper> itemsBatch = generateAndStoreItems();
        List<PgAuditRecord> recordsBatch = pgAuditRepository.findAll();
        Assertions.assertThat(recordsBatch).hasSameSizeAs(itemsBatch);

        Instant upperLimit = Instant.now();
        YPath outputTable = archiveMdmAuditToYTService.moveAuditRecordsToYt(upperLimit);

        List<PgAuditRecord> ytRecords = new ArrayList<>();
        yt.tables().read(outputTable, YTableEntryTypes.JACKSON_UTF8, node -> {
            ytRecords.add(AuditYtWriter.fromJsonNode(node));
        });
        Assertions.assertThat(ytRecords).hasSameSizeAs(recordsBatch);
        Assertions.assertThat(ytRecords)
            .usingRecursiveFieldByFieldElementComparator()
            .isSubsetOf(recordsBatch);

        List<PgAuditRecord> auditRecordsAfter = archiveMdmAuditToYTService
            .collectAuditRecordsBatch(Instant.now(), 0L, ITEMS_BATCH_SIZE);
        Assertions.assertThat(auditRecordsAfter).isEmpty();
    }
}

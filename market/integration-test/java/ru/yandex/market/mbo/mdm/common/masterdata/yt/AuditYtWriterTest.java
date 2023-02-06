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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.http.HttpUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.pgaudit.PgAuditRecord;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class AuditYtWriterTest extends MdmBaseIntegrationTestClass {
    private static final int SEED = -4;
    private static final int YT_INIT_TIMEOUT = 10;
    private static final int DATASET_SIZE = 10;

    private static final int VALID_SUPPLIER_ID = 12345;

    private static final String TEST_TABLE_1 = "mdm_audit1";
    private static final String TEST_TABLE_2 = "mdm_audit2";
    private static final String TEST_TABLE_3 = "mdm_audit3";


    private EnhancedRandom random;

    private AuditYtWriter auditYtWriter;

    @Value("${market.mdm.yt.audit-archive-path.test-prefix}")
    private String auditTablePrefix;

    @Autowired
    @Qualifier("hahnYtHttpApi")
    private UnstableInit<Yt> hahnYtHttpApi;

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;

    @Autowired
    private PgAuditRepository pgAuditRepository;

    private Yt yt;
    private YPath root;
    private YPath auditTable;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
        yt = hahnYtHttpApi.get(YT_INIT_TIMEOUT, TimeUnit.SECONDS);
        root = YPath.simple(auditTablePrefix).child(UUID.randomUUID().toString());
        JdbcTemplate yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        yt.cypress().create(
            root, CypressNodeType.MAP, true, false,
            Map.of("expiration_time", YTree.stringNode(HttpUtils.YT_INSTANT_FORMATTER
                .format(Instant.now().plus(Duration.ofDays(1)))))
        );
        auditTable = root.child("mdm_audit");
        auditYtWriter = new AuditYtWriter(yt, yqlJdbcTemplate);
    }

    @After
    public void tearDown() {
        yt.cypress().remove(root);
    }

    private List<PgAuditRecord> generateAndStoreItems() {
        List<FromIrisItemWrapper> itemsBatch = random.objects(MdmIrisPayload.Item.class, DATASET_SIZE)
            .map(item ->
                item.toBuilder().setItemId(item.getItemId().toBuilder().setSupplierId(VALID_SUPPLIER_ID)).build())
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());
        fromIrisItemRepository.insertBatch(itemsBatch);
        return pgAuditRepository.findAll();
    }

    @Test
    public void createAuditTable() {
        Assertions.assertThat(yt.cypress().exists(auditTable)).isFalse();
        auditYtWriter.createAuditTable(auditTable);
        Assertions.assertThat(yt.cypress().exists(auditTable)).isTrue();
    }

    @Test
    public void writeToYt() {
        auditYtWriter.createAuditTable(auditTable);
        List<PgAuditRecord> records = generateAndStoreItems();
        auditYtWriter.writeToYt(auditTable, records);

        long rowCount = getLongTableAttr("row_count");
        Assertions.assertThat(rowCount).isEqualTo(records.size());

        List<PgAuditRecord> ytRecords = new ArrayList<>();
        yt.tables().read(auditTable, YTableEntryTypes.JACKSON_UTF8, node -> {
            ytRecords.add(AuditYtWriter.fromJsonNode(node));
        });

        Assertions.assertThat(ytRecords)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(records);
    }

    @Test
    public void mergeTablesIntoEmptyTable() {
        generateAuditTables();

        List<String> tables = list(root);
        Assertions.assertThat(tables.size()).isEqualTo(3);

        int beforeMerge = tables.stream()
            .mapToInt(table -> countRows(root.child(table)))
            .sum();

        auditYtWriter.mergeTablesInDirectoryWithRemoving(root, "mdm_audit_archive", List.of());

        tables = list(root);
        Assertions.assertThat(tables.size()).isEqualTo(1);

        Assertions.assertThat(countRows(root.child("mdm_audit_archive"))).isEqualTo(beforeMerge);
    }

    @Test
    public void mergeTablesIntoNotEmptyTable() {
        generateAuditTables();

        YPath archiveTable = root.child("mdm_audit_archive");
        auditYtWriter.createAuditTable(archiveTable);
        auditYtWriter.writeToYt(archiveTable, generateAndStoreItems());

        List<String> tables = list(root);
        Assertions.assertThat(tables.size()).isEqualTo(4);

        Integer rowsCount = tables.stream()
            .mapToInt(table -> countRows(root.child(table)))
            .sum();

        auditYtWriter.mergeTablesInDirectoryWithRemoving(root, "mdm_audit_archive", List.of());

        tables = list(root);
        Assertions.assertThat(tables.size()).isEqualTo(1);

        Assertions.assertThat(countRows(root.child("mdm_audit_archive"))).isEqualTo(rowsCount);
    }

    private void generateAuditTables() {
        YPath table1 = root.child(TEST_TABLE_1);
        YPath table2 = root.child(TEST_TABLE_2);
        YPath table3 = root.child(TEST_TABLE_3);

        auditYtWriter.createAuditTable(table1);
        auditYtWriter.writeToYt(table1, generateAndStoreItems());
        auditYtWriter.createAuditTable(table2);
        auditYtWriter.writeToYt(table2, generateAndStoreItems());
        auditYtWriter.createAuditTable(table3);
        auditYtWriter.writeToYt(table3, generateAndStoreItems());
    }

    private Integer countRows(YPath table) {
        return yt.cypress().get(table.attribute("row_count")).intValue();
    }

    private long getLongTableAttr(String name) {
        return yt.cypress()
            .get(auditTable, List.of(name))
            .getAttributeOrThrow(name)
            .longValue();
    }

    private List<String> list(YPath path) {
        return yt.cypress()
            .list(path)
            .stream()
            .map(node -> node.getValue())
            .collect(Collectors.toList());
    }

}

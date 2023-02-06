package ru.yandex.market.mbo.mdm.common.audit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class MdmAuditPartitionCleanerServiceImplTest  extends MdmBaseDbTestClass {

    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private JdbcTemplate serviceJdbcTemplate;
    private MdmPgAuditPartitionCleanerServiceImpl auditPartitionCleanerService;

    private static final String TABLE_NAME = "mdm_audit.audit_777";
    private static final String IDS_TABLE_NAME = "mdm_audit.audit_archived_ids_777";

    @Before
    public void setUp() {
        auditPartitionCleanerService = new MdmPgAuditPartitionCleanerServiceImpl(
            serviceJdbcTemplate, skv, "audit_777", "audit_archived_ids_777", "mdm_audit");
    }

    @Test
    public void whenClearRecordsInPartitionShouldCorrectlyClean() {
        createPartitionedAuditTable(1, 1);
        createPartitionedAuditIdsTable(1, 1);
        fillTable(TABLE_NAME, 2, 1);
        fillTable(IDS_TABLE_NAME, 1, 1);

        auditPartitionCleanerService.clearAuditRecordsInPartition("1");

        Assertions.assertThat(countTableRows(TABLE_NAME)).isEqualTo(1);
        Assertions.assertThat(countTableRows(IDS_TABLE_NAME)).isEqualTo(0);

        fillTable(IDS_TABLE_NAME, 1, 2);

        auditPartitionCleanerService.clearAuditRecordsInPartition("2");

        Assertions.assertThat(countTableRows(TABLE_NAME)).isEqualTo(0);
        Assertions.assertThat(countTableRows(IDS_TABLE_NAME)).isEqualTo(0);
    }

    @Test
    public void whenClearAllShouldClearAllPartitions() {
        createPartitionedAuditTable(3, 1);
        createPartitionedAuditIdsTable(3, 1);

        fillTable(TABLE_NAME, 2, 3);
        fillTable(IDS_TABLE_NAME, 2, 3);

        Assertions.assertThat(countTableRows(TABLE_NAME)).isEqualTo(2);
        Assertions.assertThat(countTableRows(IDS_TABLE_NAME)).isEqualTo(2);

        auditPartitionCleanerService.clearAuditRecords();

        Assertions.assertThat(countTableRows(TABLE_NAME)).isEqualTo(0);
        Assertions.assertThat(countTableRows(IDS_TABLE_NAME)).isEqualTo(0);
    }

    @Test
    public void whenPartitionsHaveDifferentBoundsShouldClear() {
        createPartitionedAuditTable(5, 2);
        createPartitionedAuditIdsTable(5, 3);

        fillTable(TABLE_NAME, 3, 5);
        fillTable(IDS_TABLE_NAME, 3, 5);

        Assertions.assertThat(countTableRows(TABLE_NAME)).isEqualTo(3);
        Assertions.assertThat(countTableRows(IDS_TABLE_NAME)).isEqualTo(3);

        auditPartitionCleanerService.clearAuditRecords();

        Assertions.assertThat(countTableRows(TABLE_NAME)).isEqualTo(0);
        Assertions.assertThat(countTableRows(IDS_TABLE_NAME)).isEqualTo(0);
    }

    private void fillTable(String tableName, int rowsToInsert, int startingFrom) {
        for (int i = 0; i < rowsToInsert; ++i) {
            var insertingValue = startingFrom + i;
            serviceJdbcTemplate.execute("insert into " + tableName + " values (" + insertingValue + ")");
        }
    }

    private Long countTableRows(String tableName) {
        Long count = serviceJdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count;
    }

    private void createPartitionedAuditTable(int partitionsStartingFrom, int step) {
        serviceJdbcTemplate.execute("create table " + TABLE_NAME + " (id bigint not null) partition by range (id);");
        serviceJdbcTemplate.execute(
            "create table " + TABLE_NAME + "_partition_" + partitionsStartingFrom + " partition of " + TABLE_NAME +
                " for values from (" + partitionsStartingFrom + ") to (" + (partitionsStartingFrom + step) + ");");
        serviceJdbcTemplate.execute(
            "create table " + TABLE_NAME + "_partition_" + (partitionsStartingFrom + 1) +
                " partition of " + TABLE_NAME +
                " for values from (" + (partitionsStartingFrom + step) + ") to (" + (partitionsStartingFrom + 2 * step) + ");");
    }

    private void createPartitionedAuditIdsTable(int partitionsStartingFrom, int step) {
        serviceJdbcTemplate.execute("create table " + IDS_TABLE_NAME + " (audit_record_id bigint) " +
            "partition by range (audit_record_id);");
        serviceJdbcTemplate.execute(
            "create table " + IDS_TABLE_NAME + "_partition_" + partitionsStartingFrom + " partition of " + IDS_TABLE_NAME +
                " for values from (" + partitionsStartingFrom + ") to (" + (partitionsStartingFrom + step) + ");");
        serviceJdbcTemplate.execute(
            "create table " + IDS_TABLE_NAME + "_partition_" + (partitionsStartingFrom + 1) +
                " partition of " + IDS_TABLE_NAME +
                " for values from (" + (partitionsStartingFrom + step) + ") to (" + (partitionsStartingFrom + 2 * step) + ");");
    }
}

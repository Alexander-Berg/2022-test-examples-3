package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.pgaudit.PgAuditPartition;
import ru.yandex.market.mbo.pgaudit.PgAuditPartitionManager;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class ManageAuditPartitionsExecutorTest extends MdmBaseDbTestClass {
    private static final int PARTITION_SIZE = 100;
    @Autowired
    private NamedParameterJdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private StorageKeyValueService skv;

    private ManageAuditPartitionsExecutor executor;
    private PgAuditPartitionManager pgAuditPartitionManager;

    @Before
    public void setup() {
        pgAuditPartitionManager = new PgAuditPartitionManager("mdm_audit", jdbc, jdbc);
        executor = new ManageAuditPartitionsExecutor(
            skv,
            pgAuditPartitionManager,
            jdbc,
            transactionTemplate
        );
        skv.putValue(MdmProperties.AUDIT_PARTITION_SIZE, PARTITION_SIZE);
        skv.invalidateCache();
    }

    @Test
    public void whenManyFreePartitionsShouldRemoveEmptyAnyway() {
        prepare(100, 5, 10, 961);
        executor.execute();

        List<PgAuditPartition> auditPartitions = pgAuditPartitionManager.getAuditPartitions();
        List<PgAuditPartition> idsPartitions = pgAuditPartitionManager.getIdsPartitions();

        Assertions.assertThat(auditPartitions.size()).isEqualTo(95);
        assertPartitionRange(auditPartitions, 0, 95, (partition, index) -> {
            int partitionNumber = partition.getPartitionNumber();
            Assertions.assertThat(partitionNumber).isGreaterThanOrEqualTo(5);
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * partitionNumber);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (partitionNumber + 1));
        });

        Assertions.assertThat(idsPartitions.size()).isEqualTo(95);
        assertPartitionRange(idsPartitions, 0, 95, (partition, index) -> {
            int partitionNumber = partition.getPartitionNumber();
            Assertions.assertThat(partitionNumber).isGreaterThanOrEqualTo(5);
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * partitionNumber);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (partitionNumber + 1));
        });
    }

    @Test
    public void whenFewFreePartitionsShouldAllocateMore() {
        prepare(100, 0, 80, 7933);
        executor.execute();

        List<PgAuditPartition> auditPartitions = pgAuditPartitionManager.getAuditPartitions();
        List<PgAuditPartition> idsPartitions = pgAuditPartitionManager.getIdsPartitions();

        assertPartitionRange(auditPartitions, 0, 180, (partition, index) -> {
            Assertions.assertThat(partition.getPartitionNumber()).isEqualTo(index);
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * index);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (index + 1));
        });
        Assertions.assertThat(auditPartitions.size()).isEqualTo(180);

        assertPartitionRange(idsPartitions, 0, 180, (partition, index) -> {
            Assertions.assertThat(partition.getPartitionNumber()).isEqualTo(index);
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * index);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (index + 1));
        });
        Assertions.assertThat(idsPartitions.size()).isEqualTo(180);
    }

    @Test
    public void shouldRemoveUnusedPartitions() {
        prepare(100, 50, 80, 7933);
        executor.execute();

        List<PgAuditPartition> auditPartitions = pgAuditPartitionManager.getAuditPartitions();
        List<PgAuditPartition> idsPartitions = pgAuditPartitionManager.getIdsPartitions();

        assertPartitionRange(auditPartitions, 0, 130, (partition, index) -> {
            index += 50;
            Assertions.assertThat(partition.getPartitionNumber()).isEqualTo(index);
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * index);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (index + 1));
        });
        Assertions.assertThat(auditPartitions.size()).isEqualTo(130);

        assertPartitionRange(idsPartitions, 0, 130, (partition, index) -> {
            index += 50;
            Assertions.assertThat(partition.getPartitionNumber()).isEqualTo(index);
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * index);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (index + 1));
        });
        Assertions.assertThat(idsPartitions.size()).isEqualTo(130);
    }

    @Test
    public void whenCurrentIdExceedsOldPartitionsShouldStartFromLargerRange() {
        prepare(100, 0, 100, 10001);
        executor.execute();

        List<PgAuditPartition> auditPartitions = pgAuditPartitionManager.getAuditPartitions();
        List<PgAuditPartition> idsPartitions = pgAuditPartitionManager.getIdsPartitions();

        assertPartitionRange(auditPartitions, 0, 200, (partition, index) -> {
            Assertions.assertThat(partition.getPartitionNumber()).isEqualTo(index);
            if (index >= 100) {
                ++index;
            }
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * index);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (index + 1));
        });
        Assertions.assertThat(auditPartitions.size()).isEqualTo(200);

        assertPartitionRange(idsPartitions, 0, 200, (partition, index) -> {
            Assertions.assertThat(partition.getPartitionNumber()).isEqualTo(index);
            if (index >= 100) {
                ++index;
            }
            Assertions.assertThat(partition.getRangeFromId()).isEqualTo((long) PARTITION_SIZE * index);
            Assertions.assertThat(partition.getRangeToId()).isEqualTo((long) PARTITION_SIZE * (index + 1));
        });
        Assertions.assertThat(idsPartitions.size()).isEqualTo(200);
    }

    private void prepare(int total, int unusedUpTo, int usedUpTo, long maxId) {
        jdbc.getJdbcOperations().execute(
            "create table mdm_audit.audit_tmp (like mdm_audit.audit including all) partition by range(id)");
        jdbc.getJdbcOperations().execute("alter table mdm_audit.audit rename to audit_old");
        jdbc.getJdbcOperations().execute("alter table mdm_audit.audit_tmp rename to audit");

        jdbc.getJdbcOperations().execute("create table mdm_audit.audit_archived_ids_tmp " +
            "(like mdm_audit.audit_archived_ids including all) partition by range(audit_record_id)");
        jdbc.getJdbcOperations().execute("alter table mdm_audit.audit_archived_ids rename to archived_ids_old");
        jdbc.getJdbcOperations().execute(
            "alter table mdm_audit.audit_archived_ids_tmp rename to audit_archived_ids");

        for (int i = 0; i < total; i++) {
            jdbc.getJdbcOperations().execute(
                "create table mdm_audit.audit_partition_" + i +
                    " partition of mdm_audit.audit for values from (" +
                    (i * PARTITION_SIZE) + ") to (" + ((i + 1) * PARTITION_SIZE) + ")");
            jdbc.getJdbcOperations().execute(
                "create table mdm_audit.audit_archived_ids_partition_" + i +
                    " partition of mdm_audit.audit_archived_ids for values from (" +
                    (i * PARTITION_SIZE) + ") to (" + ((i + 1) * PARTITION_SIZE) + ")");
        }
        jdbc.getJdbcOperations().execute(
            "create table mdm_audit.audit_partition_default partition of mdm_audit.audit default");
        jdbc.getJdbcOperations().execute("create table mdm_audit.audit_archived_ids_partition_default " +
            "partition of mdm_audit.audit_archived_ids default");

        long minRecordId = unusedUpTo * PARTITION_SIZE;
        long maxRecordId = Math.max(usedUpTo * PARTITION_SIZE, maxId);
        LongStream.range(minRecordId, maxRecordId).boxed().forEach(id -> {
            jdbc.update("insert into mdm_audit.audit " +
                "(id, entity_type, entity_key, changes, event_ts, change_type, event_id) values " +
                "(:id, 'master_data', '-', '{}', now(), 'UPDATE', :id)", Map.of("id", id));
        });
    }

    private static void assertPartitionRange(List<PgAuditPartition> partitions,
                                             int from,
                                             int to,
                                             BiConsumer<PgAuditPartition, Integer> assertions) {
        for (int i = from; i < to; ++i) {
            assertions.accept(partitions.get(i), i);
        }
    }
}

package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.pgaudit.PgAuditChangeType;
import ru.yandex.market.mbo.pgaudit.PgAuditRecord;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;

/**
 * Базовый тест на то, что PG-аудит настроен корректно.
 * Должно работать из коробки для любого репозитория, если переопределить абстрактные методы в данном классе.
 *
 * @author dmserebr
 * @date 18/12/2020
 */
public abstract class MdmServicesAuditTestBase extends MdmBaseIntegrationTestClass {
    @Autowired
    protected PgAuditRepository pgAuditRepository;

    protected abstract String getEntityType();
    protected abstract AuditRecordTestInfo insertTestValue();
    protected abstract AuditRecordTestInfo updateTestValue();
    protected Set<String> getFieldsIgnoredInComparator() {
        return Set.of();
    }

    @Test
    public void testInsert() {
        List<PgAuditRecord> initialRecords = pgAuditRepository.findAll(
            new PgAuditRepository.Filter().setEntityType(getEntityType()));
        Set<Long> initialRecordIds = initialRecords.stream().map(PgAuditRecord::getId).collect(Collectors.toSet());

        AuditRecordTestInfo insertedInfo = insertTestValue();

        List<PgAuditRecord> finalRecords = pgAuditRepository.findAll(
            new PgAuditRepository.Filter().setEntityType(getEntityType()));
        List<PgAuditRecord> addedRecords = finalRecords.stream()
            .filter(finalRecord -> !initialRecordIds.contains(finalRecord.getId()))
            .collect(Collectors.toList());

        Assertions.assertThat(addedRecords).hasSize(1);
        assertAuditRecord(addedRecords.get(0),
            PgAuditChangeType.INSERT, getEntityType(),
            insertedInfo.getEntityKey(), insertedInfo.getChanges(),
            getFieldsIgnoredInComparator());
    }

    @Test
    public void testUpdate() {
        insertTestValue();

        List<PgAuditRecord> initialRecords = pgAuditRepository.findAll(
            new PgAuditRepository.Filter().setEntityType(getEntityType()));
        Set<Long> initialRecordIds = initialRecords.stream().map(PgAuditRecord::getId).collect(Collectors.toSet());

        AuditRecordTestInfo updatedInfo = updateTestValue();

        List<PgAuditRecord> finalRecords = pgAuditRepository.findAll(
            new PgAuditRepository.Filter().setEntityType(getEntityType()));
        List<PgAuditRecord> updatedRecords = finalRecords.stream()
            .filter(finalRecord -> !initialRecordIds.contains(finalRecord.getId()))
            .collect(Collectors.toList());

        Assertions.assertThat(updatedRecords).hasSize(1);
        assertAuditRecord(updatedRecords.get(0),
            PgAuditChangeType.UPDATE, getEntityType(),
            updatedInfo.getEntityKey(), updatedInfo.getChanges(),
            getFieldsIgnoredInComparator());
    }

    protected static void assertAuditRecord(PgAuditRecord record,
                                            PgAuditChangeType changeType,
                                            String entityType,
                                            String entityKey,
                                            List<Pair<String, String>> changes,
                                            Set<String> ignoredFields) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(record.getChangeType()).isEqualTo(changeType);
            softly.assertThat(record.getEntityType()).isEqualTo(entityType);
            softly.assertThat(record.getEntityKey()).isEqualTo(entityKey);

            List<Pair<String, String>> actualChanges = record.getChanges().entrySet().stream()
                .filter(entry -> !ignoredFields.contains(entry.getKey()))
                .map(entry -> new ImmutablePair<>(entry.getKey(), String.valueOf(entry.getValue())))
                .collect(Collectors.toList());
            softly.assertThat(actualChanges).containsExactlyInAnyOrderElementsOf(changes);
        });
    }

    static class AuditRecordTestInfo {
        String entityKey;
        List<Pair<String, String>> changes;

        public AuditRecordTestInfo(String entityKey, List<Pair<String, String>> changes) {
            this.entityKey = entityKey;
            this.changes = changes;
        }

        public String getEntityKey() {
            return entityKey;
        }

        public List<Pair<String, String>> getChanges() {
            return changes;
        }
    }
}

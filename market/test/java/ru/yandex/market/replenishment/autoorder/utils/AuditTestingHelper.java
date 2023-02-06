package ru.yandex.market.replenishment.autoorder.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.function.Executable;
import org.springframework.stereotype.Service;

import ru.yandex.market.mbo.pgaudit.PgAuditChangeType;
import ru.yandex.market.mbo.pgaudit.PgAuditRecord;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;

@Service
public class AuditTestingHelper {

    public static class ExpectedAuditRecord {
        private final String name;
        private final PgAuditChangeType type;
        private final String entityKey;
        private final Object[] keyValues;

        public ExpectedAuditRecord(@Nonnull String name,
                                   @Nonnull PgAuditChangeType type,
                                   @Nullable String entityKey,
                                   Object[] keyValues) {
            if (keyValues != null && keyValues.length % 2 != 0) {
                throw new IllegalArgumentException("KeyValues should have even length for key and value pairs");
            }
            this.name = name;
            this.type = type;
            this.entityKey = entityKey;
            this.keyValues = keyValues;
        }

        public boolean equals(PgAuditRecord record) {
            return record != null
                    && name.equals(record.getEntityType())
                    && type == record.getChangeType()
                    && Objects.equals(entityKey, record.getEntityKey());
        }

        @Override
        public String toString() {
            return "ExpectedAuditRecord{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", entityKey=" + entityKey +
                    '}';
        }
    }

    public static ExpectedAuditRecord expected(String name, PgAuditChangeType type, String entityKey, Object... keyValues) {
        return new ExpectedAuditRecord(name, type, entityKey, keyValues);
    }

    PgAuditRepository auditRepository;

    public AuditTestingHelper(PgAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void assertAuditRecordAdded(CheckedRunnable r, Consumer<PgAuditRecord> asserter) {
        final int before = findAuditCount();
        r.run();
        assertEquals(1, findAuditCount() - before);
        asserter.accept(findLastAuditRecord());
    }

    public void assertAuditRecordAdded(CheckedRunnable r, int len, Consumer<List<PgAuditRecord>> asserter) {
        final int before = findAuditCount();
        r.run();
        assertEquals(len, findAuditCount() - before);
        asserter.accept(findLastAuditRecords(len));
    }

    public void assertAuditRecords(CheckedRunnable r, ExpectedAuditRecord... expectedRecords) {
        final int before = findAuditCount();
        r.run();
        assertEquals(expectedRecords.length, findAuditCount() - before);
        final List<PgAuditRecord> actualRecords = findLastAuditRecords(expectedRecords.length);
        String actualRecordsString = actualRecords.stream()
                .map(PgAuditRecord::toString)
                .collect(Collectors.joining("\n", "Actual records:\n", "\n"));
        assertAll(actualRecordsString,
                Arrays.stream(expectedRecords)
                        .map(expectedRecord -> getAsserter(actualRecords, expectedRecord)));
    }

    public static void assertAuditRecord(PgAuditRecord record, String name, PgAuditChangeType type, Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalStateException("keyValues shoould have even length");
        }
        assertEquals(type, record.getChangeType());
        assertEquals(name, record.getEntityType());
        assertKeyValues(record, keyValues);
    }

    private static void assertKeyValues(PgAuditRecord actualRecord, Object... expectedKeyValues) {
        for (int i = 0; i < expectedKeyValues.length; i += 2) {
            Object value = actualRecord.getChanges().get(expectedKeyValues[i]);
            if (value instanceof List) {
                value = ((List) value).get(1);
            }
            assertEquals("Wrong value for " + expectedKeyValues[i], expectedKeyValues[i + 1], value);
        }
    }

    private static Executable getAsserter(List<PgAuditRecord> actualRecords, ExpectedAuditRecord expectedRecord) {
        return () -> {
            var actualRecord = findPgAuditRecord(actualRecords, expectedRecord);
            assertNotNull("Not found pg_audit_record " + expectedRecord.toString(), actualRecord);
            assertKeyValues(actualRecord, expectedRecord.keyValues);
        };
    }

    private static PgAuditRecord findPgAuditRecord(List<PgAuditRecord> records, ExpectedAuditRecord expectedRecord) {
        for (PgAuditRecord record : records) {
            if (expectedRecord.equals(record)) {
                return record;
            }
        }
        return null;
    }

    private int findAuditCount() {
        return auditRepository.findAll().size();
    }

    @NotNull
    private PgAuditRecord findLastAuditRecord() {
        return findLastAuditRecords(1).get(0);
    }

    private List<PgAuditRecord> findLastAuditRecords(int limit) {
        return auditRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(PgAuditRecord::getId).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}

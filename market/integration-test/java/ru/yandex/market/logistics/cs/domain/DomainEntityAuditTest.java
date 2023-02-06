package ru.yandex.market.logistics.cs.domain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.entity.Capacity;
import ru.yandex.market.logistics.cs.domain.entity.DaysOffForExport;
import ru.yandex.market.logistics.cs.domain.entity.LmsCapacity;
import ru.yandex.market.logistics.cs.domain.entity.LmsCapacityValue;
import ru.yandex.market.logistics.cs.domain.entity.LmsServiceCapacityMapping;
import ru.yandex.market.logistics.cs.domain.entity.QueueTask;
import ru.yandex.market.logistics.cs.repository.CapacityTestRepository;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;

class DomainEntityAuditTest extends AbstractIntegrationTest {

    private static final Long CAPACITY_ID = 1L;

    private static final String TRIGGER_FUNCTION_NAME = "cs_audit";

    private static final String TRIGGER_SUFFIX = "_audit_trigger";

    private static final Set<Class<?>> ENTITY_BLACK_LIST = Set.of(
        LmsCapacity.class,
        LmsCapacityValue.class,
        QueueTask.class,
        DaysOffForExport.class,
        LmsServiceCapacityMapping.class
    );

    private static final String SUITABLE_TRIGGER_EXISTS =
        "       SELECT count(*) = 1"
            + " FROM pg_trigger AS t"
            + " INNER JOIN pg_class AS c ON c.oid = t.tgrelid"
            + " INNER JOIN pg_proc AS f ON f.oid = t.tgfoid"
            + " WHERE c.relname = ?"
            + " AND f.proname = '" + TRIGGER_FUNCTION_NAME + "'"
            + " AND t.tgname = ?"
            + " AND t.tgtype = 23"; // before insert or update bitmask

    private static final String AUDIT_COLUMNS_EXIST =
        "       SELECT count(DISTINCT c.column_name) = 2"
            + " FROM information_schema.columns AS c"
            + " WHERE c.table_name = ?"
            + " AND c.column_name IN ('created', 'updated')"
            + " AND c.data_type = 'timestamp without time zone'"
            + " AND c.column_default = 'timezone(''UTC''::text, now())'";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CapacityTestRepository capacityTestRepository;

    // If you don't want your new entity to be audited, add one to the blacklist above
    @DisplayName("Для каждой таблицы существует триггер аудита")
    @Test
    void testAuditTriggersExist() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            for (Class<?> entity: getEntities()) {
                String tableName = getTableName(entity);
                String triggerName = tableName + TRIGGER_SUFFIX;

                boolean exists = isTriggerExists(connection, tableName, triggerName);

                softly.assertThat(exists)
                    .as("There is no %s trigger for %s table", triggerName, tableName)
                    .isTrue();
            }
        }
    }

    // If you don't want your new entity to be audited, add one to the blacklist above
    @DisplayName("Для каждой таблицы существуют столбцы аудита created и updated")
    @Test
    void testAuditColumnsExist() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            for (Class<?> entity: getEntities()) {
                String tableName = getTableName(entity);

                boolean exist = isAuditColumnsExist(connection, tableName);

                softly.assertThat(exist)
                    .as("There are no suitable audit columns in %s table", tableName)
                    .isTrue();
            }
        }
    }

    @DisplayName("Проверяем работоспособность триггера аудита")
    @Test
    void testAuditTrigger() throws InterruptedException {
        // Setup
        LocalDateTime beforeCreate = DateTimeUtils.nowUtc();
        Thread.sleep(0, 100);

        // Action
        runInTransaction(() -> capacityTestRepository.save(buildCapacity()));

        // Assertion
        LocalDateTime created = capacityTestRepository.findCreated(CAPACITY_ID);
        LocalDateTime updated = capacityTestRepository.findUpdated(CAPACITY_ID);
        softly.assertThat(created.isAfter(beforeCreate)).isTrue();
        softly.assertThat(updated.isAfter(beforeCreate)).isTrue();

        // Setup
        LocalDateTime beforeUpdate = DateTimeUtils.nowUtc();
        Thread.sleep(0, 100);
        Capacity capacity = capacityTestRepository.findById(CAPACITY_ID).orElseThrow();

        // Action
        runInTransaction(() -> capacityTestRepository.save(
            capacity.toBuilder().name("Oops, name's changed").build()
        ));

        // Assertion
        created = capacityTestRepository.findCreated(CAPACITY_ID);
        updated = capacityTestRepository.findUpdated(CAPACITY_ID);
        softly.assertThat(created.isAfter(beforeCreate)).isTrue();
        softly.assertThat(created.isBefore(beforeUpdate)).isTrue();
        softly.assertThat(updated.isAfter(beforeUpdate)).isTrue();
    }

    private Set<Class<?>> getEntities() {
        return new Reflections("ru.yandex.market.logistics.cs.domain.entity")
            .getTypesAnnotatedWith(Entity.class)
            .stream()
            .filter(Predicate.not(ENTITY_BLACK_LIST::contains))
            .collect(Collectors.toSet());
    }

    private String getTableName(Class<?> clazz) {
        return clazz.getAnnotation(Table.class).name();
    }

    private boolean isTriggerExists(
        Connection connection,
        String tableName,
        String triggerName
    ) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(SUITABLE_TRIGGER_EXISTS);
        statement.setString(1, tableName);
        statement.setString(2, triggerName);
        try (ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getBoolean(1);
        }
    }

    private boolean isAuditColumnsExist(
        Connection connection,
        String tableName
    ) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(AUDIT_COLUMNS_EXIST);
        statement.setString(1, tableName);
        try (ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getBoolean(1);
        }
    }

    private Capacity buildCapacity() {
        return Capacity.builder()
            .id(CAPACITY_ID)
            .path("1")
            .name("Some dumb name")
            .build();
    }

    private void runInTransaction(Runnable runnable) {
        new TransactionTemplate(transactionManager).execute(status -> {
            runnable.run();
            return null;
        });
    }

}

package ru.yandex.market.mbo.integration.test.orchestrator;

import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Класс для оркестрации выдаваемых тестовых схем.
 *
 * @author s-ermakov
 */
public class DatabaseGroupOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseGroupOrchestrator.class);

    private static final String MASTER_DB_LIQUIBASE = "mbo-functional-tests-oaas/master-db-liquibase.sql";
    private static final Duration PING_DURATION = Duration.ofMinutes(10);

    private static final boolean REUSE_TEST_SCHEMAS = Boolean.getBoolean("MBO_REUSE_TEST_SCHEMAS");
    private static final File REUSE_CACHE_FILE = new File(SystemUtils.getUserHome(), ".mbo_reuse_test_schemas_uid");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<DatabaseGroup, ScheduledFuture> pings = new HashMap<>();

    private final JdbcTemplate masterJDBCTemplate;
    private final DatabaseCreator databaseCreator;
    private final String masterSchema;

    public DatabaseGroupOrchestrator(JdbcTemplate masterJDBCTemplate, String masterSchema) {
        this.masterJDBCTemplate = masterJDBCTemplate;
        this.databaseCreator = new DatabaseCreator(masterJDBCTemplate);
        this.masterSchema = masterSchema;
        init();
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    private void init() {
        Connection connection = null;
        Database database = null;
        Liquibase liquibase = null;
        try {
            ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
            connection = masterJDBCTemplate.getDataSource().getConnection();
            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            liquibase = new Liquibase(MASTER_DB_LIQUIBASE, resourceAccessor, database);
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (liquibase.exception.LockException e) {
            throw new IllegalStateException(
                "Failed to acquire liquibase lock in oracle " + masterSchema + ".DATABASECHANGELOGLOCK table. " +
                "Probably there was a failure before. Please, release lock manually.", e);
        } catch (SQLException | LiquibaseException e) {
            throw new RuntimeException("There was some error during connection to oracle " + masterSchema, e);
        } finally {
            if (database != null) {
                try {
                    database.close();
                } catch (DatabaseException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public DatabaseGroup createGroup(Collection<String> schemas) {
        if (REUSE_TEST_SCHEMAS) {
            return doReuseGroup(schemas);
        } else {
            return doCreateGroup(schemas);
        }
    }

    private DatabaseGroup doCreateGroup(Collection<String> schemas) {
        DatabaseGroup group = lockGroup();
        startPing(group);

        for (String schema : schemas) {
            String userName = getUserName(schema, group.getGroupId());
            group.addSchema(schema, userName, userName, userName);
        }

        try {
            databaseCreator.createSchemas(group.getSchemaNames());
        } catch (Exception e) {
            log.warn("Failed to create schemas by error: " + e.getMessage() + ". Group: " + group, e);
            stopPing(group);
            releaseGroup(group);
            throw e;
        }
        return group;
    }

    private DatabaseGroup doReuseGroup(Collection<String> schemas) {
        DatabaseGroup group = getCachedGroup()
            .orElseGet(this::lockGroup);
        startPing(group);

        for (String schema : schemas) {
            String userName = getUserName(schema, group.getGroupId());
            group.addSchema(schema, userName, userName, userName);
        }
        try {
            if (group.reuseTestSchemas()) {
                databaseCreator.reuseSchemas(group.getSchemaNames());
            } else {
                databaseCreator.createSchemas(group.getSchemaNames());
            }
            cacheGroup(group);
        } catch (Exception e) {
            log.warn("Failed to reuse created schemas by error: " + e.getMessage() + ". Group: " + group, e);
            stopPing(group);
            throw e;
        }
        return group;
    }

    public void dropGroup(DatabaseGroup databaseGroup) {
        if (REUSE_TEST_SCHEMAS) {
            // do not drop schemas, if reuse set to true
            stopPing(databaseGroup);
            return;
        }

        try {
            databaseCreator.dropSchemas(databaseGroup.getSchemaNames());
        } finally {
            stopPing(databaseGroup);
            releaseGroup(databaseGroup);
        }
    }

    private Optional<DatabaseGroup> getCachedGroup() {
        if (!REUSE_CACHE_FILE.exists()) {
            return Optional.empty();
        }

        UUID clientGuid;
        try (FileInputStream inputStream = new FileInputStream(REUSE_CACHE_FILE)) {
            String fileContent = IOUtils.toString(inputStream);
            clientGuid = UUID.fromString(fileContent);
        } catch (Exception e) {
            log.warn("Failed to read REUSE_TEST_SCHEMAS cache file", e);
            return Optional.empty();
        }

        int updatedRows = masterJDBCTemplate.update(
            "update db_group_orchestrator " +
                "set ping_duration_sec = ?, " +
                "    client_name = ?, " +
                "    last_ping_time = current_timestamp " +
                "where group_id = (" +
                "   select group_id from db_group_orchestrator " +
                "   where client_id = ? " +
                "     and current_timestamp <= last_ping_time + numToDSInterval(ping_duration_sec, 'second') " +
                "   fetch first 1 row only" +
                ")",
            PING_DURATION.getSeconds(), System.getProperty("user.name"), clientGuid.toString());

        if (updatedRows == 0) {
            log.debug("Failed to find alive database group by client_id " + clientGuid);
            return Optional.empty();
        }
        if (updatedRows > 1) {
            throw new IllegalStateException("Expected to lock only one group by client_id " + clientGuid + ".");
        }

        int groupId;
        try {
            groupId = masterJDBCTemplate.queryForObject("select group_id from db_group_orchestrator " +
                "where client_id = ?", Integer.class, clientGuid.toString());
        } catch (IncorrectResultSizeDataAccessException e) {
            log.debug("Failed to find group id by client_id " + clientGuid);
            return Optional.empty();
        }

        log.debug("Successfully reuse cache group with id: " + groupId + " and client_id " + clientGuid);

        DatabaseGroup databaseGroup = new DatabaseGroup(groupId, clientGuid);
        databaseGroup.setReuseTestSchemas(true);
        return Optional.of(databaseGroup);
    }

    private void cacheGroup(DatabaseGroup databaseGroup) {
        try (FileOutputStream outputStream = new FileOutputStream(REUSE_CACHE_FILE)) {
            UUID clientId = databaseGroup.getClientGuid();
            byte[] bytes = clientId.toString().getBytes(StringUtils.UTF8);
            outputStream.write(bytes);
        } catch (Exception e) {
            log.warn("Failed to write database group to cache file. " +
                "Group: " + databaseGroup + ", file " + REUSE_CACHE_FILE, e);
        }

        log.debug("Successfully cache group : " + databaseGroup);
    }

    private DatabaseGroup lockGroup() {
        UUID clientGuid = UUID.randomUUID();

        int updatedRows = masterJDBCTemplate.update(
            "update db_group_orchestrator " +
                "set client_id = ?, " +
                "    client_name = ?, " +
                "    start_time = current_timestamp, " +
                "    ping_duration_sec = ?, " +
                "    last_ping_time = current_timestamp " +
                "where group_id = (" +
                "   select group_id from db_group_orchestrator " +
                "   where client_id is null " +
                "     or current_timestamp > last_ping_time + numToDSInterval(ping_duration_sec, 'second') " +
                "   fetch first 1 row only" +
                ")",
            clientGuid.toString(), System.getProperty("user.name"), PING_DURATION.getSeconds());

        if (updatedRows == 0) {
            throw new IllegalStateException("Failed to lock database group. No free group.");
        }
        if (updatedRows > 1) {
            throw new IllegalStateException("Expected to lock only one group. Smth went wrong.");
        }

        int groupId = masterJDBCTemplate.queryForObject("select group_id from db_group_orchestrator " +
            "where client_id = ?", Integer.class, clientGuid.toString());

        log.debug("Successfully locked group with id: " + groupId + " and client_id " + clientGuid);

        return new DatabaseGroup(groupId, clientGuid);
    }

    private void releaseGroup(DatabaseGroup databaseGroup) {
        int updatedRows = masterJDBCTemplate.update(
            "update db_group_orchestrator " +
                "set client_id = null, " +
                "    client_name = null, " +
                "    start_time = null, " +
                "    ping_duration_sec = null, " +
                "    last_ping_time = null " +
                "where group_id = ? and client_id = ?",
            databaseGroup.getGroupId(),
            databaseGroup.getClientGuid().toString()
        );

        if (updatedRows == 0) {
            String clientName = masterJDBCTemplate.queryForObject(
                "select client_name from db_group_orchestrator where group_id = ?",
                String.class, databaseGroup.getGroupId());
            log.warn("No rows cleared of group: {}. '{}' overwrite this group.", databaseGroup, clientName);
        }
        if (updatedRows > 1) {
            throw new IllegalStateException("Expected to release only one group: " + databaseGroup);
        }
    }

    private void startPing(DatabaseGroup group) {
        synchronized (pings) {
            long pingPeriod = PING_DURATION.getSeconds() / 2;
            ScheduledFuture scheduledFuture = scheduler.scheduleAtFixedRate(
                () -> ping(group), 0, pingPeriod, TimeUnit.SECONDS
            );
            pings.put(group, scheduledFuture);
            log.debug("Started ping in group: " + group);
        }
    }

    private void stopPing(DatabaseGroup group) {
        synchronized (pings) {
            ScheduledFuture scheduledFuture = pings.remove(group);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                log.debug("Stopped ping in group: " + group);
            }
        }
    }

    private void ping(DatabaseGroup group) {
        masterJDBCTemplate.update(
            "update db_group_orchestrator set last_ping_time = current_timestamp where group_id = ? and client_id = ?",
            group.getGroupId(), group.getClientGuid().toString());
        log.debug("ping group: " + group);
    }

    private String getUserName(String schemaName, int index) {
        return String.format("%s_%02d", schemaName.toUpperCase(), index);
    }
}

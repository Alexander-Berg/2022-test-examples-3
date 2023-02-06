package ru.yandex.mail.pglocal;

import lombok.val;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.pglocal.Manager.DbOptions;
import ru.yandex.mail.pglocal.Manager.MasterOptions;
import ru.yandex.mail.pglocal.Manager.SlaveOptions;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.waitAtMost;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalPgTest {
    private static final BinarySource PG_BINARY_SOURCE = new BinarySandboxSource(Version.V11);
    private static final MigrationSource MIGRATIONS = new MigrationSource.ResourceFolder("migrations");
    private static final String USER = "testuser";
    private static final String SLAVE_APP_NAME = "slave007";

    private static Manager manager;

    private static Path resolveServerPath(String name) {
        return SystemUtils.getJavaIoTmpDir()
            .toPath()
            .resolve(name);
    }

    private static boolean hasSyncSlave(Database database) {
        return database.fetch("SELECT sync_state FROM pg_stat_replication", resultSet -> {
            try {
                while (resultSet.next()) {
                    if (Objects.equals(resultSet.getString("sync_state"), "sync")) {
                        return true;
                    }
                }
                return false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @BeforeAll
    public static void init() {
        manager = new Manager(PG_BINARY_SOURCE);
    }

    @Test
    @DisplayName("Verify database could be started")
    void basicTest() {
        val serverDataPath = resolveServerPath("testdb");
        val options = new MasterOptions(DbOptions.withRandomPort(USER), SynchronousCommit.OFF, emptyList());

        val master = manager.startNewMaster(serverDataPath, options);
        try {
            master.createDatabase("testdb", USER, Optional.of(MIGRATIONS));
            assertEquals(master.status(), DatabaseStatus.STARTED, "Server expected to be started");
        } finally {
            master.purge();
        }
    }

    @Test
    @DisplayName("Verify database cluster, containing master and slave, could be started")
    void masterSyncSlaveTest() {
        val dbName = "durable_db";
        val masterDataPath = resolveServerPath("test_master_replica_db");
        val slaveDataPath = resolveServerPath("replica_db");

        val initialMasterOptions = new MasterOptions(DbOptions.withRandomPort(USER), SynchronousCommit.OFF, emptyList());
        Server master = manager.startNewMaster(masterDataPath, initialMasterOptions);
        master.createDatabase(dbName, USER, Optional.of(MIGRATIONS));
        master.stop();

        val masterOptions = new MasterOptions(DbOptions.withRandomPort(USER), SynchronousCommit.REMOTE_WRITE,
            singletonList(SLAVE_APP_NAME));
        master = manager.startMaster(masterDataPath, masterOptions);

        val slaveOptions = new SlaveOptions(DbOptions.withRandomPort(USER), SLAVE_APP_NAME, dbName);
        val slave = manager.startNewSlave(slaveDataPath, masterOptions.getDb().getPort(), slaveOptions);

        val masterDb = master.attachDatabase(dbName);
        val slaveDb = slave.attachDatabase(dbName);

        waitAtMost(10, SECONDS).until(() -> hasSyncSlave(masterDb));
        masterDb.execute("INSERT INTO users VALUES (DEFAULT, 'Bob', 25)");

        val age = slaveDb.fetch("SELECT age FROM users WHERE name = 'Bob'", rs -> {
            try {
                assertTrue(rs.next());
                return rs.getInt("age");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals((int) age, 25);

        slave.purge();
        master.purge();
    }
}

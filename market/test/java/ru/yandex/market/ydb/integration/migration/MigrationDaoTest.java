package ru.yandex.market.ydb.integration.migration;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MigrationDaoTest extends ServiceTestBase {

    private static final String HASH = "some hash";
    private static final String ANOTHER_HASH = "another hash";
    private static final String NAME = "some name";
    private static final String TABLE = "some table";

    @Autowired
    private MigrationDao migrationDao;

    @Test
    void shouldAddInitialMigrationRecord() {
        MigrationEntry change = migrationDao.add(MigrationEntry.builder()
                .hash(ANOTHER_HASH)
                .changeName(NAME)
                .tableName(TABLE)
                .initial(true));

        assertThat(migrationDao.getInitialMigrationByTableName(change.getTableName()).isPresent(), is(true));
    }

    @Test
    void shouldAddMigrationRecord() {
        MigrationEntry change = migrationDao.add(MigrationEntry.builder()
                .hash(HASH)
                .changeName(NAME)
                .tableName(TABLE)
                .initial(false));

        assertThat(migrationDao.getMigrationByHash(change.getHash()).isPresent(), is(true));
    }

    @Test
    void shouldGetLastMigrationRecord() {
        migrationDao.add(MigrationEntry.builder()
                .hash(HASH)
                .changeName(NAME)
                .tableName(TABLE)
                .initial(true));

        migrationDao.add(MigrationEntry.builder()
                .hash(ANOTHER_HASH)
                .changeName(NAME)
                .tableName(TABLE)
                .initial(false));

        migrationDao.add(MigrationEntry.builder()
                .hash(ANOTHER_HASH + 1)
                .changeName(NAME + 1)
                .tableName(TABLE)
                .initial(false));

        Optional<MigrationEntry> change = migrationDao.getLastMigrationByTableName(TABLE);

        assertThat(change.isPresent(), is(true));
        assertThat(change.get().getHash(), is(ANOTHER_HASH + 1));
    }
}

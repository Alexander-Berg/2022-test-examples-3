package ru.yandex.direct.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.mysql.schema.DatabaseSchema;
import ru.yandex.direct.mysql.schema.KeySchema;
import ru.yandex.direct.mysql.schema.ServerSchema;
import ru.yandex.direct.mysql.schema.TableSchema;
import ru.yandex.direct.utils.io.TempDirectory;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class CheckLocalSchemas {
    @Test
    public void test() throws IOException, InterruptedException, SQLException {
        /*
         * Not a real test, since it depends on installed mysql server binaries
         * <p>
         * Currently only works with mysql 5.7.6+
         * On OSX you would need to `brew install percona-server` as an example
         */
        try (TempDirectory tmpDir = new TempDirectory("mysql-test-server")) {
            MySQLServerBuilder serverBuilder = new MySQLServerBuilder();
            serverBuilder.setDataAndConfigDir(tmpDir.getPath());

            // Важно, чтобы assume был после setDataAndConfigDir и вот почему:
            //
            // В Ubuntu для mysqld есть apparmor-профиль, который запрещает доступ к каким попало файлам.
            // При запуске из ya make временные файлы (в т.ч. tmpDir) создаются в домашней папке пользователя.
            //
            // Поэтому для проверки наличия mysqld важно вызывать не просто mysqld --version,
            // а mysqld --version --datadir=<tmpDir>. Тогда, если есть apparmor-профиль, тест будет пропущен,
            // иначе он был пофейлился.
            assumeTrue(serverBuilder.mysqldIsAvailable());

            serverBuilder.initializeDataDir();
            try (MySQLServer server = serverBuilder.start()) {
                try (Connection conn = server.connect()) {
                    MySQLUtils.executeUpdate(conn, "create database foobar");
                    conn.setCatalog("foobar");
                    MySQLUtils.executeUpdate(conn,
                            "create table tbl1 (id int not null primary key, value timestamp default " +
                                    "current_timestamp)");
                    MySQLUtils.executeUpdate(conn,
                            "create table tbl2 (id int not null primary key, tbl1id int not null references tbl1 (id)" +
                                    ", foo1 int unique, foo2 int not null unique)");
                    MySQLUtils.executeUpdate(conn,
                            "create table tbl3 (id int not null primary key, value bit(3) default 5)");
                    MySQLUtils.executeUpdate(conn,
                            "create table tbl4 (id int not null primary key, value varchar(128) default " +
                                    "'foo''bar\\\\baz')");
                    MySQLUtils.executeUpdate(conn,
                            "create table tbl5 (id int not null primary key, value int unsigned default 4294967295)");
                    ServerSchema schema = ServerSchema.dump(conn);
                    assertThat(schema.getDatabases(), hasSize(1));
                    DatabaseSchema database = schema.getDatabases().get(0);
                    assertThat(database.getName(), is("foobar"));

                    List<TableSchema> tables = new ArrayList<>(database.getTables());
                    assertThat(tables, hasSize(5));
                    tables.sort(Comparator.comparing(TableSchema::getName));

                    TableSchema tbl1 = tables.get(0);
                    assertThat(tbl1.getName(), is("tbl1"));
                    assertThat(tbl1.getColumns(), hasSize(2));
                    assertThat(tbl1.getColumns().get(0).getName(), is("id"));
                    assertThat(tbl1.getColumns().get(1).getName(), is("value"));
                    assertThat(tbl1.getColumns().get(1).getDefaultValue(), is("CURRENT_TIMESTAMP"));
                    assertThat(tbl1.getKeys(), hasSize(1));
                    assertThat(tbl1.getKeys().get(0).getName(), is("PRIMARY"));
                    assertThat(tbl1.getKeys().get(0).getColumns(), hasSize(1));
                    assertThat(tbl1.getKeys().get(0).getColumns().get(0).getName(), is("id"));

                    TableSchema tbl2 = tables.get(1);
                    assertThat(tbl2.getName(), is("tbl2"));
                    assertThat(tbl2.getColumns(), hasSize(4));
                    assertThat(tbl2.getColumns().get(0).getName(), is("id"));
                    assertThat(tbl2.getColumns().get(1).getName(), is("tbl1id"));
                    assertThat(tbl2.getColumns().get(2).getName(), is("foo1"));
                    assertThat(tbl2.getColumns().get(3).getName(), is("foo2"));
                    List<KeySchema> tbl2keys = new ArrayList<>(tbl2.getKeys());
                    assertThat(tbl2keys, hasSize(3));
                    tbl2keys.sort(Comparator.comparing(KeySchema::getName));
                    KeySchema primary = tbl2keys.get(0);
                    KeySchema foo1 = tbl2keys.get(1);
                    KeySchema foo2 = tbl2keys.get(2);
                    assertThat(primary.getName(), is("PRIMARY"));
                    assertThat(foo1.getName(), is("foo1"));
                    assertThat(foo1.getType(), is("BTREE"));
                    assertThat(foo1.isUnique(), is(true));
                    assertThat(foo1.getColumns(), hasSize(1));
                    assertThat(foo1.getColumns().get(0).getName(), is("foo1"));
                    assertThat(foo1.getColumns().get(0).isNullable(), is(true));
                    assertThat(foo2.getName(), is("foo2"));
                    assertThat(foo2.getType(), is("BTREE"));
                    assertThat(foo2.isUnique(), is(true));
                    assertThat(foo2.getColumns(), hasSize(1));
                    assertThat(foo2.getColumns().get(0).getName(), is("foo2"));
                    assertThat(foo2.getColumns().get(0).isNullable(), is(false));

                    TableSchema tbl3 = tables.get(2);
                    assertThat(tbl3.getName(), is("tbl3"));
                    assertThat(tbl3.getColumns(), hasSize(2));
                    assertThat(tbl3.getColumns().get(1).getName(), is("value"));
                    assertThat(tbl3.getColumns().get(1).getDefaultValue(), is("b'101'"));

                    TableSchema tbl4 = tables.get(3);
                    assertThat(tbl4.getName(), is("tbl4"));
                    assertThat(tbl4.getColumns(), hasSize(2));
                    assertThat(tbl4.getColumns().get(1).getName(), is("value"));
                    assertThat(tbl4.getColumns().get(1).getDefaultValue(), is("foo'bar\\baz"));

                    TableSchema tbl5 = tables.get(4);
                    assertThat(tbl5.getName(), is("tbl5"));
                    assertThat(tbl5.getColumns(), hasSize(2));
                    assertThat(tbl5.getColumns().get(1).getName(), is("value"));
                    assertThat(tbl5.getColumns().get(1).getDefaultValue(), is("4294967295"));

                    byte[] json = schema.toJsonBytes();
                    ServerSchema decoded = ServerSchema.fromJson(json);
                    assertThat(decoded, is(schema));
                }
            }
        }
    }
}

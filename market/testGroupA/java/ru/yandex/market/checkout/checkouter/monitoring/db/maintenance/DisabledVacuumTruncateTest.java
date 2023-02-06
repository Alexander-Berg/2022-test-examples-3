package ru.yandex.market.checkout.checkouter.monitoring.db.maintenance;

import java.sql.Types;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.jooq.Table;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkouter.jooq.Tables;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.application.AbstractArchiveWebTestBase.ARCHIVING_TABLES;

public class DisabledVacuumTruncateTest extends AbstractServicesTestBase {

    private static final List<Table<?>> NO_VACUUM_TRUNCATE_TABLES = ImmutableList.<Table<?>>builder()
            .addAll(ARCHIVING_TABLES)
            .add(Tables.PARCEL_ROUTE)
            .build();

    @Test
    public void checkVacuumTruncateDisabledForTables() {
        String[] expectedTables = NO_VACUUM_TRUNCATE_TABLES.stream()
                .map(Table::getName)
                .toArray(String[]::new);
        String[] enabledTables = masterJdbcTemplate.queryForList(
                        "with vacuum_truncate_option as (\n" +
                                "    select oid, option_value\n" +
                                "    from pg_class, pg_options_to_table(reloptions)\n" +
                                "    where option_name = 'vacuum_truncate'\n" +
                                ")\n" +
                                "select c.relname\n" +
                                "from pg_class c\n" +
                                "   join pg_namespace n on n.oid = c.relnamespace\n" +
                                "   left join vacuum_truncate_option o on o.oid = c.oid\n" +
                                "where n.nspname = 'public'\n" +
                                "  and c.relname = any (?)\n" +
                                "  and coalesce(o.option_value, '<null>') <> 'false';",
                        new Object[]{expectedTables},
                        new int[]{Types.ARRAY},
                        String.class)
                .toArray(String[]::new);
        assertThat(enabledTables).as("tables to set vacuum_truncate=false").isEmpty();
    }

    @Test
    public void checkVacuumTruncateDisabledForTableToasts() {
        String[] expectedTables = NO_VACUUM_TRUNCATE_TABLES.stream()
                .map(Table::getName)
                .toArray(String[]::new);
        String[] enabledTables = masterJdbcTemplate.queryForList(
                        "with vacuum_truncate_option as (\n" +
                                "    select oid, option_value\n" +
                                "    from pg_class, pg_options_to_table(reloptions)\n" +
                                "    where option_name = 'vacuum_truncate'\n" +
                                ")\n" +
                                "select c.relname\n" +
                                "from pg_class c\n" +
                                "   join pg_namespace n on n.oid = c.relnamespace\n" +
                                "   join pg_class t on t.oid = c.reltoastrelid\n" +
                                "   left join vacuum_truncate_option o on o.oid = t.oid\n" +
                                "where n.nspname = 'public'\n" +
                                "  and c.relname = any (?)\n" +
                                "  and coalesce(o.option_value, '<null>') <> 'false';",
                        new Object[]{expectedTables},
                        new int[]{Types.ARRAY},
                        String.class)
                .toArray(String[]::new);
        assertThat(enabledTables).as("tables to set toast.vacuum_truncate=false").isEmpty();
    }

}

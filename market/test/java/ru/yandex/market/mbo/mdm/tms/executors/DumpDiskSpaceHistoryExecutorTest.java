package ru.yandex.market.mbo.mdm.tms.executors;

import java.sql.Timestamp;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.storage.PgDiskSpaceService;

@SuppressWarnings("checkstyle:MagicNumber")
public class DumpDiskSpaceHistoryExecutorTest extends MdmBaseDbTestClass {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void testItWorks() {
        String table = "mdm.disk_space_history";
        PgDiskSpaceService diskSpaceService = new PgDiskSpaceService(jdbcTemplate, table);
        DumpDiskSpaceHistoryExecutor executor = new DumpDiskSpaceHistoryExecutor(diskSpaceService);
        executor.execute();

        Map<String, Object> row =
            jdbcTemplate.queryForMap("select * from " + table + " limit 1", Map.of());
        Assertions.assertThat((Integer) row.get("snapshot_id")).isGreaterThan(0);
        Assertions.assertThat((Timestamp) row.get("snapshot_ts")).isAfter("2020-01-01");
        Assertions.assertThat((Long) row.get("total_size")).isGreaterThan(0);
        Assertions.assertThat((String) row.get("schema")).isNotBlank();
        Assertions.assertThat((String) row.get("table_name")).isNotBlank();
    }
}

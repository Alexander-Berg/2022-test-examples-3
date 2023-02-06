package ru.yandex.market.mbo.integration.test.tms.dashboard;

import java.util.List;
import java.util.Random;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;

import ru.yandex.market.mbo.core.dashboard.DumpHistoryDao;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpExtractorData;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;

/**
 * @author s-ermakov
 */
public class DumpHistoryDaoTest extends BaseIntegrationTest {

    @Resource
    private DumpHistoryDao dumpHistoryDao;

    @Resource
    private JdbcOperations siteCatalogPgJdbcTemplate;

    // TODO удалить, когда будет нормальная накатка ликвибейза в тестах
    @Before
    public void setUp() {
        siteCatalogPgJdbcTemplate.update("create schema if not exists site_catalog");
        siteCatalogPgJdbcTemplate.update("set search_path to site_catalog, public");
        siteCatalogPgJdbcTemplate.update("create sequence if not exists mbo_dump_history_id start 4000000");

        siteCatalogPgJdbcTemplate.update(
            "create table mbo_dump_history\n" +
                "(\n" +
                "    id              bigint      default nextval('mbo_dump_history_id'::regclass) not null\n" +
                "        constraint mbo_dump_history_pkey\n" +
                "            primary key,\n" +
                "    created         timestamp with time zone                                     not null,\n" +
                "    name            varchar(200)                                                 not null,\n" +
                "    message         bytea,\n" +
                "    type            varchar(100)                                                 not null,\n" +
                "    session_name    varchar(15),\n" +
                "    finished        timestamp with time zone,\n" +
                "    files_count     numeric(9)  default 0                                        not null,\n" +
                "    files_size      numeric(18) default 0                                        not null,\n" +
                "    process_status  smallint    default 0                                        not null,\n" +
                "    status          smallint    default 0                                        not null,\n" +
                "    hostname        text,\n" +
                "    restarted       numeric(1)  default 0                                        not null,\n" +
                "    additional_info text\n" +
                ")"
        );

        siteCatalogPgJdbcTemplate.update(
            "create table if not exists mbo_dump_history_files\n" +
                "(\n" +
                "    dhfid      serial\n" +
                "        constraint mbo_dump_history_files_pkey\n" +
                "            primary key,\n" +
                "    file_path  varchar(250),\n" +
                "    registered timestamp with time zone,\n" +
                "    log_id     bigint not null,\n" +
                "    message    bytea\n" +
                ")"
        );
    }

    @Test
    public void testLargeMessageSaveAndRead() {
        String message = createReallyBigMessage();

        DumpGroupData dumpGroupData = new DumpGroupData();
        dumpGroupData.setType(DumpGroupData.Type.STUFF.getStringValue());
        dumpGroupData.setSessionName("TEST_STUFF");
        dumpGroupData.setHostname("integration_tests");
        dumpHistoryDao.logStatusUpdateInHistory(dumpGroupData, message);

        List<DumpExtractorData> extractorsLogs = dumpHistoryDao.getDumpExtractorsLogs(dumpGroupData);
        Assertions.assertThat(extractorsLogs).hasSize(1);
        Assertions.assertThat(extractorsLogs.get(0).getExceptionMessage()).isEqualTo(message);
    }

    @SuppressWarnings("checkstyle:magicNumber")
    private String createReallyBigMessage() {
        Random random = new Random(17850);
        String alphabet = "123abc_абвгде";
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 1_000_000; i++) {
            int index = random.nextInt(alphabet.length());
            stringBuilder.append(alphabet.charAt(index));
        }
        return stringBuilder.toString();
    }
}

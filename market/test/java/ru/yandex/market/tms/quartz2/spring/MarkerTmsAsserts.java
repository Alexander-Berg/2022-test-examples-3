package ru.yandex.market.tms.quartz2.spring;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.util.AssertionErrors.assertTrue;

final class MarkerTmsAsserts {

    private MarkerTmsAsserts() {
        throw new UnsupportedOperationException();
    }

    static void assertExistTestQrtzLogRecords(JdbcTemplate jdbcTemplate) {
        int count = jdbcTemplate.queryForObject("SELECT count(*) FROM TEST_QRTZ_LOG", Integer.class);
        assertTrue("В таблице TEST_QRTZ_LOG должна появиться запись", count > 0);
    }

}

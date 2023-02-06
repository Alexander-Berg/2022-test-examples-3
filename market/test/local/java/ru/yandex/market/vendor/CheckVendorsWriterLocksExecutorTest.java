package ru.yandex.market.vendor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.placement.tms.CheckVendorApiWriterLocksExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by vladimir-k on 03/09/2018.
 */
@DbUnitDataSet(before = "/ru/yandex/market/vendor/CheckVendorsWriterLocksExecutorTest/before.csv")
class CheckVendorsWriterLocksExecutorTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    CheckVendorApiWriterLocksExecutor executor;

    @Autowired
    NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;

    @Test
    void testContext() {
        Long openLockIds = vendorNamedParameterJdbcTemplate.queryForObject(
                "SELECT count(ID) FROM VENDORS.V_OPEN_WRITE_LOCK",
                emptyMap(), Long.class);
        assertThat(openLockIds).as("single open write lock is expected").isEqualTo(1L);
        executor.doJob(null);
    }
}

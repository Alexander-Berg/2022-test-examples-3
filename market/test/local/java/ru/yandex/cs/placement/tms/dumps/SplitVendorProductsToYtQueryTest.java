package ru.yandex.cs.placement.tms.dumps;

import java.time.Clock;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.yt.VendorProductsToYtConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorProductsToYtQueryTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorProductsToYtQueryTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class SplitVendorProductsToYtQueryTest extends AbstractCsPlacementTmsFunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @Test
    void testWithoutContext() {
        var rsIsEmpty = new AtomicBoolean(true);
        var query = new SplitVendorProductsToYtQuery(
                vendorNamedParameterJdbcTemplate,
                csBillingNamedParameterJdbcTemplate,
                Clock.systemDefaultZone());

        query.execute(rs -> {
            rsIsEmpty.set(false);
            var node = new VendorProductsToYtConverter().convert(rs);
            assertThat(node.getLong("VENDOR_ID"))
                    .isIn(321L, 322L);
        }, null);
        assertThat(rsIsEmpty.get()).isFalse();
    }

    @Test
    void testWithContext() {

        var rsIsEmpty = new AtomicBoolean(true);
        var query = new SplitVendorProductsToYtQuery(
                vendorNamedParameterJdbcTemplate,
                csBillingNamedParameterJdbcTemplate,
                Clock.systemDefaultZone());
        var additionalArgs = new JobDataMap(Collections.singletonMap("DATE", new Date()));

        query.execute(rs -> {
            rsIsEmpty.set(false);
            var node = new VendorProductsToYtConverter().convert(rs);
            assertThat(node.getLong("VENDOR_ID"))
                    .isIn(321L, 322L);
        }, additionalArgs);
        assertThat(rsIsEmpty.get()).isFalse();

    }
}

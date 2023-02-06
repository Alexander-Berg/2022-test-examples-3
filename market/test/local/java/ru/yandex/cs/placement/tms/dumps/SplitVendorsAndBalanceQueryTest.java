package ru.yandex.cs.placement.tms.dumps;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.yt.VendorHoldToYtConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorsAndBalanceQueryTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorsAndBalanceQueryTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class SplitVendorsAndBalanceQueryTest extends AbstractCsPlacementTmsFunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @Test
    void execute() {
        var rsIsEmpty = new AtomicBoolean(true);
        var query = new SplitVendorsAndBalanceQuery(
                vendorNamedParameterJdbcTemplate,
                csBillingNamedParameterJdbcTemplate
        );
        query.execute(rs -> {
            rsIsEmpty.set(false);
            var convertor = new VendorHoldToYtConverter();
            var node = convertor.convert(rs);
            assertThat(node.getLong("DATASOURCE_ID"))
                    .isIn(1010L, 1011L, 1012L);
        }, null);
        assertThat(rsIsEmpty.get()).isFalse();
    }
}

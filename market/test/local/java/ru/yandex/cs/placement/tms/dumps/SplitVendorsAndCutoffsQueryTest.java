package ru.yandex.cs.placement.tms.dumps;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.yt.PaidOpinionsToYtConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorsAndCutoffsQueryTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorsAndCutoffsQueryTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class SplitVendorsAndCutoffsQueryTest extends AbstractCsPlacementTmsFunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @Test
    void execute() {
        var rsIsEmpty = new AtomicBoolean(true);
        var query = new SplitVendorsAndCutoffsQuery(
                vendorNamedParameterJdbcTemplate,
                csBillingNamedParameterJdbcTemplate
        );
        query.execute(rs -> {
            rsIsEmpty.set(false);
            var convertor = new PaidOpinionsToYtConverter();
            var node = convertor.convert(rs);
            assertThat(node.getLong("VENDOR_ID"))
                    .isIn(321L, 322L);

        }, null);
        assertThat(rsIsEmpty.get()).isFalse();
    }
}

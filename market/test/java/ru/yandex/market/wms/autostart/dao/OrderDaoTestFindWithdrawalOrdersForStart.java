package ru.yandex.market.wms.autostart.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.dao.WithdrawalOrdersTestData.withdrawalOrder900001003;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/pick_locations.xml", connection = "wmwhseConnection", type =
                REFRESH),
        @DatabaseSetup(value = "/fixtures/autostart/2/skus.xml", connection = "wmwhseConnection"),

        // Standard orders: must be ignored
        @DatabaseSetup(value = "/fixtures/autostart/2/orders_to_batch.xml", connection = "wmwhseConnection", type =
                REFRESH),

        // Withdrawal orders with "created external" status: must be fetched
        @DatabaseSetup(value = "/fixtures/autostart/withdrawals/withdrawal_orders_fit__created.xml", connection =
                "wmwhseConnection", type = REFRESH),

        // Withdrawal orders with other status: must be ignored
        @DatabaseSetup(value = "/fixtures/autostart/withdrawals/withdrawal_orders_fit__other.xml", connection =
                "wmwhseConnection", type = REFRESH),

        // Withdrawal orders with "created external" but with non-empty batchOrderNumber: must be ignored
        @DatabaseSetup(value = "/fixtures/autostart/withdrawals" +
                "/withdrawal_orders_fit__with_non_empty_batchOrderNumber.xml", connection = "wmwhseConnection", type
                = REFRESH),

        // Withdrawal orders with "created external" but referenced from waveDetails: must be ignored
        @DatabaseSetup(value = "/fixtures/autostart/withdrawals/withdrawal_orders_fit__ref_from_waveDetail.xml",
                connection = "wmwhseConnection", type = REFRESH),

        // Records in waveDetails, containing currently processed withdrawals
        @DatabaseSetup(value = "/fixtures/autostart/withdrawals/withdrawal_waveDetail_with_processed_withdrawals.xml",
                connection = "wmwhseConnection", type = REFRESH),
})
class OrderDaoTestFindWithdrawalOrdersForStart extends AutostartIntegrationTest {

    @Autowired
    protected OrderDao dao;

    @Test
    void ordersData() {
        List<Order> actual = dao.findWithdrawalOrdersForStart();
        assertThat(actual, is(equalTo(listOf(withdrawalOrder900001003()))));
    }
}

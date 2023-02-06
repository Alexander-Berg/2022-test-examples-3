package ru.yandex.market.wms.common.spring.dao;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.common.spring.dao.OrderDaoTestData.orderB000001001;
import static ru.yandex.market.wms.common.spring.dao.OrderDaoTestData.orderB000001002;
import static ru.yandex.market.wms.common.spring.dao.OrderDaoTestData.orderB000001003;
import static ru.yandex.market.wms.common.spring.dao.OrderDetailDaoTestData.orderB00000100100001;
import static ru.yandex.market.wms.common.spring.dao.OrderDetailDaoTestData.orderB00000100100002;
import static ru.yandex.market.wms.common.spring.dao.OrderDetailDaoTestData.orderB00000100200001;


class OrderDetailDaoTest extends IntegrationTest {

    @Autowired
    protected OrderDetailDao dao;

    @Disabled
    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/db/dao/sku/before.xml", connection = "wmwhseConnection"),
            @DatabaseSetup(value = "/db/dao/order/data.xml", connection = "wmwhseConnection"),
            @DatabaseSetup(value = "/db/dao/orderdetail/data.xml", connection = "wmwhseConnection"),

    })
    public void findOrderDetails() {
        assertThat(dao.findOrderDetails(Arrays.asList(orderB000001001(), orderB000001002(), orderB000001003())))
                .containsExactlyInAnyOrder(
                        orderB00000100100001(),
                        orderB00000100100002(),
                        orderB00000100200001()
                );
    }


    @Test
    @DatabaseSetup(value = "/db/dao/orderdetail/before-test.xml")
    public void getOrderDetailsFlowCounts() {
        var map = dao.getOrderDetailsSizeCounts(Arrays.asList("001", "002", "003", "003", "004", "005"));
        assertThat(map.size()).isEqualTo(4);

        assertThat(map.get("001").getOversize()).isEqualTo(2);
        assertThat(map.get("001").getNonConveyable()).isEqualTo(1);

        assertThat(map.get("002").getOversize()).isEqualTo(0);
        assertThat(map.get("002").getNonConveyable()).isEqualTo(0);

        assertThat(map.get("003").getOversize()).isEqualTo(0);
        assertThat(map.get("003").getNonConveyable()).isEqualTo(1);

        assertThat(map.get("004").getOversize()).isEqualTo(2);
        assertThat(map.get("004").getNonConveyable()).isEqualTo(4);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/sku/before.xml")
    @DatabaseSetup(value = "/db/dao/orderdetail/before-clear-batch-by_orederkeys.xml")
    @ExpectedDatabase(
            value = "/db/dao/orderdetail/after-clear-batch-by_orederkeys.xml",
            assertionMode = NON_STRICT
    )
    public void clearBatchOrderNumberByOrderKeys() {

        dao.clearBatchByOrderKeys(List.of("0001"));
    }
}

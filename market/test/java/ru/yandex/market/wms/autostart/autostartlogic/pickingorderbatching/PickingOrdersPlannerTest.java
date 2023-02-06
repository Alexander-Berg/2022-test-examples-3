package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import one.util.streamex.EntryStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.OrderDetailTestData;
import ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData;
import ru.yandex.market.wms.common.lgw.util.CollectionUtil;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class PickingOrdersPlannerTest {

    static List<OrderInventoryDetail> sampleInventoryDetailList() {
        return Arrays.asList(
                OrderInventoryDetailTestData.invROV0000000000000000001C4101001(4),
                OrderInventoryDetailTestData.invROV0000000000000000001C4100001(1),
                OrderInventoryDetailTestData.invROV0000000000000000009C4140009(1)
        );
    }

    static List<PickSku> expectedPickSkus() {
        return Arrays.asList(
                PickSkuTestData.pickSkuROV0000000000000000001C4101001(4),
                PickSkuTestData.pickSkuROV0000000000000000001C4100001(1),
                PickSkuTestData.pickSkuROV0000000000000000009C4140009(1)
        );
    }

    static List<OrderDetail> sampleOrderDetails() {
        return Arrays.asList(
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000001").openQty(BigDecimal.valueOf(6)).build(),
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000002").openQty(BigDecimal.valueOf(3)).build(),
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000009").openQty(BigDecimal.valueOf(1)).build()
        );
    }

    @Test
    public void inventory() {
        Map<SkuId, Queue<OrderInventoryDetail>> actual = PickingOrdersPlanner.inventory(sampleInventoryDetailList());
        Map<SkuId, ArrayList<OrderInventoryDetail>> mapped = EntryStream.of(actual).mapValues(ArrayList::new).toMap();
        MatcherAssert.assertThat(
                mapped,
                Matchers.is(equalTo(
                        CollectionUtil.mapOf(
                                new SkuId("100", "ROV0000000000000000001"), Arrays.asList(
                                        OrderInventoryDetailTestData.invROV0000000000000000001C4101001(4),
                                        OrderInventoryDetailTestData.invROV0000000000000000001C4100001(1)

                                ),
                                new SkuId("100", "ROV0000000000000000009"), Arrays.asList(
                                        OrderInventoryDetailTestData.invROV0000000000000000009C4140009(1)
                                )
                        )
                ))
        );
    }

    @Test
    public void accept() {
        ArrayList<PickSku> output = new ArrayList<>();
        Planner sut = new PickingOrdersPlanner(sampleInventoryDetailList());
        sampleOrderDetails().forEach(orderDetail -> sut.accept(orderDetail, entry -> output.add(entry.getKey())));
        assertThat(output, is(equalTo(expectedPickSkus())));
    }
}

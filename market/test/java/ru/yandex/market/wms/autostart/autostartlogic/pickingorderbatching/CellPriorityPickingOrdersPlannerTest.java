package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.OrderDetailTestData;
import ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData;
import ru.yandex.market.wms.common.spring.dao.entity.PickSku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class CellPriorityPickingOrdersPlannerTest {

    @Test
    public void accept() {
        var inventoryDetails = Arrays.asList(
                OrderInventoryDetailTestData.invROV0000000000000000001C4101001(4),
                OrderInventoryDetailTestData.invROV0000000000000000001C4100001(1),
                OrderInventoryDetailTestData.invROV0000000000000000009C4140009(1)
        );

        var requiredQuantities = new HashMap<SkuId, Integer>();
        requiredQuantities.put(OrderInventoryDetailTestData.invROV0000000000000000001C4101001(4).getSkuId(), 4);
        requiredQuantities.put(OrderInventoryDetailTestData.invROV0000000000000000001C4100001(1).getSkuId(), 1);
        requiredQuantities.put(OrderInventoryDetailTestData.invROV0000000000000000009C4140009(1).getSkuId(), 1);

        var orderDetails = Arrays.asList(
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000001").openQty(BigDecimal.valueOf(6)).build(),
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000002").openQty(BigDecimal.valueOf(3)).build(),
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000009").openQty(BigDecimal.valueOf(1)).build()
        );

        Planner sut = new CellPriorityPickingOrdersPlanner(inventoryDetails, requiredQuantities);

        var expectedPickSkus = Arrays.asList(
                PickSkuTestData.pickSkuROV0000000000000000001C4101001(4),
                PickSkuTestData.pickSkuROV0000000000000000001C4100001(1),
                PickSkuTestData.pickSkuROV0000000000000000009C4140009(1)
        );

        ArrayList<PickSku> output = new ArrayList<>();
        orderDetails.forEach(orderDetail -> sut.accept(orderDetail, entry -> output.add(entry.getKey())));
        assertThat(output, is(equalTo(expectedPickSkus)));
    }

    @Test
    public void acceptSelectsCellWithAllItems() {
        var inventoryDetails = Arrays.asList(
                OrderInventoryDetailTestData.invROV0000000000000000001C4101001(8),
                OrderInventoryDetailTestData.invROV0000000000000000001C4100001(10)
        );

        var requiredQuantities = new HashMap<SkuId, Integer>();
        requiredQuantities.put(OrderInventoryDetailTestData.invROV0000000000000000001C4101001(10).getSkuId(), 10);

        var orderDetails = List.of(
                OrderDetailTestData.template().storerKey("100")
                        .sku("ROV0000000000000000001").openQty(BigDecimal.valueOf(9)).build()
        );

        Planner sut = new CellPriorityPickingOrdersPlanner(inventoryDetails, requiredQuantities);

        var expectedPickSkus = List.of(
                PickSkuTestData.pickSkuROV0000000000000000001C4100001(9)
        );

        ArrayList<PickSku> output = new ArrayList<>();
        orderDetails.forEach(orderDetail -> sut.accept(orderDetail, entry -> output.add(entry.getKey())));
        assertThat(output, is(equalTo(expectedPickSkus)));
    }
}

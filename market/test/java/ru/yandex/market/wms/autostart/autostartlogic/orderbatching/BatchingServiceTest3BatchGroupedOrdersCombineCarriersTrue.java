package ru.yandex.market.wms.autostart.autostartlogic.orderbatching;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;

import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.mapOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000002002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000004001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000005002;

public interface BatchingServiceTest3BatchGroupedOrdersCombineCarriersTrue {

    default Map<String, List<OrderWithDetails>> exampleOrderGroup() {
        return mapOf(
                new Pair<>("G1",
                Arrays.asList(
                        owdB000003003(),
                        owdB000004001(),
                        owdB000005002(),
                        owdB000003004(),
                        owdB000003002(),
                        owdB000001003(),
                        owdB000001004(),
                        owdB000002002(),
                        owdB000003001()
                )));
    }
}

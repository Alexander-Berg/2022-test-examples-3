package ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.wms.autostart.autostartlogic.waves2.CategorizedOrder;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.OrderFlowCategory;
import ru.yandex.market.wms.autostart.core.model.dto.StationType;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.common.spring.dao.entity.DeliveryServiceCutoff;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

public class TestData {
    private static int counter = 0;

    private TestData() { }

    public static StationToCarrier sortStationLink(String carrier, String station) {
        return StationToCarrier.builder()
                .type(StationType.SORT)
                .carrierCode(carrier)
                .stationKey(station)
                .build();
    }

    public static DeliveryServiceCutoff cutoff(String carrier, LocalDateTime currentTime) {
        return DeliveryServiceCutoff.builder()
                    .deliveryServiceCode(carrier)
                    .shippingCutoff(currentTime.plusHours(2))
                    .build();
    }

    public static List<CategorizedOrder> toCategorizedOrders(List<OrderWithDetails> owdList, WaveType waveType) {
        List<CategorizedOrder> orders = new ArrayList<>();
        Set<OrderFlowCategory> categories = new HashSet<>();
        Map<ConsolidationLocationType, Integer> consolidationItemsCnt = new HashMap<>();

        if (waveType == WaveType.OVERSIZE) {
            categories.add(OrderFlowCategory.NON_CONV);
            categories.add(OrderFlowCategory.OVERSIZE);
            categories.add(OrderFlowCategory.SINGLE);
            consolidationItemsCnt.put(ConsolidationLocationType.OVERSIZE, 1);
        } else {
            categories.add(OrderFlowCategory.CONV);
            categories.add(OrderFlowCategory.SINGLE);
            consolidationItemsCnt.put(ConsolidationLocationType.SINGLES, 1);
        }
        for (OrderWithDetails owd: owdList) {
            orders.add(new CategorizedOrder(owd.getOrder(), categories, consolidationItemsCnt));
        }
        return orders;
    }

    public static List<OrderDetail> toOrderDetails(List<OrderWithDetails> owdList) {
        return owdList.stream().flatMap(owd -> owd.getOrderDetails().stream()).toList();
    }
}

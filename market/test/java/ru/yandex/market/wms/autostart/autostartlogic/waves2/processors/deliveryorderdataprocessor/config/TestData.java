package ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.yandex.market.wms.autostart.autostartlogic.waves2.CategorizedOrder;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.OrderFlowCategory;
import ru.yandex.market.wms.autostart.core.model.dto.StationType;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.common.spring.dao.entity.DeliveryServiceCutoff;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

public class TestData {
    private static int counter = 0;

    private TestData() { }

    public static List<CategorizedOrder> makeOrders(
            Set<String> carriers, LocalDateTime currentTime, WaveType waveType) {
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
        for (String carrierCode: carriers) {
            Order order = Order.builder()
                    .orderKey("order:" + (counter++))
                    .storerKey("storer1")
                    .carrierCode(carrierCode)
                    .totalqty(BigDecimal.ONE)
                    .shipmentDateTime(OffsetDateTime.of(currentTime.plusHours(2), ZoneOffset.ofHours(0)))
                    .build();
            orders.add(new CategorizedOrder(order, categories, consolidationItemsCnt));
        }
        return orders;
    }

    public static CategorizedOrder makeComplexOrder(String carrier, LocalDateTime currentTime) {
        Set<OrderFlowCategory> categories = new HashSet<>();
        Map<ConsolidationLocationType, Integer> consolidationItemsCnt = new HashMap<>();

        categories.add(OrderFlowCategory.CONV);
        categories.add(OrderFlowCategory.OVERSIZE);
        categories.add(OrderFlowCategory.MULTIPACK_CARRIER);
        consolidationItemsCnt.put(ConsolidationLocationType.OVERSIZE, 1);

        Order order = Order.builder()
                .orderKey("order:" + (counter++))
                .storerKey("storer1")
                .carrierCode(carrier)
                .totalqty(BigDecimal.valueOf(2))
                .shipmentDateTime(OffsetDateTime.of(currentTime.plusHours(2), ZoneOffset.ofHours(0)))
                .build();
        return new CategorizedOrder(order, categories, consolidationItemsCnt);
    }

    public static StationToCarrier sortStationLink(String carrier, String station) {
        return StationToCarrier.builder()
                .type(StationType.SORT)
                .carrierCode(carrier)
                .stationKey(station)
                .build();
    }

    public static StationToCarrier consLocationsLink(String carrier, String station, ConsolidationLocationType type) {
        return StationToCarrier.builder()
                .type(StationType.CONSOLIDATION)
                .carrierCode(carrier)
                .stationKey(station)
                .consolidationLocationType(type)
                .build();
    }

    public static TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> cutoffs(
            Set<String> carriers, LocalDateTime currentTime) {
        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> cutoffs = new TreeMap<>();
        for (String carrier: carriers) {
            DeliveryServiceCutoff cutoff = DeliveryServiceCutoff.builder()
                    .deliveryServiceCode(carrier)
                    .shippingCutoff(currentTime.plusHours(2))
                    .build();
            cutoffs.computeIfAbsent(currentTime.plusHours(2), (k) -> new ArrayList<>()).add(cutoff);
        }
        return cutoffs;
    }
}

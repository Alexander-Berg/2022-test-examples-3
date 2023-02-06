package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import ru.yandex.market.wms.common.model.enums.RotationType;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.PickSkuLocation;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuProperties;

public class RandomOrdersGenerator {
    private static final Instant NOW = Instant.now();

    public List<OrderWithDetails> genOwd(
            int ordersNum, int minItems, int maxItems, int uniqueItemsNum, LocalDateTime middleDate) {
        if (uniqueItemsNum < maxItems) {
            uniqueItemsNum = maxItems;
        }
        List<Integer> listSku = new ArrayList<>();
        for (int i = 0; i < uniqueItemsNum; i++) {
            listSku.add(i);
        }

        List<OrderWithDetails> owd = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < ordersNum; i++) {
            LocalDateTime randomDate =
                    i % 5 == 0 ? middleDate.minusHours(i % 7) : middleDate.plusHours(i % 7);

            int itemsNum = random.nextInt(maxItems - minItems + 1) + minItems;
            List<Integer> skus = random.ints(0, listSku.size())
                    //.distinct() commented it because order detail with same sku can be in the same order
                    .limit(itemsNum)
                    .boxed()
                    .toList();
            owd.add(genOwd(i, "C1", randomDate, skus));
        }
        return owd;
    }

    public OrderWithDetails genOwd(int id, String carrier, LocalDateTime shipmentDateTime) {
        Order order = genOrder(id, carrier, shipmentDateTime);
        List<OrderDetail> details = new ArrayList<>();
        details.add(genOd(order.getOrderKey(), "ROV" + LocalTime.now().getNano() % 10, 1));
        order.setTotalqty(BigDecimal.ONE);
        return OrderWithDetails.builder().order(order).orderDetails(details).build();
    }

    public OrderWithDetails genOwd(int id, String carrier, LocalDateTime shipmentDateTime, List<Integer> skuIdList) {
        Order order = genOrder(id, carrier, shipmentDateTime);
        List<OrderDetail> details = new ArrayList<>();
        for (Integer sku: skuIdList) {
            details.add(genOd(order.getOrderKey(), "ROV" + sku, 0));
        }
        Optional<BigDecimal> sumQty = details.stream().map(OrderDetail::getOpenQty).reduce(BigDecimal::add);
        order.setTotalqty(sumQty.get());
        return OrderWithDetails.builder().order(order).orderDetails(details).build();
    }

    public List<OrderInventoryDetail> genInventoryExactly(
            List<OrderWithDetails> orders, int zonesNum, int lotNum, int withExpDatePriority) {
        Map<SkuId, Integer> skuCounter = new HashMap<>();
        for (OrderWithDetails order: orders) {
            for (OrderDetail orderDetail: order.getOrderDetails()) {
                SkuId skuId = SkuId.of(orderDetail.getStorerKey(), orderDetail.getSku());
                skuCounter.putIfAbsent(skuId, 0);
                skuCounter.computeIfPresent(skuId, (key, val1) -> val1 + orderDetail.getOpenQty().intValue());
            }
        }

        List<String> zones = new ArrayList<>();
        for (int i = 0; i < zonesNum; i++) {
            zones.add("ZONE" + i);
        }

        Map<String, Integer> locationsNum = new HashMap<>();
        List<OrderInventoryDetail> inventories = new ArrayList<>();

        Random random = new Random();
        for (Map.Entry<SkuId, Integer> entry: skuCounter.entrySet()) {
            int skuNumbers = entry.getValue();
            while (skuNumbers > 0) {
                String zone = zones.get(random.nextInt(zonesNum));
                int delta = skuNumbers;
                if (skuNumbers > 10) {
                    delta = random.nextInt(skuNumbers / 4) + 1;
                }
                skuNumbers -= delta;
                int newLoc = 0;
                if (locationsNum.containsKey(zone)) {
                    locationsNum.compute(zone, (key, val) -> val + 1);
                    newLoc = locationsNum.get(zone);
                } else {
                    locationsNum.put(zone, 0);
                }
                inventories.add(genOid(entry.getKey(), zone, newLoc, delta, lotNum, withExpDatePriority));
            }
        }
        return inventories;
    }

    private Order genOrder(int id, String carrier, LocalDateTime date) {
        return Order.builder()
                .orderKey("order" + id)
                .carrierCode(carrier)
                .shipmentDateTime(date.atOffset(ZoneOffset.UTC))
                .build();
    }

    private OrderDetail genOd(String orderKey, String sku, int qty) {
        Random random = new Random();
        return OrderDetail.builder()
                .orderKey(orderKey)
                .storerKey("STORER-KEY")
                .sku(sku)
                .openQty(qty == 0 ? new BigDecimal(random.nextInt(3) + 1) : BigDecimal.valueOf(qty))
                .build();
    }

    private OrderInventoryDetail genOid(
            SkuId skuId, String zone, int locId, int qty, int lotNum, int withExpDatePriority) {
        PickSkuLocation location = genLocation(zone, locId, lotNum);
        return OrderInventoryDetail.builder()
                .skuId(skuId)
                .location(location)
                .skuProperties(genSkuProperties(withExpDatePriority, skuId))
                .qty(qty)
                .build();
    }

    private PickSkuLocation genLocation(String zone, int locId, int lotNum) {
        Random random = new Random();
        String lot = String.format("%08d", random.nextInt(lotNum) + 1);
        return PickSkuLocation.builder()
                .zone(zone)
                .loc("Z" + zone + "-L" + locId)
                .logicalLocation(String.format("%08d", locId + 1))
                .lot(lot)
                .build();
    }

    private SkuProperties genSkuProperties(int withExpDatePriority, SkuId skuId) {
        Instant expDate = null;
        String rotateBy = RotationType.BY_LOT.getValue();
        Random random = new Random();
        if (withExpDatePriority == 1 || (withExpDatePriority == 0 && skuId.hashCode() % 10 != 0)) {
            expDate = NOW.plus(random.nextInt(5) + 1, ChronoUnit.HOURS);
            rotateBy = RotationType.BY_EXPIRATION_DATE.getValue();
        }
        return SkuProperties.builder()
                .packKey("pack-" + random.nextInt())
                .lottable05(expDate)
                .rotateby(rotateBy)
                .build();
    }
}

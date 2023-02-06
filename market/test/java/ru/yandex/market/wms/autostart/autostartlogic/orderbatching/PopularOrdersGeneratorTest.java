package ru.yandex.market.wms.autostart.autostartlogic.orderbatching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.OrderItemOutOfStockException;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000001C4101001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.invROV0000000000000000002C4190002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData.sampleInventory;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001003;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000001004;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000002002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000003001;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdB000005002;
import static ru.yandex.market.wms.autostart.autostartlogic.OrderWithDetailsTestData.owdEmpty;

public class PopularOrdersGeneratorTest {
    @Test
    void shouldReturnCorrectListOfZones() {
        PopularOrdersGenerator generator = new PopularOrdersGenerator(
                listOf(owdB000002002(), owdB000001004(), owdB000001003(), owdB000005002(), owdB000003001()),
                sampleInventory());

        assertTrue(generator.getMostPopularZones(-1).isEmpty());
        assertTrue(generator.getMostPopularZones(0).isEmpty());
        assertEquals(listOf("FLOOR"), generator.getMostPopularZones(1));
        assertEquals(listOf("FLOOR", "MEZONIN_2"), generator.getMostPopularZones(2));
        assertEquals(listOf("FLOOR", "MEZONIN_2"), generator.getMostPopularZones(10));
    }

    @Test
    void wrongInventoryDataEmptyInventory() {
        assertThrows(OrderItemOutOfStockException.class,
                () -> new PopularOrdersGenerator(listOf(owdB000002002()), new ArrayList<>()));
    }

    @Test
    void wrongInventoryDataNoItemInInventory() {
        assertThrows(OrderItemOutOfStockException.class,
                () -> new PopularOrdersGenerator(listOf(owdB000002002()),
                        listOf(invROV0000000000000000001C4101001(1))));
    }

    @Test
    void wrongInventoryDataZeroItemsOfInventory() {
        assertThrows(OrderItemOutOfStockException.class,
                () -> new PopularOrdersGenerator(listOf(owdB000002002()),
                        listOf(invROV0000000000000000002C4190002(0))));
    }

    @Test
    void emptyOrderDetailEmptyInventory() {
        PopularOrdersGenerator generator =
                new PopularOrdersGenerator(listOf(owdEmpty()), new ArrayList<>());
        assertTrue(generator.getMostPopularZones(10).isEmpty());
        assertTrue(generator.getPopularZonesWithOrders().isEmpty());
        assertTrue(generator.getOrdersInZones(ImmutableSet.of("FLOOR")).isEmpty());
    }

    @Test
    void shouldReturnCorrectListOfOrdersByZones() {
        PopularOrdersGenerator generator = new PopularOrdersGenerator(
                listOf(owdB000002002(), owdB000001004(), owdB000001003(), owdB000005002(), owdB000003001()),
                sampleInventory());

        LinkedHashMap<String, List<OrderWithDetails>> zonesWithOrders = generator.getPopularZonesWithOrders();

        Set<String> actualOrders1 = zonesWithOrders.get("FLOOR").stream()
                .map(order -> order.getOrder().getOrderKey()).collect(Collectors.toSet());
        Set<String> actualOrders2 = zonesWithOrders.get("MEZONIN_2").stream()
                .map(order -> order.getOrder().getOrderKey()).collect(Collectors.toSet());
        assertEquals(ImmutableSet.of("000002002", "000001004", "000001003"), actualOrders1);
        assertEquals(ImmutableSet.of("000005002", "000003001", "000001003"), actualOrders2);
    }

    @Test
    void shouldIterateCorrectlyByZones() {
        PopularOrdersGenerator generator = new PopularOrdersGenerator(
                listOf(owdB000002002(), owdB000001004(), owdB000001003(), owdB000005002(), owdB000003001()),
                sampleInventory());

        List<OrderWithDetails> orders1 = generator.getOrdersInZones(ImmutableSet.of("FLOOR"));
        List<OrderWithDetails> orders2 = generator.getOrdersInZones(ImmutableSet.of("MEZONIN_2"));
        List<OrderWithDetails> orders1And2 = generator.getOrdersInZones(ImmutableSet.of("FLOOR", "MEZONIN_2"));

        assertEquals(ImmutableSet.of("000002002", "000001004", "000001003"),
                orders1.stream().map(o -> o.getOrder().getOrderKey()).collect(Collectors.toSet()));
        assertEquals(ImmutableSet.of("000005002", "000003001", "000001003"),
                orders2.stream().map(o -> o.getOrder().getOrderKey()).collect(Collectors.toSet()));
        assertEquals(ImmutableSet.of("000002002", "000001004", "000001003", "000005002", "000003001"),
                orders1And2.stream().map(o -> o.getOrder().getOrderKey()).collect(Collectors.toSet()));
    }
}

package ru.yandex.market.abo.clch.checker;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.abo.clch.model.SimpleDeliveryRegion;

/**
 * @author artemmz
 * @date 06.12.17.
 */
public class DeliveryRegionCheckerLoadTest extends EmptyTest {
    @Autowired
    private DeliveryRegionChecker deliveryRegionChecker;

    @Autowired
    private RegionService geoService;

    @BeforeEach
    public void setUp() throws Exception {
        deliveryRegionChecker.doConfigure();
    }

    @Test
    public void compare() {
        deliveryRegionChecker.compareData(initRegions(1, 5000), initRegions(2500, 10000));
    }

    private List<SimpleDeliveryRegion> initRegions(int fromId, int toId) {
        RegionTree regionTree = geoService.getRegionTree();
        return IntStream.range(fromId, toId)
                .mapToObj((IntFunction<Region>) regionTree::getRegion)
                .filter(Objects::nonNull)
                .map(reg -> new SimpleDeliveryRegion(reg.getId(), reg.getName(), true))
                .collect(Collectors.toList());

    }
}
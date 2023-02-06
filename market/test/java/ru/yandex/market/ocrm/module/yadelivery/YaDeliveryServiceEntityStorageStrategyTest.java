package ru.yandex.market.ocrm.module.yadelivery;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrder;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryService;
import ru.yandex.market.ocrm.module.yadelivery.test.YaDeliveryTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleYaDeliveryTestConfiguration.class)
public class YaDeliveryServiceEntityStorageStrategyTest {
    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private YaDeliveryTestUtils testUtils;
    @Inject
    private LomClient lomClient;

    private YaDeliveryOrder currentOrder;

    @BeforeEach
    void setUp() {
        currentOrder = testUtils.createYaDeliveryOrder();
    }

    @AfterEach
    void afterAll() {
        Mockito.reset(lomClient);
    }

    @Transactional
    @Test
    void getZeroItem() {
        List<Map<String, Object>> expectedMaps = testUtils.createYaDeliveryServicesMock(currentOrder, List.of());
        Assertions.assertTrue(expectedMaps.isEmpty());

        List<YaDeliveryService> actualItems = getItems(currentOrder);
        Assertions.assertTrue(actualItems.isEmpty());
    }

    @Transactional
    @Test
    void getOneItem() {
        List<Map<String, Object>> expectedMaps = testUtils.createYaDeliveryServicesMock(
                currentOrder,
                List.of(Map.of()));
        Assertions.assertEquals(1, expectedMaps.size());

        YaDeliveryService actualItem = getItems(currentOrder).stream().findAny().orElse(null);
        for (Map<String, Object> expectedMap : expectedMaps) {
            testUtils.equalsEntity(expectedMap, actualItem);
        }
    }

    @Transactional
    @Test
    void getTwoItems() {
        List<Map<String, Object>> expectedMaps = testUtils.createYaDeliveryServicesMock(
                currentOrder,
                List.of(Map.of(), Map.of()));
        Assertions.assertEquals(2, expectedMaps.size());

        List<YaDeliveryService> actualItems = getItems(currentOrder);
        for (int i = 0; i < 2; i++) {
            Map<String, Object> expectedMap = expectedMaps.get(i);
            YaDeliveryService actualItem = actualItems.get(i);
            testUtils.equalsEntity(expectedMap, actualItem);
        }
    }

    private List<YaDeliveryService> getItems(YaDeliveryOrder order) {
        return entityStorageService.list(Query.of(YaDeliveryService.FQN)
                .withFilters(Filters.eq(YaDeliveryService.PARENT, order)));
    }
}

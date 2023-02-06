package ru.yandex.market.ocrm.module.yadelivery;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.catalog.items.CatalogItemService;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.CargoStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryCargoStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrder;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrderStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrderStatusHistory;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliverySender;
import ru.yandex.market.ocrm.module.yadelivery.test.YaDeliveryTestUtils;

@SpringJUnitConfig(classes = ModuleYaDeliveryTestConfiguration.class)
@Transactional
public class YaDeliveryOrderStatusHistoryEntityStorageStrategyTest {
    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private CatalogItemService catalogItemService;
    @Inject
    private AttributeTypeService attributeTypeService;
    @Inject
    private YaDeliveryTestUtils testUtils;
    @Inject
    private LomClient lomClient;

    private YaDeliveryOrder currentOrder;

    @BeforeEach
    public void setUp() {
        YaDeliverySender orderSender = testUtils.createYaDeliverySender();
        Map<String, Object> orderProperties = Maps.of(
                YaDeliveryOrder.PLATFORM_CLIENT_ID, Randoms.positiveLongValue(),
                YaDeliveryOrder.SENDER, orderSender);
        currentOrder = testUtils.createYaDeliveryOrder(orderProperties);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(lomClient);
    }

    @Test
    void getWithOrderStatus() {
        String orderStatusName = Randoms.enumValue(OrderStatus.class).name();
        var orderStatus = catalogItemService.get(YaDeliveryOrderStatus.FQN, orderStatusName);
        Map<String, Object> expectedMap = testUtils.createYaDeliveryOrderStatusHistoryMock(
                currentOrder,
                Maps.of(YaDeliveryOrderStatusHistory.ORDER_STATUS, orderStatus,
                        YaDeliveryOrderStatusHistory.CARGO_STATUS, null));

        YaDeliveryOrderStatusHistory actualHistory = getOrderStatusHistory(currentOrder);
        testUtils.equalsEntity(expectedMap, actualHistory);
    }

    @Test
    void getWithCargoStatus() {
        String cargoStatusName = Randoms.enumValue(CargoStatus.class).name();
        var cargoStatus = catalogItemService.get(YaDeliveryCargoStatus.FQN, cargoStatusName);
        Map<String, Object> expectedMap = testUtils.createYaDeliveryOrderStatusHistoryMock(
                currentOrder,
                Maps.of(YaDeliveryOrderStatusHistory.ORDER_STATUS, null,
                        YaDeliveryOrderStatusHistory.CARGO_STATUS, cargoStatus));

        YaDeliveryOrderStatusHistory actualHistory = getOrderStatusHistory(currentOrder);
        testUtils.equalsEntity(expectedMap, actualHistory);
    }

    private YaDeliveryOrderStatusHistory getOrderStatusHistory(YaDeliveryOrder order) {
        return entityStorageService.<YaDeliveryOrderStatusHistory>list(Query.of(YaDeliveryOrderStatusHistory.FQN)
                .withFilters(Filters.eq(YaDeliveryOrderStatusHistory.PARENT, order)))
                .stream()
                .findAny()
                .orElse(null);
    }
}

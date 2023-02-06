package ru.yandex.market.wms.packing.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.pojo.OrderDetailKey;
import ru.yandex.market.wms.common.spring.service.NamedCounterService;
import ru.yandex.market.wms.packing.dao.PromoAllocationDao;
import ru.yandex.market.wms.packing.pojo.OrderLine;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackingBbxdServiceTest extends BaseTest {

    @Mock
    private PromoAllocationDao promoAllocationDao;
    @Mock
    private SerialInventoryDao serialInventoryDao;
    private SkuDaoImpl skuDao;
    @Mock
    private NamedCounterService namedCounterService;
    private PackingBbxdService packingBbxdService;

    @BeforeEach
    private void before() {
        skuDao = Mockito.mock(SkuDaoImpl.class);
        packingBbxdService = new PackingBbxdService(
                null, null, null, skuDao,
                null, null, null, null,
                null, null, null, null,
                serialInventoryDao, promoAllocationDao, namedCounterService, null);
    }

    @Test
    void getContainerItems() {
        SkuId sku1 = SkuId.of("3", "2");
        SkuId sku2 = SkuId.of("3", "3");
        String orderKey = "1";
        when(promoAllocationDao.getProcessableOrderLines(orderKey))
                .thenReturn(List.of(OrderLine.builder()
                                .lineKey(OrderDetailKey.builder()
                                        .orderKey(orderKey)
                                        .orderLineNumber("1")
                                        .build())
                                .skuId(sku2)
                                .build(),
                        OrderLine.builder()
                                .lineKey(OrderDetailKey.builder()
                                        .orderKey(orderKey)
                                        .orderLineNumber("2")
                                        .build())
                                .skuId(sku1)
                                .build()));

        when(serialInventoryDao.getById("BOX1"))
                .thenReturn(List.of(SerialInventory.builder()
                        .sku(sku1.getSku())
                        .storerKey(sku1.getStorerKey())
                        .serialNumber("1")
                        .build()));
        when(skuDao.findAll(anyCollection()))
                .thenReturn(Map.of(sku1, Sku.builder()
                        .sku(sku1.getSku())
                        .storerKey(sku1.getStorerKey())
                        .build(),
                        sku2, Sku.builder()
                                .sku(sku2.getSku())
                                .storerKey(sku2.getStorerKey())
                                .build()));

        List<PackingBbxdService.Item> result = packingBbxdService.getContainerItems(orderKey, "BOX1");

        assertions.assertThat(result).anyMatch(item -> {
            assertions.assertThat(item.getOrderDetailKey()).isEqualTo(OrderDetailKey.of(orderKey, "2"));
            assertions.assertThat(item.getSku().getSku()).isEqualTo(sku1.getSku());
            assertions.assertThat(item.getSku().getStorerKey()).isEqualTo(sku1.getStorerKey());
            return true;
        });
    }
}

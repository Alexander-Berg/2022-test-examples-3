package ru.yandex.market.wms.api.service.order;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.convert.converter.Converter;

import ru.yandex.market.wms.common.model.enums.IoFlag;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.converter.OrderDetailDTOConverter;
import ru.yandex.market.wms.common.spring.dao.entity.InstanceIdentity;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.BillOfMaterialDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDetailDTO;
import ru.yandex.market.wms.common.spring.pojo.OrderDetailKey;
import ru.yandex.market.wms.common.spring.pojo.SourceLineKey;
import ru.yandex.market.wms.common.spring.service.identities.ItemIdentityService;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderDetailServiceTest extends BaseTest {

    private OrderDetailDao orderDetailDao;
    private ItemIdentityService itemIdentityService;
    private DbConfigService configService;
    private BillOfMaterialDao billOfMaterialDao;
    private SkuDaoImpl skuDao;

    private OrderDetailService service;

    @BeforeEach
    void setUp() {
        orderDetailDao = mock(OrderDetailDao.class);
        itemIdentityService = mock(ItemIdentityService.class);
        Converter<OrderDetail, OrderDetailDTO> orderDetailDTOConverter = new OrderDetailDTOConverter();
        configService = mock(DbConfigService.class);
        billOfMaterialDao = mock(BillOfMaterialDao.class);
        skuDao = mock(SkuDaoImpl.class);
        service = new OrderDetailService(orderDetailDao, itemIdentityService, orderDetailDTOConverter, configService,
                billOfMaterialDao, skuDao);
        when(configService.getConfigAsBoolean("ENABLE_UITS_ON_GET_ORDER")).thenReturn(true);
        when(configService.getConfigAsBoolean("ORDER_CIS_FULL_ENABLED", false)).thenReturn(true);
    }

    @AfterEach
    void after() {
        Mockito.reset(skuDao, orderDetailDao, itemIdentityService, configService, billOfMaterialDao);
    }

    @Test
    //уит 456 один айтем
    void getOrderDetails_singleUit() {
        List<String> orderKeys = List.of("123");
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .sku("SKU")
                                .orderKey("123")
                                .orderLineNumber("1")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build());
        instances.put(OrderDetailKey.of("123", "1"), identities);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);

        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(1);
        List<Map<String, String>> result = orderDetails.get(0).getInstances();
        assertions.assertThat(result.size()).isEqualTo(1);
        assertions.assertThat(result).hasOnlyOneElementSatisfying(map -> {
            assertions.assertThat(map.size()).isEqualTo(1);
            assertions.assertThat(map).containsEntry("UIT", "456");
        });
    }

    @Test
    //уит 456 и киз 789 один айтем
    void getOrderDetails_uitsAndIdentities() {
        List<String> orderKeys = List.of("123");
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .sku("SKU")
                                .orderKey("123")
                                .orderLineNumber("1")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build(),
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .type("CIS")
                                .identity("0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                                        "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=")
                                .instance("456")
                                .build())
                        .build());
        instances.put(OrderDetailKey.of("123", "1"), identities);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);

        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(1);
        List<Map<String, String>> result = orderDetails.get(0).getInstances();
        assertions.assertThat(result).hasOnlyOneElementSatisfying(map -> {
            assertions.assertThat(map.size()).isEqualTo(2);
            assertions.assertThat(map).containsEntry("UIT", "456");
            assertions.assertThat(map).containsEntry("CIS_FULL", "0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                    "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=");
        });
    }

    @Test
    //уит 456 и имеи 789, имеи 123 один айтем
    //ожидаем один инстанс с уит 456 и имеи - 789, имеи2 - 123
    void getOrderDetails_uitsAndMultipleImei() {
        List<String> orderKeys = List.of("123");
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .sku("SKU")
                                .orderKey("123")
                                .orderLineNumber("1")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build(),
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .type("IMEI")
                                .identity("789")
                                .instance("456")
                                .build())
                        .build(),
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .type("IMEI")
                                .identity("123")
                                .instance("456")
                                .build())
                        .build());
        instances.put(OrderDetailKey.of("123", "1"), identities);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);

        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(1);
        List<Map<String, String>> result = orderDetails.get(0).getInstances();
        assertions.assertThat(result).hasOnlyOneElementSatisfying(map -> {
            assertions.assertThat(map.size()).isEqualTo(3);
            assertions.assertThat(map).containsEntry("UIT", "456");
            assertions.assertThat(map).containsEntry("IMEI", "789");
            assertions.assertThat(map).containsEntry("IMEI2", "123");
        });
    }

    @Test
    //уит 456, киз 789 и уит 123 два айтема
    //ожидаем один инстанс с уитом 456 и
    // второй с уитом 123 и кизом 789
    void getOrderDetails_uitsAndIdentities_multipleItems() {
        List<String> orderKeys = List.of("123");
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .sku("SKU")
                                .orderKey("123")
                                .orderLineNumber("1")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build(),
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .type("CIS")
                                .identity("0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                                        "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=")
                                .instance("456")
                                .build())
                        .build(),
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("123").build())
                        .noIdentities(true)
                        .build());
        instances.put(OrderDetailKey.of("123", "1"), identities);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);
        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        List<Map<String, String>> result = orderDetails.get(0).getInstances();
        assertions.assertThat(result.size()).isEqualTo(2);
        assertions.assertThat(result).anySatisfy(map -> {
            assertions.assertThat(map.size()).isEqualTo(2);
            assertions.assertThat(map).containsEntry("UIT", "456");
            assertions.assertThat(map).containsEntry("CIS_FULL", "0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                    "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=");
        });
        assertions.assertThat(result).anySatisfy(map -> {
            assertions.assertThat(map.size()).isEqualTo(1);
            assertions.assertThat(map).containsEntry("UIT", "123");
        });
    }

    @Test
    //уит 456, киз 789 и уит 123 два ску
    //ожидаем одну деталь с уитом 456 и
    // вторую с уитом 123 и кизом 789
    void getOrderDetails_uitsAndIdentities_differentSkus() {
        List<String> orderKeys = List.of("123");
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .orderKey("123")
                                .orderLineNumber("1")
                                .sku("SKU")
                                .storerKey("STORER")
                                .build(),
                        OrderDetail.builder()
                                .orderKey("123")
                                .orderLineNumber("2")
                                .sku("SKU1")
                                .storerKey("STORER1")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build(),
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .type("CIS")
                                .identity("0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                                        "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=")
                                .instance("456")
                                .build())
                        .build());
        List<InstanceIdentity> identities1 = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("123").build())
                        .noIdentities(true)
                        .build()
        );
        instances.put(OrderDetailKey.of("123", "1"), identities);
        instances.put(OrderDetailKey.of("123", "2"), identities1);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);
        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(2);
        assertions.assertThat(orderDetails).anySatisfy(dto -> {
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l.size()).isEqualTo(1);
            assertions.assertThat(l.get(0)).containsOnlyKeys("UIT", "CIS_FULL");
            assertions.assertThat(l.get(0)).containsValues("456", "0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                    "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=");
        });
        assertions.assertThat(orderDetails).anySatisfy(dto -> {
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l.size()).isEqualTo(1);
            assertions.assertThat(l.get(0)).containsOnlyKeys("UIT");
            assertions.assertThat(l.get(0)).containsValues("123");
        });
    }

    @Test
    //многомест
    //уит 456 - первая часть товава
    // уит 123 - вторая
    //ожидаем одну деталь с уитом 456 и
    // уитом 123
    void getOrderDetails_multiComponent() {
        List<String> orderKeys = List.of("123");
        SkuId skuId = SkuId.of("MASTER_STORER", "MASTER");
        when(skuDao.findAll(Set.of(skuId)))
                .thenReturn(Map.of(skuId, Sku.builder().manufacturerSku("MANUFACTURER").build()));
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .orderKey("123")
                                .orderLineNumber("1")
                                .sku("BOM1")
                                .storerKey("STORER")
                                .build(),
                        OrderDetail.builder()
                                .orderKey("123")
                                .orderLineNumber("2")
                                .sku("BOM2")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build());
        List<InstanceIdentity> identities1 = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("123").build())
                        .noIdentities(true)
                        .build()
        );
        instances.put(OrderDetailKey.of("123", "1"), identities);
        instances.put(OrderDetailKey.of("123", "2"), identities1);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);

        Map<SkuId, Map<SkuId, List<String>>> skuToBoms = Map.of(
                SkuId.of("MASTER_STORER", "MASTER"),
                Map.of(SkuId.of("STORER", "BOM1"), List.of("123"),
                        SkuId.of("STORER", "BOM2"), List.of("456"))
        );
        when(billOfMaterialDao.mapUitsToComponentSkus(anySet())).thenReturn(skuToBoms);
        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(1);
        assertions.assertThat(orderDetails).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getSku()).isEqualTo("MASTER");
            assertions.assertThat(dto.getStorerkey()).isEqualTo("MASTER_STORER");
            assertions.assertThat(dto.getAdjustedqty()).isEqualTo(BigDecimal.ZERO);
            assertions.assertThat(dto.getQtypicked()).isEqualTo(BigDecimal.ONE);
            assertions.assertThat(dto.getManufacturersku()).isEqualTo("MANUFACTURER");
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l).hasOnlyOneElementSatisfying(m -> {
                assertions.assertThat(m).containsOnlyKeys("UIT", "UIT2");
                assertions.assertThat(m).containsValues("123", "456");
            });
        });
    }

    @Test
    //многомест и обычный товар с кизом и уитом
    //уит 456 - первая часть товава
    // уит 123 - вторая
    //ожидаем две детали одну для многоместа с двумя уит
    //и одну для обычного товара с уит и киз
    void getOrderDetails_multiComponent_andCis() {
        List<String> orderKeys = List.of("123");
        SkuId skuId = SkuId.of("MASTER_STORER", "MASTER");
        when(skuDao.findAll(Set.of(skuId)))
                .thenReturn(Map.of(skuId, Sku.builder().manufacturerSku("MANUFACTURER").build()));
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .sku("BOM1")
                                .orderKey("123")
                                .orderLineNumber("1")
                                .storerKey("STORER")
                                .build(),
                        OrderDetail.builder()
                                .sku("BOM2")
                                .orderKey("123")
                                .orderLineNumber("2")
                                .storerKey("STORER")
                                .build(),
                        OrderDetail.builder()
                                .sku("SKU")
                                .orderKey("123")
                                .orderLineNumber("3")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build());
        List<InstanceIdentity> identities1 = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("123").build())
                        .noIdentities(true)
                        .build()
        );
        List<InstanceIdentity> identities2 = List.of(InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .type("CIS")
                        .identity("0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                                "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=")
                        .instance("987")
                        .build())
                .build());
        instances.put(OrderDetailKey.of("123", "1"), identities);
        instances.put(OrderDetailKey.of("123", "2"), identities1);
        instances.put(OrderDetailKey.of("123", "3"), identities2);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);

        Map<SkuId, Map<SkuId, List<String>>> skuToBoms = Map.of(
                skuId,
                Map.of(SkuId.of("STORER", "BOM1"), List.of("123"),
                        SkuId.of("STORER", "BOM2"), List.of("456"))
        );
        when(billOfMaterialDao.mapUitsToComponentSkus(anySet()))
                .thenReturn(skuToBoms);
        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(2);
        assertions.assertThat(orderDetails).anySatisfy(dto -> {
            assertions.assertThat(dto.getSku()).isEqualTo("MASTER");
            assertions.assertThat(dto.getStorerkey()).isEqualTo("MASTER_STORER");
            assertions.assertThat(dto.getAdjustedqty()).isEqualTo(BigDecimal.ZERO);
            assertions.assertThat(dto.getQtypicked()).isEqualTo(BigDecimal.ONE);
            assertions.assertThat(dto.getManufacturersku()).isEqualTo("MANUFACTURER");
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l).hasOnlyOneElementSatisfying(m -> {
                assertions.assertThat(m).containsOnlyKeys("UIT", "UIT2");
                assertions.assertThat(m).containsValues("123", "456");
            });
        });

        assertions.assertThat(orderDetails).anySatisfy(dto -> {
            assertions.assertThat(dto.getSku()).isEqualTo("SKU");
            assertions.assertThat(dto.getStorerkey()).isEqualTo("STORER");
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l).hasOnlyOneElementSatisfying(m -> {
                assertions.assertThat(m).containsOnlyKeys("UIT", "CIS_FULL");
                assertions.assertThat(m).containsValues("0104650194495494215+il=lIKPb(aH\u001D91EE07\u001D" +
                        "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=", "987");
            });
        });
    }

    @Test
    //уит 456, киз 789 и уит 123 два ску
    //ожидаем одну деталь с уитом 456 и
    // вторую с уитом 123 и кизом 789
    void getOrderDetails_uits_twoDetailsWithSameSkus() {
        List<String> orderKeys = List.of("123");
        when(orderDetailDao.findOrderDetailWithSkuAndBomByOrderKeys(orderKeys))
                .thenReturn(List.of(
                        OrderDetail.builder()
                                .orderKey("123")
                                .orderLineNumber("1")
                                .sku("SKU")
                                .storerKey("STORER")
                                .build(),
                        OrderDetail.builder()
                                .orderKey("123")
                                .orderLineNumber("2")
                                .sku("SKU")
                                .storerKey("STORER")
                                .build()));
        Map<SourceLineKey, List<InstanceIdentity>> instances = new HashMap<>();
        List<InstanceIdentity> identities = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("456").build())
                        .noIdentities(true)
                        .build());
        List<InstanceIdentity> identities1 = List.of(
                InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder().instance("123").build())
                        .noIdentities(true)
                        .build()
        );
        instances.put(OrderDetailKey.of("123", "1"), identities);
        instances.put(OrderDetailKey.of("123", "2"), identities1);
        when(itemIdentityService.getIdentitiesForUits(orderKeys, IoFlag.OUTBOUND, true))
                .thenReturn(instances);
        List<OrderDetailDTO> orderDetails = service.getOrderDetails(orderKeys);
        assertions.assertThat(orderDetails.size()).isEqualTo(2);
        assertions.assertThat(orderDetails).anySatisfy(dto -> {
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l.size()).isEqualTo(1);
            assertions.assertThat(l.get(0)).containsOnlyKeys("UIT");
            assertions.assertThat(l.get(0)).containsValues("456");
        });
        assertions.assertThat(orderDetails).anySatisfy(dto -> {
            List<Map<String, String>> l = dto.getInstances();
            assertions.assertThat(l.size()).isEqualTo(1);
            assertions.assertThat(l.get(0)).containsOnlyKeys("UIT");
            assertions.assertThat(l.get(0)).containsValues("123");
        });
    }


}

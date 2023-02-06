package ru.yandex.market.abo.web.controller.resupply.resupply;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.resupply.entity.ResupplyEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyStatus;
import ru.yandex.market.abo.core.resupply.entity.Warehouse;
import ru.yandex.market.abo.core.resupply.stock.ResupplyStock;
import ru.yandex.market.abo.cpa.order.CheckouterOrdersHelper;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class ResupplyItemsServiceTest {

    @Mock
    private CheckouterAPI checkouterClient;

    @Mock
    private CheckouterOrdersHelper checkouterOrdersHelper;

    @Mock
    private DeliveryParams deliveryParams;

    @InjectMocks
    private ResupplyItemsService resupplyItemsService;

    ResupplyItemEntity resupplyItemEntity;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        createEntities();
    }

    @Test
    public void getCargoTypesTest() {
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(buildMappingResponse());
        Set<Integer> cargoTypes = resupplyItemsService.getCargoTypes(resupplyItemEntity);
        assertEquals(2, cargoTypes.size());
        assertTrue(cargoTypes.contains(1));
        assertTrue(cargoTypes.contains(2));
    }

    @Test
    public void getCargoTypesMboResponseIsEmptyTest() {
        when(deliveryParams.searchFulfilmentSskuParams(any()))
                .thenReturn(MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder().build());
        Set<Integer> cargoTypes = resupplyItemsService.getCargoTypes(resupplyItemEntity);
        assertEquals(0, cargoTypes.size());
    }

    @Test
    public void getCargoTypesMboResponseErrorTest() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setCargoTypes(Set.of(1, 2));

        OrderItems orderItems = new OrderItems();
        orderItems.setContent(Collections.singletonList(orderItem));

        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(null);
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(orderItems);

        Set<Integer> cargoTypes = resupplyItemsService.getCargoTypes(resupplyItemEntity);
        assertEquals(2, cargoTypes.size());
        assertTrue(cargoTypes.contains(1));
        assertTrue(cargoTypes.contains(2));
    }

    @Test
    public void getCargoTypesMboResponseErrorAndCheckouterResponseIsNullTest() {
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(null);
        Set<Integer> cargoTypes = resupplyItemsService.getCargoTypes(resupplyItemEntity);
        assertEquals(0, cargoTypes.size());
    }

    private void createEntities() {
        ResupplyEntity resupplyEntity = new ResupplyEntity();
        resupplyEntity.setId(1L);
        resupplyEntity.setStatus(ResupplyStatus.DRAFT);
        resupplyEntity.setWarehouse(Warehouse.SOFINO);

        resupplyItemEntity = ResupplyItemEntity.builder()
                .id(1L)
                .orderId(1L)
                .orderItemId(1L)
                .supplierId(1L)
                .shopSku("12345")
                .marketSku(213L)
                .categoryId(1)
                .title("Title")
                .price(new BigDecimal(123))
                .createdAt(LocalDateTime.now())
                .resupplyStock(ResupplyStock.GOOD)
                .supplierTypeId(SupplierType.FIRST_PARTY.getId())
                .resupply(resupplyEntity)
                .build();
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildMappingResponse() {
        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType cargoType1 =
                MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                        .setId(1L)
                        .setName("CargoTypeName1")
                        .build();

        MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType cargoType2 =
                MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                        .setId(2L)
                        .setName("CargoTypeName2")
                        .build();

        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                .addFulfilmentInfo(
                        MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                                .setSupplierId(1)
                                .setShopSku("SHOPSKU1")
                                .setMarketSkuId(213L)
                                .addCargoTypes(cargoType1)
                                .addCargoTypes(cargoType2)
                                .build())
                .build();
    }
}

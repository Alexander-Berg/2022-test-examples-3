package ru.yandex.market.wms.api.service.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.wms.api.provider.OrderDetailProvider;
import ru.yandex.market.wms.api.provider.OrderDtoProvider;
import ru.yandex.market.wms.api.provider.OrderProvider;
import ru.yandex.market.wms.api.service.component.order.InterstoreOrderEnhancer;
import ru.yandex.market.wms.api.service.validation.order.OrderImportValidationService;
import ru.yandex.market.wms.common.dao.SkuDao;
import ru.yandex.market.wms.common.model.dto.OrderCheckpointDto;
import ru.yandex.market.wms.common.model.enums.OrderCheckpoint;
import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.converter.OrderDTOConverter;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.CartonDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ExternOrderKeyRegistryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDetailDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderPackDetailDTO;
import ru.yandex.market.wms.common.spring.exception.BadRequestException;
import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.common.spring.service.NamedCounterService;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private static final String YMA = "YMA";
    private static final String YMB = "YMB";
    private static final String YMC = "YMC";
    private static final String NONPACK = "NONPACK";
    private static final String STRETCH = "STRETCH";
    private static final Map<SkuId, Dimensions> ALL_DIMENSIONS = new HashMap<>();
    private static final Map<String, Carton> ALL_CARTONS = new HashMap<>();

    static {
        for (int i = 1; i <= 5; i++) {
            ALL_DIMENSIONS.put(skuId(i), dimensions(i));
        }
        Carton ymaCarton = Carton.builder().group("PK").type(YMA).length(21).width(16).height(10).volume(3360)
                .maxWeight(100).build();
        Carton ymbCarton = Carton.builder().group("PK").type(YMB).length(40).width(40).height(18).volume(28800)
                .maxWeight(100).build();
        Carton ymcCarton = Carton.builder().group("PK").type(YMC).length(31).width(22).height(12.5).volume(8525)
                .maxWeight(100).build();

        ALL_CARTONS.put(YMA, ymaCarton);
        ALL_CARTONS.put(YMB, ymbCarton);
        ALL_CARTONS.put(YMC, ymcCarton);
    }

    @Mock
    private PickDetailDao pickDetailDao;
    @Mock
    private OrderDetailService orderDetailService;
    @Mock
    private OrderDao orderDao;
    @Mock
    private OrderDTOConverter orderDTOConverter;
    @Mock
    private SkuDao skuDao;
    @Mock
    private CartonDao cartonDao;
    @Mock
    private OrderStatusHistoryDao orderStatusHistoryDao;
    @Mock
    private OrderDetailDao orderDetailDao;
    @Mock
    private ExternOrderKeyRegistryDao externOrderDao;
    @Mock
    private NamedCounterService namedCounterService;
    @Mock
    private DbConfigService configService;
    @Mock
    private InterstoreOrderEnhancer interstoreOrderEnhancer;
    @Mock
    private ImportOrderCommonService importOrderCommonService;
    @Mock
    private OrderImportValidationService orderImportValidationService;
    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(skuDao.getDimensions(any())).thenReturn(ALL_DIMENSIONS);
        when(cartonDao.getCartonsByType(any())).thenReturn(ALL_CARTONS);
        when(orderDTOConverter.convert(any(Order.class))).thenCallRealMethod();
    }

    private static SkuId skuId(int i) {
        return new SkuId("SK" + i, "SKU" + i);
    }

    private static Dimensions dimensions(int i) {
        int length = i * 11;
        int width = i * 7;
        int height = i * 5;
        int weight = i;
        int cube = length * width * height;
        return new Dimensions.DimensionsBuilder()
                .length(BigDecimal.valueOf(length))
                .width(BigDecimal.valueOf(width))
                .height(BigDecimal.valueOf(height))
                .weight(BigDecimal.valueOf(weight))
                .cube(BigDecimal.valueOf(cube))
                .build();
    }

    private Optional<Order> order(String orderKey, OrderStatus status) {
        return Optional.of(Order.builder()
                .orderKey(orderKey)
                .status(status.getValue())
                .build());
    }

    private Optional<Order> order(String orderKey, OrderStatus status, String externorderkey) {
        return Optional.of(Order.builder()
                .orderKey(orderKey)
                .type(OrderType.OUTBOUND_FIT.getCode())
                .externalOrderKey(externorderkey)
                .status(status.getValue())
                .build());
    }

    private static PickDetail pickDetail(int i, int qty, String caseId, String type) {
        SkuId skuId = skuId(i);
        return PickDetail.builder()
                .caseId(caseId)
                .selectedCartonType(type)
                .storerKey(skuId.getStorerKey())
                .sku(skuId.getSku())
                .qty(BigDecimal.valueOf(qty))
                .build();
    }

    private static OrderPackDetailDTO orderPackDetailDTO(String caseId,
                                                         Long weightGross,
                                                         Double width,
                                                         Double height,
                                                         Double length) {
        BigDecimal weightGrossBD;
        if (weightGross == null) {
            weightGrossBD = null;
        } else {
            weightGrossBD = BigDecimal.valueOf(weightGross);
        }

        return OrderPackDetailDTO.builder()
                .caseid(caseId)
                .weightGross(weightGrossBD)
                .width(width)
                .height(height)
                .length(length)
                .build();
    }

    @Test
    public void tesGetOrderByOrderKeyWhenNotExists() {
        Optional<OrderDTO> orderDTO = orderService.getOrderByOriginOrderKey("not-exist");
        assertTrue(orderDTO.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenNotPackedYetArgs")
    public void getOrderByOriginOrderKeyTestWhenNotPackedYet(String originOrderKey,
                                                             List<Order> orders,
                                                             OrderDTO expectedOrderDTO) {

        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);

        OrderDTO orderDTO = optionalOrderDTO.get();

        assertEquals(expectedOrderDTO, orderDTO);
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenCancelledStatusArgs")
    public void getOrderByOriginOrderKeyTestWhenCancelledStatus(String originOrderKey,
                                                                List<Order> orders,
                                                                OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);

        OrderDTO orderDTO = optionalOrderDTO.get();

        assertEquals(expectedOrderDTO, orderDTO);
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenPackedInSingleCartonArgs")
    public void getOrderByOriginOrderKeyTestWhenPackedInSingleCarton(String originOrderKey,
                                                                     List<String> orderKeys,
                                                                     String caseId,
                                                                     List<PickDetail> pickDetails,
                                                                     List<Order> orders,
                                                                     OrderDTO expectedOrderDTO,
                                                                     List<OrderDetailDTO> orderDetailDTOS) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderDetailService.getOrderDetails(orderKeys)).thenReturn(orderDetailDTOS);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);

        OrderDTO orderDTO = optionalOrderDTO.get();
        OrderPackDetailDTO pack = getPackAssertExists(orderDTO, caseId);

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertEquals(1, orderDTO.getOrderPacks().size()),
                // assert that selected size equals to YMA size
                () -> assertBoxSizeEquals(YMA, pack),
                // weight(i) = i*qty, so current weight = 1*1 + 2*10
                () -> assertEquals(BigDecimal.valueOf(21), pack.getWeightGross()),
                () -> assertEquals(expectedOrderDTO, orderDTO)
        );
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenPackedInMultipleCartonsArgs")
    public void getOrderByOriginOrderKeyTestWhenPackedInMultipleCartons(String originOrderKey,
                                                                        List<String> orderKeys,
                                                                        String caseId1,
                                                                        String caseId2,
                                                                        List<PickDetail> pickDetails,
                                                                        List<Order> orders,
                                                                        OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);

        OrderDTO orderDTO = optionalOrderDTO.get();
        OrderPackDetailDTO pack1 = getPackAssertExists(orderDTO, caseId1);
        OrderPackDetailDTO pack2 = getPackAssertExists(orderDTO, caseId2);

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertEquals(2, orderDTO.getOrderPacks().size()),
                // assert that selected size equals to YMA size
                () -> assertBoxSizeEquals(YMA, pack1),
                // weight(i) = i*qty, so current weight = 1*1 + 2*10
                () -> assertEquals(BigDecimal.valueOf(21), pack1.getWeightGross()),
                // assert that selected size equals to YMB size
                () -> assertBoxSizeEquals(YMB, pack2),
                // weight = i*qty, so current weight = 3*2 + 4*3
                () -> assertEquals(BigDecimal.valueOf(18), pack2.getWeightGross()),
                () -> assertEquals(expectedOrderDTO, orderDTO)
        );
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenSingleNonPackArgs")
    public void getOrderByOriginOrderKeyTestWhenSingleNonPack(String originOrderKey,
                                                              List<String> orderKeys,
                                                              String caseId,
                                                              List<PickDetail> pickDetails,
                                                              List<Order> orders,
                                                              OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);

        OrderDTO orderDTO = optionalOrderDTO.get();

        OrderPackDetailDTO pack = getPackAssertExists(orderDTO, caseId);

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertEquals(1, orderDTO.getOrderPacks().size()),
                // assert that selected size equals to dimensions of sku
                () -> assertDimensionsEquals(dimensions(5), pack),
                // weight(i) = i*qty, so current weight = 5*5
                () -> assertEquals(BigDecimal.valueOf(25), pack.getWeightGross()),
                () -> assertEquals(expectedOrderDTO, orderDTO)

        );
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenMultipleNonPacksArgs")
    public void getOrderByOriginOrderKeyTestWhenMultipleNonPacks(String originOrderKey,
                                                                 List<String> orderKeys,
                                                                 String caseId,
                                                                 List<PickDetail> pickDetails,
                                                                 List<Order> orders,
                                                                 OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);
        OrderDTO orderDTO = optionalOrderDTO.get();

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertFalse(orderDTO.getOrderPacks().isEmpty()),
                () -> assertNotNull(getPackAssertExists(orderDTO, caseId)),
                () -> assertEquals(expectedOrderDTO, orderDTO)
        );
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenMultipleStretchArgs")
    public void getOrderByOriginOrderKeyTestWhenMultipleStretch(String originOrderKey,
                                                                 List<String> orderKeys,
                                                                 String caseId,
                                                                 List<PickDetail> pickDetails,
                                                                 List<Order> orders,
                                                                 OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);
        OrderDTO orderDTO = optionalOrderDTO.get();

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertFalse(orderDTO.getOrderPacks().isEmpty()),
                () -> assertNotNull(getPackAssertExists(orderDTO, caseId)),
                () -> assertEquals(expectedOrderDTO, orderDTO)
        );
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenPackedAndNonPacked")
    public void getOrderByOriginOrderKeyTestWhenPackedAndNonPacked(String originOrderKey,
                                                                   List<String> orderKeys,
                                                                   String caseId,
                                                                   List<PickDetail> pickDetails,
                                                                   List<Order> orders,
                                                                   OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);
        OrderDTO orderDTO = optionalOrderDTO.get();
        OrderPackDetailDTO pack = getPackAssertExists(orderDTO, caseId);

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertEquals(1, orderDTO.getOrderPacks().size()),
                // in case of nonpack and packed with the same case id we use box dimensions (should not happen irl)
                () -> assertBoxSizeEquals(YMC, pack),
                // weight(i) = i*qty, so current weight = 4*4 + 5*5
                () -> assertEquals(BigDecimal.valueOf(41), pack.getWeightGross()),
                () -> assertEquals(expectedOrderDTO, orderDTO)

        );
    }

    @ParameterizedTest
    @MethodSource("getOrderByOriginOrderKeyTestWhenSelectedMultipleCartonTypesArgs")
    public void getOrderByOriginOrderKeyTestWhenSelectedMultipleCartonTypes(String originOrderKey,
                                                                            List<String> orderKeys,
                                                                            String caseId,
                                                                            List<PickDetail> pickDetails,
                                                                            List<Order> orders,
                                                                            OrderDTO expectedOrderDTO) {
        when(orderDao.findOrderByOriginOrderKey(originOrderKey)).thenReturn(orders);
        when(pickDetailDao.getPickDetailsForPackInfo(orderKeys)).thenReturn(pickDetails);
        when(orderDTOConverter.aggregateAndConvert(orders)).thenCallRealMethod();
        when(orderStatusHistoryDao.getCheckpointsByExternOrderKey(expectedOrderDTO.getExternorderkey()))
                .thenReturn(historyWithParcelCreated(expectedOrderDTO));

        Optional<OrderDTO> optionalOrderDTO = orderService.getOrderByOriginOrderKey(originOrderKey);

        OrderDTO orderDTO = optionalOrderDTO.get();
        OrderPackDetailDTO pack = getPackAssertExists(orderDTO, caseId);

        Assertions.assertAll(
                () -> assertNotNull(orderDTO.getOrderPacks()),
                () -> assertEquals(1, orderDTO.getOrderPacks().size()),
                // in case of multiple carton types under one caseId selecting the largest (should not happen irl)
                () -> assertTrue(ALL_CARTONS.get(YMB).getVolume() > ALL_CARTONS.get(YMC).getVolume()),
                () -> assertBoxSizeEquals(YMB, pack),
                // weight(i) = i*qty, so current weight = 1*2 + 3*4
                () -> assertEquals(BigDecimal.valueOf(14), pack.getWeightGross()),
                () -> assertEquals(expectedOrderDTO, orderDTO)
        );
    }

    @Test
    public void testPostOrderWhenExternOrderDaoNoExistKey() {
        String externorderkey = "EXTORDER10";
        String orderKey = "ORDER8";
        Optional<Order> optionalOrder = order(orderKey, OrderStatus.PACKED, externorderkey);
        Order order = optionalOrder.orElseThrow();
        OrderDTO originalOrderDTO = orderDTOConverter.convert(order);


        doNothing().when(externOrderDao).insert(originalOrderDTO.getExternorderkey());

        when(orderDao.selectOrderByExternalKey(any())).thenReturn(Optional.empty());
        doNothing().when(orderDao).updateOrderWithoutStatusTotalQtyGrossWgtCubeOrderlinesPalletestimate(any());
        doNothing().when(orderDao).deleteByKey(any());

        when(namedCounterService.getNextOrderKey()).thenReturn(orderKey);
        when(configService.getConfig(any())).thenReturn(null);

        doNothing().when(interstoreOrderEnhancer).setMaxAbsentItemsPricePercentForInterstore(any());
        doNothing().when(importOrderCommonService).prepareOrderForInsert(any());
        doNothing().when(orderImportValidationService).commonValidationBeforeInsert(any());

        orderService.postOrder(originalOrderDTO);
        verify(importOrderCommonService, times(1)).prepareOrderForInsert(any());
    }

    @Test
    public void testPostOrderWhenExternOrderDaoExistKey() {
        String externorderkey = "EXTORDER10";
        String orderKey = "ORDER8";
        Optional<Order> optionalOrder = order(orderKey, OrderStatus.PACKED, externorderkey);
        Order order = optionalOrder.orElseThrow();
        OrderDTO originalOrderDTO = orderDTOConverter.convert(order);


        doNothing().when(externOrderDao).insert(originalOrderDTO.getExternorderkey());

        when(orderDao.selectOrderByExternalKey(any())).thenReturn(Optional.empty());
        doNothing().when(orderDao).updateOrderWithoutStatusTotalQtyGrossWgtCubeOrderlinesPalletestimate(any());
        doNothing().when(orderDao).deleteByKey(any());

        when(namedCounterService.getNextOrderKey()).thenReturn(orderKey);
        when(configService.getConfig(any())).thenReturn(null);

        doNothing().when(interstoreOrderEnhancer).setMaxAbsentItemsPricePercentForInterstore(any());
        doNothing().when(importOrderCommonService).prepareOrderForInsert(any());
        doNothing().when(orderImportValidationService).commonValidationBeforeInsert(any());

        when(externOrderDao.exists(externorderkey)).thenReturn(true);
        orderService.postOrder(originalOrderDTO);
        verify(externOrderDao, times(0)).insert(anyString());
    }

    @ParameterizedTest
    @MethodSource("cancelOrderWhenOrderDoesNotExistArgs")
    void cancelOrderWhenOrderDoesNotExist(String externalKey) {
        when(orderDao.selectOrderDetailsByExternalOrderKey(externalKey))
                .thenReturn(Collections.emptyList());

        orderService.cancelOrder(externalKey);

        verify(orderDao, times(1)).selectOrderDetailsByExternalOrderKey(externalKey);
        verify(orderDao, times(0)).getCountFromPickDetailByKey(any(List.class));
        verify(orderDao, times(0)).getCancelableStatuses(any());
        verify(orderDao, times(0)).getOrderTypeByOrderKey(any());
        verify(orderDao, times(0)).cancelOrdersByKey(any());

        verify(orderImportValidationService, times(0))
                .checkOrderBeforeCancel(any(int.class), any(boolean.class));

        verify(orderStatusHistoryDao, times(0))
                .createOrderHistory(any(String.class), any(), any());
        verify(orderStatusHistoryDao, times(0))
                .createOrderHistory(any(List.class), any(), any());

        verify(orderDetailDao, times(0)).cancelOrder(any(OrderDetail.class));
    }

    @ParameterizedTest
    @MethodSource("cancelOrderWhenPickDetailsExistArgs")
    void cancelOrderWhenPickDetailsExist(String externalKey,
                                         List<OrderDetail> orderDetails,
                                         List<String> orderKeys,
                                         List<String> cancelableStatuses,
                                         String expectedErrorMessage,
                                         int expectedPickDetail,
                                         boolean expectedOrderCancelable) {
        when(orderDao.selectOrderDetailsByExternalOrderKey(externalKey)).thenReturn(orderDetails);
        when(orderDao.getCountFromPickDetailByKey(orderKeys)).thenReturn(expectedPickDetail);
        when(orderDao.getCancelableStatuses(orderKeys)).thenReturn(cancelableStatuses);
        doCallRealMethod().when(orderImportValidationService)
                .checkOrderBeforeCancel(any(int.class), any(boolean.class));

        BadRequestException badRequestException =
                Assertions.assertThrows(BadRequestException.class, () -> orderService.cancelOrder(externalKey));

        Assertions.assertEquals(expectedErrorMessage, badRequestException.getMessage());

        verify(orderDao, times(1)).selectOrderDetailsByExternalOrderKey(externalKey);
        verify(orderDao, times(1)).getCountFromPickDetailByKey(orderKeys);
        verify(orderDao, times(1)).getCancelableStatuses(orderKeys);
        verify(orderDao, times(0)).getOrderTypeByOrderKey(any());
        verify(orderDao, times(0)).cancelOrdersByKey(any());

        verify(orderImportValidationService, times(1))
                .checkOrderBeforeCancel(expectedPickDetail, expectedOrderCancelable);

        verify(orderStatusHistoryDao, times(0))
                .createOrderHistory(any(String.class), any(), any());
        verify(orderStatusHistoryDao, times(0))
                .createOrderHistory(any(List.class), any(), any());

        verify(orderDetailDao, times(0)).cancelOrder(any(OrderDetail.class));
    }

    @ParameterizedTest
    @MethodSource("cancelOrderWhenOrderIsNotCancelableArgs")
    void cancelOrderWhenOrderIsNotCancelable(String externalKey,
                                             List<OrderDetail> orderDetails,
                                             List<String> orderKeys,
                                             List<String> cancelableStatuses,
                                             String expectedErrorMessage,
                                             int expectedPickDetail,
                                             boolean expectedOrderCancelable) {
        when(orderDao.selectOrderDetailsByExternalOrderKey(externalKey)).thenReturn(orderDetails);
        when(orderDao.getCountFromPickDetailByKey(orderKeys)).thenReturn(expectedPickDetail);
        when(orderDao.getCancelableStatuses(orderKeys)).thenReturn(cancelableStatuses);
        doCallRealMethod().when(orderImportValidationService)
                .checkOrderBeforeCancel(any(int.class), any(boolean.class));

        BadRequestException badRequestException =
                Assertions.assertThrows(BadRequestException.class, () -> orderService.cancelOrder(externalKey));

        Assertions.assertEquals(expectedErrorMessage, badRequestException.getMessage());

        verify(orderDao, times(1)).selectOrderDetailsByExternalOrderKey(externalKey);
        verify(orderDao, times(1)).getCountFromPickDetailByKey(orderKeys);
        verify(orderDao, times(1)).getCancelableStatuses(orderKeys);
        verify(orderDao, times(0)).getOrderTypeByOrderKey(any());
        verify(orderDao, times(0)).cancelOrdersByKey(any());

        verify(orderImportValidationService, times(1))
                .checkOrderBeforeCancel(expectedPickDetail, expectedOrderCancelable);

        verify(orderStatusHistoryDao, times(0))
                .createOrderHistory(any(String.class), any(), any());
        verify(orderStatusHistoryDao, times(0))
                .createOrderHistory(any(List.class), any(), any());

        verify(orderDetailDao, times(0)).cancelOrder(any(OrderDetail.class));
    }

    @ParameterizedTest
    @MethodSource("cancelOrderArgs")
    void cancelOrder(String externalKey,
                     String orderType,
                     List<OrderDetail> orderDetails,
                     List<String> orderKeys,
                     List<String> cancelableStatuses,
                     int expectedPickDetail,
                     boolean expectedOrderCancelable) {
        when(orderDao.selectOrderDetailsByExternalOrderKey(externalKey)).thenReturn(orderDetails);
        when(orderDao.getCountFromPickDetailByKey(orderKeys)).thenReturn(expectedPickDetail);
        when(orderDao.getCancelableStatuses(orderKeys)).thenReturn(cancelableStatuses);
        doCallRealMethod().when(orderImportValidationService)
                .checkOrderBeforeCancel(any(int.class), any(boolean.class));
        when(orderDao.getOrderTypeByOrderKey(orderDetails.get(0).getOrderKey())).thenReturn(orderType);

        orderService.cancelOrder(externalKey);

        verify(orderDao, times(1)).selectOrderDetailsByExternalOrderKey(externalKey);
        verify(orderDao, times(1)).getCountFromPickDetailByKey(orderKeys);
        verify(orderDao, times(1)).getCancelableStatuses(orderKeys);
        verify(orderDao, times(1)).getOrderTypeByOrderKey(orderDetails.get(0).getOrderKey());
        verify(orderDao, times(1)).cancelOrdersByKey(orderKeys);

        verify(orderImportValidationService, times(1))
                .checkOrderBeforeCancel(expectedPickDetail, expectedOrderCancelable);

        verify(orderStatusHistoryDao, times(1))
                .createOrderHistory(orderKeys, " ", orderType);
        verify(orderStatusHistoryDao, times(2))
                .createOrderHistory(anyString(), any(String.class), anyString());

        verify(orderDetailDao, times(2)).cancelOrder(any(OrderDetail.class));
    }

    private OrderPackDetailDTO getPackAssertExists(OrderDTO orderDTO, String caseId) {
        Optional<OrderPackDetailDTO> packOptional = orderDTO.getOrderPacks().stream()
                .filter(pack -> pack.getCaseid().equals(caseId)).findAny();
        assertTrue(packOptional.isPresent());
        return packOptional.get();
    }

    private void assertBoxSizeEquals(String cartonType, OrderPackDetailDTO pack) {
        assertEquals(ALL_CARTONS.get(cartonType).getLength(), pack.getLength(), 10e-1);
        assertEquals(ALL_CARTONS.get(cartonType).getWidth(), pack.getWidth(), 10e-1);
        assertEquals(ALL_CARTONS.get(cartonType).getHeight(), pack.getHeight(), 10e-1);
    }

    private void assertDimensionsEquals(Dimensions dimensions, OrderPackDetailDTO pack) {
        assertEquals(dimensions.getLength().doubleValue(), pack.getLength(), 10e-1);
        assertEquals(dimensions.getWidth().doubleValue(), pack.getWidth(), 10e-1);
        assertEquals(dimensions.getHeight().doubleValue(), pack.getHeight(), 10e-1);
    }

    private static Stream<Arguments> cancelOrderWhenOrderDoesNotExistArgs() {
        return Stream.of(
                Arguments.of(
                        "outbound-1626852630004029"
                )
        );
    }

    private static Stream<Arguments> cancelOrderWhenPickDetailsExistArgs() {
        String expectedErrorMessage = "400 BAD_REQUEST \"Cannot cancel this Order. " +
                "The Order has Pick Details which are \'In Progress\' " +
                "and/or \'Picked\' and which may not be deleted\"";

        return Stream.of(
                Arguments.of(
                        "outbound-1626852630004029",
                        OrderDetailProvider.getOrderDetailsForSuperWarehouse(),
                        asList("A000000654", "B000000654"),
                        asList("1", "1"),
                        expectedErrorMessage,
                        1,
                        true
                ),
                Arguments.of(
                        "outbound-1626852630004030",
                        OrderDetailProvider.getOrderDetailsForOrdinaryWarehouse(),
                        Collections.singletonList("0000000654"),
                        Collections.singletonList("1"),
                        expectedErrorMessage,
                        1,
                        true
                )
        );
    }

    private static Stream<Arguments> cancelOrderWhenOrderIsNotCancelableArgs() {
        String expectedErrorMessage = "400 BAD_REQUEST \"Order is not cancelable.\"";

        return Stream.of(
                Arguments.of(
                        "outbound-1626852630004029",
                        OrderDetailProvider.getOrderDetailsForSuperWarehouse(),
                        asList("A000000654", "B000000654"),
                        asList("1", "0"),
                        expectedErrorMessage,
                        0,
                        false
                ),
                Arguments.of(
                        "outbound-1626852630004029",
                        OrderDetailProvider.getOrderDetailsForSuperWarehouse(),
                        asList("A000000654", "B000000654"),
                        asList("0", "0"),
                        expectedErrorMessage,
                        0,
                        false
                ),
                Arguments.of(
                        "outbound-1626852630004030",
                        OrderDetailProvider.getOrderDetailsForOrdinaryWarehouse(),
                        Collections.singletonList("0000000654"),
                        Collections.singletonList("0"),
                        expectedErrorMessage,
                        0,
                        false
                )
        );
    }

    private static Stream<Arguments> cancelOrderArgs() {
        return Stream.of(
                Arguments.of(
                        "outbound-1626852630004029",
                        "91",
                        OrderDetailProvider.getOrderDetailsForSuperWarehouse(),
                        asList("A000000654", "B000000654"),
                        asList("1", "1"),
                        0,
                        true
                ),
                Arguments.of(
                        "outbound-1626852630004030",
                        "91",
                        OrderDetailProvider.getOrderDetailsForOrdinaryWarehouse(),
                        Collections.singletonList("0000000654"),
                        Collections.singletonList("1"),
                        0,
                        true
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenMultipleNonPacksArgs() {
        String caseId1 = "P00061";
        String caseId2 = "P00062";
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER1";
        String orderKey2 = "ORDER2";
        String orderKey3 = "ORDER3";
        List<OrderPackDetailDTO> orderPackDetailDTOS1 = Collections.singletonList(
                orderPackDetailDTO(caseId1, 14L, null, null, null)
        );
        List<OrderPackDetailDTO> orderPackDetailDTOS2 = Collections.singletonList(
                orderPackDetailDTO(caseId2, 14L, null, null, null)
        );

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        asList(
                                pickDetail(4, 1, caseId1, NONPACK),
                                pickDetail(5, 2, caseId1, NONPACK)
                        ),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS1,
                                OrderStatus.PACKED.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId2,
                        asList(
                                pickDetail(4, 1, caseId2, NONPACK),
                                pickDetail(5, 2, caseId2, NONPACK)
                        ),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS2,
                                OrderStatus.PACKED.getValue(), 2)
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenMultipleStretchArgs() {
        String caseId1 = "P00061";
        String caseId2 = "P00062";
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER1";
        String orderKey2 = "ORDER2";
        String orderKey3 = "ORDER3";
        List<OrderPackDetailDTO> orderPackDetailDTOS1 = Collections.singletonList(
                orderPackDetailDTO(caseId1, 14L, null, null, null)
        );
        List<OrderPackDetailDTO> orderPackDetailDTOS2 = Collections.singletonList(
                orderPackDetailDTO(caseId2, 14L, null, null, null)
        );

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        asList(
                                pickDetail(4, 1, caseId1, STRETCH),
                                pickDetail(5, 2, caseId1, STRETCH)
                        ),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS1,
                                OrderStatus.PACKED.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId2,
                        asList(
                                pickDetail(4, 1, caseId2, STRETCH),
                                pickDetail(5, 2, caseId2, STRETCH)
                        ),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS2,
                                OrderStatus.PACKED.getValue(), 2)
                )
        );
    }


    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenSingleNonPackArgs() {
        String caseId1 = "P00051";
        String caseId2 = "P00052";
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER5";
        String orderKey2 = "ORDER4";
        String orderKey3 = "ORDER3";
        List<OrderPackDetailDTO> orderPackDetailDTOS1 = Collections.singletonList(
                orderPackDetailDTO(caseId1, 25L, 35.0, 25.0, 55.0)
        );
        List<OrderPackDetailDTO> orderPackDetailDTOS2 = Collections.singletonList(
                orderPackDetailDTO(caseId2, 25L, 35.0, 25.0, 55.0)
        );

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        Collections.singletonList(pickDetail(5, 5, caseId1, NONPACK)),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS1,
                                OrderStatus.PACKED.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId2,
                        Collections.singletonList(pickDetail(5, 5, caseId2, NONPACK)),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS2,
                                OrderStatus.PACKED.getValue(), 2)
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenSelectedMultipleCartonTypesArgs() {
        String caseId1 = "P00081";
        String caseId2 = "P00082";
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER8";
        String orderKey2 = "ORDER9";
        String orderKey3 = "ORDER10";
        List<OrderPackDetailDTO> orderPackDetailDTOS1 = Collections.singletonList(
                orderPackDetailDTO(caseId1, 14L, 40.0, 18.0, 40.0)
        );
        List<OrderPackDetailDTO> orderPackDetailDTOS2 = Collections.singletonList(
                orderPackDetailDTO(caseId2, 14L, 40.0, 18.0, 40.0)
        );

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        asList(
                                pickDetail(1, 2, caseId1, YMB),
                                pickDetail(3, 4, caseId1, YMC)
                        ),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS1,
                                OrderStatus.PACKED.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId2,
                        asList(
                                pickDetail(1, 2, caseId2, YMB),
                                pickDetail(3, 4, caseId2, YMC)
                        ),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS2,
                                OrderStatus.PACKED.getValue(), 2)
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenPackedAndNonPacked() {
        String caseId1 = "P00071";
        String caseId2 = "P00072";
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER6";
        String orderKey2 = "ORDER5";
        String orderKey3 = "ORDER4";
        List<OrderPackDetailDTO> orderPackDetailDTOS1 = Collections.singletonList(
                orderPackDetailDTO(caseId1, 41L, 22.0, 12.5, 31.0)
        );
        List<OrderPackDetailDTO> orderPackDetailDTOS2 = Collections.singletonList(
                orderPackDetailDTO(caseId2, 41L, 22.0, 12.5, 31.0)
        );

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        asList(
                                pickDetail(4, 4, caseId1, YMC),
                                pickDetail(5, 5, caseId1, NONPACK)
                        ),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS1,
                                OrderStatus.PACKED.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId2,
                        asList(
                                pickDetail(4, 4, caseId2, YMC),
                                pickDetail(5, 5, caseId2, NONPACK)
                        ),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS2,
                                OrderStatus.PACKED.getValue(), 2)
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenPackedInMultipleCartonsArgs() {
        String caseId1 = "P00041";
        String caseId2 = "P00042";

        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER4";
        String orderKey2 = "ORDER5";
        String orderKey3 = "ORDER3";
        List<OrderPackDetailDTO> orderPackDetailDTOS = asList(
                orderPackDetailDTO(caseId2, 18L, 40.0, 18.0, 40.0),
                orderPackDetailDTO(caseId1, 21L, 16.0, 10.0, 21.0)
        );

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        caseId2,
                        asList(
                                pickDetail(1, 1, caseId1, YMA),
                                pickDetail(2, 10, caseId1, YMA),
                                pickDetail(3, 2, caseId2, YMB),
                                pickDetail(4, 3, caseId2, YMB)
                        ),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS,
                                OrderStatus.PACKED.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId1,
                        caseId2,
                        asList(
                                pickDetail(1, 1, caseId1, YMA),
                                pickDetail(2, 10, caseId1, YMA),
                                pickDetail(3, 2, caseId2, YMB),
                                pickDetail(4, 3, caseId2, YMB)
                        ),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS,
                                OrderStatus.PACKED.getValue(), 2)
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenPackedInSingleCartonArgs() {
        String caseId1 = "P00071";
        String caseId2 = "P00072";
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER3";
        String orderKey2 = "ORDER4";
        String orderKey3 = "ORDER5";
        List<OrderPackDetailDTO> orderPackDetailDTOS1 = Collections.singletonList(
                orderPackDetailDTO(caseId1, 21L, 16.0, 10.0, 21.0)
        );

        List<OrderPackDetailDTO> orderPackDetailDTOS2 = Collections.singletonList(
                orderPackDetailDTO(caseId2, 21L, 16.0, 10.0, 21.0)
        );

        List<OrderDetailDTO> orderDetailDTO1 = Collections.singletonList(OrderDetailDTO.builder().build());
        List<OrderDetailDTO> orderDetailDTO2 = Collections.singletonList(OrderDetailDTO.builder().build());

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(orderKey1),
                        caseId1,
                        asList(
                                pickDetail(1, 1, caseId1, YMA),
                                pickDetail(2, 10, caseId1, YMA)
                        ),
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, orderPackDetailDTOS1,
                                OrderStatus.PACKED.getValue(), 1, orderDetailDTO1),
                        orderDetailDTO1
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(orderKey2, orderKey3),
                        caseId2,
                        asList(
                                pickDetail(1, 1, caseId2, YMA),
                                pickDetail(2, 10, caseId2, YMA)
                        ),
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PACKED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.PACKED.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, orderPackDetailDTOS2,
                                OrderStatus.PACKED.getValue(), 2, orderDetailDTO2),
                        orderDetailDTO2
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenNotPackedYetArgs() {
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER3";
        String orderKey2 = "ORDER4";
        String orderKey3 = "ORDER5";

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);

        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.PICKED_COMPLETE.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, Collections.emptyList(),
                                OrderStatus.PICKED_COMPLETE.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.PART_RELEASED_PART_SHIPPED.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.CREATED_EXTERNALLY.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, Collections.emptyList(),
                                OrderStatus.CREATED_EXTERNALLY.getValue(), 2)
                )
        );
    }

    private static Stream<Arguments> getOrderByOriginOrderKeyTestWhenCancelledStatusArgs() {
        String originOrderKey1 = "ORDER6";
        String originOrderKey2 = "ORDER7";
        String orderKey1 = "ORDER3";
        String orderKey2 = "ORDER4";
        String orderKey3 = "ORDER5";

        OffsetDateTime scheduledShipDate = OffsetDateTime
                .parse("2016-10-02T20:15:30+01:00", DateTimeFormatter.ISO_DATE_TIME);
        return Stream.of(
                Arguments.of(
                        originOrderKey1,
                        Collections.singletonList(
                                OrderProvider.getOrder(originOrderKey1, orderKey1, scheduledShipDate,
                                        OrderStatus.CANCELLED_EXTERNALLY.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey1, scheduledShipDate, Collections.emptyList(),
                                OrderStatus.CANCELLED_EXTERNALLY.getValue(), 1)
                ),
                Arguments.of(
                        originOrderKey2,
                        Arrays.asList(
                                OrderProvider.getOrder(originOrderKey2, orderKey2, scheduledShipDate,
                                        OrderStatus.WAITING_FOR_DETAILS.getValue()),
                                OrderProvider.getOrder(originOrderKey2, orderKey3, scheduledShipDate,
                                        OrderStatus.RETURN_ORDER_PARTIALLY_RECEIPT_AT_SECONDARY_RECEPTION.getValue())
                        ),
                        OrderDtoProvider.getOrderDTO(originOrderKey2, scheduledShipDate, Collections.emptyList(),
                                OrderStatus.WAITING_FOR_DETAILS.getValue(), 2)
                )
        );
    }

    private List<OrderCheckpointDto> historyWithParcelCreated(OrderDTO order) {
        return List.of(OrderCheckpointDto.builder()
                .orderKey(order.getOrderkey())
                .originOrderKey(order.getOriginorderkey())
                .externOrderKey(order.getExternorderkey())
                .checkpoint(OrderCheckpoint.PARCEL_CREATED)
                .addDate(Instant.now())
                .build());
    }

}

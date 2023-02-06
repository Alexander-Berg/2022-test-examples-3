package ru.yandex.market.abo.core.resupply.registry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.util.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.collections.Either;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.resupply.entity.Warehouse;
import ru.yandex.market.abo.core.resupply.registry.checkouter.OrderFetchingError;
import ru.yandex.market.abo.core.resupply.registry.checkouter.RegistryCheckouterService;
import ru.yandex.market.abo.core.resupply.registry.exception.NoBoxesInRegistryException;
import ru.yandex.market.abo.cpa.lms.DeliveryServiceManager;
import ru.yandex.market.abo.cpa.lms.model.DeliveryService;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.CreateRegistryDTO;
import ru.yandex.market.ff.client.dto.PutSupplyRequestWithInboundRegisterDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.enums.LogisticUnitType;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class RegistrySendingServiceTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private RegistryRepo registryRepo;

    @Mock
    private RegistryItemRepo registryItemRepo;

    @Mock
    private RegistryCheckouterService checkouterService;

    @Mock
    private FulfillmentWorkflowClientApi ffClient;

    @Mock
    private DeliveryServiceManager deliveryServiceManager;

    @Mock
    private LMSClient lmsClient;

    @InjectMocks
    private RegistrySendingService registrySendingService;

    @Captor
    ArgumentCaptor<CreateRegistryDTO> createRegistryDTOArgumentCaptor;

    @Captor
    ArgumentCaptor<PutSupplyRequestWithInboundRegisterDTO> putSupplyRequestDTOArgumentCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void sendToFfWithoutErrors() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRegistryRequest(any(CreateRegistryDTO.class))).thenReturn(shopRequestDTO);

        long shopRequestId = registrySendingService.sendAfterApproval(r);
        assertEquals(12L, shopRequestId);
    }

    @Test
    public void sendToFfUpdatableCustomerReturnWithoutErrors() throws Exception {
        when(deliveryServiceManager.isUpdatableCustomerReturn(any())).thenReturn(true);

        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMockAndManyBox(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.REFUND);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(1L))
                .type(null)
                .active(true)
                .build())).thenReturn(List.of(LogisticsPointResponse.newBuilder().id(1L).build()));
        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);
        registrySendingService.sendAfterApproval(r);
        Mockito.verify(ffClient).createRequestAndPutRegistry(putSupplyRequestDTOArgumentCaptor.capture());
        PutSupplyRequestWithInboundRegisterDTO value = putSupplyRequestDTOArgumentCaptor.getValue();
        assertEquals(value.getType(), 1210);
    }

    @Test
    public void sendToFfInboundRegistryWithoutErrors() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.REFUND);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(1L))
                .type(null)
                .active(true)
                .build())).thenReturn(List.of(LogisticsPointResponse.newBuilder().id(1L).build()));
        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);
        registrySendingService.sendAfterApproval(r);
        Mockito.verify(ffClient).createRequestAndPutRegistry(putSupplyRequestDTOArgumentCaptor.capture());
        PutSupplyRequestWithInboundRegisterDTO value = putSupplyRequestDTOArgumentCaptor.getValue();
        assertEquals(value.getInboundRegistry().getBoxes().size(), 1);
        assertEquals(value.getInboundRegistry()
                .getBoxes()
                .get(0)
                .getUnitInfo()
                .getCompositeId()
                .getPartialIds()
                .get(0)
                .getValue(), "two");
    }


    @Test
    public void sendToFfInboundRegistryWithErrorOfReturnId() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMockWithOldReturnId(true, 123123);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.REFUND);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);
        Assertions.assertThrows(IllegalArgumentException.class, () ->         registrySendingService.sendAfterApproval(r));
    }


    @Test
    public void sendToFfInboundRegistryWithoutErrorOfReturnId() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMockWithOldReturnId(true, 1439469);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.REFUND);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);
    }

    @Test
    public void sendToFfInboundRegistryWithoutErrorsAndManyBoxes() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMockAndManyBox(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.REFUND);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(1L))
                .type(null)
                .active(true)
                .build())).thenReturn(List.of(LogisticsPointResponse.newBuilder().id(1L).build()));
        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);
        registrySendingService.sendAfterApproval(r);
        Mockito.verify(ffClient).createRequestAndPutRegistry(putSupplyRequestDTOArgumentCaptor.capture());
        PutSupplyRequestWithInboundRegisterDTO value = putSupplyRequestDTOArgumentCaptor.getValue();
        assertEquals(value.getInboundRegistry().getBoxes().size(), 3);
        assertEquals(value.getInboundRegistry()
                .getBoxes()
                .get(1)
                .getUnitInfo()
                .getCompositeId()
                .getPartialIds()
                .get(0)
                .getValue(), "three");
    }

    @Test
    public void sendToFfInboundRegistryWithoutErrorsAndNoBoxes() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMockAndManyBox(false);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.REFUND);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(1L))
                .type(null)
                .active(true)
                .build())).thenReturn(List.of(LogisticsPointResponse.newBuilder().id(1L).build()));
        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);

        assertThrows(NoBoxesInRegistryException.class, () -> {registrySendingService.sendAfterApproval(r);});
    }

    @Test
    public void notSendToFfIfHavingCheckouterTechnicalError() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.TECHNICAL_ERROR)
                ));
        when(ffClient.createRegistryRequest(any(CreateRegistryDTO.class))).thenReturn(shopRequestDTO);

        Assertions.assertThrows(IllegalStateException.class, () -> registrySendingService.sendAfterApproval(r),
                "При загрузке поставки произошла техническая ошибка. " +
                        "Попробуйте загрузить поставку еще раз через некоторое время"
        );
    }

    @Test
    public void partialReturnHappyPass() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order)));
        when(ffClient.createRegistryRequest(any(CreateRegistryDTO.class))).thenReturn(shopRequestDTO);
        when(checkouterService.processPartialReturnOrders(any())).thenReturn(
                Collections.singletonList(new Pair<>(order, order.getItems())));

        registrySendingService.sendAfterApproval(r);

        Mockito.verify(ffClient).createRegistryRequest(createRegistryDTOArgumentCaptor.capture());
        Assert.isNonEmpty(createRegistryDTOArgumentCaptor.getValue().getLogisticUnits());
        Assertions.assertEquals(2, createRegistryDTOArgumentCaptor.getValue().getLogisticUnits().size());
        Assertions.assertTrue(createRegistryDTOArgumentCaptor.getValue().getLogisticUnits().stream()
                .anyMatch(lu -> lu.getType().equals(
                        LogisticUnitType.BOX)));
        Assertions.assertTrue(createRegistryDTOArgumentCaptor.getValue().getLogisticUnits().stream()
                .anyMatch(lu -> lu.getType().equals(
                        LogisticUnitType.ITEM)));
    }

    @Test
    public void partialReturnHappyAndFullReturnPass() throws Exception {
        HashMap<Registry, List<Order>> orderAndRegistry = initSendToFfFashionAndNotFashionOrderWithoutCheckouterMock(
                true);

        Registry r = orderAndRegistry.keySet().stream().findAny().get();

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(orderAndRegistry.get(r).get(0)), "3960111",
                        Either.right(orderAndRegistry.get(r).get(1))
                ));
        when(ffClient.createRegistryRequest(any(CreateRegistryDTO.class))).thenReturn(shopRequestDTO);
        when(checkouterService.processPartialReturnOrders(any())).thenReturn(
                orderAndRegistry.get(r).stream().map(order -> Pair.of(order, order.getItems()))
                        .collect(Collectors.toList()));

        registrySendingService.sendAfterApproval(r);

        Mockito.verify(ffClient).createRegistryRequest(createRegistryDTOArgumentCaptor.capture());
        Assert.isNonEmpty(createRegistryDTOArgumentCaptor.getValue().getLogisticUnits());
        Assertions.assertEquals(5, createRegistryDTOArgumentCaptor.getValue().getLogisticUnits().size());
        Assertions.assertTrue(createRegistryDTOArgumentCaptor.getValue().getLogisticUnits().stream()
                .anyMatch(lu -> lu.getType().equals(
                        LogisticUnitType.BOX)));
        Assertions.assertTrue(createRegistryDTOArgumentCaptor.getValue().getLogisticUnits().stream()
                .anyMatch(lu -> lu.getType().equals(
                        LogisticUnitType.ITEM)));
    }

    @Test
    public void partialReturnEmptyOrderList() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(true);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order)));
        when(ffClient.createRegistryRequest(any(CreateRegistryDTO.class))).thenReturn(shopRequestDTO);
        when(checkouterService.processPartialReturnOrders(any())).thenReturn(
                Collections.singletonList(new Pair<>(order, Collections.emptyList())));

        Assertions.assertThrows(IllegalStateException.class, () -> registrySendingService.sendAfterApproval(r),
                "При загрузке поставки произошла техническая ошибка. " +
                        "Попробуйте загрузить поставку еще раз через некоторое время"
        );
    }

    @Test
    public void noSendToFfBecauseNoBoxesInRegistry() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(false);
        Order order = orderAndRegistry.getFirst();
        Registry r = orderAndRegistry.getSecond();

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        when(checkouterService.getOrdersByItems(any()))
                .thenReturn(Map.of("3960222", Either.right(order),
                        "3960111", Either.left(OrderFetchingError.ORDER_NOT_FOUND)
                ));
        when(ffClient.createRegistryRequest(any(CreateRegistryDTO.class))).thenReturn(shopRequestDTO);

        Assertions.assertThrows(NoBoxesInRegistryException.class, () -> registrySendingService.sendAfterApproval(r));
    }

    @Test
    public void sendToFfUnpaidInboundRegistryWithoutErrors() throws Exception {
        Pair<Order, Registry> orderAndRegistry = initSendToFfWithoutCheckouterMock(true);
        Registry r = orderAndRegistry.getSecond();
        r.setType(RegistryType.UNPAID);

        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(12L);

        mockLms(172L, PointType.WAREHOUSE);
        mockLms(1L, null);

        when(ffClient.createRequestAndPutRegistry(any(PutSupplyRequestWithInboundRegisterDTO.class))).thenReturn(shopRequestDTO);
        registrySendingService.sendAsProxy(r);
        Mockito.verify(ffClient).createRequestAndPutRegistry(putSupplyRequestDTOArgumentCaptor.capture());
        PutSupplyRequestWithInboundRegisterDTO value = putSupplyRequestDTOArgumentCaptor.getValue();

        assertNotNull(value.getInboundRegistry());
        assertNotNull(value.getNextReceiver());
        assertNotNull(value.getInboundRegistry().getBoxes());
        assertNotNull(value.getShipper());

        assertEquals(value.getInboundRegistry().getBoxes().size(), 4);
        assertEquals(value.getNextReceiver().getLogisticsPointId(), 172L);
        assertEquals(value.getLogisticsPointId(), 172L);
        assertEquals(value.getShipper().getPartnerId(), 1L);

        List<PartialId> partialIds = value.getInboundRegistry()
            .getBoxes()
            .get(0)
            .getUnitInfo()
            .getCompositeId()
            .getPartialIds();
        assertEquals(partialIds
                .stream()
                .filter(id -> id.getIdType().equals(PartialIdType.BOX_ID))
                .findFirst()
                .get()
                .getValue(), "one");
        assertEquals(partialIds
                .stream()
                .filter(id -> id.getIdType().equals(PartialIdType.ORDER_ID))
                .findFirst()
                .get()
                .getValue(), "3960222");
    }

    private void mockLms(long partnerId, PointType pointType) {
        PageResult<LogisticsPointResponse> lmsResult =
                new PageResult<LogisticsPointResponse>().setData(
                        List.of(LogisticsPointResponse.newBuilder().id(partnerId).build())
                );

        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(partnerId))
                .type(pointType)
                .active(true)
                .build(), new PageRequest(0, 1))).thenReturn(lmsResult);
    }

    private Pair<Order, Registry> initSendToFfWithoutCheckouterMockWithOldReturnId(boolean secondItemWithBoxes, long id) throws Exception {
        ru.yandex.market.abo.cpa.lms.model.DeliveryService service =
                new DeliveryService(1L, "name", "jur", PartnerType.SORTING_CENTER, 136L, false, 80, true);

        Registry r = new Registry();
        r.setDeliveryService(service);
        r.setWarehouse(Warehouse.SOFINO);
        r.setDate(LocalDate.parse("2020-11-01"));
        r.setName("123456");
        r.setId(123L);

        RegistryItem item = new RegistryItem();
        item.setRegistry(r);
        item.setOrderId("3960222");
        item.setApproved(true);
        item.setMaxReceiptDate(Instant.now().plus(4, ChronoUnit.DAYS));
        item.setBoxesArray(new String[]{"one", "two"});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{});

        RegistryItem item2 = new RegistryItem();
        item2.setRegistry(r);
        item2.setOrderId("3960111");
        item2.setApproved(true);
        item2.setMaxReceiptDate(Instant.now().plus(3, ChronoUnit.DAYS));
        item2.setBoxesArray(new String[]{"two", "three"});
        item2.setSourceFulfillmentId(2L);
        item2.setBoxesApprovedArray(secondItemWithBoxes ? new String[]{"VOZVRAT_SF_PVZ_" + id } : new String[]{});

        Track validTrack = new Track();
        validTrack.setTrackCode("YA1234");
        validTrack.setDeliveryServiceId(service.getId());
        validTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        Track invalidTrack = new Track();
        validTrack.setTrackCode("YA1234");
        invalidTrack.setDeliveryServiceId(service.getId());
        invalidTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);

        Parcel parcel = new Parcel();
        parcel.setTracks(Arrays.asList(validTrack, invalidTrack));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10125");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        Order order = new Order();
        order.setId(3960222L);
        order.setDelivery(delivery);
        order.setItems(Collections.singletonList(orderItem));

        when(registryRepo.findByIdOrNull(anyLong())).thenReturn(r);
        when(registryItemRepo.findAllByRegistryId(anyLong())).thenReturn(Arrays.asList(item, item2));

        return Pair.of(order, r);
    }

    private Pair<Order, Registry> initSendToFfWithoutCheckouterMock(boolean secondItemWithBoxes) throws Exception {
        ru.yandex.market.abo.cpa.lms.model.DeliveryService service =
                new DeliveryService(1L, "name", "jur", PartnerType.SORTING_CENTER, 136L, false, 80, true);

        Registry r = new Registry();
        r.setDeliveryService(service);
        r.setWarehouse(Warehouse.SOFINO);
        r.setDate(LocalDate.parse("2020-11-01"));
        r.setName("123456");
        r.setId(123L);

        RegistryItem item = new RegistryItem();
        item.setRegistry(r);
        item.setOrderId("3960222");
        item.setApproved(true);
        item.setMaxReceiptDate(Instant.now().plus(4, ChronoUnit.DAYS));
        item.setBoxesArray(new String[]{"one", "two"});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{});

        RegistryItem item2 = new RegistryItem();
        item2.setRegistry(r);
        item2.setOrderId("3960111");
        item2.setApproved(true);
        item2.setMaxReceiptDate(Instant.now().plus(3, ChronoUnit.DAYS));
        item2.setBoxesArray(new String[]{"two", "three"});
        item2.setSourceFulfillmentId(2L);
        item2.setBoxesApprovedArray(secondItemWithBoxes ? new String[]{"two"} : new String[]{});

        Track validTrack = new Track();
        validTrack.setTrackCode("YA1234");
        validTrack.setDeliveryServiceId(service.getId());
        validTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        Track invalidTrack = new Track();
        validTrack.setTrackCode("YA1234");
        invalidTrack.setDeliveryServiceId(service.getId());
        invalidTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);

        Parcel parcel = new Parcel();
        parcel.setTracks(Arrays.asList(validTrack, invalidTrack));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10125");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        Order order = new Order();
        order.setId(3960222L);
        order.setDelivery(delivery);
        order.setItems(Collections.singletonList(orderItem));

        when(registryRepo.findByIdOrNull(anyLong())).thenReturn(r);
        when(registryItemRepo.findAllByRegistryId(anyLong())).thenReturn(Arrays.asList(item, item2));

        return Pair.of(order, r);
    }

    private Pair<Order, Registry> initSendToFfWithoutCheckouterMockAndManyBox(boolean secondItemWithBoxes) throws Exception {
        ru.yandex.market.abo.cpa.lms.model.DeliveryService service =
                new DeliveryService(1L, "name", "jur", PartnerType.SORTING_CENTER, 136L, false, 80, true);

        Registry r = new Registry();
        r.setDeliveryService(service);
        r.setWarehouse(Warehouse.SOFINO);
        r.setDate(LocalDate.parse("2020-11-01"));
        r.setName("123456");
        r.setId(123L);

        RegistryItem item = new RegistryItem();
        item.setRegistry(r);
        item.setOrderId("3960222");
        item.setApproved(true);
        item.setMaxReceiptDate(Instant.now().plus(4, ChronoUnit.DAYS));
        item.setBoxesArray(new String[]{"one", "two"});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{});

        RegistryItem item2 = new RegistryItem();
        item2.setRegistry(r);
        item2.setOrderId("3960111");
        item2.setApproved(true);
        item2.setMaxReceiptDate(Instant.now().plus(3, ChronoUnit.DAYS));
        item2.setBoxesArray(new String[]{"two", "three"});
        item2.setSourceFulfillmentId(2L);
        item2.setBoxesApprovedArray(secondItemWithBoxes ? new String[]{"two","three","four"} : new String[]{});

        Track validTrack = new Track();
        validTrack.setTrackCode("YA1234");
        validTrack.setDeliveryServiceId(service.getId());
        validTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        Track invalidTrack = new Track();
        validTrack.setTrackCode("YA1234");
        invalidTrack.setDeliveryServiceId(service.getId());
        invalidTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);

        Parcel parcel = new Parcel();
        parcel.setTracks(Arrays.asList(validTrack, invalidTrack));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10125");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        Order order = new Order();
        order.setId(3960222L);
        order.setDelivery(delivery);
        order.setItems(Collections.singletonList(orderItem));

        when(registryRepo.findByIdOrNull(anyLong())).thenReturn(r);
        when(registryItemRepo.findAllByRegistryId(anyLong())).thenReturn(Arrays.asList(item, item2));

        return Pair.of(order, r);
    }


    private HashMap<Registry, List<Order>> initSendToFfFashionAndNotFashionOrderWithoutCheckouterMock(
            boolean secondItemWithBoxes
    ) throws Exception {
        ru.yandex.market.abo.cpa.lms.model.DeliveryService service =
                new DeliveryService(1L, "name", "jur", PartnerType.SORTING_CENTER, 136L, false, 80, true);

        Registry r = new Registry();
        r.setDeliveryService(service);
        r.setWarehouse(Warehouse.SOFINO);
        r.setDate(LocalDate.parse("2020-11-01"));
        r.setName("123456");
        r.setId(123L);

        RegistryItem item = new RegistryItem();
        item.setRegistry(r);
        item.setOrderId("3960222");
        item.setApproved(true);
        item.setMaxReceiptDate(Instant.now().plus(4, ChronoUnit.DAYS));
        item.setBoxesArray(new String[]{"one", "two"});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"one"});

        RegistryItem item2 = new RegistryItem();
        item2.setRegistry(r);
        item2.setOrderId("3960111");
        item2.setApproved(true);
        item2.setMaxReceiptDate(Instant.now().plus(3, ChronoUnit.DAYS));
        item2.setBoxesArray(new String[]{"two", "three"});
        item2.setSourceFulfillmentId(2L);
        item2.setBoxesApprovedArray(secondItemWithBoxes ? new String[]{"two"} : new String[]{});

        Track validTrack = new Track();
        validTrack.setTrackCode("YA1234");
        validTrack.setDeliveryServiceId(service.getId());
        validTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        Track invalidTrack = new Track();
        validTrack.setTrackCode("YA1234");
        invalidTrack.setDeliveryServiceId(service.getId());
        invalidTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);

        Parcel parcel = new Parcel();
        parcel.setTracks(Arrays.asList(validTrack, invalidTrack));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10125");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        Order order = new Order();
        order.setId(3960222L);
        order.setDelivery(delivery);
        order.setItems(Collections.singletonList(orderItem));

        Delivery deliveryNotFashion = new Delivery();

        OrderItem orderNotFashionItem = new OrderItem();
        orderNotFashionItem.setOrderId(3960111L);
        orderNotFashionItem.setId(52714671L);
        orderNotFashionItem.setShopSku("10125");
        orderNotFashionItem.setSupplierId(48000L);
        orderNotFashionItem.setCount(1);
        orderNotFashionItem.setInstances(getArrayNode());

        OrderItem orderNotFashionItem2 = new OrderItem();
        orderNotFashionItem2.setOrderId(3960111L);
        orderNotFashionItem2.setId(52714672L);
        orderNotFashionItem2.setShopSku("10125");
        orderNotFashionItem2.setSupplierId(48000L);
        orderNotFashionItem2.setCount(1);
        orderNotFashionItem2.setInstances(getArrayNode());

        Order orderNotFashion = new Order();
        orderNotFashion.setId(3960111L);
        orderNotFashion.setDelivery(delivery);
        orderNotFashion.setItems(Arrays.asList(orderNotFashionItem, orderNotFashionItem2));

        when(registryRepo.findByIdOrNull(anyLong())).thenReturn(r);
        when(registryItemRepo.findAllByRegistryId(anyLong())).thenReturn(Arrays.asList(item, item2));
        HashMap<Registry, List<Order>> registryListHashMap = new HashMap<>();
        registryListHashMap.put(r, Arrays.asList(order, orderNotFashion));
        return registryListHashMap;
    }

    private ArrayNode getArrayNode() throws Exception {
        String json = IOUtils.toString(
                getClass().getResourceAsStream("/registry/order_item_instances.json"),
                Charsets.UTF_8
        );

        ArrayNode instances = (ArrayNode) mapper.readTree(json);
        return instances;
    }
}

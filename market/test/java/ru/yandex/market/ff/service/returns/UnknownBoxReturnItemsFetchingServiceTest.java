package ru.yandex.market.ff.service.returns;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.PagedReturnsRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.PagedReturns;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.converter.CheckouterOrderItemConverter;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.registry.RegistryEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.model.returns.BoxIdWithOrderId;
import ru.yandex.market.ff.model.returns.ReturnItemDto;
import ru.yandex.market.ff.model.returns.ReturnUnitComplexKey;
import ru.yandex.market.ff.repository.RegistryRepository;
import ru.yandex.market.ff.service.CheckouterOrderService;
import ru.yandex.market.ff.service.implementation.CheckouterOrderServiceImpl;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;

public class UnknownBoxReturnItemsFetchingServiceTest extends IntegrationTest {

    private static final String ORDER_RETURN_ID = "1232131";

    @Autowired
    private RegistryRepository registryRepository;

    @Autowired
    private CheckouterOrderItemConverter checkouterOrderItemConverter;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SoftAssertions assertions;

    private UnknownBoxReturnItemsFetchingService unknownBoxReturnItemsFetchingService;
    private CheckouterOrderService checkouterOrderService;
    private CheckouterReturnApi checkouterReturnApi;

    @BeforeEach
    public void init() {

        checkouterAPI = Mockito.mock(CheckouterAPI.class);
        checkouterReturnApi = Mockito.mock(CheckouterReturnApi.class);
        when(checkouterAPI.returns()).thenReturn(checkouterReturnApi);


        checkouterOrderService =
                new CheckouterOrderServiceImpl(Mockito.mock(CheckouterOrderHistoryEventsApi.class), checkouterAPI);


        unknownBoxReturnItemsFetchingService =
                new UnknownBoxReturnItemsFetchingService(checkouterOrderService, checkouterOrderItemConverter,
                        registryUnitService);

        assertions = new SoftAssertions();
    }

    @Test
    @DatabaseSetup("/service/returns/unknown-box-fetching/before-unknown-box-fetching.xml")
    public void enrichRegistryEntity() throws Exception {

        long orderId = 3960222L;
        Order order = getOrder(orderId);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(order));

        Return orderReturn = getReturn(orderId);
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setItems(List.of(orderReturn));

        when(checkouterReturnApi.getOrderReturns(any(), any()))
                .thenReturn(pagedReturns)
                .thenThrow(new OrderNotFoundException(orderId));

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture())).thenReturn(pagedOrders);

        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);
        var data = unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(registry);
        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey = data.getItemsData();
        List<BoxIdWithOrderId> actualAdditionalBoxes = data.getAdditionalBoxes();
        Set<ReturnUnitComplexKey> actualBoxes = data.getBoxesData();

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> expected =
                Map.of(createComplexKeyForOrderWithCIS(ORDER_RETURN_ID, "3960222"),
                        List.of(itemReturnWithCISResult(ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                                "broken")),
                        createComplexKey(ORDER_RETURN_ID, "3960222"), List.of(itemReturnResult(
                                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY))
                );

        assertions.assertThat(returnItemsGroupedByKey).containsAllEntriesOf(expected);
        assertions.assertThat(actualAdditionalBoxes).isEqualTo(Collections.emptyList());
        assertions.assertThat(actualBoxes).containsExactlyInAnyOrderElementsOf(Set.of(getBox("box1")));

        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/unknown-box-fetching/before-unknown-box-fetching-with-boxes-without-orderId.xml")
    public void ignoreBoxesWithoutOrderId() throws Exception {

        long orderId = 3960222L;
        Order order = getOrder(orderId);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(order));

        Return orderReturn = getReturn(orderId);
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setItems(List.of(orderReturn));

        when(checkouterReturnApi.getOrderReturns(any(), any()))
                .thenReturn(pagedReturns)
                .thenThrow(new OrderNotFoundException(orderId));

        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture())).thenReturn(pagedOrders);

        var data = unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(registry);

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey = data.getItemsData();
        List<BoxIdWithOrderId> actualAdditionalBoxes = data.getAdditionalBoxes();
        Set<ReturnUnitComplexKey> actualBoxes = data.getBoxesData();

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> expected =
                Map.of(createComplexKeyForOrderWithCIS(ORDER_RETURN_ID, "3960222"),
                        List.of(itemReturnWithCISResult(ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                                "broken")),
                        createComplexKey(ORDER_RETURN_ID, "3960222"), List.of(itemReturnResult(
                                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY))
                );

        assertions.assertThat(returnItemsGroupedByKey).containsAllEntriesOf(expected);
        assertions.assertThat(actualAdditionalBoxes).isEqualTo(Collections.emptyList());
        assertions.assertThat(actualBoxes).containsExactlyInAnyOrderElementsOf(Set.of(getBox("box1")));

        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/unknown-box-fetching/before-with-multiple-boxes-in-order.xml")
    public void enrichRegistryEntityWithMultipleBoxesInOneOrder() throws Exception {

        long orderId = 3960222L;
        Order order = getOrder(orderId);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(order));

        Return orderReturn = getReturn(orderId);
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setItems(List.of(orderReturn));

        when(checkouterReturnApi.getOrderReturns(any(), any()))
                .thenReturn(pagedReturns)
                .thenThrow(new OrderNotFoundException(orderId));

        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture())).thenReturn(pagedOrders);

        var data = unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(registry);
        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey = data.getItemsData();
        List<BoxIdWithOrderId> actualAdditionalBoxes = data.getAdditionalBoxes();
        Set<ReturnUnitComplexKey> actualBoxes = data.getBoxesData();

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> expected =
                Map.of(createComplexKeyForOrderWithCIS(ORDER_RETURN_ID, "3960222"),
                        List.of(itemReturnWithCISResult(ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                                "broken")),
                        createComplexKey(ORDER_RETURN_ID, "3960222"), List.of(itemReturnResult(
                                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY))
                );

        assertions.assertThat(returnItemsGroupedByKey).containsAllEntriesOf(expected);
        assertions.assertThat(actualAdditionalBoxes).isEqualTo(Collections.emptyList());
        assertions.assertThat(actualBoxes).containsExactlyInAnyOrderElementsOf(Set.of(getBox("box1"), getBox("box2")));

        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/unknown-box-fetching/before-update-parentId.xml")
    public void successfullyUpdateMainSupplyId() throws Exception {

        Order order = getOrder(3960222L);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(order));

        Return orderReturn = getReturn(3960222L);
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setItems(List.of(orderReturn));

        when(checkouterReturnApi.getOrderReturns(any(), any())).thenReturn(pagedReturns);

        var registry = createRegistry();
        unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(registry);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture())).thenReturn(pagedOrders);

        Optional<ShopRequest> request = shopRequestFetchingService.getRequest(registry.getRequestId());
        assertThat(request.get().getSupplyRequestId(), is(2L));
    }

    @Test
    @DatabaseSetup("/service/returns/unknown-box-fetching/before-enrich-order-without-order-id.xml")
    public void enrichRegistryEntityWithOrderWithoutReturn() throws Exception {

        Long orderId1 = 3960222L;
        Long orderId2 = 3960221L;
        String orderReturnIdForOrderWithoutReturn = String.valueOf(-orderId2);

        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(getOrder(orderId1), getOrder(orderId2)));

        Return orderReturn = getReturn(orderId1);
        PagedReturns pagedReturns = new PagedReturns();
        pagedReturns.setItems(List.of(orderReturn));

        when(checkouterReturnApi.getOrderReturns(any(),
                refEq(buildPagedReturnsRequest(orderId1), "statuses", "archived", "page")))
                .thenReturn(pagedReturns)
                .thenThrow(new OrderNotFoundException(orderId1));

        when(checkouterReturnApi.getOrderReturns(any(),
                refEq(buildPagedReturnsRequest(orderId2), "statuses", "archived", "page")))
                .thenThrow(new OrderNotFoundException(orderId2))
                .thenThrow(new OrderNotFoundException(orderId2));

        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture())).thenReturn(pagedOrders);

        var data = unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(registry);
        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey = data.getItemsData();
        List<BoxIdWithOrderId> actualAdditionalBoxes = data.getAdditionalBoxes();
        Set<ReturnUnitComplexKey> actualBoxes = data.getBoxesData();

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> expected =
                Map.of(
                        createComplexKeyForOrderWithCIS(ORDER_RETURN_ID, orderId1.toString()),
                        List.of(itemReturnWithCISResult(ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                                "broken")),
                        createComplexKeyForOrderWithCIS(orderReturnIdForOrderWithoutReturn, orderId2.toString()),
                        List.of(itemReturnWithCISResult(null, null)),

                        createComplexKey(ORDER_RETURN_ID, orderId1.toString()),
                        List.of(itemReturnResult(ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY)),
                        createComplexKey(orderReturnIdForOrderWithoutReturn, orderId2.toString()),
                        List.of(itemReturnResult(null))
                );

        assertions.assertThat(returnItemsGroupedByKey).isEqualTo(expected);
        assertions.assertThat(actualAdditionalBoxes).isEqualTo(Collections.emptyList());
        assertions.assertThat(actualBoxes).containsExactlyInAnyOrderElementsOf(
                Set.of(getBox("box1"), getBox("box1", orderReturnIdForOrderWithoutReturn, orderId2.toString()))
        );

        assertions.assertAll();
    }

    @NotNull
    private Order getOrder(long orderId) throws Exception {
        OrderItem orderItem = new OrderItem();
        orderItem.setOfferItemKey(new OfferItemKey("0", 0L, "0"));
        orderItem.setOrderId(orderId);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10124");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOfferItemKey(new OfferItemKey("1", 1L, "1"));
        orderItem2.setOrderId(orderId);
        orderItem2.setId(52714672L);
        orderItem2.setShopSku("10125");
        orderItem2.setSupplierId(48000L);
        orderItem2.setCount(1);
        orderItem2.setCargoTypes(objects);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(Arrays.asList(orderItem, orderItem2));
        return order;
    }

    private ArrayNode getArrayNode() throws Exception {
        String json = FileContentUtils.getFileContent("service/checkouter-order-service/order_item_instances.json");
        return (ArrayNode) MAPPER.readTree(json);
    }

    @NotNull
    private Return getReturn(long orderId) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setItemId(5271467L);
        returnItem.setCount(1);
        returnItem.setReturnReason("broken");
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);

        ReturnItem returnItem2 = new ReturnItem();
        returnItem2.setItemId(52714672L);
        returnItem2.setCount(1);
        returnItem2.setReasonType(ReturnReasonType.BAD_QUALITY);

        ReturnItem returnItem3 = new ReturnItem();
        returnItem3.setItemId(null);
        returnItem3.setCount(1);
        returnItem3.setReasonType(ReturnReasonType.DO_NOT_FIT);

        Return orderReturn = new Return();
        orderReturn.setId(1232131L);
        orderReturn.setOrderId(orderId);
        orderReturn.setItems(Arrays.asList(returnItem, returnItem2, returnItem3));
        return orderReturn;
    }

    private ReturnItemDto itemReturnResult(ru.yandex.market.ff.model.enums.ReturnReasonType reasonType) {

        ReturnItemDto returnItemDto =
                new ReturnItemDto(List.of(), reasonType, null, 1);

        return returnItemDto;
    }

    private ReturnItemDto itemReturnWithCISResult(ru.yandex.market.ff.model.enums.ReturnReasonType reasonType,
                                                  String returnReason) {
        RegistryUnitId registryUnitId =
                RegistryUnitId.of(RegistryUnitIdType.CIS, "010964018661011021mbg:zCaRlU%c08-cis1",
                        RegistryUnitIdType.SERIAL_NUMBER, "32397437-item1-9324312-1");

        ReturnItemDto returnItemDto =
                new ReturnItemDto(List.of(registryUnitId), reasonType,
                        returnReason, 1);

        return returnItemDto;
    }

    @NotNull
    private ReturnUnitComplexKey getBox(String box) {
        return getBox(box, ORDER_RETURN_ID, "3960222");
    }

    private ReturnUnitComplexKey getBox(String box, String orderReturnId, String orderId) {
        return ReturnUnitComplexKey.of(
                orderReturnId,
                box,
                null,
                null,
                orderId
        );
    }

    private ReturnUnitComplexKey createComplexKeyForOrderWithCIS(String orderReturnId, String orderId) {
        return ReturnUnitComplexKey.of(orderReturnId, "box1", 48000L, "10124", orderId);
    }

    private ReturnUnitComplexKey createComplexKey(String orderReturnId, String orderId) {
        return ReturnUnitComplexKey.of(orderReturnId, "box1", 48000L, "10125", orderId);
    }

    private RegistryEntity createRegistry() {

        UnitCount unitCount = UnitCount.of(UnitCountType.NON_COMPLIENT, 1);
        UnitCountsInfo unitCountsInfo = UnitCountsInfo.builder().unitCounts(List.of(unitCount)).build();

        RegistryUnitEntity unitEntity =
                RegistryUnitEntity.builder().type(RegistryUnitType.BOX).id(10L).unitCountsInfo(unitCountsInfo)
                        .identifiers(RegistryUnitId.of(RegistryUnitIdType.BOX_ID, "box1", RegistryUnitIdType.ORDER_ID,
                                "3960222")).build();

        return RegistryEntity.builder().registryUnits(List.of(unitEntity)).type(RegistryFlowType.PLANNED_RETURNS)
                .requestId(1L).build();
    }

    private PagedReturnsRequest buildPagedReturnsRequest(long orderId) {
        return PagedReturnsRequest.builder(orderId)
                .withPage(1)
                .withPageSize(50)
                .withArchived(false)
                .build();
    }
}

package ru.yandex.market.ff.service.returns;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.model.converter.CheckouterOrderItemConverter;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.returns.BoxIdWithOrderId;
import ru.yandex.market.ff.model.returns.ReturnItemDto;
import ru.yandex.market.ff.model.returns.ReturnRegistryEnrichmentData;
import ru.yandex.market.ff.model.returns.ReturnUnitComplexKey;
import ru.yandex.market.ff.repository.RegistryRepository;
import ru.yandex.market.ff.repository.RegistryUnitInvalidRepository;
import ru.yandex.market.ff.service.CheckouterOrderService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.LomOrderService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.implementation.CheckouterOrderServiceImpl;
import ru.yandex.market.ff.service.implementation.ReturnEntityService;
import ru.yandex.market.ff.service.lrm.LrmService;
import ru.yandex.market.ff.service.registry.converter.RegistryUnitToRegistryUnitInvalidConverter;
import ru.yandex.market.ff.util.CreateCheckouterOrdersForReturnUtils;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.ReturnItem;
import ru.yandex.market.logistics.lrm.client.model.SearchReturn;
import ru.yandex.market.logistics.lrm.client.model.SearchReturnsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class CheckouterSyncItemsFetchingServiceTest extends IntegrationTest {

    @Autowired
    private RegistryRepository registryRepository;
    @Autowired
    private ReturnEntityService returnService;
    @Autowired
    private LomOrderService lomOrderService;
    @Autowired
    private CheckouterOrderItemConverter checkouterOrderItemConverter;
    @Autowired
    private LrmService lrmService;
    @Autowired
    private ReturnsApi returnsApi;
    @Autowired
    private ConcreteEnvironmentParamService paramService;
    @Autowired
    private RegistryUnitToRegistryUnitInvalidConverter registryUnitToRegistryUnitInvalidConverter;
    @Autowired
    private RegistryUnitInvalidRepository registryUnitInvalidRepository;
    @Autowired
    private RequestSubTypeService requestSubTypeService;

    private SoftAssertions assertions;
    private CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi;

    private CheckouterSyncItemsFetchingService checkouterSyncItemsFetchingService;
    private CheckouterOrderService checkouterOrderService;

    @BeforeEach
    public void init() {
        checkouterOrderHistoryEventsApi = Mockito.mock(CheckouterOrderHistoryEventsApi.class);
        checkouterOrderService = new CheckouterOrderServiceImpl(
                checkouterOrderHistoryEventsApi,
                checkouterAPI);

        when(lomClient.searchOrders(any(), any()))
                .thenReturn(PageResult.of(List.of(CreateCheckouterOrdersForReturnUtils.createLomOrderDto()), 1, 0, 1));

        checkouterSyncItemsFetchingService =
                new CheckouterSyncItemsFetchingService(checkouterOrderService,
                        returnService,
                        lomOrderService,
                        checkouterOrderItemConverter,
                        lrmService,
                        paramService,
                        registryUnitToRegistryUnitInvalidConverter,
                        registryUnitInvalidRepository,
                        shopRequestFetchingService,
                        requestSubTypeService
                );

        assertions = new SoftAssertions();
    }

    @Test
    @DatabaseSetup("classpath:service/returns/checkouter-items-fetching/nothing-in-return-tables.xml")
    public void nothingInReturnTables() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), captor.capture()))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));

        var data = checkouterSyncItemsFetchingService.getReturnItemsGroupedByKey(registry);
        verifyZeroInteractions(checkouterOrderHistoryEventsApi);
        verify(lomClient, never()).searchOrders(any(), any());
        verify(returnsApi, never()).searchReturns(any());

        var actualBoxes = data.getAdditionalBoxes();
        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey = data.getItemsData();

        List<ReturnItemDto> expectedResult = unpaidItemReturnItemResult();

        List<ReturnItemDto> actualResult = returnItemsGroupedByKey.entrySet()
                .stream()
                .flatMap(kv -> kv.getValue().stream())
                .collect(Collectors.toList());

        var expectedIterator = expectedResult.iterator();
        var actualIterator = actualResult.iterator();

        comparisonNext(expectedIterator, actualIterator);

        assertions.assertThat(actualBoxes).isEmpty();
        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/checkouter-items-fetching/only_boxes_in_return_tables.xml")
    public void onlyBoxesInReturnTables() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
                .thenReturn(CreateCheckouterOrdersForReturnUtils.createPagedOrders(1));

        var data = checkouterSyncItemsFetchingService.getReturnItemsGroupedByKey(registry);
        verify(lomClient, never()).searchOrders(any(), any());
        verify(returnsApi, never()).searchReturns(any());

        var actualBoxes = data.getAdditionalBoxes();
        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey = data.getItemsData();

        List<ReturnItemDto> expectedResult = unpaidItemReturnItemResult();

        List<ReturnItemDto> actualResult = returnItemsGroupedByKey.entrySet()
                .stream()
                .flatMap(kv -> kv.getValue().stream())
                .collect(Collectors.toList());

        var expectedIterator = expectedResult.iterator();
        var actualIterator = actualResult.iterator();

        comparisonNext(expectedIterator, actualIterator);

        assertions.assertThat(actualBoxes).isEmpty();
        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/checkouter-items-fetching/items_in_return_tables.xml")
    public void itemsInReturnTables() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey =
                checkouterSyncItemsFetchingService.getReturnItemsGroupedByKey(registry).getItemsData();
        verify(lomClient, never()).searchOrders(any(), any());
        verify(returnsApi, never()).searchReturns(any());

        assertions.assertThat(returnItemsGroupedByKey.size()).isEqualTo(2);

        List<ReturnItemDto> actualItemDto = returnItemsGroupedByKey.get(createComplexKey());
        List<ReturnItemDto> actualItemDtoWithCIS = returnItemsGroupedByKey.get(createComplexKeyForOrderWithCIS());

        comparison(itemReturnItemResult(), actualItemDto);
        comparison(itemReturnItemResultWithCIS(), actualItemDtoWithCIS);

        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("classpath:service/returns/checkouter-items-fetching/boxes_from_lom.xml")
    public void boxesFromLom() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        ReturnRegistryEnrichmentData data = checkouterSyncItemsFetchingService.getReturnItemsGroupedByKey(registry);
        verify(returnsApi, never()).searchReturns(any());

        var returnItemsGroupedByKey = data.getItemsData();
        var additionalBoxes = data.getAdditionalBoxes();

        assertions.assertThat(returnItemsGroupedByKey.size()).isEqualTo(0);

        var expectedBoxes = List.of(
                new BoxIdWithOrderId(null, "box1"),
                new BoxIdWithOrderId("not-a-P-ff2", "1"),
                new BoxIdWithOrderId("not-a-P-ff", "1"));
        assertions.assertThat(additionalBoxes).isEqualTo(expectedBoxes);
        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/checkouter-items-fetching/items_from_lrm.xml")
    public void itemsFromLrm() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        when(returnsApi.searchReturns(any())).thenReturn(getSearchReturnsResponse());

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey =
                checkouterSyncItemsFetchingService.getReturnItemsGroupedByKey(registry).getItemsData();
        verify(lomClient, never()).searchOrders(any(), any());

        assertions.assertThat(returnItemsGroupedByKey.size()).isEqualTo(2);

        List<ReturnItemDto> actualItemDto = returnItemsGroupedByKey.get(createComplexKey());
        List<ReturnItemDto> actualItemDtoWithCIS = returnItemsGroupedByKey.get(createComplexKeyForOrderWithCIS());

        comparison(itemReturnItemResult(), actualItemDto);
        comparison(itemReturnItemResultWithCIS(), actualItemDtoWithCIS);

        assertions.assertAll();
    }

    @Test
    @DatabaseSetup("/service/returns/checkouter-items-fetching/duplicate_returns.xml")
    public void duplicateReturns() {
        var registry = registryRepository.findAllByRequestIdWithUnitsFetched(1L).get(0);

        when(returnsApi.searchReturns(any())).thenReturn(getSearchReturnsResponse());

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemsGroupedByKey =
                checkouterSyncItemsFetchingService.getReturnItemsGroupedByKey(registry).getItemsData();
        verify(lomClient, never()).searchOrders(any(), any());

        assertions.assertThat(returnItemsGroupedByKey.size()).isEqualTo(1);

        List<ReturnItemDto> actualItemDto = returnItemsGroupedByKey.get(createComplexKeyForOrderWithCIS());

        comparison(itemReturnItemResultWithCIS(), actualItemDto);

        assertions.assertAll();
    }

    @Nonnull
    private SearchReturnsResponse getSearchReturnsResponse() {
        var searchResponse = new SearchReturnsResponse();
        searchResponse.setReturns(List.of(getSearchReturn()));
        return searchResponse;
    }

    @Nonnull
    private SearchReturn getSearchReturn() {
        var item1 = new ReturnItem();
        item1.setVendorCode("sku2");
        item1.setSupplierId(11L);
        item1.setInstances(Map.of(PartialIdType.CIS.getName(), "cis1",
                PartialIdType.SERIAL_NUMBER.getName(), "sn1"));
        var item2 = new ReturnItem();
        item2.setVendorCode("sku2");
        item2.setSupplierId(11L);
        item2.setInstances(Map.of(PartialIdType.CIS.getName(), "cis2",
                PartialIdType.SERIAL_NUMBER.getName(), "sn2"));

        var searchReturn = new SearchReturn();
        searchReturn.setExternalId("extId2");
        searchReturn.setOrderExternalId("5");
        searchReturn.setItems(List.of(item1, item2));
        return searchReturn;
    }

    private void comparisonNext(Iterator<ReturnItemDto> expected, Iterator<ReturnItemDto> actual) {
        var expectedDto = expected.next();
        var actualDto = actual.next();

        assertions.assertThat(actualDto.getCount()).isEqualTo(expectedDto.getCount());
        assertions.assertThat(actualDto.getInstances()).isEqualTo(expectedDto.getInstances());
    }

    private void comparison(ReturnItemDto expected, List<ReturnItemDto> actual) {
        actual.forEach(item -> {
            assertions.assertThat(expected).isEqualTo(item);

            if (expected.getInstances() != null) {
                assertions.assertThat(expected.getInstances()).isEqualTo(item.getInstances());
            }
        });
    }

    private List<ReturnItemDto> unpaidItemReturnItemResult() {
        RegistryUnitId registryUnitId = RegistryUnitId.of(
                RegistryUnitIdType.CIS,
                "010964018661011021mbg:zCaRlU%c08-cis1",
                RegistryUnitIdType.SERIAL_NUMBER,
                "32397437-item1-9324312-1");

        ReturnItemDto returnItemDto = new ReturnItemDto(
                List.of(registryUnitId),
                null,
                null,
                1);

        return List.of(returnItemDto);
    }

    private ReturnItemDto itemReturnItemResult() {
        return new ReturnItemDto(
                List.of(),
                null,
                null,
                1);
    }

    private ReturnItemDto itemReturnItemResultWithCIS() {

        RegistryUnitId registryUnitId1 = RegistryUnitId.of(
                new UnitPartialId(RegistryUnitIdType.CIS, "cis1"),
                new UnitPartialId(RegistryUnitIdType.SERIAL_NUMBER, "sn1"));
        RegistryUnitId registryUnitId2 = RegistryUnitId.of(
                new UnitPartialId(RegistryUnitIdType.CIS, "cis2"),
                new UnitPartialId(RegistryUnitIdType.SERIAL_NUMBER, "sn2"));

        return new ReturnItemDto(
                List.of(registryUnitId1, registryUnitId2),
                null,
                null,
                1);
    }

    private ReturnUnitComplexKey createComplexKeyForOrderWithCIS() {
        return ReturnUnitComplexKey.of(
                "extId2",
                null,
                11L,
                "sku2",
                "5"
        );
    }

    private ReturnUnitComplexKey createComplexKeyForOrderWithSameBoxId() {
        return ReturnUnitComplexKey.of(
                "extId2",
                null,
                11L,
                "sku2",
                "box2"
        );
    }

    private ReturnUnitComplexKey createComplexKey() {
        return ReturnUnitComplexKey.of(
                "extId1",
                null,
                11L,
                "sku1",
                "10"
        );
    }
}

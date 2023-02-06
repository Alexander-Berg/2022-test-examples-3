package ru.yandex.market.wms.picking.modules.service;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.PickingOrderItem;
import ru.yandex.market.wms.core.base.response.SortLocsResponse;
import ru.yandex.market.wms.core.client.CoreClient;

import static org.mockito.ArgumentMatchers.eq;

class GreedySortServiceTest extends IntegrationTest {
    @Autowired
    private GreedySortService greedySortService;
    @MockBean
    @Autowired
    private CoreClient coreClient;

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(coreClient);
        Mockito.when(coreClient.sortLocList(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
                .thenAnswer((InvocationOnMock invocationOnMock) ->
                        new SortLocsResponse(invocationOnMock.getArgument(0)));
    }

    @Test
    public void testGreedySort() {
        List<String> locs = List.of("A1-01-01A1", "A1-02-04A1", "A1-04-06A1", "A1-03-09A1", "A1-05-09A1", "A1-06-01A1");
        List<PickingOrderItem> sortedItems = greedySortService.sort(items(locs), false, null);
        Mockito.verify(coreClient, Mockito.times(1)).sortLocList(eq(locs), eq(false), eq(null));
    }

    @Test
    public void testGreedySort_reverse() {
        List<String> locs = List.of("A1-06-01A1", "A1-05-09A1", "A1-03-09A1", "A1-04-06A1", "A1-02-04A1", "A1-01-01A1");
        List<PickingOrderItem> sortedItems = greedySortService.sort(items(locs), true, null);
        Mockito.verify(coreClient, Mockito.times(1)).sortLocList(eq(locs), eq(true), eq(null));
    }

    @Test
    public void testGreedySortWithFirstItem() {
        List<String> locs = List.of("A1-02-04A1", "A1-01-01A1", "A1-04-06A1", "A1-03-09A1");
        List<PickingOrderItem> items = items(locs);
        PickingOrderItem firstItem = items.stream()
                .filter(item -> item.getFromLoc().equals("A1-02-04A1")).findAny().orElse(null);
        List<PickingOrderItem> sortedItems = greedySortService.sort(items(locs), false, firstItem);
        Mockito.verify(coreClient, Mockito.times(1)).sortLocList(eq(locs), eq(false), eq(firstItem.getFromLoc()));
    }

    @Test
    public void testGreedySortWithFirstItem_reverse() {
        List<String> locs = List.of("A1-03-09A1", "A1-04-06A1", "A1-02-04A1", "A1-01-01A1");
        List<PickingOrderItem> items = items(locs);
        PickingOrderItem firstItem = items.stream()
                .filter(item -> item.getFromLoc().equals("A1-03-09A1")).findAny().orElse(null);
        List<PickingOrderItem> sortedItems = greedySortService.sort(items(locs), true, firstItem);
        Mockito.verify(coreClient, Mockito.times(1)).sortLocList(eq(locs), eq(true), eq(firstItem.getFromLoc()));
    }

    @Nonnull
    private List<PickingOrderItem> items(List<String> locs) {
        return locs.stream()
                .map(loc -> PickingOrderItem.builder().fromLoc(loc).build())
                .toList();
    }
}

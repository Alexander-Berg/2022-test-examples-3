package ru.yandex.market.ff.service.implementation;

import java.util.ArrayList;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestItemServiceUnitTest {

    @InjectMocks
    private RequestItemServiceImpl service;

    @Mock
    private ConcreteEnvironmentParamService environmentParamService;

    @Test
    void spitToBatchesBySuppliers() {

        when(environmentParamService.getPreviousSuppliedItemsBatchSize()).thenReturn(300);
        when(environmentParamService.getMaxSupplierInLastSuppliedItemsBatch()).thenReturn(2);


        List<RequestItem> requestItems = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            RequestItem requestItem = new RequestItem();
            requestItem.setSupplierId(1L);
            requestItems.add(requestItem);

            RequestItem requestItem2 = new RequestItem();
            requestItem2.setSupplierId(2L);
            requestItems.add(requestItem2);

            RequestItem requestItem3 = new RequestItem();
            requestItem3.setSupplierId(3L);
            requestItems.add(requestItem3);
        }


        List<List<RequestItem>> lists = service.spitToBatches(requestItems);
        assertFalse(lists.isEmpty());
        assertEquals(2, lists.size());
        assertEquals(200, lists.get(0).size());
        assertEquals(100, lists.get(1).size());


    }

    @Test
    void spitToBatchesByItems() {

        when(environmentParamService.getPreviousSuppliedItemsBatchSize()).thenReturn(75);
        when(environmentParamService.getMaxSupplierInLastSuppliedItemsBatch()).thenReturn(3);


        List<RequestItem> requestItems = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            RequestItem requestItem = new RequestItem();
            requestItem.setSupplierId(1L);
            requestItems.add(requestItem);

            RequestItem requestItem2 = new RequestItem();
            requestItem2.setSupplierId(2L);
            requestItems.add(requestItem2);

            RequestItem requestItem3 = new RequestItem();
            requestItem3.setSupplierId(3L);
            requestItems.add(requestItem3);
        }


        List<List<RequestItem>> lists = service.spitToBatches(requestItems);
        assertFalse(lists.isEmpty());
        assertEquals(4, lists.size());
        assertEquals(75, lists.get(0).size());
        assertEquals(75, lists.get(1).size());
        assertEquals(75, lists.get(2).size());
        assertEquals(75, lists.get(3).size());
    }


    @Test
    void spitToBatchesByItemsAndSupplier() {

        when(environmentParamService.getPreviousSuppliedItemsBatchSize()).thenReturn(120);
        when(environmentParamService.getMaxSupplierInLastSuppliedItemsBatch()).thenReturn(2);


        List<RequestItem> requestItems = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            RequestItem requestItem = new RequestItem();
            requestItem.setSupplierId(1L);
            requestItems.add(requestItem);

        }

        for (int i = 0; i < 50; i++) {

            RequestItem requestItem2 = new RequestItem();
            requestItem2.setSupplierId(2L);
            requestItems.add(requestItem2);

            RequestItem requestItem3 = new RequestItem();
            requestItem3.setSupplierId(3L);
            requestItems.add(requestItem3);

            RequestItem requestItem4 = new RequestItem();
            requestItem4.setSupplierId(4L);
            requestItems.add(requestItem4);
        }


        List<List<RequestItem>> lists = service.spitToBatches(requestItems);
        assertFalse(lists.isEmpty());
        assertEquals(3, lists.size());
        assertEquals(120, lists.get(0).size());
        assertEquals(80, lists.get(1).size());
        assertEquals(50, lists.get(2).size());
    }

    @Test
    void spitToBatchesWhenBatchSizeEqualsToItemsCount() {

        when(environmentParamService.getPreviousSuppliedItemsBatchSize()).thenReturn(100);
        when(environmentParamService.getMaxSupplierInLastSuppliedItemsBatch()).thenReturn(10);


        List<RequestItem> requestItems = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            RequestItem requestItem = new RequestItem();
            requestItem.setSupplierId(1L);
            requestItems.add(requestItem);
        }

        List<List<RequestItem>> lists = service.spitToBatches(requestItems);
        assertFalse(lists.isEmpty());
        assertEquals(1, lists.size());
        assertEquals(100, lists.get(0).size());
    }


}

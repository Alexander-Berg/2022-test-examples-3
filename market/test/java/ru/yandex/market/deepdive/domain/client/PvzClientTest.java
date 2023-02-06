package ru.yandex.market.deepdive.domain.client;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.transform.AliasedTupleSubsetResultTransformer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointOrderDto;
import ru.yandex.market.deepdive.domain.pickup_point_order.PickupPointOrder;


public class PvzClientTest {


    @Test
    public void shouldGetData() {
        PvzClient client = Mockito.mock(PvzClient.class);
        Long partnerId = 1L;
        Mockito.when(client.getPickupPoints(partnerId)).thenCallRealMethod();

        Mockito.when(client.getPageContent(0, partnerId)).thenReturn(prepareTestDataPage(0));
        Mockito.when(client.getPageContent(1, partnerId)).thenReturn(prepareTestDataPage(1));
        Mockito.when(client.getPageContent(2, partnerId)).thenReturn(prepareTestDataPage(2));
        Mockito.when(client.getPageContent(3, partnerId)).thenReturn(new ArrayList<>());


        List<PvzIntPickupPointDto> testPickupPointDtos = prepareTestDataPage(0);
        testPickupPointDtos.addAll(prepareTestDataPage(1));
        testPickupPointDtos.addAll(prepareTestDataPage(2));

        List<PvzIntPickupPointDto> gottenPickupPointDtos = client.getPickupPoints(partnerId);
        testPickupPointDtos.sort(Comparator.comparingLong(PvzIntPickupPointDto::getId));
        gottenPickupPointDtos.sort(Comparator.comparingLong(PvzIntPickupPointDto::getId));
        Assert.assertArrayEquals(testPickupPointDtos.toArray(), gottenPickupPointDtos.toArray());

    }

    List<PvzIntPickupPointDto> prepareTestDataPage(int pageNum) {
        String name = "name";
        List<PvzIntPickupPointDto> l = new ArrayList<>();
        for (int i = 10 * pageNum; i < 10 * (pageNum + 1); i++) {
            PvzIntPickupPointDto pickupPointDto = new PvzIntPickupPointDto();
            pickupPointDto.setId(i);
            pickupPointDto.setName(name + i);
            pickupPointDto.setActive(i % (pageNum + 1) == 0);
            l.add(pickupPointDto);
        }
        return l;

    }


    @Test
    public void shouldGetOrderData() {
        PvzClient client = Mockito.mock(PvzClient.class);
        Long pvzMarketId = 1L;
        Mockito.when(client.getPickupPointOrders(pvzMarketId)).thenCallRealMethod();

        Mockito.when(client.getOrdersPageContent(0, pvzMarketId)).thenReturn(prepareTestOrderDataPage(0));
        Mockito.when(client.getOrdersPageContent(1, pvzMarketId)).thenReturn(prepareTestOrderDataPage(1));
        Mockito.when(client.getOrdersPageContent(2, pvzMarketId)).thenReturn(prepareTestOrderDataPage(2));
        Mockito.when(client.getOrdersPageContent(3, pvzMarketId)).thenReturn(new ArrayList<>());


        List<PickupPointOrder> testPickupPointOrders = prepareTestOrderDataPage(0);
        for (int i = 1; i < 3; i++) {
            testPickupPointOrders.addAll(prepareTestOrderDataPage(i));
        }

        List<PickupPointOrder> gottenPickupPointOrders = client.getPickupPointOrders(pvzMarketId);
        testPickupPointOrders.sort(Comparator.comparingLong(PickupPointOrder::getId));
        gottenPickupPointOrders.sort(Comparator.comparingLong(PickupPointOrder::getId));
        Assert.assertArrayEquals(testPickupPointOrders.toArray(), gottenPickupPointOrders.toArray());
    }

    List<PickupPointOrder> prepareTestOrderDataPage(int pageNum) {
        String[] status = new String[]{
                "CREATED", "ARRIVED_TO_PICKUP_POINT",
                "STORAGE_PERIOD_EXTENDED",
                "STORAGE_PERIOD_EXPIRED",
                "DELIVERY_DATE_UPDATED_BY_DELIVERY",
                "TRANSPORTATION_RECIPIENT",
                "TRANSMITTED_TO_RECIPIENT",
                "DELIVERED_TO_RECIPIENT",
                "READY_FOR_RETURN",
                "RETURNED_ORDER_WAS_DISPATCHED",
                "RETURNED_ORDER_IS_DELIVERED_TO_SENDER",
                "LOST", "CANCELLED"
        };
        String[] paymentType = new String[]{"CARD", "CASH", "PREPAID", "UNKNOWN"};

        List<PickupPointOrder> l = new ArrayList<>();
        for (int i = 40 * pageNum; i < 40 * (pageNum + 1); i++) {
            PickupPointOrder pickupPointOrder = new PickupPointOrder();
            pickupPointOrder.setId(i);
            pickupPointOrder.setPickupPointId(i + 100);
            pickupPointOrder.setDeliveryDate(new Date(2021, 5, 20));
            pickupPointOrder.setTotalPrice(10 * i);
            pickupPointOrder.setStatus(status[i % status.length]);
            pickupPointOrder.setPaymentType(paymentType[i % paymentType.length]);
            l.add(pickupPointOrder);
        }
        return l;

    }

}

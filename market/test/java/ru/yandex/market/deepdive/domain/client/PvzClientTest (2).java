package ru.yandex.market.deepdive.domain.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzPickupPointOrderDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;

public class PvzClientTest {


    @Test
    public void pickupPointOrdersTest() {
        final long firstPageSize= 3;
        final long secondPageSize = 3;
        final long thirdPageSize = 3;
        PvzClient client = Mockito.mock(PvzClient.class);
        Mockito.when(client.getOrders(222L)).thenCallRealMethod();
        List<PvzPickupPointOrderDto> firstPage = new LinkedList<>();
        for (long i = 0; i < firstPageSize; i++) {
            PvzPickupPointOrderDto order = new PvzPickupPointOrderDto();
            order.setId(i);
            firstPage.add(order);
        }
        Mockito.when(client.getOrdersPage(0, 222L)).thenReturn(firstPage);
        List<PvzPickupPointOrderDto> secondPage = new LinkedList<>();
        for (long i = 0; i < secondPageSize; i++) {
            PvzPickupPointOrderDto order = new PvzPickupPointOrderDto();
            order.setId(i + firstPageSize);
            secondPage.add(order);
        }
        Mockito.when(client.getOrdersPage(1, 222L)).thenReturn(secondPage);
        List<PvzPickupPointOrderDto> thirdPage = new LinkedList<>();
        for (long i = 0; i < thirdPageSize; i++) {
            PvzPickupPointOrderDto order = new PvzPickupPointOrderDto();
            order.setId(i + secondPageSize + firstPageSize);
            thirdPage.add(order);
        }
        Mockito.when(client.getOrdersPage(2, 222L)).thenReturn(thirdPage);
        Mockito.when(client.getOrdersPage(3, 222L)).thenReturn(new LinkedList<>());
        Set<PvzPickupPointOrderDto> orderDtos = client.getOrders(222L);
        Assert.assertEquals(3 + 3 + 3, orderDtos.size());
    }


    @Test
    public void pagesInPvzTest() {
        final int firstPageSize = 5;
        final int secondPageSize = 4;
        final int thirdPageSize = 3;

        PvzClient client = Mockito.mock(PvzClient.class);
        Mockito.when(client.getPickupPoints(222L)).thenCallRealMethod();

        List<PvzIntPickupPointDto> firstPage = new LinkedList<>();
        for (long i = 0; i < firstPageSize; i++) {
            PvzIntPickupPointDto pickupPointDto = new PvzIntPickupPointDto();
            pickupPointDto.setId(i);
            firstPage.add(pickupPointDto);
        }
        Mockito.when(client.getPickupPointsPage(0, 222L)).thenReturn(firstPage);
        List<PvzIntPickupPointDto> secondPage = new LinkedList<>();
        for (long i = 0; i < secondPageSize; i++) {
            PvzIntPickupPointDto pickupPointDto = new PvzIntPickupPointDto();
            pickupPointDto.setId(i + firstPageSize);
            secondPage.add(pickupPointDto);
        }
        Mockito.when(client.getPickupPointsPage(1, 222L)).thenReturn(secondPage);
        List<PvzIntPickupPointDto> thirdPage = new LinkedList<>();
        for (long i = 0; i < thirdPageSize; i++) {
            PvzIntPickupPointDto pickupPointDto = new PvzIntPickupPointDto();
            pickupPointDto.setId(i); //same id's. They will not be added
            thirdPage.add(pickupPointDto);
        }
        Mockito.when(client.getPickupPointsPage(2, 222L)).thenReturn(thirdPage);
        Assert.assertEquals(firstPageSize + secondPageSize, client.getPickupPoints(222L).size());
    }
}

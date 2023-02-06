package ru.yandex.market.deepdive.updateExecutor;

import java.sql.Date;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PickupPointOrdersDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;
import ru.yandex.market.deepdive.domain.order.PaymentType;
import ru.yandex.market.deepdive.domain.order.Status;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

@Slf4j
public class UpdateExecutorTest {

    @Test
    public void checkProcessUpdate(){

        long pvzId = 1001047541;
        PvzClient client = Mockito.mock(PvzClient.class);
        Mockito.when(client.getOrdersOfPickupPoint(pvzId)).thenCallRealMethod();

        var firstPickupPointOrderDto = new PickupPointOrdersDto();
        firstPickupPointOrderDto.setId(1);
        firstPickupPointOrderDto.setPickupPointId(2);
        firstPickupPointOrderDto.setPaymentType(PaymentType.CARD);
        firstPickupPointOrderDto.setTotalPrice(1000);
        firstPickupPointOrderDto.setStatus(Status.CREATED);
        firstPickupPointOrderDto.setDeliveryDate(Date.valueOf("2020-03-20"));

        var firstPageableResponse = new PageableResponse<PickupPointOrdersDto>();
        firstPageableResponse.setContent(List.of(firstPickupPointOrderDto));

        var secondPickupPointOrderDto = new PickupPointOrdersDto();
        secondPickupPointOrderDto.setId(2);
        secondPickupPointOrderDto.setPickupPointId(2);
        secondPickupPointOrderDto.setPaymentType(PaymentType.CARD);
        secondPickupPointOrderDto.setTotalPrice(2000);
        secondPickupPointOrderDto.setStatus(Status.DELIVERED_TO_RECIPIENT);
        secondPickupPointOrderDto.setDeliveryDate(Date.valueOf("2020-03-21"));

        var secondPageableResponse = new PageableResponse<PickupPointOrdersDto>();
        secondPageableResponse.setContent(List.of(firstPickupPointOrderDto));

        var emptyPageableResponse = new PageableResponse<PickupPointOrdersDto>();
        emptyPageableResponse.setContent(Collections.emptyList());

        log.info(firstPageableResponse.toString());
        log.info(secondPageableResponse.toString());
        log.info(emptyPageableResponse.toString());

        Mockito.when(client.getOrdersOfPickupPointByPageNumber(pvzId, 0)).thenReturn(firstPageableResponse);
        Mockito.when(client.getOrdersOfPickupPointByPageNumber(pvzId, 1)).thenReturn(secondPageableResponse);
        Mockito.when(client.getOrdersOfPickupPointByPageNumber(pvzId, 2)).thenReturn(emptyPageableResponse);

        var expectedPageableResponse = new PageableResponse<PickupPointOrdersDto>();
        expectedPageableResponse.setContent(List.of(firstPickupPointOrderDto, secondPickupPointOrderDto));

        Assertions.assertIterableEquals(client.getPickupPoints(pvzId), expectedPageableResponse.getContent());






    }

}

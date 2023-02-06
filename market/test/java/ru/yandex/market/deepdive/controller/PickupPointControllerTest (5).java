package ru.yandex.market.deepdive.controller;

import java.util.Collections;
import java.util.List;


import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;
import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

@Slf4j
public class PickupPointControllerTest{

    @Test
    public void testV1WithMockito() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);

        PickupPointDto.PickupPointDtoBuilder pickupPointDtoBuilder = PickupPointDto.builder();
        List<PickupPointDto> pickupPointList = List.of(pickupPointDtoBuilder.id(1).name("first").build(), pickupPointDtoBuilder.id(2).name("second").build());

        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(pickupPointList);

        PickupPointController pickupPointController = new PickupPointController(pickupPointService);
        pickupPointController.getPickupPoints();

        Mockito.verify(pickupPointService).getPickupPoints();
    }

    @Test
    public void testLoadingAllPvz() {
        long clientId = 1001047541;
        PvzClient client = Mockito.mock(PvzClient.class);
        Mockito.when(client.getPickupPoints(clientId)).thenCallRealMethod();

        var firstPvzIntPickupPointDto = new PvzIntPickupPointDto();
        firstPvzIntPickupPointDto.setId(1);
        firstPvzIntPickupPointDto.setName("first");

        var firstPageableResponse = new PageableResponse<PvzIntPickupPointDto>();
        firstPageableResponse.setContent(List.of(firstPvzIntPickupPointDto));

        var secondPvzIntPickupPointDto = new PvzIntPickupPointDto();
        secondPvzIntPickupPointDto.setId(2);
        secondPvzIntPickupPointDto.setName("second");

        var secondPageableResponse = new PageableResponse<PvzIntPickupPointDto>();
        secondPageableResponse.setContent(List.of(secondPvzIntPickupPointDto));

        var emptyPageableResponse = new PageableResponse<PvzIntPickupPointDto>();
        emptyPageableResponse.setContent(Collections.emptyList());

        log.info(firstPageableResponse.toString());
        log.info(secondPageableResponse.toString());
        log.info(emptyPageableResponse.toString());

        Mockito.when(client.getPickupPointByPageNumber(clientId, 0)).thenReturn(firstPageableResponse);
        Mockito.when(client.getPickupPointByPageNumber(clientId, 1)).thenReturn(secondPageableResponse);
        Mockito.when(client.getPickupPointByPageNumber(clientId, 2)).thenReturn(emptyPageableResponse);

        var expectedPageableResponse = new PageableResponse<PvzIntPickupPointDto>();
        expectedPageableResponse.setContent(List.of(firstPvzIntPickupPointDto, secondPvzIntPickupPointDto));

        Assertions.assertIterableEquals(client.getPickupPoints(clientId), expectedPageableResponse.getContent());
    }


}

package ru.yandex.market.deepdive.domain.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrdersDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

public class PvzClientTest extends TestCase {

    @Test
    public void testGetPickupPoints_1PageWith2Pvz_2Pvz() {

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        List<PvzIntPickupPointDto> pvzIntPickupPointDtoList = create2PvzList();

        PageableResponse<PvzIntPickupPointDto> pageableResponse = new PageableResponse<>();
        pageableResponse.setLast(true);
        pageableResponse.setContent(pvzIntPickupPointDtoList);

        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> response = new ResponseEntity<>(
                pageableResponse,
                HttpStatus.OK
        );

        Mockito
                .when(restTemplate.exchange(
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        PvzClient pvzClient = new PvzClient(restTemplate);

        Assert.assertEquals(
                pvzIntPickupPointDtoList,
                pvzClient.getPickupPoints(212)
        );
    }

    @Test
    public void testGetPickupPoints_2PageWith2PvzAnd3Pvz_5Pvz() {

        long partnerId = 228;

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        List<PvzIntPickupPointDto> firstPage = create2PvzList();
        List<PvzIntPickupPointDto> secondPage = create3PvzList();
        List<PvzIntPickupPointDto> resultPvzList = new ArrayList<>();
        resultPvzList.addAll(firstPage);
        resultPvzList.addAll(secondPage);

        PageableResponse<PvzIntPickupPointDto> pageableResponseFirsPage = new PageableResponse<>();
        pageableResponseFirsPage.setLast(false);
        pageableResponseFirsPage.setContent(firstPage);

        PageableResponse<PvzIntPickupPointDto> pageableResponseSecondPage = new PageableResponse<>();
        pageableResponseSecondPage.setLast(true);
        pageableResponseSecondPage.setContent(secondPage);

        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> firstResponse = new ResponseEntity<>(
                pageableResponseFirsPage,
                HttpStatus.OK
        );

        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> secondResponse = new ResponseEntity<>(
                pageableResponseSecondPage,
                HttpStatus.OK
        );

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("?page=0"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(firstResponse);

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("?page=1"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(secondResponse);

        PvzClient pvzClient = new PvzClient(restTemplate);

        Assert.assertEquals(
                resultPvzList,
                pvzClient.getPickupPoints(partnerId)
        );
    }

    @Test
    public void testGetPickupPoints_HttpStatus404_ResponseStatusException() {

        long partnerId = 228;

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        List<PvzIntPickupPointDto> firstPage = create2PvzList();
        List<PvzIntPickupPointDto> secondPage = create3PvzList();
        List<PvzIntPickupPointDto> resultPvzList = new ArrayList<>();
        resultPvzList.addAll(firstPage);
        resultPvzList.addAll(secondPage);

        PageableResponse<PvzIntPickupPointDto> pageableResponseFirsPage = new PageableResponse<>();
        pageableResponseFirsPage.setLast(false);
        pageableResponseFirsPage.setContent(firstPage);

        PageableResponse<PvzIntPickupPointDto> pageableResponseSecondPage = new PageableResponse<>();
        pageableResponseSecondPage.setLast(true);
        pageableResponseSecondPage.setContent(secondPage);

        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> firstResponse = new ResponseEntity<>(
                pageableResponseFirsPage,
                HttpStatus.OK
        );

        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> secondResponse = new ResponseEntity<>(
                pageableResponseSecondPage,
                HttpStatus.NOT_FOUND
        );

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("?page=0"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(firstResponse);

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("?page=1"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(secondResponse);

        PvzClient pvzClient = new PvzClient(restTemplate);

        try {
            pvzClient.getPickupPoints(partnerId);
            Assert.fail();
        } catch (ResponseStatusException exception) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetOrders_2PvzWith2OrdersAnd3Orders_5Orders() {

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        List<PvzIntPickupPointDto> pvzIntPickupPointDtoList = create2PvzList();
        List<PvzIntOrdersDto> firstOrders = create2OrdersList();
        List<PvzIntOrdersDto> secondOrders = create2OrdersList();
        List<PvzIntOrdersDto> allOrders = new ArrayList<>();
        allOrders.addAll(firstOrders);
        allOrders.addAll(secondOrders);

        PageableResponse<PvzIntOrdersDto> firstPage = new PageableResponse<>();
        firstPage.setLast(true);
        firstPage.setContent(firstOrders);

        PageableResponse<PvzIntOrdersDto> secondPage = new PageableResponse<>();
        secondPage.setLast(true);
        secondPage.setContent(secondOrders);

        ResponseEntity<PageableResponse<PvzIntOrdersDto>> firstResponse = new ResponseEntity<>(
                firstPage,
                HttpStatus.OK
        );

        ResponseEntity<PageableResponse<PvzIntOrdersDto>> secondResponse = new ResponseEntity<>(
                secondPage,
                HttpStatus.OK
        );

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("/" + pvzIntPickupPointDtoList.get(0).getPvzMarketId() + "/"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(firstResponse);

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("/" + pvzIntPickupPointDtoList.get(1).getPvzMarketId() + "/"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(secondResponse);

        PvzClient pvzClient = new PvzClient(restTemplate);

        Assert.assertEquals(
                pvzClient.getOrders(pvzIntPickupPointDtoList),
                allOrders
        );

    }

    @Test
    public void testGetOrders_HttpStatus404_ResponseStatusException() {

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        List<PvzIntPickupPointDto> pvzIntPickupPointDtoList = create2PvzList();
        List<PvzIntOrdersDto> firstOrders = create2OrdersList();
        List<PvzIntOrdersDto> secondOrders = create2OrdersList();
        List<PvzIntOrdersDto> allOrders = new ArrayList<>();
        allOrders.addAll(firstOrders);
        allOrders.addAll(secondOrders);

        PageableResponse<PvzIntOrdersDto> firstPage = new PageableResponse<>();
        firstPage.setLast(true);
        firstPage.setContent(firstOrders);

        PageableResponse<PvzIntOrdersDto> secondPage = new PageableResponse<>();
        secondPage.setLast(true);
        secondPage.setContent(secondOrders);

        ResponseEntity<PageableResponse<PvzIntOrdersDto>> firstResponse = new ResponseEntity<>(
                firstPage,
                HttpStatus.OK
        );

        ResponseEntity<PageableResponse<PvzIntOrdersDto>> secondResponse = new ResponseEntity<>(
                secondPage,
                HttpStatus.NOT_FOUND
        );

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("/" + pvzIntPickupPointDtoList.get(0).getPvzMarketId() + "/"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(firstResponse);

        Mockito
                .when(restTemplate.exchange(
                        Mockito.contains("/" + pvzIntPickupPointDtoList.get(1).getPvzMarketId() + "/"),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(secondResponse);

        PvzClient pvzClient = new PvzClient(restTemplate);

        try {
            pvzClient.getOrders(pvzIntPickupPointDtoList);
            Assert.fail();
        } catch (ResponseStatusException exception) {
            Assert.assertTrue(true);
        }

    }

    private List<PvzIntOrdersDto> create2OrdersList() {
        List<PvzIntOrdersDto> pvzIntOrdersDtoList = new ArrayList<>();
        PvzIntOrdersDto pvzIntOrdersDto;

        pvzIntOrdersDto = new PvzIntOrdersDto();
        pvzIntOrdersDto.setId(1L);
        pvzIntOrdersDto.setPickupPointId(101L);
        pvzIntOrdersDto.setDeliveryDate(new Date());
        pvzIntOrdersDto.setPaymentType("PREPAID");
        pvzIntOrdersDto.setStatus("CREATED");
        pvzIntOrdersDto.setTotalPrice(228.0);
        pvzIntOrdersDtoList.add(pvzIntOrdersDto);

        pvzIntOrdersDto = new PvzIntOrdersDto();
        pvzIntOrdersDto.setId(2L);
        pvzIntOrdersDto.setPickupPointId(101L);
        pvzIntOrdersDto.setDeliveryDate(new Date());
        pvzIntOrdersDto.setPaymentType("PREPAID");
        pvzIntOrdersDto.setStatus("CREATED");
        pvzIntOrdersDto.setTotalPrice(5151.0);
        pvzIntOrdersDtoList.add(pvzIntOrdersDto);

        return pvzIntOrdersDtoList;
    }

    private List<PvzIntOrdersDto> create3OrdersList() {
        List<PvzIntOrdersDto> pvzIntOrdersDtoList = new ArrayList<>();
        PvzIntOrdersDto pvzIntOrdersDto;

        pvzIntOrdersDto = new PvzIntOrdersDto();
        pvzIntOrdersDto.setId(3L);
        pvzIntOrdersDto.setPickupPointId(102L);
        pvzIntOrdersDto.setDeliveryDate(new Date());
        pvzIntOrdersDto.setPaymentType("PREPAID");
        pvzIntOrdersDto.setStatus("CREATED");
        pvzIntOrdersDto.setTotalPrice(42.42);
        pvzIntOrdersDtoList.add(pvzIntOrdersDto);

        pvzIntOrdersDto = new PvzIntOrdersDto();
        pvzIntOrdersDto.setId(4L);
        pvzIntOrdersDto.setPickupPointId(102L);
        pvzIntOrdersDto.setDeliveryDate(new Date());
        pvzIntOrdersDto.setPaymentType("PREPAID");
        pvzIntOrdersDto.setStatus("CREATED");
        pvzIntOrdersDto.setTotalPrice(69.0);
        pvzIntOrdersDtoList.add(pvzIntOrdersDto);

        pvzIntOrdersDto = new PvzIntOrdersDto();
        pvzIntOrdersDto.setId(5L);
        pvzIntOrdersDto.setPickupPointId(102L);
        pvzIntOrdersDto.setDeliveryDate(new Date());
        pvzIntOrdersDto.setPaymentType("PREPAID");
        pvzIntOrdersDto.setStatus("CREATED");
        pvzIntOrdersDto.setTotalPrice(21.0);
        pvzIntOrdersDtoList.add(pvzIntOrdersDto);

        return pvzIntOrdersDtoList;
    }

    private List<PvzIntPickupPointDto> create2PvzList() {
        List<PvzIntPickupPointDto> pvzIntPickupPointDtoList = new ArrayList<>();
        PvzIntPickupPointDto pvzIntPickupPointDto;

        pvzIntPickupPointDto = new PvzIntPickupPointDto();
        pvzIntPickupPointDto.setId(1);
        pvzIntPickupPointDto.setPvzMarketId(101);
        pvzIntPickupPointDto.setName("first");
        pvzIntPickupPointDto.setActive(true);
        pvzIntPickupPointDtoList.add(pvzIntPickupPointDto);

        pvzIntPickupPointDto = new PvzIntPickupPointDto();
        pvzIntPickupPointDto.setId(2);
        pvzIntPickupPointDto.setPvzMarketId(102);
        pvzIntPickupPointDto.setName("second");
        pvzIntPickupPointDto.setActive(true);
        pvzIntPickupPointDtoList.add(pvzIntPickupPointDto);

        return pvzIntPickupPointDtoList;
    }

    private List<PvzIntPickupPointDto> create3PvzList() {
        List<PvzIntPickupPointDto> pvzIntPickupPointDtoList = new ArrayList<>();
        PvzIntPickupPointDto pvzIntPickupPointDto;

        pvzIntPickupPointDto = new PvzIntPickupPointDto();
        pvzIntPickupPointDto.setId(3);
        pvzIntPickupPointDto.setPvzMarketId(103);
        pvzIntPickupPointDto.setName("third");
        pvzIntPickupPointDto.setActive(true);
        pvzIntPickupPointDtoList.add(pvzIntPickupPointDto);

        pvzIntPickupPointDto = new PvzIntPickupPointDto();
        pvzIntPickupPointDto.setId(4);
        pvzIntPickupPointDto.setPvzMarketId(104);
        pvzIntPickupPointDto.setName("fourth");
        pvzIntPickupPointDto.setActive(true);
        pvzIntPickupPointDtoList.add(pvzIntPickupPointDto);

        pvzIntPickupPointDto = new PvzIntPickupPointDto();
        pvzIntPickupPointDto.setId(5);
        pvzIntPickupPointDto.setPvzMarketId(105);
        pvzIntPickupPointDto.setName("fifth");
        pvzIntPickupPointDto.setActive(true);
        pvzIntPickupPointDtoList.add(pvzIntPickupPointDto);

        return pvzIntPickupPointDtoList;
    }
}

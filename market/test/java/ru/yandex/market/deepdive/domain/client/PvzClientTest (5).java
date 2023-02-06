package ru.yandex.market.deepdive.domain.client;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Description;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import ru.yandex.market.deepdive.domain.client.dto.OrderDto;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

public class PvzClientTest {

    @Description("This test check if PvzClient.getPickupPoints(long partnerId)" +
            "returns correct data, call RestTemplate.exchange(...) required number of times")
    @Test
    public void getPickupPointsWithoutExceptionsTest() {
        RestTemplate templateWithoutException = Mockito.mock(RestTemplate.class);

        PageableResponse<PvzIntPickupPointDto> firstResponse = buildDefaultPageableResponseForPUP(false);
        PageableResponse<PvzIntPickupPointDto> lastResponse = buildDefaultPageableResponseForPUP(true);

        Mockito.when(templateWithoutException.exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(null),
                        Mockito.eq(new ParameterizedTypeReference<PageableResponse<PvzIntPickupPointDto>>() {
                        })))
                .thenReturn(ResponseEntity.ok(firstResponse))
                .thenReturn(ResponseEntity.ok(lastResponse));

        PvzClient client = new PvzClient(templateWithoutException);

        List<PvzIntPickupPointDto> result = client.getPickupPoints(0);

        Mockito.verify(templateWithoutException, Mockito.times(2))
                .exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(null),
                        Mockito.eq(new ParameterizedTypeReference<PageableResponse<PvzIntPickupPointDto>>() {
                        }));

        int expectedResultSize = firstResponse.getContent().size() + lastResponse.getContent().size();

        Assert.assertEquals(result.size(), expectedResultSize);

        if (!result.containsAll(firstResponse.getContent()) || !result.containsAll(lastResponse.getContent())) {
            Assert.fail("Not all elements was taken from PageableResponse");
        }

    }

    @Test(expected = ResponseStatusException.class)
    @Description("This test check if PvzClient.getPickupPoints(long partnerId) " +
            "throws ResponseStatusException, if status code is 400")
    public void getPickupPointsWithExceptionsTest() {
        RestTemplate templateWithException = Mockito.mock(RestTemplate.class);

        Mockito.when(templateWithException.exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(null),
                        Mockito.eq(new ParameterizedTypeReference<PageableResponse<PvzIntPickupPointDto>>() {
                        })))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Expected 2xx status when get response from \"test\"" +
                                ", but found " + HttpStatus.BAD_REQUEST));

        PvzClient client = new PvzClient(templateWithException);

        client.getPickupPoints(0);
    }

    private PageableResponse<PvzIntPickupPointDto> buildDefaultPageableResponseForPUP(
            boolean isLastPage) {
        PageableResponse<PvzIntPickupPointDto> response = new PageableResponse<>();
        response.setLast(isLastPage);
        List<PvzIntPickupPointDto> content = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            PvzIntPickupPointDto pointDto = new PvzIntPickupPointDto();
            pointDto.setId(i);
            pointDto.setName(String.valueOf(i));
            pointDto.setActive(false);
            content.add(pointDto);
        }
        response.setContent(content);
        return response;
    }

    @Description("This test check if PvzClient.getOrders(long partnerId)" +
            "returns correct data, call RestTemplate.exchange(...) required number of times")
    @Test
    public void getOrdersWithoutExceptionsTest() {
        RestTemplate templateWithoutException = Mockito.mock(RestTemplate.class);

        PageableResponse<OrderDto> firstResponse =
                buildDefaultPageableResponseForOrder(true, 1);
        PageableResponse<OrderDto> lastResponse =
                buildDefaultPageableResponseForOrder(true, 2);

        Mockito.when(templateWithoutException.exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(null),
                        Mockito.eq(new ParameterizedTypeReference<PageableResponse<OrderDto>>() {
                        })))
                .thenReturn(ResponseEntity.ok(firstResponse))
                .thenReturn(ResponseEntity.ok(lastResponse));

        PvzClient client = new PvzClient(templateWithoutException);

        PvzIntPickupPointDto pointDto1 = new PvzIntPickupPointDto();
        pointDto1.setPvzMarketId(1);
        PvzIntPickupPointDto pointDto2 = new PvzIntPickupPointDto();
        pointDto2.setPvzMarketId(2);

        List<OrderDto> result = client.getOrders(List.of(pointDto1, pointDto2));

        Mockito.verify(templateWithoutException, Mockito.times(2))
                .exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(null),
                        Mockito.eq(new ParameterizedTypeReference<PageableResponse<OrderDto>>() {
                        }));

        int expectedResultSize = firstResponse.getContent().size() + lastResponse.getContent().size();

        Assert.assertEquals(result.size(), expectedResultSize);

        if (!result.containsAll(firstResponse.getContent()) || !result.containsAll(lastResponse.getContent())) {
            Assert.fail("Not all elements was taken from PageableResponse");
        }

    }

    @Test(expected = ResponseStatusException.class)
    @Description("This test check if PvzClient.getOrders(long partnerId) " +
            "throws ResponseStatusException, if status code is 400")
    public void getOrdersWithExceptionsTest() {
        PvzIntPickupPointDto pointDto1 = new PvzIntPickupPointDto();
        pointDto1.setPvzMarketId(1);
        PvzIntPickupPointDto pointDto2 = new PvzIntPickupPointDto();
        pointDto2.setPvzMarketId(2);

        RestTemplate templateWithException = Mockito.mock(RestTemplate.class);

        Mockito.when(templateWithException.exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(null),
                        Mockito.eq(new ParameterizedTypeReference<PageableResponse<OrderDto>>() {
                        })))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Expected 2xx status when get response from \"test\"" +
                                ", but found " + HttpStatus.BAD_REQUEST));

        PvzClient client = new PvzClient(templateWithException);

        client.getOrders(List.of(pointDto1, pointDto2));
    }

    private PageableResponse<OrderDto> buildDefaultPageableResponseForOrder(
            boolean isLastPage, long pvzMarketId) {
        PageableResponse<OrderDto> response = new PageableResponse<>();
        response.setLast(isLastPage);
        List<OrderDto> content = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            OrderDto pointDto = new OrderDto();
            pointDto.setId(i);
            pointDto.setPvzMarketId(pvzMarketId);
            pointDto.setStatus("ARRIVED_TO_PICKUP_POINT");
            pointDto.setDeliveryDate(Date.from(Instant.now()));
            pointDto.setPaymentType("CASH");
            pointDto.setTotalPrice(111);
            content.add(pointDto);
        }
        response.setContent(content);
        return response;
    }
}

package ru.yandex.market.deepdive;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.Description;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.OrderToPvzDto;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientTests {

    @InjectMocks
    private PvzClient client;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private final long partnerId = 1;
    private final long pvzMarketId = 1;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(client, "pvzIntUrl", "https://pvz-int.tst.vs.market.yandex.net/");
    }

    @Test
    @Description("Тестируем метод PvzClient.getPickupPoints(). " +
            "Проверяем размерность тела ответа при одной странице ответа.")
    public void getPickupPointsTest() {
        PageableResponse<PvzIntPickupPointDto> response = new PageableResponse<>();
        response.setContent(List.of(new PvzIntPickupPointDto()));
        response.setLast(true);
        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> responseEntity = new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(responseEntity);
        List<PvzIntPickupPointDto> pvzs = client.getPickupPoints(partnerId);
        Assert.assertEquals(pvzs.size(), 1);
    }

    @Test
    @Description("Тестируем метод PvzClient.getPickupPoints(). " +
            "Проверяем размерность тела ответа при нескольких страницах.")
    public void getPickupPointsManyPagesTest() {
        PageableResponse<PvzIntPickupPointDto> responsePage1 = new PageableResponse<>();
        responsePage1.setContent(List.of(new PvzIntPickupPointDto()));
        responsePage1.setLast(false);
        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> responseEntity1 = new ResponseEntity<>(
                responsePage1,
                HttpStatus.OK
        );
        PageableResponse<PvzIntPickupPointDto> responsePage2 = new PageableResponse<>();
        responsePage2.setContent(List.of(new PvzIntPickupPointDto()));
        responsePage2.setLast(true);
        ResponseEntity<PageableResponse<PvzIntPickupPointDto>> responseEntity2 = new ResponseEntity<>(
                responsePage2,
                HttpStatus.OK
        );
        when(restTemplate.exchange(
                eq("https://pvz-int.tst.vs.market.yandex.net/v1/pi/partners/1/pickup-points?page=0&size=200"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(responseEntity1);
        when(restTemplate.exchange(
                eq("https://pvz-int.tst.vs.market.yandex.net/v1/pi/partners/1/pickup-points?page=1&size=200"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(responseEntity2);
        List<PvzIntPickupPointDto> pvzs = client.getPickupPoints(partnerId);
        Assert.assertEquals(pvzs.size(), 2);
    }

    @Test(expected = ResponseStatusException.class)
    @Description("Тестируем метод PvzClient.getPickupPoints(). Проверяем ошибку, когда статус не 200")
    public void getPickupPointsExceptionTest() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(
                        new ResponseEntity(HttpStatus.NOT_FOUND)
                );
        client.getPickupPoints(partnerId);
    }

    @Test
    @Description("Тестируем метод PvzClient.getOrders(). " +
            "Проверяем размерность тела ответа при одной странице ответа.")
    public void getOrdersTest() {
        PageableResponse<OrderToPvzDto> response = new PageableResponse<>();
        response.setContent(List.of(new OrderToPvzDto()));
        response.setLast(true);
        ResponseEntity<PageableResponse<OrderToPvzDto>> responseEntity = new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(responseEntity);
        List<OrderToPvzDto> orders = client.getOrders(pvzMarketId);
        Assert.assertEquals(orders.size(), 1);
    }


    @Test
    @Description("Тестируем метод PvzClient.getOrders(). " +
            "Проверяем размерность тела ответа при нескольких страницах.")
    public void getOrdersManyPagesTest() {
        PageableResponse<OrderToPvzDto> responsePage1 = new PageableResponse<>();
        responsePage1.setContent(List.of(new OrderToPvzDto()));
        responsePage1.setLast(false);
        ResponseEntity<PageableResponse<OrderToPvzDto>> responseEntity1 = new ResponseEntity<>(
                responsePage1,
                HttpStatus.OK
        );
        PageableResponse<OrderToPvzDto> responsePage2 = new PageableResponse<>();
        responsePage2.setContent(List.of(new OrderToPvzDto()));
        responsePage2.setLast(true);
        ResponseEntity<PageableResponse<OrderToPvzDto>> responseEntity2 = new ResponseEntity<>(
                responsePage2,
                HttpStatus.OK
        );
        when(restTemplate.exchange(
                eq("https://pvz-int.tst.vs.market.yandex.net/v1/pi/pickup-points/1/orders?page=0&size=200"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(responseEntity1);
        when(restTemplate.exchange(
                eq("https://pvz-int.tst.vs.market.yandex.net/v1/pi/pickup-points/1/orders?page=1&size=200"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(responseEntity2);
        List<OrderToPvzDto> orders = client.getOrders(pvzMarketId);
        Assert.assertEquals(orders.size(), 2);
    }


    @Test(expected = ResponseStatusException.class)
    @Description("Тестируем метод PvzClient.getOrders(). Проверяем ошибку, когда статус не 200")
    public void getOrdersExceptionTest() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class))
        )
                .thenReturn(
                        new ResponseEntity(HttpStatus.NOT_FOUND)
                );
        client.getOrders(pvzMarketId);
    }

}

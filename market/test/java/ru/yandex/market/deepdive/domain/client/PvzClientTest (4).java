package ru.yandex.market.deepdive.domain.client;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.Description;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import ru.yandex.market.deepdive.configuration.PvzIntServiceProperties;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrderDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PvzClientTest {

    private final List<PvzIntPickupPointDto> pickupPointDtos =
            List.of(
                    pickupPointDto(1, "test1", true),
                    pickupPointDto(2, "test2", false)
            );
    private final List<PvzIntOrderDto> orderDtos =
            List.of(
                    orderDto(1, "test1"),
                    orderDto(2, "test2")
            );

    @InjectMocks
    private PvzClient pvzClient;

    @Mock
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        PvzIntServiceProperties properties = new PvzIntServiceProperties();
        ReflectionTestUtils.setField(pvzClient, "properties", properties);
    }

    @Test
    @Description("Проверить, что клиент получает список ПВЗ, если статус = 200")
    public void testGetPickupPoints_status200() {
        PageableResponse<PvzIntPickupPointDto> pr = pickupPointPageableResponse(pickupPointDtos, true);
        when(
                restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.ok(pr));

        List<PvzIntPickupPointDto> result = pvzClient.getPickupPoints(1);

        assertEquals(pickupPointDtos, result);
    }

    @Test(expected = ResponseStatusException.class)
    @Description("Проверить, что клиент выдает ошибку, если статус != 200")
    public void testGetPickupPoints_statusNot200() {
        when(
                restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.badRequest().build());

        pvzClient.getPickupPoints(1);
    }

    @Test
    @Description("Проверить, что клиент получает список ПВЗ с двух страниц")
    public void testGetPickupPoints_2pages() {
        PageableResponse<PvzIntPickupPointDto> pr1 = pickupPointPageableResponse(pickupPointDtos, false);
        PageableResponse<PvzIntPickupPointDto> pr2 = pickupPointPageableResponse(pickupPointDtos, true);
        when(
                restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.ok(pr1), ResponseEntity.ok(pr2));

        List<PvzIntPickupPointDto> result = pvzClient.getPickupPoints(1);
        List<PvzIntPickupPointDto> expected = new ArrayList<>(pickupPointDtos);
        expected.addAll(pickupPointDtos);
        assertEquals(expected, result);
    }

    @Test
    @Description("Проверить, что клиент получает список заказов к ПВЗ, если статус = 200")
    public void testGetOrders_status200() {
        PageableResponse<PvzIntOrderDto> pr = orderPageableResponse(orderDtos, true);
        when(
                restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.ok(pr));

        List<PvzIntOrderDto> result = pvzClient.getOrders(1);

        assertEquals(orderDtos, result);
    }

    @Test(expected = ResponseStatusException.class)
    @Description("Проверить, что клиент выдает ошибку, если статус != 200")
    public void testGetOrders_statusNot200() {
        when(
                restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.badRequest().build());

        pvzClient.getOrders(1);
    }

    @Test
    @Description("Проверить, что клиент получает список заказов к ПВЗ с двух страниц")
    public void testGetOrders_2pages() {
        PageableResponse<PvzIntOrderDto> pr1 = orderPageableResponse(orderDtos, false);
        PageableResponse<PvzIntOrderDto> pr2 = orderPageableResponse(orderDtos, true);
        when(
                restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.ok(pr1), ResponseEntity.ok(pr2));

        List<PvzIntOrderDto> result = pvzClient.getOrders(1);
        List<PvzIntOrderDto> expected = new ArrayList<>(orderDtos);
        expected.addAll(orderDtos);
        assertEquals(expected, result);
    }

    private PvzIntPickupPointDto pickupPointDto(long id, String name, Boolean active) {
        PvzIntPickupPointDto pvzIntPickupPointDto = new PvzIntPickupPointDto();
        pvzIntPickupPointDto.setId(id);
        pvzIntPickupPointDto.setName(name);
        pvzIntPickupPointDto.setActive(active);

        return pvzIntPickupPointDto;
    }

    private PvzIntOrderDto orderDto(long id, String status) {
        PvzIntOrderDto pvzIntOrderDto = new PvzIntOrderDto();
        pvzIntOrderDto.setId(id);
        pvzIntOrderDto.setStatus("1");

        return pvzIntOrderDto;
    }

    private PageableResponse<PvzIntPickupPointDto> pickupPointPageableResponse(
            List<PvzIntPickupPointDto> pickupPoints,
            boolean last
    ) {
        PageableResponse<PvzIntPickupPointDto> pr = new PageableResponse<>();
        pr.setContent(pickupPoints);
        pr.setLast(last);

        return pr;
    }

    private PageableResponse<PvzIntOrderDto> orderPageableResponse(
            List<PvzIntOrderDto> orders,
            boolean last
    ) {
        PageableResponse<PvzIntOrderDto> pr = new PageableResponse<>();
        pr.setContent(orders);
        pr.setLast(last);

        return pr;
    }

}

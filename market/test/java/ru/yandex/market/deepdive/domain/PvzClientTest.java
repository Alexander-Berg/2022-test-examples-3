package ru.yandex.market.deepdive.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriTemplate;

import ru.yandex.market.deepdive.configuration.IntegrationTestConfiguration;
import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrderDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.deepdive.utils.Utils.extractFileContent;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IntegrationTestConfiguration.class)
@AutoConfigureMockMvc(secure = false)
@DisplayName("Тесты для клиента")
public class PvzClientTest {

    private static final String PICKUP_POINTS_URL = "%sv1/pi/partners/{partnerId}/pickup-points?page=%d&pageSize=%d";

    private static final String ORDERS_URL = "%sv1/pi/pickup-points/{pvzMarketId}/orders?page=%d&pageSize=%d";

    @Value("${pvz-int.api.url:https://pvz-int.tst.vs.market.yandex.net/}")
    private String pvzIntUrl;

    @Value("${pvz-int.partner.id:1001047541}")
    private Long partnerId;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private PvzClient pvzClient;

    @Test
    @DisplayName("Тест получения пустого списка ПВЗ")
    public void nonEmptyTest() {
        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format(PICKUP_POINTS_URL, pvzIntUrl, 0, 30))
                        .expand(partnerId))
        ).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("json/client/empty.json")));

        PageableResponse<PvzIntPickupPointDto> expected = new PageableResponse<PvzIntPickupPointDto>()
                .setContent(new ArrayList<>())
                .setLast(true)
                .setTotalPages(0);

        PageableResponse<PvzIntPickupPointDto> provided = pvzClient.getPickupPointsForPage(partnerId, 0);

        mockRestServiceServer.verify();
        Assertions.assertEquals(expected, provided);
    }

    @Test
    @DisplayName("Не пустой список ПВЗ")
    public void nonEmptyPvzListTest() {
        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format(PICKUP_POINTS_URL, pvzIntUrl, 0, 30))
                        .expand(partnerId))
        ).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("json/client/notEmptyPvzList.json")));

        PageableResponse<PvzIntPickupPointDto> expected = new PageableResponse<PvzIntPickupPointDto>()
                .setContent(List.of(
                        new PvzIntPickupPointDto()
                                .setActive(true)
                                .setName("one")
                                .setId(1)
                                .setPvzMarketId(321L),
                        new PvzIntPickupPointDto()
                                .setActive(true)
                                .setName("three")
                                .setId(3)
                                .setPvzMarketId(421L)
                ))
                .setLast(false)
                .setTotalPages(2);

        PageableResponse<PvzIntPickupPointDto> provided = pvzClient.getPickupPointsForPage(partnerId, 0);

        mockRestServiceServer.verify();
        Assertions.assertEquals(expected, provided);
    }

    @Test
    @DisplayName("Пустой список заказов")
    public void emptyOrdersList() {
        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format(ORDERS_URL, pvzIntUrl, 0, 30))
                        .expand(321))
        ).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("json/client/empty.json")));

        PageableResponse<PvzIntOrderDto> expected = new PageableResponse<PvzIntOrderDto>()
                .setContent(new ArrayList<>())
                .setLast(true)
                .setTotalPages(0);

        PageableResponse<PvzIntOrderDto> provided = pvzClient.getOrders(321, 0);

        mockRestServiceServer.verify();
        Assertions.assertEquals(expected, provided);
    }

    @Test
    @DisplayName("Не пустой список заказов")
    public void notEmptyOrdersList() {
        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format(ORDERS_URL, pvzIntUrl, 0, 30))
                        .expand(321))
        ).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("json/client/notEmptyOrdersList.json")));

        PageableResponse<PvzIntOrderDto> expected = new PageableResponse<PvzIntOrderDto>()
                .setContent(List.of(
                        new PvzIntOrderDto()
                                .setDeliveryDate(LocalDate.of(2021, 10, 10))
                                .setId(1)
                                .setPaymentType("CASH")
                                .setTotalPrice(BigDecimal.valueOf(1))
                                .setStatus("READY")
                                .setPickupPointId(17L)
                ))
                .setLast(false)
                .setTotalPages(23);

        PageableResponse<PvzIntOrderDto> provided = pvzClient.getOrders(321L, 0);

        mockRestServiceServer.verify();
        Assertions.assertEquals(expected, provided);
    }
}

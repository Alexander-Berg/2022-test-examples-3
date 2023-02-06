package ru.yandex.market.deepdive.domain.client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriTemplate;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrderDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.deepdive.utils.IntegrationTestUtils.extractFileContent;

public class PvzClientTest extends AbstractTest {

    private static final String PICKUP_POINTS_URL = "v1/pi/partners/{partnerId}/pickup-points";
    private static final String PVZ_ORDERS_URL = "v1/pi/pickup-points/{pvzMarketId}/orders";

    @Value("${pvz-int.api.url:https://pvz-int.tst.vs.market.yandex.net/}")
    private String pvzIntUrl;

    @Value("${pvz-int.partner.id:1001047541}")
    private Long partnerId;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private PvzClient pvzClient;

    @Test
    public void emptyPickupPointPage() {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format("%s%s?page=%d", pvzIntUrl, PICKUP_POINTS_URL, 0))
                        .expand(partnerId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("domain/client/empty_page.json"))
                );

        PageableResponse<PvzIntPickupPointDto> expected = new PageableResponse<PvzIntPickupPointDto>()
                .setContent(new ArrayList<>())
                .setTotalPages(0);

        PageableResponse<PvzIntPickupPointDto> provided = pvzClient
                .getPvzPickupPointsByPartnerIdAndPage(partnerId, 0);

        mockServer.verify();

        Assertions.assertEquals(expected, provided);
    }

    @Test
    public void nonEmptyPickupPointPage() {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format("%s%s?page=%d", pvzIntUrl, PICKUP_POINTS_URL, 0))
                        .expand(partnerId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("domain/client/non_empty_pickup_point_page.json"))
                );

        PageableResponse<PvzIntPickupPointDto> expected = new PageableResponse<PvzIntPickupPointDto>()
                .setContent(List.of(
                        new PvzIntPickupPointDto()
                        .setId(397L)
                        .setName("122")
                        .setActive(false),

                        new PvzIntPickupPointDto()
                        .setId(139L)
                        .setName("Валин ПВЗ")
                        .setActive(false)
                        .setPvzMarketId(1001137159L)
                ))
                .setTotalPages(8);

        PageableResponse<PvzIntPickupPointDto> provided = pvzClient
                .getPvzPickupPointsByPartnerIdAndPage(partnerId, 0);

        mockServer.verify();

        Assertions.assertEquals(expected, provided);
    }

    @Test
    public void emptyOrderPage() {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format("%s%s?page=%d", pvzIntUrl, PVZ_ORDERS_URL, 0))
                        .expand(1)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("domain/client/empty_page.json"))
                );

        PageableResponse<PvzIntOrderDto> expected = new PageableResponse<PvzIntOrderDto>()
                .setContent(new ArrayList<>())
                .setTotalPages(0);

        PageableResponse<PvzIntOrderDto> provided = pvzClient
                .getPvzOrdersByPvzIdAndPage(1, 0);

        mockServer.verify();

        Assertions.assertEquals(expected, provided);
    }

    @Test
    public void nonEmptyOrderPage() {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new UriTemplate(
                        String.format("%s%s?page=%d", pvzIntUrl, PVZ_ORDERS_URL, 1))
                        .expand(1)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(extractFileContent("domain/client/non_empty_order_page.json"))
                );

        PageableResponse<PvzIntOrderDto> expected = new PageableResponse<PvzIntOrderDto>()
                .setContent(List.of(
                        new PvzIntOrderDto()
                                .setId(441523L)
                                .setPickupPointId(126L)
                                .setStatus("CREATED")
                                .setDeliveryDate(LocalDate.of(2021, 4, 28))
                                .setPaymentType("PREPAID")
                                .setTotalPrice(109161.0),

                        new PvzIntOrderDto()
                                .setId(419078L)
                                .setPickupPointId(126L)
                                .setStatus("STORAGE_PERIOD_EXPIRED")
                                .setDeliveryDate(LocalDate.of(2021, 1, 13))
                                .setPaymentType("PREPAID")
                                .setTotalPrice(128996.0),

                        new PvzIntOrderDto()
                                .setId(419077L)
                                .setPickupPointId(126L)
                                .setStatus("STORAGE_PERIOD_EXPIRED")
                                .setDeliveryDate(LocalDate.of(2021, 1, 13))
                                .setPaymentType("PREPAID")
                                .setTotalPrice(128996.0)
                ))
                .setTotalPages(1);

        PageableResponse<PvzIntOrderDto> provided = pvzClient
                .getPvzOrdersByPvzIdAndPage(1, 1);

        mockServer.verify();

        Assertions.assertEquals(expected, provided);
    }
}

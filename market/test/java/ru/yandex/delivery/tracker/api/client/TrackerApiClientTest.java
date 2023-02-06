package ru.yandex.delivery.tracker.api.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.delivery.tracker.api.client.entity.errors.TrackerException;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.model.Partner;
import ru.yandex.market.delivery.tracker.domain.model.ResourceId;
import ru.yandex.market.delivery.tracker.domain.model.request.DeliveryTrackCheckpointFilter;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:client-test.properties")
class TrackerApiClientTest {

    private TrackerApiClient client;
    @Value("${delivery.tracker.client.url}")
    private String host;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        ExternalServiceProperties trackerProperties = new ExternalServiceProperties();
        trackerProperties.setUrl(host);

        ObjectMapper objectMapper = DeliveryTrackerClientFactory.getObjectMapper();
        HttpTemplate httpTemplate = DeliveryTrackerClientFactory.createHttpTemplate(
            trackerProperties,
            v -> null,
            objectMapper
        );
        mockServer = MockRestServiceServer.createServer(((HttpTemplateImpl) httpTemplate).getRestTemplate());

        client = new RestTrackerApiClient(httpTemplate);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    /**
     * Тестирование того что в случае если на запрос трекер ответил ошибкой
     * клиент выбрасывает исключение {@link TrackerException}, содержащее тело ответа
     * и сообщение идентичное сообщению об ошибке, которое вернул бэкенд
     */
    @Test
    void processError() {
        TrackerException trackerException = assertThrows(TrackerException.class, () -> {
            mockServer.expect(requestTo(host + "/services"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                    withServerError()
                        .body(extractFileContent("response/error.json"))
                        .contentType(MediaType.APPLICATION_JSON)
                );

            client.getAvailibleDeliveryServices();
            mockServer.verify();
        });
        assertEquals("Text message", trackerException.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, trackerException.getStatusCode());
    }

    /**
     * Тестирование успешного обращения к ручке GET /sources/{sourceCode}/tracks/{trackCode}
     */
    @Test
    void getSourceTrackMeta() throws Exception {
        mockServer.expect(requestTo(host + "/sources/aftership/tracks/track-code/meta"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/track-meta.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        DeliveryTrackMeta meta = client.getDeliveryTrackMetaByCode("aftership", "track-code");

        assertNotNull(meta);
        assertEquals(25006, (long) meta.getId());
        assertEquals("123123", meta.getEntityId());

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        assertEquals(format.parse("2017-05-29 16:55:24"), meta.getStartDate());
        assertEquals(format.parse("2017-05-29 17:00:00"), meta.getLastUpdatedDate());
        assertEquals(format.parse("2017-05-29 16:55:24"), meta.getNextRequestDate());
    }

    /**
     * Параметры запроса кодируются один раз
     * <p>
     * по мотивам https://st.yandex-team.ru/DELIVERY-3794
     */
    @Test
    void urlEncoding() throws Exception {
        mockServer.expect(requestTo(startsWith(host + "/track")))
            .andExpect(queryParam("trackCode", "123456"))
            .andExpect(queryParam("deliveryServiceId", "123"))
            .andExpect(queryParam("consumerId", "1"))
            .andExpect(queryParam("entityId", "4545"))
            .andExpect(queryParam("orderId", "4545"))
            .andExpect(queryParam("estimatedArrivalDateFrom", "2017-08-23"))
            .andExpect(queryParam("estimatedArrivalDateTo", "2017-08-25"))
            .andExpect(queryParam("deliveryType", "0"))
            .andExpect(queryParam("isGlobalOrder", "false"))
            .andExpect(queryParam("entityType", "0"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/track-meta.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        client.registerDeliveryTrack(
            "123456",
            123,
            1,
            "4545",
            LocalDate.of(2017, 8, 23),
            LocalDate.of(2017, 8, 25),
            DeliveryType.DELIVERY,
            false,
            EntityType.ORDER
        );
    }

    /**
     * Тестирования передачи признака глобальности заказа
     */
    @Test
    void pushGlobalOrderTrack() throws Exception {
        mockServer.expect(requestTo(startsWith(host + "/track")))
            .andExpect(queryParam("trackCode", "123456"))
            .andExpect(queryParam("deliveryServiceId", "123"))
            .andExpect(queryParam("consumerId", "1"))
            .andExpect(queryParam("entityId", "4545"))
            .andExpect(queryParam("orderId", "4545"))
            .andExpect(queryParam("estimatedArrivalDateFrom", "2017-08-23"))
            .andExpect(queryParam("estimatedArrivalDateTo", "2017-08-25"))
            .andExpect(queryParam("deliveryType", "0"))
            .andExpect(queryParam("isGlobalOrder", "true"))
            .andExpect(queryParam("entityType", "0"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/track-meta.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        client.registerDeliveryTrack(
            "123456",
            123,
            1,
            "4545",
            LocalDate.of(2017, 8, 23),
            LocalDate.of(2017, 8, 25),
            DeliveryType.DELIVERY,
            true,
            EntityType.ORDER
        );
    }

    @Test
    void registerDeliveryTrackWithoutEntityType() throws Exception {
        mockServer.expect(requestTo(startsWith(host + "/track")))
            .andExpect(queryParam("trackCode", "123456"))
            .andExpect(queryParam("deliveryServiceId", "123"))
            .andExpect(queryParam("consumerId", "1"))
            .andExpect(queryParam("entityId", "4545"))
            .andExpect(queryParam("orderId", "4545"))
            .andExpect(queryParam("estimatedArrivalDateFrom", "2017-08-23"))
            .andExpect(queryParam("estimatedArrivalDateTo", "2017-08-25"))
            .andExpect(queryParam("deliveryType", "0"))
            .andExpect(queryParam("isGlobalOrder", "true"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/track-meta.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        client.registerDeliveryTrack(
            "123456",
            123,
            1,
            "4545",
            LocalDate.of(2017, 8, 23),
            LocalDate.of(2017, 8, 25),
            DeliveryType.DELIVERY,
            true,
            null
        );
    }

    @Test
    void registerDeliveryTrackOldMethod() throws Exception {
        mockServer.expect(requestTo(startsWith(host + "/track")))
            .andExpect(queryParam("trackCode", "123456"))
            .andExpect(queryParam("deliveryServiceId", "123"))
            .andExpect(queryParam("consumerId", "1"))
            .andExpect(queryParam("entityId", "4545"))
            .andExpect(queryParam("orderId", "4545"))
            .andExpect(queryParam("estimatedArrivalDateFrom", "2017-08-23"))
            .andExpect(queryParam("estimatedArrivalDateTo", "2017-08-25"))
            .andExpect(queryParam("deliveryType", "0"))
            .andExpect(queryParam("isGlobalOrder", "true"))
            .andExpect(queryParam("entityType", "0"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/track-meta.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        client.registerDeliveryTrack(
            "123456",
            123,
            1,
            "4545",
            LocalDate.of(2017, 8, 23),
            LocalDate.of(2017, 8, 25),
            DeliveryType.DELIVERY,
            true
        );
    }

    @Test
    void registerDeliveryTrackRequest() throws Exception {
        mockServer.expect(requestTo(startsWith(host + "/track")))
            .andExpect(queryParam("trackCode", "123456"))
            .andExpect(queryParam("deliveryServiceId", "123"))
            .andExpect(queryParam("consumerId", "1"))
            .andExpect(queryParam("entityId", "4545"))
            .andExpect(queryParam("orderId", "4545"))
            .andExpect(queryParam("estimatedArrivalDateFrom", "2017-08-23"))
            .andExpect(queryParam("estimatedArrivalDateTo", "2017-08-25"))
            .andExpect(queryParam("deliveryType", "0"))
            .andExpect(queryParam("isGlobalOrder", "true"))
            .andExpect(queryParam("entityType", "0"))
            .andExpect(queryParam("apiVersion", "DS"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/track-meta.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        client.registerDeliveryTrack(
            DeliveryTrackRequest.builder()
                .trackCode("123456")
                .deliveryServiceId(123)
                .consumerId(1)
                .entityId("4545")
                .estimatedArrivalDateFrom(LocalDate.of(2017, 8, 23))
                .estimatedArrivalDateTo(LocalDate.of(2017, 8, 25))
                .deliveryType(DeliveryType.DELIVERY)
                .isGlobalOrder(true)
                .entityType(EntityType.ORDER)
                .apiVersion(ApiVersion.DS)
                .build()
        );
    }

    @Test
    void pushOrdersStatus() {

        Long partnerId = 142L;
        String orderId = "120";
        String trackCode = "A120";

        mockServer.expect(requestTo(host + "/orders/status/push"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.partners[0].id").value(partnerId))
            .andExpect(jsonPath("$.ordersId[0].orderId").value(orderId))
            .andExpect(jsonPath("$.ordersId[0].trackCode").value(trackCode))
            .andRespond(withSuccess());

        Partner partner = new Partner(partnerId);
        ResourceId resourceId = new ResourceId(orderId, trackCode);

        client.pushOrdersStatusesChanged(
            Collections.singletonList(partner),
            Collections.singletonList(resourceId)
        );
    }

    @Test
    void getTracksCheckpoints() throws IOException {
        mockServer.expect(requestTo(host + "/track/checkpoints"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"trackIds\": [1, 2]}"))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/checkpoints.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        Calendar calendar = new GregorianCalendar();
        calendar.set(2018, Calendar.JANUARY, 1, 0, 0, 0);

        List<DeliveryTrackCheckpoint> actual = client.getDeliveryTracksCheckpoints(
            DeliveryTrackCheckpointFilter.builder().trackIds(ImmutableSet.of(1L, 2L)).build()
        );
        List<DeliveryTrackCheckpoint> expected = ImmutableList.of(
            new DeliveryTrackCheckpoint()
                .setId(1)
                .setTrackId(1)
                .setCountry("country")
                .setCity("city")
                .setLocation("location")
                .setMessage("message")
                .setCheckpointStatus(CheckpointStatus.IN_TRANSIT)
                .setZipCode("zip code")
                .setDeliveryCheckpointStatus(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION)
                .setEntityType(EntityType.ORDER),
            new DeliveryTrackCheckpoint()
                .setId(2)
                .setTrackId(2)
                .setCountry("country")
                .setCity("city")
                .setLocation("location")
                .setMessage("message")
                .setCheckpointStatus(CheckpointStatus.IN_TRANSIT)
                .setZipCode("zip code")
                .setDeliveryCheckpointStatus(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION)
        );

        assertThat(actual).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    void startMultipleTracks() throws IOException {
        mockServer.expect(requestTo(host + "/track/start"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/start-tracks-request.json")))
            .andRespond(withSuccess());

        client.startMultipleTracks(Set.of(1L, 2L), 3);
    }

    private String extractFileContent(String relativePath) throws IOException {
        return IOUtils.toString(
            getSystemResourceAsStream(relativePath),
            StandardCharsets.UTF_8
        );
    }
}

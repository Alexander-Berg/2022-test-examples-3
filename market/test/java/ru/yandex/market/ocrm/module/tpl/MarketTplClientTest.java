package ru.yandex.market.ocrm.module.tpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.crm.util.Result;
import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.http.test.matcher.PathMatcher;
import ru.yandex.market.jmf.utils.SerializationUtils;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
public class MarketTplClientTest {

    private static final ObjectSerializeService serializerService = SerializationUtils.defaultObjectSerializeService();

    @Mock
    private HttpClientFactory factory;

    @Mock
    private HttpClient httpClient;

    private MarketTplClient tplClient;

    @BeforeEach
    public void setup() {
        setupHttpClient();

        tplClient = new HttpMarketTplClient(
                factory,
                serializerService);
    }

    @Test
    public void getTrackingLink() {
        byte[] rawResponse = ResourceHelpers.getResource("tracking_link_response_sample.json");
        Mockito.when(httpClient.execute(argThat(new PathMatcher("internal/tracking"))))
                .thenReturn(ResponseBuilder.newBuilder().body(rawResponse).build());

        Long orderId = 14543723L;
        Result<TrackingLink, RuntimeException> response = tplClient.getTrackingLink(orderId);
        Assertions.assertTrue(response.isOk());
        final TrackingLink actualLink = response.getValue();
        Assertions.assertEquals("11d1fb6f868848c49139ad15a6bd9658", actualLink.getTrackingId());
        Assertions.assertEquals(orderId, actualLink.getOrderId());
        Assertions.assertEquals("https://m.beru.ru/tracking/11d1fb6f868848c49139ad15a6bd9658", actualLink.getLink());
    }

    @Test
    public void trackingLinkNotFound() {
        Mockito.when(httpClient.execute(argThat(new PathMatcher("internal/tracking"))))
                .thenReturn(ResponseBuilder.newBuilder().code(404).build());

        Long orderId = 14543724L;
        Result<TrackingLink, RuntimeException> response = tplClient.getTrackingLink(orderId);
        Assertions.assertFalse(response.isOk());
        Assertions.assertNotNull(response.getError());
        Assertions.assertTrue(response.getError() instanceof NotFoundException);
    }

    @Test
    public void trackingLinkCannotBeRequestedDueToUnknownError() {
        Mockito.when(httpClient.execute(argThat(new PathMatcher("internal/tracking"))))
                .thenReturn(ResponseBuilder.newBuilder().code(500).build());

        Long orderId = 14543724L;
        Result<TrackingLink, RuntimeException> response = tplClient.getTrackingLink(orderId);
        Assertions.assertFalse(response.isOk());
        Assertions.assertNotNull(response.getError());
        Assertions.assertFalse(response.getError() instanceof NotFoundException);
    }

    @Test
    public void getTracking() {
        String trackingId = "11d1fb6f868848c49139ad15a6bd9658";
        byte[] rawResponse = ResourceHelpers.getResource("tracking_response_sample.json");
        Mockito.when(httpClient.execute(argThat(new PathMatcher("internal/tracking/" + trackingId))))
                .thenReturn(ResponseBuilder.newBuilder().body(rawResponse).build());


        Result<Tracking, RuntimeException> response = tplClient.getTracking(trackingId);
        Assertions.assertTrue(response.isOk());
        final Tracking actualTracking = response.getValue();
        Assertions.assertEquals("11d1fb6f868848c49139ad15a6bd9658", actualTracking.getTrackingId());

        final CourierInfo courierInfo = actualTracking.getCourierInfo();
        Assertions.assertNotNull(courierInfo);
        Assertions.assertEquals("Вася", courierInfo.getName());
        Assertions.assertEquals("Пупкин", courierInfo.getSurname());
        Assertions.assertEquals("+79123456789", courierInfo.getPhone());

        final DeliveryInfo deliveryInfo = actualTracking.getDeliveryInfo();
        Assertions.assertNotNull(deliveryInfo);
        Assertions.assertEquals(
                OffsetDateTime.of(
                        LocalDate.of(2020, 5, 20),
                        LocalTime.of(16, 10),
                        ZoneOffset.UTC), deliveryInfo.getExpectedTimeFrom());
        Assertions.assertEquals(
                OffsetDateTime.of(
                        LocalDate.of(2020, 5, 20),
                        LocalTime.of(17, 10),
                        ZoneOffset.UTC),
                deliveryInfo.getExpectedTimeTo());
    }

    @Test
    public void trackingInfoNotFound() {
        String trackingId = "11d1fb6f868848c49139ad15a6bd9658";
        Mockito.when(httpClient.execute(argThat(new PathMatcher("internal/tracking/" + trackingId))))
                .thenReturn(ResponseBuilder.newBuilder().code(404).build());

        Result<Tracking, RuntimeException> response = tplClient.getTracking(trackingId);
        Assertions.assertFalse(response.isOk());
        Assertions.assertNotNull(response.getError());
        Assertions.assertTrue(response.getError() instanceof NotFoundException);
    }

    @Test
    public void trackingInfoCannotBeRequestedDueToUnknownError() {
        String trackingId = "11d1fb6f868848c49139ad15a6bd9658";
        Mockito.when(httpClient.execute(argThat(new PathMatcher("internal/tracking/" + trackingId))))
                .thenReturn(ResponseBuilder.newBuilder().code(500).build());

        Result<Tracking, RuntimeException> response = tplClient.getTracking(trackingId);
        Assertions.assertFalse(response.isOk());
        Assertions.assertNotNull(response.getError());
        Assertions.assertFalse(response.getError() instanceof NotFoundException);
    }

    private void setupHttpClient() {
        Mockito.when(factory.create(any())).thenReturn(httpClient);
    }
}

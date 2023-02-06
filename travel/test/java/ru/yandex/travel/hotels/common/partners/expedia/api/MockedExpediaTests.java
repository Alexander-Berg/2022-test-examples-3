package ru.yandex.travel.hotels.common.partners.expedia.api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.opentracing.mock.MockTracer;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.logging.IAsyncHttpClientWrapper;
import ru.yandex.travel.commons.logging.masking.LogAwareRequestBuilder;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.base.EmptyResponse;
import ru.yandex.travel.hotels.common.partners.base.exceptions.RetryableHttpException;
import ru.yandex.travel.hotels.common.partners.base.exceptions.UnexpectedHttpStatusCodeException;
import ru.yandex.travel.hotels.common.partners.expedia.ApiVersion;
import ru.yandex.travel.hotels.common.partners.expedia.DefaultExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.ExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.ExpediaClientProperties;
import ru.yandex.travel.hotels.common.partners.expedia.ProfileType;
import ru.yandex.travel.hotels.common.partners.expedia.exceptions.ErrorException;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.CancellationStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.Itinerary;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ItineraryList;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ItineraryReservationRequest;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ReservationResult;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ResumeReservationStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.RoomStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.Error;
import ru.yandex.travel.hotels.common.partners.expedia.model.content.PropertyContent;
import ru.yandex.travel.hotels.common.partners.expedia.model.content.PropertyContentMap;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.PropertyAvailability;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.PropertyAvailabilityList;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.RoomPriceCheck;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.SalesChannel;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingRateStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MockedExpediaTests {
    private ExpediaClient client;
    private final ObjectMapper objectMapper = DefaultExpediaClient.createObjectMapper();

    @Mock
    private AsyncHttpClientWrapper ahc;

    @Before
    public void setUp() {
        ExpediaClientProperties properties = new ExpediaClientProperties();
        properties.setBaseUrl("http://test");
        properties.setDefaultApiVersion(ApiVersion.V3);
        properties.setProfileType(ProfileType.STANDALONE);
        properties.setApiKey("");
        properties.setApiSecret("");
        properties.setHttpReadTimeout(Duration.ofSeconds(5));
        properties.setHttpRequestTimeout(Duration.ofSeconds(5));
        client = new DefaultExpediaClient(properties, ahc, new Retry(new MockTracer()));
    }

    @Test
    public void testPropertyContent() {
        mockResponse("PropertyContentResponse.json", PropertyContentMap.class);
        PropertyContent content = client.getPropertyContentSync("26199", "192.168.1.1", "yaTravel/tests", "testrun");
        assertThat(content.getName()).isEqualTo("Гостиница \"Рэдиссон Славянская\"");
    }

    @Test
    public void testMissingPropertyContent() {
        mockResponse(new UnexpectedHttpStatusCodeException(404, getContent("PropertyContentNotFoundResponse" +
                ".json")));
        CompletableFuture<PropertyContent> content = client.getPropertyContent("404404404", "127.0.0.1", "yaTravel" +
                "/tests", "testrun");
        assertThat(content.join()).isNull();
    }

    @Test
    public void testPriceCheckMach() {
        mockResponse("PriceCheckMatchResponse.json", RoomPriceCheck.class);
        String token =
                "Ql1WAERHXV1QOxULUAtTV11QDAFTWk9dAlZdFAFRUg5LCVEFBhQGWwAHDgUBAwQPDl0TCQFHAAlQVxZrXQBpQFFVWEFSDnVtZydwLHUSEl9MVGpBTRQAD10AVldAVBIeXwdVRkcAXQBBCVZHWFlcBkwHUgAMRUxIYTYVRFUSVjtWRkRaX1sOQkVZEgdrFUlDXFxqZSN2JXkzcUdXVRYKVggFFAYOUAFfXA8MVAQCDR9QAhxTUhASAkNADABDakNLXFELWwFpUFxcAQkFDV4NH1pFEEddX1AaCWZlJhRZBEEOB0YNCwJvAgtZDV9RCQxTHldQAVRYWxZAWwdVVw5LCVdJBQVFUFAHaA9VWwcOVgdQFxVIDFMLDQYDV0URX1cUQAxZBGtCURZdXGttdSt3cS1-fXJCBl9XFVMCZkRKAEZpA0pbFxMNVgJcFl0SE1EDB0EUCAtUbUBXEUtbB1lCAEhYBk9LUAwWEVdGFUALX1QQPhUAQhEPDAs5C1MKAAdXBV4BU1UYUQUFBkxWAVdRSFkHBAAdAVoPAF5dXQJXVQBVFxVWRkoNDFo7UAAJV1UFC1IGBlAYVQQHW08AC1ZSHwxVAgcbVgIDVVICA1FVAAUKF0ZUWAEWbVoNUVhbXQoFRwFUQF4VVkVfUQoERllGBF05QQxcUgRXA1IKGwVXHlUPYgEHFQZyVAcdVSMAUh4LCVJjFxFZDV0SagRcQg8RXVtYVwsSCApbQQRdOVtdWEtHVlgOFlMQXhdXDRBfQUFAbFQNUwELa21HVlxXawJQSlpCC0VMWFwNCQUbQXYEQHFRR3ZUQCcCRCcJRgkHQHVSHQwJQXMDESFyQ3FVQ3VWHSFUEXYhF3FSEl5yQ3AJQHclFCUIQHUGHXMDQFskHB0gAEB1IhF2AUcNUR19BEBxdUR1AxImVxZ8VhMkCRR9VBEOVh1wU0YIc0gVJgoRIHZAJgNDWCQWdgMdJgEdJlQVI3wUJlIdelBLEnACQwxVF3VSRCRVFCZWRidfR3MHRyAFFXsCQHR3ElFSAD4BW1hWDF8QRwNEUA1XW0MMexADIR1UAxdGVFcQCkAdHFYGQwB3CEAHJ0EHURJYQwcRAFQTAXUTBQF9Wl5SElAGQAZ3RAQKRUxFUUFXABxWcRMHCi1RXgN0VlNEAVEdAyBEAXwUEkwWRQlYUks6WgcOBABAQxZQVVVsRFpHBVsFX1UGUR4DCUd6WQdbCksUWFwDDFRWBgMMAFRT";
        RoomPriceCheck check = client.checkPriceSync("26199", "200180001", "238100522", token, "127.0.0.1", "yaTravel" +
                "/tests", "matched");
        assertThat(check).isNotNull();
        assertThat(check.getStatus()).isEqualTo(ShoppingRateStatus.AVAILABLE);
        assertThat(check.getOccupancyPricing()).isNotEmpty();
    }

    @Test
    public void testPriceCheckPriceChanged() {
        mockResponse("PriceCheckPriceChangedResponse.json", RoomPriceCheck.class);
        String token =
                "Ql1WAERHXV1QOxULUAtTV11QDAFTWk9dAlZdFAFRUg5LCVEFBhQGWwAHDgUBAwQPDl0TCQFHAAlQVxZrXQBpQFFVWEFSDnVtZydwLHUSEl9MVGpBTRQAD10AVldAVBIeXwdVRkcAXQBBCVZHWFlcBkwHUgAMRUxIYTYVRFUSVjtWRkRaX1sOQkVZEgdrFUlDXFxqZSN2JXkzcUdXVRYKVggFFAYOUAFfXA8MVAQCDR9QAhxTUhASAkNADABDakNLXFELWwFpUFxcAQkFDV4NH1pFEEddX1AaCWZlJhRZBEEOB0YNCwJvAgtZDV9RCQxTHldQAVRYWxZAWwdVVw5LCVdJBQVFUFAHaA9VWwcOVgdQFxVIDFMLDQYDV0URX1cUQAxZBGtCURZdXGttdSt3cS1-fXJCBl9XFVMCZkRKAEZpA0pbFxMNVgJcFl0SE1EDB0EUCAtUbUBXEUtbB1lCAEhYBk9LUAwWEVdGFUALX1QQPhUAQhEPDAs5C1MKAAdXBV4BU1UYUQUFBkxWAVdRSFkHBAAdAVoPAF5dXQJXVQBVFxVWRkoNDFo7UAAJV1UFC1IGBlAYVQQHW08AC1ZSHwxVAgcbVgIDVVICA1FVAAUKF0ZUWAEWbVoNUVhbXQoFRwFUQF4VVkVfUQoERllGBF05QQxcUgRXA1IKGwVXHlUPYgEHFQZyVAcdVSMAUh4LCVJjFxFZDV0SagRcQg8RXVtYVwsSCApbQQRdOVtdWEtHVlgOFlMQXhdXDRBfQUFAbFQNUwELa21HVlxXawJQSlpCC0VMWFwNCQUbQXYEQHFRR3ZUQCcCRCcJRgkHQHVSHQwJQXMDESFyQ3FVQ3VWHSFUEXYhF3FSEl5yQ3AJQHclFCUIQHUGHXMDQFskHB0gAEB1IhF2AUcNUR19BEBxdUR1AxImVxZ8VhMkCRR9VBEOVh1wU0YIc0gVJgoRIHZAJgNDWCQWdgMdJgEdJlQVI3wUJlIdelBLEnACQwxVF3VSRCRVFCZWRidfR3MHRyAFFXsCQHR3ElFSAD4BW1hWDF8QRwNEUA1XW0MMexADIR1UAxdGVFcQCkAdHFYGQwB3CEAHJ0EHURJYQwcRAFQTAXUTBQF9Wl5SElAGQAZ3RAQKRUxFUUFXABxWcRMHCi1RXgN0VlNEAVEdAyBEAXwUEkwWRQlYUks6WgcOBABAQxZQVVVsRFpHBVsFX1UGUR4DCUd6WQdbCksUWFwDDFRWBgMMAFRT";
        RoomPriceCheck check = client.checkPriceSync("26199", "200180002", "238100522", token, "127.0.0.1", "yaTravel" +
                "/tests", "mismatch");
        assertThat(check).isNotNull();
        assertThat(check.getStatus()).isEqualTo(ShoppingRateStatus.PRICE_CHANGED);
        assertThat(check.getOccupancyPricing()).isNotEmpty();
    }

    @Test
    public void testPriceCheckSoldOut() {
        mockResponse("PriceCheckSoldOutResponse.json", RoomPriceCheck.class);
        String token =
                "Ql1WAERHXV1QOxULUAtTV11QDAFTWk9dAlZdFAFRUg5LCVEFBhQGWwAHDgUBAwQPDl0TCQFHAAlQVxZrXQBpQFFVWEFSDnVtZydwLHUSEl9MVGpBTRQAD10AVldAVBIeXwdVRkcAXQBBCVZHWFlcBkwHUgAMRUxIYTYVRFUSVjtWRkRaX1sOQkVZEgdrFUlDXFxqZSN2JXkzcUdXVRYKVggFFAYOUAFfXA8MVAQCDR9QAhxTUhASAkNADABDakNLXFELWwFpUFxcAQkFDV4NH1pFEEddX1AaCWZlJhRZBEEOB0YNCwJvAgtZDV9RCQxTHldQAVRYWxZAWwdVVw5LCVdJBQVFUFAHaA9VWwcOVgdQFxVIDFMLDQYDV0URX1cUQAxZBGtCURZdXGttdSt3cS1-fXJCBl9XFVMCZkRKAEZpA0pbFxMNVgJcFl0SE1EDB0EUCAtUbUBXEUtbB1lCAEhYBk9LUAwWEVdGFUALX1QQPhUAQhEPDAs5C1MKAAdXBV4BU1UYUQUFBkxWAVdRSFkHBAAdAVoPAF5dXQJXVQBVFxVWRkoNDFo7UAAJV1UFC1IGBlAYVQQHW08AC1ZSHwxVAgcbVgIDVVICA1FVAAUKF0ZUWAEWbVoNUVhbXQoFRwFUQF4VVkVfUQoERllGBF05QQxcUgRXA1IKGwVXHlUPYgEHFQZyVAcdVSMAUh4LCVJjFxFZDV0SagRcQg8RXVtYVwsSCApbQQRdOVtdWEtHVlgOFlMQXhdXDRBfQUFAbFQNUwELa21HVlxXawJQSlpCC0VMWFwNCQUbQXYEQHFRR3ZUQCcCRCcJRgkHQHVSHQwJQXMDESFyQ3FVQ3VWHSFUEXYhF3FSEl5yQ3AJQHclFCUIQHUGHXMDQFskHB0gAEB1IhF2AUcNUR19BEBxdUR1AxImVxZ8VhMkCRR9VBEOVh1wU0YIc0gVJgoRIHZAJgNDWCQWdgMdJgEdJlQVI3wUJlIdelBLEnACQwxVF3VSRCRVFCZWRidfR3MHRyAFFXsCQHR3ElFSAD4BW1hWDF8QRwNEUA1XW0MMexADIR1UAxdGVFcQCkAdHFYGQwB3CEAHJ0EHURJYQwcRAFQTAXUTBQF9Wl5SElAGQAZ3RAQKRUxFUUFXABxWcRMHCi1RXgN0VlNEAVEdAyBEAXwUEkwWRQlYUks6WgcOBABAQxZQVVVsRFpHBVsFX1UGUR4DCUd6WQdbCksUWFwDDFRWBgMMAFRT";
        RoomPriceCheck check = client.checkPriceSync("26199", "200180002", "238100522", token, "127.0.0.1", "yaTravel" +
                "/tests", "sold_out");
        assertThat(check).isNotNull();
        assertThat(check.getStatus()).isEqualTo(ShoppingRateStatus.SOLD_OUT);
    }

    @Test
    public void testReservationSuccessfull() {
        mockResponse("ReservationResponse.json", ReservationResult.class);
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQgHVVtWAFQKVhRcUAZXFFBWXwIbAFYMVUkAAANSWFoCBFUAAAEQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVaAV1nCwANUg8CVlMeTBMPUwkGVwRUFFAPDhMLRV9SVlFIWgURAQ5dQ11UOkNGUUFoAxMNQUYNA1kJRgcWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQsAYFcDHApxVgQdAnJQDBoDUwRuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBA1XC1EBAgxVBQ==";
        ItineraryReservationRequest request = ItineraryReservationRequest.create("yndx-test-run-4",
                "John", "Doe", "email@example.com", "123456",
                "Lva Tolstogo 16", "Moscow", "RU");
        ReservationResult reservation =
                client.reserveItinerarySync(request, token, "127.0.0.1", "yaTravel/Test", "");
        assertThat(reservation.getItineraryId()).isNotEmpty();
        assertThat(reservation.getLinks()).isNotNull();
        assertThat(reservation.getLinks().getResume()).isNotNull();
        assertThat(reservation.getLinks().getCancel()).isNotNull();
        assertThat(reservation.getLinks().getRetrieve()).isNotNull();
    }

    @Test
    public void testReservationRoomsUnavailable() {
        mockResponse("ReservationRoomsUnavailableResponse.json", Error.class);
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQgHVVtWAFQKVhRcUAZXFFBWXwIbAFYMVUkAAANSWFoCBFUAAAEQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVaAV1nCwANUg8CVlMeTBMPUwkGVwRUFFAPDhMLRV9SVlFIWgURAQ5dQ11UOkNGUUFoAxMNQUYNA1kJRgcWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQsAYFcDHApxVgQdAnJQDBoDUwRuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBA1XC1EBAgxVBQ==";
        ItineraryReservationRequest request = ItineraryReservationRequest.create("yndx-test-run-4",
                "John", "Doe", "email@example.com", "123456",
                "Lva Tolstogo 16", "Moscow", "RU");
        assertThatExceptionOfType(ErrorException.class)
                .isThrownBy(
                        () -> client.reserveItinerarySync(request, token, "127.0.0.1", "yaTravel/Test", ""))
                .matches(e -> e.getError().getType().equals(Error.ROOMS_UNAVAILABLE));
    }

    @Test
    public void testReservationPriceMismatch() {
        mockResponse(new UnexpectedHttpStatusCodeException(409,
                getContent("ReservationRoomsPriceMismatchResponse.json")));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQgHVVtWAFQKVhRcUAZXFFBWXwIbAFYMVUkAAANSWFoCBFUAAAEQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVaAV1nCwANUg8CVlMeTBMPUwkGVwRUFFAPDhMLRV9SVlFIWgURAQ5dQ11UOkNGUUFoAxMNQUYNA1kJRgcWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQsAYFcDHApxVgQdAnJQDBoDUwRuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBA1XC1EBAgxVBQ==";
        ItineraryReservationRequest request = ItineraryReservationRequest.create("yndx-test-run-4",
                "John", "Doe", "email@example.com", "123456",
                "Lva Tolstogo 16", "Moscow", "RU");
        assertThatExceptionOfType(ErrorException.class)
                .isThrownBy(
                        () -> client.reserveItinerarySync(request, token, "127.0.0.1", "yaTravel/Test", ""))
                .matches(e -> e.getError().getType().equals(Error.PRICE_MISMATCH));
    }

    @Test
    public void testReservationInvalidInput() {
        mockResponse("ReservationRoomsInvalidInput.json", Error.class);
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQgHVVtWAFQKVhRcUAZXFFBWXwIbAFYMVUkAAANSWFoCBFUAAAEQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVaAV1nCwANUg8CVlMeTBMPUwkGVwRUFFAPDhMLRV9SVlFIWgURAQ5dQ11UOkNGUUFoAxMNQUYNA1kJRgcWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQsAYFcDHApxVgQdAnJQDBoDUwRuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBA1XC1EBAgxVBQ==";
        ItineraryReservationRequest request = ItineraryReservationRequest.create("yndx-test-run-4",
                "", "", "email@ example.com", "12$3456",
                "Lva Tolstogo 16", "Moscow", "RU");
        assertThatExceptionOfType(ErrorException.class)
                .isThrownBy(
                        () -> client.reserveItinerarySync(request, token, "127.0.0.1", "yaTravel/Test", ""))
                .matches(e -> e.getError().getType().equals(Error.INVALID_INPUT));
    }

    @Test
    public void testReservationInternalErrorIsRetryable() {
        mockResponse(new RetryableHttpException(500, getContent("InternalServerError.json")));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQgHVVtWAFQKVhRcUAZXFFBWXwIbAFYMVUkAAANSWFoCBFUAAAEQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVaAV1nCwANUg8CVlMeTBMPUwkGVwRUFFAPDhMLRV9SVlFIWgURAQ5dQ11UOkNGUUFoAxMNQUYNA1kJRgcWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQsAYFcDHApxVgQdAnJQDBoDUwRuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBA1XC1EBAgxVBQ==";
        ItineraryReservationRequest request = ItineraryReservationRequest.create("yndx-test-run-4",
                "", "", "email@ example.com", "12$3456",
                "Lva Tolstogo 16", "Moscow", "RU");
        CompletableFuture<ReservationResult> reservationFuture =
                client.reserveItinerary(request, token, "127.0.0.1", "yaTravel/Test", "");
        assertThatThrownBy(reservationFuture::join).hasCauseInstanceOf(RetryableHttpException.class);
    }

    @Test
    public void testResumeReservation() {
        mockResponse(new EmptyResponse(204));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9720499609072";
        ResumeReservationStatus res =
                client.resumeItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isEqualTo(ResumeReservationStatus.SUCCESS);
    }

    @Test
    public void testResumeReservation202() {
        mockResponse(new EmptyResponse(202));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9720499609072";
        ResumeReservationStatus res =
                client.resumeItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isEqualTo(ResumeReservationStatus.UNKNOWN);
    }

    @Test
    public void testResumeReservationNotFound() {
        mockResponse(new UnexpectedHttpStatusCodeException(404, getContent("HoldBookingNotFound.json")));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9720499609072";
        ResumeReservationStatus res =
                client.resumeItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isEqualTo(ResumeReservationStatus.NOT_FOUND);
    }

    @Test
    public void testGetConfirmedReservation() {
        mockResponse("ConfirmedReservationResponse.json", Itinerary.class);
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9720499609072";
        Itinerary res =
                client.getItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isNotNull();
        assertThat(res.getRooms()).hasSize(1);
        assertThat(res.getItineraryId()).isEqualTo(itineraryId);
        assertThat(res.getRooms().get(0).getStatus()).isEqualTo(RoomStatus.BOOKED);
    }

    @Test
    public void testGetHoldReservation() {
        mockResponse("GetHoldReservationResponse.json", Itinerary.class);
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9992222801366";
        Itinerary res =
                client.getItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isNotNull();
        assertThat(res.getRooms()).isNull();
    }

    @Test
    public void testGetHoldReservationByAffiliateId() {
        mockResponse("GetHoldReservationResponse.json", ItineraryList.class);
        Itinerary res =
                client.getItineraryByAffiliateIdSync("yndx-test-run-4", "email@example.com", "127.0.0.1", "yaTravel" +
                        "/test", "");
        assertThat(res).isNotNull();
        assertThat(res.getRooms()).isNull();
    }

    @Test
    public void testGetHoldReservationNotFound() {
        mockResponse(new UnexpectedHttpStatusCodeException(404, getContent("HoldBookingNotFound.json")));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9992222801366";
        Itinerary res =
                client.getItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isNull();
    }

    @Test
    public void testCancelReservation() {
        mockResponse(new EmptyResponse(204));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9992222801366";
        CancellationStatus res =
                client.cancelItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isEqualTo(CancellationStatus.SUCCESS);
    }

    @Test
    public void testCancelReservation202() {
        mockResponse(new EmptyResponse(202));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9992222801366";
        CancellationStatus res =
                client.cancelItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isEqualTo(CancellationStatus.UNKNOWN);
    }

    @Test
    public void testCancelReservationNotFound() {
        mockResponse(new UnexpectedHttpStatusCodeException(404, getContent("HoldBookingNotFound.json")));
        String token =
                "QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhSBw9XVF8DBBQCBwRRFFAHVFUbAFIBBkkAWgIBW1QEVgMEBVAQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWU4JURwFXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWRlUUB5XWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxaDBgCUx4GWQ1DXAZmExVcS2pTEVcTQVtVVFUXBhIWXAJRFEFfVwJqFwtAEQJUBBBVQg9SH0REXlBTHlNdUgFfQ1dBEkJXXFBHaxcAQUoMX1hqUQIFVQcBAFRRVwAVUlUCURUGUQhfGAcFAFxIAVYFD1YFAAcIB1ULFkZWF0RRCQxrCFQODwUOAgFZVQ1MAlcAAktXVlABHwRQVgQZDFIAUwELDApVAwJXREYQDEFREBFIalpdDgVUBl0PH18EWlxBVRJQXWZEC1hdDAFTBQwdVQAZVQYxUwNBViQCWUYLIgQDSwJWCW4eB1hGWhdGH2oGCVMDBTcxElYGVmoHUhVSFF1JEVwOX1wJThJyCBJwUUYmBx0mAkBzUhEKA0dxUB0BBUB3AERzdRIgVRZ6VhMiCRR7JxFyVR0MIUZ0AEZyIR1wUREnUhYiUEALdBgdIAkdICUVJQkUWlIdfFNDdXEWIgRBcANHJVZAc1JDJ1RDWgUSJlMXCHsbQHUHEXJ1QSVSEQ9xQHxVECAIHCYJQyR9HHEERnpVHEN3BBxcUhEgCUF2VhZyCUB3XUFxU0RzDkd3AEN0cBJUUldpUF9bUQtTEEZVFV9XXwgQASZABXtAAgRETQdWRA1CShJTAUYLdVVEBnsXUwsVXB9UEgtXFlByEwZUeA1XURUBAhABJxIKVBZNEVUWC1McAiMdUwoqXA9VdgMHFwcHF1IiEFdwExJEFkRfXVcTb1gGXwRWRXJcBwZaRkZUDgZTBlUFCQ9XBg4=";
        String itineraryId = "9992222801366";
        CancellationStatus res =
                client.cancelItinerarySync(itineraryId, token, "127.0.0.1", "yaTravel/test", "");
        assertThat(res).isEqualTo(CancellationStatus.NOT_FOUND);
    }

    @Test
    public void testFindAvailabilitites() {
        mockResponse("AvailabilityCheckResponse.json", PropertyAvailabilityList.class);
        Map<String, PropertyAvailability> res = client.findAvailabilitiesSync(Collections.singletonList("26199"),
                LocalDate.of(2019, 3, 28),
                LocalDate.of(2019, 3, 29),
                "2", "USD", "127.0.0.1", "", null,
                SalesChannel.CACHE, true);
        assertThat(res).isNotEmpty();
        assertThat(res).containsKeys("26199");
        assertThat(res.get("26199").getRooms()).hasSize(9);
    }

    @Test
    public void testFindNoAvailabilitites() {
        mockResponse(new UnexpectedHttpStatusCodeException(404, getContent("NoAvailabilityCheckResponse.json")));
        Map<String, PropertyAvailability> res = client.findAvailabilitiesSync(Collections.singletonList("2"),
                LocalDate.of(2019, 3, 28),
                LocalDate.of(2019, 3, 29),
                "2", "USD", "127.0.0.1", "", null,
                SalesChannel.CACHE, true);
        assertThat(res).isEmpty();
    }

    @SneakyThrows
    private String getContent(String resourceName) {
        return Resources.toString(Resources.getResource("expediaResponses/" + resourceName),
                Charset.defaultCharset());
    }

    private <T> void mockResponse(String resourceName, Class<T> responseClass) {
        T response;
        try {
            response = objectMapper.readValue(getContent(resourceName),
                    responseClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (responseClass == Error.class) {
            mockResponse(new ErrorException((Error) response));
        } else {
            mockResponse(response);
        }
    }

    private <T> void mockResponse(T response) {
        CompletableFuture<T> mockedFuture;
        if (response instanceof Exception) {
            mockedFuture = CompletableFuture.failedFuture((Exception) response);
        } else {
            mockedFuture = CompletableFuture.completedFuture(response);
        }
        when(ahc.executeRequest(any(LogAwareRequestBuilder.class), any(), any(),
                any(IAsyncHttpClientWrapper.ResponseParser.class))).thenReturn(mockedFuture);
    }


}

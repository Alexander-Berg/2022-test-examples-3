package ru.yandex.travel.hotels.common.partners.bnovo.api;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.mock.MockTracer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.base.exceptions.UnexpectedHttpStatusCodeException;
import ru.yandex.travel.hotels.common.partners.bnovo.BNovoClient;
import ru.yandex.travel.hotels.common.partners.bnovo.BNovoClientProperties;
import ru.yandex.travel.hotels.common.partners.bnovo.BNovoUidMap;
import ru.yandex.travel.hotels.common.partners.bnovo.DefaultBNovoClient;
import ru.yandex.travel.hotels.common.partners.bnovo.DefaultBNovoClientWithPublicRemapping;
import ru.yandex.travel.hotels.common.partners.bnovo.exceptions.AlreadyCancelledException;
import ru.yandex.travel.hotels.common.partners.bnovo.exceptions.SoldOutException;
import ru.yandex.travel.hotels.common.partners.bnovo.model.Booking;
import ru.yandex.travel.hotels.common.partners.bnovo.model.BookingJson;
import ru.yandex.travel.hotels.common.partners.bnovo.model.BookingStatusId;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelStayMap;
import ru.yandex.travel.hotels.common.partners.bnovo.model.Offer;
import ru.yandex.travel.hotels.common.partners.bnovo.model.PriceLosRequest;
import ru.yandex.travel.hotels.common.partners.bnovo.model.RatePlan;
import ru.yandex.travel.hotels.common.partners.bnovo.model.RoomType;
import ru.yandex.travel.hotels.common.partners.bnovo.model.Stay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClientTests {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("fixtures/bnovo"));
    private BNovoClient defaultClient;
    private BNovoClient defaultClientWithRemapping;

    @Before
    public void prepareClient() {
        AsyncHttpClient ahcClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("ahcPool")
                .build());

        BNovoUidMap uidMap = originalId -> {
            if (originalId.equals("1024")) {
                return "8d34fd45-ceae-4b8d-a9fd-5422d1b17c78";
            } else {
                return null;
            }
        };
        BNovoClientProperties properties = new BNovoClientProperties();
        properties.setBaseUrl(String.format("http://localhost:%s/v1/api", wireMockRule.port()));
        properties.setPrivateApiBaseUrl(String.format("http://localhost:%s/v1/api", wireMockRule.port()));
        properties.setPricesLosApiBaseUrl(String.format("http://localhost:%s/v1/api", wireMockRule.port()));
        properties.setEnableRetries(true);
        properties.setHttpReadTimeout(Duration.ofSeconds(15));
        properties.setHttpRequestTimeout(Duration.ofSeconds(20));
        properties.setUsername("foo");
        properties.setPassword("bar");
        properties.setTokenValidityDuration(Duration.ofMinutes(10));
        AsyncHttpClientWrapper wrapper = new AsyncHttpClientWrapper(ahcClient, LoggerFactory.getLogger("test"),
                "bnovo", new MockTracer(), DefaultBNovoClient.getMethods().getNames());
        defaultClient = new DefaultBNovoClient(wrapper, properties, new Retry(new MockTracer()));
        defaultClientWithRemapping = new DefaultBNovoClientWithPublicRemapping(wrapper, properties, uidMap,
                new Retry(new MockTracer()));
    }

    @Test
    public void testRatePlans() {
        Map<Long, RatePlan> rpMap = defaultClient.getRatePlansSync(3056, null);
        assertThat(rpMap).hasSize(5);
    }

    @Test
    public void testRoomTypes() {
        Map<Long, RoomType> rtMap = defaultClient.getRoomTypesSync(3056, null);
        assertThat(rtMap).hasSize(5);
    }

    @Test
    public void testPricesLos() {
        PriceLosRequest request = PriceLosRequest.builder()
                .account(3056L)
                .adults(2)
                .checkinFrom(LocalDate.parse("2020-03-21"))
                .nights(2)
                .build();
        HotelStayMap resp = defaultClient.getPricesSync(request);
        assertThat(resp).hasSize(1);
        assertThat(resp.get("3056")).hasSize(1);
        assertThat(resp.get("3056").get(LocalDate.parse("2020-03-21"))).isNotNull();
    }

    @Test
    public void testGetAllHotels() {
        var allHotels = defaultClient.getAllHotelsSync();
        assertThat(allHotels).hasSize(1);
        assertThat(allHotels).containsKey(3056L);
        assertThat(allHotels.get(3056L).getName()).isEqualTo("Яндекс.Отель");
        assertThat(allHotels.get(3056L).getCheckinInstantForDate(LocalDate.now()))
                .isEqualTo(LocalDate.now().atTime(14, 0).atZone(ZoneId.of("Europe/Moscow")).toInstant());
    }

    @Test
    public void testGetHotelByAccountId() {
        var hotel = defaultClient.getHotelByAccountIdSync(3056L, null);
        assertThat(hotel).isNotNull();
        assertThat(hotel.getName()).isEqualTo("Яндекс.Отель");
        assertThat(hotel.getCheckinInstantForDate(LocalDate.now()))
                .isEqualTo(LocalDate.now().atTime(14, 0).atZone(ZoneId.of("Europe/Moscow")).toInstant());
    }

    @Test
    public void testGetHotelByUUD() {
        var hotel = defaultClient.getHotelByUIDSync("8d34fd45-ceae-4b8d-a9fd-5422d1b17c78");
        assertThat(hotel).isNotNull();
        assertThat(hotel.getName()).isEqualTo("Принцесса Элиза");
        assertThat(hotel.getUid()).isEqualTo("8d34fd45-ceae-4b8d-a9fd-5422d1b17c78");
        assertThat(hotel.getCheckinInstantForDate(LocalDate.now()))
                .isEqualTo(LocalDate.now().atTime(14, 0).atZone(ZoneId.of("Europe/Kaliningrad")).toInstant());
    }

    @Test
    public void testGetForbiddenHotel() {
        assertThatExceptionOfType(UnexpectedHttpStatusCodeException.class)
                .isThrownBy(() -> defaultClient.getHotelByAccountIdSync(1024L, null))
                .withMessage("Unexpected HTTP status code '403'");
    }

    @Test
    public void testGetForbiddenHotelWithMock() {
        var hotel = defaultClientWithRemapping.getHotelByAccountIdSync(1024L, null);
        assertThat(hotel.getName()).isEqualTo("Принцесса Элиза");
        assertThat(hotel.getUid()).isEqualTo("8d34fd45-ceae-4b8d-a9fd-5422d1b17c78");
    }

    @Test
    public void testCreateBooking() {
        Stay stay = Stay.builder()
                .checkin(LocalDate.of(2020, 3, 19))
                .currency("RUB")
                .nights(2)
                .rate(Offer.builder()
                        .pricesByDate(LocalDate.of(2020, 3, 19), BigDecimal.valueOf(3000))
                        .pricesByDate(LocalDate.of(2020, 3, 20), BigDecimal.valueOf(3000))
                        .price(BigDecimal.valueOf(6000))
                        .planId(11185L)
                        .roomtypeId(33697)
                        .adults(2)
                        .children(0)
                        .build())
                .build();
        BookingJson request = BookingJson.build("unit-test-1", 3056L, stay,
                "John", "Doe", "+7-999-123-45-67", "johndow@example.com", 2, Collections.emptyList());

        var bookingList = defaultClient.createBookingSync(3056L, request);
        assertThat(bookingList).isNotNull();
        assertThat(bookingList.getBookings()).hasSize(1);
        assertThat(bookingList.getBookings().get(0).getNumber()).isEqualTo("PPSUF_180320");
    }

    @Test
    public void testSoldOutOnBooking() {
        Stay stay = Stay.builder()
                .checkin(LocalDate.of(2020, 4, 28))
                .currency("RUB")
                .nights(1)
                .rate(Offer.builder()
                        .pricesByDate(LocalDate.of(2020, 4, 28), BigDecimal.valueOf(5000))
                        .price(BigDecimal.valueOf(5000))
                        .planId(11187L)
                        .roomtypeId(33699)
                        .adults(2)
                        .children(0)
                        .build())
                .build();
        BookingJson request = BookingJson.build("TEST2-YA-6630-1831-4197:0", 3056L, stay,
                "Foo", "BarOne", "828275843574385748", "foo@bar.ru", 2, Collections.emptyList());
        assertThatThrownBy(() -> defaultClient.createBookingSync(3056L, request)).isInstanceOf(SoldOutException.class);
    }

    @Test
    public void testGetBooking() {
        Booking booking = defaultClient.getBookingSync(3056L, "PPSUF_180320");
        assertThat(booking.getOtaId()).isEqualTo("yandex");
        assertThat(booking.getOtaBookingId()).isEqualTo("unit-test-1");
        assertThat(booking.isPaid()).isTrue();
        assertThat(booking.isBookingGuaranteeAutoBookingCancel()).isFalse();
    }

    @Test
    public void testConfirmBooking() {
        var confirmed = defaultClient.confirmBookingSync("unit-test-1");
        assertThat(confirmed.getConfirmedBookings()).hasSize(1);
    }

    @Test
    public void testCancelBooking() {
        var cancelled = defaultClient.cancelBookingSync(3056L, "U423C_250320", "foo@bar.ru");
        assertThat(cancelled.getStatusId()).isEqualTo(BookingStatusId.CANCELLED);
    }

    @Test
    public void testCancelAlreadyCancelledBooking() {
        assertThatThrownBy(() -> defaultClient.cancelBookingSync(3056L, "U423C_250321", "foo@bar.ru")).isInstanceOf(AlreadyCancelledException.class);
    }

    @Test
    public void testListBookings() {
        var result = defaultClient.getBookingsSync(3840L);
        assertThat(result.getBookings()).hasSize(5);
        assertThat(result.getBookings().stream().filter(b -> b.getOtaBookingId().equals("YA-9703-7503-3172:0")).findAny()).isNotEmpty();
    }
}

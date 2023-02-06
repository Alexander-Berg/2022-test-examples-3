package ru.yandex.travel.hotels.searcher.services.cache.travelline.hotels;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.base.CallContext;
import ru.yandex.travel.hotels.common.partners.travelline.TravellineClient;
import ru.yandex.travel.hotels.common.partners.travelline.model.Hotel;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInfo;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelListItem;
import ru.yandex.travel.hotels.common.partners.travelline.model.ListHotelsResponse;
import ru.yandex.travel.hotels.searcher.services.cache.Actualizable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedHotelsSearcherTests {
    private TravellineClient client = mock(TravellineClient.class);
    private CachedTravellineHotelDataSearcher searcher;
    private CallContext callContext;

    private String requestId;

    @Before
    public void setUp() {
        prepare(Duration.ofDays(1), 10);
        callContext = CallContext.forSearcher(null, null);
        requestId = UUID.randomUUID().toString();
    }

    private void prepare(Duration updateInterval, int maxHotels) {
        CachedTravellineHotelInfoSearcherProperties properties = new CachedTravellineHotelInfoSearcherProperties();
        properties.setEnabled(true);
        properties.setMaxHotels(maxHotels);
        properties.setUpdateInterval(updateInterval);
        searcher = new CachedTravellineHotelDataSearcher(client, properties);
    }

    @Test
    public void testRegularGet() {
        HotelInfo hotel = new HotelInfo();
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build()))));
        hotel.setHotel(Hotel.builder().code("foo").name("Hotel").build());
        when(client.getHotelInfoSync("foo", requestId)).thenReturn(hotel);
        var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        verify(client, times(1)).getHotelInfoSync("foo", requestId);
    }

    @Test
    public void testCaching() {
        HotelInfo h1 = new HotelInfo();
        HotelInfo h2 = new HotelInfo();
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build(),
                                HotelListItem.builder()
                                        .code("bar")
                                        .inventoryVersion(1)
                                        .build()))));
        h1.setHotel(Hotel.builder().code("foo").name("One").build());
        h2.setHotel(Hotel.builder().code("bar").name("Another").build());
        when(client.getHotelInfoSync("foo", requestId)).thenReturn(h1);
        when(client.getHotelInfoSync("bar", requestId)).thenReturn(h2);
        for (int i = 0; i < 10; i++) {
            var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
            assertThat(hotelInfo).isNotNull();
            assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("One");
        }
        verify(client, times(1)).getHotelInfoSync("foo", requestId);
        verify(client, times(0)).getHotelInfoSync("bar", requestId);
        searcher.getHotelData("test", "bar", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        verify(client, times(1)).getHotelInfoSync("foo", requestId);
        verify(client, times(1)).getHotelInfoSync("bar", requestId);
    }

    @Test
    public void testActualization() {
        HotelInfo h1 = new HotelInfo();
        HotelInfo h2 = new HotelInfo();
        h1.setHotel(Hotel.builder().code("foo").name("Initial").build());
        h2.setHotel(Hotel.builder().code("foo").name("Modified").build());
        when(client.getHotelInfoSync("foo", requestId)).thenReturn(h1).thenReturn(h2);
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build()))));

        var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Initial");

        hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();

        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Initial");
        verify(client, times(1)).getHotelInfoSync("foo", requestId);

        hotelInfo = hotelInfo.actualize(requestId).join();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Modified");
        verify(client, times(2)).getHotelInfoSync("foo", requestId);

        hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Modified");
        verify(client, times(2)).getHotelInfoSync("foo", requestId);
    }

    @Test
    public void testNull() throws InterruptedException {
        HotelInfo hotel = new HotelInfo();
        hotel.setHotel(Hotel.builder().code("foo").name("Hotel").build());
        when(client.getHotelInfoSync("foo", requestId)).thenReturn(null).thenReturn(hotel);
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build()))));
        var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached()).isNull();
        verify(client, times(1)).getHotelInfoSync("foo", requestId);
        Thread.sleep(5); // to ensure that null is evicted
        hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached()).isNotNull();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        verify(client, times(2)).getHotelInfoSync("foo", requestId);
    }

    @Test
    public void testException() throws InterruptedException {
        HotelInfo hotel = new HotelInfo();
        hotel.setHotel(Hotel.builder().code("foo").name("Hotel").build());
        when(client.getHotelInfoSync("foo", requestId)).thenThrow(new RuntimeException()).thenReturn(hotel);
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build()))));
        CompletableFuture<Actualizable<HotelInfo>> hotelInfoFuture;
        while (true) {
            hotelInfoFuture = searcher.getHotelData("test", "foo", CallContext.forSearcher(null, null), requestId);
            try {
                hotelInfoFuture.join();
                break;
            } catch (Throwable ignored) {
                continue;
            }
        }
        verify(client, times(2)).getHotelInfoSync("foo", requestId);
        var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached()).isNotNull();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();
        verify(client, times(2)).getHotelInfoSync("foo", requestId);
    }

    @Test
    public void testExpiration() throws InterruptedException {
        prepare(Duration.ofMillis(2), 10);
        HotelInfo hotel = new HotelInfo();
        hotel.setHotel(Hotel.builder().code("foo").name("Hotel").build());
        when(client.getHotelInfoSync("foo", requestId)).thenReturn(hotel);
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build()))));

        var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        searcher.getHotelData("test", "foo", callContext, requestId).join();

        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        verify(client, atLeast(1)).getHotelInfoSync("foo", requestId);
        Thread.sleep(2);
        hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        verify(client, times(2)).getHotelInfoSync("foo", requestId);
    }

    @Test
    public void testSizeLimit() throws InterruptedException {
        prepare(Duration.ofDays(1), 1);
        HotelInfo hotel = new HotelInfo();
        hotel.setHotel(Hotel.builder().code("foo").name("Hotel").build());
        when(client.getHotelInfoSync(any(), anyString())).thenReturn(hotel);
        when(client.listHotels()).thenReturn(
                CompletableFuture.completedFuture(
                        new ListHotelsResponse(List.of(
                                HotelListItem.builder()
                                        .code("foo")
                                        .inventoryVersion(1)
                                        .build(),
                                HotelListItem.builder()
                                        .code("baz")
                                        .inventoryVersion(1)
                                        .build()))));
        var hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        verify(client, times(1)).getHotelInfoSync("foo", requestId);
        searcher.getHotelData("test", "baz", callContext, requestId).join();
        Thread.sleep(5); // wait to ensure eviction
        hotelInfo = searcher.getHotelData("test", "foo", callContext, requestId).join();
        assertThat(hotelInfo).isNotNull();
        assertThat(hotelInfo.getCached().getHotel().getName()).isEqualTo("Hotel");
        verify(client, times(2)).getHotelInfoSync("foo", requestId);
    }
}

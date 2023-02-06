package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.opentracing.Tracer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.travel.commons.messaging.MessageBus;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.base.CallContext;
import ru.yandex.travel.hotels.common.partners.travelline.TravellineClient;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInventory;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInventoryResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelListItem;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelOfferAvailability;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelRef;
import ru.yandex.travel.hotels.common.partners.travelline.model.ListHotelsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.RoomStay;
import ru.yandex.travel.hotels.proto.ERequestClass;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.inmemory.InmemoryAvailabilityRepository;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.inmemory.InmemoryInventoryRepository;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.inmemory.InmemoryTransactionSupplier;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class InmemoryCachedSearcherTests {
    private TravellineClient client;
    private L2Cache cache;
    private CachedTravellineAvailabilitySearcher searcher;
    private CachedTravellineAvailabilitySearcherProperties properties;
    private MessageBus messageBus;
    private CallContext callContext;

    @Before
    public void prepare() {
        cache = getCache();
        client = mock(TravellineClient.class);
        messageBus = mock(MessageBus.class);
        when(messageBus.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        properties = new CachedTravellineAvailabilitySearcherProperties();
        searcher = new CachedTravellineAvailabilitySearcher(cache, this.client, this.client, this.client, messageBus,
                properties, new Retry(mock(Tracer.class)));
        callContext = CallContext.forSearcher(null, null);
    }

    protected L2Cache getCache() {
        return new L2CacheImplementation(new InmemoryInventoryRepository(), new InmemoryAvailabilityRepository(),
                new InmemoryTransactionSupplier());
    }

    @Test
    public void testInitialLoad() {
        mockTravellineClient(100, 365);
        searcher.updateCacheSync();
        verify(client, times(1)).listHotels();
        verify(client, times(100)).getHotelInventory(anyString());
        verifyNoMoreInteractions(client);
    }


    @Test
    public void testOfferAvailabilityCaching() {
        mockTravellineClient(1, 365);
        searcher.updateCacheSync();
        verify(client, never()).findOfferAvailability(anyString(), any(), any(), any());
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        // answer was cached, so no new calls were made
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());
    }

    @Test
    public void testUpdatesWithNoLookups() {
        mockTravellineClient(100, 365);
        searcher.updateCacheSync();
        verify(client, times(1)).listHotels();
        verify(client, times(100)).getHotelInventory(anyString());
        searcher.updateCacheSync();
        searcher.updateCacheSync();
        searcher.updateCacheSync();
        // no subsequent calls were made, since inventoryWasTouched was not set
        verify(client, times(1)).listHotels();
        verify(client, times(100)).getHotelInventory(anyString());
    }

    @Test
    public void testUpdatesWithALookup() {
        mockTravellineClient(100, 365);
        searcher.updateCacheSync();
        verify(client, times(1)).listHotels();
        verify(client, times(100)).getHotelInventory(anyString());
        searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1), ERequestClass.RC_INTERACTIVE
                , callContext, null).join();
        searcher.updateCacheSync();
        verify(client, times(2)).listHotels();
        // no new inventories, so no new calls to getHotelInventory
        verify(client, times(100)).getHotelInventory(anyString());
    }

    @Test
    public void testUpdatesWithSomeChanges() {
        var updateMap = mockTravellineClient(100, 365);
        searcher.updateCacheSync();
        verify(client, times(1)).listHotels();
        verify(client, times(100)).getHotelInventory(anyString());

        searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1), ERequestClass.RC_INTERACTIVE
                , callContext, null).join();

        updateMap.update("1", LocalDate.now());
        updateMap.update("1", LocalDate.now().plusDays(1));
        updateMap.update("2", LocalDate.now());

        // two hotels updated
        searcher.updateCacheSync();
        verify(client, times(2)).listHotels();
        verify(client, times(102)).getHotelInventory(anyString());
    }

    @Test
    public void testUpdatesCauseCacheInvalidation() {
        // This generates a single hotel (with id=0) with inventory versions for today, tomorrow and after tomorrow
        var updateMap = mockTravellineClient(1, 3);

        // updating cache
        searcher.updateCacheSync();

        // looking up for offers in 3 different requests
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(0), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();

        // OfferAvailability cache was empty, so 3 calls to partner were made
        verify(client, times(3)).findOfferAvailability(anyString(), any(), any(), any());

        // repeating the lookups to insure that responses are cached and no new calls are made
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(0), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(3)).findOfferAvailability(anyString(), any(), any(), any());

        // changing the inventory version for tomorrow, and then updating the cache
        updateMap.update("0", LocalDate.now().plusDays(1));
        searcher.updateCacheSync();

        // this should invalidate responses for tomorrow-after_tomorrow and today-after_tomorrow, so when we repeat
        // these searches two new calls are made
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(0), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(5)).findOfferAvailability(anyString(), any(), any(), any());

        // but the response for today-tomorrow is not invalidated since the checkout date is not included into
        // invalidated interval, so no new calls are made
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(5)).findOfferAvailability(anyString(), any(), any(), any());
    }

    @Test
    public void testOffersOnUncachedDatesAreNotCached() {
        // This generates a single hotel (with id=0) with inventory versions for today only
        var updateMap = mockTravellineClient(1, 1);

        // updating cache
        searcher.updateCacheSync();

        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());

        // assert that "today-tomorrow" responses are cached
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());

        // make a new request "tomorrow-after tomorrow, ensure that it made a call to partner
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(2)).findOfferAvailability(anyString(), any(), any(), any());

        // repeat it several times and ensure that it was cached, since the dates are out of cached inventory dates
        // of the hotel
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(5)).findOfferAvailability(anyString(), any(), any(), any());

        // same as above, but for "partially unknown" dates
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(6)).findOfferAvailability(anyString(), any(), any(), any());
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(2),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(9)).findOfferAvailability(anyString(), any(), any(), any());
    }

    @Test
    public void testHotelRemovalCausesExceptionsOnLookups() {
        // This generates a single hotel (with id=0) with inventory versions for today only
        var updateMap = mockTravellineClient(1, 1);

        // updating cache
        searcher.updateCacheSync();

        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());

        // assert that "today-tomorrow" responses are cached
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        assertThat(searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isNotNull();
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());
        updateMap.remove("0");
        // updating cache
        searcher.updateCacheSync();
        // assert that requests are not cached anymore
        assertThatThrownBy(() -> searcher.lookupOffers("test", "0", LocalDate.now(), LocalDate.now().plusDays(1),
                ERequestClass.RC_INTERACTIVE, callContext, null).join()).isInstanceOf(RuntimeException.class);
        verify(client, times(1)).findOfferAvailability(anyString(), any(), any(), any());

    }

    private UpdatesMap mockTravellineClient(int numHotels, int numDays) {
        UpdatesMap updatesMap = UpdatesMap.generate(numHotels, numDays);
        when(client.listHotels()).thenAnswer((Answer<CompletableFuture<ListHotelsResponse>>) invocation -> {
            var res = new ListHotelsResponse(new ArrayList<>());
            for (String code : updatesMap.getAllCodes()) {
                Long version = updatesMap.getVersion(code);
                if (version != null) {
                    res.getHotels().add(HotelListItem.builder()
                            .code(code)
                            .inventoryVersion(version)
                            .build());
                }
            }
            return CompletableFuture.completedFuture(res);
        });

        when(client.getHotelInventory(anyString())).thenAnswer((Answer<CompletableFuture<HotelInventoryResponse>>) invocation -> {
            String hotelCode = invocation.getArgument(0);
            var res = new HotelInventoryResponse(new ArrayList<>());
            for (var entry : updatesMap.getInventory(hotelCode)) {
                res.getHotelInventories().add(HotelInventory.builder()
                        .date(entry.getKey())
                        .version(entry.getValue())
                        .build());
            }
            return CompletableFuture.completedFuture(res);
        });

        when(client.findOfferAvailability(anyString(), any(LocalDate.class), any(LocalDate.class), any()))
                .thenAnswer(
                        (Answer<CompletableFuture<HotelOfferAvailability>>) invocation -> {
                            var availability = new HotelOfferAvailability();
                            availability.setRoomStays(List.of(RoomStay.builder()
                                    .hotelRef(HotelRef.builder().code(invocation.getArgument(0))
                                            .build())
                                    .build()));
                            return CompletableFuture.completedFuture(availability);
                        });
        return updatesMap;
    }

}

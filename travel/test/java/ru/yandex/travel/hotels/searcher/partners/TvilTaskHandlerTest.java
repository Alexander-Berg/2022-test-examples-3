package ru.yandex.travel.hotels.searcher.partners;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

import ru.yandex.travel.commons.health.HealthCheckedSupplier;
import ru.yandex.travel.commons.messaging.KeyValueStorage;
import ru.yandex.travel.commons.messaging.MessageBus;
import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.hotels.common.partners.tvil.TvilClient;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilOffer;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilSearchRequest;
import ru.yandex.travel.hotels.common.partners.tvil.model.TvilSearchResponse;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.ERequestClass;
import ru.yandex.travel.hotels.proto.THotelId;
import ru.yandex.travel.hotels.proto.TOffer;
import ru.yandex.travel.hotels.proto.TPriceWithDetails;
import ru.yandex.travel.hotels.proto.TSearchOffersReq;
import ru.yandex.travel.hotels.proto.TSearchOffersRsp;
import ru.yandex.travel.hotels.searcher.Task;
import ru.yandex.travel.hotels.searcher.cold.ColdConfigurationProperties;
import ru.yandex.travel.hotels.searcher.cold.ColdService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TvilTaskHandlerTest {
    private TvilClient client;
    private TvilTaskHandler handler;

    @Before
    public void init() {
        client = Mockito.mock(TvilClient.class);
        var properties = new TvilTaskHandlerProperties();
        properties.setBaseUrl("some_url");
        properties.setMaxBatchSize(2);
        properties.setClickOutPartnerId("_ya");
        handler = new TvilTaskHandler(client, properties);
        handler.setMessageBus(Mockito.mock(MessageBus.class, Mockito.RETURNS_DEEP_STUBS));
        handler.setColdService(new ColdService(new ColdConfigurationProperties()));
        handler.setEnv(new MockEnvironment());

        //noinspection unchecked
        var searchFlowOfferDataStorageSupplier = (HealthCheckedSupplier<KeyValueStorage>)Mockito.mock(HealthCheckedSupplier.class);
        var searchFlowOfferDataStorage = Mockito.mock(KeyValueStorage.class);
        Mockito.when(searchFlowOfferDataStorageSupplier.get()).thenReturn(CompletableFuture.completedFuture(searchFlowOfferDataStorage));
        Mockito.when(searchFlowOfferDataStorage.put(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(searchFlowOfferDataStorage.isHealthy()).thenReturn(true);
        handler.setSearchFlowOfferDataStorageSupplier(searchFlowOfferDataStorageSupplier);
    }

    @Test
    public void testSimple() throws Exception {
        doReturn(CompletableFuture.completedFuture(new TvilSearchResponse(Map.of(
                "424276", List.of(
                        offer(1, "Offer 1", "http://url1", 1000, 3, null),
                        offer(2, "Offer 2", "http://url2?p=v", 2000, 1, "Завтрак")
                )
        )))).when(client).searchOffers(argThat(hasHotelIds(Set.of("424276"))));

        Task task = new Task(searchOffersReq("424276"), true);
        handler.startHandle(List.of(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1, TimeUnit.SECONDS);

        assertNotNull("Http Request id was not set for the task", task.getHttpRequestId());

        assertThat(rsp.hasOffers()).isTrue();
        assertThat(rsp.getOffers().getOfferCount()).isEqualTo(2);

        TOffer offer0 = rsp.getOffers().getOffer(0);
        TOffer offer1 = rsp.getOffers().getOffer(1);

        assertThat(offer0.getDisplayedTitle().getValue()).isEqualTo("Offer 1");
        assertThat(offer0.getPrice()).isEqualTo(price(1000, ECurrency.C_RUB));
        assertThat(offer0.getLandingInfo().getLandingPageUrl()).isEqualTo("http://url1?utm_content=_ya");
        assertThat(offer0.getAvailability()).isEqualTo(3);
        assertThat(offer0.getOriginalRoomId()).isEqualTo("1");
        assertThat(offer0.hasFreeCancellation()).isTrue();
        assertThat(offer0.getFreeCancellation().getValue()).isFalse();
        assertThat(offer0.getPansion()).isEqualTo(EPansionType.PT_RO);

        assertThat(offer1.getDisplayedTitle().getValue()).isEqualTo("Offer 2");
        assertThat(offer1.getPrice()).isEqualTo(price(2000, ECurrency.C_RUB));
        assertThat(offer1.getLandingInfo().getLandingPageUrl()).isEqualTo("http://url2?p=v&utm_content=_ya");
        assertThat(offer1.getAvailability()).isEqualTo(1);
        assertThat(offer1.getOriginalRoomId()).isEqualTo("2");
        assertThat(offer1.hasFreeCancellation()).isTrue();
        assertThat(offer1.getFreeCancellation().getValue()).isFalse();
        assertThat(offer1.getPansion()).isEqualTo(EPansionType.PT_BB);
    }

    @Test
    public void testBatching() throws Exception {
        doReturn(CompletableFuture.completedFuture(new TvilSearchResponse(Map.of(
                "424276", List.of(
                        offer(1, "Offer 1", "url1", 1000, 3, null),
                        offer(2, "Offer 2", "url2", 2000, 1, null)
                ),
                "424277", List.of(
                        offer(3, "Offer 3", "url3", 3000, 5, null)
                )
        )))).when(client).searchOffers(argThat(hasHotelIds(Set.of("424276", "424277"))));

        Task task0 = new Task(searchOffersReq("424276"), true);
        Task task1 = new Task(searchOffersReq("424277"), true);

        handler.startHandle(List.of(task0, task1));

        TSearchOffersRsp rsp0 = task0.getCompletionFuture().thenApply(v -> task0.dumpResult()).get(1, TimeUnit.SECONDS);
        assertThat(rsp0.getOffers().getOfferCount()).isEqualTo(2);
        assertThat(rsp0.getOffers().getOffer(0).getDisplayedTitle().getValue()).isEqualTo("Offer 1");
        assertThat(rsp0.getOffers().getOffer(1).getDisplayedTitle().getValue()).isEqualTo("Offer 2");

        TSearchOffersRsp rsp1 = task1.getCompletionFuture().thenApply(v -> task1.dumpResult()).get(1, TimeUnit.SECONDS);
        assertThat(rsp1.getOffers().getOfferCount()).isEqualTo(1);
        assertThat(rsp1.getOffers().getOffer(0).getDisplayedTitle().getValue()).isEqualTo("Offer 3");

        verify(client, times(1)).searchOffers(any());
    }

    @Test
    public void testMultipleBatches() throws Exception {
        doReturn(CompletableFuture.completedFuture(new TvilSearchResponse()))
                .when(client).searchOffers(any());

        List<Task> tasks = List.of(
                new Task(searchOffersReq("424278"), true),
                new Task(searchOffersReq("424279"), true),
                new Task(searchOffersReq("424280"), true)
        );
        handler.startHandle(tasks);

        for (Task task : tasks) {
            TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1, TimeUnit.SECONDS);
            assertThat(rsp.getOffers().getOfferCount()).isEqualTo(0);
        }

        verify(client, times(2)).searchOffers(any());
        verify(client, times(1)).searchOffers(
                argThat(req -> Set.of("424278", "424279").equals(req.getHotelIds())));
        verify(client, times(1)).searchOffers(
                argThat(req -> Set.of("424280").equals(req.getHotelIds())));
    }

    @Test
    public void testEating() throws Exception {
        doReturn(CompletableFuture.completedFuture(new TvilSearchResponse(Map.of(
                "424276", List.of(
                        offer(1, "Offer 1", "url1", 2000, 1, null),
                        offer(2, "Offer 2", "url2", 2000, 1, "Завтрак"),
                        offer(3, "Offer 3", "url3", 2000, 1, "Обед"),
                        offer(4, "Offer 4", "url4", 2000, 1, "Ужин"),
                        offer(5, "Offer 5", "url5", 2000, 1, "Обед и ужин"),
                        offer(6, "Offer 6", "url6", 2000, 1, "Завтрак и обед"),
                        offer(7, "Offer 7", "url7", 2000, 1, "Завтрак и ужин"),
                        offer(8, "Offer 8", "url8", 2000, 1, "Завтрак, обед и ужин"),
                        offer(9, "Offer 9", "url9", 2000, 1, "...")
                )
        )))).when(client).searchOffers(argThat(hasHotelIds(Set.of("424276"))));

        Task task = new Task(searchOffersReq("424276"), true);
        handler.startHandle(List.of(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1, TimeUnit.SECONDS);

        assertThat(rsp.hasOffers()).isTrue();
        assertThat(rsp.getOffers().getOfferCount()).isEqualTo(9);

        TOffer offer1 = rsp.getOffers().getOffer(0);
        TOffer offer2 = rsp.getOffers().getOffer(1);
        TOffer offer3 = rsp.getOffers().getOffer(2);
        TOffer offer4 = rsp.getOffers().getOffer(3);
        TOffer offer5 = rsp.getOffers().getOffer(4);
        TOffer offer6 = rsp.getOffers().getOffer(5);
        TOffer offer7 = rsp.getOffers().getOffer(6);
        TOffer offer8 = rsp.getOffers().getOffer(7);
        TOffer offer9 = rsp.getOffers().getOffer(8);

        assertThat(offer1.getPansion()).isEqualTo(EPansionType.PT_RO);
        assertThat(offer2.getPansion()).isEqualTo(EPansionType.PT_BB);
        assertThat(offer3.getPansion()).isEqualTo(EPansionType.PT_RO);
        assertThat(offer4.getPansion()).isEqualTo(EPansionType.PT_BD);
        assertThat(offer5.getPansion()).isEqualTo(EPansionType.PT_BD);
        assertThat(offer6.getPansion()).isEqualTo(EPansionType.PT_BB);
        assertThat(offer7.getPansion()).isEqualTo(EPansionType.PT_HB);
        assertThat(offer8.getPansion()).isEqualTo(EPansionType.PT_FB);
        assertThat(offer9.getPansion()).isEqualTo(EPansionType.PT_UNKNOWN);
    }

    private static TSearchOffersReq searchOffersReq(String hotelId) {
        return TSearchOffersReq.newBuilder()
                .setId(hotelId)
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_TVIL).setOriginalId(hotelId))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build();
    }

    private static ArgumentMatcher<TvilSearchRequest> hasHotelIds(Set<String> hotelIds) {
        return argument -> hotelIds.equals(argument.getHotelIds());
    }

    private static TvilOffer offer(int roomId, String name, String url, int price, int available, String eating) {
        return TvilOffer.builder()
                .roomId(roomId)
                .name(name)
                .url(url)
                .price(new BigDecimal(price))
                .available(available)
                .eating(eating)
                .build();
    }

    private static TPriceWithDetails price(int amount, ECurrency currency) {
        return TPriceWithDetails.newBuilder()
                .setAmount(amount)
                .setCurrency(currency)
                .build();
    }
}

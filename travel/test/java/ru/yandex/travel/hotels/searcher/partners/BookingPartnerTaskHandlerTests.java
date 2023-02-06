package ru.yandex.travel.hotels.searcher.partners;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.ERequestClass;
import ru.yandex.travel.hotels.proto.THotelId;
import ru.yandex.travel.hotels.proto.TOffer;
import ru.yandex.travel.hotels.proto.TSearchOffersReq;
import ru.yandex.travel.hotels.proto.TSearchOffersRsp;
import ru.yandex.travel.hotels.searcher.Task;
import ru.yandex.travel.hotels.searcher.services.cache.booking.BookingCacheConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.price;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                BookingPartnerTaskHandler.class,
                BookingCacheConfiguration.class,
                CommonTestConfiguration.class,
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.booking.baseUrl=http://localhost:44444",
                "partners.booking.username=test-username",
                "partners.booking.password=secretplaceholder",
                "partners.booking.affiliate-id=350687",
                "partners.booking.reschedule_timeout=100",
                "partners.booking.cacheContent=true",
                "partners.booking.contentCacheSize=1000",
                "partners.booking.contentCacheDuration=1h"
        }
)
public class BookingPartnerTaskHandlerTests {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/booking"));

    @Autowired
    private BookingPartnerTaskHandler handler;

    /*
    private StubMapping from(Resource resource) {
        try {
            return StubMapping.buildFrom(StreamUtils.copyToString(resource.getInputStream(), Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(":(", e);
        }
    }
    */

    private void singleTaskImpl(ERequestClass requestClass) throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("179219"))
                .setOccupancy("2")
                .setCheckInDate("3019-09-27")
                .setCheckOutDate("3019-09-29")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(requestClass)
                .build(), true);

        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(2000, MILLISECONDS);

        assertNotNull("Http Request id was not set for the task", task.getHttpRequestId());

        assertTrue(rsp.hasOffers());
        assertThat(rsp.getOffers().getOfferCount(), is(4));
        assertThat(rsp.getOffers().getCacheTimeSec().getValue(), is(1200));

        TOffer offer0 = rsp.getOffers().getOffer(0);
        TOffer offer1 = rsp.getOffers().getOffer(1);
        TOffer offer2 = rsp.getOffers().getOffer(2);
        TOffer offer3 = rsp.getOffers().getOffer(3);

        assertThat(offer0.getExternalId(), is("17921902_113887049_1_1_0"));
        assertThat(offer0.getPrice(), price(
                10476, ECurrency.C_RUB
        ));

        assertThat(offer0.getLandingInfo().getLandingPageUrl(), is(
                "https://www.booking.com/hotel/ru/radisson-slavyanskaya-business-center.ru.html?aid=350687&checkin=3019-09-27&checkout=3019-09-29&room1=A,A&show_room=17921902_113887049_1_1_0#RD17921902"
        ));
        assertThat(offer0.getLandingInfo().getBookingHotelPageLandingUrl(), is(
                "https://www.booking.com/hotel/ru/radisson-slavyanskaya-business-center.ru.html?checkin=3019-09-27&checkout=3019-09-29&room1=A,A&aid=2192270"
        ));
        assertThat(offer0.getLandingInfo().getBookingSearchPageLandingUrl(), is(
                "https://www.booking.com/searchresults.ru.html?aid=2192269&checkin=3019-09-27&checkout=3019-09-29&city_id=-2960561&dest_id=-2960561&dest_type=city&highlighted_hotels=179219"
        ));
        assertThat(offer0.getAvailability(), is(9));
        assertThat(offer0.getOriginalRoomId(), is("17921901"));
        assertThat(offer0.getAvailabilityGroupKey(), is("17921901"));
        assertThat(offer0.getCapacity(), is("<=2-6"));
        assertThat(offer0.getSingleRoomCapacity(), is("<=2-6"));
        assertThat(offer0.getRoomCount(), is(1));
        assertThat(offer0.hasDisplayedTitle(), is(true));
        assertThat(offer0.getPansion(), is(EPansionType.PT_RO));
        assertThat(offer0.hasWifiIncluded(), is(true));
        assertThat(offer0.getWifiIncluded().getValue(), is(true));
        assertThat(offer0.hasFreeCancellation(), is(true));
        assertThat(offer0.getFreeCancellation().getValue(), is(false));

        // TODO(sandello): Check offer1-offer3
        assertNotNull(offer1);
        assertNotNull(offer2);
        assertNotNull(offer3);
    }

    @Test
    public void testSingleTaskInteractive() throws InterruptedException, ExecutionException, TimeoutException {
        singleTaskImpl(ERequestClass.RC_INTERACTIVE);
    }

    @Test
    public void testSingleTaskBackground() throws InterruptedException, ExecutionException, TimeoutException {
        singleTaskImpl(ERequestClass.RC_BACKGROUND);
    }

    private void multiTaskImpl(ERequestClass requestClass) throws ExecutionException, InterruptedException, TimeoutException {
        List<Task> tasks = Arrays.asList(
                new Task(TSearchOffersReq.newBuilder()
                        .setId("1")
                        .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("404032"))
                        .setOccupancy("2")
                        .setCheckInDate("3019-09-17")
                        .setCheckOutDate("3019-09-19")
                        .setCurrency(ECurrency.C_RUB)
                        .setRequestClass(requestClass)
                        .build(), true),
                new Task(TSearchOffersReq.newBuilder()
                        .setId("1")
                        .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("173545"))
                        .setOccupancy("2")
                        .setCheckInDate("3019-09-17")
                        .setCheckOutDate("3019-09-19")
                        .setCurrency(ECurrency.C_RUB)
                        .setRequestClass(requestClass)
                        .build(), true),
                new Task(TSearchOffersReq.newBuilder()
                        .setId("1")
                        .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("1198144"))
                        .setOccupancy("2")
                        .setCheckInDate("3019-09-17")
                        .setCheckOutDate("3019-09-19")
                        .setCurrency(ECurrency.C_RUB)
                        .setRequestClass(requestClass)
                        .build(), true),
                new Task(TSearchOffersReq.newBuilder()
                        .setId("1")
                        .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("179219"))
                        .setOccupancy("2")
                        .setCheckInDate("3019-09-27")
                        .setCheckOutDate("3019-09-29")
                        .setCurrency(ECurrency.C_RUB)
                        .setRequestClass(requestClass)
                        .build(), true));
        handler.startHandle(tasks);
        TSearchOffersRsp rsp0 = tasks.get(0).getCompletionFuture().thenApply(v -> tasks.get(0).dumpResult()).get(2000, MILLISECONDS);
        TSearchOffersRsp rsp1 = tasks.get(1).getCompletionFuture().thenApply(v -> tasks.get(1).dumpResult()).get(2000, MILLISECONDS);
        TSearchOffersRsp rsp2 = tasks.get(2).getCompletionFuture().thenApply(v -> tasks.get(2).dumpResult()).get(2000, MILLISECONDS);
        TSearchOffersRsp rsp3 = tasks.get(3).getCompletionFuture().thenApply(v -> tasks.get(3).dumpResult()).get(2000, MILLISECONDS);
        assertThat(rsp0.getOffers().getOfferCount(), is(0));
        assertThat(rsp1.getOffers().getOfferCount(), is(5));
        assertThat(rsp2.getOffers().getOfferCount(), is(0));
        assertThat(rsp3.getOffers().getOfferCount(), is(4));
        verify(exactly(4), getRequestedFor(urlPathEqualTo("/blockAvailability")));
    }

    @Test
    public void testMultiTaskTwoBatchInteractive() throws InterruptedException, ExecutionException, TimeoutException {
        multiTaskImpl(ERequestClass.RC_INTERACTIVE);
    }

    @Test
    public void testMultiTaskTwoBatchBackGround() throws InterruptedException, ExecutionException, TimeoutException {
        multiTaskImpl(ERequestClass.RC_BACKGROUND);
    }

    @Test
    public void testIdenticalInABatch() {
        ERequestClass requestClass = ERequestClass.RC_INTERACTIVE;
        List<Task> tasks = IntStream.range(0, 2).mapToObj(i ->
                new Task(TSearchOffersReq.newBuilder()
                        .setId("1")
                        .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("179219"))
                        .setOccupancy("2")
                        .setCheckInDate("3019-09-27")
                        .setCheckOutDate("3019-09-29")
                        .setCurrency(ECurrency.C_RUB)
                        .setRequestClass(requestClass)
                        .build(), true)).collect(Collectors.toList());
        handler.startHandle(tasks);
        List<TSearchOffersRsp> responces = tasks.stream().map(task -> {
            try {
                return task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(2000, MILLISECONDS);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
        assertEquals(2, responces.size());
        TSearchOffersRsp r0 = responces.get(0);
        TSearchOffersRsp r1 = responces.get(1);
        assertEquals(r0.getOffers().getOfferCount(), r1.getOffers().getOfferCount());
        for (int i = 0; i < r0.getOffers().getOfferCount(); i++) {
            assertEquals(r0.getOffers().getOffer(i).getPrice(), r1.getOffers().getOffer(i).getPrice());
            assertEquals(r0.getOffers().getOffer(i).getExternalId(), r1.getOffers().getOffer(i).getExternalId());
            assertEquals(r0.getOffers().getOffer(i).getAvailability(), r1.getOffers().getOffer(i).getAvailability());
            assertEquals(r0.getOffers().getOffer(i).getPansion(), r1.getOffers().getOffer(i).getPansion());
        }
    }

    @Test
    public void testMultiroomTask2Rooms() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId("179219"))
                .setOccupancy("3")
                .setCheckInDate("3019-09-27")
                .setCheckOutDate("3019-09-29")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_BACKGROUND)
                .build(), true);

        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(2000, MILLISECONDS);

        assertTrue(rsp.hasOffers());
        assertThat(rsp.getOffers().getOfferCount(), is(1));
        assertThat(rsp.getOffers().getCacheTimeSec().getValue(), is(1200));

        TOffer offer0 = rsp.getOffers().getOffer(0);

        assertThat(offer0.getExternalId(), is("17921902_113887049_1_1_0"));
        assertThat(offer0.getPrice(), price(
                20954, ECurrency.C_RUB
        ));
        assertThat(offer0.getLandingInfo().getLandingPageUrl(), is(
                "https://www.booking.com/hotel/ru/radisson-slavyanskaya-business-center.ru.html?aid=350687&checkin=3019-09-27&checkout=3019-09-29&room1=A,A,A&show_room=17921902_113887049_1_1_0#group_recommendation"
        ));
        assertThat(offer0.getLandingInfo().getBookingHotelPageLandingUrl(), is(
                "https://www.booking.com/hotel/ru/radisson-slavyanskaya-business-center.ru.html?checkin=3019-09-27&checkout=3019-09-29&room1=A,A,A&aid=2192270"
        ));
        assertThat(offer0.getLandingInfo().getBookingSearchPageLandingUrl(), is(
                "https://www.booking.com/searchresults.ru.html?aid=2192269&checkin=3019-09-27&checkout=3019-09-29&city_id=-2960561&dest_id=-2960561&dest_type=city&highlighted_hotels=179219"
        ));
        assertThat(offer0.getAvailability(), is(9));
        assertThat(offer0.getOriginalRoomId(), is("17921901"));
        assertThat(offer0.getAvailabilityGroupKey(), is("17921901"));
        assertThat(offer0.getCapacity(), is("<=4-6,6"));
        assertThat(offer0.getSingleRoomCapacity(), is("<=2-6"));
        assertThat(offer0.getRoomCount(), is(2));
        assertThat(offer0.hasDisplayedTitle(), is(true));
        assertThat(offer0.getDisplayedTitle().getValue(), is("2 × Standard room"));
        assertThat(offer0.getPansion(), is(EPansionType.PT_RO));
        assertThat(offer0.hasWifiIncluded(), is(true));
        assertThat(offer0.getWifiIncluded().getValue(), is(true));
        assertThat(offer0.hasFreeCancellation(), is(true));
        assertThat(offer0.getFreeCancellation().getValue(), is(false));
    }
}

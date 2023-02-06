package ru.yandex.travel.hotels.searcher.partners;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.TError;
import ru.yandex.travel.hotels.proto.*;
import ru.yandex.travel.hotels.searcher.Task;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.landingUrl;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.price;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                Hotels101TaskHandler.class,
                CommonTestConfiguration.class,
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.101hotels.baseUrl=http://localhost:44444/",
                "partners.101hotels.password=thepassword",
                "partners.101hotels.reschedule_timeout=100"}
)
public class Hotels101TaskHandlerTests {
    @Autowired
    private Hotels101TaskHandler handler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/hotels101"));

    @Test
    public void testSimple() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELS101).setOriginalId("10000"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);

        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000, TimeUnit.MILLISECONDS);

        assertNotNull("Http Request id was not set for the task", task.getHttpRequestId());

        assertTrue(rsp.hasOffers());
        assertThat(rsp.getOffers().getOfferCount(), is(5));

        TOffer offer0 = rsp.getOffers().getOffer(0);
        TOffer offer1 = rsp.getOffers().getOffer(1);
        TOffer offer2 = rsp.getOffers().getOffer(2);

        assertThat(offer0.getPrice(), price(
                3287, ECurrency.C_RUB
        ));
        assertThat(offer0.getLandingInfo(), landingUrl(
                "https://www.101hotels.ru/hotel/redirect/10000?in=10.08.3018&out=12.08.3018&adults=2&room_id=58586_79554S&special=5&source=yandextravel&utm_medium=meta&utm_hotel=10000&utm_city=39"
        ));
        assertThat(offer0.getFreeCancellation().getValue(), is(false));
        assertThat(offer0.getOriginalRoomId(), is("79554S"));

        assertThat(offer1.getPrice(), price(
                3460, ECurrency.C_RUB
        ));
        assertThat(offer1.getLandingInfo(), landingUrl(
                "https://www.101hotels.ru/hotel/redirect/10000?in=10.08.3018&out=12.08.3018&adults=2&room_id=58586_79554&source=yandextravel&utm_medium=meta&utm_hotel=10000&utm_city=39"
        ));
        assertThat(offer1.getFreeCancellation().getValue(), is(true));
        assertThat(offer1.getOriginalRoomId(), is("79554"));

        assertThat(offer2.getPrice(), price(
                5800, ECurrency.C_RUB
        ));
        assertThat(offer2.getLandingInfo(), landingUrl(
                "https://www.101hotels.ru/hotel/redirect/10000?in=10.08.3018&out=12.08.3018&adults=2&room_id=58587_79555&source=yandextravel&utm_medium=meta&utm_hotel=10000&utm_city=39"
        ));
        assertThat(offer2.getFreeCancellation().getValue(), is(true));
        assertThat(offer2.getOriginalRoomId(), is("79555"));

        for (TOffer offer : rsp.getOffers().getOfferList()) {
            assertThat(offer.getExternalId(), is(""));
            assertThat(offer.getAvailabilityGroupKey(), is(""));
            assertThat(offer.hasDisplayedTitle(), is(true));
            assertThat(offer.hasWifiIncluded(), is(false));
            assertThat(offer.hasFreeCancellation(), is(true));
            assertThat(offer.hasActualizationTime(), is(true));
            assertThat(offer.getOperatorId(), is(EOperatorId.OI_HOTELS101));
            assertThat(offer.getCapacity(), is("<=2"));
            assertThat(offer.getPansion(), is(EPansionType.PT_BB));
        }
    }

    @Test
    public void test503() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELS101).setOriginalId("503"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasError());
        TError error = rsp.getError();
        assertThat(error.getMessage(), is("Bad HTTP status code: 503"));
    }

    @Test
    public void test404() throws InterruptedException, ExecutionException, TimeoutException { // unauthorized
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELS101).setOriginalId("404"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasError());
        TError error = rsp.getError();
        assertThat(error.getMessage(), is("Bad HTTP status code: 404"));
    }

    @Test
    public void testBatching() throws InterruptedException, ExecutionException, TimeoutException {
        Task task1 = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELS101).setOriginalId("2585"))
                .setOccupancy("2")
                .setCheckInDate("3018-09-08")
                .setCheckOutDate("3018-09-09")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        Task task2 = new Task(TSearchOffersReq.newBuilder()
                .setId("2")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELS101).setOriginalId("1002"))
                .setOccupancy("2")
                .setCheckInDate("3018-09-08")
                .setCheckOutDate("3018-09-09")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        handler.startHandle(Arrays.asList(task1, task2));
        TSearchOffersRsp rsp1 = task1.getCompletionFuture().thenApply(v -> task1.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        TSearchOffersRsp rsp2 = task2.getCompletionFuture().thenApply(v -> task2.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(9, rsp1.getOffers().getOfferCount());
        assertEquals(34, rsp2.getOffers().getOfferCount());
        verify(exactly(1), getRequestedFor(anyUrl()));
    }
}

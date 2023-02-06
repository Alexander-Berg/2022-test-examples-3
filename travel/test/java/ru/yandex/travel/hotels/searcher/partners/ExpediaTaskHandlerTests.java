package ru.yandex.travel.hotels.searcher.partners;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.TError;
import ru.yandex.travel.hotels.proto.EOperatorId;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.ERequestClass;
import ru.yandex.travel.hotels.proto.THotelId;
import ru.yandex.travel.hotels.proto.TOffer;
import ru.yandex.travel.hotels.proto.TSearchOffersReq;
import ru.yandex.travel.hotels.proto.TSearchOffersRsp;
import ru.yandex.travel.hotels.searcher.HttpClientsConfiguration;
import ru.yandex.travel.hotels.searcher.PropertyConvertersConfiguration;
import ru.yandex.travel.hotels.searcher.Task;
import ru.yandex.travel.hotels.searcher.services.cache.expedia.ExpediaYtCacheConfiguration;
import ru.yandex.travel.infrastructure.RetryHelperAutoConfiguration;
import ru.yandex.travel.tracing.JaegerTracerConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.price;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ExpediaTaskHandler.class,
                ExpediaYtCacheConfiguration.class,
                CommonTestConfiguration.class,
                HttpClientsConfiguration.class,
                JaegerTracerConfiguration.class,
                RetryHelperAutoConfiguration.class,
                PropertyConvertersConfiguration.class,
                FakeClockConfiguration.class
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.expedia.baseUrl=http://localhost:44444",
                "partners.expedia.apiKey=apikeyplaceholder",
                "partners.expedia.secret=secretplaceholder",
                "partners.expedia.reschedule_timeout=100",
                "partners.expedia.profile-type=Standalone",
        }
)
public class ExpediaTaskHandlerTests {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/expedia"));
    @Autowired
    private ExpediaTaskHandler handler;

    @Test
    public void testSimple() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_EXPEDIA).setOriginalId("998941"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);

        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000,
                TimeUnit.MILLISECONDS);

        assertNotNull("Http Request id was not set for the task", task.getHttpRequestId());

        assertTrue(rsp.hasOffers());
        assertThat(rsp.getOffers().getOfferCount(), is(4));

        TOffer offer0 = rsp.getOffers().getOffer(0);
        TOffer offer1 = rsp.getOffers().getOffer(1);
        TOffer offer2 = rsp.getOffers().getOffer(2);
        TOffer offer3 = rsp.getOffers().getOffer(3);

        assertThat(offer0.getExternalId(), is("204524787"));
        assertThat(offer0.getPrice(), price(
                13654, ECurrency.C_RUB
        ));
        assertThat(offer0.getAvailability(), is(18));
        assertThat(offer0.getOriginalRoomId(), is("200891982"));

        assertThat(offer1.getExternalId(), is("204582481"));
        assertThat(offer1.getPrice(), price(
                13654, ECurrency.C_RUB));
        assertThat(offer1.getAvailability(), is(22));
        assertThat(offer1.getOriginalRoomId(), is("200904787"));

        assertThat(offer2.getExternalId(), is("232183265"));
        assertThat(offer2.getPrice(), price(
                13654, ECurrency.C_RUB
        ));
        assertThat(offer2.getAvailability(), is(9));
        assertThat(offer2.getOriginalRoomId(), is("211809211"));

        assertThat(offer3.getExternalId(), is("232183275"));
        assertThat(offer3.getPrice(), price(
                13654, ECurrency.C_RUB
        ));
        assertThat(offer3.getAvailability(), is(12));
        assertThat(offer3.getOriginalRoomId(), is("211809212"));

        for (TOffer offer : rsp.getOffers().getOfferList()) {
            assertThat(offer.hasDisplayedTitle(), is(true));
            assertThat(offer.hasWifiIncluded(), is(false));
            assertThat(offer.hasFreeCancellation(), is(true));
            assertThat(offer.hasActualizationTime(), is(true));
            assertThat(offer.getOperatorId(), is(EOperatorId.OI_EXPEDIA));
            assertThat(offer.getCapacity(), is("==2"));
            assertThat(offer.getFreeCancellation().getValue(), is(true));
            assertThat(offer.getPansion(), is(EPansionType.PT_RO));
            assertThat(offer.getAvailabilityGroupKey(), is(""));
        }
    }

    @Test
    public void test404() throws InterruptedException, ExecutionException, TimeoutException { // empty response - no
        // offers found
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_EXPEDIA).setOriginalId("404"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000,
                TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasOffers());
        assertThat(rsp.getOffers().getOfferCount(), is(0));
        assertFalse(rsp.hasError());
    }

    @Test
    public void test403() throws InterruptedException, ExecutionException, TimeoutException {  // Unauthorized
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_EXPEDIA).setOriginalId("403"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000,
                TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasError());
        TError error = rsp.getError();
        assertThat(error.getMessage(), is("Unexpected HTTP status code '403'"));
    }

    @Test
    public void test503GetsRetried() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_EXPEDIA).setOriginalId("503"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(10000,
                TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasError());
        verify(moreThan(3), getRequestedFor(anyUrl()));
    }

    @Test
    public void testBatching() throws InterruptedException, ExecutionException, TimeoutException {

        Task task1 = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_EXPEDIA).setOriginalId("998941"))
                .setOccupancy("2")
                .setCheckInDate("3018-09-08")
                .setCheckOutDate("3018-09-09")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        Task task2 = new Task(TSearchOffersReq.newBuilder()
                .setId("2")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_EXPEDIA).setOriginalId("520503"))
                .setOccupancy("2")
                .setCheckInDate("3018-09-08")
                .setCheckOutDate("3018-09-09")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        handler.startHandle(Arrays.asList(task1, task2));
        TSearchOffersRsp rsp1 = task1.getCompletionFuture().thenApply(v -> task1.dumpResult()).get(3000,
                TimeUnit.MILLISECONDS);
        TSearchOffersRsp rsp2 = task2.getCompletionFuture().thenApply(v -> task2.dumpResult()).get(3000,
                TimeUnit.MILLISECONDS);
        assertEquals(4, rsp1.getOffers().getOfferCount());
        assertEquals(17, rsp2.getOffers().getOfferCount());
        verify(exactly(1), getRequestedFor(anyUrl()));
    }
}

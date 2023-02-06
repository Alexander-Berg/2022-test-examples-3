package ru.yandex.travel.hotels.searcher.partners;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.TError;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.ERequestClass;
import ru.yandex.travel.hotels.proto.THotelId;
import ru.yandex.travel.hotels.proto.TOffer;
import ru.yandex.travel.hotels.proto.TSearchOffersReq;
import ru.yandex.travel.hotels.proto.TSearchOffersRsp;
import ru.yandex.travel.hotels.searcher.Task;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.TravellineCacheConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.landingUrl;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.price;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                CommonTestConfiguration.class,
                TravellineCacheConfiguration.class,
                TravellineTaskHandler.class,
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.travelline.baseUrl=http://localhost:44444/",
                "partners.travelline.reschedule_timeout=100"
        }
)
@Import(CommonTestConfiguration.class)
public class TravellineTaskHandlerTests {
    @Autowired
    private TravellineTaskHandler handler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/travelline"));

    @Test
    public void testSimple() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_TRAVELLINE).setOriginalId("1001"))
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
        assertThat(rsp.getOffers().getOfferCount(), is(1));


        // defined at `src/test/resources/fixtures/travelline/mappings/hotel_offer_availability_stub.json`
        TOffer offer0 = rsp.getOffers().getOffer(0);

        assertThat(offer0.getPrice(), price(
                165200, ECurrency.C_RUB
        ));
        assertThat(offer0.getLandingInfo(), landingUrl(""));

        assertThat(offer0.getPansion(), is(EPansionType.PT_AI));
    }

    @Test
    public void test401() throws InterruptedException, ExecutionException, TimeoutException {
        stubFor(get(anyUrl()).willReturn(
                aResponse().withStatus(401)
        ));

        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_TRAVELLINE).setOriginalId("401"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));

        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(5000, TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasError());
        TError error = rsp.getError();
        assertThat(error.getMessage(), allOf(containsString("status code"), containsString("401")));
    }

    @Test
    @Ignore("errors 5xx will be retried. The test will take 5-6 seconds")
    public void test503() throws InterruptedException, ExecutionException, TimeoutException {
        stubFor(get(anyUrl()).willReturn(
                aResponse().withStatus(503)
        ));

        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_TRAVELLINE).setOriginalId("503"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));

        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(30000, TimeUnit.MILLISECONDS);
        assertTrue(rsp.hasError());
        TError error = rsp.getError();
        assertThat(error.getMessage(), containsString("503"));
    }
}

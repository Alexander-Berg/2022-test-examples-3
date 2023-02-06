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
import ru.yandex.travel.hotels.searcher.Task;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.landingUrl;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.price;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                CommonTestConfiguration.class,
                OstrovokTaskHandler.class,
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.ostrovok.baseUrl=http://localhost:44444/",
                "partners.ostrovok.password=thepassword",
                "partners.ostrovok.reschedule_timeout=100"
        }
)
public class OstrovokTaskHandlerTests {
    @Autowired
    private OstrovokTaskHandler handler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/ostrovok"));

    @Test
    public void testSimple() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_OSTROVOK).setOriginalId("6291712"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-03")
                .setCheckOutDate("3018-08-05")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);

        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000, TimeUnit.MILLISECONDS);

        assertNotNull("Http Request id was not set for the task", task.getHttpRequestId());

        assertTrue(rsp.hasOffers());
        assertThat(rsp.getOffers().getOfferCount(), is(3));

        TOffer offer0 = rsp.getOffers().getOffer(0);
        TOffer offer1 = rsp.getOffers().getOffer(1);
        TOffer offer2 = rsp.getOffers().getOffer(2);

        assertThat(offer0.getExternalId(), is("s-d932d715-631e-5b35-9813-038ba123412c"));
        assertThat(offer0.getPrice(), price(
                19894, ECurrency.C_RUB
        ));
        assertThat(offer0.getLandingInfo(), landingUrl(
                "https://ostrovok.ru/go/rooms/corendon_vitality_hotel_amsterdam/?cur=RUB&dates=03.08.3018-05.08.3018&guests=2&lang=ru&partner_data=kB6PBPppm5xeW6c1T7XDRohvrecjpTC5fq8Johm8XCFfL_8ltGBoKuYU_omx9N_imN_cPOotATorurfpbwQVZiv5QdhC3_U-YqqKHMg9uB3ReXYZi1AS7_774TWZoOLYGt0hmPUuldKcamlcbI6ELHF90dMtjdLwXuRKy_8yWNrUC9IsNVhky05kVwV-zQ1fvDbSavASAIu5s35zt4t4kiv-aBlfWQKucHPfYjBcPLDp9xc%3D&partner_slug=yandex.affiliate.7877&request_id=7a9cfa9c23569543f7be80f544148097&room=s-d932d715-631e-5b35-9813-038ba123412c&scroll=prices&utm_campaign=ru-ru&utm_distil=ru-ru&utm_medium=cpa-metasearch&utm_source=yandex_metasearch"
        ));
        assertThat(offer0.getOriginalRoomId(), is("91"));
        assertThat(offer0.getAvailabilityGroupKey(), is("#any-residents #double-capacity #private-bathroom #room #standard #twin #window"));

        assertThat(offer1.getExternalId(), is("s-203d0fb5-273f-5124-8b4a-a52a6f5e0203"));
        assertThat(offer1.getPrice(), price(
                21374, ECurrency.C_RUB
        ));
        assertThat(offer1.getLandingInfo(), landingUrl(
                "https://ostrovok.ru/go/rooms/corendon_vitality_hotel_amsterdam/?cur=RUB&dates=03.08.3018-05.08.3018&guests=2&lang=ru&partner_data=PoLglSJrHg22hScC8tbn8e7Nmt3OvfdD985WIL88mL25FebLdidPVlAE5r5C1T31QSVSXmMygq_GtMFrJ5a-_LzxqysC6tm9EnBVrJTmR5Y2pbEKuCoMrs0ttudk2huZFs3J7q2tCsxlUid_gVfz0bgVX5F2aDeFlwUibajAPQVrFUCizu2uBBkKJOE3muuX2BYXlOvcS6cFDqLlNNTe_hSIceq8pyD-xnaabQ%3D%3D&partner_slug=yandex.affiliate.7877&request_id=7a9cfa9c23569543f7be80f544148097&room=s-203d0fb5-273f-5124-8b4a-a52a6f5e0203&scroll=prices&utm_campaign=ru-ru&utm_distil=ru-ru&utm_medium=cpa-metasearch&utm_source=yandex_metasearch"
        ));
        assertThat(offer1.getOriginalRoomId(), is("90"));
        assertThat(offer1.getAvailabilityGroupKey(), is("#any-residents #double #double-capacity #private-bathroom #room #standard #window"));

        assertThat(offer2.getExternalId(), is("s-fabcf2f6-35a8-5fae-8d92-f1c668bba4ee"));
        assertThat(offer2.getPrice(), price(
                21866, ECurrency.C_RUB
        ));
        assertThat(offer2.getLandingInfo(), landingUrl(
                "https://ostrovok.ru/go/rooms/corendon_vitality_hotel_amsterdam/?cur=RUB&dates=03.08.3018-05.08.3018&guests=2&lang=ru&partner_data=SQmtoZbewP8WCCP0tORkD9uVxlwCl77NyIdrsGr7ZOCHS8AGRJ0ZnnfbfYTCjQ-_1Ue4egms3NTo_bO_lsIMmePqO6G94R9XS59p_3jn-KSuFxUuwAu67q4ikBvwUzw0DMEra_zcyFJM-zUYVvqgN4oF2XOR9n8LzvvLsqoYpnXKCpXSBgyePT6q-wgoAGBXgX-OqT1jzlAloPJPHHY9aiC8dgfWY1MWvA%3D%3D&partner_slug=yandex.affiliate.7877&request_id=7a9cfa9c23569543f7be80f544148097&room=s-fabcf2f6-35a8-5fae-8d92-f1c668bba4ee&scroll=prices&utm_campaign=ru-ru&utm_distil=ru-ru&utm_medium=cpa-metasearch&utm_source=yandex_metasearch"
        ));
        assertThat(offer2.getOriginalRoomId(), is("91"));
        assertThat(offer2.getAvailabilityGroupKey(), is("#any-residents #double-capacity #private-bathroom #room #standard #twin #window"));

        for (TOffer offer : rsp.getOffers().getOfferList()) {
            assertThat(offer.getAvailability(), is(0));
            assertThat(offer.hasDisplayedTitle(), is(true));
            assertThat(offer.hasWifiIncluded(), is(false));
            assertThat(offer.hasFreeCancellation(), is(true));
            assertThat(offer.hasActualizationTime(), is(true));
            assertThat(offer.getOperatorId(), is(EOperatorId.OI_OSTROVOK));
            assertThat(offer.getCapacity(), is("==2"));
            assertThat(offer.getPansion(), is(EPansionType.PT_RO));
            assertThat(offer.getFreeCancellation().getValue(), is(false));
        }
    }

    @Test
    public void test401() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_OSTROVOK).setOriginalId("401"))
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
        assertThat(error.getMessage(), is("Bad HTTP status code: 401"));
    }

    @Test
    public void test503() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_OSTROVOK).setOriginalId("503"))
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
    public void testBatching() throws InterruptedException, ExecutionException, TimeoutException {
        Task task1 = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_OSTROVOK).setOriginalId("7467357"))
                .setOccupancy("2")
                .setCheckInDate("3018-09-08")
                .setCheckOutDate("3018-09-09")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        Task task2 = new Task(TSearchOffersReq.newBuilder()
                .setId("2")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_OSTROVOK).setOriginalId("7936791"))
                .setOccupancy("2")
                .setCheckInDate("3018-09-08")
                .setCheckOutDate("3018-09-09")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        handler.startHandle(Arrays.asList(task1, task2));
        TSearchOffersRsp rsp1 = task1.getCompletionFuture().thenApply(v -> task1.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        TSearchOffersRsp rsp2 = task2.getCompletionFuture().thenApply(v -> task2.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(2, rsp1.getOffers().getOfferCount());
        assertEquals(3, rsp2.getOffers().getOfferCount());
        assertEquals(7526, rsp1.getOffers().getOffer(0).getPrice().getAmount());
        assertEquals(8708, rsp1.getOffers().getOffer(1).getPrice().getAmount());
        assertEquals(22921, rsp2.getOffers().getOffer(0).getPrice().getAmount());
        assertEquals(22921, rsp2.getOffers().getOffer(1).getPrice().getAmount());
        assertEquals(24764, rsp2.getOffers().getOffer(2).getPrice().getAmount());
        verify(exactly(1), getRequestedFor(anyUrl()));
    }
}

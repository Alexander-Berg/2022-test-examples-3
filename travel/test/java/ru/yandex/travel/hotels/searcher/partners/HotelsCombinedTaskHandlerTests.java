package ru.yandex.travel.hotels.searcher.partners;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.landingUrl;
import static ru.yandex.travel.hotels.searcher.partners.MatcherUtils.price;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                HotelsCombinedTaskHandler.class,
                CommonTestConfiguration.class,
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.hotelscombined.baseUrl=http://localhost:44444/",
                "partners.hotelscombined.baseMhsUrl=http://localhost:44444/",
                "partners.hotelscombined.apikey=apikeyplaceholder",
                "partners.hotelscombined.reschedule_timeout=100",
                "partners.hotelscombined.maxBatchSize=50"}
)
public class HotelsCombinedTaskHandlerTests {
    @Autowired
    private HotelsCombinedTaskHandler handler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/hotelscombined"));


    @Test
    public void testSimpleMhs() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("00000000-0000-0001-0000-000000000001")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELSCOMBINED).setOriginalId("1042985"))
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
        TOffer offer2 = rsp.getOffers().getOffer(3);
        TOffer offer3 = rsp.getOffers().getOffer(4);

        assertThat(offer0.getPrice(), price(
                2853, ECurrency.C_RUB
        ));
        assertThat(offer0.getLandingInfo(), landingUrl(
                "https://redirect.datahc.com/ProviderRedirect.ashx?key=0.11650846.-1557705166.0.RUB.1174914190&source=201-0&a_aid=166338&brandID=507062&splash=false"
        ));
        assertThat(offer0.getOperatorId(), is(EOperatorId.OI_AGODA));
        assertThat(offer0.getFreeCancellation().getValue(), is(false));
        assertThat(offer0.getPansion(), is(EPansionType.PT_BB));

        assertThat(offer1.getPrice(), price(
                3542, ECurrency.C_RUB
        ));
        assertThat(offer1.getLandingInfo(), landingUrl(
                "https://redirect.datahc.com/ProviderRedirect.ashx?key=0.13446421.-1557705289.0.RUB.34891394&source=201-1&a_aid=166338&brandID=507062&splash=false"
        ));
        assertThat(offer1.getOperatorId(), is(EOperatorId.OI_HOTELSCOM));
        assertThat(offer1.getFreeCancellation().getValue(), is(false));
        assertThat(offer1.getPansion(), is(EPansionType.PT_RO));

        assertThat(offer2.getPrice(), price(
                3365, ECurrency.C_RUB
        ));
        assertThat(offer2.getLandingInfo(), landingUrl(
                "https://redirect.datahc.com/ProviderRedirect.ashx?key=0.6444981.-1557705802.0.RUB.2037114184&source=201-3&a_aid=166338&brandID=507062&splash=false"
        ));
        assertThat(offer2.getOperatorId(), is(EOperatorId.OI_AMOMA));
        assertThat(offer2.getFreeCancellation().getValue(), is(false));
        assertThat(offer2.getPansion(), is(EPansionType.PT_BB));

        assertThat(offer3.getPrice(), price(
                3465, ECurrency.C_RUB
        ));
        assertThat(offer3.getLandingInfo(), landingUrl(
                "https://redirect.datahc.com/ProviderRedirect.ashx?key=0.6554981.-1557705802.0.RUB.2037114184&source=201-3&a_aid=166338&brandID=507062&splash=false"
        ));

        for (TOffer offer : rsp.getOffers().getOfferList()) {
            assertThat(offer.getExternalId(), is(""));
            assertThat(offer.getAvailability(), is(0));
            assertThat(offer.hasDisplayedTitle(), is(true));
            assertThat(offer.hasWifiIncluded(), is(false));
            assertThat(offer.hasFreeCancellation(), is(true));
            assertThat(offer.hasActualizationTime(), is(true));
            assertThat(offer.getCapacity(), is("==2"));
            assertThat(offer.getOriginalRoomId(), is(""));
            assertThat(offer.getAvailabilityGroupKey(), is(""));
        }
    }

    @Test
    public void testSimpleBatch() throws InterruptedException, ExecutionException, TimeoutException {
        Task task1 = new Task(TSearchOffersReq.newBuilder()
                .setId("00000000-0000-0001-0000-000000000001")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELSCOMBINED).setOriginalId("1042985"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);
        Task task2 = new Task(TSearchOffersReq.newBuilder()
                .setId("00000000-0000-0001-0000-000000000002")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELSCOMBINED).setOriginalId("1042986"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .build(), true);

        handler.startHandle(Arrays.asList(task1, task2));
        TSearchOffersRsp rsp1 = task1.getCompletionFuture().thenApply(v -> task1.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        TSearchOffersRsp rsp2 = task2.getCompletionFuture().thenApply(v -> task2.dumpResult()).get(1000, TimeUnit.MILLISECONDS);

        assertTrue(rsp1.hasOffers());
        assertThat(rsp1.getOffers().getOfferCount(), is(3));
        assertTrue(rsp2.hasOffers());
        assertThat(rsp2.getOffers().getOfferCount(), is(1));
    }

    @Test
    public void test401Mhs() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("00000000-0000-0001-0000-000000000001")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELSCOMBINED).setOriginalId("401"))
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
    public void testStringIdFails() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("00000000-0000-0001-0000-000000000001")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELSCOMBINED).setOriginalId("hotelId"))
                .setOccupancy("2")
                .setCheckInDate("3018-08-10")
                .setCheckOutDate("3018-08-12")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);
        handler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(1000, TimeUnit.MILLISECONDS);
        assertThat(rsp.getOffers().getOfferCount(), is(0));
    }

    @Test
    public void test503Mhs() throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("00000000-0000-0001-0000-000000000001")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_HOTELSCOMBINED).setOriginalId("503"))
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
    public void testGetPansion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode ai = mapper.readTree("{\"inclusions\": [4]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(ai), is(EPansionType.PT_AI));

        JsonNode fb1 = mapper.readTree("{\"inclusions\": [3]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(fb1), is(EPansionType.PT_FB));

        JsonNode fb2 = mapper.readTree("{\"inclusions\": [0, 1, 2]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(fb2), is(EPansionType.PT_FB));

        JsonNode bb = mapper.readTree("{\"inclusions\": [0]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(bb), is(EPansionType.PT_BB));

        JsonNode hb1 = mapper.readTree("{\"inclusions\": [0, 1]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(hb1), is(EPansionType.PT_BB));

        JsonNode hb2 = mapper.readTree("{\"inclusions\": [0, 2]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(hb2), is(EPansionType.PT_HB));

        JsonNode bd = mapper.readTree("{\"inclusions\": [2]}");
        assertThat(HotelsCombinedTaskHandler.getPansion(bd), is(EPansionType.PT_BD));

        JsonNode ro = mapper.readTree("{\"inclusions\": []}");
        assertThat(HotelsCombinedTaskHandler.getPansion(ro), is(EPansionType.PT_RO));
    }
}

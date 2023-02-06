package ru.yandex.travel.hotels.searcher.partners;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.hotels.proto.EOperatorId;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.ERequestClass;
import ru.yandex.travel.hotels.proto.THotelId;
import ru.yandex.travel.hotels.proto.TSearchOffersReq;
import ru.yandex.travel.hotels.proto.TSearchOffersRsp;
import ru.yandex.travel.hotels.searcher.Task;
import ru.yandex.travel.hotels.searcher.services.cache.bronevik.BronevikCacheConfiguration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                BronevikPartnerTaskHandler.class,
                BronevikCacheConfiguration.class,
                CommonTestConfiguration.class,
        },
        properties = {
                "spring.profiles.active=test,fake-clock",
                "partners.bronevik.baseUrl=http://localhost:44444",
                "partners.bronevik.client.login=test-username",
                "partners.bronevik.client.wsdlLocation=bronevik_test.wsdl",
                "partners.bronevik.client.password=secretplaceholder",
                "partners.bronevik.client.clientKey=asdasdasd",
                "partners.bronevik.client.soapType=development",
                "partners.bronevik.cacheContent=false",
                "partners.bronevik.enableHotelTimezone=false"
        }
)
public class BronevikPartnerTaskHandlerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .port(44444)
            .usingFilesUnderClasspath("fixtures/bronevik"));

    @Autowired
    BronevikPartnerTaskHandler bronevikPartnerTaskHandler;

    @Before
    public void init() {
        // full logs for xml client
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "999999");
    }

    private void singleTaskImpl(ERequestClass requestClass) throws InterruptedException, ExecutionException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BRONEVIK).setOriginalId("244"))
                .setOccupancy("2")
                .setCheckInDate("2022-05-02")
                .setCheckOutDate("2022-05-05")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(requestClass)
                .build(), true);

        bronevikPartnerTaskHandler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(2000, MILLISECONDS);
        assertTrue(rsp.hasOffers());
        var tOfferList = rsp.getOffers().getOfferList()
                .stream()
                .filter(tOffer -> tOffer.getExternalId().compareTo(
                        "T1IyNDQjMTM2OTcjZG91YmxlIzE0NjkjMjAyMi0wNS0wMiMyMDIyLTA1LTA2IzIjMiMwIzEw") == 0)
                .collect(Collectors.toList());
        assertThat(tOfferList.size(), is(1));
        var tOffer = tOfferList.get(0);
        assertThat(tOffer.getExternalId(), is("T1IyNDQjMTM2OTcjZG91YmxlIzE0NjkjMjAyMi0wNS0wMiMyMDIyLTA1LTA2IzIjMiMwIzEw"));
        assertNotNull(tOffer.getId());
        assertThat(tOffer.getOperatorId(), is(EOperatorId.OI_BRONEVIK));
        assertThat(tOffer.getAvailability(), is(5));

        assertThat(tOffer.getOriginalRoomId(), is("1469"));
        assertThat(tOffer.getRoomCount(), is(1));
        assertThat(tOffer.getPrice().getAmount(), is(23876));

    }

    @Test
    public void testSingleTaskInteractive() throws InterruptedException, ExecutionException, TimeoutException {
        singleTaskImpl(ERequestClass.RC_INTERACTIVE);
    }

    @Test
    public void testSingleTaskBackground() throws InterruptedException, ExecutionException, TimeoutException {
        singleTaskImpl(ERequestClass.RC_BACKGROUND);
    }

    @Test
    public void bronevikExceptionTest() throws ExecutionException, InterruptedException, TimeoutException {
        Task task = new Task(TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BRONEVIK).setOriginalId("500"))
                .setOccupancy("2")
                .setCheckInDate("2022-05-02")
                .setCheckOutDate("2022-05-05")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE)
                .build(), true);

        bronevikPartnerTaskHandler.startHandle(Collections.singletonList(task));
        TSearchOffersRsp rsp = task.getCompletionFuture().thenApply(v -> task.dumpResult()).get(10000, MILLISECONDS);
        assertTrue(rsp.hasError());
        var error = rsp.getError();
        assertThat(error.getAttributeOrThrow("class"), startsWith("ru.yandex.travel.hotels.common.partners.bronevik.BronevikException"));
    }
}

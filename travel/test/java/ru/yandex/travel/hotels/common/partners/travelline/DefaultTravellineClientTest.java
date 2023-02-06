package ru.yandex.travel.hotels.common.partners.travelline;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.exception.RemotelyClosedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.base.exceptions.PartnerException;
import ru.yandex.travel.hotels.common.partners.base.exceptions.RetryableHttpException;
import ru.yandex.travel.hotels.common.partners.base.exceptions.RetryableIOException;
import ru.yandex.travel.hotels.common.partners.travelline.exceptions.CacheNotFoundException;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelChainDetailsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetails;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetailsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelOfferStatus;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelStatusChangedResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.TaxType;
import ru.yandex.travel.hotels.proto.EPartnerId;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
public class DefaultTravellineClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("fixtures/travelline"));

    private DefaultTravellineClient travellineClient;

    @Before
    public void init() {
        AsyncHttpClientWrapper clientWrapper = new AsyncHttpClientWrapper(Dsl.asyncHttpClient(Dsl.config()
                // The way how client is configured in searchers. Needed to verify that the correct pool is used across
                // the process of request/response handling
                .setThreadPoolName(EPartnerId.PI_TRAVELLINE + "_POOL")
                .build()),
                log, "travelline", new MockTracer(), DefaultTravellineClient.getMethods().getNames());
        TravellineClientProperties clientProperties = new TravellineClientProperties();
        clientProperties.setHttpRequestTimeout(Duration.ofMillis(2000));
        clientProperties.setHttpReadTimeout(Duration.ofMillis(2000));
        clientProperties.setBaseUrl(String.format("http://localhost:%s", wireMockRule.port()));
        clientProperties.setApiKey("");
        travellineClient = new DefaultTravellineClient(clientWrapper, clientProperties, new Retry(new MockTracer()));
    }

    @Test
    public void testHotelStatusChanged() {
        HotelStatusChangedResponse response = travellineClient.notifyHotelStatusChangedSync("1024");
        Assert.assertNull(response.getErrors());
        Assert.assertNull(response.getWarnings());
    }

    @Test
    public void testHotelDetails() {
        HotelDetailsResponse response = travellineClient.getHotelDetailsSync("1234");
        Assert.assertNull(response.getErrors());
        Assert.assertNull(response.getWarnings());
        Assert.assertNotNull(response.getHotelDetails());
        HotelDetails hotelDetails = response.getHotelDetails();
        Assert.assertEquals("1234", hotelDetails.getHotelRef().getCode());
        Assert.assertEquals("ООО \"ПУШКА ИНН-СЕРВИС\"", hotelDetails.getBankAccountDetails().getPersonLegalName());
        Assert.assertEquals("Филиал ООО \"ПУШКА ИНН-СЕРВИС\"", hotelDetails.getBankAccountDetails().getBranchName());
        Assert.assertEquals("0123456789", hotelDetails.getBankAccountDetails().getInn());
        Assert.assertEquals("123456789", hotelDetails.getBankAccountDetails().getKpp());
        Assert.assertEquals("044030790", hotelDetails.getBankAccountDetails().getBic());
        Assert.assertEquals("40702810190190000480", hotelDetails.getBankAccountDetails().getCurrentAccount());
        Assert.assertEquals(TaxType.COMMON, hotelDetails.getBankAccountDetails().getTax());
        Assert.assertEquals(HotelOfferStatus.ACCEPTED, hotelDetails.getOfferStatus());
        Assert.assertEquals(LocalDateTime.of(2019, 9, 14, 0, 39), hotelDetails.getOfferUpdatedAt());
    }

    @Test
    public void testHotelChainDetails() {
        HotelChainDetailsResponse response = travellineClient.getHotelChainDetailsSync("1234567890");
        Assert.assertNull(response.getErrors());
        Assert.assertNull(response.getWarnings());
        Assert.assertNotNull(response.getHotelChainDetails().getHotelRefs());
        Assert.assertEquals(2, response.getHotelChainDetails().getHotelRefs().size());
        Assert.assertEquals("1024", response.getHotelChainDetails().getHotelRefs().get(0).getCode());
        Assert.assertEquals("1025", response.getHotelChainDetails().getHotelRefs().get(1).getCode());
    }

    @Test
    public void testCacheNotFound() {
        assertThatExceptionOfType(CacheNotFoundException.class).isThrownBy(
                () -> travellineClient.findOfferAvailabilitySync("1174", LocalDate.of(2020, 5, 2),
                        LocalDate.of(2020, 5, 3)));
    }

    @Test
    public void testSlowConnection() {
        assertThatExceptionOfType(PartnerException.class).isThrownBy(
                        () -> travellineClient.getHotelDetailsSync("SLOW_CONNECTION"))
                .withCauseInstanceOf(RetryableHttpException.class)
                .withRootCauseInstanceOf(TimeoutException.class);
    }

    @Test
    public void testMalformedResponse() {
        assertThatExceptionOfType(PartnerException.class).isThrownBy(
                        () -> travellineClient.getHotelDetailsSync("MALFORMED_RESPONSE_CHUNK"))
                .withCauseInstanceOf(RetryableIOException.class)
                .withRootCauseInstanceOf(RemotelyClosedException.class);
    }

    @Test
    public void testConnectionReset() {
        assertThatExceptionOfType(PartnerException.class).isThrownBy(
                        () -> travellineClient.getHotelDetailsSync("CONNECTION_RESET_BY_PEER"))
                .withCauseInstanceOf(RetryableHttpException.class)
                .withRootCauseInstanceOf(TimeoutException.class);
    }

    @Test(timeout = 2000)
    public void testBaseClientDoesNotExploitCommonForJoinPool() throws InterruptedException {
        // occupy common pool
        new Thread(() ->
                Arrays.stream(new int[100]).parallel().forEach(i -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
        ).start();

        // launch tasks
        long start = System.currentTimeMillis();
        var counter = new AtomicInteger();
        System.out.println("Starting sending");
        for (int i = 0; i < 100; i++) {
            travellineClient.getHotelDetails("MALFORMED_RESPONSE_CHUNK").whenComplete((r, t) -> {
                counter.incrementAndGet();
            });
        }
        while (counter.get() < 100) {
            Thread.sleep(10);
        }
        System.out.println("done. Took " + (System.currentTimeMillis() - start) + "ms");
    }

}

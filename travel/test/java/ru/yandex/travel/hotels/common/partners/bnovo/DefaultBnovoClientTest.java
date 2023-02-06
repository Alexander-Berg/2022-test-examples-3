package ru.yandex.travel.hotels.common.partners.bnovo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.base.exceptions.UnexpectedHttpStatusCodeException;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelConnectionStatus;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelDetails;
import ru.yandex.travel.hotels.common.partners.bnovo.model.HotelDetailsResponse;
import ru.yandex.travel.hotels.common.partners.bnovo.model.TaxType;

import static org.junit.Assert.fail;

@Slf4j
public class DefaultBnovoClientTest {

    public static final String TOKEN = "123";

    private static class BNovoClientWithMockedAuth extends DefaultBNovoClient {

        public BNovoClientWithMockedAuth(AsyncHttpClientWrapper clientWrapper, BNovoClientProperties properties,
                                         Retry retryHelper) {
            super(clientWrapper, properties, retryHelper);
        }

        @Override
        protected CompletableFuture<Token> authenticate(String requestId) {
            return CompletableFuture.completedFuture(new Token(TOKEN, Instant.now()));
        }
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("fixtures/bnovo"));

    private DefaultBNovoClient bnovoClient;

    @Before
    public void init() {
        AsyncHttpClientWrapper clientWrapper = new AsyncHttpClientWrapper(new DefaultAsyncHttpClient(),
                log, "bnovo", new MockTracer(), DefaultBNovoClient.getMethods().getNames());
        BNovoClientProperties clientProperties = new BNovoClientProperties();
        clientProperties.setHttpRequestTimeout(Duration.ofMillis(2000));
        clientProperties.setHttpReadTimeout(Duration.ofMillis(2000));
        clientProperties.setBaseUrl(String.format("http://localhost:%s", wireMockRule.port()));
        bnovoClient = new BNovoClientWithMockedAuth(clientWrapper, clientProperties, new Retry(new MockTracer()));
    }

    @Test
    public void testHotelStatusChanged() {
        //verify, that call is correct. Response body is empty
        bnovoClient.notifyHotelStatusChangedSync("1024");
    }

    @Test
    public void testHotelDetailsNotFound() {
        try {
            bnovoClient.getHotelDetailsSync("1111");
            fail("Call didn't fail");
        } catch (UnexpectedHttpStatusCodeException e) {
            Assert.assertEquals(404, e.getStatusCode());
        }
    }

    @Test
    public void testHotelDetails() {
        HotelDetailsResponse response = bnovoClient.getHotelDetailsSync("1234");
        Assert.assertNotNull(response.getHotelDetails());
        HotelDetails hotelDetails = response.getHotelDetails();
        Assert.assertEquals("1234", hotelDetails.getHotelCode());
        Assert.assertEquals("191186", hotelDetails.getAddress().getPostalCode());
        Assert.assertEquals("Санкт-Петербург", hotelDetails.getAddress().getCityName());
        Assert.assertEquals("191186, г. Санкт-Петербург, пр-т Мира, д. 150", hotelDetails.getAddress().getAddressLine());
        Assert.assertEquals(3, hotelDetails.getContactInfo().size());
        Assert.assertEquals("ООО \"ПУШКА ИНН-СЕРВИС\"", hotelDetails.getBankAccountDetails().getPersonLegalName());
        Assert.assertEquals("Филиал ООО \"ПУШКА ИНН-СЕРВИС\" в Новосибирске",
                hotelDetails.getBankAccountDetails().getBranchName());
        Assert.assertEquals("Российская Федерация", hotelDetails.getBankAccountDetails().getCountry());
        Assert.assertEquals("191186", hotelDetails.getBankAccountDetails().getAddress().getPostalCode());
        Assert.assertEquals("Санкт-Петербург", hotelDetails.getBankAccountDetails().getAddress().getCityName());
        Assert.assertEquals("191186, г. Санкт-Петербург, пр-т Мира, д. 150", hotelDetails.getBankAccountDetails().getAddress().getAddressLine());
        Assert.assertEquals("0123456789", hotelDetails.getBankAccountDetails().getInn());
        Assert.assertEquals("123456789", hotelDetails.getBankAccountDetails().getKpp());
        Assert.assertEquals("044030790", hotelDetails.getBankAccountDetails().getBic());
        Assert.assertEquals("ПАО \"БАНК \"САНКТ-ПЕТЕРБУРГ\"", hotelDetails.getBankAccountDetails().getBankName());
        Assert.assertEquals("30101810900000000790", hotelDetails.getBankAccountDetails().getCorrespondingAccount());
        Assert.assertEquals("40702810190190000480", hotelDetails.getBankAccountDetails().getCurrentAccount());
        Assert.assertEquals(TaxType.COMMON, hotelDetails.getBankAccountDetails().getTax());
        Assert.assertEquals(HotelConnectionStatus.CONNECTED, hotelDetails.getConnectionStatus());
        Assert.assertEquals(LocalDateTime.of(2019, 9, 14, 0, 39), hotelDetails.getConnectionStatusUpdatedAt());
    }
}

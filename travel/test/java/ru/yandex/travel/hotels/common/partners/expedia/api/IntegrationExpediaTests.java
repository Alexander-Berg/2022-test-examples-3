package ru.yandex.travel.hotels.common.partners.expedia.api;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.opentracing.mock.MockTracer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.expedia.ApiVersion;
import ru.yandex.travel.hotels.common.partners.expedia.DefaultExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.ExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.ExpediaClientProperties;
import ru.yandex.travel.hotels.common.partners.expedia.Helpers;
import ru.yandex.travel.hotels.common.partners.expedia.exceptions.ErrorException;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.CancellationStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ResumeReservationStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.Itinerary;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ItineraryReservationRequest;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ReservationResult;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.RoomStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.Error;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.SalesChannel;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingRateStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@Ignore
public class IntegrationExpediaTests {
    private static final String API_KEY = "";      // DO NOT COMMIT ACTUAL VALUES HERE
    private static final String API_SECRET = "";   // INPUT ONLY FOR LOCAL RUNS

    private ExpediaClient client;
    private AsyncHttpClient ahc;
    private Retry retryHelper;

    @Before
    public void setUp() {
        ahc = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("ahcPool")
                .build());
        AsyncHttpClientWrapper wrapper = new AsyncHttpClientWrapper(ahc,
                LoggerFactory.getLogger(IntegrationExpediaTests.class), "expedia",
                new MockTracer(),
                DefaultExpediaClient.getMethods().getNames());

        retryHelper = new Retry(new MockTracer());

        ExpediaClientProperties properties = new ExpediaClientProperties();
        properties.setBaseUrl("https://test.ean.com");
        properties.setApiKey(API_KEY);
        properties.setApiSecret(API_SECRET);
        properties.setDefaultApiVersion(ApiVersion.V2_4);
        properties.setHttpReadTimeout(Duration.ofSeconds(5));
        properties.setHttpRequestTimeout(Duration.ofSeconds(5));
        client = new DefaultExpediaClient(properties, wrapper, retryHelper);
    }

    @After
    public void tearDown() throws IOException {
        ahc.close();
    }


    @Test
    public void fullRun() {
        String hotelId = "26199";                    // Radisson Slavyanskaya
        String anotherHotelId = "2794444";           // Vega Izmailovo
        LocalDate checkin = LocalDate.now().plusDays(14);
        LocalDate checkout = checkin.plusDays(1);
        String occupancy = "2";
        String currency = "RUB";
        String ipAddress = "5.255.255.50";           // one of yandex's addresses
        String userAgent = "yandexTravelIntegrationalTesting/v2";
        String sessionId = UUID.randomUUID().toString();
        String affiliateId = "INTEGRATION-" + String.valueOf(System.currentTimeMillis());
        String firstName = "John";
        String lastName = "Dow";
        String email = "foo@bar.com";
        String phone = "1234567890";
        String billingAddress = "Lva Tolstogo str., 16";
        String billingCity = "Moscow";
        String billingCountry = "RU";


        // FIND AVAILABILITIES call
        var availabilities = client.findAvailabilitiesSync(List.of(hotelId, anotherHotelId), checkin, checkout,
                occupancy, currency,
                ipAddress, sessionId,
                null,
                SalesChannel.CACHE, true);
        assertThat(availabilities).isNotEmpty();
        assertThat(availabilities).containsKeys(hotelId, anotherHotelId);
        assertThat(availabilities.get(hotelId).getRooms()).isNotEmpty();
        assertThat(availabilities.get(anotherHotelId).getRooms()).isNotEmpty();

        var firstRoomWithNonEmptyOffer = availabilities.get(hotelId).getRooms().stream()
                .filter(r -> r.getRates().size() > 0).findFirst().orElseThrow();
        var offer = firstRoomWithNonEmptyOffer.getRates().get(0);
        var priceCheckToken =
                Helpers.retrievePriceCheckToken(offer.getBedGroups().values().iterator().next().getLinks().getPriceCheck().getHref());

        // PRICE CHECK call
        var priceCheckResult = client.checkPriceSync(hotelId, firstRoomWithNonEmptyOffer.getId(), offer.getId(),
                priceCheckToken,
                ipAddress, userAgent, sessionId);
        assertThat(priceCheckResult.getStatus()).isEqualTo(ShoppingRateStatus.AVAILABLE);
        var reservationToken = Helpers.retrieveReservationToken(priceCheckResult.getLinks().getBook().getHref());
        ItineraryReservationRequest reservationRequest = ItineraryReservationRequest.create(affiliateId, firstName,
                lastName, email, phone, billingAddress, billingCity, billingCountry);

        // PROPERTY CONTENT call
        var propertyContent = client.getPropertyContentSync(hotelId, ipAddress, userAgent, sessionId);
        assertThat(propertyContent).isNotNull();

        // HOLD call
        ReservationResult reservationResult = client.reserveItinerarySync(reservationRequest, reservationToken
                , ipAddress, userAgent, sessionId);
        assertThat(reservationResult).isNotNull();

        // second HOLD call
        ErrorException error = catchThrowableOfType(
                () -> client.reserveItinerarySync(reservationRequest, reservationToken, ipAddress, userAgent,
                        sessionId),
                ErrorException.class);

        assertThat(error).isNotNull();
        assertThat(error.getError().getType()).isEqualTo(Error.INVALID_INPUT);
        assertThat(error.getError().getErrors()).anyMatch(i -> i.getType().equals(Error.DUPLICATE_ITINERARY));

        // GET BY AFFILIATE call
        Itinerary itinerary = client.getItineraryByAffiliateIdSync(affiliateId, email, ipAddress, userAgent,
                sessionId);
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getItineraryId()).isEqualTo(reservationResult.getItineraryId());
        Itinerary missingItinerary = client.getItineraryByAffiliateIdSync("wrong", email, ipAddress,
                userAgent, sessionId);
        assertThat(missingItinerary).isNull();
        var confirmationToken = Helpers.retrieveConfirmationToken(itinerary.getLinks().getResume().getHref());
        assertThat(confirmationToken).isNotEmpty();

        // RESUME call
        var resumeStatus = client.resumeItinerarySync(itinerary.getItineraryId(), confirmationToken, ipAddress,
                userAgent, sessionId);
        assertThat(resumeStatus).isEqualTo(ResumeReservationStatus.SUCCESS);

        // GET call
        var reservedItinerary = client.getItinerarySync(itinerary.getItineraryId(), confirmationToken, ipAddress,
                userAgent, sessionId);
        assertThat(reservedItinerary).isNotNull();
        assertThat(reservedItinerary.getRooms()).hasSize(1);
        var refundInfo =
                Helpers.retriveRoomAndRefundToken(reservedItinerary.getRooms().get(0).getLinks().getCancel().getHref(),
                        reservedItinerary.getItineraryId());

        // CANCEL CONFIRMED call
        var cancellationStatus = client.cancelConfirmedItinerarySync(reservedItinerary.getItineraryId(),
                refundInfo[1], refundInfo[0], ipAddress, userAgent, sessionId);
        assertThat(cancellationStatus).isEqualTo(CancellationStatus.SUCCESS);

        // another GET call
        var cancelledItinerary = client.getItinerarySync(itinerary.getItineraryId(), confirmationToken, ipAddress,
                userAgent, sessionId);
        assertThat(cancelledItinerary).isNotNull();
        assertThat(cancelledItinerary.getRooms()).hasSize(1);
        assertThat(cancelledItinerary.getRooms().get(0).getStatus()).isEqualTo(RoomStatus.CANCELED);
    }

}

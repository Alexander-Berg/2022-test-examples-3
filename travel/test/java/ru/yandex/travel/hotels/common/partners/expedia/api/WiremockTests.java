package ru.yandex.travel.hotels.common.partners.expedia.api;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.expedia.DefaultExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.ExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.ExpediaClientProperties;
import ru.yandex.travel.hotels.common.partners.expedia.Helpers;
import ru.yandex.travel.hotels.common.partners.expedia.ProfileType;
import ru.yandex.travel.hotels.common.partners.expedia.exceptions.ErrorException;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.CancellationStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.ItineraryReservationRequest;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.RoomStatus;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.Error;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.Frequency;
import ru.yandex.travel.hotels.common.partners.expedia.model.content.PropertyRatingType;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.FeeScope;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.SalesChannel;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingRateStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static ru.yandex.travel.hotels.common.partners.expedia.ApiVersion.V2_4;
import static ru.yandex.travel.hotels.common.partners.expedia.ApiVersion.V3;

@Slf4j
public class WiremockTests {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("fixtures/expedia"));

    private ExpediaClient expediaClient;

    @Before
    public void init() {
        AsyncHttpClientWrapper clientWrapper = new AsyncHttpClientWrapper(new DefaultAsyncHttpClient(),
                log, "expedida", new MockTracer(), DefaultExpediaClient.getMethods().getNames());
        ExpediaClientProperties clientPropertiesV3 = new ExpediaClientProperties();
        clientPropertiesV3.setProfileType(ProfileType.STANDALONE);
        clientPropertiesV3.setHttpRequestTimeout(Duration.ofMillis(2000));
        clientPropertiesV3.setHttpReadTimeout(Duration.ofMillis(2000));
        clientPropertiesV3.setBaseUrl(String.format("http://localhost:%s", wireMockRule.port()));
        clientPropertiesV3.setApiKey("foo");
        clientPropertiesV3.setDefaultApiVersion(V2_4);
        expediaClient = new DefaultExpediaClient(clientPropertiesV3, clientWrapper, new Retry(new MockTracer()));
    }

    @Test
    public void testGetPropertyContentV24() {
        var res = expediaClient.usingApi(V2_4).getPropertyContentSync("26199", "127.0.0.1", "unit-test",
                UUID.randomUUID().toString());
        assertThat(res.getPropertyId()).isEqualTo("26199");
        assertThat(res.getName()).isEqualTo("Гостиница \"Рэдиссон Славянская\"");
        assertThat(res.getAddress().getLine1()).isNotEmpty();
        assertThat(res.getAddress().getLine2()).isNull();
        assertThat(res.getAddress().getPostalCode()).isNotEmpty();
        assertThat(res.getAddress().getCountryCode()).isEqualTo("RU");
        assertThat(res.getAddress().getObfuscationRequired()).isFalse();
        assertThat(res.getAddress().getLocalized()).isNotNull();
        assertThat(res.getRatings().getProperty().getRating()).isEqualTo(4);
        assertThat(res.getRatings().getProperty().getType()).isEqualTo(PropertyRatingType.STAR);
        assertThat(res.getRatings().getGuest()).isNotNull();
        assertThat(res.getLocation().getCoordinates()).isNotNull();
        assertThat(res.getLocation().getCoordinates().getLatitude()).isGreaterThan(0);
        assertThat(res.getLocation().getCoordinates().getLongitude()).isGreaterThan(0);
        assertThat(res.getPhone()).isNotEmpty();
        assertThat(res.getFax()).isNotEmpty();
        assertThat(res.getCategory().getId()).isEqualTo("1");
        assertThat(res.getCategory().getName()).isEqualTo("Отель");
        assertThat(res.getRank()).isNotNull();
        assertThat(res.getBusinessModel().isExpediaCollect()).isTrue();
        assertThat(res.getBusinessModel().isPropertyCollect()).isTrue();
        assertThat(res.getCheckin().getBeginTime()).isNotEmpty();
        assertThat(res.getCheckin().getEndTime()).isNotEmpty();
        assertThat(res.getCheckin().getInstructions()).isNotEmpty();
        assertThat(res.getCheckin().getSpecialInstructions()).isNotEmpty();
        assertThat(res.getCheckin().getMinAge()).isNotNull();
        assertThat(res.getCheckout().getTime()).isNotNull();
        assertThat(res.getFees().getMandatory()).isNotEmpty();
        assertThat(res.getFees().getOptional()).isNotEmpty();
        assertThat(res.getPolicies().getKnowBeforeYouGo()).isNotEmpty();
        assertThat(res.getAttributes().getPets().get("2050").getId()).isEqualTo("2050");
        assertThat(res.getAttributes().getPets().get("2050").getName()).isNotEmpty();
        assertThat(res.getAttributes().getPets().get("2050").getName()).isNotEmpty();
        assertThat(res.getAmenities()).hasSize(43);
        assertThat(res.getAmenities().entrySet().stream()
                .allMatch(e -> e.getValue().getId().equals(e.getKey()) && e.getValue().getName() != null)).isTrue();
        assertThat(res.getImages()).hasSize(76);
        assertThat(res.getOnsitePayments().getCurrency()).isEqualTo("RUB");
        assertThat(res.getOnsitePayments().getTypes()).hasSize(8);
        assertThat(res.getRooms()).hasSize(9);
        assertThat(res.getRooms().get("166341")).extracting("id", "name", "descriptions", "amenities",
                "images", "bedGroups")
                .allMatch(Objects::nonNull);
    }

    @Test
    public void testGetPropertyContentV3() {
        var res = expediaClient.usingApi(V3).getPropertyContentSync("26199", "127.0.0.1", "unit-test",
                UUID.randomUUID().toString());
        assertThat(res.getPropertyId()).isEqualTo("26199");
        assertThat(res.getName()).isEqualTo("Гостиница \"Рэдиссон Славянская\"");
        assertThat(res.getAddress().getLine1()).isNotEmpty();
        assertThat(res.getAddress().getLine2()).isNull();
        assertThat(res.getAddress().getPostalCode()).isNotEmpty();
        assertThat(res.getAddress().getCountryCode()).isEqualTo("RU");
        assertThat(res.getAddress().getObfuscationRequired()).isFalse();
        assertThat(res.getAddress().getLocalized()).isNotNull();
        assertThat(res.getRatings().getProperty().getRating()).isEqualTo(4);
        assertThat(res.getRatings().getProperty().getType()).isEqualTo(PropertyRatingType.STAR);
        assertThat(res.getRatings().getGuest()).isNotNull();
        assertThat(res.getLocation().getCoordinates()).isNotNull();
        assertThat(res.getLocation().getCoordinates().getLatitude()).isGreaterThan(0);
        assertThat(res.getLocation().getCoordinates().getLongitude()).isGreaterThan(0);
        assertThat(res.getPhone()).isNotEmpty();
        assertThat(res.getFax()).isNotEmpty();
        assertThat(res.getCategory().getId()).isEqualTo("1");
        assertThat(res.getCategory().getName()).isEqualTo("Отель");
        assertThat(res.getRank()).isNotNull();
        assertThat(res.getBusinessModel().isExpediaCollect()).isTrue();
        assertThat(res.getBusinessModel().isPropertyCollect()).isTrue();
        assertThat(res.getCheckin().getBeginTime()).isNotEmpty();
        assertThat(res.getCheckin().getEndTime()).isNotEmpty();
        assertThat(res.getCheckin().getInstructions()).isNotEmpty();
        assertThat(res.getCheckin().getSpecialInstructions()).isNotEmpty();
        assertThat(res.getCheckin().getMinAge()).isNotNull();
        assertThat(res.getCheckout().getTime()).isNotNull();
        assertThat(res.getFees().getMandatory()).isNotEmpty();
        assertThat(res.getFees().getOptional()).isNotEmpty();
        assertThat(res.getPolicies().getKnowBeforeYouGo()).isNotEmpty();
        assertThat(res.getAttributes().getPets().get("2050").getId()).isEqualTo("2050");
        assertThat(res.getAttributes().getPets().get("2050").getName()).isNotEmpty();
        assertThat(res.getAttributes().getPets().get("2050").getName()).isNotEmpty();
        assertThat(res.getAmenities()).hasSize(43);
        assertThat(res.getAmenities().entrySet().stream()
                .allMatch(e -> e.getValue().getId().equals(e.getKey()) && e.getValue().getName() != null)).isTrue();
        assertThat(res.getImages()).hasSize(76);
        assertThat(res.getOnsitePayments().getCurrency()).isEqualTo("RUB");
        assertThat(res.getOnsitePayments().getTypes()).hasSize(8);
        assertThat(res.getRooms()).hasSize(9);
        assertThat(res.getRooms().get("166341")).extracting("id", "name", "descriptions", "amenities",
                        "images", "bedGroups")
                .allMatch(Objects::nonNull);
    }

    @Test
    public void testNoPropertyContentV24() {
        var res = expediaClient.usingApi(V2_4).getPropertyContentSync("0", "127.0.0.1", "unit-test",
                UUID.randomUUID().toString());
        assertThat(res).isNull();
    }

    @Test
    public void testNoPropertyContentV3() {
        var res = expediaClient.usingApi(V3).getPropertyContentSync("0", "127.0.0.1", "unit-test",
                UUID.randomUUID().toString());
        assertThat(res).isNull();
    }

    @Test
    public void testShoppingV24() {
        var res = expediaClient.usingApi(V2_4).findAvailabilitiesSync(
                List.of("26199", "16536518", "36066585"),
                LocalDate.of(2020, 5, 16),
                LocalDate.of(2020, 5, 18),
                "2", "RUB", "127.0.0.1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), SalesChannel.CACHE, true);
        assertThat(res).isNotEmpty();
        assertThat(Helpers.retrievePriceCheckToken(res.get("36066585").getRooms().get(0).getRates().get(0).getBedGroups().values().iterator().next().getLinks().getPriceCheck().getHref())).isNotNull();
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getTotals().getInclusive().getBillableCurrency().getValue()).isEqualByComparingTo("26841.22");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getBillableCurrency().getValue()).isEqualByComparingTo("3.28");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getBillableCurrency().getCurrency()).isEqualTo("EUR");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getRequestCurrency().getValue()).isEqualByComparingTo("271.42");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getRequestCurrency().getCurrency()).isEqualTo("RUB");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getFrequency()).isEqualTo(Frequency.PER_NIGHT);
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getScope()).isEqualTo(FeeScope.PER_PERSON);
    }

    @Test
    public void testShoppingV3() {
        var res = expediaClient.usingApi(V3).findAvailabilitiesSync(
                List.of("26199", "16536518", "36066585"),
                LocalDate.of(2020, 5, 16),
                LocalDate.of(2020, 5, 18),
                "2", "RUB", "127.0.0.1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), SalesChannel.CACHE, true);
        assertThat(res).isNotEmpty();
        assertThat(Helpers.retrievePriceCheckToken(res.get("36066585").getRooms().get(0).getRates().get(0).getBedGroups().values().iterator().next().getLinks().getPriceCheck().getHref())).isNotNull();
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getTotals().getInclusive().getBillableCurrency().getValue()).isEqualByComparingTo("26841.22");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getBillableCurrency().getValue()).isEqualByComparingTo("3.28");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getBillableCurrency().getCurrency()).isEqualTo("EUR");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getRequestCurrency().getValue()).isEqualByComparingTo("271.42");
        assertThat(res.get("36066585").getRooms().get(0).getRates().get(0).getOccupancyPricing().get("2").getFees().getMandatoryTax().getRequestCurrency().getCurrency()).isEqualTo("RUB");
    }

    @Test
    public void testShoppingNoAvailabilityV24() {
        var res = expediaClient.usingApi(V2_4).findAvailabilitiesSync(
                List.of("0"),
                LocalDate.of(2020, 5, 16),
                LocalDate.of(2020, 5, 18),
                "2", "RUB", "127.0.0.1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), SalesChannel.CACHE, true);
        assertThat(res).isEmpty();
    }

    @Test
    public void testShoppingNoAvailabilityV3() {
        var res = expediaClient.usingApi(V3).findAvailabilitiesSync(
                List.of("0"),
                LocalDate.of(2020, 5, 16),
                LocalDate.of(2020, 5, 18),
                "2", "RUB", "127.0.0.1", UUID.randomUUID().toString(), UUID.randomUUID().toString(), SalesChannel.CACHE, true);
        assertThat(res).isEmpty();
    }

    @Test
    public void testPriceCheckMatchedV24() {
        var res = expediaClient.usingApi(V2_4).checkPriceSync("26199", "187779", "238100529", "token",
                "127.0.0.1", "unit-tests", UUID.randomUUID().toString());
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(ShoppingRateStatus.AVAILABLE);
        assertThat(res.getLinks().getBook().getHref()).isNotEmpty();
        assertThat(Helpers.retrieveReservationToken(res.getLinks().getBook().getHref())).isNotNull();
        assertThat(res.getOccupancyPricing().get("2")).isNotNull();
    }

    @Test
    public void testPriceCheckMatchedV3() {
        var res = expediaClient.usingApi(V3).checkPriceSync("26199", "187779", "238100529", "token",
                "127.0.0.1", "unit-tests", UUID.randomUUID().toString());
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(ShoppingRateStatus.AVAILABLE);
        assertThat(res.getLinks().getBook().getHref()).isNotEmpty();
        assertThat(Helpers.retrieveReservationToken(res.getLinks().getBook().getHref())).isNotNull();
        assertThat(res.getOccupancyPricing().get("2")).isNotNull();
    }

    @Test
    public void testPriceCheckPriceMismatchV24() {
        var res = expediaClient.usingApi(V2_4).checkPriceSync("26199", "187779", "0", "token",
                "127.0.0.1", "unit-tests", UUID.randomUUID().toString());
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(ShoppingRateStatus.PRICE_CHANGED);
        assertThat(res.getLinks().getBook().getHref()).isNotEmpty();
        assertThat(Helpers.retrieveReservationToken(res.getLinks().getBook().getHref())).isNotNull();
        assertThat(res.getOccupancyPricing().get("2")).isNotNull();
    }

    @Test
    public void testPriceCheckPriceMismatchV3() {
        var res = expediaClient.usingApi(V3).checkPriceSync("26199", "187779", "0", "token",
                "127.0.0.1", "unit-tests", UUID.randomUUID().toString());
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(ShoppingRateStatus.PRICE_CHANGED);
        assertThat(res.getLinks().getBook().getHref()).isNotEmpty();
        assertThat(Helpers.retrieveReservationToken(res.getLinks().getBook().getHref())).isNotNull();
        assertThat(res.getOccupancyPricing().get("2")).isNotNull();
    }

    @Test
    public void testPriceCheckSoldOutV24() {
        var res = expediaClient.usingApi(V2_4).checkPriceSync("26199", "187779", "1", "token",
                "127.0.0.1", "unit-tests", UUID.randomUUID().toString());
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(ShoppingRateStatus.SOLD_OUT);
        assertThat(res.getLinks().getBook()).isNull();
        assertThat(res.getOccupancyPricing()).isNull();
    }

    @Test
    public void testPriceCheckSoldOutV3() {
        var res = expediaClient.usingApi(V3).checkPriceSync("26199", "187779", "1", "token",
                "127.0.0.1", "unit-tests", UUID.randomUUID().toString());
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(ShoppingRateStatus.SOLD_OUT);
        assertThat(res.getLinks().getBook()).isNull();
        assertThat(res.getOccupancyPricing()).isNull();
    }

    @Test
    public void testHoldV24() {
        var hold = expediaClient.usingApi(V2_4).reserveItinerarySync(generateRequest(), "success", "127.0.0.1",
                "unit-tests", UUID.randomUUID().toString());
        assertThat(hold).isNotNull();
        assertThat(hold.getLinks().getResume()).isNotNull();
        assertThat(Helpers.retrieveConfirmationToken(hold.getLinks().getResume().getHref())).isNotNull();
    }

    @Test
    public void testHoldV3() {
        var hold = expediaClient.usingApi(V3).reserveItinerarySync(generateRequest(), "success", "127.0.0.1",
                "unit-tests", UUID.randomUUID().toString());
        assertThat(hold).isNotNull();
        assertThat(hold.getLinks().getResume()).isNotNull();
        assertThat(Helpers.retrieveConfirmationToken(hold.getLinks().getResume().getHref())).isNotNull();
    }

    @Test
    public void testHoldV24PriceMismatch() {
        var ex = catchThrowableOfType(
                () -> expediaClient.usingApi(V2_4).reserveItinerarySync(generateRequest(), "price_mismatch",
                        "127.0.0.1", "unit-tests", UUID.randomUUID().toString()),
                ErrorException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getError().getType()).isEqualTo(Error.PRICE_MISMATCH);
    }

    @Test
    public void testHoldV3PriceMismatch() {
        var ex = catchThrowableOfType(
                () -> expediaClient.usingApi(V3).reserveItinerarySync(generateRequest(), "price_mismatch",
                        "127.0.0.1", "unit-tests", UUID.randomUUID().toString()),
                ErrorException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getError().getType()).isEqualTo(Error.PRICE_MISMATCH);
    }

    @Test
    public void testHoldV24SoldOut() {
        var ex = catchThrowableOfType(
                () -> expediaClient.usingApi(V2_4).reserveItinerarySync(generateRequest(), "rooms_unavailable",
                        "127.0.0.1", "unit-tests", UUID.randomUUID().toString()),
                ErrorException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getError().getType()).isEqualTo(Error.ROOMS_UNAVAILABLE);
    }

    @Test
    public void testHoldV3SoldOut() {
        var ex = catchThrowableOfType(
                () -> expediaClient.usingApi(V3).reserveItinerarySync(generateRequest(), "rooms_unavailable",
                        "127.0.0.1", "unit-tests", UUID.randomUUID().toString()),
                ErrorException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getError().getType()).isEqualTo(Error.ROOMS_UNAVAILABLE);
    }

    @Test
    public void testGetHeldByAffiliateIdV24() {
        var itinerary = expediaClient.usingApi(V2_4).getItineraryByAffiliateIdSync("hold", "john-doe@example.com",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks().getResume().getHref()).startsWith("/2.4");
    }

    @Test
    public void testGetHeldByAffiliateIdV3() {
        var itinerary = expediaClient.usingApi(V3).getItineraryByAffiliateIdSync("hold", "john-doe@example.com",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks().getResume().getHref()).startsWith("/v3");
    }

    @Test
    public void testGetMissingByAffiliateIdV24() {
        var itinerary = expediaClient.usingApi(V2_4).getItineraryByAffiliateIdSync("missing", "john-doe@example.com",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNull();
    }

    @Test
    public void testGetMissingByAffiliateIdV3() {
        var itinerary = expediaClient.usingApi(V3).getItineraryByAffiliateIdSync("missing", "john-doe@example.com",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNull();
    }

    @Test
    public void testGetConfirmedByAffiliateIdV24() {
        var itinerary = expediaClient.usingApi(V2_4).getItineraryByAffiliateIdSync("confirmed", "john-doe@example.com",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks()).isNull();
        assertThat(itinerary.getRooms()).isNotEmpty();
        assertThat(itinerary.getRooms().get(0).getStatus()).isEqualTo(RoomStatus.BOOKED);
        assertThat(itinerary.getRooms().get(0).getLinks().getCancel().getHref()).startsWith("/2.4");
    }

    @Test
    public void testGetConfirmedByAffiliateIdV3() {
        var itinerary = expediaClient.usingApi(V3).getItineraryByAffiliateIdSync("confirmed", "john-doe@example.com",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks()).isNull();
        assertThat(itinerary.getRooms()).isNotEmpty();
        assertThat(itinerary.getRooms().get(0).getStatus()).isEqualTo(RoomStatus.BOOKED);
        assertThat(itinerary.getRooms().get(0).getLinks().getCancel().getHref()).startsWith("/v3");
    }

    @Test
    public void testGetHeldByIdV24() {
        var itinerary = expediaClient.usingApi(V2_4).getItinerarySync("7342105350748", "hold", "127.0.0.1",
                "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks().getResume().getHref()).startsWith("/2.4");
    }

    @Test
    public void testGetHeldByIdV3() {
        var itinerary = expediaClient.usingApi(V3).getItinerarySync("7342105350748", "hold", "127.0.0.1",
                "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks().getResume().getHref()).startsWith("/v3");
    }

    @Test
    public void testGetHeldMissingIdV24() {
        var itinerary = expediaClient.usingApi(V2_4).getItinerarySync("7342105350740", "missing",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNull();
    }

    @Test
    public void testGetHeldMissingIdV3() {
        var itinerary = expediaClient.usingApi(V3).getItinerarySync("7342105350740", "missing",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNull();
    }

    @Test
    public void testConfirmedByIdV24() {
        var itinerary = expediaClient.usingApi(V2_4).getItinerarySync("7342105350748", "confirmed",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks()).isNull();
        assertThat(itinerary.getRooms()).isNotEmpty();
        assertThat(itinerary.getRooms().get(0).getStatus()).isEqualTo(RoomStatus.BOOKED);
        assertThat(itinerary.getRooms().get(0).getLinks().getCancel().getHref()).startsWith("/2.4");
    }

    @Test
    public void testConfirmedByIdV3() {
        var itinerary = expediaClient.usingApi(V3).getItinerarySync("7342105350748", "confirmed",
                "127.0.0.1", "unit-test", UUID.randomUUID().toString());
        assertThat(itinerary).isNotNull();
        assertThat(itinerary.getLinks()).isNull();
        assertThat(itinerary.getRooms()).isNotEmpty();
        assertThat(itinerary.getRooms().get(0).getStatus()).isEqualTo(RoomStatus.BOOKED);
        assertThat(itinerary.getRooms().get(0).getLinks().getCancel().getHref()).startsWith("/v3");
    }

    @Test
    public void testCancelConfirmedV24() {
        var status = expediaClient.usingApi(V2_4).cancelConfirmedItinerarySync("7342105350748", "cancelConfirmed_404",
                "127.0.0.1", "unit-test", "CancelConfirmed", "sid");
        assertThat(status).isEqualTo(CancellationStatus.ALREADY_CANCELLED);
    }

    @Test
    public void testCancelConfirmedV3() {
        var status = expediaClient.usingApi(V3).cancelConfirmedItinerarySync("7342105350748", "cancelConfirmed_404",
                "127.0.0.1", "unit-test", "CancelConfirmed", "sid");
        assertThat(status).isEqualTo(CancellationStatus.ALREADY_CANCELLED);
    }

    private ItineraryReservationRequest generateRequest() {
        return ItineraryReservationRequest.create(
                "unit-test",
                "John",
                "Doe",
                "john-doe@example.com",
                "79011234567",
                "Lva Tolstogo, 16",
                "Moscow",
                "ru");
    }


}

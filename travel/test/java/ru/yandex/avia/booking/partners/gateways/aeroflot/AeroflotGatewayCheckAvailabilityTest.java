package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ru.yandex.avia.booking.enums.ClassOfService;
import ru.yandex.avia.booking.partners.gateways.BookingRetryableException;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.SearchData;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckRequest;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckResponse;
import ru.yandex.avia.booking.partners.gateways.model.availability.DepartureIsTooCloseException;
import ru.yandex.avia.booking.partners.gateways.model.availability.VariantNotAvailableException;
import ru.yandex.avia.booking.partners.gateways.model.search.FareInfo;
import ru.yandex.avia.booking.partners.gateways.model.search.Flight;
import ru.yandex.avia.booking.partners.gateways.model.search.PriceInfo;
import ru.yandex.avia.booking.partners.gateways.model.search.Variant;
import ru.yandex.avia.booking.tests.wiremock.Wiremock;
import ru.yandex.avia.booking.tests.wiremock.WiremockServerResolver;
import ru.yandex.avia.booking.tests.wiremock.WiremockUri;
import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.defaultAhcClient;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.defaultMapper;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3MultipleTariffs;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3SingleTariff;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.stubRequest;
import static ru.yandex.travel.testing.misc.TestResources.readResource;

@ExtendWith(WiremockServerResolver.class)
class AeroflotGatewayCheckAvailabilityTest {
    private AsyncHttpClientWrapper ahcClientWrapper;
    private AeroflotGateway gateway;

    @BeforeEach
    void before(@WiremockUri String bookingUri) {
        ahcClientWrapper = Mockito.spy(defaultAhcClient());
        gateway = AeroflotApiStubsHelper.defaultGateway(bookingUri, ahcClientWrapper);
    }

    @Test
    void checkAvailability_singleSuccess(@Wiremock WireMockServer wmServer) throws Exception {
        String offerPriceRs = readResource("aeroflot/v3/offer_price_rs_v3_sample.xml");
        stubRequest(wmServer, offerPriceRs, new ContainsPattern("IATA_OfferPriceRQ"));

        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        AvailabilityCheckResponse response = gateway.checkAvailabilityAll(
                AvailabilityCheckRequest.builder().token(variant).build());

        // checking conversion to generic variant
        Variant variantUpd = response.getVariant();
        Money totalPrice = variantUpd.getPriceInfo().getTotal();
        assertThat(totalPrice.getNumber().intValueExact()).isEqualTo(207432);
        assertThat(totalPrice.getCurrency().getCurrencyCode()).isEqualTo("RUB");
        assertThat(variantUpd.getSegments().get(0).getFlights().get(0).getBaseClass())
                .isEqualTo(ClassOfService.ECONOMY);
        assertThat(variantUpd.getSegments().stream().map(seg -> seg.getFlights().stream()
                .map(fl -> fl.getDepCode() + "->" + fl.getArrCode()).collect(toList())).collect(toList()))
                .isEqualTo(asList(asList("SVO->VVO", "VVO->KHV"), asList("KHV->VVO", "VVO->SVO")));

        // no SeatsLeft info in NDC API v3 OfferPriceRS
        variantUpd.getSegments().stream()
                .flatMap(s -> s.getFlights().stream())
                .forEach(f -> assertThat(f.getSeatsLeft()).isNull());

        // json convertibility check
        // VariantServiceIntegrationTest uses manually copied output of this code for mocking
        String json = defaultMapper.writer().writeValueAsString(response);
        //log.info(json);
        AvailabilityCheckResponse convertedResponse = defaultMapper.readValue(json, AvailabilityCheckResponse.class);
        assertThat(convertedResponse).isEqualTo(response);

        SearchData searchData = response.getVariant().getSearchData();
        assertThat(searchData).isNotNull();
        assertThat(searchData.getQid()).isEqualTo("180208-232505-325.ticket.plane" +
                ".c213_c2_2018-03-21_None_economy_1_0_0_ru.ru");
        assertThat(searchData.getExternalBookingUrl()).isEqualTo("http://yandex.mlsd.ru/flights__from_meta?flight_id" +
                "=146980020001&external_subject_id=10208");

        // the three first has failed, the last one was successful
        List<Flight> fwdFlights = variantUpd.getSegments().get(0).getFlights();
        List<Flight> bwdFlights = variantUpd.getSegments().get(1).getFlights();
        assertThat(fwdFlights.get(0).getAircraftSeats()).isNull();
        assertThat(fwdFlights.get(1).getAircraftSeats()).isNull();
        assertThat(bwdFlights.get(0).getAircraftSeats()).isNull();
    }

    @Test
    void checkAvailability_multipleTariffs(@Wiremock WireMockServer wmServer) throws Exception {
        String offerPriceFare2 = readResource("aeroflot/v3/offer_price_rs_v3_sample_for_large.xml");
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 2555),
                new ContainsPattern("<PriceClassRefID>FARE1_NNOR</PriceClassRefID>"));
        stubRequest(wmServer, offerPriceFare2,
                new ContainsPattern("<PriceClassRefID>FARE2_NCOR</PriceClassRefID>"));
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 6155),
                new ContainsPattern("<PriceClassRefID>FARE3_NFOR</PriceClassRefID>"));

        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3MultipleTariffs());
        AvailabilityCheckResponse response = gateway.checkAvailabilityAll(
                AvailabilityCheckRequest.builder().token(variant).build());

        // checking conversion to generic variant
        Variant variantUpd = response.getVariant();
        Money totalPrice = variantUpd.getPriceInfo().getTotal();
        assertThat(totalPrice.getNumber().intValueExact()).isEqualTo(3755);
        assertThat(totalPrice.getCurrency().getCurrencyCode()).isEqualTo("RUB");
        assertThat(variantUpd.getSegments().get(0).getFlights().get(0).getBaseClass())
                .isEqualTo(ClassOfService.ECONOMY);

        // multiple tariffs check
        List<PriceInfo> tariffs = variantUpd.getAllTariffs();
        assertThat(tariffs.size()).isEqualTo(3);
        assertThat(tariffs.get(0).getTotal()).isEqualTo(Money.of(2555, "RUB"));
        assertThat(tariffs.get(1).getTotal()).isEqualTo(Money.of(3755, "RUB"));
        assertThat(tariffs.get(2).getTotal()).isEqualTo(Money.of(6155, "RUB"));

        assertThat(variantUpd.getPriceInfo().getCategoryPrices().get(0).getFareInfo().stream()
                .map(FareInfo::getFlightId).collect(toList()))
                .isEqualTo(tariffs.get(0).getCategoryPrices().get(0).getFareInfo().stream()
                        .map(FareInfo::getFlightId).collect(toList()));

        // json convertibility check
        String json = defaultMapper.writer().writeValueAsString(response);
        AvailabilityCheckResponse convertedResponse = defaultMapper.readValue(json, AvailabilityCheckResponse.class);
        assertThat(convertedResponse).isEqualTo(response);
    }

    @Test
    void checkAvailabilityPartialSuccess(@Wiremock WireMockServer wmServer) {
        String offerPriceFare2 = readResource("aeroflot/v3/offer_price_rs_v3_sample_for_large.xml");
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_421.xml"),
                new ContainsPattern("<PriceClassRefID>FARE1_NNOR</PriceClassRefID>"));
        stubRequest(wmServer, offerPriceFare2,
                new ContainsPattern("<PriceClassRefID>FARE2_NCOR</PriceClassRefID>"));
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 6155),
                new ContainsPattern("<PriceClassRefID>FARE3_NFOR</PriceClassRefID>"));

        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3MultipleTariffs());
        AvailabilityCheckResponse response = gateway.checkAvailabilityAll(
                AvailabilityCheckRequest.builder().token(variant).build());
        Variant checked = response.getVariant();
        assertThat(checked.getAllTariffs().size()).isEqualTo(2);
        assertThat(checked.getAllTariffs().get(0).getTotal()).isEqualTo(Money.of(3755, "RUB"));
        assertThat(checked.getAllTariffs().get(1).getTotal()).isEqualTo(Money.of(6155, "RUB"));
        assertThat(checked.getPriceInfo().getTotal()).isEqualTo(Money.of(3755, "RUB"));
    }

    @Test
    void checkAvailabilitySelectedNa(@Wiremock WireMockServer wmServer) {
        String offerPriceFare2 = readResource("aeroflot/v3/offer_price_rs_v3_sample_for_large.xml");
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 2555),
                new ContainsPattern("<PriceClassRefID>FARE1_NNOR</PriceClassRefID>"));
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_421.xml"),
                new ContainsPattern("<PriceClassRefID>FARE2_NCOR</PriceClassRefID>"));
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 6155),
                new ContainsPattern("<PriceClassRefID>FARE3_NFOR</PriceClassRefID>"));

        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3MultipleTariffs());
        AvailabilityCheckResponse response = gateway.checkAvailabilityAll(
                AvailabilityCheckRequest.builder().token(variant).build());
        Variant checked = response.getVariant();
        assertThat(checked.getAllTariffs().size()).isEqualTo(2);
        assertThat(checked.getAllTariffs().get(0).getTotal()).isEqualTo(Money.of(2555, "RUB"));
        assertThat(checked.getAllTariffs().get(1).getTotal()).isEqualTo(Money.of(6155, "RUB"));
        assertThat(checked.getPriceInfo().getTotal()).isEqualTo(Money.of(2555, "RUB"));
    }

    @Test
    void checkAvailabilityAll_allNotAvailable(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_421.xml"));

        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3MultipleTariffs());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.checkAvailabilityAll(
                        AvailabilityCheckRequest.builder().token(variant).build()))
                .withMessageContaining("None of the 3 tariffs is available at the moment");
    }

    @Test
    void checkAvailabilityAll_tariffsSorting(@Wiremock WireMockServer wmServer) {
        String offerPriceFare2 = readResource("aeroflot/v3/offer_price_rs_v3_sample_for_large.xml");
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 1000),
                new ContainsPattern("<PriceClassRefID>FARE1_NNOR</PriceClassRefID>"));
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 2000),
                new ContainsPattern("<PriceClassRefID>FARE2_NCOR</PriceClassRefID>"));
        stubRequest(wmServer, copyOfferPriceRsWithNewPrice(offerPriceFare2, 3000),
                new ContainsPattern("<PriceClassRefID>FARE3_NFOR</PriceClassRefID>"));

        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3MultipleTariffs());
        Collections.reverse(variant.getAllTariffs());
        assertThat(variant.getAllTariffs().stream().map(AeroflotTotalOffer::getId).collect(toList())).isEqualTo(List.of(
                // for some reason the tariffs aren't sorted
                "1ADT-VKO.202012020830.LED.SU.6012.N.NFOR",
                "1ADT-VKO.202012020830.LED.SU.6012.N.NCOR",
                "1ADT-VKO.202012020830.LED.SU.6012.N.NNOR"));

        AvailabilityCheckResponse response = gateway.checkAvailabilityAll(
                AvailabilityCheckRequest.builder().token(variant).build());
        Variant checked = response.getVariant();
        assertThat(checked.getAllTariffs().stream().map(PriceInfo::getId).collect(toList())).isEqualTo(List.of(
                // after availability check they all are sorted
                "1ADT-VKO.202012020830.LED.SU.6012.N.NNOR",
                "1ADT-VKO.202012020830.LED.SU.6012.N.NCOR",
                "1ADT-VKO.202012020830.LED.SU.6012.N.NFOR"));
    }

    private String copyOfferPriceRsWithNewPrice(String content, double newTotalPrice) {
        return content.replaceAll("<TotalAmount CurCode=\"RUB\">\\d+</TotalAmount>",
                "<TotalAmount CurCode=\"RUB\">" + newTotalPrice + "</TotalAmount>");
    }

    @Test
    void checkAvailabilityNotAvailable(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_421.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("The offer isn't available anymore")
                .withMessageContaining("Not Available and Waitlist is Closed");
    }

    @Test
    void checkAvailabilityTariffNotAvailable(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_740.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("The tariff isn't available anymore")
                .withMessageContaining("Rate not available");
    }

    @Test
    void checkAvailabilityTariffNotAvailable_911comment(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_911_sabre_no_fare.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("Unable to process - system error")
                .withMessageContaining("SabreNoFareForClassException: Тариф не соответствует классу бронирования");
    }

    @Test
    void checkAvailabilityDepartureIsTooClose(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_350.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(DepartureIsTooCloseException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("The offer isn't available anymore")
                .withMessageContaining("Segment departure is too close")
                .withMessageContaining("Дата вылета меньше допустимого значения");
    }

    @Test
    void checkAvailabilityDepartureIsTooClose_911(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_911_segment_too_close.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(DepartureIsTooCloseException.class)
                .isThrownBy(() -> gateway.checkAvailabilityAll(
                        AvailabilityCheckRequest.builder().token(variant).build()))
                .withMessageContaining("The offer isn't available anymore")
                .withMessageContaining("Дата вылета меньше допустимого значения");
    }

    @Test
    @Disabled
        // todo(tlg-13): TDB. At the moment, it's not clear how the following error should look like in NDC API v3:
        // <Error ShortText="Unable to process - system error" Code="911" Type="911">SBService error: HTTP 500
        // 1001000260 SabreFlightNoOpException</Error>
    void checkAvailabilitySabreFlightNoOp(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, "<FlightPriceRQ ",
                "server_responses/aeroflot/errors/flight_price_response_error_911_sabre_na.xml");
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(VariantNotAvailableException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("The offer isn't available anymore")
                .withMessageContaining("SBService error: HTTP 500 1001000260 SabreFlightNoOpException");
    }

    @Test
    void checkAvailabilityFailureUnexpected(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_715.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("Unexpected OfferPriceRQ API error")
                .withMessageContaining("Invalid fare basis");
    }

    @Test
    void checkAvailabilityTmpUnavailable(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/v3/errors/offer_price_v3_error_304.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(BookingRetryableException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("NDC API is temporarily unavailable")
                .withMessageContaining("System Temporarily Unavailable");
    }

    @Test
    void checkAvailabilityTooManyRequests(@Wiremock WireMockServer wmServer) {
        stubRequest(wmServer, readResource("aeroflot/generic_errors/generic_response_error_429.xml"));
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        // the exception shouldn't happen in testing anymore, no special treatment for it
        // if it reproduces, we should rather Aeroflot support (Ramax) to increase the limits
        //assertThatExceptionOfType(BookingTooManyRequestsException.class)
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("Unexpected OfferPriceRQ exception")
                .withMessageContaining("Received bad API response")
                .withMessageContaining("Too Many Requests");
    }

    @Test
    void checkAvailabilityNetworkUnreachable() {
        doReturn(CompletableFuture.failedFuture(new ConnectException("Network is unreachable: gw.aeroflot.io/185.69" +
                ".81.208:443")))
                .when(ahcClientWrapper).executeRequest(any());
        AeroflotVariant variant = gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff());
        assertThatExceptionOfType(BookingRetryableException.class)
                .isThrownBy(() -> gateway.checkAvailabilitySingle(variant))
                .withMessageContaining("Recoverable error during OfferPriceRQ")
                .withMessageContaining("Network is unreachable");
    }
}

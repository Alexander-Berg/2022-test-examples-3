package ru.yandex.travel.train.partners.im;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.testing.misc.TestResources;
import ru.yandex.travel.train.model.AdditionalPlaceRequirements;
import ru.yandex.travel.train.model.CabinGenderKind;
import ru.yandex.travel.train.model.CabinPlaceDemands;
import ru.yandex.travel.train.model.CarStorey;
import ru.yandex.travel.train.model.CarType;
import ru.yandex.travel.train.model.DocumentType;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.Sex;
import ru.yandex.travel.train.partners.im.model.OrderFullCustomerRequest;
import ru.yandex.travel.train.partners.im.model.PlaceRange;
import ru.yandex.travel.train.partners.im.model.RailwayPassengerRequest;
import ru.yandex.travel.train.partners.im.model.RailwayReservationRequest;
import ru.yandex.travel.train.partners.im.model.RateValue;
import ru.yandex.travel.train.partners.im.model.ReservationCreateRequest;
import ru.yandex.travel.train.partners.im.model.ReservationCreateResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class TestReservationCreate {
    private static final Logger logger = LoggerFactory.getLogger(TestReservationConfirm.class);
    private ImClient client;

    @Rule
    public WireMockRule wireMockRule
            = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        var testClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("expediaAhcPool")
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
                .build());
        var clientWrapper = new AsyncHttpClientWrapper(
                testClient, logger, "testDestination", new MockTracer(),
                Arrays.stream(DefaultImClient.Method.values()).map(Enum::name).collect(Collectors.toSet())
        );
        client = new DefaultImClient(clientWrapper, "pos", Duration.ofSeconds(10),
                String.format("http://localhost:%d/", wireMockRule.port()),"ya", "***");
    }

    @Test
    public void testReservationCreateSuccess() {
        stubFor(post(anyUrl()).willReturn(aResponse()
                .withBody(TestResources.readResource("ReservationCreateResponse.json"))));
        var request = createRequest();

        ReservationCreateResponse reservation = client.reservationCreate(request, null);

        verify(postRequestedFor(urlPathEqualTo("/Order/V1/Reservation/Create")).withRequestBody(equalToJson(
                TestResources.readResource("ReservationCreateRequest.json")
        )));
        assertThat(reservation.getOrderId()).isEqualTo(1561134);
        assertThat(reservation.getCustomers().get(0).getIndex()).isEqualTo(0);
        assertThat(reservation.getCustomers().get(0).getDocumentType()).isEqualTo(DocumentType.BIRTH_CERTIFICATE);
        assertThat(reservation.getCustomers().get(1).getIndex()).isEqualTo(1);
        assertThat(reservation.getCustomers().get(1).getDocumentType()).isEqualTo(DocumentType.RUSSIAN_PASSPORT);

        var reservationResult = reservation.getReservationResults().get(0);
        assertThat(reservationResult.getConfirmTill()).isEqualTo("2019-07-09T15:22:25");

        assertThat(reservationResult.getAmount()).isEqualTo(BigDecimal.valueOf(8394.3));
        assertThat(reservationResult.getArrivalDateTime()).isEqualTo("2019-07-13T17:59");
        assertThat(reservationResult.getCarDescription()).isEqualTo("У0");
        assertThat(reservationResult.getCarNumber()).isEqualTo("16");
        assertThat(reservationResult.getCarType()).isEqualTo(CarType.COMPARTMENT);
        assertThat(reservationResult.getCarrier()).isEqualTo("ФПК ВОСТ-СИБИРСКИЙ");
        assertThat(reservationResult.getCarrierCode()).isEqualTo("01");
        assertThat(reservationResult.getCarrierTin()).isEqualTo("7708709686");
        assertThat(reservationResult.getConfirmTill()).isEqualTo("2019-07-09T15:22:25");
        assertThat(reservationResult.getCountryCode()).isEqualTo("20");
        assertThat(reservationResult.getDepartureDateTime()).isEqualTo("2019-07-12T13:10");
        assertThat(reservationResult.getDestinationStation()).isEqualTo("ЕКАТЕРИНБУРГ-ПАССАЖИРС");
        assertThat(reservationResult.getDestinationStationCode()).isEqualTo("2030000");
        assertThat(reservationResult.getDestinationTimeZoneDifference()).isEqualTo(2);
        assertThat(reservationResult.getIndex()).isEqualTo(0);
        assertThat(reservationResult.getInternationalServiceClass()).isEqualTo("1/2");
        assertThat(reservationResult.isSuburban()).isEqualTo(true);
        assertThat(reservationResult.isOnlyFullReturnPossible()).isEqualTo(true);
        assertThat(reservationResult.getLocalArrivalDateTime()).isEqualTo("2019-07-13T17:59");
        assertThat(reservationResult.getLocalDepartureDateTime()).isEqualTo("2019-07-12T13:10");
        assertThat(reservationResult.getOrderItemId()).isEqualTo(1447864);
        assertThat(reservationResult.getOriginStation()).isEqualTo("МОСКВА КАЗАНСКАЯ");
        assertThat(reservationResult.getOriginStationCode()).isEqualTo("2000003");
        assertThat(reservationResult.getOriginTimeZoneDifference()).isEqualTo(0);
        assertThat(reservationResult.getServiceClass()).isEqualTo("2Л");
        assertThat(reservationResult.getTrainDescription()).isEqualTo("СКОРЫЙ");
        assertThat(reservationResult.getTrainNumber()).isEqualTo("082ИА");
        assertThat(reservationResult.getTimeDescription()).isEqualTo("РАЗРЕШЕН ПРОВОЗ ЖИВОТНЫХ; МУЖ.");
        assertThat(reservationResult.getTripDuration()).isEqualTo(1609);

        var blank0 = reservationResult.getBlanks().get(0);
        assertThat(blank0.getAdditionalPrice()).isEqualTo(BigDecimal.valueOf(1400.1));
        assertThat(blank0.getAmount()).isEqualTo(BigDecimal.valueOf(2252.3));
        assertThat(blank0.getBaseFare()).isEqualTo(BigDecimal.valueOf(852.2));
        assertThat(blank0.getOrderItemBlankId()).isEqualTo(1092996);
        assertThat(blank0.getServicePrice()).isEqualTo(BigDecimal.valueOf(156.0));
        assertThat(blank0.getTariffInfo().getTariffType()).isEqualTo("Child");
        assertThat(blank0.getTariffType()).isEqualTo("Child");
        assertThat(blank0.getVatRateValues().get(0)).isEqualTo(createRateValue(0, 0));
        assertThat(blank0.getVatRateValues().get(1)).isEqualTo(createRateValue(20.0, 26.0));
        assertThat(blank0.getFareInfo().getCarrierTin()).isEqualTo("7708709686");

        var passenger0 = reservationResult.getPassengers().get(0);
        assertThat(passenger0.getAmount()).isEqualTo(BigDecimal.valueOf(2252.3));
        assertThat(passenger0.getOrderItemBlankId()).isEqualTo(1092996);
        assertThat(passenger0.getOrderCustomerId()).isEqualTo(2016226);
        assertThat(passenger0.getOrderCustomerReferenceIndex()).isEqualTo(0);
        assertThat(passenger0.getPlacesWithType().get(0).getNumber()).isEqualTo("019");
        assertThat(passenger0.getPlacesWithType().get(0).getType()).isEqualTo(ReservationPlaceType.LOWER);
        assertThat(passenger0.getCategory()).isEqualTo(PassengerCategory.CHILD);
    }

    private RateValue createRateValue(double rate, double value) {
        var res = new RateValue();
        res.setRate(rate);
        res.setValue(BigDecimal.valueOf(value));
        return res;
    }

    private ReservationCreateRequest createRequest() {
        var request = new ReservationCreateRequest();
        request.setCustomers(new ArrayList<>());
        var customer0 = new OrderFullCustomerRequest();
        request.getCustomers().add(customer0);
        customer0.setBirthday(LocalDateTime.parse("2012-07-20T00:00:00"));
        customer0.setSex(Sex.MALE);
        customer0.setDocumentNumber("XXрр625252");
        customer0.setDocumentType(DocumentType.BIRTH_CERTIFICATE);
        customer0.setFirstName("Актер");
        customer0.setMiddleName("-");
        customer0.setLastName("Гениальный");
        customer0.setCitizenshipCode("RU");
        customer0.setIndex(0);
        var customer1 = new OrderFullCustomerRequest();
        request.getCustomers().add(customer1);
        customer1.setBirthday(LocalDateTime.parse("1990-07-20T00:00:00"));
        customer1.setSex(Sex.MALE);
        customer1.setDocumentNumber("6505038238");
        customer1.setDocumentType(DocumentType.RUSSIAN_PASSPORT);
        customer1.setFirstName("Михаил");
        customer1.setMiddleName("-");
        customer1.setLastName("Арбузов");
        customer1.setCitizenshipCode("RU");
        customer1.setIndex(1);
        request.setReservationItems(new ArrayList<>());
        var reservationItem = new RailwayReservationRequest();
        request.getReservationItems().add(reservationItem);
        reservationItem.setCarStorey(CarStorey.NO_VALUE);
        reservationItem.setCabinPlaceDemands(CabinPlaceDemands.NO_VALUE);
        reservationItem.setCabinGenderKind(CabinGenderKind.MALE);
        reservationItem.setAdditionalPlaceRequirements(AdditionalPlaceRequirements.NO_VALUE);
        reservationItem.setPassengers(new ArrayList<>());
        var passenger0 = new RailwayPassengerRequest();
        reservationItem.getPassengers().add(passenger0);
        passenger0.setCategory(PassengerCategory.CHILD);
        passenger0.setPreferredAdultTariffType("Full");
        passenger0.setOrderCustomerIndex(0);
        var passenger1 = new RailwayPassengerRequest();
        reservationItem.getPassengers().add(passenger1);
        passenger1.setCategory(PassengerCategory.ADULT);
        passenger1.setPreferredAdultTariffType("Full");
        passenger1.setPhone("79123456789");
        passenger1.setContactEmailOrPhone("email@email.com");
        passenger1.setOrderCustomerIndex(1);
        reservationItem.setTrainNumber("082И");
        reservationItem.setSetElectronicRegistration(true);
        reservationItem.setServiceClass("2Л");
        reservationItem.setInternationalServiceClass("1/2");
        reservationItem.setProviderPaymentForm("Card");
        reservationItem.setOriginCode("2000003");
        reservationItem.setDestinationCode("2030000");
        reservationItem.setUpperPlaceQuantity(1);
        reservationItem.setLowerPlaceQuantity(1);
        reservationItem.setDepartureDate(LocalDateTime.parse("2019-07-12T13:10:00"));
        reservationItem.setCarType(CarType.COMPARTMENT);
        reservationItem.setBedding(true);
        reservationItem.setPlaceRange(new PlaceRange());
        reservationItem.getPlaceRange().setFrom(19);
        reservationItem.getPlaceRange().setTo(20);
        reservationItem.setGiveAdditionalTariffForChildIfPossible(false);
        reservationItem.setCarNumber("16");
        reservationItem.setIndex(0);
        return request;
    }
}

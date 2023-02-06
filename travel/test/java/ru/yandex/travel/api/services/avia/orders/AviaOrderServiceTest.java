package ru.yandex.travel.api.services.avia.orders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.avia.booking.enums.PassengerCategory;
import ru.yandex.avia.booking.partners.gateways.model.booking.TravellerInfo;
import ru.yandex.avia.booking.service.dto.FlightDTO;
import ru.yandex.avia.booking.service.dto.SegmentDTO;
import ru.yandex.avia.booking.service.dto.VariantDTO;
import ru.yandex.avia.booking.service.dto.form.TravellerFormDTO;
import ru.yandex.travel.api.services.avia.orders.AviaOrderService.ParsedPhone;
import ru.yandex.travel.api.services.avia.references.AirlineLoyaltyProgramCodeMapper;
import ru.yandex.travel.api.services.avia.references.AirlineLoyaltyProgramCodeMapperImpl;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.avia.td.AviaTdFlight;
import ru.yandex.travel.api.services.avia.td.AviaTdInfo;
import ru.yandex.travel.api.services.avia.td.AviaTdSegment;
import ru.yandex.travel.api.services.common.PhoneCountryCodesService;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.avia.booking.enums.PassengerCategory.ADULT;
import static ru.yandex.avia.booking.enums.PassengerCategory.CHILD;
import static ru.yandex.avia.booking.enums.PassengerCategory.INFANT;

public class AviaOrderServiceTest {
    @Test
    public void testTravellersWithSameName_renamed() {
        List<TravellerInfo> travellers = asList(
                traveller("f1", "l1"),
                traveller("f1", "l1"),
                traveller("f2", "l1"),
                traveller("f2", "l1")
        );
        AviaOrderService.handleTravellersWithSameName(travellers);
        assertThat(fullName(travellers.get(0))).isEqualTo("f1 l1");
        assertThat(fullName(travellers.get(1))).isEqualTo("f1 l1 Jr");
        assertThat(fullName(travellers.get(2))).isEqualTo("f2 l1");
        assertThat(fullName(travellers.get(3))).isEqualTo("f2 l1 Jr");
    }

    @Test
    public void testTravellersWithSameName_tooManySameNames() {
        List<TravellerInfo> travellers = asList(
                traveller("same", "name"),
                traveller("sAme", "name"),
                traveller("same", "namE")
        );
        assertThatThrownBy(
                () -> AviaOrderService.handleTravellersWithSameName(travellers)
        ).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Too many passengers with the same name");
    }

    @Test
    public void testTravellersForMiddleNameSubstitutionToNoname() {
        List<TravellerFormDTO> documents = asList(travellerForm("f1", "l1"));
        AviaGeobaseCountryService countryService = Mockito.mock(AviaGeobaseCountryService.class);
        AirlineLoyaltyProgramCodeMapper loyaltyProgramMapper = new AirlineLoyaltyProgramCodeMapperImpl();
        List<PassengerCategory> categories = List.of(ADULT);
        List<TravellerInfo> travellers = AviaOrderService.convertTravellers(
                documents, mockTdInfo(), countryService, loyaltyProgramMapper, categories, true);
        assertThat(fullName(travellers.get(0))).isEqualTo("f1 Noname l1");
    }

    @Test
    public void testTravellersWithSameName_duplicatesWithExtraWhitespaces() {
        List<TravellerFormDTO> documents = asList(
                travellerForm("f1", "l1"),
                travellerForm("f1", "l1 "),
                travellerForm("f2", "l  1"),
                travellerForm("f2", "l 1")
        );
        AviaGeobaseCountryService countryService = Mockito.mock(AviaGeobaseCountryService.class);
        AirlineLoyaltyProgramCodeMapper loyaltyProgramMapper = new AirlineLoyaltyProgramCodeMapperImpl();
        List<PassengerCategory> categories = List.of(ADULT, ADULT, ADULT, ADULT);
        List<TravellerInfo> travellers = AviaOrderService.convertTravellers(
                documents, mockTdInfo(), countryService, loyaltyProgramMapper, categories, false);
        assertThat(fullName(travellers.get(0))).isEqualTo("f1 l1");
        assertThat(fullName(travellers.get(1))).isEqualTo("f1 l1 Jr");
        assertThat(fullName(travellers.get(2))).isEqualTo("f2 l 1");
        assertThat(fullName(travellers.get(3))).isEqualTo("f2 l 1 Jr");
    }

    @Test
    public void testNonameOrEmpty() {
        assertThat(AviaOrderService.nonameOrEmpty("", true)).isEqualTo(AviaOrderService.NONAME_MIDDLENAME);
        assertThat(AviaOrderService.nonameOrEmpty("x", true)).isEqualTo("x");
        assertThat(AviaOrderService.nonameOrEmpty("", false)).isEqualTo("");
        assertThat(AviaOrderService.nonameOrEmpty("x", false)).isEqualTo("x");
    }

    @Test
    public void validateVariant() {
        VariantDTO variant = VariantDTO.builder()
                .legs(List.of(SegmentDTO.builder()
                        .flights(List.of(FlightDTO.builder()
                                // valid departure
                                .departure(LocalDateTime.parse("2020-05-26T23:43:00"))
                                .build()))
                        .build()))
                .build();
        Clock now = Clock.fixed(Instant.parse("2020-05-26T09:43:00Z"), ZoneId.of("UTC"));

        // no errors
        AviaOrderService.validateVariant(variant, now);

        // illegal departure
        variant.getLegs().get(0).getFlights().get(0).setDeparture(LocalDateTime.parse("2020-05-15T09:43:00"));
        assertThatThrownBy(() -> AviaOrderService.validateVariant(variant, now))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Departure date in the past");
    }

    @Test
    public void phoneParserTests() {
        PhoneCountryCodesService countryCodesService = new PhoneCountryCodesService();
        assertThat(ParsedPhone.create("+7 (916) 123-45-67", countryCodesService))
                .isEqualTo(new ParsedPhone(7, 9161234567L));
        assertThat(ParsedPhone.create("911 1234567", countryCodesService))
                .isEqualTo(new ParsedPhone(7, 9111234567L));
        assertThat(ParsedPhone.create("374 911 1234567", countryCodesService))
                .isEqualTo(new ParsedPhone(374, 9111234567L));
        // an illegal phone but we still want the last fallback to work
        assertThat(ParsedPhone.create("49", countryCodesService))
                .isEqualTo(new ParsedPhone(4, 9L));
    }

    @Test
    public void passengerCategories_validation() {
        String arrivalDate = "2020-12-26";
        List<TravellerFormDTO> documents = asList(
                travellerForm("P1", "P1", "1970-01-01"),
                travellerForm("P2", "P2", "1970-01-01"),
                travellerForm("CH1", "CH1", "2010-01-01"),
                travellerForm("INF1", "AS CHILD", "2019-01-01"),
                travellerForm("INF2", "AS INFANT", "2020-07-01")
        );
        AviaGeobaseCountryService countryService = Mockito.mock(AviaGeobaseCountryService.class);
        List<PassengerCategory> categories = List.of(ADULT, ADULT, CHILD, CHILD, INFANT);
        AirlineLoyaltyProgramCodeMapper loyaltyProgramMapper = new AirlineLoyaltyProgramCodeMapperImpl();
        List<TravellerInfo> travellers = AviaOrderService.convertTravellers(
                documents, mockTdInfo(arrivalDate), countryService, loyaltyProgramMapper, categories, false);
        assertThat(fullName(travellers.get(0))).isEqualTo("P1 P1");
        assertThat(fullName(travellers.get(1))).isEqualTo("P2 P2");
        assertThat(fullName(travellers.get(2))).isEqualTo("CH1 CH1");
        assertThat(fullName(travellers.get(3))).isEqualTo("INF1 AS CHILD");
        assertThat(fullName(travellers.get(4))).isEqualTo("INF2 AS INFANT");
        assertThat(travellers.stream().map(TravellerInfo::getCategory).collect(toList())).isEqualTo(categories);
    }

    @Test
    public void passengerCategories_validationErrors() {
        String arrivalDate = "2020-12-26";
        AviaGeobaseCountryService countryService = Mockito.mock(AviaGeobaseCountryService.class);
        AirlineLoyaltyProgramCodeMapper loyaltyProgramMapper = new AirlineLoyaltyProgramCodeMapperImpl();
        List<PassengerCategory> categories = List.of(ADULT, CHILD, INFANT);

        assertThatThrownBy(() ->
                AviaOrderService.convertTravellers(
                        List.of(travellerForm("P1", "P1", "2010-01-01")), mockTdInfo(arrivalDate), countryService,
                        loyaltyProgramMapper,
                        List.of(ADULT),
                        true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected the CHILD category for the passenger of age 10 but got ADULT");

        assertThatThrownBy(() ->
                AviaOrderService.convertTravellers(
                        List.of(travellerForm("CH1", "CH1", "1980-01-01")), mockTdInfo(arrivalDate), countryService,
                        loyaltyProgramMapper,
                        List.of(CHILD),
                        true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected the ADULT category for the passenger of age 40 but got CHILD");

        assertThatThrownBy(() ->
                AviaOrderService.convertTravellers(
                        List.of(travellerForm("INF2", "AS INFANT", "2010-07-01")), mockTdInfo(arrivalDate),
                        countryService, loyaltyProgramMapper,
                        List.of(INFANT),
                        true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected the CHILD category for the passenger of age 10 but got INFANT");
    }

    private static String fullName(TravellerInfo traveller) {
        return traveller.getFirstName()
                + (traveller.getMiddleName() != null ? " " + traveller.getMiddleName() : "")
                + " " + traveller.getLastName()
                + (traveller.getLastNameSuffix() != null ? " " + traveller.getLastNameSuffix() : "");
    }

    private static TravellerInfo traveller(String firstName, String lastName) {
        return TravellerInfo.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private static TravellerFormDTO travellerForm(String firstName, String lastName) {
        return travellerForm(firstName, lastName, "1970-01-01");
    }

    private static TravellerFormDTO travellerForm(String firstName, String lastName, String dateOfBirth) {
        return TravellerFormDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .citizenship("RU")
                .dateOfBirth(LocalDate.parse(dateOfBirth))
                .build();
    }

    private AviaTdInfo mockTdInfo() {
        return mockTdInfo("2020-05-26");
    }

    private AviaTdInfo mockTdInfo(String arrivalDate) {
        AviaTdInfo tdData = new AviaTdInfo();
        tdData.setSegments(List.of(AviaTdSegment.builder()
                .segments(List.of(AviaTdFlight.builder()
                        .arrivalDateTime(LocalDateTime.parse(arrivalDate + "T18:34"))
                        .build()))
                .build()));
        return tdData;
    }
}

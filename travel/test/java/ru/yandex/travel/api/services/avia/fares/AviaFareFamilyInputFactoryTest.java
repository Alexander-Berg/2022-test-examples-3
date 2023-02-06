package ru.yandex.travel.api.services.avia.fares;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.avia.booking.partners.gateways.model.search.Flight;
import ru.yandex.avia.booking.partners.gateways.model.search.PriceInfo;
import ru.yandex.avia.booking.partners.gateways.model.search.Segment;
import ru.yandex.avia.booking.partners.gateways.model.search.Variant;
import ru.yandex.travel.api.services.avia.references.AviaGeoDataService;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.dictionaries.avia.AviaAirportDictionary;
import ru.yandex.travel.api.services.dictionaries.avia.AviaSettlementDictionary;
import ru.yandex.travel.dicts.avia.TAirport;
import ru.yandex.travel.dicts.avia.TSettlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("FieldMayBeFinal")
public class AviaFareFamilyInputFactoryTest {
    private static AviaAirportDictionary airportDictionary = Mockito.mock(AviaAirportDictionary.class);
    private static AviaSettlementDictionary settlementDictionary = Mockito.mock(AviaSettlementDictionary.class);
    private static AviaGeobaseCountryService geobaseCountryService = Mockito.mock(AviaGeobaseCountryService.class);
    private static AviaFareFamilyInputFactory testConverter = new AviaFareFamilyInputFactory(
            new AviaGeoDataService(airportDictionary, settlementDictionary, geobaseCountryService)
    );

    @Before
    public void init() {
        List<TAirport> testAirports = List.of(
                TAirport.newBuilder().setId(9600213L).setIataCode("SVO").setSettlementId(213L).build(),
                TAirport.newBuilder().setId(9600366L).setIataCode("LED").setSettlementId(2L).build(),
                TAirport.newBuilder().setId(9600371L).setIataCode("JFK").setSettlementId(202L).build()
        );
        for (TAirport testAirport : testAirports) {
            when(airportDictionary.getByIataCode(testAirport.getIataCode())).thenReturn(testAirport);
        }
        List<TSettlement> testSettlements = List.of(
                TSettlement.newBuilder().setId(213L).setIataCode("MOW").setCountryId(225).build(),
                TSettlement.newBuilder().setId(2L).setIataCode("LED").setCountryId(225).build(),
                TSettlement.newBuilder().setId(202L).setIataCode("NYC").setCountryId(84).build()
        );
        for (TSettlement testSettlement : testSettlements) {
            when(settlementDictionary.getById(testSettlement.getId())).thenReturn(testSettlement);
        }
        when(geobaseCountryService.getIsoName(225)).thenReturn("RU");
        when(geobaseCountryService.getIsoName(84)).thenReturn("US");
    }

    @Test
    public void testInputConversion() {
        Map<String, String> fareCodes = Map.of("seg1", "YFOX", "seg2", "YFOX");
        Document legDoc = testConverter.convertVariant(testVariant("RU", testSegment("SVO", "LED", "JFK")), fareCodes);

        assertThat(legDoc).isNotNull();
        Node seg1 = legDoc.selectSingleNode("Leg/Seg[1]");
        assertThat(seg1.valueOf("FareCodePrefix")).isEqualTo("YFO");
        assertThat(seg1.valueOf("FromAirport")).isEqualTo("SVO");
        assertThat(seg1.valueOf("FromCountry")).isEqualTo("RU");
        assertThat(seg1.valueOf("ToAirport")).isEqualTo("LED");
        assertThat(seg1.valueOf("ToCountry")).isEqualTo("RU");
        assertThat(seg1.valueOf("DepartureTs")).isEqualTo("1554076800");
        assertThat(seg1.valueOf("ArrivalTs")).isEqualTo("1554148800");

        Node seg2 = legDoc.selectSingleNode("Leg/Seg[2]");
        assertThat(seg2.valueOf("FareCodePrefix")).isEqualTo("YFO");
        assertThat(seg2.valueOf("FromAirport")).isEqualTo("LED");
        assertThat(seg2.valueOf("FromCountry")).isEqualTo("RU");
        assertThat(seg2.valueOf("ToAirport")).isEqualTo("JFK");
        assertThat(seg2.valueOf("ToCountry")).isEqualTo("US");
        assertThat(seg2.valueOf("DepartureTs")).isEqualTo("1554163200");
        assertThat(seg2.valueOf("ArrivalTs")).isEqualTo("1554235200");
    }

    @Test
    public void testMultipleTicketsInputConversion() {
        Map<String, String> mockFareCodes = Mockito.spy(new HashMap<>());
        when(mockFareCodes.get(anyString())).thenReturn("FARE1");

        Document legDocs4 = testConverter.convertVariant(testVariant("RU", testSegment("SVO", "LED", "JFK", "LED", "SVO")), mockFareCodes);
        assertThat(segmentDepartures(legDocs4)).isEqualTo(List.of("SVO", "LED", "JFK", "LED"));

        assertThatThrownBy(() -> testConverter.convertVariant(testVariant("RU", testSegment("SVO", "LED", "JFK", "UFA", "KUF", "SVO")), mockFareCodes))
                .isExactlyInstanceOf(AviaFareRulesException.class)
                .hasMessageContaining("Multiple tickets aren't supported");
    }

    @Test
    public void testNoCountryOfSale() {
        Map<String, String> mockFareCodes = Mockito.spy(new HashMap<>());
        when(mockFareCodes.get(anyString())).thenReturn("FARE1");

        assertThatThrownBy(() -> testConverter.convertVariant(testVariant(null, testSegment("SVO", "LED")), mockFareCodes))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("countryOfSale can not be null or empty");
    }

    private List<String> segmentDepartures(Document ticketData) {
        return ticketData.selectNodes("/Leg/Seg/FromAirport").stream()
                .map(Node::getText)
                .collect(Collectors.toList());
    }

    private Variant testVariant(String countryOfSale, Segment... segments) {
        return Variant.builder()
                .segments(List.of(segments))
                .priceInfo(PriceInfo.builder().id("offer1").build())
                .countryOfSale(countryOfSale)
                .build();
    }

    private Segment testSegment(String... airports) {
        List<Flight> flights = new ArrayList<>();
        LocalDate departure = LocalDate.parse("2019-04-01");
        for (int i = 0; i < airports.length - 1; i++) {
            flights.add(Flight.builder()
                    .id("seg" + (i + 1))
                    .depCode(airports[i])
                    .arrCode(airports[i + 1])
                    .departureDateTime(departure.atStartOfDay())
                    .arrivalDateTime(departure.atTime(LocalTime.parse("20:00:00")))
                    .build());
            departure = departure.plusDays(1);
        }
        return Segment.builder()
                .flights(flights)
                .build();
    }
}

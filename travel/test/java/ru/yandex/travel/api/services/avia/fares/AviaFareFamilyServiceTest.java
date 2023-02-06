package ru.yandex.travel.api.services.avia.fares;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.dom4j.DocumentHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.avia.booking.ff.model.ChargeValue;
import ru.yandex.avia.booking.ff.model.FareFamily;
import ru.yandex.avia.booking.ff.model.SegmentFare;
import ru.yandex.avia.booking.ff.model.TermAvailability;
import ru.yandex.avia.booking.ff.model.TermValue;
import ru.yandex.avia.booking.partners.gateways.model.search.CategoryPrice;
import ru.yandex.avia.booking.partners.gateways.model.search.FareInfo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SuppressWarnings("FieldMayBeFinal")
public class AviaFareFamilyServiceTest {
    private AviaFareFamilyReference mockReference = Mockito.mock(AviaFareFamilyReference.class);
    private AviaAirportDictionary mockAirportCacheService = Mockito.mock(AviaAirportDictionary.class);
    private AviaSettlementDictionary mockSettlementCacheService = Mockito.mock(AviaSettlementDictionary.class);
    private AviaGeobaseCountryService mockGeobaseCountryService = Mockito.mock(AviaGeobaseCountryService.class);
    private AviaFareFamilyInputFactory testInputFactory = Mockito.spy(new AviaFareFamilyInputFactory(
            new AviaGeoDataService(mockAirportCacheService, mockSettlementCacheService, mockGeobaseCountryService)));
    private AviaFareFamilyService testService = new AviaFareFamilyService(mockReference, testInputFactory);

    private AviaFareFamilyProperties realResources = RealResources();
    private AviaFareFamilyReference realReference = new AviaFareFamilyReference(
            AviaFareFamilyReferenceParser.loadFareFamilies(realResources.getFareFamiliesFile()),
            AviaFareFamilyReferenceParser.loadExternalExpressions(realResources.getExternalExpressionsFile())
    );
    private AviaFareFamilyService realService = new AviaFareFamilyService(realReference, testInputFactory);

    protected AviaFareFamilyProperties RealResources() {
        return new AviaFareFamilyProperties()
                .setFareFamiliesFile("avia/fare_families/aeroflot_fare_families_v2_from_avia.json")
                .setExternalExpressionsFile("avia/fare_families/aeroflot_ff_expressions.xml");
    }

    @Test
    public void testCheckValidMainTariff() {
        when(mockReference.isFareCodeSupported(eq("FARE1"))).thenReturn(true);
        testService.checkUnknownFareCodesForRequestedVariant(testVariant("FARE1"));
    }

    @Test
    public void testCheckInvalidMainTariff() {
        when(mockReference.isFareCodeSupported(eq("FARE2"))).thenReturn(false);
        Assertions.assertThatExceptionOfType(AviaUnknownFareFamilyException.class)
                .isThrownBy(() -> testService.checkUnknownFareCodesForRequestedVariant(testVariant("FARE2")));
    }

    @Test
    public void testRemoveAlternativeOffersWithUnknownTariffs() {
        when(mockReference.isFareCodeSupported(any())).thenThrow(new RuntimeException("illegal scenario"));
        doReturn(true).when(mockReference).isFareCodeSupported(eq("FARE1"));
        doReturn(false).when(mockReference).isFareCodeSupported(eq("FARE2"));
        doReturn(false).when(mockReference).isFareCodeSupported(eq("FARE3"));
        doReturn(true).when(mockReference).isFareCodeSupported(eq("FARE4"));

        Variant variant = testVariant("FARE1", "FARE2", "FARE3", "FARE4");
        assertThat(variant.getAllTariffs().size()).isEqualTo(4);

        testService.removeOffersWithUnknownFareCodes(variant);

        assertThat(variant.getAllTariffs().size()).isEqualTo(2);
        assertThat(variant.getAllTariffs().get(0).getCategoryPrices().get(0).getFareInfo().get(0).getFareBasis()).isEqualTo("FARE1");
        assertThat(variant.getAllTariffs().get(1).getCategoryPrices().get(0).getFareInfo().get(0).getFareBasis()).isEqualTo("FARE4");
    }

    @Test
    // no miles anymore
    @Ignore
    public void testMultipleCodesFromSameFamily() {
        when(mockAirportCacheService.getByIataCode(any())).thenReturn(TAirport.newBuilder().build());
        when(mockSettlementCacheService.getById(any())).thenReturn(TSettlement.newBuilder().build());
        when(mockGeobaseCountryService.getIsoName(any())).thenReturn("c1");
        Map<String, SegmentFare> terms = realService.getFareTerms(testVariant(List.of("fl1", "fl2"), List.of("YFO", "QFO")), "ru");
        assertThat(terms.get("fl1").getTerms().get("miles").getMiles()).isEqualTo("200%");
        assertThat(terms.get("fl2").getTerms().get("miles").getMiles()).isEqualTo("150%");
    }

    @Test
    public void testMultipleCodesFromDifferentFamilies() {
        when(mockAirportCacheService.getByIataCode(any())).thenReturn(TAirport.newBuilder().build());
        when(mockSettlementCacheService.getById(any())).thenReturn(TSettlement.newBuilder().build());
        when(mockGeobaseCountryService.getIsoName(any())).thenReturn("c1");
        assertThatThrownBy(() -> realService.getFareTerms(testVariant(List.of("fl1", "fl2"), List.of("YFO", "YNO")), "ru"))
                .isExactlyInstanceOf(AviaFareRulesException.class)
                .hasMessageContaining("Don't know how to combine fare families");
    }

    @Test
    public void testMultipleCodesFromDifferentCombinableFamilies() {
        when(mockAirportCacheService.getByIataCode(any())).thenReturn(TAirport.newBuilder().build());
        when(mockSettlementCacheService.getById(any())).thenReturn(TSettlement.newBuilder().build());
        when(mockGeobaseCountryService.getIsoName(any())).thenReturn("c1");
        Map<String, SegmentFare> terms = realService.getFareTerms(testVariant(List.of("fl1", "fl2"), List.of("ECO1", "SCO2")), "ru");
        assertThat(terms.get("fl1").getTariffGroupName()).isEqualTo("Эконом ОПТИМУМ");
        assertThat(terms.get("fl2").getTariffGroupName()).isEqualTo("Комфорт ОПТИМУМ");
        // independent terms
        //assertThat(terms.get("fl1").getTerms().get("miles").getMiles()).isEqualTo("100%");
        //assertThat(terms.get("fl2").getTerms().get("miles").getMiles()).isEqualTo("150%");
        assertThat(terms.get("fl1").getTerms().get("baggage").getPlaces()).isEqualTo(1);
        assertThat(terms.get("fl2").getTerms().get("baggage").getPlaces()).isEqualTo(2);
        // strictly combined terms
        assertThat(terms.get("fl1").getTerms().get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(terms.get("fl2").getTerms().get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(terms.get("fl1").getTerms().get("refundable")).isEqualTo(terms.get("fl2").getTerms().get("refundable"));
    }

    @Test
    public void testCombinedTermValuesNotAvailable() {
        doReturn(DocumentHelper.createDocument()).when(testInputFactory).convertVariant(any(), any());
        when(mockReference.getFareFamily(any())).thenReturn(realReference.getFareFamily("YFOX"));
        when(mockReference.getFares((FareFamily) any(), any(), any()))
                .thenReturn(Map.of(
                        "fl1", SegmentFare.builder().terms(Map.of("refundable",
                                TermValue.builder().availability(TermAvailability.FREE).build())).build(),
                        "fl2", SegmentFare.builder().terms(Map.of("refundable",
                                TermValue.builder().availability(TermAvailability.NOT_AVAILABLE).build())).build()));
        TermValue refundNotAvailable = TermValue.builder().availability(TermAvailability.NOT_AVAILABLE).build();
        Map<String, SegmentFare> terms = testService.getFareTerms(testVariant(List.of("fl1", "fl2"), List.of("YFO", "YNO")), "ru");
        assertThat(terms.get("fl1").getTerms().get("refundable")).isEqualTo(refundNotAvailable);
        assertThat(terms.get("fl2").getTerms().get("refundable")).isEqualTo(refundNotAvailable);
    }

    @Test
    public void testCombinedTermValuesCharge() {
        doReturn(DocumentHelper.createDocument()).when(testInputFactory).convertVariant(any(), any());
        when(mockReference.getFareFamily(any())).thenReturn(realReference.getFareFamily("YFOX"));
        TermValue termCharge1000Rub = TermValue.builder()
                .availability(TermAvailability.CHARGE)
                .charge(new ChargeValue(BigDecimal.valueOf(1000), "RUB")).build();
        TermValue termCharge50Eur = TermValue.builder()
                .availability(TermAvailability.CHARGE)
                .charge(new ChargeValue(BigDecimal.valueOf(50), "EUR")).build();
        when(mockReference.getFares((FareFamily) any(), any(), any()))
                .thenReturn(Map.of(
                        "fl1", SegmentFare.builder().terms(Map.of("refundable", termCharge1000Rub)).build(),
                        "fl2", SegmentFare.builder().terms(Map.of("refundable", termCharge50Eur)).build()));
        Map<String, SegmentFare> terms = testService.getFareTerms(testVariant(List.of("fl1", "fl2"), List.of("YFO", "YNO")), "ru");
        assertThat(terms.get("fl1").getTerms().get("refundable")).isEqualTo(termCharge50Eur);
        assertThat(terms.get("fl2").getTerms().get("refundable")).isEqualTo(termCharge50Eur);
    }

    private Variant testVariant(String... fareCodes) {
        List<PriceInfo> offers = Stream.of(fareCodes).map(fc -> PriceInfo.builder()
                        .categoryPrices(List.of(CategoryPrice.builder()
                                .fareInfo(List.of(FareInfo.builder()
                                        .fareBasis(fc)
                                        .flightId("fl1")
                                        .build()))
                                .build()))
                        .build())
                .collect(Collectors.toList());
        return Variant.builder()
                .segments(List.of(Segment.builder()
                        .flights(List.of(Flight.builder()
                                .id("fl1")
                                .build()))
                        .build()))
                .priceInfo(offers.get(0))
                .allTariffs(offers)
                .build();
    }

    private Variant testVariant(List<String> segments, List<String> fareCodes) {
        PriceInfo offer = PriceInfo.builder()
                .categoryPrices(List.of(CategoryPrice.builder()
                        .fareInfo(IntStream.range(0, segments.size()).mapToObj(idx -> FareInfo.builder()
                                .flightId(segments.get(idx))
                                .fareBasis(fareCodes.get(idx))
                                .build()
                        ).collect(Collectors.toList()))
                        .build()))
                .build();
        return Variant.builder()
                .segments(List.of(Segment.builder()
                        .flights(segments.stream().map(segId -> Flight.builder()
                                .id(segId)
                                .departureDateTime(LocalDateTime.MIN)
                                .arrivalDateTime(LocalDateTime.MAX)
                                .depCode("DEP")
                                .arrCode("ARR")
                                .build()
                        ).collect(Collectors.toList()))
                        .build()))
                .priceInfo(offer)
                .countryOfSale("RU")
                .build();
    }
}

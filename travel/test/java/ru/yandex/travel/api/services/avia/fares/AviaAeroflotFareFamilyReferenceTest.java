package ru.yandex.travel.api.services.avia.fares;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.avia.booking.enums.ClassOfService;
import ru.yandex.avia.booking.ff.model.ChargeValue;
import ru.yandex.avia.booking.ff.model.SegmentFare;
import ru.yandex.avia.booking.ff.model.TermAvailability;
import ru.yandex.avia.booking.ff.model.TermValue;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class AviaAeroflotFareFamilyReferenceTest {
    private final AviaFareFamilyProperties properties = Properties();
    private final AviaFareFamilyReference reference = new AviaFareFamilyReference(
            AviaFareFamilyReferenceParser.loadFareFamilies(properties.getFareFamiliesFile()),
            AviaFareFamilyReferenceParser.loadExternalExpressions(properties.getExternalExpressionsFile())
    );

    protected AviaFareFamilyProperties Properties() {
        return new AviaFareFamilyProperties()
                .setFareFamiliesFile("avia/fare_families/aeroflot_fare_families_v2_from_avia.json")
                .setExternalExpressionsFile("avia/fare_families/aeroflot_ff_expressions.xml");
    }

    @Test
    public void testDefaultOptions() {
        Map<String, SegmentFare> fares = reference.getFares("QVUX", AviaFareFamilyReferenceTest.legData("QVU", "SVO:RU", "LED:RU"), "ru");
        assertThat(fares.size()).isEqualTo(1);

        SegmentFare fare = fares.get("seg1");
        assertThat(fare.getBaseClass()).isEqualTo(ClassOfService.ECONOMY);
        assertThat(fare.getTariffGroupName()).isEqualTo("Эконом БЮДЖЕТ");

        assertThat(fare.getTerms().size()).isEqualTo(8);
        assertThat(fare.getTerms().get("open_return_date").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        //assertThat(fare.getTerms().get("miles").getMiles()).isEqualTo("75%");
        assertThat(fare.getTerms().get("refundable").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        assertThat(fare.getTerms().get("refundable_no_show").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        assertThat(fare.getTerms().get("changing_carriage")).isEqualTo(charge("RUB", 2800, "За транзакцию"));
        assertThat(fare.getTerms().get("changing_carriage_no_show").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        assertThat(fare.getTerms().get("baggage").getPlaces()).isEqualTo(1);
        assertThat(fare.getTerms().get("baggage").getWeight()).isEqualTo(23);
        assertThat(fare.getTerms().get("baggage").getSize()).isEqualTo("203");
        assertThat(fare.getTerms().get("carry_on")).isEqualTo(TermValue.builder().places(1).weight(10).size("55x40x25").specialNotes(
                singletonList("Габариты одного места ручной клади не должны превышать: 55 см в длину, 40 см в ширину, 25 см в высоту")).build());
        assertThat(fare.getTerms().get("disclosure_url").getSpecialNotes())
                .isEqualTo(singletonList("https://www.aeroflot.ru/ru-ru/information/purchase/rate/fare_rules"));
    }

    @Test
    public void testDefaultComfortClassicOptions() {
        Map<String, SegmentFare> fares = reference.getFares("WCOLR", AviaFareFamilyReferenceTest.legData("WCO", "SVO:RU", "LED:RU"), "ru");
        assertThat(fares.size()).isEqualTo(1);

        SegmentFare fare = fares.get("seg1");
        assertThat(fare.getBaseClass()).isEqualTo(ClassOfService.PREMIUM_ECONOMY);
        assertThat(fare.getTariffGroupName()).isEqualTo("Комфорт ОПТИМУМ");

        assertThat(fare.getTerms().size()).isEqualTo(8);
        assertThat(fare.getTerms().get("open_return_date").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        //assertThat(fare.getTerms().get("miles").getMiles()).isEqualTo("150%");
        assertThat(fare.getTerms().get("refundable")).isEqualTo(charge("RUB", 2800, "За транзакцию"));
        assertThat(fare.getTerms().get("refundable_no_show").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        assertThat(fare.getTerms().get("changing_carriage")).isEqualTo(charge("RUB", 2800, "За транзакцию"));
        assertThat(fare.getTerms().get("changing_carriage_no_show").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        assertThat(fare.getTerms().get("baggage").getPlaces()).isEqualTo(2);
        assertThat(fare.getTerms().get("baggage").getWeight()).isEqualTo(23);
        assertThat(fare.getTerms().get("carry_on")).isEqualTo(TermValue.builder().places(1).weight(10).size("55x40x25").specialNotes(
                singletonList("Габариты одного места ручной клади не должны превышать: 55 см в длину, 40 см в ширину, 25 см в высоту")).build());
        assertThat(fare.getTerms().get("disclosure_url").getSpecialNotes())
                .isEqualTo(singletonList("https://www.aeroflot.ru/ru-ru/information/purchase/rate/fare_rules"));
    }

    @Test
    public void testDefaultComfortMaximumOptions() {
        Map<String, SegmentFare> fares = reference.getFares("WFMXX", AviaFareFamilyReferenceTest.legData("WFM", "SVO:RU", "LED:RU"), "ru");
        assertThat(fares.size()).isEqualTo(1);

        SegmentFare fare = fares.get("seg1");
        assertThat(fare.getBaseClass()).isEqualTo(ClassOfService.PREMIUM_ECONOMY);
        assertThat(fare.getTariffGroupName()).isEqualTo("Комфорт МАКСИМУМ");

        assertThat(fare.getTerms().size()).isEqualTo(8);
        assertThat(fare.getTerms().get("open_return_date").getAvailability()).isEqualTo(TermAvailability.FREE);
        //assertThat(fare.getTerms().get("miles").getMiles()).isEqualTo("200%");
        assertThat(fare.getTerms().get("refundable").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(fare.getTerms().get("refundable_no_show").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(fare.getTerms().get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(fare.getTerms().get("changing_carriage_no_show").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(fare.getTerms().get("baggage").getPlaces()).isEqualTo(2);
        assertThat(fare.getTerms().get("baggage").getWeight()).isEqualTo(23);
        assertThat(fare.getTerms().get("carry_on")).isEqualTo(TermValue.builder().places(1).weight(10).size("55x40x25").specialNotes(
                singletonList("Габариты одного места ручной клади не должны превышать: 55 см в длину, 40 см в ширину, 25 см в высоту")).build());
        assertThat(fare.getTerms().get("disclosure_url").getSpecialNotes())
                .isEqualTo(singletonList("https://www.aeroflot.ru/ru-ru/information/purchase/rate/fare_rules"));
    }

    // this term rule is not supported at the moment and disabled in our reference
    @Ignore
    @Test
    public void testKoreaRefundSpecialRule() {
        TermValue genericValue = getTermExt("RU", "RSXX", "seg1", "SVO:RU", "ICN:KR").get("refundable");
        assertThat(genericValue.getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);

        TermValue koreaValue = getTermExt("KR", "RSXX", "seg1", "ICN:KR", "SVO:RU").get("refundable");
        assertThat(koreaValue).isEqualTo(TermValue.builder()
                .availability(TermAvailability.CHARGE)
                .charge(new ChargeValue(new BigDecimal("40"), "USD"))
                .specialNotes(singletonList("За транзакцию"))
                .build());
    }

    @Test
    public void testChangeCarriageSpecialRules() {
        TermValue rubValue = getTermExt("RU", "RSXX", "seg1", "SVO:RU", "LED:RU").get("changing_carriage");
        assertThat(rubValue).isEqualTo(charge("RUB", 2800, "За транзакцию"));

        TermValue jpyValue = getTermExt("JP", "RSXX", "seg1", "NRT:JP", "LED:RU").get("changing_carriage");
        assertThat(jpyValue).isEqualTo(charge("JPY", 4900, "За транзакцию"));
        // Since RASPTICKETS-21812 segment-related charges no longer depend on CountryOfSale
        // TermValue jpyFromRuValue = getTermExt("JP", "RSXX", "seg1", "VVO:RU", "LED:RU").get("changing_carriage");
        // assertThat(jpyFromRuValue).isEqualTo(charge("RUB", 2800, "За транзакцию"));

        TermValue eurValue = getTermExt("DE", "RSXX", "seg1", "PAR:FR", "LED:RU").get("changing_carriage");
        assertThat(eurValue).isEqualTo(charge("EUR", 37, "За транзакцию"));
    }

    @Test
    public void testChangeCarriageSpecialRulesWithTransitions() {
        Map<String, SegmentFare> rubFares = getFaresExt("RU", "RSXX", "SVO:RU", "SVX:RU", "LED:RU");
        assertThat(rubFares.get("seg1").getTerms().get("changing_carriage")).isEqualTo(charge("RUB", 2800, "За транзакцию"));
        assertThat(rubFares.get("seg2").getTerms().get("changing_carriage")).isEqualTo(charge("RUB", 2800, "За транзакцию"));

        Map<String, SegmentFare> jpyFares = getFaresExt("JP", "RSXJ", "NRT:JP", "SVO:RU", "LED:RU");
        assertThat(jpyFares.get("seg1").getTerms().get("changing_carriage")).isEqualTo(charge("JPY", 4900, "За транзакцию"));
        // Since RASPTICKETS-21812 segment-related charges no longer depend on CountryOfSale
        // assertThat(jpyFares.get("seg2").getTerms().get("changing_carriage")).isEqualTo(charge("JPY", 4900, "За транзакцию"));

        Map<String, SegmentFare> eurFares = getFaresExt("FR", "RSXX", "PAR:FR", "SVO:RU", "SFO:US");
        assertThat(eurFares.get("seg1").getTerms().get("changing_carriage")).isEqualTo(charge("EUR", 37, "За транзакцию"));
        // Since RASPTICKETS-21812 segment-related charges no longer depend on CountryOfSale
        // assertThat(eurFares.get("seg2").getTerms().get("changing_carriage")).isEqualTo(charge("EUR", 37, "За транзакцию"));

        // TODO(RASPTICKETS-21846): tests below have stopped working and should be fixed
        // Map<String, SegmentFare> usdFares = getFaresExt("MX", "RSXX", "SVO:RU", "VVO:CN", "NRT:JP");
        // assertThat(usdFares.get("seg1").getTerms().get("changing_carriage")).isEqualTo(charge("EUR", 37, "За транзакцию"));
        // assertThat(usdFares.get("seg2").getTerms().get("changing_carriage")).isEqualTo(charge("EUR", 37, "За транзакцию"));
    }

    @Test
    // new rules from Avia guys
    @Ignore
    public void testChangeCarriageNoShowBsv() {
        // no stops, the exception should apply
        TermValue allowed = getTerm("YNBX", "seg1", "XXX:KW", "YYY:ES").get("changing_carriage_no_show");
        assertThat(allowed).isEqualTo(charge("USD", 85, "За транзакцию"));

        TermValue naSeg1 = getTerm("YNBX", "seg1", "XXX:KW", "ZZZ:RU", "YYY:ES").get("changing_carriage_no_show");
        assertThat(naSeg1.getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        TermValue naSeg2 = getTerm("YNBX", "seg2", "XXX:KW", "ZZZ:RU", "YYY:ES").get("changing_carriage_no_show");
        assertThat(naSeg2.getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
    }

    @Test
    // new rules from Avia guys
    @Ignore
    public void testChangeCarriageNoShowUs() {
        Map<String, SegmentFare> naValues = getFares("YCLX", "SVO:RU", "SVX:RU", "SFO:US");
        assertThat(naValues.get("seg1").getTerms().get("changing_carriage_no_show")).isEqualTo(availability(TermAvailability.NOT_AVAILABLE, null));
        assertThat(naValues.get("seg2").getTerms().get("changing_carriage_no_show")).isEqualTo(availability(TermAvailability.NOT_AVAILABLE, null));

        Map<String, SegmentFare> naUsValues = getFares("YCLX", "SFO:US", "SVX:RU", "LED:RU");
        assertThat(naUsValues.size()).isEqualTo(2);
        for (SegmentFare fare : naUsValues.values()) {
            TermValue naUsValue = fare.getTerms().get("changing_carriage_no_show");
            assertThat(naUsValue.getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
            //assertThat(naUsValue.getSpecialNotes().get(0)).contains("40USD");
        }
    }

    @Test
    // some new rules from Avia
    @Ignore
    public void testBaggageDelBjsSha() {
        assertThat(getTerm("RSOX", "seg1", "JFK:US", "PEK:CN").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg1", "JFK:US", "NAY:CN").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg1", "SHA:CN", "LGA:US").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg1", "DEL:IN", "EWR:US").get("baggage").getPlaces()).isEqualTo(2);

        // rule exceptions
        assertThat(getTerm("RSOX", "seg1", "SHA:CN", "SVX:RU").get("baggage").getPlaces()).isEqualTo(1);

        assertThat(getTerm("RSOX", "seg1", "SHA:CN", "JFK:US", "SVX:RU").get("baggage").getPlaces()).isEqualTo(1);
        assertThat(getTerm("RSOX", "seg2", "SHA:CN", "JFK:US", "SVX:RU").get("baggage").getPlaces()).isEqualTo(1);

        // exceptions lvl 2
        assertThat(getTerm("RSOX", "seg1", "SHA:CN", "DME:RU", "SVX:RU").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg2", "SHA:CN", "VKO:RU", "SVX:RU").get("baggage").getPlaces()).isEqualTo(2);
    }

    @Test
    // some new rules from Avia
    @Ignore
    public void testBaggageHanSgn() {
        assertThat(getTerm("RSOX", "seg1", "HAN:VN", "MOW:RU").get("baggage").getPlaces()).isEqualTo(1);
        assertThat(getTerm("RSOX", "seg1", "HAN:VN", "MOW:RU:2019-10-28").get("baggage").getPlaces()).isEqualTo(1);

        assertThat(getTerm("RSOX", "seg1", "HAN:VN", "MOW:RU:2019-10-26").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg1", "MOW:RU", "HAN:VN:2019-10-26").get("baggage").getPlaces()).isEqualTo(2);

        assertThat(getTerm("RSOX", "seg1", "HAN:VN", "LGA:US", "SVX:RU:2019-10-26").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg2", "HAN:VN", "LGA:US", "SVX:RU:2019-10-26").get("baggage").getPlaces()).isEqualTo(2);
    }

    @Test
    // for some reason Avia decided to ignore these rules
    @Ignore
    public void testBaggageOtherExtensions() {
        assertThat(getTerm("RSOX", "seg1", "JFK:US", "TLV:IL").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg1", "TLV:IL", "WAS:US").get("baggage").getPlaces()).isEqualTo(2);

        assertThat(getTerm("RSOX", "seg1", "TLV:IL", "MOW:RU", "WAS:US").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg2", "TLV:IL", "MOW:RU", "WAS:US").get("baggage").getPlaces()).isEqualTo(2);

        assertThat(getTerm("RSOX", "seg1", "SVO:RU", "NRT:JP").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg1", "NRT:JP", "LED:RU").get("baggage").getPlaces()).isEqualTo(2);

        assertThat(getTerm("RSOX", "seg1", "NRT:JP", "JFK:US", "ZIA:RU").get("baggage").getPlaces()).isEqualTo(2);
        assertThat(getTerm("RSOX", "seg2", "NRT:JP", "JFK:US", "ZIA:RU").get("baggage").getPlaces()).isEqualTo(2);
    }

    @Test
    // no special rules anymore
    @Ignore
    public void testFlexMowKznRovKhvUus() {
        // defaults
        assertThat(getTerm("YFOX", "seg1", "SVO:RU", "KZN:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(getTerm("BFOX", "seg1", "ROV:RU", "KZN:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(getTerm("BFOX", "seg1", "ROV:RU", "KHV:RU", "SVO:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.FREE);

        // restrictions
        assertThat(getTerm("BFOX", "seg1", "ROV:RU", "SVO:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(getTerm("BFMX", "seg1", "SVO:RU", "KZN:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(getTerm("BFOX", "seg1", "KHV:RU", "UUS:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(getTerm("BFMX", "seg1", "UUS:RU", "KHV:RU").get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
    }

    @Test
    // no special rules anymore
    @Ignore
    public void testClassicMowKznRovKhvUus() {
        assertThat(getTerm("MFLX", "seg1", "SVO:RU", "KZN:RU").get("refundable").getSpecialNotes()).isEqualTo(singletonList("За транзакцию"));
        List<String> extendedNodes = getTerm("LFXX", "seg1", "SVO:RU", "KZN:RU").get("refundable").getSpecialNotes();
        assertThat(extendedNodes.size()).isEqualTo(2);
        assertThat(extendedNodes.get(0)).isEqualTo("За транзакцию");
        assertThat(extendedNodes.get(1)).startsWith("Дополнительно взимается неустойка в размере 25%");

        assertThat(getTerm("LFLX", "seg1", "ROV:RU", "SVO:RU").get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(getTerm("MFXX", "seg1", "SVO:RU", "KZN:RU").get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.FREE);
    }

    @Test
    // no special rules anymore
    @Ignore
    public void testClassicMowKznRovKhvUusCombined() {
        // any LF* segment will cause more strict terms
        for (SegmentFare value : getFaresExt("RU", List.of("SVO:RU", "LED:RU", "KZN:RU"), List.of("LFL", "LFX")).values()) {
            assertThat(value.getTerms().get("refundable").getSpecialNotes().size()).isEqualTo(2);
            assertThat(value.getTerms().get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        }
        for (SegmentFare value : getFaresExt("RU", List.of("SVO:RU", "LED:RU", "KZN:RU"), List.of("LFX", "MFL")).values()) {
            assertThat(value.getTerms().get("refundable").getSpecialNotes().size()).isEqualTo(2);
            assertThat(value.getTerms().get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        }
        for (SegmentFare value : getFaresExt("RU", List.of("SVO:RU", "LED:RU", "KZN:RU"), List.of("MFX", "LFL")).values()) {
            assertThat(value.getTerms().get("refundable").getSpecialNotes().size()).isEqualTo(2);
            assertThat(value.getTerms().get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        }
        for (SegmentFare value : getFaresExt("RU", List.of("SVO:RU", "LED:RU", "KZN:RU"), List.of("MFL", "MFX")).values()) {
            assertThat(value.getTerms().get("refundable").getSpecialNotes().size()).isEqualTo(1);
            assertThat(value.getTerms().get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.FREE);
        }
    }

    @Test
    // no miles anymore
    @Ignore
    public void testRuInternationalCombined() {
        Map<String, SegmentFare> fares = getFaresExt("RU", List.of("SVO:RU", "LED:RU", "NYC:US"), List.of("YNB", "QNO"));
        // combined
        assertThat(fares.get("seg1").getTerms().get("changing_carriage")).isEqualTo(charge("RUB", 2800, "За транзакцию"));
        assertThat(fares.get("seg2").getTerms().get("changing_carriage")).isEqualTo(charge("RUB", 2800, "За транзакцию"));

        // separate
        //assertThat(fares.get("seg1").getTerms().get("miles").getMiles()).isEqualTo("125%");
        //assertThat(fares.get("seg2").getTerms().get("miles").getMiles()).isEqualTo("75%");
    }

    Map<String, TermValue> getTerm(String fareCode, String segmentId, String... segmentPoints) {
        return getFares(fareCode, segmentPoints).get(segmentId).getTerms();
    }

    Map<String, TermValue> getTermExt(String countryOfSale, String fareCode, String segmentId, String... segmentPoints) {
        return getFaresExt(countryOfSale, fareCode, segmentPoints).get(segmentId).getTerms();
    }

    Map<String, TermValue> getTermExt(String countryOfSale, String segmentId, List<String> segmentPoints, List<String> farePrefixes) {
        return getFaresExt(countryOfSale, segmentPoints, farePrefixes).get(segmentId).getTerms();
    }

    Map<String, SegmentFare> getFares(String fareCode, String... segmentPoints) {
        return reference.getFares(fareCode, AviaFareFamilyReferenceTest.legData(fareCode.substring(0, 3), segmentPoints), "ru");
    }

    Map<String, SegmentFare> getFaresExt(String countryOfSale, String fareCode, String... segmentPoints) {
        return reference.getFares(fareCode, AviaFareFamilyReferenceTest.legDataExt(countryOfSale, List.of(segmentPoints), fareCode.substring(0, 3)), "ru");
    }

    Map<String, SegmentFare> getFaresExt(String countryOfSale, List<String> segmentPoints, List<String> farePrefixes) {
        return reference.getFares(farePrefixes.get(0), AviaFareFamilyReferenceTest.legDataExt(countryOfSale, segmentPoints, farePrefixes), "ru");
    }

    TermValue charge(String currency, int value, String specialNotes) {
        return TermValue.builder()
                .availability(TermAvailability.CHARGE)
                .charge(new ChargeValue(BigDecimal.valueOf(value), currency))
                .specialNotes(Strings.isNullOrEmpty(specialNotes) ? emptyList() : singletonList(specialNotes))
                .build();
    }

    TermValue availability(TermAvailability availability, String specialNotes) {
        return TermValue.builder()
                .availability(availability)
                .specialNotes(Strings.isNullOrEmpty(specialNotes) ? null : singletonList(specialNotes))
                .build();
    }

    TermValue baggage(int places, int weight) {
        return TermValue.builder()
                .places(places)
                .weight(weight)
                .size("158")
                .build();
    }
}

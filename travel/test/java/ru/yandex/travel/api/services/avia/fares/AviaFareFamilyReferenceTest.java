package ru.yandex.travel.api.services.avia.fares;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.assertj.core.api.Assertions;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import ru.yandex.avia.booking.ff.model.ChargeValue;
import ru.yandex.avia.booking.ff.model.SegmentFare;
import ru.yandex.avia.booking.ff.model.TermAvailability;
import ru.yandex.avia.booking.ff.model.TermValue;
import ru.yandex.avia.booking.partners.gateways.aeroflot.parsing.XmlUtils;
import ru.yandex.travel.testing.misc.TestResources;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AviaFareFamilyReferenceTest {
    private final AviaFareFamilyProperties testResources = TestResources();
    private final AviaFareFamilyReference testFareFamily = new AviaFareFamilyReference(
            AviaFareFamilyReferenceParser.loadFareFamilies(testResources.getFareFamiliesFile()),
            AviaFareFamilyReferenceParser.loadExternalExpressions(testResources.getExternalExpressionsFile())
    );

    protected AviaFareFamilyProperties TestResources(){
        return  new AviaFareFamilyProperties()
                .setFareFamiliesFile("avia/fare_families_samples/aeroflot/SU_ff_v2_test.json")
                .setExternalExpressionsFile("avia/fare_families/aeroflot_ff_expressions.xml");
    }

    @Test
    public void testDefaultValues() {
        Document legData = XmlUtils.parseXml(TestResources.readResource("avia/fare_families_samples/aeroflot/ff_leg_MOW_LED_no_connects.xml"));
        Map<String, SegmentFare> fares = testFareFamily.getFares("QFOX", legData, "ru");

        // all economy terms (4 business-only aren't included)
        assertThat(fares.keySet().size()).isEqualTo(1);
        SegmentFare fare = fares.get("seg1");
        Map<String, TermValue> terms = fare.getTerms();
        assertThat(terms.size()).isEqualTo(9);
        assertThat(terms.get("open_return_date").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(terms.get("miles").getMiles()).isEqualTo("150%");
        assertThat(terms.get("seat_preselection").getAvailability()).isEqualTo(TermAvailability.FREE);
        assertThat(terms.get("seat_preselection").getSpecialNotes()).isEqualTo(singletonList(
                "Предоставляется без доплаты при наличии билета (только через раздел «Проверить бронирование» на сайте ПАО «Аэрофлот»)"
        ));
        assertThat(terms.get("refundable").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(terms.get("refundable").getCharge()).isEqualTo(new ChargeValue(new BigDecimal("2300"), "RUB"));
        assertThat(terms.get("refundable_no_show").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);
        assertThat(terms.get("changing_carriage").getAvailability()).isEqualTo(TermAvailability.CHARGE);
        assertThat(terms.get("changing_carriage_no_show").getAvailability()).isEqualTo(TermAvailability.NOT_AVAILABLE);

        TermValue baggage = terms.get("baggage");
        assertThat(baggage.getPlaces()).isEqualTo(1);
        assertThat(baggage.getWeight()).isEqualTo(23);
        assertThat(baggage.getSize()).isEqualTo("158 cm");
        assertThat(baggage.getSpecialNotes()).isEqualTo(Arrays.asList(
                "Перевозки экипажей морских судов - 2 места",
                "Элитным участникам программ лояльности ..."
        ));
        assertThat(terms.get("carry_on").getPlaces()).isEqualTo(1);
        assertThat(terms.get("carry_on").getWeight()).isEqualTo(10);
    }

    @Test
    public void testTariffPrefixMatching() {
        Assertions.assertThat(testFareFamily.getFares("YFOX",
                legData("YFO", "SVO:RU", "LED:RU"), "ru"
        ).get("seg1").getTerms().get("miles").getMiles()).isEqualTo("200%");
        Assertions.assertThat(testFareFamily.getFares("QFMX",
                legData("QFM", "SVO:RU", "LED:RU"), "ru"
        ).get("seg1").getTerms().get("miles").getMiles()).isEqualTo("150%");
    }

    @Test
    public void testGeoBasedTerms() {
        Assertions.assertThat(testFareFamily.getFares("YFOX",
                legData("YFO", "ANY:JP", "LED:RU"), "ru"
        ).get("seg1").getTerms().get("refundable").getCharge().getValue()).isEqualTo(BigDecimal.valueOf(4500));
        Assertions.assertThat(testFareFamily.getFares("YFOX",
                legData("YFO", "SVO:RU", "LED:RU"), "ru"
        ).get("seg1").getTerms().get("refundable").getCharge().getValue()).isEqualTo(BigDecimal.valueOf(2300));
    }

    @Test
    public void testExternalExpressions() {
        Assertions.assertThat(testFareFamily.getFares("YFOX",
                legData("YFO", "SHA:CN", "SOME:RU"), "ru"
        ).get("seg1").getTerms().get("baggage").getPlaces()).isEqualTo(2);
        Assertions.assertThat(testFareFamily.getFares("QFMX",
                legData("YFO", "SHA:CN", "КЕЙ:RU"), "ru"
        ).get("seg1").getTerms().get("baggage").getPlaces()).isEqualTo(1);
    }

    @Test
    public void testSpecialNotesInheritance() {
        Assertions.assertThat(testFareFamily.getFares("YFOX",
                legData("YFO", "SVO:RU", "LED:RU"), "ru"
        ).get("seg1").getTerms().get("carry_on").getSpecialNotes()).isEqualTo(singletonList("Elite/ElitePlus members ..."));
        Assertions.assertThat(testFareFamily.getFares("QFMX",
                legData("YFO", "INH_NOTES:RU", "LED:RU"), "ru"
        ).get("seg1").getTerms().get("carry_on").getSpecialNotes()).isEqualTo(singletonList("Переопределённая заметка"));
    }

    @Test
    public void testFailOnNotMatched() {
        assertThatThrownBy(() -> testFareFamily.getFares("AFOX",
                legData("AFO", "SVO:RU", "LED:RU"), "ru"
        )).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("No fare family matches the fare family code: AFOX");
        assertThatThrownBy(() -> testFareFamily.getFares("xFOX",
                legData("xFO", "SVO:RU", "LED:RU"), "ru"
        )).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Can't find term value for every segment: termCode=miles");
    }

    @Test
    public void testBrokenXpathRefs() {
        assertThatThrownBy(() -> new AviaFareFamilyReference(
                AviaFareFamilyReferenceParser.loadFareFamilies(testResources.getFareFamiliesFile()),
                emptyMap()
        ))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Undefined external_xpath_ref: AFL_BAGGAGE_DEL_BJS_SHA_XPATH");
    }

    static Document legData(String tariffPrefix, String... route) {
        return legDataExt("RU", List.of(route), tariffPrefix);
    }

    static Document legDataExt(String countryOfSale, List<String> route, String tariffPrefix) {
        List<String> tariffPrefixes = new ArrayList<>();
        for (int i = 0; i < route.size() - 1; i++) {
            tariffPrefixes.add(tariffPrefix);
        }
        return legDataExt(countryOfSale, route, tariffPrefixes);
    }

    static Document legDataExt(String countryOfSale, List<String> route, List<String> tariffPrefixes) {
        Preconditions.checkArgument(route.size() == tariffPrefixes.size() + 1,
                "there should be N tariff prefixes for N+1 route points");

        Document doc = DocumentHelper.createDocument();
        Element root = DocumentHelper.createElement("Leg");
        root.addElement("CountryOfSale").setText(countryOfSale);
        doc.setRootElement(root);

        for (int i = 0; i < route.size() - 1; i++) {
            Element seg = root.addElement("Seg");
            seg.addElement("Id").setText("seg" + (i + 1));
            seg.addElement("FareCodePrefix").setText(tariffPrefixes.get(i));
            String[] from = route.get(i).split(":");
            seg.addElement("FromAirport").setText(from[0]);
            seg.addElement("FromCountry").setText(from[1]);
            String[] to = route.get(i + 1).split(":");
            seg.addElement("ToAirport").setText(to[0]);
            seg.addElement("ToCountry").setText(to[1]);

            if (to.length > 2) {
                LocalDate date = LocalDate.parse(to[2]);
                seg.addElement("ArrivalTs").setText(date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) + "");
            }
        }

        return doc;
    }
}

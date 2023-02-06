package ru.yandex.avia.booking.partners.gateways.aeroflot.v3;

import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotNdcApiVersion;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotAnonymousTraveller;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotApplicableSegmentRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.AirShoppingRs;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OfferPriceRq;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OfferPriceRs;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3ModelXmlConverter;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3ModelXmlConverterConfig;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3RequestFactory;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3RequestFactoryConfig;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.OfferPriceRequestParams;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.testing.misc.TestResources.readResource;

public class AeroflotNdcApiV3CompatibilityConverterTest {
    private final AeroflotNdcApiV3CompatibilityConverter compatibilityConverter =
            new AeroflotNdcApiV3CompatibilityConverter();
    // we use these external components as they make it easier to prepare test data and compare the results
    private final AeroflotNdcApiV3RequestFactory requestsFactory = new AeroflotNdcApiV3RequestFactory(
            AeroflotNdcApiV3RequestFactoryConfig.builder()
                    .aggregatorId(() -> "AeroflotNdcApiV3CompatibilityConverterTestAggregatorId")
                    .build());
    private final AeroflotNdcApiV3ModelXmlConverter xmlConverter = new AeroflotNdcApiV3ModelXmlConverter(
            AeroflotNdcApiV3ModelXmlConverterConfig.builder()
                    .unknownPropertiesAllowed(false)
                    .prettyPrinterEnabled(true)
                    .build());

    @Test
    public void testAirShoppingRsToOfferPriceRqConversion() {
        String responseXml = readResource("aeroflot/v3/air_shopping_rs_v3_sample.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        String offerId = "2ADT.1CHD-SVO.202010021540.VVO.SU.1700.A.ABSLR-VVO.202010031840.KHV.SU.5602.M.ABSLR-" +
                "KHV.202010121915.VVO.SU.5601.M.ABSLR-VVO.202010130840.SVO.SU.1701.A.ABSLR";
        airShoppingRs = requestsFactory.createTrimmedAirShoppingRsData(airShoppingRs, offerId);
        AeroflotVariant variant = compatibilityConverter.convertToV1Variant(airShoppingRs, offerId, null, null);

        AirShoppingRs restoredAirShoppingRq = compatibilityConverter.convertToV3AirShoppingDataForPriceCheck(variant);
        OfferPriceRq actualOfferPriceRq = offerPriceRq(restoredAirShoppingRq, offerId);
        OfferPriceRq expectedOfferPriceRq = offerPriceRq(airShoppingRs, offerId);

        assertThat(xmlConverter.convertToXml(actualOfferPriceRq))
                .isEqualTo(xmlConverter.convertToXml(expectedOfferPriceRq));
    }

    @Test
    public void testOfferPriceRsConversion() {
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(
                readResource("aeroflot/v3/air_shopping_rs_v3_sample.xml"), AirShoppingRs.class);
        String offerId = "2ADT.1CHD-SVO.202010021540.VVO.SU.1700.A.ABSLR-VVO.202010031840.KHV.SU.5602.M.ABSLR-" +
                "KHV.202010121915.VVO.SU.5601.M.ABSLR-VVO.202010130840.SVO.SU.1701.A.ABSLR";
        airShoppingRs = requestsFactory.createTrimmedAirShoppingRsData(airShoppingRs, offerId);
        AeroflotVariant sourceVariant = compatibilityConverter.convertToV1Variant(airShoppingRs, offerId, null, null);

        OfferPriceRs offerPriceRs = xmlConverter.convertFromXml(
                readResource("aeroflot/v3/offer_price_rs_v3_sample.xml"), OfferPriceRs.class);
        AeroflotVariant updatedVariant = compatibilityConverter.convertToV1Variant(offerPriceRs, sourceVariant);

        assertThat(updatedVariant.getApiVersion()).isEqualTo(AeroflotNdcApiVersion.V3);
        assertThat(updatedVariant.getTravellers().stream()
                .map(AeroflotAnonymousTraveller::getCategory).collect(toList()))
                .isEqualTo(List.of("ADT", "ADT", "CHD"));
        assertThat(updatedVariant.getOriginDestinations().stream()
                .map(od -> od.getDepartureCode() + "/" + od.getArrivalCode()).collect(toList()))
                .isEqualTo(List.of("MOW/KHV", "KHV/MOW"));
        assertThat(updatedVariant.getSegments().stream()
                .map(seg -> seg.getDeparture().getAirportCode() + "/" + seg.getArrival().getAirportCode())
                .collect(toList()))
                .isEqualTo(List.of("SVO/VVO", "VVO/KHV", "KHV/VVO", "VVO/SVO"));
        assertThat(updatedVariant.getOffer().getTotalPrice()).isEqualTo(Money.of(207432, "RUB"));
        assertThat(updatedVariant.getOffer().getOwnerCode()).isEqualTo("SU");
        assertThat(updatedVariant.getOffer().getCategoryOffers().stream()
                .map(co -> co.getId() + "/" + co.getTravellerId() + "/" + co.getTotalPrice().getTotalPrice())
                .collect(toList()))
                .isEqualTo(List.of("1/PAX1_ADT/RUB 74036", "1/PAX2_ADT/RUB 74036", "2/PAX3_CHD/RUB 59360"));
        assertThat(updatedVariant.getOffer().getSegmentRefs().stream()
                .map(AeroflotApplicableSegmentRef::getSegmentId).collect(toList()))
                .isEqualTo(List.of("SEG72_SVOVVO", "SEG73_VVOKHV", "SEG74_KHVVVO", "SEG75_VVOSVO"));
    }

    private OfferPriceRq offerPriceRq(AirShoppingRs airShoppingRs, String offerId) {
        return requestsFactory.createOfferPriceRq(OfferPriceRequestParams.builder()
                .language("RU")
                .countryOfSale("RU")
                .shoppingResponseId("rsp_id")
                .dataLists(airShoppingRs.getResponse().getDataLists())
                .offer(AeroflotNdcApiV3Helper.findOfferById(airShoppingRs, offerId))
                .build());
    }
}

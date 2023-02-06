package ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

import ru.yandex.avia.booking.enums.ClassOfService;
import ru.yandex.avia.booking.enums.PassengerCategory;
import ru.yandex.avia.booking.enums.Sex;
import ru.yandex.avia.booking.model.OriginDestination;
import ru.yandex.avia.booking.model.Passengers;
import ru.yandex.avia.booking.model.SearchRequest;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTicketCouponStatusCode;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTicketDocTypeCode;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.AeroflotNdcApiV3Helper;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.AirShoppingRq;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.AirShoppingRs;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.BaggageAllowance;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.BaggageAllowanceRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.BookingRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.CabinType;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.CarrierAircraftType;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.ContactInfo;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Coupon;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.DataLists;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.DatedOperatingLeg;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.EmailAddress;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.FareCalculationInfo;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.FareComponent;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.FareDetail;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.FarePriceType;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.GenderCode;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.IdentityDoc;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.IdentityDocType;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Individual;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.LoyaltyProgram;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.LoyaltyProgramAccount;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.MarketingCarrierInfo;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Measure;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.MoneyAmount;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Offer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OfferItem;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OfferPriceRq;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OfferPriceRs;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OfferService;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OperatingCarrierInfo;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Order;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OrderCreateRq;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OrderItem;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OrderRetrieveRq;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OrderService;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OrderViewRs;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.OriginDest;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PTC;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Pax;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaxJourney;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaxSegment;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PayloadAttributes;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaymentCard;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaymentInfo;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaymentInstructions;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaymentMethod;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaymentTrx;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PaymentType;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Phone;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PieceAllowance;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PieceDimensionAllowance;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PieceWeightAllowance;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PriceClass;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PriceClassDesc;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.PriceDetalization;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.RBD;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.ScheduledLocation;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.ServiceAssociations;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.ShoppingResponse;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.SoldAirlineInfo;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.StatusMessage;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Tax;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.TaxSummary;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Term;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.TermDesc;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Ticket;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.TicketDocInfo;
import ru.yandex.avia.booking.partners.gateways.model.booking.TravellerInfo;
import ru.yandex.travel.testing.misc.TestResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.avia.booking.enums.DocumentType.BIRTH_CERTIFICATE;
import static ru.yandex.avia.booking.enums.DocumentType.PASSPORT;
import static ru.yandex.avia.booking.enums.PassengerCategory.ADULT;
import static ru.yandex.avia.booking.enums.PassengerCategory.CHILD;
import static ru.yandex.avia.booking.enums.Sex.FEMALE;
import static ru.yandex.avia.booking.enums.Sex.MALE;

/**
 * In reality, also tests the pojo model xml binding annotations and {@linkplain AeroflotNdcApiV3ModelXmlConverter}.
 */
public class AeroflotNdcApiV3RequestFactoryTest {
    private final AeroflotNdcApiV3RequestFactory requestsFactory = new AeroflotNdcApiV3RequestFactory(
            AeroflotNdcApiV3RequestFactoryConfig.builder()
                    .aggregatorId(() -> "Yandex")
                    .apiVersion("18.2")
                    .build());
    private final AeroflotNdcApiV3ModelXmlConverter xmlConverter = new AeroflotNdcApiV3ModelXmlConverter(
            AeroflotNdcApiV3ModelXmlConverterConfig.builder()
                    .unknownPropertiesAllowed(false)
                    .prettyPrinterEnabled(true)
                    .build());

    @Test
    public void airShoppingRqXml() {
        SearchRequest userRequest = SearchRequest.builder()
                .route(List.of(
                        new OriginDestination(LocalDate.parse("2020-10-02"), "MOW", "KHV"),
                        new OriginDestination(LocalDate.parse("2020-10-12"), "KHV", "MOW")
                ))
                .passengers(new Passengers(2, 1, 0))
                .classOfService(ClassOfService.ECONOMY)
                .country("gb")
                .language("en")
                .maxSearchResults(12)
                .build();
        AirShoppingRq airShoppingRq = requestsFactory.createAirShoppingRq(userRequest, false);
        String generatedXml = xmlConverter.convertToXml(airShoppingRq);
        String expectedXml = TestResources.readResource("aeroflot/v3/air_shopping_rq_v3_sample.xml");

        assertThat(generatedXml).isEqualTo(removeLinesWithComments(expectedXml));
    }

    @Test
    public void airShoppingRs() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        assertThat(airShoppingRs.getXmlns()).isEqualTo(AirShoppingRs.SCHEMA);

        DataLists dataLists = airShoppingRs.getResponse().getDataLists();
        assertThat(dataLists.getOriginDestList()).isEqualTo(List.of(
                new OriginDest("OD1_MOWKHV", "MOW", "KHV", List.of("FLT1_SVOKHV", "FLT3_SVOKHV", "FLT45_SVOKHV")),
                new OriginDest("OD2_KHVMOW", "KHV", "MOW", List.of("FLT2_KHVSVO", "FLT4_KHVSVO", "FLT46_KHVSVO"))
        ));
        assertThat(dataLists.getPaxJourneyList()).isEqualTo(List.of(
                new PaxJourney(Duration.parse("PT07H40M"), "FLT1_SVOKHV", List.of("SEG1_SVOKHV")),
                new PaxJourney(Duration.parse("PT08H05M"), "FLT2_KHVSVO", List.of("SEG2_KHVSVO")),
                new PaxJourney(Duration.parse("PT21H20M"), "FLT45_SVOKHV", List.of("SEG72_SVOVVO", "SEG73_VVOKHV")),
                new PaxJourney(Duration.parse("PT22H"), "FLT46_KHVSVO", List.of("SEG74_KHVVVO", "SEG75_VVOSVO"))
        ));
        assertThat(dataLists.getPaxList()).isEqualTo(List.of(
                Pax.builder().paxID("PAX1_ADT").ptc(PTC.ADULT).build(),
                Pax.builder().paxID("PAX2_ADT").ptc(PTC.ADULT).build(),
                Pax.builder().paxID("PAX3_CHD").ptc(PTC.CHILD).build()
        ));
        assertThat(dataLists.getPaxSegmentList()).hasSize(6);
        assertThat(dataLists.getPaxSegmentList()).first().isEqualTo(PaxSegment.builder()
                .arrival(new ScheduledLocation(LocalDateTime.parse("2020-10-03T07:55:00"), "KHV", "Новый", null))
                .datedOperatingLeg(DatedOperatingLeg.builder()
                        .arrival(ScheduledLocation.builder().build())
                        .carrierAircraftType(new CarrierAircraftType("77W", "Boeing 777-300ER"))
                        .datedOperatingLegID("OPLEG1_SVOKHV")
                        .dep(ScheduledLocation.builder().build())
                        .build())
                .dep(new ScheduledLocation(LocalDateTime.parse("2020-10-02T17:15:00"), "SVO", "Шереметьево", "B"))
                .duration(Duration.parse("PT07H40M"))
                .marketingCarrierInfo(new MarketingCarrierInfo("SU", "Аэрофлот", "1710"))
                .operatingCarrierInfo(new OperatingCarrierInfo("SU", null, "1710"))
                .paxSegmentID("SEG1_SVOKHV")
                .build());
        assertThat(dataLists.getPaxSegmentList().get(4).getOperatingCarrierInfo())
                .isEqualTo(new OperatingCarrierInfo("HZ", null, "5601"));
        assertThat(dataLists.getPaxSegmentList()).last().satisfies(seg ->
                assertThat(seg.getSegmentTypeCode()).isEqualTo("MP"));
        assertThat(dataLists.getPriceClassList()).hasSize(7).first().isEqualTo(
                new PriceClass("FT", null, "BPXRTRF", "Эконом Плоский", "FARE1_BPXRTRF")
        );

        List<Offer> offers = airShoppingRs.getResponse().getOffersGroup().getCarrierOffers().getOffer();
        List<FareComponent> expectedAdultsFareComponents = List.of(
                fareComponent("S", "Комфорт", "ABSLR", "SEG72_SVOVVO", "A"),
                fareComponent("Y", "Эконом", "ABSLR", "SEG73_VVOKHV", "M"),
                fareComponent("Y", "Эконом", "ABSLR", "SEG74_KHVVVO", "M"),
                fareComponent("S", "Комфорт", "ABSLR", "SEG75_VVOSVO", "A")
        );
        List<FareComponent> expectedChildrenFareComponents = List.of(
                fareComponent("S", "Комфорт", "ABSLR/CH25", "SEG72_SVOVVO", "A"),
                fareComponent("Y", "Эконом", "ABSLR/CH25", "SEG73_VVOKHV", "M"),
                fareComponent("Y", "Эконом", "ABSLR/CH25", "SEG74_KHVVVO", "M"),
                fareComponent("S", "Комфорт", "ABSLR/CH25", "SEG75_VVOSVO", "A")
        );
        assertThat(offers).hasSize(2).last().isEqualTo(Offer.builder()
                .offerID("2ADT.1CHD-SVO.202010021540.VVO.SU.1700.A.ABSLR-VVO.202010031840.KHV.SU.5602.M.ABSLR" +
                        "-KHV.202010121915.VVO.SU.5601.M.ABSLR-VVO.202010130840.SVO.SU.1701.A.ABSLR")
                .offerItem(List.of(
                        OfferItem.builder()
                                .fareDetail(FareDetail.builder()
                                        .fareComponent(expectedAdultsFareComponents)
                                        .farePriceType(FarePriceType.builder()
                                                .farePriceTypeCode("Sell")
                                                .price(priceDetalization(56000, 18036))
                                                .build())
                                        .build())
                                .mandatoryInd(true)
                                .offerItemID("1")
                                .service(service(List.of("PAX1_ADT", "PAX2_ADT"),
                                        List.of("FLT45_SVOKHV", "FLT46_KHVSVO")))
                                .build(),
                        OfferItem.builder()
                                .fareDetail(FareDetail.builder()
                                        .fareComponent(expectedChildrenFareComponents)
                                        .farePriceType(FarePriceType.builder()
                                                .farePriceTypeCode("Sell")
                                                .price(priceDetalization(42000, 17360))
                                                .build())
                                        .build())
                                .mandatoryInd(true)
                                .offerItemID("2")
                                .service(service(List.of("PAX3_CHD"), List.of("FLT45_SVOKHV", "FLT46_KHVSVO")))
                                .build()))
                .ownerCode("SU")
                .ownerTypeCode("ORA")
                .totalPrice(priceDetalization(154000, 53432))
                .webAddressURL("https://afl-test.test.aeroflot.ru/sb/app/ru-ru#/passengers?adults=2&children=1" +
                        "&segments=SVO20201002VVO.SU1700.A.ABSLR.O.CA_VVO20201003KHV.SU5602.M.ABSLR.I.CA" +
                        "-KHV20201012VVO.SU5601.M.ABSLR.O.CA_VVO20201013SVO.SU1701.A.ABSLR.I.CA&referrer=Yandex")
                .build());

        assertThat(airShoppingRs.getResponse().getShoppingResponse()).isEqualTo(ShoppingResponse.builder()
                .ownerCode("SU")
                .shoppingResponseID("MOW.20201002.KHV-KHV.20201012.MOW_2020-09-24T12:42:59.655628")
                .build());

        assertThat(airShoppingRs.getPayloadAttributes()).isEqualTo(PayloadAttributes.builder()
                .correlationID("se-test-dfa36332d2d9fea4aae73519ba5efece807d414a")
                .primaryLangID("ru")
                .timestamp(Instant.parse("2020-09-24T12:42:59Z"))
                .versionNumber("18.2")
                .build());
    }

    @Test
    public void airShoppingRs_trimmedData() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample_large.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);

        AirShoppingRs trimmed = requestsFactory.createTrimmedAirShoppingRsData(airShoppingRs,
                "1ADT-VKO.202012020830.LED.SU.6012.N.NCOR");
        assertThat(trimmed.getXmlns()).isEqualTo(AirShoppingRs.SCHEMA);
        String trimmedXml = xmlConverter.convertToXml(trimmed);
        String expectedXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample_trimmed.xml");

        assertThat(trimmedXml).isEqualTo(removeLinesWithComments(expectedXml));
    }

    @Test
    public void offerPriceRq() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        String offerId = "2ADT.1CHD-SVO.202010021540.VVO.SU.1700.A.ABSLR-VVO.202010031840.KHV.SU.5602.M.ABSLR-" +
                "KHV.202010121915.VVO.SU.5601.M.ABSLR-VVO.202010130840.SVO.SU.1701.A.ABSLR";
        airShoppingRs = requestsFactory.createTrimmedAirShoppingRsData(airShoppingRs, offerId);

        OfferPriceRequestParams params = createOfferPriceRqParams(airShoppingRs, offerId);
        OfferPriceRq offerPriceRq = requestsFactory.createOfferPriceRq(params);
        String generatedXml = xmlConverter.convertToXml(offerPriceRq);
        String expectedXml = TestResources.readResource("aeroflot/v3/offer_price_rq_v3_sample.xml");

        assertThat(generatedXml).isEqualTo(removeLinesWithComments(expectedXml));
    }

    @Test
    public void offerPriceRq_redundantPriceClassesRemoved() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample_trimmed.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        String offerId = airShoppingRs.getResponse().getOffersGroup().getCarrierOffers().getOffer().get(1).getOfferID();

        OfferPriceRequestParams params = createOfferPriceRqParams(airShoppingRs, offerId);
        OfferPriceRq offerPriceRq = requestsFactory.createOfferPriceRq(params);

        assertThat(offerPriceRq.getRequest().getDataLists().getPriceClassList()).hasSize(1)
                .first().satisfies(pc -> assertThat(pc.getPriceClassID()).isEqualTo("FARE2_NCOR"));
    }

    @Test
    public void offerPriceRs() {
        String responseXml = TestResources.readResource("aeroflot/v3/offer_price_rs_v3_sample.xml");
        OfferPriceRs offerPriceRs = xmlConverter.convertFromXml(responseXml, OfferPriceRs.class);
        assertThat(offerPriceRs.getXmlns()).isEqualTo(OfferPriceRs.SCHEMA);

        // mainly testing the new data not shown in AirShoppingRS
        DataLists dataLists = offerPriceRs.getResponse().getDataLists();
        assertThat(dataLists.getBaggageAllowanceList()).isEqualTo(List.of(
                BaggageAllowance.builder()
                        .baggageAllowanceID("BAG1_SVOVVO_ADT")
                        .pieceAllowance(new PieceAllowance("Traveler", null, null, null, 2))
                        .typeCode("Checked")
                        .build(),
                BaggageAllowance.builder()
                        .baggageAllowanceID("CARRY1_SVOVVO_ADT")
                        .descText(List.of("Не более 10 кг (22 фунтов)", "Не более 55 x 40 x 25 см"))
                        .pieceAllowance(PieceAllowance.builder()
                                .applicablePartyText("Traveler")
                                .pieceDimensionAllowance(List.of(
                                        new PieceDimensionAllowance("Length",
                                                new Measure(new BigDecimal(55), "Centimeter")),
                                        new PieceDimensionAllowance("Width",
                                                new Measure(new BigDecimal(40), "Centimeter")),
                                        new PieceDimensionAllowance("Height",
                                                new Measure(new BigDecimal(25), "Centimeter"))
                                ))
                                .pieceWeightAllowance(new PieceWeightAllowance(new Measure(new BigDecimal(10),
                                        "Kilogram")))
                                .totalQty(1)
                                .build())
                        .typeCode("CarryOn")
                        .build()
        ));
        assertThat(dataLists.getOriginDestList()).hasSize(2);
        assertThat(dataLists.getPaxJourneyList()).hasSize(2);
        assertThat(dataLists.getPaxList()).hasSize(3);
        assertThat(dataLists.getPriceClassList()).hasSize(2);
        assertThat(dataLists.getPriceClassList().get(0).getDesc()).hasSize(7);
        assertThat(dataLists.getPriceClassList().get(0).getDesc().get(0)).isEqualTo(new PriceClassDesc(
                "https://afl-test.test.aeroflot.ru/ru-ru/information/purchase/rate/fare_rules", null));
        assertThat(dataLists.getPriceClassList().get(0).getDesc().get(1)).isEqualTo(new PriceClassDesc(
                null, "Норма бесплатного провоза багажа: 1 место"));
        assertThat(dataLists.getTermsList()).isEqualTo(List.of(
                Term.builder()
                        .desc(TermDesc.builder()
                                .descText("Политика конфиденциальности")
                                .url("https://afl-test.test.aeroflot.ru/ru-ru/information/legal/privacy_policy")
                                .build())
                        .termID("TERM1")
                        .build(),
                Term.builder()
                        .desc(TermDesc.builder()
                                .descText("Договор перевозки")
                                .url("https://afl-test.test.aeroflot.ru/ru-ru/information/legal/contract")
                                .build())
                        .termID("TERM2")
                        .build()
        ));

        Offer offer = offerPriceRs.getResponse().getPricedOffer();
        assertThat(offer.getBaggageAllowance()).isEqualTo(List.of(
                BaggageAllowanceRef.builder()
                        .baggageAllowanceRefID("BAG1_SVOVVO_ADT")
                        .paxJourneyRefID("FLT45_SVOKHV")
                        .paxRefID(List.of("PAX1_ADT", "PAX2_ADT"))
                        .build(),
                BaggageAllowanceRef.builder()
                        .baggageAllowanceRefID("CARRY8_VVOSVO_CHD")
                        .paxJourneyRefID("FLT46_KHVSVO")
                        .paxRefID(List.of("PAX3_CHD"))
                        .build()
        ));
        PriceDetalization priceDetalization = offer.getOfferItem().get(0).getFareDetail().getFarePriceType().getPrice();
        assertThat(priceDetalization.getTaxSummary()).hasSize(1);
        assertThat(priceDetalization.getTaxSummary().get(0).getTax()).hasSize(4).first().isEqualTo(Tax.builder()
                .amount(new MoneyAmount(new BigDecimal(15600), "RUB"))
                .descText("Аэропортовый сбор")
                .taxCode("YQF")
                .build());
        assertThat(offer.getTotalPrice().getTaxSummary()).hasSize(1);
        assertThat(offer.getTotalPrice().getTaxSummary().get(0).getTax()).hasSize(4).first().isEqualTo(Tax.builder()
                .amount(new MoneyAmount(new BigDecimal(1696), "RUB"))
                .descText("Терминальный сбор, Российская Федерация")
                .taxCode("RI4")
                .build());

        assertThat(offerPriceRs.getResponse().getWarning()).isEqualTo(StatusMessage.builder()
                .code("727")
                .descText("Invalid amount")
                .langCode("en")
                .typeCode("9321")
                .build());
    }

    @Test
    public void orderCreateRq() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        String offerId = "2ADT.1CHD-SVO.202010021540.VVO.SU.1700.A.ABSLR-VVO.202010031840.KHV.SU.5602.M.ABSLR-" +
                "KHV.202010121915.VVO.SU.5601.M.ABSLR-VVO.202010130840.SVO.SU.1701.A.ABSLR";
        airShoppingRs = requestsFactory.createTrimmedAirShoppingRsData(airShoppingRs, offerId);

        OrderCreateRequestParams params = orderCreateRqParams(airShoppingRs, offerId, List.of(
                travellerBuilder(ADULT, MALE, "Perviy", "Surnameov", "1970-01-01", "123456789")
                        .middleName("Patr").build(),
                travellerBuilder(ADULT, FEMALE, "Vtoraya", "Surnameova", "1975-10-11", "321456789")
                        .lastNameSuffix("III").build(),
                travellerBuilder(CHILD, MALE, "Tretiy", "Surnameov", "2012-07-09", "921972414")
                        .lastNameSuffix("Jr").nationalityCode("cn").documentType(BIRTH_CERTIFICATE).build()
        ));
        OrderCreateRq orderCreateRq = requestsFactory.createOrderCreateRq(params);
        String generatedXml = xmlConverter.convertToXml(orderCreateRq);
        String expectedXml = TestResources.readResource("aeroflot/v3/order_create_rq_v3_sample.xml");

        assertThat(generatedXml).isEqualTo(removeLinesWithComments(expectedXml));
    }

    @Test
    public void createOrderRq_countryCodesConversion() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample_trimmed.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        String offerId = airShoppingRs.getResponse().getOffersGroup().getCarrierOffers().getOffer().get(0).getOfferID();

        OrderCreateRequestParams params = orderCreateRqParams(airShoppingRs, offerId,
                List.of(singleTestTravellerBuilder().nationalityCode("ab").build()));
        OrderCreateRq orderCreateRq = requestsFactory.createOrderCreateRq(params);
        Pax passenger = orderCreateRq.getRequest().getDataLists().getPaxList().get(0);

        // AB gets changed to GE
        assertThat(passenger.getIdentityDoc().getCitizenshipCountryCode()).isEqualTo("GE");
        assertThat(passenger.getIdentityDoc().getIssuingCountryCode()).isEqualTo("GE");
    }

    @Test
    public void createOrderRq_redundantPriceClassesRemoved() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample_trimmed.xml");
        AirShoppingRs airShoppingRs = xmlConverter.convertFromXml(responseXml, AirShoppingRs.class);
        String offerId = airShoppingRs.getResponse().getOffersGroup().getCarrierOffers().getOffer().get(2).getOfferID();

        OrderCreateRequestParams params = orderCreateRqParams(airShoppingRs, offerId,
                List.of(singleTestTravellerBuilder().nationalityCode("ab").build()));
        OrderCreateRq orderCreateRq = requestsFactory.createOrderCreateRq(params);

        assertThat(orderCreateRq.getRequest().getDataLists().getPriceClassList()).hasSize(1)
                .first().satisfies(pc -> assertThat(pc.getPriceClassID()).isEqualTo("FARE3_NFOR"));
    }

    @Test
    public void orderCreateRs() {
        String responseXml = TestResources.readResource("aeroflot/v3/order_create_rs_v3_sample.xml");
        OrderViewRs orderViewRs = xmlConverter.convertFromXml(responseXml, OrderViewRs.class);
        assertThat(orderViewRs.getXmlns()).isEqualTo(OrderViewRs.SCHEMA);

        DataLists dataLists = orderViewRs.getResponse().getDataLists();
        assertThat(dataLists.getBaggageAllowanceList()).hasSize(2);
        assertThat(dataLists.getContactInfoList()).isEqualTo(List.of(ContactInfo.builder()
                .contactInfoID("CONTACT1")
                .emailAddress(EmailAddress.builder().emailAddressText("SOME@EXAMPLE.COM").build())
                .phone(Phone.builder().phoneNumber(79111111111L).build())
                .build()));
        assertThat(dataLists.getOriginDestList()).hasSize(2);
        assertThat(dataLists.getPaxJourneyList()).hasSize(2);
        assertThat(dataLists.getPaxList()).hasSize(3);
        assertThat(dataLists.getPaxList().get(0)).isEqualTo(Pax.builder()
                .contactInfoRefID("CONTACT1")
                .identityDoc(new IdentityDoc(LocalDate.parse("1972-05-28"), "RU", LocalDate.parse("2020-10-01"),
                        "9812766654", IdentityDocType.PASSPORT, "RU"))
                .individual(new Individual(GenderCode.MALE, "VASILY VASILYEVICH", "01.01", null, null, "IVANOV"))
                .loyaltyProgramAccount(LoyaltyProgramAccount.builder()
                        .accountNumber("123456789")
                        .loyaltyProgram(new LoyaltyProgram("SU", "Aeroflot Bonus"))
                        .build())
                .paxID("PAX_01.01")
                .ptc(PTC.ADULT)
                .build());
        assertThat(dataLists.getPaxList().get(1).getIndividual().getGivenName()).isEqualTo("VASILY VASILYEVICH JR");
        assertThat(dataLists.getPaxList().get(2).getPaxRefID()).isEqualTo("PAX_01.01");
        assertThat(dataLists.getPaxList().get(2).getPtc()).isEqualTo(PTC.CHILD);
        assertThat(dataLists.getPaxSegmentList()).hasSize(2);

        Order order = orderViewRs.getResponse().getOrder();
        assertThat(order.getOrderID()).isEqualTo("SU555LDXDVR-202009261307");
        assertThat(order.getOrderItem()).hasSize(2);
        OrderItem orderItem = order.getOrderItem().get(0);
        assertThat(orderItem.getFareDetail().getFareCalculationInfo()).isEqualTo(
                new FareCalculationInfo("MOW SU X/VVO SU KHV28000SU X/VVO SU MOW28000RUB56000END", null, null));
        assertThat(orderItem.getFareDetail().getFareComponent()).hasSize(4);
        PriceDetalization price = orderItem.getFareDetail().getFarePriceType().getPrice();
        assertThat(price.getBaseAmount()).isEqualTo(new MoneyAmount(new BigDecimal("56000.00"), "RUB"));
        assertThat(price.getTaxSummary()).hasSize(2);
        assertThat(price.getTaxSummary().get(0).getTax().get(0).getQualifierCode()).isEqualTo("49");
        assertThat(price.getTaxSummary().get(1).getTax().get(0).getQualifierCode()).isEqualTo("73");
        assertThat(orderItem.getOrderItemID()).isEqualTo("FLT-ADT-01.01");
        assertThat(orderItem.getPaymentTimeLimitDateTime()).isEqualTo("2020-09-27T16:17:00Z");
        assertThat(orderItem.getService()).hasSize(4).first().isEqualTo(OrderService.builder()
                .paxRefID(List.of("PAX_01.01"))
                .serviceAssociations(ServiceAssociations.builder()
                        .paxSegmentRefID(List.of("SEG_20201002_SVOVVO_SU1700"))
                        .build())
                .serviceID("SERV-FLIGHT-20201002-SVOVVO-SU1700")
                .build());
        assertThat(order.getWebAddressURI()).startsWith(
                "https://afl-test.test.aeroflot.ru/sb/pnr/app/ru-en#/pnr?pnr_locator=LDXDVR&pnr_key=1019174");

        List<TicketDocInfo> tickets = orderViewRs.getResponse().getTicketDocInfo();
        assertThat(tickets).hasSize(2);
        assertThat(tickets.get(0).getBookingRef()).isEqualTo(BookingRef.builder()
                .bookingID("LDXDVR")
                .bookingRefTypeCode("PNR")
                .build());
        assertThat(tickets.get(0).getEndorsementText()).isEqualTo("P9812766654 NONREF/HEBO3BPATEH");
        assertThat(tickets.get(0).getFareDetail().getFareComponent()).hasSize(4);
        assertThat(tickets.get(0).getFareDetail().getFarePriceType().getPrice().getTotalAmount())
                .isEqualTo(new MoneyAmount(new BigDecimal("74036.00"), "RUB"));
        assertThat(tickets.get(0).getPaxRefID()).isEqualTo("PAX_01.01");
        assertThat(tickets.get(0).getPaymentInfoRefID()).isEqualTo("PAY1");
        Ticket ticket1 = tickets.get(0).getTicket();
        assertThat(ticket1.getCoupon()).hasSize(4).first().isEqualTo(Coupon.builder()
                .baggageAllowanceRefID("BAG1_ABSLR_ADT")
                .couponNumber("1")
                .couponStatusCode(AeroflotTicketCouponStatusCode.OK)
                .fareBasisCode("ABSLR")
                .remarkText("Comfort")
                .serviceRefID("SERV-SEAT-20201002-SVOVVO-SU1700-")
                .soldAirlineInfo(new SoldAirlineInfo("SEG_20201002_SVOVVO_SU1700"))
                .build());
        assertThat(ticket1.getTicketDocTypeCode()).isEqualTo(AeroflotTicketDocTypeCode.TICKET);
        assertThat(ticket1.getTicketNumber()).isEqualTo("5552127962242");

        assertThat(orderViewRs.getPaymentInfo()).isEqualTo(PaymentInfo.builder()
                .amount(MoneyAmount.builder().value(new BigDecimal("207432")).curCode("RUB").build())
                .paymentInfoID("PAY1")
                .paymentMethod(PaymentMethod.builder()
                        .paymentCard(new PaymentCard("VI", "4554", PaymentInstructions.builder()
                                .payerAuthenticationRequestText("Необходима авторизация по 3D Secure / Secure Code")
                                .redirectionURL("https://pay.test.aeroflot.ru/test-rc/aeropayment/epr/payment2.html?" +
                                        "mdOrder=802bb67a-4d6d-4619-bf94-35770259d1f2&language=ru" +
                                        "&message=order_progress_payment#/waiting")
                                .build()))
                        .build())
                .paymentTrx(PaymentTrx.builder().descText("60").trxDataText("630").build())
                .typeCode(PaymentType.CREDIT_CARD)
                .build());
    }

    @Test
    public void orderRetrieveRq() {
        OrderRetrieveRequestParams params = new OrderRetrieveRequestParams("SU555LDXDVR-202009261307", "SU", "RU");
        OrderRetrieveRq orderRetrieveRq = requestsFactory.createOrderRetrieveRq(params);
        String generatedXml = xmlConverter.convertToXml(orderRetrieveRq);
        String expectedXml = TestResources.readResource("aeroflot/v3/order_retrieve_rq_v3_sample.xml");

        assertThat(generatedXml).isEqualTo(removeLinesWithComments(expectedXml));
    }

    @Test
    public void unknownFieldsAndErrors() {
        String responseXml = TestResources.readResource("aeroflot/v3/air_shopping_rs_v3_sample_failure_and_unknown.xml");

        assertThatThrownBy(() -> xmlConverter.convertFromXml(responseXml, AirShoppingRs.class))
                .hasCauseInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining("Unrecognized field \"Abyrvalg\"");

        AeroflotNdcApiV3ModelXmlConverter moreTolerantConvert = new AeroflotNdcApiV3ModelXmlConverter(
                AeroflotNdcApiV3ModelXmlConverterConfig.builder()
                        .unknownPropertiesAllowed(true)
                        .prettyPrinterEnabled(false)
                        .build());
        AirShoppingRs response = moreTolerantConvert.convertFromXml(responseXml, AirShoppingRs.class);

        assertThat(response.getError()).isEqualTo(StatusMessage.builder()
                .code("130")
                .descText("Invalid Origin and Destination Pair")
                .langCode("en")
                .typeCode("9321")
                .build());
    }

    private static String removeLinesWithComments(String xml) {
        // for test purposes only
        return xml.replaceAll("\n *<!--[^\n]*--> *", "");
    }

    private static FareComponent fareComponent(String cabinTypeCode, String cabinTypeName, String fareBasisCode,
                                               String paxSegmentRefId, String rbdCode) {
        return FareComponent.builder()
                .cabinType(new CabinType(cabinTypeCode, cabinTypeName))
                .fareBasisCode(fareBasisCode)
                .paxSegmentRefID(paxSegmentRefId)
                .priceClassRefID("FARE3_ABSLR")
                .rbd(new RBD(rbdCode))
                .build();
    }

    private static PriceDetalization priceDetalization(double equivAmount, double taxAmount) {
        return PriceDetalization.builder()
                .equivAmount(new MoneyAmount(new BigDecimal(equivAmount), "RUB"))
                .taxSummary(List.of(new TaxSummary(null, new MoneyAmount(new BigDecimal(taxAmount), "RUB"))))
                .totalAmount(new MoneyAmount(new BigDecimal(equivAmount + taxAmount), "RUB"))
                .build();
    }

    private static OfferService service(List<String> paxRefs, List<String> journeyRefs) {
        return OfferService.builder()
                .paxRefID(paxRefs)
                .serviceAssociations(ServiceAssociations.builder()
                        .paxJourneyRefID(journeyRefs)
                        .build())
                .serviceID("None")
                .build();
    }

    private static OfferPriceRequestParams createOfferPriceRqParams(AirShoppingRs airShoppingRs, String offerId) {
        return OfferPriceRequestParams.builder()
                .countryOfSale("RU")
                .language("RU")
                .dataLists(airShoppingRs.getResponse().getDataLists())
                .offer(AeroflotNdcApiV3Helper.findOfferById(airShoppingRs, offerId))
                .shoppingResponseId(airShoppingRs.getResponse().getShoppingResponse().getShoppingResponseID())
                .build();
    }

    private OrderCreateRequestParams orderCreateRqParams(AirShoppingRs airShoppingRs, String offerId,
                                                         List<TravellerInfo> passengers) {
        return OrderCreateRequestParams.builder()
                // variant
                .language("RU")
                .countryOfSale("RU")
                .dataLists(airShoppingRs.getResponse().getDataLists())
                .offer(AeroflotNdcApiV3Helper.findOfferById(airShoppingRs, offerId))
                // booking form
                .travellers(passengers)
                .contactEmail("some@example.com")
                .contactPhoneCountryCode(7)
                .contactPhoneNumber(9111111111L)
                // payment
                .tokenizedCard("7918236e-8b47-430b-9a64-4aa7df84ad47")
                .redirectUrl("https://travel-test.yandex.ru/")
                .build();
    }

    private TravellerInfo.TravellerInfoBuilder singleTestTravellerBuilder() {
        return travellerBuilder(ADULT, MALE, "Test", "Testov", "1970-01-01", "1234567890");
    }

    private TravellerInfo.TravellerInfoBuilder travellerBuilder(PassengerCategory category, Sex sex, String firstName,
                                                                String lastName, String dateOfBirth, String docId) {
        return TravellerInfo.builder()
                .category(category)
                .firstName(firstName)
                .lastName(lastName)
                .nationalityCode("ru")
                .dateOfBirth(LocalDate.parse(dateOfBirth))
                .documentNumber(docId)
                .documentValidTill(LocalDate.parse("2021-01-01"))
                .documentType(PASSPORT)
                .sex(sex);
    }
}

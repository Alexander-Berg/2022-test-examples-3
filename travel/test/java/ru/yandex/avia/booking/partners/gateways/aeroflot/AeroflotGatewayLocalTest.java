package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.avia.booking.enums.ClassOfService;
import ru.yandex.avia.booking.enums.DocumentType;
import ru.yandex.avia.booking.enums.PassengerCategory;
import ru.yandex.avia.booking.enums.Sex;
import ru.yandex.avia.booking.model.OriginDestination;
import ru.yandex.avia.booking.model.Passengers;
import ru.yandex.avia.booking.model.SearchRequest;
import ru.yandex.avia.booking.partners.gateways.aeroflot.demo.AeroflotDemoCardTokenizer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderCreateResult;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderStatus;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotServicePayload;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.AirShoppingRs;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.model.Offer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3ModelXmlConverter;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3ModelXmlConverterConfig;
import ru.yandex.avia.booking.partners.gateways.aeroflot.v3.requests.AeroflotNdcApiV3RequestFactory;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckRequest;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckResponse;
import ru.yandex.avia.booking.partners.gateways.model.availability.VariantNotAvailableException;
import ru.yandex.avia.booking.partners.gateways.model.booking.ClientInfo;
import ru.yandex.avia.booking.partners.gateways.model.booking.TravellerInfo;
import ru.yandex.avia.booking.partners.gateways.model.search.PriceInfo;
import ru.yandex.avia.booking.remote.RpcContext;
import ru.yandex.travel.testing.TestUtils;
import ru.yandex.travel.testing.time.SettableClock;

import static java.util.stream.Collectors.joining;

@Disabled
@Slf4j
public class AeroflotGatewayLocalTest {
    private final ObjectMapper tdMapper = AeroflotApiStubsHelper.defaultMapper;
    private final AeroflotNdcApiV3ModelXmlConverter xmlConverter = new AeroflotNdcApiV3ModelXmlConverter(
            AeroflotNdcApiV3ModelXmlConverterConfig.builder().build());

    private final AeroflotGateway gateway;
    private final SettableClock clock;

    public AeroflotGatewayLocalTest() throws Exception {
        // todo(tlg-13): TRAVELBACK-1149: use yav instead
        String authTokenFile = System.getProperty("user.home") + "/.yav-local/aeroflot_client_id";
        //String authTokenFile = System.getProperty("user.home") + "/.yav-local/aeroflot_prod_client_id";
        String promoAuthTokenFile = System.getProperty("user.home") + "/.yav-local/aeroflot_promo_client_id";
        String authToken = Files.readString(Path.of(authTokenFile)).trim();
        String promoAuthToken = Files.readString(Path.of(promoAuthTokenFile)).trim();
        AeroflotProviderProperties config = AeroflotProviderProperties.builder()
                .searchUrl("https://ndc-search.aeroflot.io/api/sb/ndc/v3.0/asrq")
                //.searchUrl("https://ndc-search.aeroflot.io/api/pr/ndc/v3.0/asrq")
                .bookingUrl("https://gw.aeroflot.io/api/sb/ndc/v3.0/ep")
                //.bookingUrl("http://partners-proxy.testing.avia.yandex.net/boy_aeroflot_testing/api/sb/ndc/v3.0/ep")
                //.bookingUrl("http://partners-proxy.production.avia.yandex.net/boy_aeroflot/api/sb/ndc/v3.0/ep")
                .userName("Yandex")
                .readTimeout(Duration.ofMinutes(5))
                .authToken(authToken)
                .contentType("application/x-iata.ndc.v1+xml")
                .enableTestingScenarios(true)
                .promo2020(AeroflotProviderProperties.WhiteMonday2020Promo.builder()
                        .enabled(false)
                        .startsAt(Instant.parse("2020-12-01T05:00:00Z"))
                        .endsAt(Instant.parse("2020-12-10T13:00:00Z"))
                        .userName("Yandex_2")
                        .authToken(promoAuthToken)
                        .build())
                .build();
        clock = new SettableClock();
        gateway = new AeroflotGateway(config, AeroflotApiStubsHelper.defaultAhcClient(), clock);
    }

    @Test
    public void testFlow() {
        clock.setCurrentTime(Instant.now());
        AirShoppingRs airShoppingRs = gateway.searchVariants(searchRequest(
                //"SVO", "KHV", LocalDate.parse("2020-12-04"), LocalDate.parse("2020-12-18"), 10));
                "SVO", "KHV", LocalDate.now().plusWeeks(8), null, 10));
        log.info("Search API returned {} offers", airShoppingRs.getResponse()
                .getOffersGroup().getCarrierOffers().getOffer().size());

        String offerId = airShoppingRs.getResponse().getOffersGroup().getCarrierOffers().getOffer().get(0).getOfferID();
        AeroflotVariant variant = gateway.resolveVariantInfo(tdDataJson(airShoppingRs, offerId));
        AeroflotTotalOffer offer = variant.getOffer();
        log.info("Parsed variant: price={}, tariffs={}, url={}",
                offer.getTotalPrice(), variant.getAllTariffs().size(), offer.getDisclosureUrl());

        AvailabilityCheckResponse res = gateway.checkAvailabilityAll(
                AvailabilityCheckRequest.builder().token(variant).build());
        log.info("Availability check result: price={}", res.getVariant().getPriceInfo().getTotal());

        for (PriceInfo tariff : res.getVariant().getAllTariffs()) {
            log.info("Tariff: id={}, price={}", tariff.getId(), tariff.getTotal());
        }

        AeroflotOrderCreateResult createResult = gateway.initPayment(variant, List.of(travellerInfo()),
                AeroflotDemoCardTokenizer.tokenizeDemoCard(false), clientInfo(),
                "https://travel-test.yandex.ru", RpcContext.empty());
        log.info("Created order status: {}", createResult);

        AeroflotOrderStatus status;
        do {
            status = gateway.getOrderStatus(payloadForStatus(variant, createResult.getOrderRef()), RpcContext.empty())
                    .getStatusCode();
            log.info("Order status: {}", status);
            if (status == AeroflotOrderStatus.PAID_TICKETED) {
                log.info("The order is paid and the tickets are ready, quitting");
                break;
            } else {
                log.info("Will wait for a bit before checking the order status again... 3ds link: {}",
                        createResult.getConfirmationUrl());
                TestUtils.sleep(Duration.ofSeconds(15));
            }
        } while (true);
    }

    @Test
    public void testAllVariants() {
        Stopwatch sw = Stopwatch.createStarted();
        Map<String, String> results = new LinkedHashMap<>();

        AirShoppingRs airShoppingRs = gateway.searchVariants(searchRequest(
                "MOW", "LED", LocalDate.now().plusWeeks(8), null, 300));
        log.info("Search API returned {} offers", airShoppingRs.getResponse()
                .getOffersGroup().getCarrierOffers().getOffer().size());

        for (Offer apiOffer : airShoppingRs.getResponse().getOffersGroup().getCarrierOffers().getOffer()) {
            try {
                AeroflotVariant variant = gateway.resolveVariantInfo(tdDataJson(airShoppingRs, apiOffer.getOfferID()));
                AeroflotTotalOffer offer = variant.getOffer();
                log.info("Checking variant: price={}, tariffs={}, url={}",
                        offer.getTotalPrice(), variant.getAllTariffs().size(), offer.getDisclosureUrl());

                AeroflotVariant updatedVariant = gateway.checkAvailabilitySingle(variant);
                log.info("Availability check result: price={}", updatedVariant.getOffer().getTotalPrice());

                results.put(apiOffer.getOfferID(), String.format("" +
                                "\tSource price: %s\n" +
                                "\tUpdated price: %s\n" +
                                "\tRoute: %s",
                        offer.getTotalPrice(), updatedVariant.getOffer().getTotalPrice(),
                        updatedVariant.getSegments().stream()
                                .map(f -> f.getDeparture().getAirportCode() + "->" + f.getArrival().getAirportCode())
                                .collect(joining(", "))
                ));
                Thread.sleep(1000);
            } catch (VariantNotAvailableException e) {
                results.put(apiOffer.getOfferID(), "Not available: " + e.getMessage());
            } catch (Exception e) {
                results.put(apiOffer.getOfferID(), "Failed: " + e.getMessage());
                log.info("Call failed", e);
            }
        }
        log.info("Full processing took {} seconds", sw.elapsed(TimeUnit.SECONDS));

        log.info("Results:\n{}", results.entrySet().stream()
                .map(e -> e.getKey() + ":\n" + e.getValue())
                .collect(joining("\n")));
    }

    @SuppressWarnings("SameParameterValue")
    private SearchRequest searchRequest(String from, String to, LocalDate date, LocalDate backData,
                                        int maxSearchResults) {
        List<OriginDestination> route = new ArrayList<>();
        route.add(new OriginDestination(date, from, to));
        if (backData != null) {
            route.add(new OriginDestination(backData, to, from));
        }
        return SearchRequest.builder()
                .language("ru")
                .country("ru")
                .classOfService(ClassOfService.ECONOMY)
                .route(route)
                .passengers(new Passengers(1, 0, 0))
                .maxSearchResults(maxSearchResults)
                .build();
    }

    private JsonNode tdDataJson(AirShoppingRs airShoppingRs, String selectedOfferId) {
        int cabinType = AeroflotNdcApiV3RequestFactory.classMapping.get(ClassOfService.ECONOMY).getValue();
        return tdMapper.createObjectNode()
                .set("order_data", tdMapper.createObjectNode()
                        .set("booking_info", tdMapper.createObjectNode()
                                .put("AirShoppingRS", xmlConverter.convertToXml(airShoppingRs))
                                .put("OfferId", selectedOfferId)
                                .put("CountryCode", "RU")
                                .put("LanguageCode", "ru")
                                .put("CabinType", cabinType)
                        )
                );
    }

    private ClientInfo clientInfo() {
        return ClientInfo.builder()
                .userIp("127.0.0.1")
                .userAgent("YandexTestUa")
                .phone("phone")
                .phoneCountryCode(7)
                .phoneNumber(9111111111L)
                .email("some@example.com")
                .build();
    }

    private TravellerInfo travellerInfo() {
        return TravellerInfo.builder()
                .travellerInfoId("someId_" + 1)
                .category(PassengerCategory.ADULT)
                .firstName("Sergey")
                .middleName(null)
                .lastName("Testov")
                .dateOfBirth(LocalDate.of(1970, 1, 1))
                .nationalityCode("RU")
                .documentNumber("123412345" + 0)
                .documentType(DocumentType.PASSPORT)
                .documentValidTill(LocalDate.now().plusYears(5))
                .sex(Sex.MALE)
                .build();
    }

    private AeroflotServicePayload payloadForStatus(AeroflotVariant variant, AeroflotOrderRef orderRef) {
        return AeroflotServicePayload.builder()
                .partnerId("any")
                .variant(variant)
                .travellers(List.of())
                .bookingRef(orderRef)
                .clientInfo(clientInfo())
                .preliminaryCost(Money.of(0, "RUB"))
                .build();
    }
}

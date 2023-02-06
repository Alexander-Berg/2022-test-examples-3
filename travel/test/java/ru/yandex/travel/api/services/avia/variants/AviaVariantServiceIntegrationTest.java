package ru.yandex.travel.api.services.avia.variants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.avia.booking.partners.gateways.BookingGateway;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckResponse;
import ru.yandex.avia.booking.partners.gateways.model.availability.DepartureIsTooCloseException;
import ru.yandex.avia.booking.service.dto.VariantCheckResponseDTO;
import ru.yandex.avia.booking.service.dto.VariantDTO;
import ru.yandex.avia.booking.service.dto.promo.AeroflotPlusPromo2021DTO;
import ru.yandex.avia.booking.services.tdapi.AviaTicketDaemonApiClient;
import ru.yandex.travel.api.services.avia.AviaBookingProviderResolver;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.avia.variants.model.AviaVariantAvailabilityCheck;
import ru.yandex.travel.api.services.avia.variants.repositories.AviaVariantRepository;
import ru.yandex.travel.api.services.dictionaries.avia.AviaAirlineDictionary;
import ru.yandex.travel.api.services.dictionaries.avia.AviaAirportDictionary;
import ru.yandex.travel.api.services.dictionaries.avia.AviaSettlementDictionary;
import ru.yandex.travel.commons.jackson.MoneySerializersModule;
import ru.yandex.travel.dicts.avia.TAirline;
import ru.yandex.travel.dicts.avia.TAirport;
import ru.yandex.travel.dicts.avia.TSettlement;
import ru.yandex.travel.testing.misc.TestResources;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"avia-booking.enabled=true"})
@ActiveProfiles("test")
public class AviaVariantServiceIntegrationTest {

    @Autowired
    AviaVariantService variantService;

    @MockBean
    AviaBookingProviderResolver resolver;

    @Autowired
    AviaVariantRepository variantAvailabilityCheckRepository;

    @MockBean
    AviaGeobaseCountryService geobaseCountryService;

    @MockBean
    AviaAirlineDictionary airlineCacheService;

    @MockBean
    AviaAirportDictionary airportDictionary;

    @MockBean
    AviaSettlementDictionary settlementDictionary;

    @MockBean
    AviaTicketDaemonApiClient aviaTicketDaemonApiClient;

    BookingGateway gateway;

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new MoneySerializersModule());

    @Before
    public void init() {
        // common infrastructure mocks
        when(airlineCacheService.getById(any())).thenReturn(TAirline.newBuilder().build());
        when(airlineCacheService.getByIataCode(any())).thenReturn(TAirline.newBuilder().build());
        when(airportDictionary.getById(any())).thenReturn(TAirport.newBuilder().build());
        when(airportDictionary.getByIataCode(any())).thenReturn(TAirport.newBuilder().build());
        when(settlementDictionary.getById(any())).thenReturn(TSettlement.newBuilder().build());

        gateway = Mockito.mock(BookingGateway.class);
        when(resolver.gatewayForPartner(any(), any())).thenReturn(gateway);
    }

    @Test
    @Transactional
    public void testGetAeroflotVariantFormat() throws Exception {
        AvailabilityCheckResponse response = objectMapper.readValue(TestResources.readResource(
                "avia/aeroflot/aeroflot_check_availability_result.json"),
                AvailabilityCheckResponse.class);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testGetAeroflotVariantFormat");
        when(geobaseCountryService.getIsoName(any())).thenAnswer(call -> "RU");
        JsonNode ticketDaemonInfo = objectMapper.readTree(TestResources.readResource("avia/ticket_daemon/booking_info_by_token_sample.json"));

        VariantCheckResponseDTO result = variantService.checkAvailability(ticketDaemonInfo).join();
        AviaVariantAvailabilityCheck availabilityCheck = variantAvailabilityCheckRepository.getOne(result.getToken().getAvailabilityCheckId());

        JsonNode data = availabilityCheck.getData();
        JsonNode variant = data.path("variant_info").path("variant");
        assertThat(data.path("price_info").path("first_check_price").path("value").intValue()).isEqualTo(151349);
        assertThat(variant.path("id").textValue()).isEqualTo(availabilityCheck.getId() + "!3ADT.2CHD.1INF-" +
                "SVO.201812241740.110.180.240.KUF.SU.1214.N.NFMR-" +
                "KUF.201812242320.145.240.180.LED.SU.6332.N.NFMR-" +
                "LED.201901030025.160.180.300.UFA.SU.6411.N.NFMR-" +
                "UFA.201901030625.135.300.180.SVO.SU.1235.N.NFMR");

        // Segments check
        JsonNode legs = variant.path("legs");
        assertThat(legs.size()).isEqualTo(2);
        List<JsonNode> fls1 = arrayValues(legs.get(0).path("flights"));
        assertThat(fls1.stream().map(f -> f.path("id").textValue()).collect(toList()))
                .isEqualTo(asList("SEG_SVOKUF_1", "SEG_KUFLED_2"));

        // fare terms (same for every segment)
        List<JsonNode> allFf = new ArrayList<>();
        for (JsonNode leg : legs) {
            for (JsonNode flight : leg.get("flights")) {
                allFf.add(flight.get("fareTerms"));
            }
        }
        assertThat(allFf.size()).isEqualTo(4);
        for (JsonNode ff : allFf) {
            assertThat(ff.path("tariffGroupName").textValue()).isEqualTo("Эконом МАКСИМУМ");
            assertThat(Sets.newHashSet(ff.path("terms").fieldNames()))
                    .contains("open_return_date", "baggage", "refundable_no_show", "disclosure_url",
                            "changing_carriage", "refundable", "carry_on", "changing_carriage_no_show");
        }

        // seatsLeft example
        assertThat(legs.at("/0/flights/0/seatsLeft").intValue()).isEqualTo(5);
        assertThat(legs.at("/0/flights/1/seatsLeft").isNull()).isTrue();

        List<JsonNode> fls2 = arrayValues(legs.get(1).path("flights"));
        assertThat(fls2.stream().map(f -> f.path("id").textValue()).collect(toList()))
                .isEqualTo(asList("SEG_LEDUFA_3", "SEG_UFASVO_4"));

        // Offer check
        JsonNode offer = variant.path("variantPriceInfo");
        String mainOfferId = offer.path("id").textValue();
        assertThat(mainOfferId).contains("3ADT.2CHD.1INF", "SVO", "KUF", "LED", "UFA", "NFMR");
        assertThat(offer.path("total").path("value").intValue()).isEqualTo(151349);
        List<JsonNode> catOffers = arrayValues(offer.path("categoryPrices"));
        assertThat(catOffers.stream().map(co -> co.path("passengerCategory").textValue()).collect(toList()))
                .isEqualTo(asList("infant", "adult", "child"));
        assertThat(catOffers.stream().map(co -> co.path("quantity").intValue()).collect(toList()))
                .isEqualTo(asList(1, 3, 2));
        assertThat(catOffers.stream().map(co -> co.path("total").path("value").intValue()).collect(toList()))
                .isEqualTo(asList(0, 32733, 26575));
        List<JsonNode> adtFares = arrayValues(catOffers.get(1).path("fareInfo"));
        assertThat(adtFares.stream().map(fi -> fi.path("fareBasis").textValue()).collect(toList()))
                .isEqualTo(asList("NFMR/IN00", "NFMR/IN00", "NFMR/IN00", "NFMR/IN00"));
        assertThat(adtFares.stream().map(fi -> fi.path("flightId").textValue()).collect(toList()))
                .isEqualTo(asList("SEG_SVOKUF_1", "SEG_KUFLED_2", "SEG_LEDUFA_3", "SEG_UFASVO_4"));

        List<JsonNode> allVariants = arrayValues(data.path("all_variants")).stream()
                .map(n -> n.path("variantPriceInfo")).collect(toList());
        assertThat(allVariants.size()).isEqualTo(3);
        assertThat(allVariants.stream().map(t -> t.path("id").textValue()).collect(toList()))
                .contains(mainOfferId);
        assertThat(allVariants.stream().map(fi -> fi.path("total").path("value").intValue()).collect(toList()))
                .isEqualTo(asList(99609, 115359, 151349));

        // different tariff ids
        List<String> allVariantIds = arrayValues(data.path("all_variants")).stream()
                .map(n -> n.path("id").textValue()).collect(toList());
        assertThat(allVariantIds.get(0)).isEqualTo(availabilityCheck.getId() +
                "!1ADT-SVO.202104061450.IGT.SU.1078.R.RCORISLB");
        assertThat(allVariantIds.get(1)).startsWith(availabilityCheck.getId() +
                "!3ADT.2CHD.1INF-SVO.201812241740.110.180.240.KUF.SU.1214.N.NCLR");
        assertThat(allVariantIds.get(2)).startsWith(availabilityCheck.getId() +
                "!3ADT.2CHD.1INF-SVO.201812241740.110.180.240.KUF.SU.1214.N.NFMR");

        // a full response data sample is available here: https://paste.yandex-team.ru/618348
    }

    @Test
    @Transactional
    public void testCheckAvailability_promoCampaigns() throws Exception {
        AvailabilityCheckResponse response = objectMapper.readValue(TestResources.readResource(
                "avia/aeroflot/aeroflot_check_availability_result.json"),
                AvailabilityCheckResponse.class);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testGetAeroflotVariantFormat");
        when(geobaseCountryService.getIsoName(any())).thenAnswer(call -> "RU");
        JsonNode ticketDaemonInfo = objectMapper.readTree(TestResources.readResource("avia/ticket_daemon/booking_info_by_token_sample.json"));

        VariantCheckResponseDTO result = variantService.checkAvailability(ticketDaemonInfo).join();
        AviaVariantAvailabilityCheck availabilityCheck = variantAvailabilityCheckRepository.getOne(result.getToken().getAvailabilityCheckId());
        JsonNode data = availabilityCheck.getData();

        VariantDTO variant = variantService.parseVariant(data);
        assertThat(variant.getId()).startsWith(availabilityCheck.getId() + "!3ADT.2CHD.1INF-");
        assertThat(variant.getVariantPriceInfo().getTotal()).isEqualTo(Money.of(151349, "RUB"));
        assertThat(variant.getPromoCampaigns()).isNotNull();

        List<VariantDTO> allVariants = variantService.parseAllVariants(data);
        VariantDTO plusVariant = allVariants.get(0);
        assertThat(plusVariant.getVariantPriceInfo().getId()).isEqualTo("1ADT-SVO.202104061450.IGT.SU.1078.R.RCORISLB");
        assertThat(plusVariant.getVariantPriceInfo().getTotal()).isEqualTo(Money.of(99609, "RUB"));
        AeroflotPlusPromo2021DTO plusPromo = plusVariant.getPromoCampaigns().getPlusPromo2021();
        assertThat(plusPromo.getEnabled()).isTrue();
        assertThat(plusPromo.getTotalPlusPoints()).isEqualTo(3_000);
    }

    @Test
    public void testCheckWithUnsupportedCountry() throws Exception {
        AvailabilityCheckResponse response = objectMapper.readValue(TestResources.readResource(
                "avia/aeroflot/aeroflot_check_availability_result.json"),
                AvailabilityCheckResponse.class);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testCheckWithUnsupportedCountry");
        when(geobaseCountryService.getIsoName(any())).thenAnswer(call -> "RU");
        JsonNode ticketDaemonInfo = objectMapper.readTree(TestResources.readResource("avia/ticket_daemon/booking_info_by_token_sample.json"));
        ((ObjectNode) ticketDaemonInfo.at("/redirect_data/order_data/booking_info")).put("CountryCode", "FR");

        assertThatThrownBy(() -> variantService.checkAvailability(ticketDaemonInfo).join())
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only RU is a supported country of sale at the moment");
    }

    @Test
    public void testCheckWithTooCloseDeparture() throws Exception {
        AvailabilityCheckResponse response = objectMapper.readValue(TestResources.readResource(
                "avia/aeroflot/aeroflot_check_availability_result.json"),
                AvailabilityCheckResponse.class);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testCheckWithTooCloseDeparture");
        when(gateway.checkAvailabilityAll(any())).thenThrow(new DepartureIsTooCloseException("test: less than 6h"));
        JsonNode ticketDaemonInfo = objectMapper.readTree(TestResources.readResource("avia/ticket_daemon/booking_info_by_token_sample.json"));

        assertThatThrownBy(() -> variantService.checkAvailability(ticketDaemonInfo).join())
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(AviaVariantNotSupportedException.class)
                .hasMessageContaining("External redirect is expected");
    }

    private List<JsonNode> arrayValues(JsonNode node) {
        ArrayNode array = (ArrayNode) node;
        List<JsonNode> nodes = new ArrayList<>();
        for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
            nodes.add(it.next());
        }
        return nodes;
    }
}

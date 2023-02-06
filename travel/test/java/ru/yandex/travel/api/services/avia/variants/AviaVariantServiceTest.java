package ru.yandex.travel.api.services.avia.variants;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.avia.booking.partners.gateways.BookingGateway;
import ru.yandex.avia.booking.partners.gateways.BookingTestingScenarios;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckResponse;
import ru.yandex.avia.booking.partners.gateways.model.availability.VariantNotAvailableException;
import ru.yandex.avia.booking.partners.gateways.model.search.PriceInfo;
import ru.yandex.avia.booking.partners.gateways.model.search.Variant;
import ru.yandex.avia.booking.service.dto.VariantCheckResponseDTO;
import ru.yandex.avia.booking.services.tdapi.AviaTicketDaemonApiClient;
import ru.yandex.travel.api.exceptions.UnsupportedCurrencyException;
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.api.services.avia.AviaBookingMeters;
import ru.yandex.travel.api.services.avia.AviaBookingProviderResolver;
import ru.yandex.travel.api.services.avia.fares.AviaFareFamilyService;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.avia.references.AviaReferenceJsonFactory;
import ru.yandex.travel.api.services.avia.td.AviaTdInfoExtractor;
import ru.yandex.travel.api.services.avia.variants.AviaBookingProperties.VariantCache;
import ru.yandex.travel.api.services.avia.variants.model.AviaAvailabilityCheckState;
import ru.yandex.travel.api.services.avia.variants.model.AviaVariantAvailabilityCheck;
import ru.yandex.travel.api.services.avia.variants.repositories.AviaCachedVariantRepository;
import ru.yandex.travel.api.services.avia.variants.repositories.AviaVariantRepository;
import ru.yandex.travel.api.services.dictionaries.avia.AviaAirlineDictionary;
import ru.yandex.travel.dicts.avia.TAirline;
import ru.yandex.travel.testing.misc.TestResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "avia-booking.enabled=true",
        "avia-booking.enable-testing-scenarios=true"
})
@ActiveProfiles("test")
public class AviaVariantServiceTest {

    @Autowired
    Environment environment;

    @Autowired
    AviaVariantService variantService;

    @MockBean
    AviaBookingProviderResolver resolver;

    @Autowired
    AviaVariantRepository variantAvailabilityCheckRepository;

    @MockBean
    AviaVariantDTOFactory variantDTOFactory;

    @MockBean
    AviaReferenceJsonFactory referenceJsonFactory;

    @MockBean
    AviaFareFamilyService fareFamilyService;

    @MockBean
    AviaAirlineDictionary airlineDictionary;

    @MockBean
    AviaTicketDaemonApiClient aviaTicketDaemonApiClient;

    @MockBean
    AviaGeobaseCountryService geobaseCountryService;

    BookingGateway gateway;

    static ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void init() {
        // common infrastructure mocks
        when(airlineDictionary.getById(any())).thenReturn(TAirline.newBuilder().build());
        when(airlineDictionary.getByIataCode(any())).thenReturn(TAirline.newBuilder().build());

        gateway = Mockito.mock(BookingGateway.class);
        when(resolver.gatewayForPartner(any(), any())).thenReturn(gateway);
    }

    @Test
    @Transactional
    public void testVariantsFound() {
        Money availabilityCheckPrice = Money.of(1000, "RUB");
        AvailabilityCheckResponse response = createResponse(availabilityCheckPrice);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testVariantsFound");
        JsonNode ticketDaemonInfo = createJsonNode();

        VariantCheckResponseDTO result = variantService.checkAvailability(ticketDaemonInfo).join();
        AviaVariantAvailabilityCheck value = variantAvailabilityCheckRepository.getOne(result.getToken().getAvailabilityCheckId());
        Assertions.assertThat(value.getState()).isEqualTo(AviaAvailabilityCheckState.SUCCESS);
        JsonNode priceInfo = value.getData().get("price_info");
        assertThat(priceInfo).isNotNull();
        Assertions.assertThat(value.getData().get("variant_info")).isNotNull();
        assertThat(result.getRedirectUrl()).isNotBlank();
    }

    @Test
    @Transactional
    public void testVariantsNotFound() {
        when(gateway.checkAvailabilityAll(any())).thenThrow(new VariantNotAvailableException("no such offer"));
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testVariantsNotFound");
        JsonNode ticketDaemonInfo = createJsonNode();

        assertThatThrownBy(() -> variantService.checkAvailability(ticketDaemonInfo).join())
                .isExactlyInstanceOf(CompletionException.class)
                .hasMessageContaining("Not available")
                .hasCauseExactlyInstanceOf(AviaVariantsNotFoundException.class)
                .satisfies(ce -> {
                    AviaVariantsNotFoundException e = (AviaVariantsNotFoundException) ce.getCause();
                    AviaVariantAvailabilityCheck value = variantAvailabilityCheckRepository.getOne(e.getAvailabilityCheckId());
                    Assertions.assertThat(value.getState()).isEqualTo(AviaAvailabilityCheckState.ERROR);
                });
    }

    @Test
    public void testTestingScenariosNotFound() {
        Money availabilityCheckPrice = Money.of(1000, "RUB");
        AvailabilityCheckResponse response = createResponse(availabilityCheckPrice);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testTestingScenariosNotFound");
        JsonNode ticketDaemonInfo = createJsonNode();
        addUtmSource(ticketDaemonInfo, BookingTestingScenarios.AVAILABILITY_CHECK_NOT_FOUND);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> variantService.checkAvailability(ticketDaemonInfo).join())
                .withCauseExactlyInstanceOf(AviaVariantsNotFoundException.class)
                .withMessageContaining("Not available");
    }

    @Test
    @Transactional
    public void testTestingScenariosNewPrice() {
        Money availabilityCheckPrice = Money.of(1000, "RUB");
        AvailabilityCheckResponse response = createResponse(availabilityCheckPrice);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testTestingScenariosNewPrice");
        JsonNode ticketDaemonInfo = createJsonNode();
        addUtmSource(ticketDaemonInfo, BookingTestingScenarios.AVAILABILITY_CHECK_NEW_PRICE);

        VariantCheckResponseDTO result = variantService.checkAvailability(ticketDaemonInfo).join();
        AviaVariantAvailabilityCheck value = variantAvailabilityCheckRepository.getOne(result.getToken().getAvailabilityCheckId());

        Assertions.assertThat(value.getState()).isEqualTo(AviaAvailabilityCheckState.SUCCESS);
        JsonNode preliminaryPrice = value.getData().get("price_info").get("preliminary_price");
        JsonNode checkPrice = value.getData().get("price_info").get("first_check_price");
        assertThat(preliminaryPrice.get("value").asInt()).isEqualTo(950);
        assertThat(checkPrice.get("value").asInt()).isEqualTo(1000);
    }

    @Test
    public void testTestingScenariosUnhandledError() {
        Money availabilityCheckPrice = Money.of(1000, "RUB");
        AvailabilityCheckResponse response = createResponse(availabilityCheckPrice);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testTestingScenariosUnhandledError");
        JsonNode ticketDaemonInfo = createJsonNode();
        addUtmSource(ticketDaemonInfo, BookingTestingScenarios.AVAILABILITY_CHECK_UNHANDLED_ERROR);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> variantService.checkAvailability(ticketDaemonInfo).join())
                .withMessageContaining("testing scenario: UNHANDLED ERROR")
                .withCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void testTestingScenariosDisabled() {
        AviaBookingProperties properties = AviaBookingProperties.builder()
                .enabled(true)
                .enableTestingScenarios(false)
                .variantCache(VariantCache.builder()
                        .lockAttempts(1)
                        .checkTtl(Duration.ZERO)
                        .maxConcurrency(1)
                        .shutdownTimeout(Duration.ofSeconds(5))
                        .build())
                .build();
        AviaVariantService variantService = new AviaVariantService(
                Mockito.mock(AviaVariantRepository.class),
                new AviaTdInfoExtractor(Mockito.mock(ApiTokenEncrypter.class)),
                resolver,
                Mockito.mock(AviaVariantInfoJsonFactory.class),
                Mockito.mock(PlatformTransactionManager.class),
                new AviaVariantCacheService(properties, Mockito.mock(AviaCachedVariantRepository.class)),
                Mockito.mock(AviaFareFamilyService.class),
                new AviaBookingMeters(),
                aviaTicketDaemonApiClient,
                airlineDictionary,
                properties,
                environment);
        variantService.init();

        Money availabilityCheckPrice = Money.of(1000, "RUB");
        AvailabilityCheckResponse response = createResponse(availabilityCheckPrice);
        when(gateway.checkAvailabilityAll(any())).thenReturn(response);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testTestingScenariosDisabled");
        JsonNode ticketDaemonInfo = createJsonNode();
        addUtmSource(ticketDaemonInfo, BookingTestingScenarios.AVAILABILITY_CHECK_UNHANDLED_ERROR);
        variantService.checkAvailability(ticketDaemonInfo).join();
    }

    @Test
    @Transactional
    public void testVariantRefresh() {
        AvailabilityCheckResponse rsp1 = createResponse(Money.of(1000, "RUB"));
        AvailabilityCheckResponse rsp2 = createResponse(Money.of(1100, "RUB"));
        when(gateway.checkAvailabilityAll(any())).thenReturn(rsp1, rsp2);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testVariantRefresh");

        VariantCheckResponseDTO result = variantService.checkAvailability(createJsonNode()).join();
        AviaVariantAvailabilityCheck check1 = variantAvailabilityCheckRepository.getOne(result.getToken().getAvailabilityCheckId());
        Assertions.assertThat(check1.getData().at("/price_info/first_check_price/value").asInt()).isEqualTo(1000);

        JsonNode refreshed = variantService.reCheckAvailability(check1.getId()).join();
        assertThat(refreshed.at("/price_info/first_check_price/value").asInt()).isEqualTo(1100);
        AviaVariantAvailabilityCheck stored = variantAvailabilityCheckRepository.getOne(check1.getId());
        Assertions.assertThat(stored.getData().at("/price_info/first_check_price/value").asInt()).isEqualTo(1100);
    }

    @Test
    public void testUnsupportedCurrency() {
        AvailabilityCheckResponse rsp1 = createResponse(Money.of(1000, "USD"));
        when(gateway.checkAvailabilityAll(any())).thenReturn(rsp1);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testUnsupportedCurrency");

        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> variantService.checkAvailability(createJsonNode()).join())
                .withMessageContaining("Unsupported currency")
                .withCauseExactlyInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    @Transactional
    public void testAlternativeOffersWithUnsupportedCurrency() {
        AvailabilityCheckResponse rsp1 = createResponse(
                Money.of(1000, "RUB"),
                Money.of(1000, "USD"),
                Money.of(1000, "EUR")
        );
        when(gateway.checkAvailabilityAll(any())).thenReturn(rsp1);
        when(gateway.getExternalVariantId(any())).thenReturn("some_variant_id_testAlternativeOffersWithUnsupportedCurrency");

        VariantCheckResponseDTO result = variantService.checkAvailability(createJsonNode()).join();
        AviaVariantAvailabilityCheck check = variantAvailabilityCheckRepository.getOne(result.getToken().getAvailabilityCheckId());

        Assertions.assertThat(check.getData().get("all_variants").size()).isEqualTo(1);
        JsonNode price = check.getData().at("/price_info/first_check_price");
        assertThat(price.get("value").intValue()).isEqualTo(1000);
        assertThat(price.get("currency").textValue()).isEqualTo("RUB");
    }

    private void addUtmSource(JsonNode tdData, String value) {
        ((ObjectNode) tdData.get("additional_data")).put("utm_source", value);
    }

    private AvailabilityCheckResponse createResponse(Money... offerPrices) {
        return createResponse("RU", offerPrices);
    }

    @SuppressWarnings("SameParameterValue")
    private AvailabilityCheckResponse createResponse(String countryOfSale, Money... offerPrices) {
        List<PriceInfo> offers = Stream.of(offerPrices)
                .map(p -> PriceInfo.builder().id("offer_" + p).total(p).build())
                .collect(Collectors.toList());
        Variant variant = Variant.builder()
                .priceInfo(offers.get(0))
                .allTariffs(offers)
                .countryOfSale(countryOfSale)
                .build();
        return AvailabilityCheckResponse.builder()
                .variant(variant)
                .build();
    }

    static JsonNode createJsonNode() {
        try {
            return objectMapper.readTree(TestResources.readResource("avia/ticket_daemon/booking_info_by_token_sample.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

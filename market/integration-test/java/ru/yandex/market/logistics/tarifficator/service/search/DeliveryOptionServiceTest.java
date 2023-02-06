package ru.yandex.market.logistics.tarifficator.service.search;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.dto.DeliveryOptionDto;
import ru.yandex.market.logistics.tarifficator.model.dto.DeliveryOptionsSearchRequestDto;
import ru.yandex.market.logistics.tarifficator.model.dto.ServiceCostDto;
import ru.yandex.market.logistics.tarifficator.model.enums.PricingType;
import ru.yandex.market.logistics.tarifficator.model.enums.ServiceType;

import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;

@DisplayName("Интеграционный тест сервиса DeliveryOptionService")
class DeliveryOptionServiceTest extends AbstractContextualTest {

    @Autowired
    private DeliveryOptionService deliveryOptionService;

    @BeforeEach
    public void setup() {
        clock.setFixed(Instant.parse("2019-08-12T11:00:00.00Z"), ZoneOffset.UTC);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource({"requestProvider", "requestWithOnlyRequiredFieldsProvider"})
    @DisplayName("Получение опций доставки")
    @DatabaseSetup("/service/search/db/before/data.xml")
    void getTariffs(
        @SuppressWarnings("unused") String caseName,
        Consumer<DeliveryOptionsSearchRequestDto> requestAdjuster,
        List<DeliveryOptionDto> expectedOptions
    ) {
        DeliveryOptionsSearchRequestDto request = createRequest();
        requestAdjuster.accept(request);
        softly.assertThat(deliveryOptionService.findDeliveryOptions(request))
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(expectedOptions);
    }

    private DeliveryOptionsSearchRequestDto createRequest() {
        return DeliveryOptionsSearchRequestDto.builder()
            .tariffIds(Set.of(1L, 100L, 200L, 300L))
            .locationFrom(213)
            .locationTo(197)
            .date(LocalDateTime.of(2019, 8, 22, 11, 0, 0).toInstant(ZoneOffset.UTC))
            .weight(new BigDecimal("19"))
            .length(50)
            .width(30)
            .height(10)
            .isPublic(false)
            .build();
    }

    private static Stream<Arguments> requestProvider() {
        return Stream.<Triple<String, Consumer<DeliveryOptionsSearchRequestDto>, List<DeliveryOptionDto>>>of(
            Triple.of(
                "Запрос непубличных опций за 2019-08-22 по всем тарифам",
                request -> {
                },
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(1)
                        .minDays(4)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("180.00"))
                        .servicesCost(Set.of(
                            serviceInsurance("0.007000", "0.00", "1500.00"),
                            serviceReturn("0.800000", "0.00", "999999.00"),
                            serviceCash("0.022000", "31.00", "6600.00")
                        ))
                        .build(),
                    DeliveryOptionDto.builder()
                        .tariffId(100)
                        .minDays(5)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("8.00"))
                        .servicesCost(Set.of(
                            serviceInsurance("0.005000", "0.00", "1500.00"),
                            serviceReturn("0.750000", "0.00", "999999.00"),
                            serviceCash("0.022000", "30.00", "6600.00")
                        ))
                        .build(),
                    DeliveryOptionDto.builder()
                        .tariffId(200)
                        .minDays(5)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("7.00"))
                        .build()
                )
            ),
            Triple.of(
                "Запрос непубличных опций за 2019-08-22 по одному тарифу",
                request -> request.setTariffIds(Set.of(1L)),
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(1)
                        .minDays(4)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("180.00"))
                        .servicesCost(Set.of(
                            serviceInsurance("0.007000", "0.00", "1500.00"),
                            serviceReturn("0.800000", "0.00", "999999.00"),
                            serviceCash("0.022000", "31.00", "6600.00")
                        ))
                        .build()
                )
            ),
            Triple.of(
                "Запрос непубличных опций за 2019-08-22 Питер - Москва",
                request -> {
                    request.setLocationFrom(2);
                    request.setLocationTo(213);
                },
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(200)
                        .minDays(1)
                        .maxDays(2)
                        .deliveryCost(new BigDecimal("3.00"))
                        .build()
                )
            ),
            Triple.of(
                "Запрос опций за дату, на которую нет активных прайсов",
                request ->
                    request.setDate(LocalDateTime.of(2017, 8, 22, 11, 0, 0).toInstant(ZoneOffset.UTC)),
                List.of()
            ),
            Triple.of(
                "Запрос опций со слишком большим весом",
                request -> request.setWeight(new BigDecimal("501")),
                List.of()
            ),
            Triple.of(
                "Запрос опций со слишком большой длиной",
                request -> request.setLength(251),
                List.of()
            ),
            Triple.of(
                "Запрос опций со слишком большой шириной",
                request -> request.setWidth(201),
                List.of()
            ),
            Triple.of(
                "Запрос опций со слишком большой высотой",
                request -> request.setHeight(201),
                List.of()
            ),
            Triple.of(
                "Запрос опций по тарифам, по которым нет данных",
                request -> request.setTariffIds(Set.of(999L)),
                List.of()
            ),
            Triple.of(
                "Запрос опций по пустому списку тарифов",
                request -> request.setTariffIds(Set.of()),
                List.of()
            ),
            Triple.of(
                "Запрос опций для больших габаритов",
                request -> {
                    request.setLength(215);
                    request.setHeight(215);
                    request.setWidth(215);
                    request.setIsPublic(true);
                    request.setWeight(new BigDecimal("1000"));
                    request.setDate(LocalDateTime.of(2020, 9, 22, 11, 0, 0).toInstant(ZoneOffset.UTC));
                },
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(100)
                        .minDays(5)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("8.00"))
                        .build()
                )
            )
        )
            .map(triple -> Arguments.of(triple.first, triple.second, triple.third));
    }

    private static ServiceCostDto serviceInsurance(String price, String min, String max) {
        return ServiceCostDto.builder()
            .code(ServiceType.INSURANCE)
            .pricingType(PricingType.PERCENT_COST)
            .priceValue(new BigDecimal(price))
            .minCost(new BigDecimal(min))
            .maxCost(new BigDecimal(max))
            .build();
    }

    private static Stream<Arguments> requestWithOnlyRequiredFieldsProvider() {
        return Stream.of(
            Triple.of(
                "Запрос публичных опций за 2019-08-22 по всем тарифам",
                (Consumer<DeliveryOptionsSearchRequestDto>) request -> {
                    request.setIsPublic(true);
                    request.setTariffIds(null);
                },
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(1)
                        .minDays(4)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("178.00"))
                        .servicesCost(Set.of(
                            serviceInsurance("0.005000", "0.00", "1500.00"),
                            serviceReturn("0.750000", "0.00", "999999.00"),
                            serviceCash("0.022000", "30.00", "6600.00")
                        ))
                        .build(),
                    DeliveryOptionDto.builder()
                        .tariffId(100)
                        .minDays(5)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("10.00"))
                        .servicesCost(Set.of(
                            serviceCash("0.022000", "30.00", "6600.00")
                        ))
                        .build(),
                    DeliveryOptionDto.builder()
                        .tariffId(200)
                        .minDays(5)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("10.00"))
                        .build()
                )
            ),
            Triple.of(
                "Запрос непубличных опций за сегодня (2019-08-22) по одному тарифу",
                (Consumer<DeliveryOptionsSearchRequestDto>) request -> {
                    request.setTariffIds(Set.of(1L));
                    request.setDate(null);
                },
                List.of(
                    DeliveryOptionDto.builder()
                        .tariffId(1)
                        .minDays(4)
                        .maxDays(7)
                        .deliveryCost(new BigDecimal("180.00"))
                        .servicesCost(Set.of(
                            serviceInsurance("0.007000", "0.00", "1500.00"),
                            serviceReturn("0.800000", "0.00", "999999.00"),
                            serviceCash("0.022000", "31.00", "6600.00")
                        ))
                        .build()
                )
            )
        )
            .map(triple -> Arguments.of(triple.first, triple.second, triple.third));
    }

    private static ServiceCostDto serviceReturn(String price, String min, String max) {
        return ServiceCostDto.builder()
            .code(ServiceType.RETURN)
            .pricingType(PricingType.PERCENT_DELIVERY)
            .priceValue(new BigDecimal(price))
            .minCost(new BigDecimal(min))
            .maxCost(new BigDecimal(max))
            .build();
    }

    private static ServiceCostDto serviceCash(String price, String min, String max) {
        return ServiceCostDto.builder()
            .code(ServiceType.CASH_SERVICE)
            .pricingType(PricingType.PERCENT_CASH)
            .priceValue(new BigDecimal(price))
            .minCost(new BigDecimal(min))
            .maxCost(new BigDecimal(max))
            .build();
    }

}

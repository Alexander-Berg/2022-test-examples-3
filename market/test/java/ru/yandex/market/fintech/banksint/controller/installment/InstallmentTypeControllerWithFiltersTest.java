package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.SupplierType;
import ru.yandex.market.fintech.instalment.model.InstallmentAvailabilityDto;
import ru.yandex.market.fintech.instalment.model.RequestInstallmentTypeWithFiltersDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.fintech.banksint.controller.installment.InstallmentUtils.createInstallmentAvailabilityDto;
import static ru.yandex.market.fintech.banksint.controller.installment.InstallmentUtils.createInstallmentDto;

class InstallmentTypeControllerWithFiltersTest extends FunctionalTest {
    private static final String INSTALLMENT_TYPE_REQUEST_URL = "/supplier/fin-service/installments?shop_id={shopId}";
    private static final RecursiveComparisonConfiguration COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreCollectionOrder(true)
                    .build();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentType.sql"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    void installmentTypeGetTest(
            String name,
            String url,
            Map<String, ?> queryParams,
            RequestInstallmentTypeWithFiltersDto request,
            Consumer<ResponseEntity<List<InstallmentAvailabilityDto>>> responseConsumer
    ) {

        HttpEntity<RequestInstallmentTypeWithFiltersDto> httpEntity = new HttpEntity<>(request);
        responseConsumer.accept(
                testRestTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        httpEntity,
                        new ParameterizedTypeReference<List<InstallmentAvailabilityDto>>() {
                        },
                        queryParams));
    }


    static Arguments createArguments(
            String name,
            String url,
            Map<String, ?> queryParams,
            RequestInstallmentTypeWithFiltersDto request,
            Consumer<ResponseEntity<List<InstallmentAvailabilityDto>>> responseConsumer
    ) {
        return Arguments.of(
                name,
                url,
                queryParams,
                request,
                responseConsumer
        );
    }

    @SuppressWarnings("MethodLength")
    static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                createArguments(
                        "Запрос c пустыми фильтрами",
                        INSTALLMENT_TYPE_REQUEST_URL,
                        Map.of("shopId", "123456"),
                        createRequest(emptyList(), emptyList()),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                            assertThat(response.getBody())
                                    .usingRecursiveFieldByFieldElementComparator(COMPARISON_CONFIGURATION)
                                    .containsExactly(
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "1.5 месяца", "Не на все категории", 45, "MONTH_AND_HALF",
                                                            3.0f,
                                                            100,
                                                            200, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "6 месяцев", null, 180, "HALF_YEAR",
                                                            5.0f,
                                                            1000,
                                                            2000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto("24 месяца", null, 720, "CATEGORY_TEST",
                                                            20.0f,
                                                            10000,
                                                            20000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto("24 месяца", null, 721, "BRAND_TEST",
                                                            20.0f,
                                                            100000,
                                                            200000, SupplierType.PARTNER)
                                            )
                                    );
                        }
                ),
                createArguments(
                        "Запрос c фильтрами по категории",
                        INSTALLMENT_TYPE_REQUEST_URL,
                        Map.of("shopId", "123456"),
                        createRequest(List.of(90945L), emptyList()),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                            assertThat(response.getBody())
                                    .usingRecursiveFieldByFieldElementComparator(COMPARISON_CONFIGURATION)
                                    .containsExactly(
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "1.5 месяца", "Не на все категории", 45, "MONTH_AND_HALF",
                                                            3.0f,
                                                            100,
                                                            200, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "6 месяцев", null, 180, "HALF_YEAR",
                                                            5.0f,
                                                            1000,
                                                            2000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto("24 месяца", null, 720, "CATEGORY_TEST",
                                                            20.0f,
                                                            10000,
                                                            20000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    false,
                                                    List.of("UNSUPPORTED_CATEGORY_ID"),
                                                    createInstallmentDto("24 месяца", null, 721, "BRAND_TEST",
                                                            20.0f,
                                                            100000,
                                                            200000, SupplierType.PARTNER)
                                            )
                                    );
                        }
                ),
                createArguments(
                        "Запрос c фильтрами по брендам",
                        INSTALLMENT_TYPE_REQUEST_URL,
                        Map.of("shopId", "123456"),
                        createRequest(emptyList(), List.of(142L)),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                            assertThat(response.getBody())
                                    .usingRecursiveFieldByFieldElementComparator(COMPARISON_CONFIGURATION)
                                    .containsExactly(
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "1.5 месяца", "Не на все категории", 45, "MONTH_AND_HALF",
                                                            3.0f,
                                                            100,
                                                            200, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "6 месяцев", null, 180, "HALF_YEAR",
                                                            5.0f,
                                                            1000,
                                                            2000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    false,
                                                    List.of("UNSUPPORTED_BRAND_ID"),
                                                    createInstallmentDto("24 месяца", null, 720, "CATEGORY_TEST",
                                                            20.0f,
                                                            10000,
                                                            20000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto("24 месяца", null, 721, "BRAND_TEST",
                                                            20.0f,
                                                            100000,
                                                            200000, SupplierType.PARTNER)
                                            ));
                        }
                ),
                createArguments(
                        "Запрос c фильтрами по брендам и категориям",
                        INSTALLMENT_TYPE_REQUEST_URL,
                        Map.of("shopId", "123456"),
                        createRequest(List.of(90867L), List.of(142L)),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                            assertThat(response.getBody())
                                    .usingRecursiveFieldByFieldElementComparator(COMPARISON_CONFIGURATION)
                                    .containsExactly(createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "1.5 месяца", "Не на все категории", 45, "MONTH_AND_HALF",
                                                            3.0f,
                                                            100,
                                                            200, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "6 месяцев", null, 180, "HALF_YEAR",
                                                            5.0f,
                                                            1000,
                                                            2000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    false,
                                                    List.of("UNSUPPORTED_BRAND_ID", "UNSUPPORTED_CATEGORY_ID"),
                                                    createInstallmentDto("24 месяца", null, 720, "CATEGORY_TEST",
                                                            20.0f,
                                                            10000,
                                                            20000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto("24 месяца", null, 721, "BRAND_TEST",
                                                            20.0f,
                                                            100000,
                                                            200000, SupplierType.PARTNER)
                                            )
                                    );
                        }
                ),
                createArguments(
                        "Запрос c фильтрами по брендам и категориям (только рассрочки с пустыми ограничениями)",
                        INSTALLMENT_TYPE_REQUEST_URL,
                        Map.of("shopId", "123456"),
                        createRequest(List.of(9999L), List.of(9999L)),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                            assertThat(response.getBody())
                                    .usingRecursiveFieldByFieldElementComparator(COMPARISON_CONFIGURATION)
                                    .containsExactly(
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "1.5 месяца", "Не на все категории", 45, "MONTH_AND_HALF",
                                                            3.0f,
                                                            100,
                                                            200, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    true,
                                                    emptyList(),
                                                    createInstallmentDto(
                                                            "6 месяцев", null, 180, "HALF_YEAR",
                                                            5.0f,
                                                            1000,
                                                            2000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    false,
                                                    List.of("UNSUPPORTED_BRAND_ID", "UNKNOWN_CATEGORY_ID"),
                                                    createInstallmentDto("24 месяца", null, 720, "CATEGORY_TEST",
                                                            20.0f,
                                                            10000,
                                                            20000, SupplierType.PARTNER)
                                            ),
                                            createInstallmentAvailabilityDto(
                                                    false,
                                                    List.of("UNSUPPORTED_BRAND_ID", "UNKNOWN_CATEGORY_ID"),
                                                    createInstallmentDto("24 месяца", null, 721, "BRAND_TEST",
                                                            20.0f,
                                                            100000,
                                                            200000, SupplierType.PARTNER)
                                            )
                                    );
                        }
                )
        );
    }

    private static RequestInstallmentTypeWithFiltersDto createRequest(
            List<Long> categoryIds,
            List<Long> brandIds
    ) {
        var request = new RequestInstallmentTypeWithFiltersDto();
        request.setBrandIds(brandIds);
        request.setCategoryIds(categoryIds);
        return request;
    }

}

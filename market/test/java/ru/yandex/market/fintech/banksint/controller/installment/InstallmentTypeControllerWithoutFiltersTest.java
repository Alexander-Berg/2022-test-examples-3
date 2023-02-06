package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.SupplierType;
import ru.yandex.market.fintech.instalment.model.InstallmentAvailabilityDto;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.fintech.banksint.controller.installment.InstallmentUtils.createInstallmentAvailabilityDto;
import static ru.yandex.market.fintech.banksint.controller.installment.InstallmentUtils.createInstallmentDto;

class InstallmentTypeControllerWithoutFiltersTest extends FunctionalTest {
    private static final String INSTALLMENT_TYPE_REQUEST_URL = "/supplier/fin-service/installments?shop_id={shopId}";
    private static final String INVALID_INSTALLMENT_TYPE_REQUEST_URL = "/supplier/fin-service/installments";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentType.sql"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    void installmentTypeGetTest(
            String name,
            String url,
            Map<String, ?> queryParams,
            Consumer<ResponseEntity<List<InstallmentAvailabilityDto>>> responseConsumer
    ) {
        responseConsumer.accept(
                testRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<InstallmentAvailabilityDto>>() {
                        },
                        queryParams));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedErrorTestData")
    void installmentTypeGetErrorTest(
            String name,
            String url,
            Map<String, ?> queryParams,
            Consumer<ResponseEntity<String>> responseConsumer
    ) {
        responseConsumer.accept(
                testRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        String.class,
                        queryParams));
    }

    public static Arguments createArguments(
            String name,
            String url,
            Map<String, ?> queryParams,
            Consumer<ResponseEntity<List<InstallmentAvailabilityDto>>> responseConsumer
    ) {
        return Arguments.of(
                name,
                url,
                queryParams,
                responseConsumer
        );
    }

    public static Arguments createErrorArguments(
            String name,
            String url,
            Map<String, ?> queryParams,
            Consumer<ResponseEntity<String>> responseConsumer
    ) {
        return Arguments.of(
                name,
                url,
                queryParams,
                responseConsumer
        );
    }

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                createArguments(
                        "Запрос доступных рассрочек",
                        INSTALLMENT_TYPE_REQUEST_URL,
                        Map.of("shopId", "123456"),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                            assertThat(response.getBody()).isEqualTo(getExpectedResponse());
                        }
                )
        );
    }

    public static Stream<Arguments> parameterizedErrorTestData() {
        return Stream.of(
                createErrorArguments(
                        "Запрос c shopId = null",
                        INVALID_INSTALLMENT_TYPE_REQUEST_URL,
                        emptyMap(),
                        response -> {
                            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                            assertThat(response.getBody()).contains("Required Long parameter 'shop_id' is not present");
                        }
                )
        );
    }

    private static List<InstallmentAvailabilityDto> getExpectedResponse() {
        return List.of(
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
}



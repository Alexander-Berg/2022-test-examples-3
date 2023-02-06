package ru.yandex.market.fintech.banksint.controller.installment;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentCustomGroupMapper;
import ru.yandex.market.fintech.banksint.service.dtoconverter.CustomInstallmentGroupDtoEntityConverter;
import ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupDto.SourceEnum.DYNAMIC_GROUPS;
import static ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupDto.SourceEnum.FILE;

class InstallmentForGroupProductsControllerCreateTest extends FunctionalTest {
    private static final String REQUEST_URL = "/supplier/fin-service" +
            "/installments/group/custom?shop_id={shopId}";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstallmentCustomGroupMapper installmentCustomGroupMapper;

    @Autowired
    private CustomInstallmentGroupDtoEntityConverter dtoConverter;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("CreateInstallmentCustomGroup.sql"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    void createCustomInstallmentGroupDto(
            String name,
            String url,
            Map<String, ?> queryParams,
            CustomInstallmentGroupDto request
    ) {

        HttpEntity<CustomInstallmentGroupDto> httpEntity = new HttpEntity<>(request);
        var response = testRestTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<CustomInstallmentGroupDto>() {
                },
                queryParams);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var createdCustomGroupDto = response.getBody();
        assertThat(createdCustomGroupDto).isNotNull();
        assertThat(createdCustomGroupDto.getId()).isNotNull();
        if (request.getEnabled() == null) {
            // null must be converted into true
            assertThat(createdCustomGroupDto.getEnabled()).isTrue();
            request.setEnabled(true);
        }

        if (request.getSource() == FILE) {
            assertThat(createdCustomGroupDto)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "resourceInfo")
                    .isEqualTo(request);
        } else {
            assertThat(createdCustomGroupDto)
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(request);
        }

        long shopId = Long.parseLong(String.valueOf(queryParams.get("shopId")));
        var dbCreatedCustomGroup = installmentCustomGroupMapper
                .getCustomInstallmentGroupByIdAndShopId(createdCustomGroupDto.getId(), shopId);
        assertThat(createdCustomGroupDto).isEqualTo(dtoConverter.toDto(dbCreatedCustomGroup));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"parameterizedTestErrorData", "parameterizedTestErrorDataForFileSourceObjects"})
    void tryToCreateCustomInstallmentGroupDto(
            String name,
            String url,
            Map<String, ?> queryParams,
            CustomInstallmentGroupDto request,
            Consumer<ResponseEntity<String>> responseConsumer
    ) {

        HttpEntity<CustomInstallmentGroupDto> httpEntity = new HttpEntity<>(request);
        var response = testRestTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<String>() {
                },
                queryParams);
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        responseConsumer.accept(response);
    }

    public static Arguments createArguments(
            String name,
            String url,
            Map<String, ?> queryParams,
            Supplier<CustomInstallmentGroupDto> request
    ) {
        return Arguments.of(
                name,
                url,
                queryParams,
                request.get()
        );
    }

    public static Arguments createErrorArguments(
            String name,
            String url,
            Map<String, ?> queryParams,
            Supplier<CustomInstallmentGroupDto> request,
            Consumer<ResponseEntity<String>> responseConsumer
    ) {
        return Arguments.of(
                name,
                url,
                queryParams,
                request.get(),
                responseConsumer
        );
    }

    @SuppressWarnings("MethodLength")
    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                createArguments(
                        "Создание группы рассрочек с валидными параметрами",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        InstallmentForGroupProductsControllerCreateTest::createDefaultInstallmentGroup
                ),
                createArguments(
                        "Создание группы рассрочек с минимальным набором валидных параметров (brands)",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setEnabled(null);
                            request.setStartDateTime(null);
                            request.setEndDateTime(null);
                            request.setCategoryIds(Collections.emptyList());
                            return request;
                        }
                ),
                createArguments(
                        "Создание группы рассрочек с минимальным набором валидных параметров (categories)",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setEnabled(null);
                            request.setStartDateTime(null);
                            request.setEndDateTime(null);
                            request.setBrandIds(Collections.emptyList());
                            return request;
                        }
                ),
                createArguments(
                        "Создание группы рассрочек с ограничениями по бренду и категориям",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("BRAND_TEST"));
                            request.setCategoryIds(List.of(90867L));
                            request.setBrandIds(List.of(142L));
                            return request;
                        }
                ),
                createArguments(
                        "Создание группы рассрочек с валидными параметрами и источником FILE",
                        REQUEST_URL,
                        Map.of("shopId", "1"),
                        InstallmentForGroupProductsControllerCreateTest::createDefaultFileInstallmentGroup
                ),
                createArguments(
                        "Создание группы рассрочек с минимальным набором валидных параметров и источником FILE",
                        REQUEST_URL,
                        Map.of("shopId", "1"),
                        () -> {
                            var request = createDefaultFileInstallmentGroup();
                            request.setEnabled(null);
                            request.setStartDateTime(null);
                            request.setEndDateTime(null);
                            return request;
                        }
                )
        );
    }

    private static CustomInstallmentGroupDto createDefaultInstallmentGroup() {
        CustomInstallmentGroupDto dto = new CustomInstallmentGroupDto();
        dto.setName("Boring installment custom group");
        dto.setCategoryIds(List.of(1L));
        dto.setBrandIds(List.of(2L));
        dto.setInstallments(List.of("MONTH_AND_HALF"));
        dto.setEnabled(true);
        dto.setStartDateTime(Instant.now().atOffset(ZoneOffset.UTC));
        dto.setEndDateTime(Instant.now().plus(1, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC));
        dto.setSource(DYNAMIC_GROUPS);
        return dto;
    }

    private static CustomInstallmentGroupDto createDefaultFileInstallmentGroup() {
        CustomInstallmentGroupDto dto = new CustomInstallmentGroupDto();
        dto.setName("Boring installment custom group");
        dto.setCategoryIds(List.of());
        dto.setBrandIds(List.of());
        dto.setInstallments(List.of());
        dto.setEnabled(true);
        dto.setStartDateTime(Instant.now().atOffset(ZoneOffset.UTC));
        dto.setEndDateTime(Instant.now().plus(1, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC));
        dto.setSource(FILE);
        dto.setResourceId("de7cd822-d5f9-47cb-8fee-36546d7c9ab1");
        return dto;
    }

    public static Stream<Arguments> parameterizedTestErrorData() {
        return Stream.of(
                createErrorArguments(
                        "Создание группы рассрочек с пустым именем",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setName(null);
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("NotNull.customInstallmentGroupDto.name")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с пустым списком рассрочек",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(null);
                            return request;
                        },
                        response -> assertThat(response.getBody())
                                .contains("customInstallmentGroup.installments: is null or empty")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с пустым списком брендов и категорий",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setBrandIds(null);
                            request.setCategoryIds(null);
                            return request;
                        },
                        response -> {
                            assertThat(response.getBody()).contains("categoryIds: is null or empty");
                            assertThat(response.getBody()).contains("brandIds: is null or empty");
                        }
                ),
                createErrorArguments(
                        "Создание группы рассрочек с пустым source",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setSource(null);
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("NotNull.customInstallmentGroupDto.source")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с пустым shopId",
                        REQUEST_URL,
                        Map.of("shopId", ""),
                        InstallmentForGroupProductsControllerCreateTest::createDefaultInstallmentGroup,
                        response -> assertThat(response.getBody()).contains("shopId: must not be null")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с startDate>endDate",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setEndDateTime(Instant.now().atOffset(ZoneOffset.UTC));
                            request.setStartDateTime(Instant.now().plus(1, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC));
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("startDate: must be greater than or equal" +
                                " to " +
                                "endDate")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с невалидным installment",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("INVALID"));
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("Invalid installments: [INVALID]")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с невалидным installment(unsupported brands)",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("BRAND_TEST"));
                            request.setCategoryIds(List.of(90867L));
                            request.setBrandIds(List.of(99L));
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("Invalid installments: [BRAND_TEST]")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с невалидным installment(unsupported categoryIds)",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("BRAND_TEST"));
                            request.setCategoryIds(List.of(99L));
                            request.setBrandIds(List.of(142L));
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("Invalid installments: [BRAND_TEST]")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с невалидным отключенной installment",
                        REQUEST_URL,
                        Map.of("shopId", "123456"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("DISABLED_TEST"));
                            request.setCategoryIds(List.of(99L));
                            request.setBrandIds(List.of(142L));
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains("Invalid installments: [DISABLED_TEST]")
                )
        );
    }

    public static Stream<Arguments> parameterizedTestErrorDataForFileSourceObjects() {
        return Stream.of(
                createErrorArguments(
                        "Создание группы рассрочек с источником FILE и null resourceId",
                        REQUEST_URL,
                        Map.of("shopId", "1"),
                        () -> {
                            var request = createDefaultFileInstallmentGroup();
                            request.setResourceId(null);
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains(
                                "createCustomGroup.customInstallmentGroup.resourceId: must be non-null")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с источником FILE и неверным shopId",
                        REQUEST_URL,
                        Map.of("shopId", "1000"),
                        InstallmentForGroupProductsControllerCreateTest::createDefaultFileInstallmentGroup,
                        response -> assertThat(response.getBody()).contains(
                                "Invalid resourceId: 'de7cd822-d5f9-47cb-8fee-36546d7c9ab1' and/or shopId: '1000'")
                ),
                createErrorArguments(
                        "Создание группы рассрочек с источником FILE и неверным shopId",
                        REQUEST_URL,
                        Map.of("shopId", "1"),
                        () -> {
                            var request = createDefaultFileInstallmentGroup();
                            request.setResourceId("incorrect-resourceId");
                            return request;
                        },
                        response -> assertThat(response.getBody()).contains(
                                "Invalid resourceId: 'incorrect-resourceId' and/or shopId: '1'")
                )
        );
    }
}


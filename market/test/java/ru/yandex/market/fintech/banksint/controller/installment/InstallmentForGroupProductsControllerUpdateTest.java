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
import static ru.yandex.market.fintech.instalment.model.CustomInstallmentGroupDto.SourceEnum.FILE;

public class InstallmentForGroupProductsControllerUpdateTest extends FunctionalTest {
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
        jdbcTemplate.execute(readClasspathFile("ListInstallmentCustomGroup.sql"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    void updateCustomInstallmentGroupDto(
            String name,
            String url,
            Map<String, ?> queryParams,
            CustomInstallmentGroupDto request
    ) {

        HttpEntity<CustomInstallmentGroupDto> httpEntity = new HttpEntity<>(request);
        var response = testRestTemplate.exchange(
                url,
                HttpMethod.PUT,
                httpEntity,
                new ParameterizedTypeReference<CustomInstallmentGroupDto>() {
                },
                queryParams);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

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
                    .ignoringFields("resourceInfo")
                    .isEqualTo(request);
        } else {
            assertThat(createdCustomGroupDto).isEqualTo(request);
        }

        long shopId = Long.parseLong(String.valueOf(queryParams.get("shopId")));
        var dbCreatedCustomGroup = installmentCustomGroupMapper
                .getCustomInstallmentGroupByIdAndShopId(createdCustomGroupDto.getId(), shopId);
        assertThat(createdCustomGroupDto).isEqualTo(dtoConverter.toDto(dbCreatedCustomGroup));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestErrorData")
    void tryToUpdateCustomInstallmentGroupDto(
            String name,
            String url,
            Map<String, ?> queryParams,
            CustomInstallmentGroupDto request,
            HttpStatus httpStatus,
            Consumer<ResponseEntity<String>> responseConsumer
    ) {

        HttpEntity<CustomInstallmentGroupDto> httpEntity = new HttpEntity<>(request);
        var response = testRestTemplate.exchange(
                url,
                HttpMethod.PUT,
                httpEntity,
                new ParameterizedTypeReference<String>() {
                },
                queryParams);
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
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
            HttpStatus httpStatus,
            Consumer<ResponseEntity<String>> responseConsumer
    ) {
        return Arguments.of(
                name,
                url,
                queryParams,
                request.get(),
                httpStatus,
                responseConsumer
        );
    }

    @SuppressWarnings("MethodLength")
    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                createArguments(
                        "Обновление группы рассрочек с валидными параметрами",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        InstallmentForGroupProductsControllerUpdateTest::createDefaultInstallmentGroup
                ),
                createArguments(
                        "Обновление группы рассрочек с минимальным набором валидных параметров (brands)",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
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
                        "Обновление группы рассрочек с минимальным набором валидных параметров (categories)",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
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
                        "Обновление группы рассрочек с ограничениями по бренду и категориям",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("BRAND_TEST"));
                            request.setCategoryIds(List.of(90867L));
                            request.setBrandIds(List.of(142L));
                            return request;
                        }
                ),
                createArguments(
                        "Обновление группы рассрочек с валидными параметрами и источником FILE",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        InstallmentForGroupProductsControllerUpdateTest::createDefaultFileInstallmentGroup
                ),
                createArguments(
                        "Обновление группы рассрочек с минимальным набором валидных параметров и источником FILE",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
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
        dto.setId(4L);
        dto.setName("Boring installment custom group");
        dto.setCategoryIds(List.of(1L));
        dto.setBrandIds(List.of(2L));
        dto.setInstallments(List.of("MONTH_AND_HALF"));
        dto.setEnabled(true);
        dto.setStartDateTime(Instant.now().atOffset(ZoneOffset.UTC));
        dto.setEndDateTime(Instant.now().plus(1, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC));
        dto.setSource(CustomInstallmentGroupDto.SourceEnum.DYNAMIC_GROUPS);
        return dto;
    }

    private static CustomInstallmentGroupDto createDefaultFileInstallmentGroup() {
        CustomInstallmentGroupDto dto = new CustomInstallmentGroupDto();
        dto.setId(4L);
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

    @SuppressWarnings("MethodLength")
    public static Stream<Arguments> parameterizedTestErrorData() {
        return Stream.of(
                createErrorArguments(
                        "Обновление группы рассрочек с пустым именем",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setName(null);
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("NotNull.customInstallmentGroupDto.name")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с пустым списком рассрочек",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(null);
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody())
                                .contains("customInstallmentGroup.installments: is null or empty")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с пустым списком брендов и категорий",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setBrandIds(null);
                            request.setCategoryIds(null);
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> {
                            assertThat(response.getBody()).contains("brandIds: is null or empty");
                            assertThat(response.getBody()).contains("categoryIds: is null or empty");
                        }
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с пустым source",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setSource(null);
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("NotNull.customInstallmentGroupDto.source")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с пустым shopId",
                        REQUEST_URL,
                        Map.of("shopId", ""),
                        InstallmentForGroupProductsControllerUpdateTest::createDefaultInstallmentGroup,
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("shopId: must not be null")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с startDate>endDate",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setEndDateTime(Instant.now().atOffset(ZoneOffset.UTC));
                            request.setStartDateTime(Instant.now().plus(1, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC));
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("startDate: must be greater than or equal" +
                                " to " +
                                "endDate")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с невалидным installment",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("INVALID"));
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("Invalid installments: [INVALID]")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с невалидным installment(unsupported brands)",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("BRAND_TEST"));
                            request.setCategoryIds(List.of(90867L));
                            request.setBrandIds(List.of(99L));
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("Invalid installments: [BRAND_TEST]")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с невалидным installment(unsupported categoryIds)",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("BRAND_TEST"));
                            request.setCategoryIds(List.of(99L));
                            request.setBrandIds(List.of(142L));
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("Invalid installments: [BRAND_TEST]")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с невалидным отключенной installment",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setInstallments(List.of("DISABLED_TEST"));
                            request.setCategoryIds(List.of(99L));
                            request.setBrandIds(List.of(142L));
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains("Invalid installments: [DISABLED_TEST]")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с невалидным shop_id (99999)",
                        REQUEST_URL,
                        Map.of("shopId", "99999"),
                        InstallmentForGroupProductsControllerUpdateTest::createDefaultInstallmentGroup,
                        HttpStatus.NOT_FOUND,
                        response -> assertThat(response.getBody())
                                .contains("InstallmentGroup(id=4, shopId=99999) not found")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с несуществующим id (99999)",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setId(99999L);
                            return request;
                        },
                        HttpStatus.NOT_FOUND,
                        response -> assertThat(response.getBody())
                                .contains("InstallmentGroup(id=99999, shopId=42) not found")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с несуществующим id (null)",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultInstallmentGroup();
                            request.setId(null);
                            return request;
                        },
                        HttpStatus.NOT_FOUND,
                        response -> assertThat(response.getBody())
                                .contains("InstallmentGroup(id=null, shopId=42) not found")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с источником FILE и неверным shopId",
                        REQUEST_URL,
                        Map.of("shopId", "99999"),
                        InstallmentForGroupProductsControllerUpdateTest::createDefaultFileInstallmentGroup,
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains(
                                "Invalid resourceId: 'de7cd822-d5f9-47cb-8fee-36546d7c9ab1' and/or shopId: '99999'")
                ),
                createErrorArguments(
                        "Обновление группы рассрочек с источником FILE и неверным shopId",
                        REQUEST_URL,
                        Map.of("shopId", "42"),
                        () -> {
                            var request = createDefaultFileInstallmentGroup();
                            request.setResourceId("incorrect-resourceId");
                            return request;
                        },
                        HttpStatus.BAD_REQUEST,
                        response -> assertThat(response.getBody()).contains(
                                "Invalid resourceId: 'incorrect-resourceId' and/or shopId: '42'")
                )
        );
    }

}

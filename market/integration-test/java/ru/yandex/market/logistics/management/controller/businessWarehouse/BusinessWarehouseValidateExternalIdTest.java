package ru.yandex.market.logistics.management.controller.businessWarehouse;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus.INVALID;
import static ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus.OK;
import static ru.yandex.market.logistics.management.util.TestUtil.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Проверка уникальности externalId склада в пределах бизнеса")
@DatabaseSetup("/data/controller/businessWarehouse/external_id_validation_prepare.xml")
class BusinessWarehouseValidateExternalIdTest extends AbstractContextualTest {
    private static final long DEFAULT_BUSINESS_ID = 12345;
    private static final long DEFAULT_PARTNER_ID = 1;
    private static final long NON_EXISTING_ID = 100500;
    private static final String DEFAULT_EXTERNAL_ID = "ext-id";

    @MethodSource
    @DisplayName("Проверка работы валидации externalId, невалидные кейсы")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void externalIdValidationLogicForInvalidCases(
        String displayName,
        BusinessWarehouseValidationRequest validationRequest
    ) {
        performValidateSuccess(validationRequest, INVALID);
    }

    @Nonnull
    private static Stream<Arguments> externalIdValidationLogicForInvalidCases() {
        return Stream.of(
            Arguments.of(
                "Полное совпадение externalId, businessId, partnerId другой",
                validationRequest(DEFAULT_EXTERNAL_ID, DEFAULT_BUSINESS_ID, NON_EXISTING_ID)
            ),
            Arguments.of(
                "Совпадение только по businessId и externalId, partnerId не указан",
                validationRequest(DEFAULT_EXTERNAL_ID, DEFAULT_BUSINESS_ID, null)
            )
        );
    }

    @MethodSource
    @DisplayName("Проверка работы валидации externalId, валидные кейсы")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void externalIdValidationLogicForValidCases(
        String displayName,
        BusinessWarehouseValidationRequest validationRequest
    ) {
        performValidateSuccess(validationRequest, OK);
    }

    @Nonnull
    private static Stream<Arguments> externalIdValidationLogicForValidCases() {
        return Stream.of(
            Arguments.of(
                "Существует совпадение с неактивной точкой",
                validationRequest("ext-id-inactive", DEFAULT_BUSINESS_ID, DEFAULT_PARTNER_ID)
            ),
            Arguments.of(
                "Существует совпадение с точкой не с типом WAREHOUSE",
                validationRequest("ext-id-pickup", DEFAULT_BUSINESS_ID, DEFAULT_PARTNER_ID)
            ),
            Arguments.of(
                "Лог точки с данным businessId и externalId не существует",
                validationRequest("non-existing-ext-id", NON_EXISTING_ID, null)
            ),
            Arguments.of(
                "Существует точка с таким же externalId, но businessId и partnerId другие",
                validationRequest(DEFAULT_EXTERNAL_ID, NON_EXISTING_ID, NON_EXISTING_ID)
            ),
            Arguments.of(
                "Полное совпадение externalId, businessId и partnerId, "
                    + "может быть только при попытке обновить склад существующими данными",
                validationRequest(DEFAULT_EXTERNAL_ID, DEFAULT_BUSINESS_ID, DEFAULT_PARTNER_ID)
            ),
            Arguments.of(
                "Совпадение только по externalId, partnerId не указан",
                validationRequest(DEFAULT_EXTERNAL_ID, NON_EXISTING_ID, null)
            ),
            //кейс вряд ли воспроизводим, чтобы он воспроизвелся необходимо чтобы businessId в несу и лмс различались
            //суть - та же попытка обновить уже имеющимися данными склад
            Arguments.of(
                "Совпадение только по partnerId и по externalId",
                validationRequest(DEFAULT_EXTERNAL_ID, NON_EXISTING_ID, DEFAULT_PARTNER_ID)
            )
        );
    }

    @SneakyThrows
    @MethodSource
    @DisplayName("Проверка валидаций тела запроса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void requestDtoValidations(
        String displayName,
        BusinessWarehouseValidationRequest validationRequest,
        String fieldName,
        String errorCode,
        String message
    ) {
        performValidationRequest(validationRequest)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                "businessWarehouseValidationRequest",
                fieldName,
                errorCode,
                message
            ));
    }

    @Nonnull
    private static Stream<Arguments> requestDtoValidations() {
        return Stream.of(
            Arguments.of(
                "Пустой externalId",
                validationRequest("   ", DEFAULT_BUSINESS_ID, null),
                "externalId",
                "NotBlank",
                "must not be blank"
            ),
            Arguments.of(
                "Нет businessId",
                validationRequest(DEFAULT_EXTERNAL_ID, null, null),
                "businessId",
                "NotNull",
                "must not be null"
            ),
            Arguments.of(
                "Нет externalId",
                validationRequest(null, DEFAULT_BUSINESS_ID, null),
                "externalId",
                "NotBlank",
                "must not be blank"
            )
        );
    }

    @Nonnull
    private static BusinessWarehouseValidationRequest validationRequest(
        @Nullable String externalId,
        @Nullable Long businessId,
        @Nullable Long partnerId
    ) {
        return BusinessWarehouseValidationRequest.builder()
            .externalId(externalId)
            .businessId(businessId)
            .partnerId(partnerId)
            .build();
    }

    @SneakyThrows
    private void performValidateSuccess(
        BusinessWarehouseValidationRequest request,
        BusinessWarehouseValidationStatus expectedStatus
    ) {
        performValidationRequest(request)
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(expectedStatus.toString())));
    }

    @Nonnull
    @SneakyThrows
    private ResultActions performValidationRequest(BusinessWarehouseValidationRequest request) {
        return mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/business-warehouse/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }
}

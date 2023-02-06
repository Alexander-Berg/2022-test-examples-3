package ru.yandex.market.logistics.nesu.controller.internal.registration;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.TaxSystem;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.logistics.nesu.jobs.producer.CreateTrustProductProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetupNewShopProducer;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_EXTERNAL_ID_LENGTH;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_NAME_LENGTH;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_SYMBOLS_IN_EXTERNAL_ID;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Регистрация магазина")
@ParametersAreNonnullByDefault
class ShopRegistrationTest extends AbstractContextualTest {
    private static final Map<String, Object> DAAS_ONLY_VALIDATION_ARGUMENTS = Map.of(
        "roles", EnumSet.of(ShopRole.DAAS),
        "fields", List.of("marketId", "balanceClientId", "taxSystem", "balanceContractId", "balancePersonId")
    );

    private static final Map<String, Object> DBS_ONLY_VALIDATION_ARGUMENTS = Map.of(
        "roles", EnumSet.of(ShopRole.DROPSHIP_BY_SELLER, ShopRole.RETAIL),
        "fields", List.of("regionId")
    );

    private static final Set<ShopRole> ROLES_WITH_REGION = EnumSet.of(ShopRole.DROPSHIP_BY_SELLER, ShopRole.RETAIL);

    private static final String OBJECT_NAME = "registerShopDto";

    @Autowired
    private CreateTrustProductProducer createTrustProductProducer;

    @Autowired
    private SetupNewShopProducer setupNewShopProducer;

    @BeforeEach
    void setup() {
        doNothing().when(createTrustProductProducer).produceTask(anyLong());
        doNothing().when(setupNewShopProducer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(createTrustProductProducer, setupNewShopProducer);
    }

    @Test
    @DisplayName("Успешная регистрация DaaS магазина")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_daas_registration.xml",
        assertionMode = NON_STRICT
    )
    void successDaas() throws Exception {
        registerShop("controller/shop-registration/register_daas_shop_request.json")
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(createTrustProductProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успешная регистрация Dropship магазина")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_registration.xml",
        assertionMode = NON_STRICT
    )
    void successDropship() throws Exception {
        registerShop("controller/shop-registration/register_dropship_shop_request.json")
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Успешная регистрация Dropship магазина с созданием партнера при регистрации")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_with_create_partner_registration.xml",
        assertionMode = NON_STRICT
    )
    void successDropshipWithEarlySetup() throws Exception {
        registerShop("controller/shop-registration/register_dropship_with_create_partner_shop_request.json")
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(setupNewShopProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успешная регистрация Crossdock магазина")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_supplier_registration.xml",
        assertionMode = NON_STRICT
    )
    void successSupplier() throws Exception {
        registerShop("controller/shop-registration/register_supplier_shop_request.json")
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Успешная регистрация Dropship-by-Seller магазина")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_by_seller_registration.xml",
        assertionMode = NON_STRICT
    )
    void successDropshipBySeller() throws Exception {
        registerShop("controller/shop-registration/register_dropship_by_seller_shop_request.json")
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(setupNewShopProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успешная регистрация RETAIL магазина")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_retail_registration.xml",
        assertionMode = NON_STRICT
    )
    void successRetail() throws Exception {
        registerShop("controller/shop-registration/register_retail_shop_request.json")
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(setupNewShopProducer).produceTask(1);
    }


    @Test
    @DisplayName("Магазин уже существует")
    @DatabaseSetup("/controller/shop-registration/before_shop_already_exists_registration.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/before_shop_already_exists_registration.xml",
        assertionMode = NON_STRICT
    )
    void shopAlreadyExists() throws Exception {
        registerShop("controller/shop-registration/register_daas_shop_request.json")
            .andExpect(status().isAlreadyReported());
    }

    @Test
    @DisplayName("Некорректные данные")
    void invalidData() throws Exception {
        registerShop("controller/shop-registration/register_shop_invalid_request.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError("businessId", "must not be null", OBJECT_NAME, "NotNull"),
                fieldError("id", "must not be null", OBJECT_NAME, "NotNull"),
                fieldError("name", "must not be blank", OBJECT_NAME, "NotBlank"),
                fieldError("role", "must not be null", OBJECT_NAME, "NotNull")
            )));
    }

    @Test
    @DisplayName("Некорректные данные DAAS магазина")
    void invalidDataDaas() throws Exception {
        registerShop("controller/shop-registration/register_daas_shop_invalid_request.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError(
                    "balanceClientId",
                    "must be not null for roles in [DAAS]",
                    OBJECT_NAME,
                    "FieldsNotNullForRoles",
                    DAAS_ONLY_VALIDATION_ARGUMENTS
                ),
                fieldError(
                    "balanceContractId",
                    "must be not null for roles in [DAAS]",
                    OBJECT_NAME,
                    "FieldsNotNullForRoles",
                    DAAS_ONLY_VALIDATION_ARGUMENTS
                ),
                fieldError(
                    "balancePersonId",
                    "must be not null for roles in [DAAS]",
                    OBJECT_NAME,
                    "FieldsNotNullForRoles",
                    DAAS_ONLY_VALIDATION_ARGUMENTS
                ),
                fieldError(
                    "marketId",
                    "must be not null for roles in [DAAS]",
                    OBJECT_NAME,
                    "FieldsNotNullForRoles",
                    DAAS_ONLY_VALIDATION_ARGUMENTS
                ),
                fieldError("name", "must not be blank", OBJECT_NAME, "NotBlank"),
                fieldError(
                    "taxSystem",
                    "must be not null for roles in [DAAS]",
                    OBJECT_NAME,
                    "FieldsNotNullForRoles",
                    DAAS_ONLY_VALIDATION_ARGUMENTS
                )
            )));
    }

    @ParameterizedTest
    @EnumSource(value = ShopRole.class, names = {"DROPSHIP_BY_SELLER", "DROPSHIP", "SUPPLIER", "RETAIL"})
    @DisplayName("Минимальный набор необходимых полей")
    void earlyRegistration(ShopRole shopRole) throws Exception {
        mockMvc.perform(
            post("/internal/shops/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto(shopRole)))
        )
            .andExpect(status().isOk());

        verifyProducerMocks(15L, shopRole);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Некорректные данные региона для DBS и RETAIL магазинов")
    void invalidRegionId(String displayName, String jsonPath) throws Exception {
        registerShop(jsonPath)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError(
                    "regionId",
                    "must be not null for roles in [DROPSHIP_BY_SELLER, RETAIL]",
                    OBJECT_NAME,
                    "FieldsNotNullForRoles",
                    DBS_ONLY_VALIDATION_ARGUMENTS
                )
            )));
    }

    @Nonnull
    private static Stream<Arguments> invalidRegionId() {
        return Stream.of(
            Arguments.of("DBS shop", "controller/shop-registration/register_dbs_shop_invalid_request.json"),
            Arguments.of("RETAIL shop", "controller/shop-registration/register_retail_shop_invalid_request.json")
        );
    }

    @Test
    @DisplayName("Валидация названия региона и локации")
    void incorrectRegionName() throws Exception {
        registerShop("controller/shop-registration/register_retail_invalid_region_name.json")
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Locality name is not found for region 225"));
    }

    @MethodSource
    @DisplayName("Проверка стандартных валидаций дто")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void defaultValidationsChecks(
        String displayName,
        RegisterShopDto invalidDto,
        ValidationErrorData.ValidationErrorDataBuilder errorBuilder
    ) throws Exception {
        registerShop(invalidDto)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(errorBuilder.forObject(OBJECT_NAME)));
    }

    @Nonnull
    private static Stream<Arguments> defaultValidationsChecks() {
        return Stream.of(
            Arguments.of(
                "id = null",
                registerShopDtoBuilder(ShopRole.DROPSHIP).id(null).build(),
                fieldErrorBuilder("id", ValidationErrorData.ErrorType.NOT_NULL)
            ),
            Arguments.of(
                "role = null",
                registerShopDtoBuilder(ShopRole.DROPSHIP).role(null).build(),
                fieldErrorBuilder("role", ValidationErrorData.ErrorType.NOT_NULL)
            ),
            Arguments.of(
                "businessId = null",
                registerShopDtoBuilder(ShopRole.DROPSHIP).businessId(null).build(),
                fieldErrorBuilder("businessId", ValidationErrorData.ErrorType.NOT_NULL)
            ),
            Arguments.of(
                "name is Blank",
                registerShopDtoBuilder(ShopRole.DROPSHIP).name("    ").build(),
                fieldErrorBuilder("name", ValidationErrorData.ErrorType.NOT_BLANK)
            ),
            Arguments.of(
                "name has more than 256 symbols",
                registerShopDtoBuilder(ShopRole.DROPSHIP).name(INVALID_NAME_LENGTH).build(),
                fieldErrorBuilder("name", ValidationErrorData.ErrorType.size(0, 256))
            ),
            Arguments.of(
                "externalId has more than 64 symbols",
                registerShopDtoBuilder(ShopRole.DROPSHIP).externalId(INVALID_EXTERNAL_ID_LENGTH).build(),
                fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.size(0, 64))
            ),
            Arguments.of(
                "externalId has invalid symbols",
                registerShopDtoBuilder(ShopRole.DROPSHIP).externalId(INVALID_SYMBOLS_IN_EXTERNAL_ID).build(),
                fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.VALID_EXTERNAL_ID)
            )
        );
    }

    private void verifyProducerMocks(long shopId, ShopRole shopRole) {
        if (shopRole == ShopRole.DAAS) {
            verify(createTrustProductProducer).produceTask(shopId);
        }

        if (ROLES_WITH_REGION.contains(shopRole)) {
            verify(setupNewShopProducer).produceTask(shopId);
        }
    }

    @Nonnull
    private static RegisterShopDto.RegisterShopDtoBuilder registerShopDtoBuilder() {
        return registerShopDtoBuilder(ShopRole.DAAS);
    }

    @Nonnull
    private static RegisterShopDto.RegisterShopDtoBuilder registerShopDtoBuilder(ShopRole shopRole) {
        return RegisterShopDto.builder()
            .id(15L)
            .role(shopRole)
            .businessId(100L)
            .balanceClientId(1L)
            .taxSystem(TaxSystem.ESN)
            .balanceContractId(1L)
            .balancePersonId(1L)
            .name("магазин");
    }

    @Nonnull
    private RegisterShopDto registerDto(ShopRole shopRole) {
        RegisterShopDto.RegisterShopDtoBuilder builder = registerShopDtoBuilder().role(shopRole);

        if (ROLES_WITH_REGION.contains(shopRole)) {
            builder.regionId(213);
        }

        return builder.build();
    }

    @Nonnull
    private ResultActions registerShop(String contentFile) throws Exception {
        return mockMvc.perform(
            post("/internal/shops/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(contentFile))
        );
    }

    @Nonnull
    private ResultActions registerShop(RegisterShopDto registerShopDto) throws Exception {
        return mockMvc.perform(
            post("/internal/shops/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerShopDto))
        );
    }
}

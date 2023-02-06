package ru.yandex.market.logistics.nesu.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.dto.ShopOwnDeliveryRestrictionsDto;
import ru.yandex.market.logistics.nesu.model.dto.ShopOwnDeliveryRestrictionsDto.ShopOwnDeliveryRestrictionsDtoBuilder;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/sender/before/prepare_for_search.xml")
class ShopOwnDeliveryRestrictionsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Попытка обновить несуществующие ограничения")
    void createShopOwnDeliveryRestrictions() throws Exception {
        putShopOwnDeliveryRestrictions(50001L, defaultShopOwnDeliveryRestrictionsBuilder().build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_OWN_DELIVERY_RESTRICTIONS] with ids [50001]"));
    }

    @Test
    @DisplayName("Обновить ограничения")
    @DatabaseSetup("/repository/shop/own-delivery-restrictions/before/shop_own_delivery_restrictions.xml")
    @ExpectedDatabase(
        value = "/repository/shop/own-delivery-restrictions/after/shop_own_delivery_restrictions.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateShopOwnDeliveryRestrictions() throws Exception {
        putShopOwnDeliveryRestrictions(50001L, defaultShopOwnDeliveryRestrictionsBuilder().build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-own-delivery-restrictions/get-after-edit.json"));
    }

    @Test
    @DisplayName("Магазин не найден при попытке обновить ограничения")
    void updateShopOwnDeliveryRestrictionsShopNotFound() throws Exception {
        putShopOwnDeliveryRestrictions(1L, defaultShopOwnDeliveryRestrictionsBuilder().build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @Test
    @DisplayName("Получить пустые ограничения в виде таблицы")
    void getEmptyShopOwnDeliveryRestrictionsGrid() throws Exception {
        getShopOwnDeliveryRestrictionsGrid(50001L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-own-delivery-restrictions/get-grid-empty.json"));
    }

    @Test
    @DisplayName("Получить ограничения в виде таблицы")
    @DatabaseSetup("/repository/shop/own-delivery-restrictions/before/shop_own_delivery_restrictions.xml")
    void getShopOwnDeliveryRestrictionsGrid() throws Exception {
        getShopOwnDeliveryRestrictionsGrid(50001L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-own-delivery-restrictions/get-grid.json"));
    }

    @Test
    @DisplayName("Магазин не найден при попытке получить ограничения в виде таблицы")
    void getShopOwnDeliveryRestrictionsGridShopNotFound() throws Exception {
        getShopOwnDeliveryRestrictionsGrid(1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @Test
    @DisplayName("Получить пустые ограничения для редактирования")
    void getEmptyShopOwnDeliveryRestrictionsForEdit() throws Exception {
        getShopOwnDeliveryRestrictionsForEdit(50001L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-own-delivery-restrictions/get-for-edit-empty.json"));
    }

    @Test
    @DisplayName("Получить ограничения для редактирования")
    @DatabaseSetup("/repository/shop/own-delivery-restrictions/before/shop_own_delivery_restrictions.xml")
    void getShopOwnDeliveryRestrictionsForEdit() throws Exception {
        getShopOwnDeliveryRestrictionsForEdit(50001L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-own-delivery-restrictions/get-for-edit.json"));
    }

    @Test
    @DisplayName("Магазин не найден при попытке получить ограничения для редактирования")
    void getShopOwnDeliveryRestrictionsForEditShopNotFound() throws Exception {
        getShopOwnDeliveryRestrictionsForEdit(1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("ownDeliveryRestrictionsValidationProvider")
    @DisplayName("Валидация запроса на обновление ограничений")
    void ownDeliveryRestrictionsRequestValidation(
        ValidationErrorData errorData,
        ShopOwnDeliveryRestrictionsDto body
    ) throws Exception {
        putShopOwnDeliveryRestrictions(50001L, body)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(errorData));
    }

    @Nonnull
    private static Stream<Arguments> ownDeliveryRestrictionsValidationProvider() {
        return Stream.of(
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceCount",
                    "must not be null",
                    "NotNull"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceCount(null).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServicePickupPointCount",
                    "must not be null",
                    "NotNull"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServicePickupPointCount(null).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceTariffCount",
                    "must not be null",
                    "NotNull"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceTariffCount(null).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceTariffDirectionCount",
                    "must not be null",
                    "NotNull"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceTariffDirectionCount(null).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceTariffWeightBreaksCount",
                    "must not be null",
                    "NotNull"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceTariffWeightBreaksCount(null).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceCount",
                    "must be greater than or equal to 0",
                    "PositiveOrZero"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceCount(-1).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServicePickupPointCount",
                    "must be greater than or equal to 0",
                    "PositiveOrZero"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServicePickupPointCount(-1).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceTariffCount",
                    "must be greater than or equal to 0",
                    "PositiveOrZero"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceTariffCount(-1).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceTariffDirectionCount",
                    "must be greater than or equal to 0",
                    "PositiveOrZero"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceTariffDirectionCount(-1).build()
            ),
            Arguments.of(
                shopOwnDeliveryObjectError(
                    "deliveryServiceTariffWeightBreaksCount",
                    "must be greater than or equal to 0",
                    "PositiveOrZero"
                ),
                defaultShopOwnDeliveryRestrictionsBuilder().deliveryServiceTariffWeightBreaksCount(-1).build()
            )
        );
    }

    private static ValidationErrorData shopOwnDeliveryObjectError(String field, String message, String code) {
        return ValidationErrorData.fieldError(field, message, "shopOwnDeliveryRestrictionsDto", code);
    }

    private static ShopOwnDeliveryRestrictionsDtoBuilder defaultShopOwnDeliveryRestrictionsBuilder() {
        return ShopOwnDeliveryRestrictionsDto.builder()
            .deliveryServiceCount(10)
            .deliveryServicePickupPointCount(11)
            .deliveryServiceTariffCount(12)
            .deliveryServiceTariffDirectionCount(13)
            .deliveryServiceTariffWeightBreaksCount(14);
    }

    @Nonnull
    private ResultActions getShopOwnDeliveryRestrictionsGrid(long shopId) throws Exception {
        return mockMvc.perform(
            get("/admin/shops/own-delivery/restrictions")
                .param("shopId", String.valueOf(shopId))
        );
    }

    @Nonnull
    private ResultActions getShopOwnDeliveryRestrictionsForEdit(long shopId) throws Exception {
        return mockMvc.perform(get("/admin/shops/own-delivery/restrictions/" + shopId));
    }

    @Nonnull
    private ResultActions putShopOwnDeliveryRestrictions(
        long shopId,
        ShopOwnDeliveryRestrictionsDto body
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = request(
            HttpMethod.PUT,
            "/admin/shops/own-delivery/restrictions/" + shopId,
            body
        );
        return mockMvc.perform(requestBuilder);
    }

}

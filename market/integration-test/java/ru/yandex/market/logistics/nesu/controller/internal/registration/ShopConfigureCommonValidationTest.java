package ru.yandex.market.logistics.nesu.controller.internal.registration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Конфигурирование магазина, ошибки валидации")
class ShopConfigureCommonValidationTest extends BaseShopConfigureTest {
    @Test
    @DisplayName("Магазин не существует")
    void shopNotFound() throws Exception {
        configureShop()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @Test
    @DisplayName("Невозможно сконфигурировать DaaS магазин")
    @DatabaseSetup("/controller/shop-registration/after_daas_registration.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_daas_registration.xml",
        assertionMode = NON_STRICT
    )
    void daasFailure() throws Exception {
        configureShop()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Shop with role DAAS cannot be configured"));
    }

    @Test
    @DisplayName("Передан идентификатор null как магазин")
    void nullShop() throws Exception {
        configureShop(null, defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("For input string: \"null\""));
    }

    @ParameterizedTest
    @MethodSource
    @DatabaseSetup(value = {
        "/controller/shop-registration/dropship_not_configured.xml",
        "/controller/shop-registration/supplier_not_configured.xml",
        "/controller/shop-registration/dbs_not_configured.xml",
    })
    @DisplayName("Ранняя регистрация. Невалидный запрос")
    void earlyRegisterValidation(
        @SuppressWarnings("unused") String name,
        Long shopId,
        List<String> fields
    ) throws Exception {
        configureShop(shopId, ConfigureShopDto.builder().build())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fields.stream()
                    .map(field -> fieldError(field, "must not be null", "configureShopDto", "NotNull"))
                    .collect(Collectors.toList())
            ));
    }

    @Nonnull
    private static Stream<Arguments> earlyRegisterValidation() {
        return Stream.of(
            Arguments.of("DBS", 4L, List.of("balanceClientId", "balanceContractId", "balancePersonId", "marketId")),
            Arguments.of("DROPSHIP", 2L, List.of("balanceClientId", "marketId")),
            Arguments.of("SUPPLIER", 3L, List.of("balanceClientId", "marketId"))
        );
    }
}

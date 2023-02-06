package ru.yandex.market.logistics.nesu.controller.internal.registration;

import java.util.function.UnaryOperator;
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
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Конфигурирование FBS магазина")
class ShopConfigureDropshipTest extends BaseShopConfigureTest {
    @Test
    @DisplayName("Ранняя регистрация. Успешная конфигурация дропшипа")
    @DatabaseSetup("/controller/shop-registration/dropship_not_configured.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/dropship_configured.xml",
        assertionMode = NON_STRICT
    )
    void successEarlyRegister() throws Exception {
        configureShop(2L, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(noContent());
        verify(pushMarketIdToLmsProducer).produceTask(1L, 2L);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Повторная настройка магазина с теми же данными")
    @DatabaseSetup("/controller/shop-registration/dropship_configured.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/dropship_configured.xml",
        assertionMode = NON_STRICT
    )
    void reconfigure(
        @SuppressWarnings("unused") String displayName,
        UnaryOperator<ConfigureShopDto.ConfigureShopDtoBuilder> configureShopDtoUpdater
    ) throws Exception {
        configureShop(2L, configureShopDtoUpdater.apply(configureShopDtoBuilder()).build())
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Nonnull
    private static Stream<Arguments> reconfigure() {
        return Stream.of(
            Arguments.of(
                "Те же данные для конфигурации",
                (UnaryOperator<ConfigureShopDto.ConfigureShopDtoBuilder>) configureShopDtoBuilder ->
                    configureShopDtoBuilder
            ),
            Arguments.of(
                "Отличается значение balanceContractId",
                (UnaryOperator<ConfigureShopDto.ConfigureShopDtoBuilder>) configureShopDtoBuilder ->
                    configureShopDtoBuilder
                        .balanceContractId(2600L)
            ),
            Arguments.of(
                "Отличается значение balancePersonId",
                (UnaryOperator<ConfigureShopDto.ConfigureShopDtoBuilder>) configureShopDtoBuilder ->
                    configureShopDtoBuilder
                        .balancePersonId(2000L)
            )
        );
    }

    @DisplayName("Повторная настройка c другими данными")
    @DatabaseSetup("/controller/shop-registration/dropship_configured.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/dropship_reconfigured.xml",
        assertionMode = NON_STRICT
    )
    void reconfigureShop() throws Exception {
        configureShop(
            2L,
            configureShopDtoBuilder()
                .marketId(10L)
                .balanceClientId(2500L)
                .build()
        )
            .andExpect(status().isOk());
    }
}

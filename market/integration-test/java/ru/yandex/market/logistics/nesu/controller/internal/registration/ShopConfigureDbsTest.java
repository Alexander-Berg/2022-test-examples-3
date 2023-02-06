package ru.yandex.market.logistics.nesu.controller.internal.registration;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Конфигурирование DBS магазина")
class ShopConfigureDbsTest extends BaseShopConfigureTest {
    @Test
    @DisplayName("Успешная установка marketId для DSBS магазина")
    @DatabaseSetup({
        "/controller/shop-registration/after_dropship_by_seller_registration.xml",
        "/controller/shop-registration/dbs_shop_partner_setting.xml",
    })
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_by_seller_configuration.xml",
        assertionMode = NON_STRICT
    )
    void success() throws Exception {
        configureShop()
            .andExpect(status().isOk())
            .andExpect(noContent());
        verify(pushMarketIdToLmsProducer).produceTask(3, 1);
    }

    @Test
    @DisplayName("Повторная настройка магазина с теми же данными")
    @DatabaseSetup({
        "/controller/shop-registration/after_dropship_by_seller_configuration.xml",
        "/controller/shop-registration/dbs_shop_partner_setting.xml",
    })
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_by_seller_configuration.xml",
        assertionMode = NON_STRICT
    )
    void reconfigureWithSameData() throws Exception {
        configureShop()
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @DisplayName("Повторная настройка магазина")
    @DatabaseSetup("/controller/shop-registration/after_dropship_by_seller_configuration.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_by_seller_reconfiguration.xml",
        assertionMode = NON_STRICT
    )
    void reconfigureWithOtherData() throws Exception {
        configureShop(
            1L,
            configureShopDtoBuilder()
                .marketId(10L)
                .balanceClientId(2500L)
                .balanceContractId(2600L)
                .balancePersonId(2000L)
                .build()
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ранняя регистрация. Успешная конфигурация DSBS магазина")
    @DatabaseSetup("/controller/shop-registration/dbs_not_configured.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/dbs_configured.xml",
        assertionMode = NON_STRICT
    )
    void successEarlyRegister() throws Exception {
        configureShop(4L, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(noContent());
        verify(pushMarketIdToLmsProducer).produceTask(3L, 4L);
    }
}

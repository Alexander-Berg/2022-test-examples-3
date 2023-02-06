package ru.yandex.market.logistics.nesu.service.activation;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.model.entity.Shop;

@DisplayName("Активация Supplier-магазина")
public class SupplierActivationTest extends ShopActivationTest {
    @Test
    @DisplayName("OFF -> NEED_SETTINGS, у магазина заполнены не все поля")
    @DatabaseSetup("/service/shop/activation/prepare_supplier_off.xml")
    @DatabaseSetup(value = "/service/shop/activation/null_market_id.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/service/shop/activation/after/need_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void needSettigsFromOff() {
        Shop shop = shopRepository.getShop(1L);
        shopStatusService.activateFromStatus(shop, ShopStatus.OFF);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.NEED_SETTINGS);
    }

    @Test
    @DisplayName("OFF -> NEED_SETTINGS, у магазина нет связки с партнером")
    @DatabaseSetup("/service/shop/activation/prepare_supplier_off.xml")
    @ExpectedDatabase(
        value = "/service/shop/activation/after/need_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void needSettingsFromOffWithWarehouse() {
        Shop shop = shopRepository.getShop(1L);
        shopStatusService.activateFromStatus(shop, ShopStatus.OFF);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.NEED_SETTINGS);
    }

    @Test
    @DisplayName("OFF -> ACTIVE")
    @DatabaseSetup("/service/shop/activation/prepare_supplier_off.xml")
    @DatabaseSetup(value = "/service/shop/activation/partner_setting.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/service/shop/activation/after/active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void activeFromOff() {
        Shop shop = shopRepository.getShop(1L);
        shopStatusService.activateFromStatus(shop, ShopStatus.OFF);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.ACTIVE);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Неудачная активация")
    @DatabaseSetup("/service/shop/activation/prepare_supplier.xml")
    void activationFail(@SuppressWarnings("unused") String name, Shop shop) {
        shopStatusService.activateFromStatus(shop, ShopStatus.NEED_SETTINGS);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.NEED_SETTINGS);
    }

    @Nonnull
    private static Stream<Arguments> activationFail() {
        return Stream.of(
            Arguments.of("Без marketId", validSupplier().setMarketId(null)),
            Arguments.of("Без balanceClientId", validSupplier().setBalanceClientId(null)),
            Arguments.of("Без склада", validSupplier().setId(2L))
        );
    }

    @Override
    protected Shop getShop() {
        return validSupplier();
    }
}

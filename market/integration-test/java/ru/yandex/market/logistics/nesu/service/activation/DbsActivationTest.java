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
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.marketId.MarketIdDto;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPartnerIdPayload;
import ru.yandex.market.logistics.nesu.jobs.processor.PushMarketIdToLmsProcessor;
import ru.yandex.market.logistics.nesu.model.entity.Shop;

import static org.mockito.Mockito.verify;

@DisplayName("Активация Dbs-магазина")
public class DbsActivationTest extends ShopActivationTest {
    @Autowired
    private PushMarketIdToLmsProcessor pushMarketIdToLmsProcessor;
    @Autowired
    private LMSClient lmsClient;

    @Test
    @DatabaseSetup("/service/shop/activation/prepare_dbs.xml")
    @DisplayName("Успех после пуша marketId")
    void success() {
        pushMarketIdToLmsProcessor.processPayload(new ShopIdPartnerIdPayload("1", 2, 1));
        assertShopStatus(ShopStatus.ACTIVE);
        verify(lmsClient).setBusinessWarehouseMarketId(2L, MarketIdDto.of(400L));
    }

    @Test
    @DisplayName("OFF -> NEED_SETTINGS, у магазина заполнены не все поля")
    @DatabaseSetup("/service/shop/activation/prepare_dbs_off.xml")
    @DatabaseSetup(value = "/service/shop/activation/null_market_id.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/service/shop/activation/after/need_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void needSettingsFromOff() {
        Shop shop = shopRepository.getShop(1L);
        shopStatusService.activateFromStatus(shop, ShopStatus.OFF);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.NEED_SETTINGS);
    }

    @Test
    @DisplayName("OFF -> NEED_SETTINGS, у магазина нет связки с партнером")
    @DatabaseSetup("/service/shop/activation/prepare_dbs_off.xml")
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
    @DatabaseSetup("/service/shop/activation/prepare_dbs_off.xml")
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
    @DisplayName("Неудачная активация дбс-магазина")
    void activationFail(@SuppressWarnings("unused") String name, Shop shop) {
        shopStatusService.activateFromStatus(shop, ShopStatus.NEED_SETTINGS);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.NEED_SETTINGS);
    }

    @Nonnull
    private static Stream<Arguments> activationFail() {
        return Stream.of(
            Arguments.of("Без marketId", validDbsShop().setMarketId(null)),
            Arguments.of("Без balanceClientId", validDbsShop().setBalanceClientId(null)),
            Arguments.of("Без balanceContractId", validDbsShop().setBalanceContractId(null)),
            Arguments.of("Без balancePersonId", validDbsShop().setBalancePersonId(null)),
            Arguments.of("Без склада", validDbsShop().setId(2L))
        );
    }

    @Override
    protected Shop getShop() {
        return validDbsShop();
    }
}

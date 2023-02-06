package ru.yandex.market.logistics.nesu.service.activation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.model.entity.Shop;

public class ShopActivationCommonTest extends AbstractContextualTest {
    @Autowired
    private ShopStatusService shopActivationService;

    @ParameterizedTest
    @EnumSource(ShopRole.class)
    @DisplayName("Активатор есть для всех типов магазинов")
    void activatorsForAllRoles(ShopRole role) {
        softly.assertThatCode(() -> shopActivationService.activateFromStatus(
            new Shop().setId(1L).setRole(role),
            ShopStatus.NEED_SETTINGS
        ))
            .doesNotThrowAnyException();
    }
}

package ru.yandex.market.adv.promo.service.environment;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.PROMO_MAX_COUNT;

public class CachedEnvironmentServiceTest extends FunctionalTest {

    @Autowired
    private CachedEnvironmentService cachedEnvironmentService;

    @Test
    @DbUnitDataSet
    @DisplayName("Обновление значение существующей настройки")
    void putValue_updateExistingSetting() {
        Assertions.assertEquals(Optional.empty(), cachedEnvironmentService.getSettingIntegerValue(PROMO_MAX_COUNT));
        cachedEnvironmentService.putSetting("promo_max_count", "qwe");
        Assertions.assertEquals(Optional.empty(), cachedEnvironmentService.getSettingIntegerValue(PROMO_MAX_COUNT));
        Assertions.assertEquals("qwe", cachedEnvironmentService.getSettingStringValue(PROMO_MAX_COUNT).get());
        cachedEnvironmentService.putSetting("promo_max_count", "123");
        Assertions.assertEquals(123, cachedEnvironmentService.getSettingIntegerValue(PROMO_MAX_COUNT).get());
        cachedEnvironmentService.putSetting("promo_max_count", "567");
        Assertions.assertEquals(567, cachedEnvironmentService.getSettingIntegerValue(PROMO_MAX_COUNT).get());
    }
}

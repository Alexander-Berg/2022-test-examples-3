package ru.yandex.market.adv.promo.service.environment;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.PROMO_MAX_COUNT;

public class EnvironmentServiceTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DbUnitDataSet(
            before = "EnvironmentServiceTest/getSettingIntegerValue_invalidCast/before.csv"
    )
    @DisplayName("Кейс, когда ожидается целочисленное значение настройки окружения, а значение - другого типа")
    void getSettingIntegerValue_invalidCast() {
        Assertions.assertEquals(Optional.empty(), environmentService.getSettingIntegerValue(PROMO_MAX_COUNT));
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Кейс, когда настройка окружения ещё не была заведена")
    void get_notFound() {
        Assertions.assertEquals(Optional.empty(), environmentService.getSettingIntegerValue(PROMO_MAX_COUNT));
    }

    @Test
    @DbUnitDataSet(before = "EnvironmentServiceTest/getBooleanTrue/before.csv")
    @DisplayName("Кейс, когда из окружения берётся логическая переменная со значением true")
    void getBooleanTrue() {
        Assertions.assertEquals(Optional.of(true), environmentService.getSettingsBooleanValue("testbool"));
    }

    @Test
    @DbUnitDataSet(before = "EnvironmentServiceTest/getBooleanFalse/before.csv")
    @DisplayName("Кейс, когда из окружения берётся логическая переменная со значением false")
    void getBooleanFalse() {
        Assertions.assertEquals(Optional.of(false), environmentService.getSettingsBooleanValue("testbool"));
    }
}

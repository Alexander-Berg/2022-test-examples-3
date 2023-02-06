package ru.yandex.market.adv.promo.service.environment.dao;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.PROMO_MAX_COUNT;

public class EnvironmentSettingDaoTest extends FunctionalTest {

    @Autowired
    private EnvironmentSettingDao environmentSettingDao;

    @Test
    @DbUnitDataSet
    @DisplayName("Кейс, когда настройка окружения ещё не была заведена")
    void get_notFound() {
        Assertions.assertEquals(Optional.empty(), environmentSettingDao.get(PROMO_MAX_COUNT));
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentSettingDaoTest/get_OK/before.csv"
    )
    @DisplayName("Кейс, когда настройка окружения уже заведена")
    void get_OK() {
        Assertions.assertEquals("150000", environmentSettingDao.get(PROMO_MAX_COUNT).get());
    }
}

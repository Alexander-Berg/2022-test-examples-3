package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяем чекер {@link NotAgencyUserChecker}.
 */
@DbUnitDataSet(before = "NotAgencyUserCheckerTest.before.csv")
class NotAgencyUserCheckerTest extends FunctionalTest {

    @Autowired
    private NotAgencyUserChecker notAgencyUserChecker;

    /**
     * 999 - отсутствующий uid
     * 10 - uid с бизнесом
     * 50 - uid агентского логина, есть у нас в базе без линков
     * 60 - uid ГП без линка к кампании в бизнесе, на подагентском клиенте
     * 70 - uid агентского логина, нет у нас в базе
     * 80 - uid с клиентом которого нет у нас в базе
     */
    @ParameterizedTest
    @CsvSource({"999,true", "10,true", "50,false", "60,true", "70,false", "80,true"})
    void testCheckTyped(long uid, boolean result) {
        assertEquals(result, notAgencyUserChecker.checkTyped(() -> uid, new Authority()));
    }

}

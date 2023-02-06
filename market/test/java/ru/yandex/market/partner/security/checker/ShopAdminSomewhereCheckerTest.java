package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.core.security.checker.ShopAdminSomewhereChecker;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link ShopAdminSomewhereChecker}.
 */
@DbUnitDataSet(before = "ShopAdminSomewhereCheckerTest.csv")
public class ShopAdminSomewhereCheckerTest extends FunctionalTest {
    @Autowired
    private ShopAdminSomewhereChecker shopAdminSomewhereChecker;

    /**
     * 999 100 - отсутствующий uid
     * 10, 404 - отсутствующая кампания
     * 10 100 - uid овнера
     * 20 100 - uid админа
     * 10 200 - нет доступа
     * 10 null - овнер
     * 20 null - админ
     */
    @ParameterizedTest
    @CsvSource({"999,100,false", "10,404,false", "10,100,true", "20,100,true",
            "10,200,false", "10,-1,true", "20,-1,true"})
    void testCheck(long uid, long campaignId, boolean result) {
        assertEquals(result, shopAdminSomewhereChecker.check(new DefaultCampaignable(campaignId, uid, uid), null));
    }
}

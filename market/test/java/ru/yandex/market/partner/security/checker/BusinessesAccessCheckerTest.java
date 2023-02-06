package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessesAccessCheckerTest.csv")
public class BusinessesAccessCheckerTest extends FunctionalTest {
    @Autowired
    private BusinessesAccessChecker businessesAccessChecker;

    /**
     * 999 - отсутствующий uid
     * 10 - uid с бизнесом
     * 20 - uid с контактом без бизнеса
     * 30 - uid с линком к кампании в бизнесе
     * 40 - uid с линком к кампании в бизнесе, но без роли
     * 50 - uid ГП в TPL
     */
    @ParameterizedTest
    @CsvSource({"999,false", "10,true", "20,false", "30,true", "40,false", "50,false"})
    void testCheck(long uid, boolean result) {
        assertEquals(result, businessesAccessChecker.checkTyped(() -> uid, null));
    }
}

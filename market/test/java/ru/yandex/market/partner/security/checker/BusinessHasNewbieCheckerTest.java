package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

/**
 * Проверяем {@link BusinessHasNewbieChecker}.
 */
@DbUnitDataSet(before = "BusinessHasNewbieCheckerTest.before.csv")
class BusinessHasNewbieCheckerTest extends FunctionalTest {
    final static Authority AUTHORITY = new Authority();

    @Autowired
    private BusinessHasNewbieChecker businessHasNewbieChecker;


    @ParameterizedTest
    @CsvSource({
            //3 магазина: is_newbie = true, is_newbie=false, is_newbie=null
            "31,true",
            //поставщик, ever_activated=false, is_newbie=false
            "32,true",
            //магазин, is_newbie=null
            "33,false",
            //2 поставщика кросдока, у одного обе программы активны, у другого только fulfilment
            "34,false",
            //дбс и поставщик, у дбс программа неактивирована
            "35,true",
            //магазин с неактивированной cpc программой. На доступность визарда не влияет, тк cpc
            "36,false",
            //с нулевым клиентом, не реплицированный фбс
            "37,true",
            //с нулевым клиентом, реплицированный фбс
            "38,false"})
    void checkTyped(long businessId, boolean neededValue) {
        final DefaultBusinessUidable data = new DefaultBusinessUidable(businessId, -1, -1);
        Assertions.assertEquals(neededValue, businessHasNewbieChecker.checkTyped(data, AUTHORITY));
    }
}

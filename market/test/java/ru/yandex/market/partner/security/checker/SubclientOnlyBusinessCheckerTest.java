package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для (@link SubclientOnlyBusinessChecker}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessesAccessCheckerTest.csv")
public class SubclientOnlyBusinessCheckerTest extends FunctionalTest {
    @Autowired
    private SubclientOnlyBusinessChecker subclientOnlyBusinessChecker;

    @ParameterizedTest
    @CsvSource({"999, true", "100, false", "101, true"})
    void testCheck(long businessId, boolean expectedResult) {
        assertEquals(expectedResult,
                subclientOnlyBusinessChecker.checkTyped(new DefaultBusinessUidable(businessId, 0, 0), null));
    }
}

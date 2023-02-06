package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

public class HasBusinessUrlFeedCheckerTest extends FunctionalTest {

    @Autowired
    private HasBusinessUrlFeedChecker hasBusinessUrlFeedChecker;

    @ParameterizedTest
    @CsvSource(value = {
            "100; с фидом по ссылке; true",
            "200; без фида по ссылке; false",
            "300; вообще без фида; false",
            "400; с дефолтным фидом; false"
    }, delimiter = ';')
    @DbUnitDataSet(before = "HasBusinessUrlFeedCheckerTest.before.csv")
    void testChecker(long businessId, String authParam, boolean expectedResult) {
        boolean result = hasBusinessUrlFeedChecker.checkTyped(
                new DefaultBusinessUidable(businessId, 0, 0),
                new Authority("test", authParam)
        );
        Assertions.assertEquals(expectedResult, result);
    }
}

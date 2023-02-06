package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;

@DbUnitDataSet(before = "CanOwnBusinessCheckerTest.csv")
public class CanOwnBusinessCheckerTest extends FunctionalTest {
    @Autowired
    private CanOwnBusinessChecker canOwnBusinessChecker;

    static Stream<Arguments> args() {
        return Stream.of(
                //нет контакта
                Arguments.of(100, true),
                //нет линков, не суперадмин
                Arguments.of(110, true),
                //нет линков, суперадмин
                Arguments.of(120, true),
                //есть линки, нет роли BUSINESS_OWNER
                Arguments.of(130, false),
                //есть линки, есть роль BUSINESS_OWNER
                Arguments.of(140, true)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void testCanOwnBusiness(long uid, boolean expected) {
        Assertions.assertEquals(expected, canOwnBusinessChecker.checkTyped(() -> uid, null));
    }
}

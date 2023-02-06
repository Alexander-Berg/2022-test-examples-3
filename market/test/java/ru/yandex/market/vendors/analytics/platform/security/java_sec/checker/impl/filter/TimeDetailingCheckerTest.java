package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.filter;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.o
 */
@DbUnitDataSet(before = "FilterCheckerTest.before.csv")
public class TimeDetailingCheckerTest extends AbstractCheckerTest {

    @ParameterizedTest(name = "{index}")
    @MethodSource("timeDetailingCheckArguments")
    @DisplayName("Проверка доступа к детализациям времени")
    void timeDetailingCheck(long uid, long hid, TimeDetailing timeDetailing, boolean expectedResult) {
        var requestBody = timeDetailingCheckerRequest(uid, hid, timeDetailing);
        assertAccess("timeDetailingChecker", requestBody, expectedResult);
    }

    private static Stream<Arguments> timeDetailingCheckArguments() {
        return Stream.of(
                Arguments.of(1L, 91491L, TimeDetailing.DAY, true),
                Arguments.of(1L, 91492L, TimeDetailing.DAY, false),
                Arguments.of(2L, 91491L, TimeDetailing.DAY, false),
                Arguments.of(2L, 91491L, TimeDetailing.MONTH, true),
                Arguments.of(1001L, 91492L, TimeDetailing.DAY, true),
                Arguments.of(1001L, 91493L, TimeDetailing.MONTH, true),
                Arguments.of(1001L, 91493L, TimeDetailing.WEEK, false),
                Arguments.of(1001L, 91494L, TimeDetailing.YEAR, true)
        );
    }

    private static String timeDetailingCheckerRequest(long uid, long hid, TimeDetailing timeDetailing) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"hid\": %s,\n"
                        + "   \"timeDetailing\": \"%s\"\n"
                        + "}",
                uid,
                hid,
                timeDetailing
        );
    }
}

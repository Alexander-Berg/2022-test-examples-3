package ru.yandex.market.pers.shopinfo.db;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

@DbUnitDataSet(before = "env.before.csv")
public class DbEnvironmentServiceTest  extends FunctionalTest {
    @Autowired
    DbEnvironmentService dbEnvironmentService;

    @ParameterizedTest
    @MethodSource
    void testReturningValues(String name, boolean expectedVal) {
        Assertions.assertEquals(expectedVal, dbEnvironmentService.getBoolValue(name, false));
    }

    private static Stream<Arguments> testReturningValues() {
        return Stream.of(
                Arguments.of("testName1", true),
                Arguments.of("testName2", false),
                Arguments.of("testName3", false),
                Arguments.of("testName4", false)
        );
    }
}

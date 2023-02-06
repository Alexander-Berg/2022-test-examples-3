package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.Uidable;

@DbUnitDataSet(before = "HasAccessToCampaignOfTypeCheckerTest.before.csv")
class HasAccessToCampaignOfTypeCheckerTest extends FunctionalTest {

    private static final long UID = 1L;
    private static final Uidable checkerData = () -> UID;

    @Autowired
    private HasAccessToCampaignOfTypeChecker hasAccessToCampaignOfTypeChecker;

    @ParameterizedTest
    @MethodSource("parameters")
    public void checkTyped(String authorityParams, boolean expected, String message) {
        Authority authority = new Authority("test", authorityParams);
        boolean result = hasAccessToCampaignOfTypeChecker.checkTyped(checkerData, authority);
        Assertions.assertEquals(expected, result, message);
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(
                        "SHOP",
                        Boolean.TRUE,
                        "Должны получить true т.к. у пользователя есть кампания типа SHOP"
                ),
                Arguments.of(
                        "-SHOP",
                        Boolean.FALSE,
                        "Должны получить false т.к. у пользователя есть кампания типа SHOP, а мы проверяем ее " +
                                "отсутствие"
                ),
                Arguments.of(
                        "SUPPLIER_1P,-SHOP",
                        Boolean.FALSE,
                        "Должны получить false т.к. несмотря на наличие кампании типа SUPPLIER_1P, у него так же есть" +
                                " и SHOP"
                ),
                Arguments.of(
                        "-TPL",
                        Boolean.TRUE,
                        "Должны получить true т.к. нет кампании TPL, и мы проверяем ее отсутствие"
                ),
                Arguments.of(
                        "TPL",
                        Boolean.FALSE,
                        "Должны получить false т.к. нет кампании TPL, а мы проверяем ее наличие"
                ),
                Arguments.of(
                        "SHOP,SUPPLIER_1P",
                        Boolean.TRUE,
                        "Должны получить true т.к. у пользователя есть кампания типа и SHOP и SUPPLIER_1P"
                )
        );
    }
}
